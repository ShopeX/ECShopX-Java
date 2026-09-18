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

package cn.shopex.ecshopx.kaquan.openapi.thirdapi.v2;

import cn.shopex.ecshopx.kaquan.domain.VipGradeOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

class OpenapiMemberCardVipGradeOrderOpenApiFormatSupport {

	private OpenapiMemberCardVipGradeOrderOpenApiFormatSupport() {}

	static Map<String, Object> formatOpenApiVipGradeOrderRow(
			VipGradeOrder entity, String mobilePlain, ObjectMapper objectMapper) {
		if (entity == null) {
			return defaultEmptyOpenApiVipGradeOrderRow();
		}

		String orderIdStr = entity.getOrderId() != null ? String.valueOf(entity.getOrderId()) : "";
		String priceYuan = BigDecimal.valueOf(entity.getPrice() != null ? entity.getPrice() : 0)
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.toPlainString();
		int userId = OpenapiMemberCardVipGradeOpenApiFormatSupport.intOrZero(entity.getUserId());
		String mobile = OpenapiMemberCardVipGradeOpenApiFormatSupport.stringOrEmpty(mobilePlain);
		int vipGradeId = OpenapiMemberCardVipGradeOpenApiFormatSupport.intOrZero(
				entity.getVipGradeId() != null ? entity.getVipGradeId().longValue() : null);
		String lvType = OpenapiMemberCardVipGradeOpenApiFormatSupport.stringOrEmpty(entity.getLvType());
		String title = OpenapiMemberCardVipGradeOpenApiFormatSupport.stringOrEmpty(entity.getTitle());

		Map<String, Object> cardTypeObj = OpenapiMemberCardVipGradeOpenApiFormatSupport.parseJsonObject(
				entity.getCardType(), objectMapper);
		int cardTypeDay = parseCardTypeDay(cardTypeObj.get("day"));

		int rawDiscount = entity.getDiscount() != null ? entity.getDiscount() : 0;
		String discountStr = BigDecimal.valueOf(100 - rawDiscount)
				.divide(BigDecimal.TEN, 1, RoundingMode.HALF_UP)
				.toPlainString();
		String created = OpenapiMemberCardVipGradeOpenApiFormatSupport.formatEpochSeconds(entity.getCreated());
		String updated = OpenapiMemberCardVipGradeOpenApiFormatSupport.formatEpochSeconds(entity.getUpdated());

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("order_id", orderIdStr);
		out.put("price", priceYuan);
		out.put("user_id", userId);
		out.put("mobile", mobile);
		out.put("vip_grade_id", vipGradeId);
		out.put("lv_type", lvType);
		out.put("title", title);
		out.put("card_type", cardTypeDay);
		out.put("discount", discountStr);
		out.put("created", created);
		out.put("updated", updated);
		return out;
	}

	private static Map<String, Object> defaultEmptyOpenApiVipGradeOrderRow() {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("order_id", "");
		out.put("price", "0.00");
		out.put("user_id", 0);
		out.put("mobile", "");
		out.put("vip_grade_id", 0);
		out.put("lv_type", "");
		out.put("title", "");
		out.put("card_type", 0);
		out.put("discount", "10.0");
		out.put("created", "");
		out.put("updated", "");
		return out;
	}

	private static int parseCardTypeDay(Object day) {
		if (day == null) {
			return 0;
		}
		if (day instanceof Number number) {
			return number.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(day).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
