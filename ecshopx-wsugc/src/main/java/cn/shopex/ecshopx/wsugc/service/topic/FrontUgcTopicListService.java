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

import cn.shopex.ecshopx.wsugc.domain.Topic;
import cn.shopex.ecshopx.wsugc.mapper.TopicMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FrontUgcTopicListService {

	private static final String H5_LANG_TAG = "zh-CN";

	private final TopicMapper topicMapper;
	private final TopicOutsideLangReadService topicOutsideLangReadService;
	private final TopicDetailService topicDetailService;

	public FrontUgcTopicListService(
			TopicMapper topicMapper,
			TopicOutsideLangReadService topicOutsideLangReadService,
			TopicDetailService topicDetailService) {
		this.topicMapper = topicMapper;
		this.topicOutsideLangReadService = topicOutsideLangReadService;
		this.topicDetailService = topicDetailService;
	}

	public Object buildH5TopicList(long companyId, int page, int pageSize, String topicName, String sort) {
		LambdaQueryWrapper<Topic> w = new LambdaQueryWrapper<>();
		w.eq(Topic::getCompanyId, companyId).eq(Topic::getEnabled, 1).eq(Topic::getStatus, 1);

		boolean nameFilter = StringUtils.hasText(topicName);
		String topicNameQuery = nameFilter ? topicName.trim() : "";
		if (nameFilter) {
			List<Long> langIds =
					topicOutsideLangReadService.findDataIdsByFieldContains(
							companyId, H5_LANG_TAG, "topic_name", topicNameQuery);
			if (!langIds.isEmpty()) {
				w.in(Topic::getTopicId, langIds);
			} else {
				w.like(Topic::getTopicName, escapeLike(topicNameQuery) + "%");
			}
		}

		String sortParam = sort == null ? "" : sort;
		if (sortParam.isEmpty() || sortParam.trim().isEmpty()) {
			w.orderByAsc(Topic::getPOrder).orderByDesc(Topic::getCreated);
		} else {
			String[] parts = sortParam.split(" ", -1);
			String fieldRaw = parts[0];
			String dirRaw = parts.length > 1 ? parts[1] : null;
			String dirSql = sqlDirection(dirRaw);
			String quoted = "`" + quoteBacktickIdentifier(fieldRaw) + "`";
			w.last("ORDER BY " + quoted + " " + dirSql + ", `p_order` ASC");
		}

		Long total = topicMapper.selectCount(w);
		if (total == null || total == 0L) {
			return Collections.emptyList();
		}

		List<Topic> topics;
		if (pageSize > 0) {
			Page<Topic> p = new Page<>(page, pageSize, false);
			topicMapper.selectPage(p, w);
			topics = p.getRecords() != null ? p.getRecords() : Collections.emptyList();
		} else {
			topics = topicMapper.selectList(w);
			if (topics == null) {
				topics = Collections.emptyList();
			}
		}

		List<LinkedHashMap<String, Object>> rows = new ArrayList<>();
		for (Topic entity : topics) {
			LinkedHashMap<String, Object> row =
					topicDetailService.toH5TopicListRowMap(entity, companyId, H5_LANG_TAG);
			if (nameFilter) {
				long tid = entity.getTopicId() != null ? entity.getTopicId() : 0L;
				Object topicNameCell = row.get("topic_name");
				String displayName = topicNameCell == null ? "" : topicNameCell.toString().trim();
				long rank = Objects.equals(displayName, topicNameQuery) ? -1L : tid;
				row.put("rank", rank);
				row = ksortRowMap(row);
			}
			rows.add(row);
		}

		if (nameFilter) {
			rows.sort(
					Comparator.comparingLong((LinkedHashMap<String, Object> m) -> longFromMap(m, "rank"))
							.thenComparingLong(m -> longFromMap(m, "topic_id")));
		}

		Map<String, Object> body = new TreeMap<>();
		body.put("list", rows);
		body.put("total_count", total);
		return body;
	}

	private static String escapeLike(String s) {
		return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static String quoteBacktickIdentifier(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replace("`", "``");
	}

	private static String sqlDirection(String dirRaw) {
		if (dirRaw == null) {
			return "ASC";
		}
		if (dirRaw.isEmpty()) {
			return "ASC";
		}
		String d = dirRaw.trim();
		if (d.equalsIgnoreCase("desc")) {
			return "DESC";
		}
		if (d.equalsIgnoreCase("asc")) {
			return "ASC";
		}
		return dirRaw;
	}

	private static LinkedHashMap<String, Object> ksortRowMap(Map<String, Object> src) {
		TreeMap<String, Object> sorted = new TreeMap<>(src);
		return new LinkedHashMap<>(sorted);
	}

	private static long longFromMap(Map<String, Object> m, String key) {
		Object v = m.get(key);
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
