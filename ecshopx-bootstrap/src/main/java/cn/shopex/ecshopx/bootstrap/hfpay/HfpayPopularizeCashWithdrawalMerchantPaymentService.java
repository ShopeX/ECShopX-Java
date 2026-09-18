/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.bootstrap.hfpay;

import cn.shopex.ecshopx.common.dispatch.HfpayPopularizeWithdrawEventDispatchPublisher;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.hfpay.domain.HfpayBankCard;
import cn.shopex.ecshopx.hfpay.domain.HfpayMerchantPayment;
import cn.shopex.ecshopx.hfpay.mapper.HfpayBankCardMapper;
import cn.shopex.ecshopx.hfpay.mapper.HfpayMerchantPaymentMapper;
import cn.shopex.ecshopx.hfpay.service.enterapply.HfpayEnterapplyReadService;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayAcouJsonPostClient;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayOrderApplyIdGenerator;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayPaymentSettingService;
import cn.shopex.ecshopx.orders.domain.MerchantPaymentTrade;
import cn.shopex.ecshopx.orders.mapper.MerchantPaymentTradeMapper;
import cn.shopex.ecshopx.orders.service.MerchantPaymentTradeWriteService;
import cn.shopex.ecshopx.popularize.port.PopularizeHfpayRebateMerchantWithdrawalPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service("hfpayPopularizeCashWithdrawalMerchantPaymentService")
public class HfpayPopularizeCashWithdrawalMerchantPaymentService implements PopularizeHfpayRebateMerchantWithdrawalPort {

	private static final String REL_SCENE_NAME_POPULARIZE = "popularize_rebate_cash_withdrawal";

	private final HfPayPaymentSettingService hfPayPaymentSettingService;
	private final HfpayEnterapplyReadService hfpayEnterapplyReadService;
	private final HfpayBankCardMapper hfpayBankCardMapper;
	private final HfpayMerchantPaymentMapper hfpayMerchantPaymentMapper;
	private final HfPayAcouJsonPostClient hfPayAcouJsonPostClient;
	private final HfPayOrderApplyIdGenerator hfPayOrderApplyIdGenerator;
	private final MerchantPaymentTradeWriteService merchantPaymentTradeWriteService;
	private final MerchantPaymentTradeMapper merchantPaymentTradeMapper;
	private final HfpayPopularizeWithdrawEventDispatchPublisher hfpayPopularizeWithdrawEventDispatchPublisher;

	public HfpayPopularizeCashWithdrawalMerchantPaymentService(
			HfPayPaymentSettingService hfPayPaymentSettingService,
			HfpayEnterapplyReadService hfpayEnterapplyReadService,
			HfpayBankCardMapper hfpayBankCardMapper,
			HfpayMerchantPaymentMapper hfpayMerchantPaymentMapper,
			HfPayAcouJsonPostClient hfPayAcouJsonPostClient,
			HfPayOrderApplyIdGenerator hfPayOrderApplyIdGenerator,
			MerchantPaymentTradeWriteService merchantPaymentTradeWriteService,
			MerchantPaymentTradeMapper merchantPaymentTradeMapper,
			HfpayPopularizeWithdrawEventDispatchPublisher hfpayPopularizeWithdrawEventDispatchPublisher) {
		this.hfPayPaymentSettingService = hfPayPaymentSettingService;
		this.hfpayEnterapplyReadService = hfpayEnterapplyReadService;
		this.hfpayBankCardMapper = hfpayBankCardMapper;
		this.hfpayMerchantPaymentMapper = hfpayMerchantPaymentMapper;
		this.hfPayAcouJsonPostClient = hfPayAcouJsonPostClient;
		this.hfPayOrderApplyIdGenerator = hfPayOrderApplyIdGenerator;
		this.merchantPaymentTradeWriteService = merchantPaymentTradeWriteService;
		this.merchantPaymentTradeMapper = merchantPaymentTradeMapper;
		this.hfpayPopularizeWithdrawEventDispatchPublisher = hfpayPopularizeWithdrawEventDispatchPublisher;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> merchantPayment(Map<String, Object> paymentData) {
		long companyId = longVal(paymentData.get("company_id"));
		String relSceneId = str(paymentData.get("rel_scene_id"));
		String relSceneName = str(paymentData.get("rel_scene_name"));
		String userIdRaw = str(paymentData.get("user_id"));
		long transAmtFen = longVal(paymentData.get("trans_amt"));
		String spbillCreateIp = str(paymentData.get("spbill_create_ip"));

		if (!StringUtils.hasText(relSceneId) || !StringUtils.hasText(relSceneName)) {
			throw new BadRequestException("参数错误");
		}
		if (!StringUtils.hasText(userIdRaw)) {
			throw new BadRequestException("请先完成实名认证");
		}
		long userId;
		try {
			userId = Long.parseLong(userIdRaw.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("请先完成实名认证");
		}

		Map<String, Object> setting = hfPayPaymentSettingService.loadForCompany(companyId);
		Object isOpen = setting.get("is_open");
		if (isOpen != null && "false".equalsIgnoreCase(String.valueOf(isOpen).trim())) {
			throw new BadRequestException("请检查汇付天下支付是否启用");
		}

		Map<String, Object> enterDetail = hfpayEnterapplyReadService.getEnterapplyByCompanyAndUser(companyId, userId);
		if (enterDetail == null || enterDetail.isEmpty()) {
			throw new BadRequestException("请先完成实名认证");
		}
		String userCustId = str(enterDetail.get("user_cust_id"));
		String acctId = str(enterDetail.get("acct_id"));
		if (!StringUtils.hasText(userCustId) || !StringUtils.hasText(acctId)) {
			throw new BadRequestException("请先完成实名认证");
		}

		HfpayBankCard bankRow = hfpayBankCardMapper.selectOne(new LambdaQueryWrapper<HfpayBankCard>()
				.eq(HfpayBankCard::getCompanyId, companyId)
				.eq(HfpayBankCard::getUserId, userId)
				.eq(HfpayBankCard::getIsCash, "1")
				.last("LIMIT 1"));
		if (bankRow == null || !StringUtils.hasText(bankRow.getBindCardId())) {
			throw new BadRequestException("请绑定提现银行卡");
		}
		String bindCardId = bankRow.getBindCardId().trim();

		long relSceneIdLong;
		try {
			relSceneIdLong = Long.parseLong(relSceneId.trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数错误");
		}

		String merCustId = String.valueOf(setting.get("mer_cust_id")).trim();

		HfpayMerchantPayment existing = hfpayMerchantPaymentMapper.selectOne(new LambdaQueryWrapper<HfpayMerchantPayment>()
				.eq(HfpayMerchantPayment::getCompanyId, companyId)
				.eq(HfpayMerchantPayment::getRelSceneId, relSceneIdLong)
				.eq(HfpayMerchantPayment::getRelSceneName, relSceneName)
				.last("LIMIT 1"));

		if (existing == null) {
			HfpayMerchantPayment created = new HfpayMerchantPayment();
			created.setCompanyId(companyId);
			created.setRelSceneId(relSceneIdLong);
			created.setRelSceneName(relSceneName);
			created.setMerCustId(merCustId);
			created.setUserCustId(userCustId);
			created.setAcctId(acctId);
			created.setTransAmt(transAmtFen);
			created.setStatus(0);
			hfpayMerchantPaymentMapper.insert(created);
			existing = hfpayMerchantPaymentMapper.selectOne(new LambdaQueryWrapper<HfpayMerchantPayment>()
					.eq(HfpayMerchantPayment::getCompanyId, companyId)
					.eq(HfpayMerchantPayment::getRelSceneId, relSceneIdLong)
					.eq(HfpayMerchantPayment::getRelSceneName, relSceneName)
					.last("LIMIT 1"));
		}

		if (existing == null || existing.getStatus() == null || existing.getStatus() != 1) {
			String transAmtYuan = new BigDecimal(transAmtFen)
					.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
					.toPlainString();
			LinkedHashMap<String, Object> payPayload = new LinkedHashMap<>();
			payPayload.put("version", "10");
			payPayload.put("mer_cust_id", merCustId);
			payPayload.put("user_cust_id", merCustId);
			payPayload.put("order_id", hfPayOrderApplyIdGenerator.nextOrderId());
			payPayload.put(
					"order_date",
					LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.BASIC_ISO_DATE));
			payPayload.put("div_type", "0");
			payPayload.put("in_cust_id", userCustId);
			payPayload.put("in_acct_id", acctId);
			payPayload.put("trans_amt", transAmtYuan);

			Map<String, Object> payResult = hfPayAcouJsonPostClient.pay026(setting, payPayload);
			String respCode = payResult.get("resp_code") == null ? "" : String.valueOf(payResult.get("resp_code")).trim();
			if (!"C00000".equals(respCode)) {
				Map<String, Object> fail = new LinkedHashMap<>();
				fail.put("status", "FAILED");
				Object desc = payResult.get("resp_desc");
				fail.put("error_desc", desc == null ? "" : String.valueOf(desc));
				return fail;
			}

			LambdaUpdateWrapper<HfpayMerchantPayment> uw = new LambdaUpdateWrapper<>();
			uw.eq(HfpayMerchantPayment::getCompanyId, companyId)
					.eq(HfpayMerchantPayment::getRelSceneId, relSceneIdLong)
					.eq(HfpayMerchantPayment::getRelSceneName, relSceneName);
			uw.set(HfpayMerchantPayment::getStatus, 1);
			uw.set(HfpayMerchantPayment::getHfOrderId, str(payResult.get("order_id")));
			uw.set(HfpayMerchantPayment::getHfOrderDate, str(payResult.get("order_date")));
			uw.set(HfpayMerchantPayment::getRespCode, respCode);
			uw.set(HfpayMerchantPayment::getRespDesc, str(payResult.get("resp_desc")));
			hfpayMerchantPaymentMapper.update(null, uw);
		}

		String merchantTradeId = merchantPaymentTradeWriteService.genMerchantTradeId(userId);
		int ts = (int) Instant.now().getEpochSecond();
		MerchantPaymentTrade trade = new MerchantPaymentTrade();
		trade.setMerchantTradeId(merchantTradeId);
		trade.setCompanyId(companyId);
		trade.setRelSceneId(relSceneId);
		trade.setRelSceneName(StringUtils.hasText(relSceneName) ? relSceneName : REL_SCENE_NAME_POPULARIZE);
		trade.setReUserName("");
		trade.setMobile("");
		trade.setAmount(transAmtFen);
		trade.setUserId(userId);
		trade.setOpenId("");
		trade.setPaymentDesc("佣金提现");
		trade.setSpbillCreateIp(StringUtils.hasText(spbillCreateIp) ? spbillCreateIp : "127.0.0.1");
		trade.setPaymentAction("HFPAY");
		trade.setCheckName("NO_CHECK");
		trade.setMchid("");
		trade.setMchAppid("");
		trade.setStatus("NOT_PAY");
		trade.setCurPayFee(Long.toString(transAmtFen));
		trade.setCreateTime(ts);
		trade.setUpdateTime(ts);
		trade.setHfCashType("T1");
		trade.setUserCustId(userCustId);
		trade.setBindCardId(bindCardId);

		merchantPaymentTradeMapper.insert(trade);
		hfpayPopularizeWithdrawEventDispatchPublisher.publishPopularizeWithdrawAfterMerchantTradePersist(merchantTradeId);

		Map<String, Object> ok = new LinkedHashMap<>();
		ok.put("status", "SUCCESS");
		ok.put("error_desc", "SUCCESS");
		return ok;
	}

	private static long longVal(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o == null) {
			return 0L;
		}
		return Long.parseLong(o.toString().trim());
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
	}
}
