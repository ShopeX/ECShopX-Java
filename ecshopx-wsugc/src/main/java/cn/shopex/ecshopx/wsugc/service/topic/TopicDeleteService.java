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

package cn.shopex.ecshopx.wsugc.service.topic;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wsugc.mapper.TopicMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TopicDeleteService {

	private final TopicMultiLangDeleteService topicMultiLangDeleteService;
	private final TopicMapper topicMapper;

	public TopicDeleteService(
			TopicMultiLangDeleteService topicMultiLangDeleteService, TopicMapper topicMapper) {
		this.topicMultiLangDeleteService = topicMultiLangDeleteService;
		this.topicMapper = topicMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> deleteTopics(Map<String, Object> input) {
		Object raw = input.get("topic_id");
		if (raw == null) {
			throw new ResourceException("topic_id参数不能为空");
		}

		List<Object> displayIds = new ArrayList<>();
		LinkedHashSet<Long> idsToDelete = new LinkedHashSet<>();

		if (raw instanceof Collection<?> col) {
			if (col.isEmpty()) {
				throw new ResourceException("topic_id参数不能为空");
			}
			for (Object el : col) {
				displayIds.add(toDisplayToken(el));
				tryAddParseableLong(idsToDelete, el);
			}
		} else {
			displayIds.add(toDisplayToken(raw));
			tryAddParseableLong(idsToDelete, raw);
		}

		if (!idsToDelete.isEmpty()) {
			topicMultiLangDeleteService.deleteMultiLangForTopics(new ArrayList<>(idsToDelete));
			topicMapper.deleteBatchIds(idsToDelete);
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		if (raw instanceof Collection<?>) {
			out.put("topic_id", displayIds);
		} else {
			out.put("topic_id", toDisplayToken(raw));
		}
		out.put("message", "删除成功");
		return out;
	}

	private static Object toDisplayToken(Object el) {
		if (el == null) {
			return null;
		}
		Long parsed = tryParseLong(el);
		if (parsed != null) {
			return parsed;
		}
		return el.toString().trim();
	}

	private static Long tryParseLong(Object el) {
		if (el == null) {
			return null;
		}
		if (el instanceof Number n) {
			return n.longValue();
		}
		String s = el.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static void tryAddParseableLong(LinkedHashSet<Long> ids, Object el) {
		Long l = tryParseLong(el);
		if (l != null) {
			ids.add(l);
		}
	}
}
