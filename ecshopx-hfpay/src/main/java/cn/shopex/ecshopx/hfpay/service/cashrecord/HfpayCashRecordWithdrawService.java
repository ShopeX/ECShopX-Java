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

package cn.shopex.ecshopx.hfpay.service.cashrecord;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.hfpay.domain.HfpayBankCard;
import cn.shopex.ecshopx.hfpay.domain.HfpayCashRecord;
import cn.shopex.ecshopx.hfpay.domain.HfpayEnterapply;
import cn.shopex.ecshopx.hfpay.domain.HfpayWithdrawSet;
import cn.shopex.ecshopx.common.dispatch.HfpayDistributorWithdrawEventDispatchPublisher;
import cn.shopex.ecshopx.hfpay.mapper.HfpayBankCardMapper;
import cn.shopex.ecshopx.hfpay.mapper.HfpayCashRecordMapper;
import cn.shopex.ecshopx.hfpay.mapper.HfpayEnterapplyMapper;
import cn.shopex.ecshopx.hfpay.mapper.HfpayWithdrawSetMapper;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayAcouJsonPostClient;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayOrderApplyIdGenerator;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayPaymentSettingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HfpayCashRecordWithdrawService {

	private final HfpayEnterapplyMapper enterapplyMapper;
	private final HfpayWithdrawSetMapper withdrawSetMapper;
	private final HfpayBankCardMapper bankCardMapper;
	private final HfpayCashRecordMapper cashRecordMapper;
	private final HfPayPaymentSettingService paymentSettingService;
	private final HfPayAcouJsonPostClient acouJsonPostClient;
	private final HfPayOrderApplyIdGenerator orderApplyIdGenerator;
	private final HfpayDistributorWithdrawEventDispatchPublisher distributorWithdrawEventDispatchPublisher;
	private final ZoneId businessZoneId;

	public HfpayCashRecordWithdrawService(
			HfpayEnterapplyMapper enterapplyMapper,
			HfpayWithdrawSetMapper withdrawSetMapper,
			HfpayBankCardMapper bankCardMapper,
			HfpayCashRecordMapper cashRecordMapper,
			HfPayPaymentSettingService paymentSettingService,
			HfPayAcouJsonPostClient acouJsonPostClient,
			HfPayOrderApplyIdGenerator orderApplyIdGenerator,
			HfpayDistributorWithdrawEventDispatchPublisher distributorWithdrawEventDispatchPublisher,
			@org.springframework.beans.factory.annotation.Value("${ecshopx.hfpay.business-zone-id:}")
					String businessZoneIdProp) {
		this.enterapplyMapper = enterapplyMapper;
		this.withdrawSetMapper = withdrawSetMapper;
		this.bankCardMapper = bankCardMapper;
		this.cashRecordMapper = cashRecordMapper;
		this.paymentSettingService = paymentSettingService;
		this.acouJsonPostClient = acouJsonPostClient;
		this.orderApplyIdGenerator = orderApplyIdGenerator;
		this.distributorWithdrawEventDispatchPublisher = distributorWithdrawEventDispatchPublisher;
		this.businessZoneId =
				StringUtils.hasText(businessZoneIdProp) ? ZoneId.of(businessZoneIdProp.trim()) : ZoneId.systemDefault();
	}

	public Map<String, Object> withdraw(long companyId, long operatorId, Map<String, Object> merged) {
		assertWithdrawTimeWindow();

		long distributorId = parsePositiveDistributorId(merged.get("distributor_id"));
		String amountRaw = str(merged.get("withdrawal_amount"));
		if (!StringUtils.hasText(amountRaw)) {
			throw new ResourceException("请输入提现金额");
		}
		String trimmedAmt = amountRaw.trim();
		BigDecimal yuan;
		try {
			yuan = new BigDecimal(trimmedAmt);
		} catch (NumberFormatException e) {
			throw new ResourceException("提现金额不低于0.01元的数字");
		}
		if (yuan.signum() < 0) {
			throw new ResourceException("提现金额不低于0.01元的数字");
		}
		try {
			BigDecimal fenCheck = yuan.multiply(new BigDecimal("100")).setScale(0, RoundingMode.UNNECESSARY);
			if (fenCheck.compareTo(BigDecimal.ONE) < 0) {
				throw new ResourceException("提现金额不低于0.01元的数字");
			}
		} catch (ArithmeticException e) {
			throw new ResourceException("提现金额不低于0.01元的数字");
		}
		int withdrawalAmountFen = yuan.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).intValueExact();

		HfpayEnterapply enter = loadApprovedEnterapply(companyId, distributorId);
		String userCustId = str(enter.getUserCustId());
		String acctId = str(enter.getAcctId());
		if (!StringUtils.hasText(userCustId) || !StringUtils.hasText(acctId)) {
			throw new ResourceException("未查询到汇付账户");
		}

		HfpayWithdrawSet withdrawSet = loadManualWithdrawSet(companyId);

		HfpayBankCard bank = loadCashBankCard(distributorId);
		String bindCardId = str(bank.getBindCardId());
		if (!StringUtils.hasText(bindCardId)) {
			throw new ResourceException("未绑定银行卡");
		}

		Map<String, Object> setting = paymentSettingService.loadForCompany(companyId);
		String merCustId = String.valueOf(setting.get("mer_cust_id")).trim();

		LinkedHashMap<String, Object> qryPayload = new LinkedHashMap<>();
		qryPayload.put("version", 10);
		qryPayload.put("mer_cust_id", merCustId);
		qryPayload.put("user_cust_id", userCustId);
		qryPayload.put("acct_id", acctId);

		Map<String, Object> qryResult = acouJsonPostClient.qry001(setting, qryPayload);
		String qryCode = qryResult.get("resp_code") == null ? "" : String.valueOf(qryResult.get("resp_code")).trim();
		if (!"C00000".equals(qryCode)) {
			Object desc = qryResult.get("resp_desc");
			throw new ResourceException(desc == null ? "" : String.valueOf(desc));
		}

		long balanceFen = parseBalanceFen(qryResult.get("balance"));
		if (balanceFen <= 0L || withdrawalAmountFen > balanceFen) {
			throw new ResourceException("可提现余额不足");
		}

		long retainFen =
				new BigDecimal(withdrawSet.getDistributorMoney() == null ? "0" : withdrawSet.getDistributorMoney().trim())
						.movePointRight(2)
						.longValue();
		if (retainFen > 0L && (balanceFen - retainFen) < 0L) {
			throw new ResourceException("可提现余额未满足提现限额");
		}

		String orderId = orderApplyIdGenerator.nextOrderId();

		LocalDateTime now = LocalDateTime.now();
		HfpayCashRecord row = new HfpayCashRecord();
		row.setCompanyId(companyId);
		row.setDistributorId(distributorId);
		row.setOrderId(orderId);
		row.setUserCustId(userCustId);
		row.setTransAmt(withdrawalAmountFen);
		row.setCashType("T1");
		row.setBindCardId(bindCardId);
		row.setOperatorId(operatorId);
		row.setCashStatus(0);
		row.setCreatedAt(now);
		row.setUpdatedAt(now);

		cashRecordMapper.insert(row);
		HfpayCashRecord reloaded = cashRecordMapper.selectById(row.getHfpayCashRecordId());
		if (reloaded == null) {
			throw new ResourceException("提现记录创建失败");
		}

		Map<String, Object> snapshot = HfpayCashRecordRowConverter.columnNamesData(reloaded);

		distributorWithdrawEventDispatchPublisher.publishDistributorWithdrawAfterPersist(
				reloaded.getHfpayCashRecordId(), companyId, distributorId, withdrawalAmountFen);

		return snapshot;
	}

	private void assertWithdrawTimeWindow() {
		ZonedDateTime now = ZonedDateTime.now(businessZoneId);
		ZonedDateTime openAt = now.toLocalDate().atStartOfDay(businessZoneId).plusHours(10);
		if (now.isBefore(openAt)) {
			throw new ResourceException("提现操作请在10:00:00-23:59:59进行");
		}
	}

	private static long parsePositiveDistributorId(Object raw) {
		if (raw == null) {
			throw new ResourceException("请选择店铺");
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			throw new ResourceException("请选择店铺");
		}
		try {
			long v = Long.parseLong(s);
			if (v <= 0L) {
				throw new ResourceException("请选择店铺");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new ResourceException("请选择店铺");
		}
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private HfpayEnterapply loadApprovedEnterapply(long companyId, long distributorId) {
		LambdaQueryWrapper<HfpayEnterapply> w = new LambdaQueryWrapper<>();
		w.eq(HfpayEnterapply::getCompanyId, companyId)
				.eq(HfpayEnterapply::getDistributorId, distributorId)
				.eq(HfpayEnterapply::getStatus, "3")
				.in(HfpayEnterapply::getApplyType, "1", "2");
		HfpayEnterapply enter = enterapplyMapper.selectOne(w);
		if (enter == null) {
			throw new ResourceException("未查询到汇付账户");
		}
		return enter;
	}

	private HfpayWithdrawSet loadManualWithdrawSet(long companyId) {
		LambdaQueryWrapper<HfpayWithdrawSet> w = new LambdaQueryWrapper<>();
		w.eq(HfpayWithdrawSet::getCompanyId, companyId);
		HfpayWithdrawSet set = withdrawSetMapper.selectOne(w);
		if (set == null || set.getWithdrawMethod() == null || set.getWithdrawMethod() != 2) {
			throw new ResourceException("未开启手动提现");
		}
		return set;
	}

	private HfpayBankCard loadCashBankCard(long distributorId) {
		LambdaQueryWrapper<HfpayBankCard> w = new LambdaQueryWrapper<>();
		w.eq(HfpayBankCard::getDistributorId, distributorId).eq(HfpayBankCard::getIsCash, "1");
		HfpayBankCard card = bankCardMapper.selectOne(w);
		if (card == null) {
			throw new ResourceException("未绑定银行卡");
		}
		return card;
	}

	private static long parseBalanceFen(Object balanceRaw) {
		if (balanceRaw == null) {
			return 0L;
		}
		String s = String.valueOf(balanceRaw).trim();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			BigDecimal yuan = new BigDecimal(s);
			return yuan.multiply(new BigDecimal("100")).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
		} catch (Exception e) {
			return 0L;
		}
	}
}
