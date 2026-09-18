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
import cn.shopex.ecshopx.deposit.domain.DepositTrade;
import cn.shopex.ecshopx.deposit.mapper.DepositTradeMapper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class DepositTradeConsumeService {

	private static final String BALANCE_SHORT = "余额不足，支付失败";

	private final DepositTradeMapper depositTradeMapper;
	private final DepositTradeIdGenerator depositTradeIdGenerator;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final UserDepositBalanceReadService userDepositBalanceReadService;
	private final UserDepositBalanceMutationService userDepositBalanceMutationService;
	private final StringRedisTemplate redis;

	public DepositTradeConsumeService(
			DepositTradeMapper depositTradeMapper,
			DepositTradeIdGenerator depositTradeIdGenerator,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			UserDepositBalanceReadService userDepositBalanceReadService,
			UserDepositBalanceMutationService userDepositBalanceMutationService,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redis) {
		this.depositTradeMapper = depositTradeMapper;
		this.depositTradeIdGenerator = depositTradeIdGenerator;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.userDepositBalanceReadService = userDepositBalanceReadService;
		this.userDepositBalanceMutationService = userDepositBalanceMutationService;
		this.redis = redis;
	}

	public void consume(Map<String, Object> consumeData) {
		try {
			long companyId = parseRequiredLong(consumeData.get("company_id"));
			long userId = parseRequiredLong(consumeData.get("user_id"));
			long moneyFen = parseRequiredLong(consumeData.get("money"));
			String memberCardCode = nullToEmpty(consumeData.get("member_card_code"));
			String shopId = nullToEmpty(consumeData.get("shop_id"));
			String shopName = nullToEmpty(consumeData.get("shop_name"));
			String mobilePlain = nullToEmpty(consumeData.get("mobile"));
			String openId = nullToEmpty(consumeData.get("open_id"));
			String detail = consumeData.get("detail") == null ? "购买商品" : consumeData.get("detail").toString();
			String curPayFee = nullToEmpty(consumeData.get("cur_pay_fee"));

			String depositTradeId = depositTradeIdGenerator.nextDepositTradeId(userId);
			long nowSec = System.currentTimeMillis() / 1000L;
			String mobileEnc = sensitiveFieldEncryptor.encrypt(mobilePlain);

			DepositTrade row = new DepositTrade();
			row.setDepositTradeId(depositTradeId);
			row.setCompanyId(String.valueOf(companyId));
			row.setMemberCardCode(memberCardCode.isEmpty() ? null : memberCardCode);
			row.setShopId(shopId.isEmpty() ? "0" : shopId);
			row.setShopName(shopName.isEmpty() ? null : shopName);
			row.setUserId(String.valueOf(userId));
			row.setMobile(mobileEnc);
			row.setOpenId(openId.isEmpty() ? null : openId);
			row.setMoney(String.valueOf(moneyFen));
			row.setTradeType("consume");
			row.setTradeStatus("SUCCESS");
			row.setDetail(detail.isEmpty() ? null : detail);
			row.setTimeStart(String.valueOf(nowSec));
			row.setTimeExpire(String.valueOf(nowSec));
			row.setCurPayFee(curPayFee.isEmpty() ? String.valueOf(moneyFen) : curPayFee);

			depositTradeMapper.insert(row);

			long balance = userDepositBalanceReadService.getUserDepositTotal(companyId, userId);
			if (moneyFen > balance) {
				throw new ResourceException(BALANCE_SHORT);
			}
			userDepositBalanceMutationService.applyUserDepositTotalDelta(companyId, userId, -moneyFen);

			String companyField = String.valueOf(companyId);
			redis.opsForHash().increment("shopDepositTotal", companyField, -moneyFen);
			String dayKey = "dayConsumeTotal" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
			redis.opsForHash().increment(dayKey, companyField, moneyFen);
		} catch (ResourceException e) {
			if (BALANCE_SHORT.equals(e.getMessage())) {
				throw e;
			}
			throw new ResourceException("支付失败");
		} catch (Exception e) {
			throw new ResourceException("支付失败");
		}
	}

	private static long parseRequiredLong(Object o) {
		if (o == null) {
			throw new IllegalArgumentException("Required numeric value is null");
		}
		return Long.parseLong(o.toString().trim());
	}

	private static String nullToEmpty(Object o) {
		return o == null ? "" : o.toString();
	}
}
