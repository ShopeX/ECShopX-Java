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

import cn.shopex.ecshopx.common.discount.DiscountCardItemScopeGateway;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DiscountStandardCardItemIdsService {

	private final DiscountCardItemScopeGateway itemScopeGateway;

	public DiscountStandardCardItemIdsService(DiscountCardItemScopeGateway itemScopeGateway) {
		this.itemScopeGateway = itemScopeGateway;
	}

	public Map<String, Object> apply(Map<String, Object> dataInfo) {
		long companyId = ((Number) dataInfo.get("company_id")).longValue();
		Object ubObj = dataInfo.get("use_bound");
		int useBound = ubObj instanceof Number n ? n.intValue() : DiscountStandardCardSetParamsService.FOR_ALL_ITEMS;
		dataInfo.put("apply_scope", "");
		if (useBound != DiscountStandardCardSetParamsService.FOR_CATEGORY_ITEMS
				&& useBound != DiscountStandardCardSetParamsService.FOR_TAG_ITEMS
				&& useBound != DiscountStandardCardSetParamsService.FOR_BRAND_ITEMS) {
			return dataInfo;
		}
		// 分类/标签/品牌均允许暂无关联商品，后续再挂
		if (useBound == DiscountStandardCardSetParamsService.FOR_CATEGORY_ITEMS) {
			List<Long> categoryIds = new ArrayList<>();
			Object ic = dataInfo.get("item_category");
			if (ic instanceof List<?> l) {
				for (Object o : l) {
					categoryIds.add(DiscountCardParamNormalize.longFromObject(o, 0L));
				}
			}
			dataInfo.put("apply_scope", itemScopeGateway.joinCategoryNames(companyId, categoryIds));
		} else if (useBound == DiscountStandardCardSetParamsService.FOR_TAG_ITEMS) {
			List<Long> tagIds = parseCommaWrappedLongs(DiscountCardParamNormalize.stringVal(dataInfo.get("tag_ids")));
			dataInfo.put("apply_scope", itemScopeGateway.joinTagNames(companyId, tagIds));
		} else if (useBound == DiscountStandardCardSetParamsService.FOR_BRAND_ITEMS) {
			List<Integer> brandIds = new ArrayList<>();
			for (String s : parseCommaWrappedStrings(DiscountCardParamNormalize.stringVal(dataInfo.get("brand_ids")))) {
				try {
					brandIds.add(Integer.parseInt(s));
				} catch (NumberFormatException e) {
					brandIds.add((int) DiscountCardParamNormalize.longFromObject(s, 0L));
				}
			}
			dataInfo.put("apply_scope", itemScopeGateway.joinBrandNames(companyId, brandIds));
		}
		dataInfo.put("rel_item_ids", new ArrayList<Long>());
		return dataInfo;
	}

	private static List<Long> parseCommaWrappedLongs(String raw) {
		List<Long> out = new ArrayList<>();
		for (String s : parseCommaWrappedStrings(raw)) {
			if (StringUtils.hasText(s)) {
				out.add(DiscountCardParamNormalize.longFromObject(s, 0L));
			}
		}
		return out;
	}

	private static List<String> parseCommaWrappedStrings(String raw) {
		if (!StringUtils.hasText(raw)) {
			return List.of();
		}
		String t = raw.trim();
		if (t.startsWith(",")) {
			t = t.substring(1);
		}
		if (t.endsWith(",")) {
			t = t.substring(0, t.length() - 1);
		}
		List<String> out = new ArrayList<>();
		for (String p : t.split(",")) {
			if (StringUtils.hasText(p.trim())) {
				out.add(p.trim());
			}
		}
		return out;
	}
}
