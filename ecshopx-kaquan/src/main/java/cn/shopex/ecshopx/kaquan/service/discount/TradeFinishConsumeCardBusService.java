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

package cn.shopex.ecshopx.kaquan.service.discount;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TradeFinishConsumeCardBusService {

	private static final Logger log = LoggerFactory.getLogger(TradeFinishConsumeCardBusService.class);

	private final UserDiscountConsumCardService userDiscountConsumCardService;
	private final ObjectMapper objectMapper;

	public TradeFinishConsumeCardBusService(
			UserDiscountConsumCardService userDiscountConsumCardService, ObjectMapper objectMapper) {
		this.userDiscountConsumCardService = userDiscountConsumCardService;
		this.objectMapper = objectMapper;
	}

	public void onTradeFinishTradeRow(Map<String, Object> tradeFinishPayload) {
		if (tradeFinishPayload == null || tradeFinishPayload.isEmpty()) {
			return;
		}
		Object rawPayType = tradeFinishPayload.get("pay_type");
		String payType = rawPayType == null ? "" : String.valueOf(rawPayType).trim();
		if (!StringUtils.hasText(payType) || "point".equalsIgnoreCase(payType) || "deposit".equalsIgnoreCase(payType)) {
			return;
		}

		long companyId = parsePositiveLong(tradeFinishPayload.get("company_id"));
		long userId = parsePositiveLong(tradeFinishPayload.get("user_id"));
		if (companyId <= 0L || userId <= 0L) {
			log.debug("[tradeFinishConsumeCard] missing company_id or user_id");
			return;
		}

		String transId =
				tradeFinishPayload.get("order_id") == null
						? ""
						: String.valueOf(tradeFinishPayload.get("order_id")).trim();
		String fee = stringFeeFromPayFee(tradeFinishPayload.get("pay_fee"));
		String shopId = tolerantString(tradeFinishPayload.get("shop_id"));

		List<Map<String, Object>> discountRows = parseDiscountInfoStructure(tradeFinishPayload.get("discount_info"));
		if (discountRows == null) {
			return;
		}
		if (discountRows.isEmpty()) {
			return;
		}

		List<String> codes = new ArrayList<>();
		for (Map<String, Object> row : discountRows) {
			expandCouponCodesFromRow(row, codes);
		}
		if (codes.isEmpty()) {
			return;
		}

		for (String couponCode : codes) {
			if (!StringUtils.hasText(couponCode)) {
				continue;
			}
			String trimmed = couponCode.trim();
			try {
				userDiscountConsumCardService.consumeCouponForTradeFinishPayBill(
						companyId, userId, trimmed, transId, fee, shopId);
			} catch (Exception e) {
				log.debug(
						"[tradeFinishConsumeCard] consume failed couponCode={} transId={}: {}",
						trimmed,
						transId,
						e.getMessage(),
						e);
			}
		}
	}

	private void expandCouponCodesFromRow(Map<String, Object> discountRow, List<String> outCodes) {
		Object couponCodes = discountRow.get("coupon_code");
		if (couponCodes instanceof List<?> codes) {
			for (Object codeObj : codes) {
				if (codeObj == null) {
					continue;
				}
				String couponCode = String.valueOf(codeObj).trim();
				if (StringUtils.hasText(couponCode)) {
					outCodes.add(couponCode);
				}
			}
			return;
		}
		if (couponCodes instanceof String || couponCodes instanceof Number) {
			String couponCode = String.valueOf(couponCodes).trim();
			if (StringUtils.hasText(couponCode)) {
				outCodes.add(couponCode);
			}
		}
	}

	/**
	 * Parses {@code discount_info} like {@code NormalOrderCronService#parseDiscountInfo}: JSON string to list of maps;
	 * a single object map is wrapped as a one-element list; already-structured list/map objects are accepted.
	 *
	 * @return {@code null} when JSON decoding fails (caller should stop); empty list when there is nothing to consume.
	 */
	private List<Map<String, Object>> parseDiscountInfoStructure(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof List<?> list) {
			List<Map<String, Object>> out = new ArrayList<>();
			for (Object el : list) {
				if (el instanceof Map<?, ?> m) {
					out.add(asStringKeyMap(m));
				}
			}
			return out;
		}
		if (raw instanceof Map<?, ?> m) {
			return List.of(asStringKeyMap(m));
		}
		if (raw instanceof String s) {
			if (!StringUtils.hasText(s)) {
				return List.of();
			}
			try {
				JsonNode node = objectMapper.readTree(s);
				if (node == null || node.isNull()) {
					return List.of();
				}
				if (node.isArray()) {
					return objectMapper.convertValue(node, new TypeReference<List<Map<String, Object>>>() {});
				}
				if (node.isObject()) {
					Map<String, Object> one = objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {});
					return List.of(one);
				}
				log.debug("[tradeFinishConsumeCard] discount_info not array or object");
				return List.of();
			} catch (Exception e) {
				log.debug("[tradeFinishConsumeCard] parse discount_info failed", e);
				return null;
			}
		}
		log.debug("[tradeFinishConsumeCard] discount_info unsupported type");
		return List.of();
	}

	private static Map<String, Object> asStringKeyMap(Map<?, ?> m) {
		Map<String, Object> mm = new LinkedHashMap<>();
		for (Map.Entry<?, ?> e : m.entrySet()) {
			mm.put(String.valueOf(e.getKey()), e.getValue());
		}
		return mm;
	}

	private static long parsePositiveLong(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String stringFeeFromPayFee(Object payFee) {
		return payFee == null ? "" : String.valueOf(payFee).trim();
	}

	private static String tolerantString(Object raw) {
		if (raw == null) {
			return "";
		}
		return String.valueOf(raw).trim();
	}
}
