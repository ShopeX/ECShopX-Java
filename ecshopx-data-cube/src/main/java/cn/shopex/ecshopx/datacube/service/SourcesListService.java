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

package cn.shopex.ecshopx.datacube.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.datacube.domain.Sources;
import cn.shopex.ecshopx.datacube.mapper.SourcesMapper;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SourcesListService {

	private static final Pattern INTEGER_STRING = Pattern.compile("^[+\\-]?\\d+$");

	private static final String MSG = "获取来源列表出错.";

	private final SourcesMapper sourcesMapper;

	private final MemberTagsMapper memberTagsMapper;

	private final ObjectMapper objectMapper;

	public SourcesListService(
			SourcesMapper sourcesMapper, MemberTagsMapper memberTagsMapper, ObjectMapper objectMapper) {
		this.sourcesMapper = sourcesMapper;
		this.memberTagsMapper = memberTagsMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getSourcesList(Map<String, Object> filter, String pageRaw, String pageSizeRaw) {
		Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
		validatePage(pageRaw, fieldErrors);
		validatePageSize(pageSizeRaw, fieldErrors);
		if (!fieldErrors.isEmpty()) {
			throw new ResourceException(MSG);
		}

		int page = parseValidatedPositiveInt(pageRaw.trim());
		int pageSize = parseValidatedPageSizeInt(pageSizeRaw.trim());

		page = page < 1 ? 1 : page;
		pageSize = pageSize > 1000 ? 1000 : pageSize;
		pageSize = pageSize <= 0 ? 10 : pageSize;

		Object companyObj = filter.get("company_id");
		if (!(companyObj instanceof Number)) {
			throw new ResourceException(MSG);
		}
		long companyId = ((Number) companyObj).longValue();

		LambdaQueryWrapper<Sources> w = Wrappers.lambdaQuery(Sources.class).eq(Sources::getCompanyId, companyId);
		if (filter.containsKey("source_name")) {
			w.eq(Sources::getSourceName, String.valueOf(filter.get("source_name")));
		}
		w.orderByDesc(Sources::getSourceId);

		Page<Sources> mp = new Page<>(page, pageSize);
		Page<Sources> pageResult = sourcesMapper.selectPage(mp, w);
		long total = pageResult.getTotal();
		int totalCount = total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;

		List<Map<String, Object>> list = new ArrayList<>();
		List<Long> mergedTagIds = new ArrayList<>();

		for (Sources entity : pageResult.getRecords()) {
			LinkedHashMap<String, Object> row = toListRowRawTags(entity);
			String rawTags = entity.getTagsId();
			Object decoded = decodeTagsIdForResponse(rawTags);
			appendTagIdsFromDecoded(decoded, mergedTagIds);
			if (decoded instanceof Collection<?> c && !c.isEmpty()) {
				row.put("tagsId", new ArrayList<>(c));
			} else {
				row.put("tagsId", null);
			}
			list.add(row);
		}

		LinkedHashMap<String, String> checkTags = new LinkedHashMap<>();
		if (!mergedTagIds.isEmpty()) {
			List<MemberTags> tagRows =
					memberTagsMapper.selectList(new LambdaQueryWrapper<MemberTags>()
							.eq(MemberTags::getCompanyId, companyId)
							.in(MemberTags::getTagId, mergedTagIds)
							.orderByDesc(MemberTags::getCreated));
			for (MemberTags tag : tagRows) {
				if (tag.getTagId() != null) {
					String name = tag.getTagName() != null ? tag.getTagName() : "";
					checkTags.put(String.valueOf(tag.getTagId()), name);
				}
			}
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		out.put("list", list);
		out.put("checkTags", checkTags.isEmpty() ? Collections.emptyList() : checkTags);
		return out;
	}

	private Object decodeTagsIdForResponse(String stored) {
		if (stored == null) {
			return null;
		}
		String t = stored.trim();
		if (!StringUtils.hasText(t)) {
			return null;
		}
		try {
			return objectMapper.readValue(t, Object.class);
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	private static void appendTagIdsFromDecoded(Object decoded, List<Long> out) {
		if (!(decoded instanceof Collection<?> c) || c.isEmpty()) {
			return;
		}
		for (Object el : c) {
			Long id = elementToTagId(el);
			if (id != null) {
				out.add(id);
			}
		}
	}

	private static Long elementToTagId(Object el) {
		if (el == null) {
			return null;
		}
		if (el instanceof Number n) {
			return n.longValue();
		}
		if (el instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return null;
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				return null;
			}
		}
		try {
			return Long.parseLong(String.valueOf(el).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static LinkedHashMap<String, Object> toListRowRawTags(Sources entity) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		Long sid = entity.getSourceId();
		row.put("sourceId", sid != null && sid <= Integer.MAX_VALUE ? sid.intValue() : sid);
		Long cid = entity.getCompanyId();
		row.put("companyId", cid != null && cid <= Integer.MAX_VALUE ? cid.intValue() : cid);
		row.put("sourceName", entity.getSourceName());
		row.put("created", entity.getCreated());
		row.put("updated", entity.getUpdated());
		row.put("tagsId", entity.getTagsId());
		return row;
	}

	private static void validatePage(String raw, Map<String, List<String>> fieldErrors) {
		if (raw == null || raw.trim().isEmpty()) {
			fieldErrors.put("page", List.of("The page field is required."));
			return;
		}
		String s = raw.trim();
		if (!INTEGER_STRING.matcher(s).matches()) {
			fieldErrors.put("page", List.of("The page must be an integer."));
			return;
		}
		long v;
		try {
			v = Long.parseLong(s);
		} catch (NumberFormatException e) {
			fieldErrors.put("page", List.of("The page must be an integer."));
			return;
		}
		if (v < 1L) {
			fieldErrors.put("page", List.of("The page must be at least 1."));
			return;
		}
		if (v > Integer.MAX_VALUE) {
			fieldErrors.put("page", List.of("The page must be an integer."));
		}
	}

	private static void validatePageSize(String raw, Map<String, List<String>> fieldErrors) {
		if (raw == null || raw.trim().isEmpty()) {
			fieldErrors.put("pageSize", List.of("The page size field is required."));
			return;
		}
		String s = raw.trim();
		if (!INTEGER_STRING.matcher(s).matches()) {
			fieldErrors.put("pageSize", List.of("The page size must be an integer."));
			return;
		}
		long v;
		try {
			v = Long.parseLong(s);
		} catch (NumberFormatException e) {
			fieldErrors.put("pageSize", List.of("The page size must be an integer."));
			return;
		}
		if (v < 1L) {
			fieldErrors.put("pageSize", List.of("The page size must be at least 1."));
			return;
		}
		if (v > 1000L) {
			fieldErrors.put("pageSize", List.of("The page size may not be greater than 1000."));
		}
	}

	private static int parseValidatedPositiveInt(String trimmed) {
		return (int) Long.parseLong(trimmed);
	}

	private static int parseValidatedPageSizeInt(String trimmed) {
		return Math.toIntExact(Long.parseLong(trimmed));
	}
}
