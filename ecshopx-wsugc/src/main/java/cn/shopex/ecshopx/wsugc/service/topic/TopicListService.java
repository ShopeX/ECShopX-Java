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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.wsugc.domain.Topic;
import cn.shopex.ecshopx.wsugc.mapper.TopicMapper;
import cn.shopex.ecshopx.wsugc.service.post.UgcPostUserIdResolveService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TopicListService {

	private static final Logger log = LoggerFactory.getLogger(TopicListService.class);

	private final TopicMapper topicMapper;
	private final UgcPostUserIdResolveService ugcPostUserIdResolveService;
	private final TopicDetailService topicDetailService;
	private final TopicOutsideLangReadService topicOutsideLangReadService;

	public TopicListService(
			TopicMapper topicMapper,
			UgcPostUserIdResolveService ugcPostUserIdResolveService,
			TopicDetailService topicDetailService,
			TopicOutsideLangReadService topicOutsideLangReadService) {
		this.topicMapper = topicMapper;
		this.ugcPostUserIdResolveService = ugcPostUserIdResolveService;
		this.topicDetailService = topicDetailService;
		this.topicOutsideLangReadService = topicOutsideLangReadService;
	}

	public Object buildList(
			long companyId,
			String langTag,
			int page,
			int pageSize,
			String topicName,
			String isTopRaw,
			String status,
			String source,
			String nickname,
			String mobile,
			String sort) {
		LambdaQueryWrapper<Topic> w = new LambdaQueryWrapper<>();
		w.eq(Topic::getCompanyId, companyId);

		boolean topicNameSearchActive = StringUtils.hasText(topicName);
		String topicNameQuery = topicNameSearchActive ? topicName.trim() : "";
		if (topicNameSearchActive) {
			List<Long> langIds =
					topicOutsideLangReadService.findDataIdsByFieldContains(
							companyId, langTag, "topic_name", topicNameQuery);
			if (!langIds.isEmpty()) {
				w.in(Topic::getTopicId, langIds);
			} else {
				w.like(Topic::getTopicName, escapeLike(topicNameQuery) + "%");
			}
		}
		if (isTopRaw != null && !isTopRaw.isEmpty()) {
			w.eq(Topic::getIsTop, parseIsTop(isTopRaw));
		}
		Integer statusVal = tryParseIntFilter(status);
		if (statusVal != null) {
			w.eq(Topic::getStatus, statusVal);
		}
		Integer sourceVal = tryParseIntFilter(source);
		if (sourceVal != null) {
			w.eq(Topic::getSource, sourceVal);
		}
		if (StringUtils.hasText(mobile)) {
			w.in(Topic::getUserId, ugcPostUserIdResolveService.userIdsByMobile(mobile.trim()));
		} else if (StringUtils.hasText(nickname)) {
			w.in(Topic::getUserId, ugcPostUserIdResolveService.userIdsByNicknameContains(nickname.trim()));
		}
		applySort(w, sort);

		Page<Topic> p = new Page<>(page, pageSize);
		topicMapper.selectPage(p, w);

		if (p.getTotal() == 0L || p.getRecords() == null || p.getRecords().isEmpty()) {
			try {
				log.debug("topic list empty companyId={} page={} pageSize={}", companyId, page, pageSize);
			} catch (Exception ignored) {
				// debug only
			}
			return Collections.emptyList();
		}

		List<LinkedHashMap<String, Object>> rows = new ArrayList<>();
		for (Topic e : p.getRecords()) {
			LinkedHashMap<String, Object> row = topicDetailService.toAdminListRowMap(e, langTag);
			if (topicNameSearchActive) {
				long tid = e.getTopicId() != null ? e.getTopicId() : 0L;
				Object topicNameCell = row.get("topic_name");
				String displayName =
						topicNameCell == null ? "" : topicNameCell.toString().trim();
				long rank = Objects.equals(displayName, topicNameQuery) ? -1L : tid;
				row.put("rank", rank);
				row = ksortRowMap(row);
			}
			rows.add(row);
		}

		if (topicNameSearchActive) {
			rows.sort(
					Comparator.comparingLong((LinkedHashMap<String, Object> m) -> longFromMap(m, "rank"))
							.thenComparingLong(m -> longFromMap(m, "topic_id")));
		}

		try {
			log.debug("topic list size={} total={}", rows.size(), p.getTotal());
		} catch (Exception ignored) {
			// debug only
		}

		Map<String, Object> body = new TreeMap<>();
		body.put("list", rows);
		body.put("total_count", p.getTotal());
		return body;
	}

	private static Integer tryParseIntFilter(String raw) {
		if (raw == null || raw.trim().isEmpty()) {
			return null;
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Integer parseIsTop(String raw) {
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
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

	private static String escapeLike(String s) {
		return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static void applySort(LambdaQueryWrapper<Topic> wrapper, String sort) {
		if (sort == null || sort.trim().isEmpty()) {
			wrapper.orderByAsc(Topic::getPOrder).orderByDesc(Topic::getCreated);
			return;
		}
		String trimmed = sort.trim();
		int sp = trimmed.indexOf(' ');
		if (sp <= 0 || sp >= trimmed.length() - 1) {
			throw new BadRequestException("sort 参数格式无效");
		}
		String field = trimmed.substring(0, sp).trim();
		String direction = trimmed.substring(sp + 1).trim();
		if (!StringUtils.hasText(field) || !StringUtils.hasText(direction)) {
			throw new BadRequestException("sort 参数格式无效");
		}
		String dirLower = direction.toLowerCase();
		boolean asc;
		if ("asc".equals(dirLower)) {
			asc = true;
		} else if ("desc".equals(dirLower)) {
			asc = false;
		} else {
			throw new BadRequestException("sort 参数格式无效");
		}
		switch (field) {
			case "p_order" -> {
				if (asc) {
					wrapper.orderByAsc(Topic::getPOrder);
				} else {
					wrapper.orderByDesc(Topic::getPOrder);
				}
			}
			case "created" -> {
				if (asc) {
					wrapper.orderByAsc(Topic::getCreated);
				} else {
					wrapper.orderByDesc(Topic::getCreated);
				}
			}
			case "updated" -> {
				if (asc) {
					wrapper.orderByAsc(Topic::getUpdated);
				} else {
					wrapper.orderByDesc(Topic::getUpdated);
				}
			}
			case "topic_id" -> {
				if (asc) {
					wrapper.orderByAsc(Topic::getTopicId);
				} else {
					wrapper.orderByDesc(Topic::getTopicId);
				}
			}
			case "user_id" -> {
				if (asc) {
					wrapper.orderByAsc(Topic::getUserId);
				} else {
					wrapper.orderByDesc(Topic::getUserId);
				}
			}
			case "status" -> {
				if (asc) {
					wrapper.orderByAsc(Topic::getStatus);
				} else {
					wrapper.orderByDesc(Topic::getStatus);
				}
			}
			case "source" -> {
				if (asc) {
					wrapper.orderByAsc(Topic::getSource);
				} else {
					wrapper.orderByDesc(Topic::getSource);
				}
			}
			case "is_top" -> {
				if (asc) {
					wrapper.orderByAsc(Topic::getIsTop);
				} else {
					wrapper.orderByDesc(Topic::getIsTop);
				}
			}
			case "enabled" -> {
				if (asc) {
					wrapper.orderByAsc(Topic::getEnabled);
				} else {
					wrapper.orderByDesc(Topic::getEnabled);
				}
			}
			case "operator_id" -> {
				if (asc) {
					wrapper.orderByAsc(Topic::getOperatorId);
				} else {
					wrapper.orderByDesc(Topic::getOperatorId);
				}
			}
			case "company_id" -> {
				if (asc) {
					wrapper.orderByAsc(Topic::getCompanyId);
				} else {
					wrapper.orderByDesc(Topic::getCompanyId);
				}
			}
			case "topic_name" -> {
				if (asc) {
					wrapper.orderByAsc(Topic::getTopicName);
				} else {
					wrapper.orderByDesc(Topic::getTopicName);
				}
			}
			default -> throw new BadRequestException("sort 参数格式无效");
		}
		if (!"p_order".equals(field)) {
			wrapper.orderByAsc(Topic::getPOrder);
		}
	}
}
