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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsActivity;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeam;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsTeamMapper;
import cn.shopex.ecshopx.promotions.service.multilang.PromotionGroupsActivityItemMultiLangReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromotionGroupsTeamListService {

	private final PromotionGroupsTeamMapper promotionGroupsTeamMapper;
	private final MembersMapper membersMapper;
	private final MembersInfoMapper membersInfoMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final PromotionGroupsActivityMapper promotionGroupsActivityMapper;
	private final PromotionGroupsActivityAdminRowAssembler promotionGroupsActivityAdminRowAssembler;
	private final PromotionGroupsActivityItemMultiLangReadService promotionGroupsActivityItemMultiLangReadService;

	public PromotionGroupsTeamListService(
			PromotionGroupsTeamMapper promotionGroupsTeamMapper,
			MembersMapper membersMapper,
			MembersInfoMapper membersInfoMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			PromotionGroupsActivityMapper promotionGroupsActivityMapper,
			PromotionGroupsActivityAdminRowAssembler promotionGroupsActivityAdminRowAssembler,
			PromotionGroupsActivityItemMultiLangReadService promotionGroupsActivityItemMultiLangReadService) {
		this.promotionGroupsTeamMapper = promotionGroupsTeamMapper;
		this.membersMapper = membersMapper;
		this.membersInfoMapper = membersInfoMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.promotionGroupsActivityMapper = promotionGroupsActivityMapper;
		this.promotionGroupsActivityAdminRowAssembler = promotionGroupsActivityAdminRowAssembler;
		this.promotionGroupsActivityItemMultiLangReadService = promotionGroupsActivityItemMultiLangReadService;
	}

	public Map<String, Object> getPromotionGroupsTeamList(
			long companyId,
			long actId,
			String pageRaw,
			String pageSizeRaw,
			Integer view,
			Long startTime,
			Long endTime,
			String requestLangTag) {
		int page = parsePage(pageRaw);
		int pageSize = parsePageSize(pageSizeRaw);

		LambdaQueryWrapper<PromotionGroupsTeam> wrapper = new LambdaQueryWrapper<>();
		wrapper
				.eq(PromotionGroupsTeam::getCompanyId, companyId)
				.eq(PromotionGroupsTeam::getActId, actId);
		if (startTime != null && startTime != 0L) {
			wrapper.ge(PromotionGroupsTeam::getBeginTime, startTime);
		}
		if (endTime != null && endTime != 0L) {
			wrapper.le(PromotionGroupsTeam::getEndTime, endTime);
		}
		if (view != null && view != 0) {
			wrapper.eq(PromotionGroupsTeam::getTeamStatus, view.longValue());
		}
		wrapper.orderByDesc(PromotionGroupsTeam::getCreated);

		long total = promotionGroupsTeamMapper.selectCount(wrapper);
		List<Map<String, Object>> list;
		if (total == 0) {
			list = List.of();
		} else {
			Page<PromotionGroupsTeam> p = new Page<>(page, pageSize, false);
			promotionGroupsTeamMapper.selectPage(p, wrapper);
			List<PromotionGroupsTeam> records = p.getRecords();
			list = new ArrayList<>(records.size());
			for (PromotionGroupsTeam team : records) {
				list.add(teamRowWithoutUsername(team));
			}
			enrichUsername(companyId, list, records);
		}

		Object groupsActivity = buildGroupsActivity(companyId, actId, requestLangTag);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", list);
		out.put("groupsActivity", groupsActivity);
		return out;
	}

	private LinkedHashMap<String, Object> teamRowWithoutUsername(PromotionGroupsTeam team) {
		LinkedHashMap<String, Object> teamRow = new LinkedHashMap<>();
		teamRow.put("id", team.getId());
		teamRow.put("team_id", team.getTeamId());
		teamRow.put("company_id", team.getCompanyId());
		teamRow.put("act_id", team.getActId());
		teamRow.put("head_mid", team.getHeadMid());
		teamRow.put("begin_time", team.getBeginTime());
		teamRow.put("end_time", team.getEndTime());
		teamRow.put("join_person_num", team.getJoinPersonNum());
		teamRow.put("team_status", team.getTeamStatus());
		teamRow.put("group_goods_type", team.getGroupGoodsType());
		teamRow.put("disabled", team.getDisabled());
		teamRow.put("created", team.getCreated());
		teamRow.put("updated", team.getUpdated());
		return teamRow;
	}

	private void enrichUsername(
			long companyId, List<Map<String, Object>> list, List<PromotionGroupsTeam> records) {
		if (list.isEmpty()) {
			return;
		}
		Set<Long> headIds = new LinkedHashSet<>();
		for (PromotionGroupsTeam t : records) {
			Long hm = t.getHeadMid();
			if (hm != null) {
				headIds.add(hm);
			}
		}
		Map<Long, String> mobileByUserId = new LinkedHashMap<>();
		Map<Long, String> usernameByUserId = new LinkedHashMap<>();
		if (!headIds.isEmpty()) {
			List<Long> userIdList = new ArrayList<>(headIds);
			List<Members> membersRows =
					membersMapper.selectList(
							new LambdaQueryWrapper<Members>()
									.eq(Members::getCompanyId, companyId)
									.in(Members::getUserId, userIdList));
			for (Members m : membersRows) {
				Long uid = m.getUserId();
				if (uid == null) {
					continue;
				}
				String mob = m.getMobile();
				String decrypted = mob == null ? "" : blankToEmpty(sensitiveFieldEncryptor.decrypt(mob));
				mobileByUserId.put(uid, decrypted);
			}
			List<MembersInfo> infoRows =
					membersInfoMapper.selectList(
							new LambdaQueryWrapper<MembersInfo>()
									.eq(MembersInfo::getCompanyId, companyId)
									.in(MembersInfo::getUserId, userIdList));
			for (MembersInfo info : infoRows) {
				Long uid = info.getUserId();
				if (uid == null) {
					continue;
				}
				if (usernameByUserId.containsKey(uid)) {
					continue;
				}
				String un = info.getUsername();
				String decrypted = un == null ? "" : blankToEmpty(sensitiveFieldEncryptor.decrypt(un));
				usernameByUserId.put(uid, decrypted);
			}
		}
		for (int i = 0; i < list.size(); i++) {
			Map<String, Object> row = list.get(i);
			Long headMid = records.get(i).getHeadMid();
			String username = headMid == null ? "" : usernameByUserId.getOrDefault(headMid, "");
			if (!StringUtils.hasText(username) && headMid != null) {
				username = mobileByUserId.getOrDefault(headMid, "");
			}
			row.put("username", username);
		}
	}

	private Object buildGroupsActivity(long companyId, long actId, String requestLangTag) {
		PromotionGroupsActivity entity =
				promotionGroupsActivityMapper.selectOne(
						new LambdaQueryWrapper<PromotionGroupsActivity>()
								.eq(PromotionGroupsActivity::getCompanyId, companyId)
								.eq(PromotionGroupsActivity::getGroupsActivityId, actId)
								.eq(PromotionGroupsActivity::getDisabled, Boolean.FALSE));
		if (entity == null) {
			return List.of();
		}
		int now = (int) Instant.now().getEpochSecond();
		Map<String, Object> row = promotionGroupsActivityAdminRowAssembler.toRow(entity, now);
		Map<Long, Map<String, Object>> rowByActivityId = new LinkedHashMap<>();
		rowByActivityId.put(entity.getGroupsActivityId(), row);
		promotionGroupsActivityItemMultiLangReadService.applyBatch(
				companyId, List.of(entity.getGroupsActivityId()), rowByActivityId, requestLangTag);
		return row;
	}

	private static String blankToEmpty(String s) {
		return s == null ? "" : s;
	}

	private static int parsePage(String pageRaw) {
		if (!StringUtils.hasText(pageRaw)) {
			return 1;
		}
		try {
			return Integer.parseInt(pageRaw.trim());
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private static int parsePageSize(String pageSizeRaw) {
		if (!StringUtils.hasText(pageSizeRaw)) {
			return 20;
		}
		try {
			return Integer.parseInt(pageSizeRaw.trim());
		} catch (NumberFormatException e) {
			return 20;
		}
	}
}
