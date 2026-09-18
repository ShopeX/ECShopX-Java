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

package cn.shopex.ecshopx.theme.service;

import cn.shopex.ecshopx.members.service.MemberTagsCheckAndProcessService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class PagesTemplateDecoratorMemberTagsFilterService {

	private final MemberTagsCheckAndProcessService memberTagsCheckAndProcessService;

	public PagesTemplateDecoratorMemberTagsFilterService(
			MemberTagsCheckAndProcessService memberTagsCheckAndProcessService) {
		this.memberTagsCheckAndProcessService = memberTagsCheckAndProcessService;
	}

	public void apply(long companyId, long userId, List<Map<String, Object>> list) {
		if (list == null || list.isEmpty()) {
			return;
		}
		for (int i = 0; i < list.size(); i++) {
			list.get(i).put("_stableWidgetIdx", i);
		}
		LinkedHashMap<Long, LinkedHashSet<String>> tagRelWidgetIds = new LinkedHashMap<>();
		for (int i = 0; i < list.size(); i++) {
			Map<String, Object> widget = list.get(i);
			String topKey = String.valueOf(widget.get("_stableWidgetIdx"));
			Map<String, Object> params = getParamsMap(widget);
			if (params == null) {
				continue;
			}
			Object meberTags = params.get("meber_tags");
			if (hasMemberTagsConfigured(meberTags)) {
				if (userId <= 0L) {
					params.remove("meber_tags");
					widget.put("_removeTop", Boolean.TRUE);
				} else {
					collectTagRelKeys(meberTags, topKey, tagRelWidgetIds);
				}
			}
			if ("contentpart".equals(String.valueOf(widget.get("name")))) {
				processContentpartPass1(userId, widget, topKey, tagRelWidgetIds);
			}
		}
		removeMarkedTopLevel(list);

		if (userId > 0L && !tagRelWidgetIds.isEmpty()) {
			List<Long> tagIds = new ArrayList<>(tagRelWidgetIds.keySet());
			List<Map<String, Object>> bindTags =
					memberTagsCheckAndProcessService.checkAndProcessTag(companyId, userId, tagIds);
			Set<String> filterWidgetIds = new LinkedHashSet<>();
			for (Map<String, Object> tag : bindTags) {
				if (Boolean.TRUE.equals(tag.get("related"))) {
					Object tid = tag.get("tag_id");
					long tagId = longOrZero(tid);
					LinkedHashSet<String> keys = tagRelWidgetIds.get(tagId);
					if (keys != null) {
						filterWidgetIds.addAll(keys);
					}
				}
			}
			for (int i = 0; i < list.size(); i++) {
				Map<String, Object> widget = list.get(i);
				String topKey = String.valueOf(widget.get("_stableWidgetIdx"));
				Map<String, Object> params = getParamsMap(widget);
				if (params == null) {
					continue;
				}
				if (hasMemberTagsConfigured(params.get("meber_tags")) && !filterWidgetIds.contains(topKey)) {
					widget.put("_removeTop", Boolean.TRUE);
				}
				if ("contentpart".equals(String.valueOf(widget.get("name")))) {
					processContentpartPass2(widget, topKey, filterWidgetIds);
				}
			}
			removeMarkedTopLevel(list);
		}

		for (Map<String, Object> widget : list) {
			if ("contentpart".equals(String.valueOf(widget.get("name")))) {
				normalizeContentpartChildren(widget);
			}
			widget.remove("_stableWidgetIdx");
		}
	}

	private static void removeMarkedTopLevel(List<Map<String, Object>> list) {
		list.removeIf(w -> Boolean.TRUE.equals(w.get("_removeTop")));
		for (Map<String, Object> w : list) {
			w.remove("_removeTop");
		}
	}

	private void processContentpartPass1(
			long userId,
			Map<String, Object> widget,
			String topKey,
			LinkedHashMap<Long, LinkedHashSet<String>> tagRelWidgetIds) {
		Map<String, Object> params = getParamsMap(widget);
		if (params == null) {
			return;
		}
		Object dataRoot = params.get("data");
		if (!(dataRoot instanceof Map<?, ?> dm)) {
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> dataMap = (Map<String, Object>) (Map<?, ?>) dm;
		Object dataArr = dataMap.get("data");
		if (!(dataArr instanceof List<?> partsRaw)) {
			return;
		}
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> parts = (List<Map<String, Object>>) (List<?>) partsRaw;
		for (int pkey = 0; pkey < parts.size(); pkey++) {
			Map<String, Object> part = parts.get(pkey);
			if (part == null) {
				continue;
			}
			part.putIfAbsent("_stablePartIdx", pkey);
			String stablePartKey = topKey + "_" + part.get("_stablePartIdx");
			Object pMeber = part.get("meber_tags");
			if (hasMemberTagsConfigured(pMeber)) {
				if (userId <= 0L) {
					part.put("_removePart", Boolean.TRUE);
				} else {
					collectTagRelKeys(pMeber, stablePartKey, tagRelWidgetIds);
				}
			}
			Object childrenObj = part.get("children");
			if (!(childrenObj instanceof List<?> childrenRaw)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> children = (List<Map<String, Object>>) (List<?>) childrenRaw;
			for (int ckey = 0; ckey < children.size(); ckey++) {
				Map<String, Object> child = children.get(ckey);
				if (child == null) {
					continue;
				}
				child.putIfAbsent("_stableChildIdx", ckey);
				String stableChildKey = stablePartKey + "_" + child.get("_stableChildIdx");
				Object cMeber = child.get("meber_tags");
				if (hasMemberTagsConfigured(cMeber)) {
					if (userId <= 0L) {
						child.put("_removeChild", Boolean.TRUE);
					} else {
						collectTagRelKeys(cMeber, stableChildKey, tagRelWidgetIds);
					}
				}
			}
		}
		removeMarkedParts(parts);
	}

	private static void removeMarkedParts(List<Map<String, Object>> parts) {
		parts.removeIf(p -> p != null && Boolean.TRUE.equals(p.get("_removePart")));
		for (Map<String, Object> p : parts) {
			if (p == null) {
				continue;
			}
			p.remove("_removePart");
			Object ch = p.get("children");
			if (ch instanceof List<?> lr) {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> children = (List<Map<String, Object>>) (List<?>) lr;
				children.removeIf(c -> c != null && Boolean.TRUE.equals(c.get("_removeChild")));
				for (Map<String, Object> c : children) {
					if (c != null) {
						c.remove("_removeChild");
					}
				}
			}
		}
	}

	private static void processContentpartPass2(
			Map<String, Object> widget, String topKey, Set<String> filterWidgetIds) {
		Map<String, Object> params = getParamsMap(widget);
		if (params == null) {
			return;
		}
		Object dataRoot = params.get("data");
		if (!(dataRoot instanceof Map<?, ?> dm)) {
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> dataMap = (Map<String, Object>) (Map<?, ?>) dm;
		Object dataArr = dataMap.get("data");
		if (!(dataArr instanceof List<?> partsRaw)) {
			return;
		}
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> parts = (List<Map<String, Object>>) (List<?>) partsRaw;
		for (Map<String, Object> part : parts) {
			if (part == null) {
				continue;
			}
			Object sp = part.get("_stablePartIdx");
			String partKey = topKey + "_" + sp;
			if (hasMemberTagsConfigured(part.get("meber_tags")) && !filterWidgetIds.contains(partKey)) {
				part.put("_removePart", Boolean.TRUE);
				continue;
			}
			Object childrenObj = part.get("children");
			if (!(childrenObj instanceof List<?> childrenRaw)) {
				continue;
			}
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> children = (List<Map<String, Object>>) (List<?>) childrenRaw;
			for (Map<String, Object> child : children) {
				if (child == null) {
					continue;
				}
				Object sc = child.get("_stableChildIdx");
				String childKey = partKey + "_" + sc;
				if (hasMemberTagsConfigured(child.get("meber_tags")) && !filterWidgetIds.contains(childKey)) {
					child.put("_removeChild", Boolean.TRUE);
				}
			}
		}
		removeMarkedParts(parts);
	}

	private static void normalizeContentpartChildren(Map<String, Object> widget) {
		Map<String, Object> params = getParamsMap(widget);
		if (params == null) {
			return;
		}
		Object dataRoot = params.get("data");
		if (!(dataRoot instanceof Map<?, ?> dm)) {
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> dataMap = (Map<String, Object>) (Map<?, ?>) dm;
		Object dataArr = dataMap.get("data");
		if (!(dataArr instanceof List<?> partsRaw)) {
			return;
		}
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> parts = (List<Map<String, Object>>) (List<?>) partsRaw;
		for (Map<String, Object> part : parts) {
			if (part == null) {
				continue;
			}
			part.remove("_stablePartIdx");
			Object childrenObj = part.get("children");
			if (childrenObj instanceof List<?> lr) {
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> children = (List<Map<String, Object>>) (List<?>) lr;
				for (Map<String, Object> c : children) {
					if (c != null) {
						c.remove("_stableChildIdx");
					}
				}
				part.put("children", new ArrayList<>(children));
			}
		}
		dataMap.put("data", new ArrayList<>(parts));
	}

	private static void collectTagRelKeys(
			Object meberTags, String key, LinkedHashMap<Long, LinkedHashSet<String>> tagRelWidgetIds) {
		if (!(meberTags instanceof List<?> rels)) {
			return;
		}
		for (Object o : rels) {
			if (!(o instanceof Map<?, ?> rm)) {
				continue;
			}
			long tagId = longOrZero(rm.get("tag_id"));
			if (tagId <= 0L) {
				continue;
			}
			tagRelWidgetIds
					.computeIfAbsent(tagId, k -> new LinkedHashSet<>())
					.add(key);
		}
	}

	private static boolean hasMemberTagsConfigured(Object meberTags) {
		return meberTags instanceof List<?> l && !l.isEmpty();
	}

	private static Map<String, Object> getParamsMap(Map<String, Object> row) {
		Object p = row.get("params");
		if (!(p instanceof Map<?, ?> m)) {
			return null;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> out = (Map<String, Object>) (Map<?, ?>) m;
		return out;
	}

	private static long longOrZero(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v == null) {
			return 0L;
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
