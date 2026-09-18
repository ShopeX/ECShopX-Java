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

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DiscountNewGiftCardSetParamsApplier {

	public static final int USE_BOUND_ASSIGN_ITEMS = 1;
	public static final int USE_BOUND_ALL_ITEMS = 0;

	private final ObjectMapper objectMapper;

	public DiscountNewGiftCardSetParamsApplier(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public void apply(Map<String, Object> params, boolean isCreate) {
		params.put("coupon_type", DiscountCardParamNormalize.normalizeCouponType(params.get("coupon_type")));
		if (!params.containsKey("guide_issue_quantity")) {
			params.put("guide_issue_quantity", 0);
		} else {
			params.put("guide_issue_quantity", DiscountCardParamNormalize.parseIntFlexible(params.get("guide_issue_quantity"), 0));
		}
		double hour = 0;
		boolean hourKnown = false;
		String dateType = DiscountCardParamNormalize.stringVal(params.get("date_type"));
		if (DiscountCardActionValidationService.DATE_TYPE_LONG.equals(dateType)) {
			params.put("begin_date", params.get("begin_time"));
			params.put("fixed_term", params.get("days"));
			params.put("end_date", 0);
			int ft = DiscountCardParamNormalize.parseIntFlexible(params.get("fixed_term"), 0);
			hour = ft * 24.0;
			hourKnown = true;
		} else if (DiscountCardActionValidationService.DATE_TYPE_SHORT.equals(dateType)) {
			int begin = DiscountCardParamNormalize.parseIntFlexible(params.get("begin_time"), 0);
			int end = DiscountCardParamNormalize.parseIntFlexible(params.get("end_time"), 0);
			params.put("begin_date", begin);
			params.put("end_date", end);
			params.put("fixed_term", 0);
			hour = (end - begin) / 3600.0;
			hourKnown = true;
		}
		int lock = DiscountCardParamNormalize.parseIntFlexible(params.get("lock_time"), 0);
		if (params.containsKey("lock_time") && hourKnown && lock > hour) {
			throw new ResourceException(KaquanDiscountCardMessages.LOCK_TIME_CANNOT_EXCEED_USAGE_TIME);
		}
		if (params.containsKey("receive")) {
			Object r = params.get("receive");
			boolean t = Boolean.TRUE.equals(r) || "true".equalsIgnoreCase(String.valueOf(r)) || "1".equals(String.valueOf(r));
			params.put("receive", t ? 1 : 0);
		} else if (isCreate) {
			params.put("receive", 0);
		}
		params.remove("days");
		params.remove("begin_time");
		params.remove("end_time");
		Object distRaw = params.get("distributor_ids");
		if (distRaw instanceof List<?> dl) {
			if (!dl.isEmpty()) {
				List<Integer> distributorIds = parseJsonIntList(distRaw);
				if (!distributorIds.isEmpty()) {
					params.put("distributor_id", distributorIds);
					params.remove("distributor_ids");
					params.put("use_all_shops", "false");
				}
			}
		} else if (distRaw != null && StringUtils.hasText(String.valueOf(distRaw).trim())) {
			List<Integer> distributorIds = parseJsonIntList(distRaw);
			if (!distributorIds.isEmpty()) {
				params.put("distributor_id", distributorIds);
				params.remove("distributor_ids");
				params.put("use_all_shops", "false");
			} else {
				params.remove("distributor_ids");
				params.put("use_all_shops", "true");
			}
		}
		Object ut = params.get("user_tag_ids");
		if (ut != null && StringUtils.hasText(String.valueOf(ut).trim())) {
			params.put("user_tag_ids", parseJsonListSafe(ut));
		}
		Object g = params.get("grade_ids");
		if (g != null && StringUtils.hasText(String.valueOf(g).trim())) {
			params.put("grade_ids", parseJsonListSafe(g));
		}
		Object vg = params.get("vip_grade_ids");
		if (vg != null && StringUtils.hasText(String.valueOf(vg).trim())) {
			params.put("vip_grade_ids", parseJsonListSafe(vg));
		}
		Object it = params.get("items");
		if (it != null && StringUtils.hasText(String.valueOf(it).trim())) {
			List<Map<String, Object>> parsedItems = parseItemsJson(it);
			if (parsedItems != null && !parsedItems.isEmpty()) {
				params.put("items", parsedItems);
				params.put("use_bound", USE_BOUND_ASSIGN_ITEMS);
			} else {
				params.put("items", List.of(Map.of("id", 0)));
				params.put("use_bound", USE_BOUND_ALL_ITEMS);
			}
		} else if (isCreate) {
			params.put("items", List.of(Map.of("id", 0)));
			params.put("use_bound", USE_BOUND_ALL_ITEMS);
		}
		int gl = DiscountCardParamNormalize.parseIntFlexible(params.get("get_limit"), 1);
		if (params.containsKey("get_limit") && gl <= 0) {
			params.put("get_limit", 1);
		}
		if ("forbid".equals(DiscountCardParamNormalize.stringVal(params.get("use_all_items")))) {
			params.put("use_all_items", "false");
		}
	}

	private List<Integer> parseJsonIntList(Object raw) {
		try {
			if (raw instanceof List<?> l) {
				List<Integer> out = new ArrayList<>();
				for (Object o : l) {
					out.add((int) DiscountCardParamNormalize.longFromObject(o, 0L));
				}
				return out;
			}
			List<Object> arr = objectMapper.readValue(String.valueOf(raw), new TypeReference<>() {});
			List<Integer> out = new ArrayList<>();
			for (Object o : arr) {
				out.add((int) DiscountCardParamNormalize.longFromObject(o, 0L));
			}
			return out;
		} catch (Exception e) {
			return List.of();
		}
	}

	private List<Object> parseJsonListSafe(Object raw) {
		try {
			if (raw instanceof List<?> l) {
				return new ArrayList<>(l);
			}
			return objectMapper.readValue(String.valueOf(raw), new TypeReference<>() {});
		} catch (Exception e) {
			return List.of();
		}
	}

	private List<Map<String, Object>> parseItemsJson(Object raw) {
		try {
			if (raw instanceof List<?> l) {
				List<Map<String, Object>> out = new ArrayList<>();
				for (Object o : l) {
					if (o instanceof Map<?, ?> m) {
						Map<String, Object> mm = new HashMap<>();
						for (Map.Entry<?, ?> e : m.entrySet()) {
							mm.put(String.valueOf(e.getKey()), e.getValue());
						}
						out.add(mm);
					}
				}
				return out;
			}
			return objectMapper.readValue(String.valueOf(raw), new TypeReference<>() {});
		} catch (Exception e) {
			return null;
		}
	}
}
