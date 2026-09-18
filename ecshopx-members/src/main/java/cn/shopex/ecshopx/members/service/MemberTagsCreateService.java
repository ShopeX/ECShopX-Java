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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.MemberRelTags;
import cn.shopex.ecshopx.members.domain.MemberTagGroup;
import cn.shopex.ecshopx.members.domain.MemberTagGroupRel;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.mapper.MemberRelTagsMapper;
import cn.shopex.ecshopx.members.mapper.MemberTagGroupMapper;
import cn.shopex.ecshopx.members.mapper.MemberTagGroupRelMapper;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MemberTagsCreateService {

	private final MemberTagsMapper memberTagsMapper;
	private final MemberTagGroupMapper memberTagGroupMapper;
	private final MemberTagGroupRelMapper memberTagGroupRelMapper;
	private final MemberRelTagsMapper memberRelTagsMapper;
	private final JdbcTemplate jdbcTemplate;
	private final MemberTagsMultiLangWriteService memberTagsMultiLangWriteService;
	private final MemberTagsSnakeFormatter memberTagsSnakeFormatter;
	private final MemberTagGroupDeleteTxService memberTagGroupDeleteTxService;

	public MemberTagsCreateService(
			MemberTagsMapper memberTagsMapper,
			MemberTagGroupMapper memberTagGroupMapper,
			MemberTagGroupRelMapper memberTagGroupRelMapper,
			MemberRelTagsMapper memberRelTagsMapper,
			JdbcTemplate jdbcTemplate,
			MemberTagsMultiLangWriteService memberTagsMultiLangWriteService,
			MemberTagsSnakeFormatter memberTagsSnakeFormatter,
			MemberTagGroupDeleteTxService memberTagGroupDeleteTxService) {
		this.memberTagsMapper = memberTagsMapper;
		this.memberTagGroupMapper = memberTagGroupMapper;
		this.memberTagGroupRelMapper = memberTagGroupRelMapper;
		this.memberRelTagsMapper = memberRelTagsMapper;
		this.jdbcTemplate = jdbcTemplate;
		this.memberTagsMultiLangWriteService = memberTagsMultiLangWriteService;
		this.memberTagsSnakeFormatter = memberTagsSnakeFormatter;
		this.memberTagGroupDeleteTxService = memberTagGroupDeleteTxService;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createTags(long companyId, long distributorId, Map<String, Object> merged, String requestLangTag) {
		Object tn = merged.get("tag_name");
		if (tn == null || !StringUtils.hasText(tn.toString().trim())) {
			throw new BadRequestException("标签名称不能为空");
		}
		String tagNameTrim = tn.toString().trim();
		assertNoDuplicateTagName(companyId, distributorId, tagNameTrim);

		String tagColor = resolveStringOrDefault(merged.get("tag_color"), "#ff1939");
		String fontColor = resolveStringOrDefault(merged.get("font_color"), "#ffffff");
		String description = resolveNullableDescription(merged.get("description"));
		int categoryId = parseCategoryId(merged.get("category_id"));
		Long groupIdParsed = parseGroupIdForBinding(merged.get("group_id"));

		long nowSec = System.currentTimeMillis() / 1000L;
		MemberTags row = new MemberTags();
		row.setCompanyId(companyId);
		row.setDistributorId(distributorId);
		row.setTagName(tagNameTrim);
		row.setDescription(description);
		row.setTagColor(tagColor);
		row.setFontColor(fontColor);
		row.setCategoryId(categoryId);
		row.setCreated(nowSec);
		row.setUpdated(nowSec);

		try {
			memberTagsMapper.insert(row);
		} catch (DataAccessException e) {
			throw mapDataAccess(e);
		}
		if (row.getTagId() == null) {
			throw new ResourceException("创建失败");
		}

		try {
			memberTagsMultiLangWriteService.afterTagCreate(row.getTagId(), companyId, merged, requestLangTag);
		} catch (DataAccessException e) {
			throw mapDataAccess(e);
		}

		if (groupIdParsed != null && groupIdParsed > 0) {
			LambdaQueryWrapper<MemberTagGroup> gw = new LambdaQueryWrapper<MemberTagGroup>()
					.eq(MemberTagGroup::getGroupId, groupIdParsed)
					.eq(MemberTagGroup::getCompanyId, companyId)
					.eq(MemberTagGroup::getDistributorId, distributorId);
			if (memberTagGroupMapper.selectOne(gw) == null) {
				throw new ResourceException("标签组不存在或不属于当前公司");
			}
			LambdaQueryWrapper<MemberTagGroupRel> rw = new LambdaQueryWrapper<MemberTagGroupRel>()
					.eq(MemberTagGroupRel::getGroupId, groupIdParsed)
					.eq(MemberTagGroupRel::getTagId, row.getTagId())
					.eq(MemberTagGroupRel::getCompanyId, companyId)
					.eq(MemberTagGroupRel::getDistributorId, distributorId);
			if (memberTagGroupRelMapper.selectOne(rw) == null) {
				MemberTagGroupRel rel = new MemberTagGroupRel();
				rel.setGroupId(groupIdParsed);
				rel.setTagId(row.getTagId());
				rel.setCompanyId(companyId);
				rel.setDistributorId(distributorId);
				rel.setCreated(nowSec);
				try {
					memberTagGroupRelMapper.insert(rel);
				} catch (DataAccessException e) {
					throw mapDataAccess(e);
				}
			}
		}

		return memberTagsSnakeFormatter.toSnakeTagRow(row);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createTagGroup(
			long companyId, long distributorId, Map<String, Object> merged, String requestLangTag) {
		Object gn = merged.get("group_name");
		String groupName = gn == null ? "" : gn.toString().trim();
		if (!StringUtils.hasText(groupName)) {
			throw new ResourceException("标签组名称不能为空");
		}
		String description = merged.get("description") == null ? "" : merged.get("description").toString();

		List<Long> tagIdList = normalizeTagIds(merged.get("tag_ids"));
		long nowSec = System.currentTimeMillis() / 1000L;

		LambdaQueryWrapper<MemberTagGroup> countW = new LambdaQueryWrapper<MemberTagGroup>()
				.eq(MemberTagGroup::getCompanyId, companyId)
				.eq(MemberTagGroup::getDistributorId, distributorId);
		Long existingGroupCountBoxed = memberTagGroupMapper.selectCount(countW);
		long existingGroupCount = existingGroupCountBoxed == null ? 0L : existingGroupCountBoxed.longValue();

		if (existingGroupCount == 0) {
			LambdaQueryWrapper<MemberTags> tw = new LambdaQueryWrapper<MemberTags>()
					.eq(MemberTags::getCompanyId, companyId)
					.eq(MemberTags::getDistributorId, distributorId);
			List<MemberTags> allTags;
			try {
				allTags = memberTagsMapper.selectList(tw);
			} catch (DataAccessException e) {
				throw mapDataAccess(e);
			}
			if (allTags != null && !allTags.isEmpty()) {
				List<Long> defaultTagIds = new ArrayList<>();
				for (MemberTags t : allTags) {
					if (t.getTagId() != null) {
						defaultTagIds.add(t.getTagId());
					}
				}
				if (!defaultTagIds.isEmpty()) {
					MemberTagGroup defaultGroup = new MemberTagGroup();
					defaultGroup.setGroupName("默认标签组");
					defaultGroup.setDescription("系统自动创建，包含当前全部标签");
					defaultGroup.setCompanyId(companyId);
					defaultGroup.setDistributorId(distributorId);
					defaultGroup.setCreated(nowSec);
					defaultGroup.setUpdated(nowSec);
					try {
						memberTagGroupMapper.insert(defaultGroup);
					} catch (DataAccessException e) {
						throw mapDataAccess(e);
					}
					if (defaultGroup.getGroupId() == null) {
						throw new ResourceException("创建失败");
					}
					bindTagsToGroup(defaultGroup.getGroupId(), defaultTagIds, companyId, distributorId, nowSec);
				}
			}
		}

		MemberTagGroup group = new MemberTagGroup();
		group.setGroupName(groupName);
		group.setDescription(description);
		group.setCompanyId(companyId);
		group.setDistributorId(distributorId);
		group.setCreated(nowSec);
		group.setUpdated(nowSec);
		try {
			memberTagGroupMapper.insert(group);
		} catch (DataAccessException e) {
			throw mapDataAccess(e);
		}
		if (group.getGroupId() == null) {
			throw new ResourceException("创建失败");
		}

		if (!tagIdList.isEmpty()) {
			LambdaQueryWrapper<MemberTags> inw = new LambdaQueryWrapper<MemberTags>()
					.eq(MemberTags::getCompanyId, companyId)
					.eq(MemberTags::getDistributorId, distributorId)
					.in(MemberTags::getTagId, tagIdList);
			List<MemberTags> valid;
			try {
				valid = memberTagsMapper.selectList(inw);
			} catch (DataAccessException e) {
				throw mapDataAccess(e);
			}
			List<Long> validTagIds =
					valid.stream().map(MemberTags::getTagId).filter(Objects::nonNull).distinct().toList();
			if (!validTagIds.isEmpty()) {
				bindTagsToGroup(group.getGroupId(), validTagIds, companyId, distributorId, nowSec);
			}
		}

		Object tagsRaw = merged.get("tags");
		if (tagsRaw instanceof List<?> tagList && !tagList.isEmpty()) {
			for (Object item : tagList) {
				if (!(item instanceof Map<?, ?> mapItem)) {
					throw new ResourceException("标签名称不能为空");
				}
				Map<String, Object> tagData = new LinkedHashMap<>();
				mapItem.forEach((k, v) -> tagData.put(String.valueOf(k), v));
				Object tname = tagData.get("tag_name");
				if (tname == null || !StringUtils.hasText(tname.toString().trim())) {
					throw new ResourceException("标签名称不能为空");
				}
				createTagWithGroupInSameTx(companyId, distributorId, group.getGroupId(), tagData, requestLangTag);
			}
		}

		MemberTagGroup persisted;
		try {
			persisted = memberTagGroupMapper.selectById(group.getGroupId());
		} catch (DataAccessException e) {
			throw mapDataAccess(e);
		}
		if (persisted == null) {
			throw new ResourceException("创建失败");
		}
		return toSnakeGroupRow(persisted);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateTagGroup(
			long companyId,
			long distributorId,
			long groupId,
			Map<String, Object> merged,
			String requestLangTag) {
		if (groupId <= 0) {
			throw new ResourceException("标签组ID不能为空");
		}
		Object gn = merged.get("group_name");
		String groupName = gn == null ? "" : gn.toString().trim();
		if (!StringUtils.hasText(groupName)) {
			throw new ResourceException("标签组名称不能为空");
		}

		LambdaQueryWrapper<MemberTagGroup> gw = new LambdaQueryWrapper<MemberTagGroup>()
				.eq(MemberTagGroup::getGroupId, groupId)
				.eq(MemberTagGroup::getCompanyId, companyId)
				.eq(MemberTagGroup::getDistributorId, distributorId);
		MemberTagGroup groupRow;
		try {
			groupRow = memberTagGroupMapper.selectOne(gw);
		} catch (DataAccessException e) {
			throw mapDataAccess(e);
		}
		if (groupRow == null) {
			throw new ResourceException("标签组不存在");
		}

		long nowSec = System.currentTimeMillis() / 1000L;
		groupRow.setGroupName(groupName);
		if (merged.containsKey("description")) {
			groupRow.setDescription(merged.get("description") == null ? null : merged.get("description").toString());
		}
		groupRow.setUpdated(nowSec);
		try {
			memberTagGroupMapper.updateById(groupRow);
		} catch (DataAccessException e) {
			throw mapDataAccess(e);
		}

		List<Long> deleteIdList = normalizeDeleteIds(merged.get("deleteids"));
		for (Long tagId : deleteIdList) {
			if (tagId == null || tagId <= 0) {
				continue;
			}
			LambdaQueryWrapper<MemberRelTags> relCountW = new LambdaQueryWrapper<MemberRelTags>()
					.eq(MemberRelTags::getCompanyId, companyId)
					.eq(MemberRelTags::getTagId, tagId);
			Long relCountBoxed;
			try {
				relCountBoxed = memberRelTagsMapper.selectCount(relCountW);
			} catch (DataAccessException e) {
				throw mapDataAccess(e);
			}
			long relCount = relCountBoxed == null ? 0L : relCountBoxed.longValue();
			if (relCount > 0) {
				try {
					memberRelTagsMapper.delete(relCountW);
				} catch (DataAccessException e) {
					throw mapDataAccess(e);
				}
				try {
					jdbcTemplate.update(
							"UPDATE members_tags SET self_tag_count = GREATEST(0, self_tag_count - ?) WHERE tag_id = ? AND company_id = ?",
							relCount,
							tagId,
							companyId);
				} catch (DataAccessException e) {
					throw mapDataAccess(e);
				}
			}
			LambdaQueryWrapper<MemberTagGroupRel> delRel = new LambdaQueryWrapper<MemberTagGroupRel>()
					.eq(MemberTagGroupRel::getGroupId, groupId)
					.eq(MemberTagGroupRel::getTagId, tagId)
					.eq(MemberTagGroupRel::getCompanyId, companyId)
					.eq(MemberTagGroupRel::getDistributorId, distributorId);
			try {
				memberTagGroupRelMapper.delete(delRel);
			} catch (DataAccessException e) {
				throw mapDataAccess(e);
			}
			LambdaQueryWrapper<MemberTagGroupRel> cntW = new LambdaQueryWrapper<MemberTagGroupRel>()
					.eq(MemberTagGroupRel::getTagId, tagId)
					.eq(MemberTagGroupRel::getCompanyId, companyId)
					.eq(MemberTagGroupRel::getDistributorId, distributorId)
					.ne(MemberTagGroupRel::getGroupId, groupId);
			Long otherBoxed;
			try {
				otherBoxed = memberTagGroupRelMapper.selectCount(cntW);
			} catch (DataAccessException e) {
				throw mapDataAccess(e);
			}
			long other = otherBoxed == null ? 0L : otherBoxed.longValue();
			if (other == 0) {
				LambdaQueryWrapper<MemberTags> delTag = new LambdaQueryWrapper<MemberTags>()
						.eq(MemberTags::getTagId, tagId)
						.eq(MemberTags::getCompanyId, companyId)
						.eq(MemberTags::getDistributorId, distributorId);
				try {
					memberTagsMapper.delete(delTag);
				} catch (DataAccessException e) {
					throw mapDataAccess(e);
				}
			}
		}

		Object tagsRaw = merged.get("tags");
		if (tagsRaw instanceof List<?> tagList && !tagList.isEmpty()) {
			for (Object item : tagList) {
				if (!(item instanceof Map<?, ?> mapItem)) {
					throw new ResourceException("标签名称不能为空");
				}
				Map<String, Object> tagData = new LinkedHashMap<>();
				mapItem.forEach((k, v) -> tagData.put(String.valueOf(k), v));
				Object tname = tagData.get("tag_name");
				if (tname == null || !StringUtils.hasText(tname.toString().trim())) {
					throw new ResourceException("标签名称不能为空");
				}
				String tagNameTrim = tname.toString().trim();
				long tagIdForUpsert = parseTagIdForUpsert(tagData.get("tag_id"));
				if (tagIdForUpsert == 0L) {
					createTagWithGroupInSameTx(companyId, distributorId, groupId, tagData, requestLangTag);
				} else {
					LambdaQueryWrapper<MemberTags> tagW = new LambdaQueryWrapper<MemberTags>()
							.eq(MemberTags::getTagId, tagIdForUpsert)
							.eq(MemberTags::getCompanyId, companyId)
							.eq(MemberTags::getDistributorId, distributorId);
					MemberTags entity;
					try {
						entity = memberTagsMapper.selectOne(tagW);
					} catch (DataAccessException e) {
						throw mapDataAccess(e);
					}
					if (entity == null) {
						throw new ResourceException("标签不存在或不属于当前公司");
					}
					assertNoDuplicateTagNameExcluding(companyId, distributorId, tagNameTrim, tagIdForUpsert);
					entity.setTagName(tagNameTrim);
					if (tagData.containsKey("tag_color")) {
						entity.setTagColor(resolveStringOrDefault(tagData.get("tag_color"), entity.getTagColor()));
					}
					if (tagData.containsKey("font_color")) {
						entity.setFontColor(resolveStringOrDefault(tagData.get("font_color"), entity.getFontColor()));
					}
					if (tagData.containsKey("description")) {
						entity.setDescription(resolveNullableDescription(tagData.get("description")));
					}
					if (tagData.containsKey("category_id")) {
						entity.setCategoryId(parseCategoryId(tagData.get("category_id")));
					}
					entity.setUpdated(nowSec);
					try {
						memberTagsMapper.updateById(entity);
					} catch (DataAccessException e) {
						throw mapDataAccess(e);
					}
					bindTagsToGroup(groupId, List.of(tagIdForUpsert), companyId, distributorId, nowSec);
				}
			}
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("group_id", groupId);
		out.put("group_name", groupName);
		return out;
	}

	private static List<Long> normalizeDeleteIds(Object raw) {
		return normalizeTagIds(raw);
	}

	private void assertNoDuplicateTagNameExcluding(
			long companyId, long distributorId, String tagNameTrim, long excludeTagId) {
		LambdaQueryWrapper<MemberTags> dup = new LambdaQueryWrapper<MemberTags>()
				.eq(MemberTags::getCompanyId, companyId)
				.eq(MemberTags::getDistributorId, distributorId)
				.eq(MemberTags::getTagName, tagNameTrim)
				.ne(MemberTags::getTagId, excludeTagId);
		if (memberTagsMapper.selectOne(dup) != null) {
			throw new ResourceException("标签名称不能重复");
		}
	}

	private static long parseTagIdForUpsert(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t) || "0".equals(t)) {
				return 0L;
			}
			try {
				long v = Long.parseLong(t);
				if (v < 0L) {
					throw new ResourceException("标签不存在或不属于当前公司");
				}
				return v;
			} catch (NumberFormatException e) {
				throw new ResourceException("标签不存在或不属于当前公司");
			}
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			if (v == 0L) {
				return 0L;
			}
			if (v < 0L) {
				throw new ResourceException("标签不存在或不属于当前公司");
			}
			return v;
		}
		String t = raw.toString().trim();
		if (!StringUtils.hasText(t) || "0".equals(t)) {
			return 0L;
		}
		try {
			long v = Long.parseLong(t);
			if (v < 0L) {
				throw new ResourceException("标签不存在或不属于当前公司");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new ResourceException("标签不存在或不属于当前公司");
		}
	}

	private static List<Long> normalizeTagIds(Object raw) {
		if (raw == null) {
			return List.of();
		}
		LinkedHashSet<Long> out = new LinkedHashSet<>();
		if (raw instanceof Collection<?> coll) {
			for (Object o : coll) {
				appendNormalizedTagId(out, o);
			}
			return new ArrayList<>(out);
		}
		if (raw instanceof Object[] arr) {
			for (Object o : arr) {
				appendNormalizedTagId(out, o);
			}
			return new ArrayList<>(out);
		}
		if (raw instanceof long[] arr) {
			for (long v : arr) {
				if (v > 0) {
					out.add(v);
				}
			}
			return new ArrayList<>(out);
		}
		if (raw instanceof int[] arr) {
			for (int v : arr) {
				if (v > 0) {
					out.add((long) v);
				}
			}
			return new ArrayList<>(out);
		}
		if (raw instanceof Number n) {
			appendNormalizedTagId(out, n);
			return new ArrayList<>(out);
		}
		if (raw instanceof String str) {
			appendNormalizedTagId(out, str);
			return new ArrayList<>(out);
		}
		return List.of();
	}

	private static void appendNormalizedTagId(LinkedHashSet<Long> out, Object o) {
		if (o == null) {
			return;
		}
		if (o instanceof Number n) {
			long v = n.longValue();
			if (v > 0) {
				out.add(v);
			}
			return;
		}
		String s = String.valueOf(o).trim();
		if (!StringUtils.hasText(s)) {
			return;
		}
		try {
			long v = Long.parseLong(s);
			if (v > 0) {
				out.add(v);
			}
		} catch (NumberFormatException e) {
			// ignore invalid element
		}
	}

	private void bindTagsToGroup(
			long groupId, List<Long> tagIds, long companyId, long distributorId, long nowSec) {
		for (Long tagId : tagIds) {
			if (tagId == null || tagId <= 0) {
				continue;
			}
			LambdaQueryWrapper<MemberTagGroupRel> rw = new LambdaQueryWrapper<MemberTagGroupRel>()
					.eq(MemberTagGroupRel::getGroupId, groupId)
					.eq(MemberTagGroupRel::getTagId, tagId)
					.eq(MemberTagGroupRel::getCompanyId, companyId)
					.eq(MemberTagGroupRel::getDistributorId, distributorId);
			if (memberTagGroupRelMapper.selectOne(rw) != null) {
				continue;
			}
			MemberTagGroupRel rel = new MemberTagGroupRel();
			rel.setGroupId(groupId);
			rel.setTagId(tagId);
			rel.setCompanyId(companyId);
			rel.setDistributorId(distributorId);
			rel.setCreated(nowSec);
			try {
				memberTagGroupRelMapper.insert(rel);
			} catch (DataAccessException e) {
				throw mapDataAccess(e);
			}
		}
	}

	private void createTagWithGroupInSameTx(
			long companyId,
			long distributorId,
			long newGroupId,
			Map<String, Object> tagData,
			String requestLangTag) {
		Object tn = tagData.get("tag_name");
		String tagNameTrim = tn.toString().trim();
		assertNoDuplicateTagName(companyId, distributorId, tagNameTrim);
		String tagColor = resolveStringOrDefault(tagData.get("tag_color"), "#ff1939");
		String fontColor = resolveStringOrDefault(tagData.get("font_color"), "#ffffff");
		String tagDescription = resolveNullableDescription(tagData.get("description"));
		int categoryId = parseCategoryId(tagData.get("category_id"));
		long nowSec = System.currentTimeMillis() / 1000L;
		MemberTags row = new MemberTags();
		row.setCompanyId(companyId);
		row.setDistributorId(distributorId);
		row.setTagName(tagNameTrim);
		row.setDescription(tagDescription);
		row.setTagColor(tagColor);
		row.setFontColor(fontColor);
		row.setCategoryId(categoryId);
		row.setCreated(nowSec);
		row.setUpdated(nowSec);
		try {
			memberTagsMapper.insert(row);
		} catch (DataAccessException e) {
			throw mapDataAccess(e);
		}
		if (row.getTagId() == null) {
			throw new ResourceException("创建失败");
		}
		try {
			memberTagsMultiLangWriteService.afterTagCreate(row.getTagId(), companyId, tagData, requestLangTag);
		} catch (DataAccessException e) {
			throw mapDataAccess(e);
		}
		LambdaQueryWrapper<MemberTagGroup> gw = new LambdaQueryWrapper<MemberTagGroup>()
				.eq(MemberTagGroup::getGroupId, newGroupId)
				.eq(MemberTagGroup::getCompanyId, companyId)
				.eq(MemberTagGroup::getDistributorId, distributorId);
		if (memberTagGroupMapper.selectOne(gw) == null) {
			throw new ResourceException("标签组不存在或不属于当前公司");
		}
		bindTagsToGroup(newGroupId, List.of(row.getTagId()), companyId, distributorId, nowSec);
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteTag(long companyId, long distributorId, String pathTagId) {
		QueryWrapper<MemberTags> wrapper = new QueryWrapper<>();
		wrapper.eq("tag_id", pathTagId == null ? "" : pathTagId.trim());
		wrapper.eq("company_id", companyId);
		wrapper.eq("distributor_id", distributorId);
		List<MemberTags> list;
		try {
			list = memberTagsMapper.selectList(wrapper);
		} catch (DataAccessException e) {
			throw mapDataAccess(e);
		}
		if (list == null || list.isEmpty()) {
			return;
		}
		for (MemberTags row : list) {
			if (row.getTagId() == null) {
				continue;
			}
			Long id = row.getTagId();
			memberTagsMultiLangWriteService.deleteMultiLangRowsForMembersTags(id);
			memberTagsMapper.deleteById(id);
		}
	}

	public void deleteTagGroup(long companyId, long distributorId, long groupId) {
		LambdaQueryWrapper<MemberTagGroup> gw = new LambdaQueryWrapper<MemberTagGroup>()
				.eq(MemberTagGroup::getGroupId, groupId)
				.eq(MemberTagGroup::getCompanyId, companyId)
				.eq(MemberTagGroup::getDistributorId, distributorId);
		MemberTagGroup groupRow;
		try {
			groupRow = memberTagGroupMapper.selectOne(gw);
		} catch (DataAccessException e) {
			throw mapDataAccess(e);
		}
		if (groupRow == null) {
			return;
		}

		List<MemberTagGroupRel> rels;
		try {
			LambdaQueryWrapper<MemberTagGroupRel> rw = new LambdaQueryWrapper<MemberTagGroupRel>()
					.eq(MemberTagGroupRel::getGroupId, groupId)
					.eq(MemberTagGroupRel::getCompanyId, companyId)
					.eq(MemberTagGroupRel::getDistributorId, distributorId);
			rels = memberTagGroupRelMapper.selectList(rw);
		} catch (DataAccessException e) {
			throw mapDataAccess(e);
		}
		List<Long> tagIds = rels == null
				? List.of()
				: rels.stream()
						.map(MemberTagGroupRel::getTagId)
						.filter(Objects::nonNull)
						.distinct()
						.toList();

		if (!tagIds.isEmpty()) {
			LambdaQueryWrapper<MemberRelTags> cntW = new LambdaQueryWrapper<MemberRelTags>()
					.eq(MemberRelTags::getCompanyId, companyId)
					.in(MemberRelTags::getTagId, tagIds);
			Long countBoxed;
			try {
				countBoxed = memberRelTagsMapper.selectCount(cntW);
			} catch (DataAccessException e) {
				throw mapDataAccess(e);
			}
			if (countBoxed != null && countBoxed > 0) {
				throw new ResourceException("该标签组下的标签已绑定会员，无法删除");
			}
		}

		memberTagGroupDeleteTxService.deleteTagGroupInTransaction(companyId, distributorId, groupId, tagIds);
	}

	public Map<String, Object> formatTagGroupListRow(MemberTagGroup g) {
		return toSnakeGroupRow(g);
	}

	private static Map<String, Object> toSnakeGroupRow(MemberTagGroup g) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		Long gid = g.getGroupId();
		m.put("group_id", gid == null ? null : String.valueOf(gid));
		m.put("group_name", g.getGroupName());
		m.put("description", g.getDescription());
		Long cid = g.getCompanyId();
		m.put("company_id", cid != null && cid <= Integer.MAX_VALUE ? cid.intValue() : cid);
		Long did = g.getDistributorId();
		m.put("distributor_id", did != null && did <= Integer.MAX_VALUE ? did.intValue() : did);
		m.put("wechat_group_id", g.getWechatGroupId());
		Long cr = g.getCreated();
		m.put("created", cr != null && cr <= Integer.MAX_VALUE ? cr.intValue() : cr);
		Long up = g.getUpdated();
		m.put("updated", up != null && up <= Integer.MAX_VALUE ? up.intValue() : up);
		return m;
	}

	private void assertNoDuplicateTagName(long companyId, long distributorId, String tagNameTrim) {
		LambdaQueryWrapper<MemberTags> dup = new LambdaQueryWrapper<MemberTags>()
				.eq(MemberTags::getCompanyId, companyId)
				.eq(MemberTags::getDistributorId, distributorId)
				.eq(MemberTags::getTagName, tagNameTrim);
		if (memberTagsMapper.selectOne(dup) != null) {
			throw new ResourceException("标签名称不能重复");
		}
	}

	private static String resolveStringOrDefault(Object raw, String defaultVal) {
		if (raw == null) {
			return defaultVal;
		}
		String s = raw.toString().trim();
		return StringUtils.hasText(s) ? s : defaultVal;
	}

	private static String resolveNullableDescription(Object raw) {
		if (raw == null) {
			return null;
		}
		String s = raw.toString().trim();
		return StringUtils.hasText(s) ? s : null;
	}

	private static int parseCategoryId(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			return 0;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static Long parseGroupIdForBinding(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof String str) {
			String g = str.trim();
			if (!StringUtils.hasText(g)) {
				return null;
			}
			if ("0".equals(g)) {
				return null;
			}
			try {
				long v = Long.parseLong(g);
				return v > 0 ? v : null;
			} catch (NumberFormatException e) {
				throw new ResourceException("标签组不存在或不属于当前公司");
			}
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			if (v == 0L) {
				return null;
			}
			return v > 0 ? v : null;
		}
		String g = raw.toString().trim();
		if (!StringUtils.hasText(g)) {
			return null;
		}
		if ("0".equals(g)) {
			return null;
		}
		try {
			long v = Long.parseLong(g);
			return v > 0 ? v : null;
		} catch (NumberFormatException e) {
			throw new ResourceException("标签组不存在或不属于当前公司");
		}
	}

	static ResourceException mapDataAccess(DataAccessException e) {
		Throwable c = e.getMostSpecificCause();
		String msg = c != null ? c.getMessage() : null;
		return new ResourceException(StringUtils.hasText(msg) ? msg : "创建失败");
	}

}
