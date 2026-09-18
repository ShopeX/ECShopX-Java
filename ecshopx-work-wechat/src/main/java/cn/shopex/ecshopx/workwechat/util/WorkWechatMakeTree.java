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

package cn.shopex.ecshopx.workwechat.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WorkWechatMakeTree {

	private WorkWechatMakeTree() {
	}

	public static List<Map<String, Object>> makeTree(List<Map<String, Object>> arr) {
		if (arr == null) {
			return List.of();
		}
		LinkedHashMap<String, Map<String, Object>> items = new LinkedHashMap<>();
		for (Map<String, Object> row : arr) {
			if (row == null) {
				continue;
			}
			String key = String.valueOf(row.get("id"));
			items.put(key, row);
		}
		List<Map<String, Object>> tree = new ArrayList<>();
		for (Map<String, Object> v : items.values()) {
			Object p = v.get("parentid");
			String parentKey = p == null ? "" : String.valueOf(p);
			Map<String, Object> parent = items.get(parentKey);
			if (parent != null) {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> children =
						(List<Map<String, Object>>) parent.computeIfAbsent("children", k -> new ArrayList<>());
				children.add(v);
			} else {
				tree.add(v);
			}
		}
		return tree;
	}
}
