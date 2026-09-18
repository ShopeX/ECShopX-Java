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

package cn.shopex.ecshopx.members.service.reltag;

import cn.shopex.ecshopx.members.dispatch.PushMemberTagRelationJobDispatchPublisher;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class MemberTagRelationPushCoordinator {

	private static final Logger log = LoggerFactory.getLogger(MemberTagRelationPushCoordinator.class);

	private static final int ASYNC_THRESHOLD = 50;

	private final MembersMapper membersMapper;
	private final MemberTagsMapper memberTagsMapper;
	private final PushMemberTagRelationJobDispatchPublisher pushMemberTagRelationJobDispatchPublisher;
	private final MemberTagRelationMarketingSyncService memberTagRelationMarketingSyncService;

	public MemberTagRelationPushCoordinator(
			MembersMapper membersMapper,
			MemberTagsMapper memberTagsMapper,
			PushMemberTagRelationJobDispatchPublisher pushMemberTagRelationJobDispatchPublisher,
			MemberTagRelationMarketingSyncService memberTagRelationMarketingSyncService) {
		this.membersMapper = membersMapper;
		this.memberTagsMapper = memberTagsMapper;
		this.pushMemberTagRelationJobDispatchPublisher = pushMemberTagRelationJobDispatchPublisher;
		this.memberTagRelationMarketingSyncService = memberTagRelationMarketingSyncService;
	}

	public void afterCommitPushAdd(long companyId, List<Long> userIds, List<Long> tagIds) {
		try {
			List<Map<String, Object>> relations = buildRelationsCartesian(companyId, userIds, tagIds);
			if (relations.isEmpty()) {
				return;
			}
			if (relations.size() > ASYNC_THRESHOLD) {
				pushMemberTagRelationJobDispatchPublisher.enqueuePushMemberTagRelation(companyId, "add", relations);
			} else {
				memberTagRelationMarketingSyncService.syncRelationBatchesToShoppingGuide(companyId, "add", relations);
			}
		} catch (Exception e) {
			log.warn(
					"[createRelTags] marketing push failed company_id={} user_ids={} tag_ids={} error={}",
					companyId,
					userIds,
					tagIds,
					e.toString());
		}
	}

	public void afterCommitPushRemove(long companyId, long userId, long tagId) {
		try {
			List<Map<String, Object>> relations = buildRelationsCartesian(companyId, List.of(userId), List.of(tagId));
			if (relations.isEmpty()) {
				return;
			}
			if (relations.size() > ASYNC_THRESHOLD) {
				pushMemberTagRelationJobDispatchPublisher.enqueuePushMemberTagRelation(companyId, "remove", relations);
			} else {
				memberTagRelationMarketingSyncService.syncRelationBatchesToShoppingGuide(
						companyId, "remove", relations);
			}
		} catch (Exception e) {
			log.warn(
					"[delRelMemberTag] marketing push failed company_id={} user_id={} tag_id={} error={}",
					companyId,
					userId,
					tagId,
					e.toString());
		}
	}

	private List<Map<String, Object>> buildRelationsCartesian(long companyId, List<Long> userIds, List<Long> tagIds) {
		if (CollectionUtils.isEmpty(userIds) || CollectionUtils.isEmpty(tagIds)) {
			return List.of();
		}
		Set<Long> distinctUserIds = new LinkedHashSet<>();
		for (Long uid : userIds) {
			if (uid != null && uid > 0L) {
				distinctUserIds.add(uid);
			}
		}
		Set<Long> distinctTagIds = new LinkedHashSet<>();
		for (Long tid : tagIds) {
			if (tid != null && tid > 0L) {
				distinctTagIds.add(tid);
			}
		}
		if (distinctUserIds.isEmpty() || distinctTagIds.isEmpty()) {
			return List.of();
		}

		List<Members> members =
				membersMapper.selectList(
						new LambdaQueryWrapper<Members>()
								.eq(Members::getCompanyId, companyId)
								.in(Members::getUserId, distinctUserIds));
		if (members == null || members.isEmpty()) {
			log.warn(
					"[MemberTagRelationPush] no member rows company_id={} user_ids={}",
					companyId,
					distinctUserIds);
			return List.of();
		}

		List<MemberTags> tags =
				memberTagsMapper.selectList(
						new LambdaQueryWrapper<MemberTags>()
								.eq(MemberTags::getCompanyId, companyId)
								.in(MemberTags::getTagId, distinctTagIds));
		if (tags == null || tags.isEmpty()) {
			log.warn(
					"[MemberTagRelationPush] no tag rows company_id={} tag_ids={}",
					companyId,
					distinctTagIds);
			return List.of();
		}

		List<Map<String, Object>> relations = new ArrayList<>();
		for (Members m : members) {
			if (m.getUserId() == null) {
				continue;
			}
			for (MemberTags t : tags) {
				if (t.getTagId() == null) {
					continue;
				}
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("user_id", String.valueOf(m.getUserId()));
				row.put("mobile", m.getMobile() != null ? m.getMobile() : "");
				row.put("tag_id", String.valueOf(t.getTagId()));
				row.put("tag_name", t.getTagName() != null ? t.getTagName() : "");
				row.put("tag_type", tagTypeFromSource(t.getSource()));
				row.put("wechat_tag_id", t.getWechatTagId());
				relations.add(row);
			}
		}
		if (relations.isEmpty()) {
			log.warn(
					"[MemberTagRelationPush] empty cartesian company_id={} user_ids={} tag_ids={}",
					companyId,
					distinctUserIds,
					distinctTagIds);
		}
		return relations;
	}

	private static String tagTypeFromSource(String source) {
		if ("wechat".equals(source)) {
			return "wechat";
		}
		if ("staff".equals(source)) {
			return "staff";
		}
		return "self";
	}
}
