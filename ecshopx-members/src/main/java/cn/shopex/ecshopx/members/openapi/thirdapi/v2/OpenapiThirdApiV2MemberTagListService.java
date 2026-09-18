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

package cn.shopex.ecshopx.members.openapi.thirdapi.v2;

import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import cn.shopex.ecshopx.members.service.MemberTagsListLangReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2MemberTagListService {

	private static final DateTimeFormatter DATETIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final MemberTagsMapper memberTagsMapper;
	private final MemberTagsListLangReadService memberTagsListLangReadService;

	public OpenapiThirdApiV2MemberTagListService(
			MemberTagsMapper memberTagsMapper,
			MemberTagsListLangReadService memberTagsListLangReadService) {
		this.memberTagsMapper = memberTagsMapper;
		this.memberTagsListLangReadService = memberTagsListLangReadService;
	}

	public Map<String, Object> executeOpenapiGetTagsList(
			long companyId, int page, int pageSize, String tagNameRaw, String langRaw) {
		LambdaQueryWrapper<MemberTags> baseFilter = new LambdaQueryWrapper<MemberTags>()
				.eq(MemberTags::getCompanyId, companyId);
		if (phpTruthyTagName(tagNameRaw)) {
			baseFilter.like(MemberTags::getTagName, tagNameRaw);
		}

		long totalCount = memberTagsMapper.selectCount(baseFilter);

		LambdaQueryWrapper<MemberTags> listWrapper = new LambdaQueryWrapper<MemberTags>()
				.eq(MemberTags::getCompanyId, companyId)
				.select(
						MemberTags::getTagId,
						MemberTags::getTagName,
						MemberTags::getCategoryId,
						MemberTags::getDescription,
						MemberTags::getTagColor,
						MemberTags::getFontColor,
						MemberTags::getSelfTagCount,
						MemberTags::getCreated,
						MemberTags::getUpdated)
				.orderByDesc(MemberTags::getCreated);
		if (phpTruthyTagName(tagNameRaw)) {
			listWrapper.like(MemberTags::getTagName, tagNameRaw);
		}

		Page<MemberTags> pageRequest = new Page<>(page, pageSize, false);
		memberTagsMapper.selectPage(pageRequest, listWrapper);
		List<MemberTags> entities = pageRequest.getRecords();

		List<Map<String, Object>> list = entities.stream()
				.map(OpenapiThirdApiV2MemberTagListService::formatOpenApiListRow)
				.toList();

		if (!list.isEmpty()) {
			String lang = StringUtils.hasText(langRaw) ? langRaw.trim() : "zh-CN";
			Map<Long, Object> descriptionsByTagId = new LinkedHashMap<>();
			for (Map<String, Object> row : list) {
				Long tagId = longOrNull(row.get("tag_id"));
				if (tagId != null) {
					descriptionsByTagId.put(tagId, row.get("description"));
				}
			}
			memberTagsListLangReadService.applyListLang(companyId, lang, list);
			for (Map<String, Object> row : list) {
				Long tagId = longOrNull(row.get("tag_id"));
				if (tagId != null && descriptionsByTagId.containsKey(tagId)) {
					row.put("description", descriptionsByTagId.get(tagId));
				}
			}
		}

		return formatListStruct(totalCount, list, page, pageSize);
	}

	private static boolean phpTruthyTagName(String raw) {
		if (raw == null) {
			return false;
		}
		if (raw.isEmpty()) {
			return false;
		}
		return !"0".equals(raw);
	}

	private static Map<String, Object> formatOpenApiListRow(MemberTags entity) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("tag_id", entity.getTagId());
		row.put("tag_name", entity.getTagName());
		row.put("category_id", entity.getCategoryId());
		row.put("description", entity.getDescription());
		row.put("tag_color", entity.getTagColor());
		row.put("font_color", entity.getFontColor());
		row.put("self_tag_count", entity.getSelfTagCount());
		row.put("created", formatEpochSeconds(entity.getCreated()));
		row.put("updated", formatEpochSeconds(entity.getUpdated()));
		return row;
	}

	private static String formatEpochSeconds(Long epoch) {
		if (epoch == null) {
			return null;
		}
		return Instant.ofEpochSecond(epoch).atZone(ZoneId.systemDefault()).format(DATETIME_FMT);
	}

	public static Map<String, Object> formatListStruct(
			long totalCount, List<Map<String, Object>> list, int page, int pageSize) {
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", totalCount);
		result.put("is_last_page", computeIsLastPage(totalCount, page, pageSize));
		result.put("pager", Map.of("page", page, "page_size", pageSize));
		result.put("list", list != null ? list : List.of());
		return result;
	}

	public static Map<String, Object> formatListStructPhpHandlerOrder(
			long totalCount, List<Map<String, Object>> list, int page, int pageSize) {
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", totalCount);
		result.put("list", list != null ? list : List.of());
		result.put("is_last_page", computeIsLastPage(totalCount, page, pageSize));
		result.put("pager", Map.of("page", page, "page_size", pageSize));
		return result;
	}

	public static int computeIsLastPage(long totalCount, int page, int pageSize) {
		if (pageSize <= 0) {
			return 1;
		}
		long totalPage = (long) Math.ceil((double) totalCount / pageSize);
		return totalPage <= page ? 1 : 0;
	}

	private static Long longOrNull(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
