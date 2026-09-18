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

import cn.shopex.ecshopx.members.domain.MemberTagGroup;
import cn.shopex.ecshopx.members.domain.MemberTagGroupRel;
import cn.shopex.ecshopx.members.domain.MemberTagLiveCountRow;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.mapper.MemberRelTagsMapper;
import cn.shopex.ecshopx.members.mapper.MemberTagGroupMapper;
import cn.shopex.ecshopx.members.mapper.MemberTagGroupRelMapper;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MemberTagLibraryQueryService {

	private static final String DEFAULT_TAG_COLOR = "#ff1939";
	private static final String DEFAULT_FONT_COLOR = "#ffffff";
	private static final String DEFAULT_TAG_STATUS = "online";
	private static final String DEFAULT_SOURCE = "self";
	private static final DateTimeFormatter DATETIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final MemberTagGroupMapper memberTagGroupMapper;
	private final MemberTagGroupRelMapper memberTagGroupRelMapper;
	private final MemberTagsMapper memberTagsMapper;
	private final MemberRelTagsMapper memberRelTagsMapper;

	public MemberTagLibraryQueryService(
			MemberTagGroupMapper memberTagGroupMapper,
			MemberTagGroupRelMapper memberTagGroupRelMapper,
			MemberTagsMapper memberTagsMapper,
			MemberRelTagsMapper memberRelTagsMapper) {
		this.memberTagGroupMapper = memberTagGroupMapper;
		this.memberTagGroupRelMapper = memberTagGroupRelMapper;
		this.memberTagsMapper = memberTagsMapper;
		this.memberRelTagsMapper = memberRelTagsMapper;
	}

	public List<Map<String, Object>> tagLibrary(long companyId) {
		List<MemberTagGroup> groupList =
				memberTagGroupMapper.selectList(
						new LambdaQueryWrapper<MemberTagGroup>()
								.eq(MemberTagGroup::getCompanyId, companyId)
								.and(
										w ->
												w.isNull(MemberTagGroup::getWechatGroupId)
														.or()
														.eq(MemberTagGroup::getWechatGroupId, ""))
								.select(
										MemberTagGroup::getGroupId,
										MemberTagGroup::getGroupName,
										MemberTagGroup::getDescription,
										MemberTagGroup::getCreated,
										MemberTagGroup::getUpdated));

		List<MemberTagGroupRel> relList =
				memberTagGroupRelMapper.selectList(
						new LambdaQueryWrapper<MemberTagGroupRel>()
								.eq(MemberTagGroupRel::getCompanyId, companyId)
								.orderByDesc(MemberTagGroupRel::getCreated)
								.select(MemberTagGroupRel::getGroupId, MemberTagGroupRel::getTagId));

		LinkedHashMap<Long, List<Long>> groupTagsMap = new LinkedHashMap<>();
		for (MemberTagGroupRel rel : relList) {
			Long groupId = rel.getGroupId();
			Long tagId = rel.getTagId();
			if (groupId == null || tagId == null) {
				continue;
			}
			groupTagsMap.computeIfAbsent(groupId, k -> new ArrayList<>()).add(tagId);
		}

		List<MemberTags> tagEntities =
				memberTagsMapper.selectList(
						new LambdaQueryWrapper<MemberTags>()
								.eq(MemberTags::getCompanyId, companyId)
								.and(
										w ->
												w.isNull(MemberTags::getWechatTagId)
														.or()
														.eq(MemberTags::getWechatTagId, ""))
								.orderByDesc(MemberTags::getCreated));

		LinkedHashMap<Long, MemberTags> tagsMap = new LinkedHashMap<>();
		for (MemberTags tag : tagEntities) {
			Long tagId = tag.getTagId();
			if (tagId != null) {
				tagsMap.put(tagId, tag);
			}
		}

		List<Long> tagIds = new ArrayList<>(tagsMap.keySet());
		Map<Long, Integer> countMap = new HashMap<>();
		if (!tagIds.isEmpty()) {
			List<MemberTagLiveCountRow> counts =
					memberRelTagsMapper.selectLiveMemberCountByTagIds(companyId, tagIds);
			for (MemberTagLiveCountRow row : counts) {
				Long tid = row.getTagId();
				if (tid == null) {
					continue;
				}
				long n = row.getNum() == null ? 0L : row.getNum();
				countMap.put(tid, (int) Math.min(Integer.MAX_VALUE, n));
			}
		}

		log.info(
				"[tagLibrary] 已过滤企业微信标签和标签组, company_id={}, group_count={}, tag_count={}",
				companyId,
				groupList.size(),
				tagsMap.size());

		List<Map<String, Object>> result = new ArrayList<>();
		for (MemberTagGroup group : groupList) {
			Long groupId = group.getGroupId();
			List<Map<String, Object>> taglist = new ArrayList<>();
			List<Long> relTagIds = groupTagsMap.getOrDefault(groupId, List.of());
			for (Long tagId : relTagIds) {
				MemberTags tag = tagsMap.get(tagId);
				if (tag != null) {
					taglist.add(formatTagRow(tag, countMap));
				}
			}
			result.add(formatGroupRow(group, taglist));
		}

		Set<Long> groupedTagIds = new HashSet<>();
		for (List<Long> ids : groupTagsMap.values()) {
			groupedTagIds.addAll(ids);
		}
		List<Map<String, Object>> ungroupedTags = new ArrayList<>();
		for (Map.Entry<Long, MemberTags> e : tagsMap.entrySet()) {
			if (!groupedTagIds.contains(e.getKey())) {
				ungroupedTags.add(formatTagRow(e.getValue(), countMap));
			}
		}
		if (!ungroupedTags.isEmpty()) {
			LinkedHashMap<String, Object> virtualGroup = new LinkedHashMap<>();
			virtualGroup.put("group_id", 0L);
			virtualGroup.put("group_name", "未分组");
			virtualGroup.put("description", "未分配到任何标签组的标签");
			virtualGroup.put("created", "");
			virtualGroup.put("updated", "");
			virtualGroup.put("taglist", ungroupedTags);
			result.add(virtualGroup);
		}
		return result;
	}

	private Map<String, Object> formatGroupRow(
			MemberTagGroup group, List<Map<String, Object>> taglist) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("group_id", group.getGroupId());
		row.put("group_name", group.getGroupName());
		row.put("description", nullToEmpty(group.getDescription()));
		row.put("created", formatTimestamp(group.getCreated()));
		row.put("updated", formatTimestamp(group.getUpdated()));
		row.put("taglist", taglist);
		return row;
	}

	private Map<String, Object> formatTagRow(MemberTags tag, Map<Long, Integer> countMap) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("tag_id", tag.getTagId());
		row.put("tag_name", tag.getTagName());
		row.put("description", nullToEmpty(tag.getDescription()));
		row.put("tag_color", defaultIfBlank(tag.getTagColor(), DEFAULT_TAG_COLOR));
		row.put("font_color", defaultIfBlank(tag.getFontColor(), DEFAULT_FONT_COLOR));
		row.put("tag_icon", nullToEmpty(tag.getTagIcon()));
		row.put("tag_status", defaultIfBlank(tag.getTagStatus(), DEFAULT_TAG_STATUS));
		row.put("source", defaultIfBlank(tag.getSource(), DEFAULT_SOURCE));
		row.put("self_tag_count", countMap.getOrDefault(tag.getTagId(), 0));
		row.put("created", formatTimestamp(tag.getCreated()));
		row.put("updated", formatTimestamp(tag.getUpdated()));
		return row;
	}

	private static String formatTimestamp(Long epochSeconds) {
		if (epochSeconds == null) {
			return "";
		}
		return DATETIME_FMT.format(Instant.ofEpochSecond(epochSeconds));
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	private static String defaultIfBlank(String s, String defaultVal) {
		return (s == null || s.isBlank()) ? defaultVal : s;
	}
}
