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

package cn.shopex.ecshopx.goods.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ItemsCategoryTreeService {

	/**
	 * 将平面行转为树；{@code pid}、{@code treeStartLevel} 由查询字符串按前缀数字规则解析（非严格 {@link Integer#parseInt}）。
	 */
	public List<Map<String, Object>> getTree(List<Map<String, Object>> flatRows, long pid, int treeStartLevel,
			boolean isShowChildren) {
		Map<Long, List<Map<String, Object>>> byParent = new HashMap<>();
		for (Map<String, Object> item : flatRows) {
			long p = toLongKey(item.get("parent_id"));
			byParent.computeIfAbsent(p, k -> new ArrayList<>()).add(item);
		}
		return buildTree(byParent, pid, treeStartLevel, isShowChildren);
	}

	static int parseLeadingIntFromString(String raw) {
		if (raw == null) {
			return 0;
		}
		int i = 0;
		int n = raw.length();
		while (i < n && Character.isWhitespace(raw.charAt(i))) {
			i++;
		}
		if (i >= n) {
			return 0;
		}
		int sign = 1;
		char c0 = raw.charAt(i);
		if (c0 == '+' || c0 == '-') {
			if (c0 == '-') {
				sign = -1;
			}
			i++;
		}
		if (i >= n) {
			return 0;
		}
		long acc = 0;
		boolean any = false;
		while (i < n && Character.isDigit(raw.charAt(i))) {
			any = true;
			acc = acc * 10 + (raw.charAt(i) - '0');
			if (acc > Integer.MAX_VALUE) {
				return sign > 0 ? Integer.MAX_VALUE : Integer.MIN_VALUE;
			}
			i++;
		}
		if (!any) {
			return 0;
		}
		return (int) (sign * acc);
	}

	private List<Map<String, Object>> buildTree(Map<Long, List<Map<String, Object>>> byParent, long pid, int level,
			boolean isShowChildren) {
		List<Map<String, Object>> tree = new ArrayList<>();
		List<Map<String, Object>> siblings = byParent.get(pid);
		if (siblings == null) {
			return tree;
		}
		for (Map<String, Object> item : siblings) {
			Map<String, Object> node = new LinkedOrderMap(item);
			node.put("level", level);
			node.put("children", new ArrayList<Map<String, Object>>());
			List<Map<String, Object>> children = buildTree(byParent, toLongKey(item.get("category_id")), level + 1,
					isShowChildren);
			node.put("children", children);
			int catLevel = toIntOrZero(node.get("category_level"));
			if (catLevel == 3) {
				node.remove("children");
			} else if (!isShowChildren && !isMainCategory(node) && children.isEmpty()) {
				node.remove("children");
			}
			tree.add(node);
		}
		return tree;
	}

	private static boolean isMainCategory(Map<String, Object> node) {
		Object v = node.get("is_main_category");
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		if (v instanceof String s) {
			return !"0".equals(s) && !s.isEmpty() && !"false".equalsIgnoreCase(s);
		}
		return false;
	}

	static int toIntOrZero(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(v.toString());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	static long toLongKey(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	/** 浅拷贝 LinkedHashMap 以保持字段顺序与可变性。 */
	private static final class LinkedOrderMap extends LinkedHashMap<String, Object> {
		private static final long serialVersionUID = 1L;

		LinkedOrderMap(Map<String, Object> src) {
			super(src);
		}
	}
}
