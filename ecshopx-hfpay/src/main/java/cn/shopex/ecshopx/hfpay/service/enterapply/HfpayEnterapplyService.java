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

package cn.shopex.ecshopx.hfpay.service.enterapply;

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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HfpayEnterapplyService {

	private final HfpayEnterapplyMapper enterapplyMapper;
	private final HfpayBankCardMapper bankCardMapper;
	private final HfpayWithdrawSetMapper withdrawSetMapper;
	private final HfpayCashRecordMapper cashRecordMapper;
	private final HfPayPaymentSettingService paymentSettingService;
	private final HfPayAcouJsonPostClient acouJsonPostClient;
	private final HfPayOrderApplyIdGenerator orderApplyIdGenerator;
	private final HfpayDistributorWithdrawEventDispatchPublisher distributorWithdrawEventDispatchPublisher;

	public HfpayEnterapplyService(
			HfpayEnterapplyMapper enterapplyMapper,
			HfpayBankCardMapper bankCardMapper,
			HfpayWithdrawSetMapper withdrawSetMapper,
			HfpayCashRecordMapper cashRecordMapper,
			HfPayPaymentSettingService paymentSettingService,
			HfPayAcouJsonPostClient acouJsonPostClient,
			HfPayOrderApplyIdGenerator orderApplyIdGenerator,
			HfpayDistributorWithdrawEventDispatchPublisher distributorWithdrawEventDispatchPublisher) {
		this.enterapplyMapper = enterapplyMapper;
		this.bankCardMapper = bankCardMapper;
		this.withdrawSetMapper = withdrawSetMapper;
		this.cashRecordMapper = cashRecordMapper;
		this.paymentSettingService = paymentSettingService;
		this.acouJsonPostClient = acouJsonPostClient;
		this.orderApplyIdGenerator = orderApplyIdGenerator;
		this.distributorWithdrawEventDispatchPublisher = distributorWithdrawEventDispatchPublisher;
	}

	/**
	 * 定时任务：扫已开通进件（含企业/个体/个人），满足条件则发起全额 T1 取现并经由统一派发总线触发取现提交（cash01）。
	 */
	public void distributorWithdraw() {
		LambdaQueryWrapper<HfpayEnterapply> base = new LambdaQueryWrapper<>();
		base.eq(HfpayEnterapply::getStatus, "3")
				.in(HfpayEnterapply::getApplyType, "1", "2", "3");

		long count = enterapplyMapper.selectCount(base);
		if (count <= 0L) {
			return;
		}
		if (count > 500L) {
			int pageSize = 50;
			int pageCount = (int) Math.round(count / (double) pageSize);
			for (int page = 1; page <= pageCount; page++) {
				Page<HfpayEnterapply> p = new Page<>(page, pageSize, false);
				List<HfpayEnterapply> list = enterapplyMapper.selectPage(p, base).getRecords();
				addCash(list);
			}
		} else {
			Page<HfpayEnterapply> p = new Page<>(1, 500, false);
			addCash(enterapplyMapper.selectPage(p, base).getRecords());
		}
	}

	private void addCash(List<HfpayEnterapply> data) {
		if (data == null || data.isEmpty()) {
			return;
		}
		for (HfpayEnterapply val : data) {
			Long companyId = val.getCompanyId();
			Long distributorId = val.getDistributorId();
			if (companyId == null || distributorId == null) {
				continue;
			}
			HfpayBankCard bank = bankCardMapper.selectOne(
					new LambdaQueryWrapper<HfpayBankCard>()
							.eq(HfpayBankCard::getDistributorId, distributorId)
							.eq(HfpayBankCard::getIsCash, "1"));
			if (bank == null) {
				continue;
			}
			String userCustId = str(val.getUserCustId());
			String acctId = str(val.getAcctId());
			if (!StringUtils.hasText(userCustId) || !StringUtils.hasText(acctId)) {
				continue;
			}
			String bindCardId = str(bank.getBindCardId());
			if (!StringUtils.hasText(bindCardId)) {
				continue;
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
				continue;
			}
			long balanceFen = parseBalanceFen(qryResult.get("balance"));
			HfpayWithdrawSet withdrawSet = withdrawSetMapper.selectOne(
					new LambdaQueryWrapper<HfpayWithdrawSet>().eq(HfpayWithdrawSet::getCompanyId, companyId));
			if (withdrawSet != null
					&& withdrawSet.getWithdrawMethod() != null
					&& withdrawSet.getWithdrawMethod() == 2) {
				continue;
			}
			long retainFen = 0L;
			if (withdrawSet != null && StringUtils.hasText(withdrawSet.getDistributorMoney())) {
				try {
					retainFen = new BigDecimal(withdrawSet.getDistributorMoney().trim())
							.movePointRight(2)
							.setScale(0, RoundingMode.UNNECESSARY)
							.longValueExact();
				} catch (Exception e) {
					retainFen = 0L;
				}
			}
			if (balanceFen <= 0L) {
				continue;
			}
			if (balanceFen - retainFen < 0L) {
				continue;
			}
			int transAmtFen;
			try {
				transAmtFen = Math.toIntExact(balanceFen);
			} catch (ArithmeticException e) {
				continue;
			}
			String orderId = orderApplyIdGenerator.nextOrderId();
			LocalDateTime now = LocalDateTime.now();
			HfpayCashRecord row = new HfpayCashRecord();
			row.setCompanyId(companyId);
			row.setDistributorId(distributorId);
			row.setOrderId(orderId);
			row.setUserCustId(userCustId);
			row.setTransAmt(transAmtFen);
			row.setCashType("T1");
			row.setBindCardId(bindCardId);
			row.setOperatorId(0L);
			row.setCashStatus(0);
			row.setCreatedAt(now);
			row.setUpdatedAt(now);
			cashRecordMapper.insert(row);
			HfpayCashRecord reloaded = cashRecordMapper.selectById(row.getHfpayCashRecordId());
			if (reloaded == null) {
				continue;
			}
			distributorWithdrawEventDispatchPublisher.publishDistributorWithdrawAfterPersist(
					reloaded.getHfpayCashRecordId(), companyId, distributorId, transAmtFen);
		}
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
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
