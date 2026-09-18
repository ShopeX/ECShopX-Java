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

package cn.shopex.ecshopx.deposit.service;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.CurrencyExchangeRate;
import cn.shopex.ecshopx.companys.service.currency.CompanyDefaultCurrencyService;
import cn.shopex.ecshopx.deposit.domain.DepositTrade;
import cn.shopex.ecshopx.deposit.mapper.DepositTradeMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class DepositAdminRechargeService {

	private static final Logger log = LoggerFactory.getLogger(DepositAdminRechargeService.class);

	private final MemberAccountService memberAccountService;
	private final CompanyDefaultCurrencyService companyDefaultCurrencyService;
	private final DepositTradeIdGenerator depositTradeIdGenerator;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final DepositTradeMapper depositTradeMapper;
	private final UserDepositBalanceMutationService userDepositBalanceMutationService;
	private final TransactionTemplate transactionTemplate;

	public DepositAdminRechargeService(
			MemberAccountService memberAccountService,
			CompanyDefaultCurrencyService companyDefaultCurrencyService,
			DepositTradeIdGenerator depositTradeIdGenerator,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			DepositTradeMapper depositTradeMapper,
			UserDepositBalanceMutationService userDepositBalanceMutationService,
			PlatformTransactionManager platformTransactionManager) {
		this.memberAccountService = memberAccountService;
		this.companyDefaultCurrencyService = companyDefaultCurrencyService;
		this.depositTradeIdGenerator = depositTradeIdGenerator;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.depositTradeMapper = depositTradeMapper;
		this.userDepositBalanceMutationService = userDepositBalanceMutationService;
		this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
	}

	public void recharge(long companyId, Map<String, Object> merged) {
		long userId = parseUserId(merged);
		long moneyFen = parseMoneyFen(merged);
		Map<String, Object> member = memberAccountService.getMemberInfo(userId, companyId);
		if (member == null || member.isEmpty()) {
			throw new ResourceException("会员不存在");
		}
		Object mobileObj = member.get("mobile");
		String mobileRaw = mobileObj == null ? "" : mobileObj.toString();
		String mobileEnc = sensitiveFieldEncryptor.encrypt(mobileRaw);
		try {
			transactionTemplate.executeWithoutResult(
					status -> adminRechargeCore(companyId, userId, moneyFen, member, mobileEnc));
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.debug("后台充值预存款失败：{}", e.getMessage(), e);
			throw new ResourceException(e.getMessage() != null ? e.getMessage() : "后台充值失败");
		}
	}

	private void adminRechargeCore(
			long companyId,
			long userId,
			long moneyFen,
			Map<String, Object> member,
			String mobileEnc) {
		String tradeId = depositTradeIdGenerator.nextDepositTradeId(userId);
		CurrencyExchangeRate cur = companyDefaultCurrencyService.getCur(companyId);
		Double rate = cur.getRate();
		boolean multiBranch = rate != null && Double.compare(rate, 0.0) != 0;

		DepositTrade row = new DepositTrade();
		row.setDepositTradeId(tradeId);
		row.setCompanyId(String.valueOf(companyId));
		Object ucc = member.get("user_card_code");
		row.setMemberCardCode(ucc == null ? "" : ucc.toString());
		row.setUserId(String.valueOf(userId));
		row.setMobile(mobileEnc);
		row.setTradeType("recharge_admin");
		row.setTradeStatus("SUCCESS");
		String epoch = String.valueOf(Instant.now().getEpochSecond());
		row.setTimeStart(epoch);
		row.setTimeExpire(epoch);
		row.setDetail(
				"后台充值"
						+ BigDecimal.valueOf(moneyFen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).toPlainString()
						+ "元");

		if (multiBranch) {
			double curFeeRate =
					BigDecimal.valueOf(rate).setScale(4, RoundingMode.HALF_UP).doubleValue();
			row.setCurPayFee(String.valueOf(moneyFen));
			row.setCurFeeSymbol(cur.getSymbol() != null ? cur.getSymbol() : "");
			row.setCurFeeType(cur.getCurrency() != null ? cur.getCurrency() : "CNY");
			row.setCurFeeRate(curFeeRate);
			row.setMoney(String.valueOf(Math.round(moneyFen * curFeeRate)));
		} else {
			row.setMoney(String.valueOf(moneyFen));
			row.setFeeType(null);
			row.setCurPayFee(null);
			row.setCurFeeType(null);
			row.setCurFeeRate(null);
			row.setCurFeeSymbol(null);
		}

		depositTradeMapper.insert(row);
		DepositTrade verify = depositTradeMapper.selectById(tradeId);
		if (verify == null) {
			throw new ResourceException("添加失败");
		}
		userDepositBalanceMutationService.addUserDepositTotal(companyId, userId, moneyFen);
	}

	private static long parseUserId(Map<String, Object> merged) {
		Object o = merged.get("user_id");
		if (o == null) {
			throw new ResourceException("用户ID必填");
		}
		String t = o.toString().trim();
		if (t.isEmpty()) {
			throw new ResourceException("用户ID必填");
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new ResourceException("用户ID必填");
		}
	}

	private static long parseMoneyFen(Map<String, Object> merged) {
		Object o = merged.get("money");
		if (o == null) {
			throw new ResourceException("充值金额必填");
		}
		String t = o.toString().trim();
		if (t.isEmpty()) {
			throw new ResourceException("充值金额必填");
		}
		long fen;
		try {
			fen = new BigDecimal(t).longValue();
		} catch (NumberFormatException e) {
			throw new ResourceException("充值金额必填");
		}
		if (fen < 1) {
			throw new ResourceException("至少充值1分钱");
		}
		return fen;
	}
}
