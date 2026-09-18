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

package cn.shopex.ecshopx.members.service.admin;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.members.admin.AdminMemberDistributorShopCodeLookupPort;
import cn.shopex.ecshopx.common.members.admin.MemberUnionidByUserIdLookupPort;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.MarketingCenterOpenApiSignedFormClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminMemberReChangeRegDistributorService {

	private static final Logger log = LoggerFactory.getLogger(AdminMemberReChangeRegDistributorService.class);

	private final MembersMapper membersMapper;
	private final MembersAssociationsMapper membersAssociationsMapper;
	private final MemberUnionidByUserIdLookupPort memberUnionidByUserIdLookupPort;
	private final MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient;
	private final AdminMemberDistributorShopCodeLookupPort distributorShopCodeLookupPort;
	private final ObjectMapper objectMapper;

	public AdminMemberReChangeRegDistributorService(
			MembersMapper membersMapper,
			MembersAssociationsMapper membersAssociationsMapper,
			MemberUnionidByUserIdLookupPort memberUnionidByUserIdLookupPort,
			MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient,
			AdminMemberDistributorShopCodeLookupPort distributorShopCodeLookupPort,
			ObjectMapper objectMapper) {
		this.membersMapper = membersMapper;
		this.membersAssociationsMapper = membersAssociationsMapper;
		this.memberUnionidByUserIdLookupPort = memberUnionidByUserIdLookupPort;
		this.marketingCenterOpenApiSignedFormClient = marketingCenterOpenApiSignedFormClient;
		this.distributorShopCodeLookupPort = distributorShopCodeLookupPort;
		this.objectMapper = objectMapper;
	}

	public LinkedHashMap<String, Object> reChangeRegDistributor(long companyId, Map<String, Object> merged) {
		if (!merged.containsKey("user_id") || merged.get("user_id") == null) {
			throw new BadRequestException("会员ID必填");
		}
		if (!merged.containsKey("distributor_id") || merged.get("distributor_id") == null) {
			throw new BadRequestException("分销商ID必填");
		}
		String distRaw = String.valueOf(merged.get("distributor_id")).trim();
		long distributorId;
		try {
			distributorId = Long.parseLong(distRaw);
		} catch (NumberFormatException e) {
			throw new BadRequestException("分销商ID格式错误");
		}

		List<Long> normalizedIds = normalizeUserIds(merged);
		LambdaQueryWrapper<Members> q = Wrappers.lambdaQuery();
		q.eq(Members::getCompanyId, companyId).in(Members::getUserId, normalizedIds).orderByDesc(Members::getCreated).last("LIMIT 1000");
		List<Members> list = membersMapper.selectList(q);
		if (list.isEmpty()) {
			throw new ResourceException("未找到指定的会员信息");
		}

		LinkedHashSet<Long> needUpdateSet = new LinkedHashSet<>();
		for (Members member : list) {
			long curOp = member.getOpDistributor() == null ? 0L : member.getOpDistributor().longValue();
			if (curOp != distributorId) {
				needUpdateSet.add(member.getUserId());
			}
		}
		List<Long> needUpdateUserIds = new ArrayList<>(needUpdateSet);

		if (needUpdateUserIds.isEmpty()) {
			log.info("批量更新会员注册分销商：所有会员门店已一致，无需更新 company_id={} user_ids={} distributor_id={}",
					companyId, normalizedIds, distributorId);
			LinkedHashMap<String, Object> skip = new LinkedHashMap<>();
			skip.put("status", Boolean.TRUE);
			skip.put("message", "所有会员门店已一致，无需更新");
			skip.put("affected_rows", Integer.valueOf(0));
			return skip;
		}

		try {
			notifyUnbindMembers(companyId, needUpdateUserIds);
		} catch (Exception e) {
			log.error("批量更新会员注册分销商：通知导购端解绑失败 company_id={}", companyId, e);
		}

		LambdaUpdateWrapper<Members> uw = Wrappers.lambdaUpdate();
		uw.eq(Members::getCompanyId, companyId)
				.in(Members::getUserId, needUpdateUserIds)
				.set(Members::getOpDistributor, safeIntOpDistributor(distributorId))
				.set(Members::getUpdated, System.currentTimeMillis() / 1000L);
		int result = membersMapper.update(null, uw);
		int skippedCount = normalizedIds.size() - needUpdateUserIds.size();
		log.info("批量更新会员注册分销商：主更新完成 company_id={} original_user_ids={} need_update_user_ids={} skipped_count={} distributor_id={} affected_rows={}",
				companyId, normalizedIds, needUpdateUserIds, skippedCount, distributorId, result);

		try {
			syncStoreFriendRelations(companyId, distributorId, needUpdateUserIds);
		} catch (Exception e) {
			log.error("批量更新会员注册分销商：好友关系同步失败 company_id={}", companyId, e);
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("status", Boolean.TRUE);
		out.put("message", "更新成功");
		out.put("affected_rows", Integer.valueOf(result));
		return out;
	}

	private void syncStoreFriendRelations(long companyId, long distributorId, List<Long> needUpdateUserIds) {
		Optional<String> targetShop = distributorShopCodeLookupPort.findShopCodeByDistributorId(companyId, distributorId);
		if (targetShop.isEmpty()) {
			log.warn("批量更新会员注册分销商：未找到门店信息或门店编号 company_id={} distributor_id={}", companyId, distributorId);
			return;
		}
		String storeBn = targetShop.get();
		List<MembersAssociations> assocRows = membersAssociationsMapper.selectList(Wrappers.<MembersAssociations>lambdaQuery()
				.eq(MembersAssociations::getCompanyId, companyId)
				.in(MembersAssociations::getUserId, needUpdateUserIds)
				.eq(MembersAssociations::getUserType, "wechat"));
		if (assocRows.isEmpty()) {
			log.warn("未找到会员关联信息");
			return;
		}
		List<String> unionids = new ArrayList<>();
		LinkedHashSet<String> seenU = new LinkedHashSet<>();
		for (MembersAssociations a : assocRows) {
			if (!StringUtils.hasText(a.getUnionid())) {
				continue;
			}
			String u = a.getUnionid().trim();
			if (seenU.add(u)) {
				unionids.add(u);
			}
		}
		if (unionids.isEmpty()) {
			log.warn("未找到会员unionid");
			return;
		}
		LinkedHashMap<String, Object> friendPayload = new LinkedHashMap<>();
		friendPayload.put("store_bn", storeBn);
		friendPayload.put("unionids", new ArrayList<>(unionids));
		Map<String, Object> friendRoot = marketingCenterOpenApiSignedFormClient.postReturningFullRootMap(companyId,
				"members.store.friend.check", friendPayload);

		LinkedHashMap<String, String> unionidToUserId = new LinkedHashMap<>();
		for (MembersAssociations a : assocRows) {
			if (!StringUtils.hasText(a.getUnionid()) || a.getUserId() == null) {
				continue;
			}
			String u = a.getUnionid().trim();
			String uid = String.valueOf(a.getUserId()).trim();
			if (StringUtils.hasText(u) && StringUtils.hasText(uid)) {
				unionidToUserId.putIfAbsent(u, uid);
			}
		}

		Set<String> hasFriendUnionids = new LinkedHashSet<>();
		long nowSec = System.currentTimeMillis() / 1000L;
		Object dataObj = friendRoot.get("data");
		if (!friendRoot.isEmpty() && dataObj instanceof List<?> dataList && !dataList.isEmpty()) {
			boolean anyMatched = false;
			for (Object rowObj : dataList) {
				if (!(rowObj instanceof Map<?, ?> row)) {
					continue;
				}
				Object unionObj = row.get("unionid");
				Object workObj = row.get("work_userid");
				String union = unionObj == null ? "" : String.valueOf(unionObj).trim();
				String workUserid = workObj == null ? "" : String.valueOf(workObj).trim();
				if (!StringUtils.hasText(union) || !unionidToUserId.containsKey(union)) {
					continue;
				}
				String userIdStr = unionidToUserId.get(union);
				long userId;
				try {
					userId = Long.parseLong(userIdStr);
				} catch (NumberFormatException e) {
					continue;
				}
				hasFriendUnionids.add(union);
				anyMatched = true;
				LambdaUpdateWrapper<Members> uwOne = Wrappers.lambdaUpdate();
				uwOne.eq(Members::getCompanyId, companyId)
						.eq(Members::getUserId, userId)
						.set(Members::getIsBecomeFriend, Boolean.TRUE)
						.set(Members::getHasFp, Boolean.TRUE)
						.set(Members::getFpSalesperson, workUserid)
						.set(Members::getUpdated, nowSec);
				membersMapper.update(null, uwOne);
			}
			if (!anyMatched) {
				log.warn("查询到好友关系但无法匹配到会员");
			}
		} else {
			log.info("未查询到好友关系或接口返回异常");
		}

		if (!friendRoot.isEmpty()) {
			List<String> noFriendUnionids = unionids.stream().filter(u -> !hasFriendUnionids.contains(u)).collect(Collectors.toList());
			List<Long> noFriendUserIds = new ArrayList<>();
			for (String u : noFriendUnionids) {
				String uidStr = unionidToUserId.get(u);
				if (uidStr == null) {
					continue;
				}
				try {
					noFriendUserIds.add(Long.parseLong(uidStr.trim()));
				} catch (NumberFormatException ignored) {
				}
			}
			if (!noFriendUserIds.isEmpty()) {
				LambdaUpdateWrapper<Members> uwClear = Wrappers.lambdaUpdate();
				uwClear.eq(Members::getCompanyId, companyId)
						.in(Members::getUserId, noFriendUserIds)
						.set(Members::getHasFp, Boolean.FALSE)
						.set(Members::getUpdated, nowSec);
				membersMapper.update(null, uwClear);
			}
		} else {
			log.info("导购接口返回异常，不更新无好友关系会员");
		}
	}

	private void notifyUnbindMembers(long companyId, List<Long> userIds) {
		if (userIds == null || userIds.isEmpty()) {
			return;
		}
		List<Members> members = membersMapper.selectList(Wrappers.<Members>lambdaQuery()
				.eq(Members::getCompanyId, companyId)
				.in(Members::getUserId, userIds));
		LinkedHashMap<Long, Long> userToOp = new LinkedHashMap<>();
		for (Members m : members) {
			if (m.getUserId() != null) {
				long op = m.getOpDistributor() == null ? 0L : m.getOpDistributor().longValue();
				userToOp.put(m.getUserId(), op);
			}
		}
		LinkedHashMap<Long, List<String>> grouped = new LinkedHashMap<>();
		for (Map.Entry<Long, Long> e : userToOp.entrySet()) {
			long userId = e.getKey();
			Optional<String> unionOpt = memberUnionidByUserIdLookupPort.findUnionidByUserId(companyId, userId);
			if (unionOpt.isEmpty()) {
				continue;
			}
			String unionid = unionOpt.get();
			long oldDist = e.getValue() == null || e.getValue() <= 0 ? 0L : e.getValue();
			grouped.computeIfAbsent(oldDist, k -> new ArrayList<>()).add(unionid);
		}
		for (Map.Entry<Long, List<String>> group : grouped.entrySet()) {
			long oldDist = group.getKey();
			List<String> unionids = group.getValue();
			if (unionids == null || unionids.isEmpty()) {
				continue;
			}
			String storeBn = "";
			if (oldDist > 0) {
				storeBn = distributorShopCodeLookupPort.findShopCodeByDistributorId(companyId, oldDist).orElse("");
			}
			if (!StringUtils.hasText(storeBn) && oldDist > 0) {
				log.warn("通知导购端解绑会员：门店编号不存在 company_id={} distributor_id={}", companyId, oldDist);
				continue;
			}
			LinkedHashMap<String, Object> params = new LinkedHashMap<>();
			params.put("store_bn", storeBn);
			params.put("unionids", new ArrayList<>(unionids));
			Map<String, Object> root = marketingCenterOpenApiSignedFormClient.postReturningFullRootMap(companyId, "members.unbindMembers", params);
			Number err = toNumber(root.get("errcode"));
			if (err != null && err.intValue() == 0) {
				Object data = root.get("data");
				if (data instanceof Map<?, ?> dm) {
					Object results = dm.get("results");
					if (results instanceof List<?> rl) {
						int ok = 0;
						int fail = 0;
						for (Object item : rl) {
							if (item instanceof Map<?, ?> im) {
								Object s = im.get("success");
								if (Boolean.TRUE.equals(s) || "true".equalsIgnoreCase(String.valueOf(s))) {
									ok++;
								} else {
									fail++;
								}
							}
						}
						log.info("通知导购端解绑会员：接口返回统计 success={} fail={} company_id={}", ok, fail, companyId);
					}
				}
			} else {
				log.warn("通知导购端解绑会员：接口调用失败 company_id={} store_bn={} unionids={} root={}", companyId, storeBn, unionids, root);
			}
		}
	}

	private static Number toNumber(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n;
		}
		if (o instanceof String s && StringUtils.hasText(s.trim())) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	private List<Long> normalizeUserIds(Map<String, Object> merged) {
		Object rawUid = merged.get("user_id");
		LinkedHashSet<Long> ids = new LinkedHashSet<>();

		if (rawUid instanceof String s && StringUtils.hasText(s.trim())) {
			String ts = s.trim();
			try {
				JsonNode root = objectMapper.readTree(ts);
				if (root != null && root.isArray()) {
					boolean anyPositiveYet = false;
					for (JsonNode el : root) {
						try {
							long v = parseLongFromJsonNode(el);
							if (v > 0) {
								ids.add(v);
								anyPositiveYet = true;
							}
						} catch (NumberFormatException e) {
							if (!anyPositiveYet && ids.isEmpty()) {
								throw new BadRequestException("会员ID不能为空");
							}
							throw new BadRequestException("有效的会员ID不能为空");
						}
					}
					if (!ids.isEmpty()) {
						return new ArrayList<>(ids);
					}
					throw new BadRequestException("有效的会员ID不能为空");
				}
				if (root != null && (root.isIntegralNumber() || root.isNumber())) {
					long v = root.longValue();
					if (v > 0) {
						ids.add(v);
						return new ArrayList<>(ids);
					}
					throw new BadRequestException("有效的会员ID不能为空");
				}
				if (root != null && root.isTextual()) {
					try {
						long v = Long.parseLong(root.asText().trim());
						if (v > 0) {
							ids.add(v);
							return new ArrayList<>(ids);
						}
					} catch (NumberFormatException e) {
						throw new BadRequestException("会员ID不能为空");
					}
					throw new BadRequestException("有效的会员ID不能为空");
				}
			} catch (JsonProcessingException e) {
				// fall through to scalar parse of whole string
			}
			try {
				long v = Long.parseLong(ts);
				if (v > 0) {
					ids.add(v);
					return new ArrayList<>(ids);
				}
				throw new BadRequestException("有效的会员ID不能为空");
			} catch (NumberFormatException e) {
				throw new BadRequestException("会员ID不能为空");
			}
		}

		if (rawUid instanceof Collection<?> coll) {
			boolean anyPositiveYet = false;
			for (Object o : coll) {
				try {
					long v = Long.parseLong(String.valueOf(o).trim());
					if (v > 0) {
						ids.add(v);
						anyPositiveYet = true;
					}
				} catch (NumberFormatException e) {
					if (!anyPositiveYet && ids.isEmpty()) {
						throw new BadRequestException("会员ID不能为空");
					}
					throw new BadRequestException("有效的会员ID不能为空");
				}
			}
			if (ids.isEmpty()) {
				throw new BadRequestException("有效的会员ID不能为空");
			}
			return new ArrayList<>(ids);
		}

		try {
			long v = Long.parseLong(String.valueOf(rawUid).trim());
			if (v <= 0) {
				throw new BadRequestException("有效的会员ID不能为空");
			}
			ids.add(v);
		} catch (NumberFormatException e) {
			throw new BadRequestException("会员ID不能为空");
		}
		return new ArrayList<>(ids);
	}

	private static long parseLongFromJsonNode(JsonNode el) {
		if (el == null || el.isNull()) {
			throw new NumberFormatException();
		}
		if (el.isIntegralNumber()) {
			return el.longValue();
		}
		if (el.isNumber()) {
			return el.longValue();
		}
		if (el.isTextual()) {
			return Long.parseLong(el.asText().trim());
		}
		throw new NumberFormatException();
	}

	private static int safeIntOpDistributor(long distributorId) {
		long clamped = Math.min(Math.max(distributorId, 0L), Integer.MAX_VALUE);
		return (int) clamped;
	}
}
