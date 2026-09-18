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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DiscountStandardCardSetParamsService {

	public static final int FOR_ALL_ITEMS = 0;
	public static final int FOR_ASSIGN_ITEMS = 1;
	public static final int FOR_CATEGORY_ITEMS = 2;
	public static final int FOR_TAG_ITEMS = 3;
	public static final int FOR_BRAND_ITEMS = 4;
	public static final int FOR_EXCEPT_ASSIGN_ITEMS = 5;

	private final ObjectMapper objectMapper;

	public DiscountStandardCardSetParamsService(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@SuppressWarnings("unchecked")
	public void apply(Map<String, Object> dataInfo) {
		String couponType = DiscountCardParamNormalize.normalizeCouponType(dataInfo.get("coupon_type"));
		dataInfo.put("coupon_type", couponType);
		if (!dataInfo.containsKey("guide_issue_quantity")) {
			dataInfo.put("guide_issue_quantity", 0);
		} else {
			dataInfo.put("guide_issue_quantity", DiscountCardParamNormalize.parseIntFlexible(dataInfo.get("guide_issue_quantity"), 0));
		}
		String dateType = DiscountCardParamNormalize.stringVal(dataInfo.get("date_type"));
		if ("DATE_TYPE_FIX_TERM".equals(dateType)) {
			dataInfo.put("fixed_term", dataInfo.get("days"));
			dataInfo.put("begin_date", dataInfo.get("begin_time"));
			dataInfo.put("end_date", dataInfo.get("end_time") != null ? dataInfo.get("end_time") : "");
		} else if ("DATE_TYPE_FIX_TIME_RANGE".equals(dateType)) {
			dataInfo.put("fixed_term", 0);
			dataInfo.put("begin_date", dataInfo.get("begin_time"));
			dataInfo.put("end_date", dataInfo.get("end_time"));
		} else if ("DATE_TYPE_FIX_MONTH".equals(dateType)) {
			dataInfo.put("fixed_term", 0);
			dataInfo.put("begin_date", dataInfo.get("begin_time"));
			dataInfo.put("end_date", 0);
		}
		dataInfo.remove("days");
		dataInfo.remove("begin_time");
		dataInfo.remove("end_time");
		if (dataInfo.containsKey("time_limit")) {
			Object tl = dataInfo.get("time_limit");
			boolean empty = tl == null || (tl instanceof List<?> list && list.isEmpty());
			if (empty) {
				List<Map<String, String>> def = new ArrayList<>();
				for (String day : List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SUNDAY", "SATURDAY")) {
					def.add(new LinkedHashMap<>(Map.of("type", day)));
				}
				dataInfo.put("time_limit", def);
			}
		}
		Object relItemIds = dataInfo.get("rel_item_ids");
		String useAllItems = DiscountCardParamNormalize.stringVal(dataInfo.get("use_all_items"));
		if (relItemIds != null && "false".equals(useAllItems)) {
			Object ub = dataInfo.get("use_bound");
			int useBound = ub instanceof Number n ? n.intValue() : DiscountCardParamNormalize.parseIntFlexible(ub, FOR_ASSIGN_ITEMS);
			if (useBound == FOR_EXCEPT_ASSIGN_ITEMS) {
				dataInfo.put("use_bound", FOR_EXCEPT_ASSIGN_ITEMS);
			} else {
				dataInfo.put("use_bound", FOR_ASSIGN_ITEMS);
			}
			List<Long> ids = parseRelItemIds(relItemIds);
			dataInfo.put("tag_ids", "");
			dataInfo.put("brand_ids", "");
			if (ids.isEmpty()) {
				dataInfo.remove("rel_item_ids");
			} else {
				dataInfo.put("rel_item_ids", ids);
			}
		}
		if ("true".equals(useAllItems)) {
			dataInfo.put("use_bound", FOR_ALL_ITEMS);
		}
		dataInfo.put("create_queue", false);
		String filterType = useAllItems;
		Object itemCategory = dataInfo.get("item_category");
		if (itemCategory != null && "category".equals(filterType)) {
			dataInfo.put("create_queue", true);
			dataInfo.put("use_bound", FOR_CATEGORY_ITEMS);
			List<Long> cats = parseJsonLongArray(itemCategory);
			dataInfo.put("item_category", cats);
			dataInfo.put("tag_ids", "");
			dataInfo.put("brand_ids", "");
			if (cats.isEmpty()) {
				throw new ResourceException(KaquanDiscountCardMessages.PLEASE_SELECT_MAIN_CATEGORY);
			}
		}
		if ("tag".equals(filterType)) {
			Object tagIds = dataInfo.get("tag_ids");
			if (tagIds == null || !StringUtils.hasText(String.valueOf(tagIds).trim())) {
				throw new ResourceException(KaquanDiscountCardMessages.PLEASE_SELECT_TAGS);
			}
			List<String> tids = parseJsonStringIdList(tagIds);
			if (tids.isEmpty()) {
				throw new ResourceException(KaquanDiscountCardMessages.PLEASE_SELECT_TAGS);
			}
			dataInfo.put("tag_ids", "," + String.join(",", tids) + ",");
			dataInfo.put("use_bound", FOR_TAG_ITEMS);
			dataInfo.put("brand_ids", "");
			dataInfo.put("create_queue", true);
		}
		if ("brand".equals(filterType)) {
			Object brandIds = dataInfo.get("brand_ids");
			if (brandIds == null || !StringUtils.hasText(String.valueOf(brandIds).trim())) {
				throw new ResourceException(KaquanDiscountCardMessages.PLEASE_SELECT_BRAND);
			}
			List<String> bids = parseJsonStringIdList(brandIds);
			if (bids.isEmpty()) {
				throw new ResourceException(KaquanDiscountCardMessages.PLEASE_SELECT_BRAND);
			}
			dataInfo.put("brand_ids", "," + String.join(",", bids) + ",");
			dataInfo.put("use_bound", FOR_BRAND_ITEMS);
			dataInfo.put("tag_ids", "");
			dataInfo.put("create_queue", true);
		}
		if (dataInfo.containsKey("receive")) {
			Object r = dataInfo.get("receive");
			boolean truthy = Boolean.TRUE.equals(r) || "true".equalsIgnoreCase(String.valueOf(r)) || "1".equals(String.valueOf(r));
			dataInfo.put("receive", truthy ? 1 : 0);
		}
	}

	private List<Long> parseRelItemIds(Object raw) {
		if (raw instanceof List<?> l) {
			List<Long> out = new ArrayList<>();
			for (Object o : l) {
				out.add(DiscountCardParamNormalize.longFromObject(o, 0L));
			}
			return out;
		}
		if (raw instanceof String s) {
			try {
				List<?> arr = objectMapper.readValue(s, List.class);
				List<Long> out = new ArrayList<>();
				for (Object o : arr) {
					out.add(DiscountCardParamNormalize.longFromObject(o, 0L));
				}
				return out;
			} catch (JsonProcessingException e) {
				return List.of();
			}
		}
		return List.of();
	}

	private List<String> parseJsonStringIdList(Object raw) {
		if (raw instanceof List<?> l) {
			List<String> out = new ArrayList<>();
			for (Object o : l) {
				if (o != null) {
					String s = String.valueOf(o).trim();
					if (!s.isEmpty()) {
						out.add(s);
					}
				}
			}
			return out;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				return List.of();
			}
			try {
				return parseJsonStringIdList(objectMapper.readValue(t, List.class));
			} catch (JsonProcessingException e) {
				return List.of();
			}
		}
		return List.of();
	}

	private List<Long> parseJsonLongArray(Object raw) {
		if (raw instanceof List<?> l) {
			List<Long> out = new ArrayList<>();
			for (Object o : l) {
				out.add(DiscountCardParamNormalize.longFromObject(o, 0L));
			}
			return out;
		}
		if (raw instanceof String s) {
			try {
				List<?> arr = objectMapper.readValue(s, List.class);
				List<Long> out = new ArrayList<>();
				for (Object o : arr) {
					out.add(DiscountCardParamNormalize.longFromObject(o, 0L));
				}
				return out;
			} catch (JsonProcessingException e) {
				return List.of();
			}
		}
		return List.of();
	}
}
