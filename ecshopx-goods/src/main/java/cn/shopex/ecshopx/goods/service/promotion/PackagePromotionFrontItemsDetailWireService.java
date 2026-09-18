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

package cn.shopex.ecshopx.goods.service.promotion;

import cn.shopex.ecshopx.goods.service.items.GoodsItemsListRowMapper;
import cn.shopex.ecshopx.goods.service.items.ItemsDetailSpecDisplayService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PackagePromotionFrontItemsDetailWireService {

	private static final ObjectMapper OM = new ObjectMapper();

	private static final Set<String> STRIP_KEYS =
			Set.of(
					"promotion_activity",
					"activity_price",
					"tagList",
					"operator_name",
					"distributor_name",
					"item_holder",
					"supplier_name",
					"gross_profit_rate",
					"commission_ratio",
					"itemMainCatName",
					"itemCatName",
					"item_cat_id",
					"data_source");

	private final ItemsDetailSpecDisplayService itemsDetailSpecDisplayService;

	public PackagePromotionFrontItemsDetailWireService(ItemsDetailSpecDisplayService itemsDetailSpecDisplayService) {
		this.itemsDetailSpecDisplayService = itemsDetailSpecDisplayService;
	}

	public void apply(Map<String, Object> detail) {
		if (detail == null || detail.isEmpty()) {
			return;
		}
		itemsDetailSpecDisplayService.apply(detail);
		for (String key : STRIP_KEYS) {
			detail.remove(key);
		}
		detail.put("brand_logo", normalizeBrandLogo(detail.get("brand_logo")));
		detail.put("spec_pics", normalizeSpecPics(detail.get("spec_pics")));
		normalizeRebateConf(detail);
		normalizeNumericScalar(detail, "rebate");
		normalizeNumericScalar(detail, "weight");
		normalizePointNum(detail);
		normalizeCategoryCustomizePageId(detail);
	}

	@SuppressWarnings("unchecked")
	private static void normalizeCategoryCustomizePageId(Map<String, Object> detail) {
		normalizeCategoryCustomizePageIdNodes(detail.get("item_category_main"));
		normalizeCategoryCustomizePageIdNodes(detail.get("item_category_info"));
	}

	@SuppressWarnings("unchecked")
	private static void normalizeCategoryCustomizePageIdNodes(Object nodes) {
		if (!(nodes instanceof List<?> list)) {
			return;
		}
		for (Object o : list) {
			if (!(o instanceof Map<?, ?> raw)) {
				continue;
			}
			Map<String, Object> node = (Map<String, Object>) raw;
			Object cpid = node.get("customize_page_id");
			if (cpid == null) {
				normalizeCategoryCustomizePageIdNodes(node.get("children"));
				continue;
			}
			if (cpid instanceof Number n && n.longValue() == 0L) {
				node.put("customize_page_id", null);
			} else if ("0".equals(cpid.toString().trim())) {
				node.put("customize_page_id", null);
			}
			normalizeCategoryCustomizePageIdNodes(node.get("children"));
		}
	}

	private static void normalizePointNum(Map<String, Object> detail) {
		Object specItems = detail.get("spec_items");
		boolean hasSpecItems = specItems instanceof List<?> l && !l.isEmpty();
		if (hasSpecItems) {
			detail.remove("point_num");
			return;
		}
		Object pn = detail.get("point_num");
		if (pn instanceof Number n && n.intValue() == 0) {
			detail.remove("point_num");
		}
	}

	private static void normalizeRebateConf(Map<String, Object> detail) {
		Object rc = detail.get("rebate_conf");
		if (rc == null) {
			detail.put("rebate_conf", List.of());
		} else if (rc instanceof String s && !StringUtils.hasText(s.trim())) {
			detail.put("rebate_conf", List.of());
		}
	}

	private static void normalizeNumericScalar(Map<String, Object> detail, String key) {
		Object v = detail.get(key);
		if (v instanceof String s && StringUtils.hasText(s)) {
			try {
				if (s.contains(".")) {
					detail.put(key, Double.parseDouble(s.trim()));
				} else {
					detail.put(key, Long.parseLong(s.trim()));
				}
			} catch (NumberFormatException ignored) {
			}
		}
	}

	private static List<Object> normalizeBrandLogo(Object raw) {
		if (raw == null) {
			return new ArrayList<>();
		}
		if (raw instanceof List<?> list) {
			return new ArrayList<>(list);
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			return new ArrayList<>();
		}
		if (s.startsWith("[")) {
			try {
				JsonNode n = OM.readTree(s);
				if (n != null && n.isArray()) {
					List<Object> out = new ArrayList<>();
					for (JsonNode el : n) {
						if (!el.isNull()) {
							out.add(el.isTextual() ? el.asText() : el.toString());
						}
					}
					return out;
				}
			} catch (Exception ignored) {
			}
		}
		return new ArrayList<>(List.of(s));
	}

	private static List<String> normalizeSpecPics(Object raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw instanceof List<?> list) {
			List<String> out = new ArrayList<>();
			for (Object o : list) {
				if (o != null && StringUtils.hasText(o.toString())) {
					out.add(o.toString());
				}
			}
			return out;
		}
		if (raw instanceof String s) {
			Object resolved = GoodsItemsListRowMapper.resolvePicsForListRow(s);
			if (resolved instanceof List<?> l) {
				List<String> out = new ArrayList<>();
				for (Object o : l) {
					if (o != null && StringUtils.hasText(o.toString())) {
						out.add(o.toString());
					}
				}
				return out;
			}
			if (resolved instanceof String one && StringUtils.hasText(one)) {
				return List.of(one);
			}
			return StringUtils.hasText(s.trim()) ? List.of(s.trim()) : List.of();
		}
		return List.of();
	}
}
