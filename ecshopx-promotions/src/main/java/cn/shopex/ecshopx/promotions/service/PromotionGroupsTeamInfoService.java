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
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeam;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsTeamMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsTeamMemberMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromotionGroupsTeamInfoService {

	private static final Logger log = LoggerFactory.getLogger(PromotionGroupsTeamInfoService.class);

	private final PromotionGroupsTeamMemberMapper promotionGroupsTeamMemberMapper;
	private final PromotionGroupsTeamMapper promotionGroupsTeamMapper;
	private final MembersMapper membersMapper;
	private final MembersInfoMapper membersInfoMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final ObjectMapper objectMapper;

	public PromotionGroupsTeamInfoService(
			PromotionGroupsTeamMemberMapper promotionGroupsTeamMemberMapper,
			PromotionGroupsTeamMapper promotionGroupsTeamMapper,
			MembersMapper membersMapper,
			MembersInfoMapper membersInfoMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			ObjectMapper objectMapper) {
		this.promotionGroupsTeamMemberMapper = promotionGroupsTeamMemberMapper;
		this.promotionGroupsTeamMapper = promotionGroupsTeamMapper;
		this.membersMapper = membersMapper;
		this.membersInfoMapper = membersInfoMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getPromotionGroupsTeamInfo(
			long companyId,
			String teamId,
			String pageRaw,
			String pageSizeRaw,
			Long startTime,
			Long endTime,
			String orderId) {
		int page = parsePage(pageRaw);
		int pageSize = parsePageSize(pageSizeRaw);
		String orderIdParam =
				(orderId == null || orderId.isEmpty() || "0".equals(orderId)) ? null : orderId;

		long total =
				promotionGroupsTeamMemberMapper.countTeamMemberOrderList(
						companyId, teamId, startTime, endTime, orderIdParam);
		int offset = (page - 1) * pageSize;
		List<LinkedHashMap<String, Object>> rawRows =
				promotionGroupsTeamMemberMapper.selectTeamMemberOrderListPage(
						companyId, teamId, startTime, endTime, orderIdParam, offset, pageSize);

		List<LinkedHashMap<String, Object>> processedList = new ArrayList<>();
		for (LinkedHashMap<String, Object> raw : rawRows) {
			processedList.add(mergeJoinRow(raw));
		}

		for (LinkedHashMap<String, Object> row : processedList) {
			normalizeMemberInfoJson(row);
		}

		Set<Long> userIdSet = new LinkedHashSet<>();
		for (LinkedHashMap<String, Object> row : processedList) {
			Long uid = toLong(row.get("user_id"));
			if (uid != null) {
				userIdSet.add(uid);
			}
		}

		Map<Long, String> mobileByUserId = new LinkedHashMap<>();
		Map<Long, String> usernameByUserId = new LinkedHashMap<>();
		if (!userIdSet.isEmpty()) {
			List<Long> userIdList = new ArrayList<>(userIdSet);
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
				String decrypted =
						mob == null ? "" : blankToEmpty(sensitiveFieldEncryptor.decrypt(mob));
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
				String un = info.getUsername();
				String decrypted =
						un == null ? "" : blankToEmpty(sensitiveFieldEncryptor.decrypt(un));
				usernameByUserId.put(uid, decrypted);
			}
		}

		for (LinkedHashMap<String, Object> row : processedList) {
			@SuppressWarnings("unchecked")
			Map<String, Object> memberInfo = (Map<String, Object>) row.get("member_info");
			if (memberInfo == null) {
				memberInfo = new LinkedHashMap<>();
				row.put("member_info", memberInfo);
			}
			String nickname = memberInfo.get("nickname") == null ? "" : String.valueOf(memberInfo.get("nickname"));
			if (!PromotionGroupsTeamMemberInfoResolver.isUsableDisplayNickname(nickname)) {
				Long uid = toLong(row.get("user_id"));
				nickname = uid == null ? "" : usernameByUserId.getOrDefault(uid, "");
				if (!StringUtils.hasText(nickname) && uid != null) {
					nickname = mobileByUserId.getOrDefault(uid, "");
				}
				memberInfo.put("nickname", nickname);
			}
		}

		List<PromotionGroupsTeam> teamRows =
				promotionGroupsTeamMapper.selectList(
						new LambdaQueryWrapper<PromotionGroupsTeam>()
								.eq(PromotionGroupsTeam::getCompanyId, companyId)
								.eq(PromotionGroupsTeam::getTeamId, teamId)
								.last("LIMIT 1"));
		PromotionGroupsTeam team = teamRows.isEmpty() ? null : teamRows.get(0);

		Object teamInfoObject;
		if (team == null) {
			teamInfoObject = List.of();
		} else {
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
			teamInfoObject = teamRow;
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", processedList);
		out.put("teamInfo", teamInfoObject);
		return out;
	}

	private static String blankToEmpty(String s) {
		return s == null ? "" : s;
	}

	private void normalizeMemberInfoJson(LinkedHashMap<String, Object> row) {
		Object raw = row.get("member_info");
		if (raw == null) {
			row.put("member_info", new LinkedHashMap<String, Object>());
			return;
		}
		if (raw instanceof Map<?, ?> m) {
			LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
			for (Map.Entry<?, ?> e : m.entrySet()) {
				if (e.getKey() != null) {
					copy.put(e.getKey().toString(), e.getValue());
				}
			}
			row.put("member_info", copy);
			return;
		}
		if (raw instanceof String str) {
			if (!StringUtils.hasText(str)) {
				row.put("member_info", new LinkedHashMap<String, Object>());
				return;
			}
			try {
				@SuppressWarnings("unchecked")
				Map<String, Object> parsed = objectMapper.readValue(str, Map.class);
				LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
				if (parsed != null) {
					for (Map.Entry<String, Object> e : parsed.entrySet()) {
						if (e.getKey() != null) {
							copy.put(e.getKey(), e.getValue());
						}
					}
				}
				row.put("member_info", copy);
			} catch (JsonProcessingException e) {
				log.warn("member_info JSON parse failed: {}", e.getMessage());
				row.put("member_info", new LinkedHashMap<String, Object>());
			}
			return;
		}
		row.put("member_info", new LinkedHashMap<String, Object>());
	}

	private static LinkedHashMap<String, Object> mergeJoinRow(Map<String, Object> raw) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : raw.entrySet()) {
			String snake = keyToSnake(e.getKey());
			if (snake.startsWith("m_")) {
				row.put(snake.substring(2), e.getValue());
			}
		}
		for (Map.Entry<String, Object> e : raw.entrySet()) {
			String snake = keyToSnake(e.getKey());
			if (snake.startsWith("o_")) {
				row.put(snake.substring(2), e.getValue());
			}
		}
		return row;
	}

	private static String keyToSnake(String k) {
		if (k == null || k.isEmpty()) {
			return "";
		}
		if (k.indexOf('_') >= 0) {
			return k.toLowerCase();
		}
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < k.length(); i++) {
			char c = k.charAt(i);
			if (Character.isUpperCase(c) && i > 0) {
				b.append('_');
			}
			b.append(Character.toLowerCase(c));
		}
		return b.toString();
	}

	private static Long toLong(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
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
