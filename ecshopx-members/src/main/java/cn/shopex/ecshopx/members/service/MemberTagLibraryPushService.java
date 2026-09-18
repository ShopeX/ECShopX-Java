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

import cn.shopex.ecshopx.members.domain.MemberRelTags;
import cn.shopex.ecshopx.members.domain.MemberTagGroup;
import cn.shopex.ecshopx.members.domain.MemberTagGroupRel;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.mapper.MemberRelTagsMapper;
import cn.shopex.ecshopx.members.mapper.MemberTagGroupMapper;
import cn.shopex.ecshopx.members.mapper.MemberTagGroupRelMapper;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberTagLibraryPushService {

	private static final Logger log = LoggerFactory.getLogger(MemberTagLibraryPushService.class);

	private final MemberTagGroupMapper memberTagGroupMapper;
	private final MemberTagsMapper memberTagsMapper;
	private final MemberTagGroupRelMapper memberTagGroupRelMapper;
	private final MemberRelTagsMapper memberRelTagsMapper;

	public MemberTagLibraryPushService(
			MemberTagGroupMapper memberTagGroupMapper,
			MemberTagsMapper memberTagsMapper,
			MemberTagGroupRelMapper memberTagGroupRelMapper,
			MemberRelTagsMapper memberRelTagsMapper) {
		this.memberTagGroupMapper = memberTagGroupMapper;
		this.memberTagsMapper = memberTagsMapper;
		this.memberTagGroupRelMapper = memberTagGroupRelMapper;
		this.memberRelTagsMapper = memberRelTagsMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> pushTagLibrary(long companyId, List<Map<String, Object>> tagLibrary) {
		Map<String, Object> stats = newStats();
		List<String> pushedWechatGroupIds = new ArrayList<>();
		List<String> pushedWechatTagIds = new ArrayList<>();

		for (Map<String, Object> groupData : tagLibrary) {
			try {
				if (!isPhpEmpty(groupData.get("wechat_group_id"))) {
					pushedWechatGroupIds.add(stringValue(groupData.get("wechat_group_id")));
				}
				List<Map<String, Object>> taglist = resolveTaglist(groupData.get("taglist"));
				for (Map<String, Object> tagData : taglist) {
					if (!isPhpEmpty(tagData.get("wechat_tag_id"))) {
						pushedWechatTagIds.add(stringValue(tagData.get("wechat_tag_id")));
					}
				}
				processTagGroup(companyId, groupData, stats);
			} catch (Exception e) {
				log.error("[TagLibraryPushService] 处理标签组失败", e);
				inc(stats, "fail_count");
			}
		}

		if (!pushedWechatGroupIds.isEmpty()) {
			stats.put("deleted_groups", deleteObsoleteGroups(companyId, pushedWechatGroupIds));
		}
		if (!pushedWechatTagIds.isEmpty()) {
			stats.put("deleted_tags", deleteObsoleteTags(companyId, pushedWechatTagIds));
		}

		log.info("[TagLibraryPushService] 标签库推送成功 company_id={} stats={}", companyId, stats);
		return stats;
	}

	private void processTagGroup(long companyId, Map<String, Object> groupData, Map<String, Object> stats) {
		if (isPhpEmpty(groupData.get("group_name"))) {
			log.warn("[TagLibraryPushService] 标签组名称为空，跳过 data={}", groupData);
			inc(stats, "fail_count");
			return;
		}

		String groupName = stringValue(groupData.get("group_name"));
		String description = stringOrDefault(groupData, "description", "");
		String wechatGroupId = nullableString(groupData.get("wechat_group_id"));
		List<Map<String, Object>> taglist = resolveTaglist(groupData.get("taglist"));

		long groupId = findOrCreateTagGroup(companyId, groupName, description, wechatGroupId, stats);

		List<Long> processedTagIds = new ArrayList<>();
		for (Map<String, Object> tagData : taglist) {
			Long tagId = processTag(companyId, tagData, stats);
			if (tagId != null) {
				createTagGroupRelation(companyId, groupId, tagId);
				processedTagIds.add(tagId);
			}
		}

		if (!processedTagIds.isEmpty()) {
			cleanOldRelations(companyId, groupId, processedTagIds);
		}

		inc(stats, "success_count");
	}

	private long findOrCreateTagGroup(
			long companyId,
			String groupName,
			String description,
			String wechatGroupId,
			Map<String, Object> stats) {
		if (wechatGroupId != null) {
			MemberTagGroup existing =
					memberTagGroupMapper.selectOne(
							new LambdaQueryWrapper<MemberTagGroup>()
									.eq(MemberTagGroup::getCompanyId, companyId)
									.eq(MemberTagGroup::getWechatGroupId, wechatGroupId));
			if (existing != null) {
				existing.setGroupName(groupName);
				existing.setDescription(description);
				existing.setUpdated(nowSec());
				memberTagGroupMapper.updateById(existing);
				inc(stats, "updated_groups");
				return existing.getGroupId();
			}
		}

		MemberTagGroup newGroup = new MemberTagGroup();
		newGroup.setCompanyId(companyId);
		newGroup.setGroupName(groupName);
		newGroup.setDescription(description);
		newGroup.setDistributorId(0L);
		long now = nowSec();
		newGroup.setCreated(now);
		newGroup.setUpdated(now);
		if (wechatGroupId != null) {
			newGroup.setWechatGroupId(wechatGroupId);
		}
		memberTagGroupMapper.insert(newGroup);
		inc(stats, "created_groups");
		return newGroup.getGroupId();
	}

	private Long processTag(long companyId, Map<String, Object> tagData, Map<String, Object> stats) {
		if (isPhpEmpty(tagData.get("tag_name"))) {
			log.warn("[TagLibraryPushService] 标签名称为空，跳过 data={}", tagData);
			return null;
		}

		String tagName = stringValue(tagData.get("tag_name"));
		String tagDescription = stringOrDefault(tagData, "description", "");
		String wechatTagId = nullableString(tagData.get("wechat_tag_id"));
		String tagColor = stringOrDefault(tagData, "tag_color", "#3e7bff");
		String fontColor = stringOrDefault(tagData, "font_color", "#ffffff");
		String tagIcon = stringOrDefault(tagData, "tag_icon", "");
		String tagStatus = stringOrDefault(tagData, "tag_status", "online");
		String source = stringOrDefault(tagData, "source", "wechat");

		return findOrCreateTag(
				companyId,
				tagName,
				tagDescription,
				wechatTagId,
				tagColor,
				fontColor,
				tagIcon,
				tagStatus,
				source,
				stats);
	}

	private long findOrCreateTag(
			long companyId,
			String tagName,
			String tagDescription,
			String wechatTagId,
			String tagColor,
			String fontColor,
			String tagIcon,
			String tagStatus,
			String source,
			Map<String, Object> stats) {
		if (wechatTagId != null) {
			MemberTags existing =
					memberTagsMapper.selectOne(
							new LambdaQueryWrapper<MemberTags>()
									.eq(MemberTags::getCompanyId, companyId)
									.eq(MemberTags::getWechatTagId, wechatTagId));
			if (existing != null) {
				existing.setTagName(tagName);
				existing.setDescription(tagDescription);
				existing.setTagColor(tagColor);
				existing.setFontColor(fontColor);
				existing.setTagIcon(tagIcon);
				existing.setTagStatus(tagStatus);
				existing.setSource(source);
				existing.setUpdated(nowSec());
				memberTagsMapper.updateById(existing);
				inc(stats, "updated_tags");
				return existing.getTagId();
			}
		}

		MemberTags newTag = new MemberTags();
		newTag.setCompanyId(companyId);
		newTag.setTagName(tagName);
		newTag.setDescription(tagDescription);
		newTag.setTagColor(tagColor);
		newTag.setFontColor(fontColor);
		newTag.setTagIcon(tagIcon);
		newTag.setTagStatus(tagStatus);
		newTag.setSource(source);
		newTag.setDistributorId(0L);
		long now = nowSec();
		newTag.setCreated(now);
		newTag.setUpdated(now);
		if (wechatTagId != null) {
			newTag.setWechatTagId(wechatTagId);
		}
		memberTagsMapper.insert(newTag);
		inc(stats, "created_tags");
		return newTag.getTagId();
	}

	private void createTagGroupRelation(long companyId, long groupId, long tagId) {
		MemberTagGroupRel existing =
				memberTagGroupRelMapper.selectOne(
						new LambdaQueryWrapper<MemberTagGroupRel>()
								.eq(MemberTagGroupRel::getCompanyId, companyId)
								.eq(MemberTagGroupRel::getGroupId, groupId)
								.eq(MemberTagGroupRel::getTagId, tagId));
		if (existing != null) {
			return;
		}

		MemberTagGroupRel rel = new MemberTagGroupRel();
		rel.setCompanyId(companyId);
		rel.setGroupId(groupId);
		rel.setTagId(tagId);
		rel.setDistributorId(0L);
		rel.setCreated(nowSec());
		memberTagGroupRelMapper.insert(rel);
	}

	private void cleanOldRelations(long companyId, long groupId, List<Long> processedTagIds) {
		memberTagGroupRelMapper.delete(
				new LambdaQueryWrapper<MemberTagGroupRel>()
						.eq(MemberTagGroupRel::getCompanyId, companyId)
						.eq(MemberTagGroupRel::getGroupId, groupId)
						.notIn(MemberTagGroupRel::getTagId, processedTagIds));
	}

	private int deleteObsoleteGroups(long companyId, List<String> pushedWechatGroupIds) {
		List<MemberTagGroup> obsoleteGroups =
				memberTagGroupMapper.selectList(
						new LambdaQueryWrapper<MemberTagGroup>()
								.eq(MemberTagGroup::getCompanyId, companyId)
								.isNotNull(MemberTagGroup::getWechatGroupId)
								.notIn(MemberTagGroup::getWechatGroupId, pushedWechatGroupIds));

		int deletedCount = 0;
		for (MemberTagGroup group : obsoleteGroups) {
			memberTagGroupRelMapper.delete(
					new LambdaQueryWrapper<MemberTagGroupRel>()
							.eq(MemberTagGroupRel::getCompanyId, companyId)
							.eq(MemberTagGroupRel::getGroupId, group.getGroupId()));
			memberTagGroupMapper.deleteById(group.getGroupId());
			deletedCount++;
			log.info(
					"[TagLibraryPushService] 删除多余标签组 company_id={} group_id={} group_name={} wechat_group_id={}",
					companyId,
					group.getGroupId(),
					group.getGroupName(),
					group.getWechatGroupId());
		}
		return deletedCount;
	}

	private int deleteObsoleteTags(long companyId, List<String> pushedWechatTagIds) {
		List<MemberTags> obsoleteTags =
				memberTagsMapper.selectList(
						new LambdaQueryWrapper<MemberTags>()
								.eq(MemberTags::getCompanyId, companyId)
								.isNotNull(MemberTags::getWechatTagId)
								.notIn(MemberTags::getWechatTagId, pushedWechatTagIds));

		int deletedCount = 0;
		for (MemberTags tag : obsoleteTags) {
			memberTagGroupRelMapper.delete(
					new LambdaQueryWrapper<MemberTagGroupRel>()
							.eq(MemberTagGroupRel::getCompanyId, companyId)
							.eq(MemberTagGroupRel::getTagId, tag.getTagId()));
			memberRelTagsMapper.delete(
					new LambdaQueryWrapper<MemberRelTags>().eq(MemberRelTags::getTagId, tag.getTagId()));
			memberTagsMapper.deleteById(tag.getTagId());
			deletedCount++;
			log.info(
					"[TagLibraryPushService] 删除多余标签 company_id={} tag_id={} tag_name={} wechat_tag_id={}",
					companyId,
					tag.getTagId(),
					tag.getTagName(),
					tag.getWechatTagId());
		}
		return deletedCount;
	}

	private static Map<String, Object> newStats() {
		Map<String, Object> stats = new LinkedHashMap<>();
		stats.put("success_count", 0);
		stats.put("fail_count", 0);
		stats.put("created_groups", 0);
		stats.put("updated_groups", 0);
		stats.put("created_tags", 0);
		stats.put("updated_tags", 0);
		stats.put("deleted_groups", 0);
		stats.put("deleted_tags", 0);
		return stats;
	}

	private static void inc(Map<String, Object> stats, String key) {
		stats.put(key, ((Integer) stats.get(key)) + 1);
	}

	static boolean isPhpEmpty(Object raw) {
		if (raw == null) {
			return true;
		}
		if (raw instanceof String s) {
			String t = s.trim();
			return t.isEmpty() || "0".equals(t);
		}
		if (raw instanceof Number n) {
			return n.longValue() == 0L;
		}
		if (raw instanceof Boolean b) {
			return !b;
		}
		return false;
	}

	static String stringValue(Object raw) {
		return raw != null ? String.valueOf(raw) : "";
	}

	static String stringOrDefault(Map<String, Object> map, String key, String defaultValue) {
		if (map == null || !map.containsKey(key)) {
			return defaultValue;
		}
		Object raw = map.get(key);
		if (isPhpEmpty(raw)) {
			return defaultValue;
		}
		return String.valueOf(raw);
	}

	static String nullableString(Object raw) {
		return isPhpEmpty(raw) ? null : stringValue(raw);
	}

	@SuppressWarnings("unchecked")
	static List<Map<String, Object>> resolveTaglist(Object raw) {
		if (!(raw instanceof List<?> list)) {
			return Collections.emptyList();
		}
		List<Map<String, Object>> result = new ArrayList<>();
		for (Object element : list) {
			if (element instanceof Map<?, ?> map) {
				result.add((Map<String, Object>) map);
			}
		}
		return result;
	}

	static long nowSec() {
		return System.currentTimeMillis() / 1000L;
	}
}
