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

package cn.shopex.ecshopx.orders.service.memberlink;

import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TradeFinishLinkMemberBusService {

	private final MemberAccountService memberAccountService;

	public TradeFinishLinkMemberBusService(MemberAccountService memberAccountService) {
		this.memberAccountService = memberAccountService;
	}

	public void handleTradeFinishRow(Map<String, Object> tradeRowSnakeCase) {
		if (tradeRowSnakeCase == null || tradeRowSnakeCase.isEmpty()) {
			return;
		}
		Long companyId = parsePositiveLong(first(tradeRowSnakeCase, "company_id", "companyId"));
		Long userId = parsePositiveLong(first(tradeRowSnakeCase, "user_id", "userId"));
		if (companyId == null || userId == null) {
			return;
		}
		Long distributorId = parsePositiveLong(first(tradeRowSnakeCase, "distributor_id", "distributorId"));
		if (distributorId != null) {
			memberAccountService.ensureShopRelMemberIfAbsent(companyId, userId, distributorId, "distributor");
			return;
		}
		Long shopId = parsePositiveLong(first(tradeRowSnakeCase, "shop_id", "shopId"));
		if (shopId != null) {
			memberAccountService.ensureShopRelMemberIfAbsent(companyId, userId, shopId, "shop");
		}
	}

	private static Object first(Map<String, Object> m, String a, String b) {
		Object x = m.get(a);
		if (x != null) {
			return x;
		}
		return m.get(b);
	}

	private static Long parsePositiveLong(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			return v > 0L ? v : null;
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			long v = Long.parseLong(s);
			return v > 0L ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
