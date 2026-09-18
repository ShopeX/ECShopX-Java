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

package cn.shopex.ecshopx.kaquan.service.vipgrade;

import cn.shopex.ecshopx.common.order.front.VipGradeWxappOrderCreatePort;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class VipGradeWxappOrderCreatePortImpl implements VipGradeWxappOrderCreatePort {

	private final VipGradeOrderBuyCreateService vipGradeOrderBuyCreateService;

	public VipGradeWxappOrderCreatePortImpl(VipGradeOrderBuyCreateService vipGradeOrderBuyCreateService) {
		this.vipGradeOrderBuyCreateService = vipGradeOrderBuyCreateService;
	}

	@Override
	public Map<String, Object> createOrder(Map<String, Object> params) {
		return buildTempOrCreatePayload(params);
	}

	@Override
	public Map<String, Object> getOrderTempInfo(Map<String, Object> params) {
		return buildTempOrCreatePayload(params);
	}

	private Map<String, Object> buildTempOrCreatePayload(Map<String, Object> params) {
		long companyId = longVal(params.get("company_id"));
		long userId = longVal(params.get("user_id"));
		String mobile = stringVal(params.get("mobile"));
		String vipGradeId = firstText(params.get("vip_grade_id"), params.get("grade_id"));
		String cardTypeName = firstText(params.get("card_type_name"), params.get("name"), params.get("card_type"));
		long distributorId = longVal(params.get("distributor_id"));
		Map<String, Object> merged =
				vipGradeOrderBuyCreateService.createDataForBuy(companyId, userId, mobile, vipGradeId, cardTypeName, distributorId);
		return normalizeForWxappPayment(merged);
	}

	private static Map<String, Object> normalizeForWxappPayment(Map<String, Object> merged) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>(merged);
		if (!out.containsKey("create_time") && out.containsKey("created")) {
			out.put("create_time", out.get("created"));
		}
		if (!out.containsKey("total_fee")) {
			if (out.containsKey("price")) {
				out.put("total_fee", out.get("price"));
			}
		}
		out.putIfAbsent("discount_fee", 0);
		out.putIfAbsent("discount_info", Map.of());
		out.putIfAbsent("fee_rate", out.get("fee_rate"));
		out.putIfAbsent("fee_type", out.get("fee_type"));
		out.putIfAbsent("fee_symbol", out.get("fee_symbol"));
		out.putIfAbsent("team_id", null);
		out.putIfAbsent("shop_id", out.getOrDefault("shop_id", 0L));
		out.putIfAbsent("point", 0);
		out.putIfAbsent("prescription_status", 0);
		out.putIfAbsent("title", out.get("title"));
		out.putIfAbsent("order_id", out.get("order_id"));
		return out;
	}

	private static String firstText(Object... candidates) {
		if (candidates == null) {
			return "";
		}
		for (Object c : candidates) {
			String s = stringVal(c);
			if (StringUtils.hasText(s)) {
				return s.trim();
			}
		}
		return "";
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
