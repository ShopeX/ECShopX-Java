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

import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagRelationPushFailException;
import cn.shopex.ecshopx.members.domain.MemberTags;
import cn.shopex.ecshopx.members.mapper.MemberTagsMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MemberTagRelationInboundPushService {

	private static final Logger log = LoggerFactory.getLogger(MemberTagRelationInboundPushService.class);

	private final MemberAccountService memberAccountService;
	private final MemberTagsMapper memberTagsMapper;
	private final MemberRelTagsBatchCreateService memberRelTagsBatchCreateService;

	public MemberTagRelationInboundPushService(
			MemberAccountService memberAccountService,
			MemberTagsMapper memberTagsMapper,
			MemberRelTagsBatchCreateService memberRelTagsBatchCreateService) {
		this.memberAccountService = memberAccountService;
		this.memberTagsMapper = memberTagsMapper;
		this.memberRelTagsBatchCreateService = memberRelTagsBatchCreateService;
	}

	public Map<String, Object> pushTagRelations(
			long companyId,
			String action,
			String userId,
			String mobile,
			List<Map<String, Object>> relations) {
		if (relations == null || relations.isEmpty()) {
			return handleClearMode(companyId, userId, mobile);
		}
		return handleNormalMode(companyId, relations);
	}

	private Map<String, Object> handleClearMode(long companyId, String userIdRaw, String mobileRaw) {
		log.info("[tagRelationPush] 清空会员标签模式 company_id={} user_id={}", companyId, userIdRaw);
		try {
			Map<String, Object> member = findMember(companyId, userIdRaw, mobileRaw);
			if (member.isEmpty()) {
				log.warn("[tagRelationPush] 未找到会员 company_id={} user_id={}", companyId, userIdRaw);
				throw new OpenapiMemberTagRelationPushFailException("E1001", "未找到会员");
			}
			long actualUserId = ((Number) member.get("user_id")).longValue();
			memberRelTagsBatchCreateService.deleteRelTagsByUserIdNoPush(actualUserId, companyId);
			log.info(
					"[tagRelationPush] 清空会员标签成功 company_id={} user_id={}",
					companyId,
					actualUserId);
			Map<String, Object> stats = new LinkedHashMap<>();
			stats.put("success_count", 1);
			stats.put("fail_count", 0);
			stats.put("processed_members", 1);
			stats.put("processed_tags", 0);
			return stats;
		} catch (OpenapiMemberTagRelationPushFailException e) {
			throw e;
		} catch (Exception e) {
			log.error("[tagRelationPush] 清空会员标签失败 company_id={} user_id={}", companyId, userIdRaw, e);
			throw new OpenapiMemberTagRelationPushFailException(
					"E9999", "清空会员标签失败：" + e.getMessage());
		}
	}

	private Map<String, Object> handleNormalMode(long companyId, List<Map<String, Object>> relations) {
		Map<String, Object> stats = newStats();
		try {
			log.info(
					"[tagRelationPush] 开始处理（正常模式） company_id={} count={}",
					companyId,
					relations.size());
			Map<String, MemberRelationGroup> groups = groupRelationsByUserId(relations);
			stats.put("processed_members", groups.size());

			for (Map.Entry<String, MemberRelationGroup> entry : groups.entrySet()) {
				String userIdKey = entry.getKey();
				MemberRelationGroup group = entry.getValue();
				try {
					Map<String, Object> member = findMember(companyId, userIdKey, group.mobile());
					if (member.isEmpty()) {
						log.warn(
								"[tagRelationPush] 未找到会员 company_id={} user_id={} mobile={}",
								companyId,
								userIdKey,
								group.mobile());
						inc(stats, "fail_count");
						continue;
					}
					long actualUserId = ((Number) member.get("user_id")).longValue();
					List<Long> tagIds = resolveTagIds(companyId, group.tags(), stats);
					memberRelTagsBatchCreateService.replaceMemberTagsInboundNoPush(
							actualUserId, tagIds, companyId);
					inc(stats, "success_count");
					log.info(
							"[tagRelationPush] 会员标签处理成功 user_id={} tag_count={}",
							actualUserId,
							tagIds.size());
				} catch (Exception e) {
					inc(stats, "fail_count");
					log.error(
							"[tagRelationPush] 会员标签处理失败 company_id={} user_id={}",
							companyId,
							userIdKey,
							e);
				}
			}

			log.info("[tagRelationPush] 处理完成 company_id={} stats={}", companyId, stats);
			return stats;
		} catch (Exception e) {
			log.error("[tagRelationPush] 处理失败 company_id={}", companyId, e);
			throw new OpenapiMemberTagRelationPushFailException(
					"E9999", "标签关系推送失败：" + e.getMessage());
		}
	}

	private Map<String, MemberRelationGroup> groupRelationsByUserId(List<Map<String, Object>> relations) {
		Map<String, MutableGroup> groups = new LinkedHashMap<>();
		for (Map<String, Object> relation : relations) {
			Object rawUserId = relation.get("user_id");
			if (isPhpEmpty(rawUserId)) {
				continue;
			}
			String userIdKey = stringValue(rawUserId);
			MutableGroup group = groups.computeIfAbsent(userIdKey, k -> new MutableGroup());
			if (group.mobile == null && !isPhpEmpty(relation.get("mobile"))) {
				group.mobile = stringValue(relation.get("mobile"));
			}
			group.tags.add(relation);
		}
		Map<String, MemberRelationGroup> result = new LinkedHashMap<>();
		for (Map.Entry<String, MutableGroup> entry : groups.entrySet()) {
			MutableGroup g = entry.getValue();
			result.put(entry.getKey(), new MemberRelationGroup(g.mobile, g.tags));
		}
		return result;
	}

	private Map<String, Object> findMember(long companyId, String userIdRaw, String mobileRaw) {
		Map<String, Object> filter = new HashMap<>();
		if (isPhpTruthy(userIdRaw)) {
			Long userId = parseUserIdLong(userIdRaw);
			if (userId != null) {
				filter.put("user_id", userId);
			}
		}
		Map<String, Object> member = memberAccountService.getMemberRowForAdminFilter(companyId, filter);
		if (member.isEmpty() && isPhpTruthy(mobileRaw)) {
			filter = new HashMap<>();
			filter.put("mobile", mobileRaw);
			member = memberAccountService.getMemberRowForAdminFilter(companyId, filter);
		}
		return member;
	}

	private List<Long> resolveTagIds(
			long companyId, List<Map<String, Object>> tagRelations, Map<String, Object> stats) {
		List<Long> tagIds = new ArrayList<>();
		for (Map<String, Object> tagRelation : tagRelations) {
			String wechatTagId = nullableString(tagRelation.get("wechat_tag_id"));
			String tagIdParam = nullableString(tagRelation.get("tag_id"));
			MemberTags entity = null;
			if (wechatTagId != null) {
				entity =
						memberTagsMapper.selectOne(
								new LambdaQueryWrapper<MemberTags>()
										.eq(MemberTags::getCompanyId, companyId)
										.eq(MemberTags::getWechatTagId, wechatTagId)
										.last("LIMIT 1"));
			} else if (tagIdParam != null) {
				Long tagIdLong = parseUserIdLong(tagIdParam);
				if (tagIdLong != null) {
					entity =
							memberTagsMapper.selectOne(
									new LambdaQueryWrapper<MemberTags>()
											.eq(MemberTags::getCompanyId, companyId)
											.eq(MemberTags::getTagId, tagIdLong)
											.last("LIMIT 1"));
				}
			} else {
				continue;
			}
			if (entity != null) {
				tagIds.add(entity.getTagId());
				inc(stats, "processed_tags");
			} else {
				log.warn(
						"[tagRelationPush] 未找到标签 wechat_tag_id={} tag_id={}",
						wechatTagId,
						tagIdParam);
			}
		}
		return tagIds;
	}

	private static Long parseUserIdLong(String raw) {
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Map<String, Object> newStats() {
		Map<String, Object> stats = new LinkedHashMap<>();
		stats.put("success_count", 0);
		stats.put("fail_count", 0);
		stats.put("processed_members", 0);
		stats.put("processed_tags", 0);
		return stats;
	}

	private static void inc(Map<String, Object> stats, String key) {
		stats.put(key, ((Integer) stats.get(key)) + 1);
	}

	private static boolean isPhpEmpty(Object raw) {
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

	private static boolean isPhpTruthy(String value) {
		return value != null && !value.isEmpty() && !"0".equals(value);
	}

	private static String stringValue(Object raw) {
		return raw != null ? String.valueOf(raw) : "";
	}

	private static String nullableString(Object raw) {
		return isPhpEmpty(raw) ? null : stringValue(raw);
	}

	private record MemberRelationGroup(String mobile, List<Map<String, Object>> tags) {}

	private static final class MutableGroup {
		String mobile;
		final List<Map<String, Object>> tags = new ArrayList<>();
	}
}
