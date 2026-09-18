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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SourcesDetailService {

	private final SourcesMapper sourcesMapper;

	private final MemberTagsMapper memberTagsMapper;

	private final ObjectMapper objectMapper;

	public SourcesDetailService(
			SourcesMapper sourcesMapper, MemberTagsMapper memberTagsMapper, ObjectMapper objectMapper) {
		this.sourcesMapper = sourcesMapper;
		this.memberTagsMapper = memberTagsMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> buildDetailMap(long sourceId) {
		Sources row = sourcesMapper.selectById(sourceId);
		if (row == null) {
			throw new ResourceException("source_id=" + sourceId + "的来源不存在");
		}
		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		Long sid = row.getSourceId();
		result.put("source_id", sid != null && sid <= Integer.MAX_VALUE ? sid.intValue() : sid);
		result.put("source_name", row.getSourceName());
		Long cid = row.getCompanyId();
		result.put("company_id", cid != null && cid <= Integer.MAX_VALUE ? cid.intValue() : cid);
		result.put("tags_id", decodeTagsIdForResponse(row.getTagsId()));
		result.put("created", row.getCreated());
		result.put("updated", row.getUpdated());
		return result;
	}

	public void appendCheckTagsIfTruthy(long jwtCompanyId, Map<String, Object> result) {
		Object decoded = result.get("tags_id");
		if (!isTruthyTagsIdDecoded(decoded)) {
			return;
		}
		List<Long> ids = parseTagIdsForCheckTags(decoded);
		if (ids.isEmpty()) {
			return;
		}
		Page<MemberTags> p = new Page<>(1, 100);
		LambdaQueryWrapper<MemberTags> wrapper = Wrappers.lambdaQuery(MemberTags.class)
				.eq(MemberTags::getCompanyId, jwtCompanyId)
				.in(MemberTags::getTagId, ids)
				.orderByDesc(MemberTags::getCreated);
		Page<MemberTags> pageResult = memberTagsMapper.selectPage(p, wrapper);
		List<Map<String, Object>> list = new ArrayList<>();
		for (MemberTags tag : pageResult.getRecords()) {
			list.add(memberTagToSnakeRow(tag));
		}
		result.put("checkTags", list);
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

	private static boolean isTruthyTagsIdDecoded(Object decoded) {
		if (decoded == null) {
			return false;
		}
		if (decoded instanceof Boolean b && !b) {
			return false;
		}
		if (decoded instanceof Number n && n.doubleValue() == 0.0) {
			return false;
		}
		if (decoded instanceof String s && s.trim().isEmpty()) {
			return false;
		}
		if (decoded instanceof Collection<?> c && c.isEmpty()) {
			return false;
		}
		if (decoded instanceof Map<?, ?>) {
			return false;
		}
		return true;
	}

	private static List<Long> parseTagIdsForCheckTags(Object decoded) {
		List<Long> out = new ArrayList<>();
		if (decoded instanceof Collection<?> c) {
			for (Object el : c) {
				Long id = elementToTagId(el);
				if (id != null) {
					out.add(id);
				}
			}
			return out;
		}
		if (decoded instanceof Number n) {
			out.add(n.longValue());
			return out;
		}
		if (decoded instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				return out;
			}
			try {
				out.add(Long.parseLong(t));
			} catch (NumberFormatException e) {
				return List.of();
			}
			return out;
		}
		return out;
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

	private static Map<String, Object> memberTagToSnakeRow(MemberTags tag) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		Long tid = tag.getTagId();
		m.put("tag_id", tid != null && tid <= Integer.MAX_VALUE ? tid.intValue() : tid);
		Long cid = tag.getCompanyId();
		m.put("company_id", cid != null && cid <= Integer.MAX_VALUE ? cid.intValue() : cid);
		m.put("tag_name", tag.getTagName());
		m.put("description", tag.getDescription());
		m.put("tag_icon", tag.getTagIcon());
		m.put("saleman_id", tag.getSalemanId());
		m.put("tag_status", tag.getTagStatus());
		m.put("category_id", tag.getCategoryId());
		m.put("self_tag_count", tag.getSelfTagCount());
		m.put("tag_color", tag.getTagColor());
		m.put("font_color", tag.getFontColor());
		Long did = tag.getDistributorId();
		m.put("distributor_id", did != null && did <= Integer.MAX_VALUE ? did.intValue() : did);
		Long cr = tag.getCreated();
		m.put("created", cr != null && cr <= Integer.MAX_VALUE ? cr.intValue() : cr);
		Long up = tag.getUpdated();
		m.put("updated", up != null && up <= Integer.MAX_VALUE ? up.intValue() : up);
		return m;
	}
}
