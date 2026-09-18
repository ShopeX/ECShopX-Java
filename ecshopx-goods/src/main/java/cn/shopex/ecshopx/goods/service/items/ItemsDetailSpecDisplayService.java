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

package cn.shopex.ecshopx.goods.service.items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ItemsDetailSpecDisplayService {

	@SuppressWarnings("unchecked")
	public void apply(Map<String, Object> detail) {
		if (detail == null) {
			return;
		}
		Object specItemsObj = detail.get("spec_items");
		if (specItemsObj instanceof List<?> rawList) {
			for (Object o : rawList) {
				if (!(o instanceof Map<?, ?> sm)) {
					continue;
				}
				Map<String, Object> specRow = (Map<String, Object>) sm;
				Object ispec = specRow.get("item_spec");
				if (ispec instanceof List<?> isl) {
					List<Map<String, Object>> itemSpec = new ArrayList<>();
					for (Object x : isl) {
						if (x instanceof Map<?, ?> m) {
							itemSpec.add((Map<String, Object>) m);
						}
					}
					itemSpec.sort(Comparator.comparing(ItemsDetailSpecDisplayService::specId).reversed());
					List<String> ids = new ArrayList<>();
					List<String> names = new ArrayList<>();
					for (Map<String, Object> row : itemSpec) {
						Object svid = row.get("spec_value_id");
						if (svid != null) {
							ids.add(svid.toString());
						}
						Object svn = row.get("spec_value_name");
						names.add(svn != null ? svn.toString() : "");
					}
					specRow.put("item_spec", itemSpec);
					specRow.put("custom_spec_id", String.join("-", ids));
					specRow.put("custom_spec_name", String.join("、", names));
				}
			}
		}
		Object descObj = detail.get("item_spec_desc");
		if (descObj instanceof List<?> dl) {
			List<Map<String, Object>> desc = new ArrayList<>();
			for (Object o : dl) {
				if (o instanceof Map<?, ?> m) {
					desc.add((Map<String, Object>) m);
				}
			}
			if (!"supplier_goods".equals(Objects.toString(detail.get("data_source"), ""))) {
				desc.sort(Comparator.comparing((Map<String, Object> a) -> specId(a)).reversed());
			}
			detail.put("item_spec_desc", desc);
		}
	}

	private static long specId(Map<String, Object> m) {
		Object s = m.get("spec_id");
		if (s instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(Objects.toString(s, "0"));
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
