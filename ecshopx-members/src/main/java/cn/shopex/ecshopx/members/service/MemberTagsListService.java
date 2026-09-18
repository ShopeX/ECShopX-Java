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

package cn.shopex.ecshopx.members.service;

import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.members.domain.MemberTagGroup;
import cn.shopex.ecshopx.members.domain.MemberTagGroupRel;
import cn.shopex.ecshopx.members.domain.MemberTagLiveCountRow;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.mapper.MemberRelTagsMapper;
import cn.shopex.ecshopx.members.mapper.MemberTagGroupMapper;
import cn.shopex.ecshopx.members.mapper.MemberTagGroupRelMapper;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class MemberTagsListService {

	private final MemberTagsMapper memberTagsMapper;
	private final MemberRelTagsMapper memberRelTagsMapper;
	private final MemberTagGroupMapper memberTagGroupMapper;
	private final MemberTagGroupRelMapper memberTagGroupRelMapper;
	private final MemberTagsCreateService memberTagsCreateService;
	private final MemberTagsSnakeFormatter memberTagsSnakeFormatter;
	private final MemberTagsListLangReadService memberTagsListLangReadService;

	public MemberTagsListService(
			MemberTagsMapper memberTagsMapper,
			MemberRelTagsMapper memberRelTagsMapper,
			MemberTagGroupMapper memberTagGroupMapper,
			MemberTagGroupRelMapper memberTagGroupRelMapper,
			MemberTagsCreateService memberTagsCreateService,
			MemberTagsSnakeFormatter memberTagsSnakeFormatter,
			MemberTagsListLangReadService memberTagsListLangReadService) {
		this.memberTagsMapper = memberTagsMapper;
		this.memberRelTagsMapper = memberRelTagsMapper;
		this.memberTagGroupMapper = memberTagGroupMapper;
		this.memberTagGroupRelMapper = memberTagGroupRelMapper;
		this.memberTagsCreateService = memberTagsCreateService;
		this.memberTagsSnakeFormatter = memberTagsSnakeFormatter;
		this.memberTagsListLangReadService = memberTagsListLangReadService;
	}

	public Object getTagsInfo(long companyId, long distributorId, long tagId, String requestLangTag) {
		LambdaQueryWrapper<MemberTags> w = new LambdaQueryWrapper<>();
		w.eq(MemberTags::getTagId, tagId)
				.eq(MemberTags::getCompanyId, companyId)
				.eq(MemberTags::getDistributorId, distributorId);
		MemberTags entity = memberTagsMapper.selectOne(w);
		if (entity == null) {
			return Collections.emptyList();
		}
		Map<String, Object> row = memberTagsSnakeFormatter.toSnakeTagRow(entity);
		ArrayList<Map<String, Object>> buf = new ArrayList<>();
		buf.add(row);
		memberTagsListLangReadService.applyListLang(companyId, requestLangTag, buf);

		List<MemberTagLiveCountRow> counts =
				memberRelTagsMapper.selectLiveMemberCountByTagIds(companyId, List.of(tagId));
		Map<Long, Integer> byTag = new LinkedHashMap<>();
		for (MemberTagLiveCountRow r : counts) {
			Long tid = r.getTagId();
			if (tid == null || tid <= 0L) {
				continue;
			}
			long n = r.getNum() == null ? 0L : r.getNum();
			int ni = (int) Math.min(Integer.MAX_VALUE, n);
			byTag.put(tid, ni);
		}
		Long id = parseTagIdLong(row.get("tag_id"));
		row.put("self_tag_count", id == null ? 0 : byTag.getOrDefault(id, 0));
		return row;
	}

	public Map<String, Object> getTagsList(
			long companyId, long distributorId, MemberTagsListParams params, String requestLangTag) {
		LambdaQueryWrapper<MemberTags> w = new LambdaQueryWrapper<>();
		w.eq(MemberTags::getCompanyId, companyId).eq(MemberTags::getDistributorId, distributorId);

		if (params.tagName() != null) {
			String escaped = escapeLike(params.tagName());
			w.and(q -> q.like(MemberTags::getTagName, "%" + escaped + "%"));
		}

		if (params.categoryId() != null) {
			String t = params.categoryId().trim();
			long v = LeadingNumberParser.parseAsLong(t);
			int categoryEq = (int) Math.min(Integer.MAX_VALUE, Math.max(Integer.MIN_VALUE, v));
			w.eq(MemberTags::getCategoryId, categoryEq);
		}

		if (params.tagStatus() != null) {
			if (Objects.equals("self", params.tagStatus())) {
				w.eq(MemberTags::getTagStatus, "self");
			} else {
				w.ne(MemberTags::getTagStatus, "self");
			}
		}

		w.orderByDesc(MemberTags::getCreated);

		long total = memberTagsMapper.selectCount(w);
		int totalCount = (int) Math.min(Integer.MAX_VALUE, total);

		List<MemberTags> entities;
		if (total == 0) {
			entities = List.of();
		} else if (params.pageSize() > 0) {
			Page<MemberTags> page = new Page<>(params.page(), params.pageSize(), false);
			memberTagsMapper.selectPage(page, w);
			entities = page.getRecords();
		} else {
			entities = memberTagsMapper.selectList(w);
		}

		List<Map<String, Object>> rows =
				entities.stream()
						.map(memberTagsSnakeFormatter::toSnakeTagRow)
						.collect(Collectors.toCollection(ArrayList::new));

		if (!rows.isEmpty()) {
			memberTagsListLangReadService.applyListLang(companyId, requestLangTag, rows);
		}

		List<Long> tagIds =
				rows.stream()
						.map(r -> r.get("tag_id"))
						.map(MemberTagsListService::parseTagIdLong)
						.filter(id -> id != null && id > 0L)
						.distinct()
						.toList();

		if (!tagIds.isEmpty()) {
			List<MemberTagLiveCountRow> counts =
					memberRelTagsMapper.selectLiveMemberCountByTagIds(companyId, tagIds);
			Map<Long, Integer> byTag = new LinkedHashMap<>();
			for (MemberTagLiveCountRow r : counts) {
				Long tid = r.getTagId();
				if (tid == null || tid <= 0L) {
					continue;
				}
				long n = r.getNum() == null ? 0L : r.getNum();
				int ni = (int) Math.min(Integer.MAX_VALUE, n);
				byTag.put(tid, ni);
			}
			for (Map<String, Object> row : rows) {
				Long id = parseTagIdLong(row.get("tag_id"));
				if (id == null) {
					continue;
				}
				row.put("self_tag_count", byTag.getOrDefault(id, 0));
			}
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", totalCount);
		data.put("list", rows);
		return data;
	}

	public Map<String, Object> getTagGroupList(
			long companyId, int page, int pageSize, String groupNameRaw, String requestLangTag) {
		final long distributorScope = 0L;

		LambdaQueryWrapper<MemberTagGroup> baseWrapper = new LambdaQueryWrapper<>();
		baseWrapper
				.eq(MemberTagGroup::getCompanyId, companyId)
				.eq(MemberTagGroup::getDistributorId, distributorScope)
				.orderByDesc(MemberTagGroup::getCreated);

		if (!isBlankQueryToken(groupNameRaw)) {
			String keyword = groupNameRaw;
			String escaped = escapeLike(keyword);

			LambdaQueryWrapper<MemberTagGroup> nameW = new LambdaQueryWrapper<>();
			nameW
					.eq(MemberTagGroup::getCompanyId, companyId)
					.eq(MemberTagGroup::getDistributorId, distributorScope)
					.like(MemberTagGroup::getGroupName, "%" + escaped + "%")
					.orderByDesc(MemberTagGroup::getCreated);
			List<MemberTagGroup> nameHits = memberTagGroupMapper.selectList(nameW);
			LinkedHashSet<Long> groupIdSet = new LinkedHashSet<>();
			for (MemberTagGroup g : nameHits) {
				if (g.getGroupId() != null) {
					groupIdSet.add(g.getGroupId());
				}
			}

			LambdaQueryWrapper<MemberTags> tagNameW = new LambdaQueryWrapper<>();
			tagNameW
					.eq(MemberTags::getCompanyId, companyId)
					.eq(MemberTags::getDistributorId, distributorScope)
					.like(MemberTags::getTagName, "%" + escaped + "%");
			List<MemberTags> tagNameHits = memberTagsMapper.selectList(tagNameW);
			List<Long> matchedTagIds =
					tagNameHits.stream()
							.map(MemberTags::getTagId)
							.filter(Objects::nonNull)
							.distinct()
							.toList();
			if (!matchedTagIds.isEmpty()) {
				LambdaQueryWrapper<MemberTagGroupRel> relByTagW = new LambdaQueryWrapper<>();
				relByTagW
						.eq(MemberTagGroupRel::getCompanyId, companyId)
						.eq(MemberTagGroupRel::getDistributorId, distributorScope)
						.in(MemberTagGroupRel::getTagId, matchedTagIds)
						.orderByDesc(MemberTagGroupRel::getCreated);
				List<MemberTagGroupRel> relByTag = memberTagGroupRelMapper.selectList(relByTagW);
				for (MemberTagGroupRel rel : relByTag) {
					if (rel.getGroupId() != null) {
						groupIdSet.add(rel.getGroupId());
					}
				}
			}

			if (groupIdSet.isEmpty()) {
				Map<String, Object> empty = new LinkedHashMap<>();
				empty.put("total_count", 0);
				empty.put("list", List.of());
				return empty;
			}
			baseWrapper.in(MemberTagGroup::getGroupId, groupIdSet);
		}

		long total = memberTagGroupMapper.selectCount(baseWrapper);
		int totalCount = (int) Math.min(Integer.MAX_VALUE, total);

		Page<MemberTagGroup> pageObj = new Page<>(page, pageSize, false);
		memberTagGroupMapper.selectPage(pageObj, baseWrapper);
		List<MemberTagGroup> groups = pageObj.getRecords();

		if (groups.isEmpty()) {
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("total_count", totalCount);
			data.put("list", List.of());
			return data;
		}

		List<Long> groupIds =
				groups.stream().map(MemberTagGroup::getGroupId).filter(Objects::nonNull).toList();

		LambdaQueryWrapper<MemberTagGroupRel> relW = new LambdaQueryWrapper<>();
		relW
				.eq(MemberTagGroupRel::getCompanyId, companyId)
				.eq(MemberTagGroupRel::getDistributorId, distributorScope)
				.in(MemberTagGroupRel::getGroupId, groupIds)
				.orderByDesc(MemberTagGroupRel::getCreated);
		List<MemberTagGroupRel> relRows = memberTagGroupRelMapper.selectList(relW);

		Map<Long, List<Long>> orderedTagIdsByGroup = new LinkedHashMap<>();
		for (MemberTagGroupRel rel : relRows) {
			Long gid = rel.getGroupId();
			Long tid = rel.getTagId();
			if (gid == null || tid == null) {
				continue;
			}
			orderedTagIdsByGroup.computeIfAbsent(gid, k -> new ArrayList<>()).add(tid);
		}

		List<Long> allRelTagIds =
				relRows.stream()
						.map(MemberTagGroupRel::getTagId)
						.filter(Objects::nonNull)
						.distinct()
						.toList();

		Map<Long, MemberTags> tagById = new LinkedHashMap<>();
		if (!allRelTagIds.isEmpty()) {
			LambdaQueryWrapper<MemberTags> tagW = new LambdaQueryWrapper<>();
			tagW
					.eq(MemberTags::getCompanyId, companyId)
					.eq(MemberTags::getDistributorId, distributorScope)
					.in(MemberTags::getTagId, allRelTagIds);
			List<MemberTags> tagEntities = memberTagsMapper.selectList(tagW);
			for (MemberTags t : tagEntities) {
				if (t.getTagId() != null) {
					tagById.putIfAbsent(t.getTagId(), t);
				}
			}
		}

		List<Map<String, Object>> flatList = new ArrayList<>();
		List<Map<String, Object>> listRows = new ArrayList<>();
		for (MemberTagGroup group : groups) {
			Map<String, Object> row = memberTagsCreateService.formatTagGroupListRow(group);
			List<Map<String, Object>> tags = new ArrayList<>();
			row.put("tags", tags);
			Long gid = group.getGroupId();
			if (gid != null) {
				for (Long tid : orderedTagIdsByGroup.getOrDefault(gid, List.of())) {
					MemberTags ent = tagById.get(tid);
					if (ent != null) {
						Map<String, Object> tagMap = memberTagsSnakeFormatter.toSnakeTagRow(ent);
						tags.add(tagMap);
						flatList.add(tagMap);
					}
				}
			}
			listRows.add(row);
		}

		if (!flatList.isEmpty()) {
			memberTagsListLangReadService.applyListLang(companyId, requestLangTag, flatList);
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", totalCount);
		data.put("list", listRows);
		return data;
	}

	private static boolean isBlankQueryToken(String raw) {
		if (raw == null) {
			return true;
		}
		if (raw.isEmpty()) {
			return true;
		}
		if ("0".equals(raw)) {
			return true;
		}
		return false;
	}

	private static String escapeLike(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static Long parseTagIdLong(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			long l = n.longValue();
			return l > 0L ? l : null;
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			long l = Long.parseLong(s);
			return l > 0L ? l : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
