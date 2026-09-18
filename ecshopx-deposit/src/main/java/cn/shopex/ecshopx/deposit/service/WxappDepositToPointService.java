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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.point.service.PointMemberBalanceReadService;
import cn.shopex.ecshopx.point.service.PointMemberDepositExchangeWriteService;
import cn.shopex.ecshopx.point.service.PointMemberMoneyToPointService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WxappDepositToPointService {

	private static final Logger log = LoggerFactory.getLogger(WxappDepositToPointService.class);

	private final UserDepositBalanceReadService userDepositBalanceReadService;
	private final PointMemberMoneyToPointService pointMemberMoneyToPointService;
	private final PointMemberDepositExchangeWriteService pointMemberDepositExchangeWriteService;
	private final DepositTradeConsumeService depositTradeConsumeService;
	private final PointMemberBalanceReadService pointMemberBalanceReadService;

	public WxappDepositToPointService(
			UserDepositBalanceReadService userDepositBalanceReadService,
			PointMemberMoneyToPointService pointMemberMoneyToPointService,
			PointMemberDepositExchangeWriteService pointMemberDepositExchangeWriteService,
			DepositTradeConsumeService depositTradeConsumeService,
			PointMemberBalanceReadService pointMemberBalanceReadService) {
		this.userDepositBalanceReadService = userDepositBalanceReadService;
		this.pointMemberMoneyToPointService = pointMemberMoneyToPointService;
		this.pointMemberDepositExchangeWriteService = pointMemberDepositExchangeWriteService;
		this.depositTradeConsumeService = depositTradeConsumeService;
		this.pointMemberBalanceReadService = pointMemberBalanceReadService;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> execute(Map<String, Object> claims, long moneyFen) {
		long companyId = parseCompanyIdFromClaims(claims);
		long userId = parseUserIdFromClaims(claims);

		long depositBefore = userDepositBalanceReadService.getUserDepositTotal(companyId, userId);
		if (moneyFen > depositBefore) {
			throw new ResourceException("储值金额不足请充值");
		}

		long point = pointMemberMoneyToPointService.moneyToPoint(companyId, moneyFen);

		String yuan =
				BigDecimal.valueOf(moneyFen)
						.divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN)
						.stripTrailingZeros()
						.toPlainString();
		log.info(
				"point:member-{}-{}|{}储值兑换积分{}",
				companyId,
				companyId,
				yuan,
				point);

		pointMemberDepositExchangeWriteService.addPointForDepositExchange(userId, companyId, point, moneyFen);

		Map<String, Object> consumeData = new LinkedHashMap<>();
		consumeData.put("company_id", companyId);
		consumeData.put("user_id", userId);
		consumeData.put("money", moneyFen);
		Object mcc = claims.get("user_card_code");
		consumeData.put("member_card_code", mcc == null ? "" : mcc.toString());
		consumeData.put("shop_id", claims.get("shop_id") == null ? "" : claims.get("shop_id").toString());
		consumeData.put("shop_name", claims.get("shop_name") == null ? "" : claims.get("shop_name").toString());
		consumeData.put("mobile", claims.get("mobile") == null ? "" : claims.get("mobile").toString());
		consumeData.put("open_id", claims.get("open_id") == null ? "" : claims.get("open_id").toString());
		consumeData.put("detail", "购买商品");
		consumeData.put(
				"cur_pay_fee",
				claims.get("pay_fee") == null ? "" : claims.get("pay_fee").toString());

		depositTradeConsumeService.consume(consumeData);

		long depositAfter = userDepositBalanceReadService.getUserDepositTotal(companyId, userId);
		long pointAfter = pointMemberBalanceReadService.getPointBalance(companyId, userId);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("company_id", String.valueOf(companyId));
		out.put("user_id", String.valueOf(userId));
		out.put("deposit", String.valueOf(depositAfter));
		out.put("point", String.valueOf(pointAfter));
		return out;
	}

	private static long parseCompanyIdFromClaims(Map<String, Object> claims) {
		Object o = claims.get("company_id");
		if (o == null) {
			throw new ResourceException("商户ID必填");
		}
		String t = o.toString().trim();
		if (t.isEmpty()) {
			throw new ResourceException("商户ID必填");
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			throw new ResourceException("商户ID必填");
		}
	}

	private static long parseUserIdFromClaims(Map<String, Object> claims) {
		Object o = claims.get("user_id");
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
}
