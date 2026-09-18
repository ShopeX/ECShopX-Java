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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsActivity;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeam;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeamMember;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsTeamMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsTeamMemberMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromotionGroupsTeamJoinService {

	private final PromotionGroupsTeamMapper promotionGroupsTeamMapper;
	private final PromotionGroupsTeamMemberMapper promotionGroupsTeamMemberMapper;
	private final PromotionGroupsActivityMapper promotionGroupsActivityMapper;
	private final PromotionGroupsTeamIdGenerator promotionGroupsTeamIdGenerator;
	private final ObjectMapper objectMapper;

	public PromotionGroupsTeamJoinService(
			PromotionGroupsTeamMapper promotionGroupsTeamMapper,
			PromotionGroupsTeamMemberMapper promotionGroupsTeamMemberMapper,
			PromotionGroupsActivityMapper promotionGroupsActivityMapper,
			PromotionGroupsTeamIdGenerator promotionGroupsTeamIdGenerator,
			ObjectMapper objectMapper) {
		this.promotionGroupsTeamMapper = promotionGroupsTeamMapper;
		this.promotionGroupsTeamMemberMapper = promotionGroupsTeamMemberMapper;
		this.promotionGroupsActivityMapper = promotionGroupsActivityMapper;
		this.promotionGroupsTeamIdGenerator = promotionGroupsTeamIdGenerator;
		this.objectMapper = objectMapper;
	}

	public String createGroupsTeam(long companyId, long actId, long headMid, long beginTime, long endTime) {
		String teamId = promotionGroupsTeamIdGenerator.genId(headMid);
		int now = (int) beginTime;
		PromotionGroupsTeam team = new PromotionGroupsTeam();
		team.setTeamId(teamId);
		team.setCompanyId(companyId);
		team.setActId(actId);
		team.setHeadMid(headMid);
		team.setBeginTime(beginTime);
		team.setEndTime(endTime);
		team.setGroupGoodsType("normal");
		team.setJoinPersonNum(0L);
		team.setTeamStatus(1L);
		team.setDisabled(true);
		team.setCreated(now);
		team.setUpdated(now);
		promotionGroupsTeamMapper.insert(team);
		return teamId;
	}

	public void createGroupsTeamMember(
			String teamId,
			long companyId,
			long actId,
			long memberId,
			String orderId,
			String headimgurl,
			String nickname) {
		long now = System.currentTimeMillis() / 1000L;
		PromotionGroupsTeamMember member = new PromotionGroupsTeamMember();
		member.setTeamId(teamId);
		member.setCompanyId(companyId);
		member.setActId(actId);
		member.setMemberId(memberId);
		member.setJoinTime(now);
		member.setOrderId(orderId);
		member.setGroupGoodsType("normal");
		member.setDisabled(true);
		member.setMemberInfo(encodeMemberInfo(headimgurl, nickname));
		promotionGroupsTeamMemberMapper.insert(member);
	}

	public PromotionGroupsTeam incrementJoinNum(String teamId, long requiredPersonNum) {
		PromotionGroupsTeam team =
				promotionGroupsTeamMapper.selectOne(
						new LambdaQueryWrapper<PromotionGroupsTeam>()
								.eq(PromotionGroupsTeam::getTeamId, teamId)
								.last("LIMIT 1"));
		if (team == null) {
			throw new ResourceException("拼团活动已结束，记得下次及时参团哦～");
		}
		long join = team.getJoinPersonNum() == null ? 0L : team.getJoinPersonNum();
		long newJoin = join + 1L;
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<PromotionGroupsTeam> uw =
				new LambdaUpdateWrapper<PromotionGroupsTeam>().eq(PromotionGroupsTeam::getTeamId, teamId);
		uw.set(PromotionGroupsTeam::getJoinPersonNum, newJoin);
		uw.set(PromotionGroupsTeam::getUpdated, now);
		if (newJoin == 1L) {
			uw.set(PromotionGroupsTeam::getDisabled, false);
		}
		if (newJoin >= requiredPersonNum) {
			uw.set(PromotionGroupsTeam::getTeamStatus, 2L);
		}
		promotionGroupsTeamMapper.update(null, uw);
		return promotionGroupsTeamMapper.selectOne(
				new LambdaQueryWrapper<PromotionGroupsTeam>()
						.eq(PromotionGroupsTeam::getTeamId, teamId)
						.last("LIMIT 1"));
	}

	public PromotionGroupsActivity loadActivity(long companyId, long actId) {
		PromotionGroupsActivity activity =
				promotionGroupsActivityMapper.selectOne(
						new LambdaQueryWrapper<PromotionGroupsActivity>()
								.eq(PromotionGroupsActivity::getGroupsActivityId, actId)
								.eq(PromotionGroupsActivity::getCompanyId, companyId)
								.last("LIMIT 1"));
		if (activity == null) {
			throw new ResourceException("该活动不存在");
		}
		return activity;
	}

	public PromotionGroupsTeamMember findMember(long companyId, long orderId, long memberId) {
		return promotionGroupsTeamMemberMapper.selectOne(
				new LambdaQueryWrapper<PromotionGroupsTeamMember>()
						.eq(PromotionGroupsTeamMember::getCompanyId, companyId)
						.eq(PromotionGroupsTeamMember::getOrderId, String.valueOf(orderId))
						.eq(PromotionGroupsTeamMember::getMemberId, memberId)
						.last("LIMIT 1"));
	}

	public PromotionGroupsTeam findTeam(String teamId) {
		if (!StringUtils.hasText(teamId)) {
			return null;
		}
		return promotionGroupsTeamMapper.selectOne(
				new LambdaQueryWrapper<PromotionGroupsTeam>()
						.eq(PromotionGroupsTeam::getTeamId, teamId)
						.last("LIMIT 1"));
	}

	public List<PromotionGroupsTeamMember> listSuccessMembers(String teamId) {
		return promotionGroupsTeamMemberMapper.selectGroupTeamSuccessMembers(teamId);
	}

	public void enableMember(PromotionGroupsTeamMember member) {
		promotionGroupsTeamMemberMapper.update(
				null,
				new LambdaUpdateWrapper<PromotionGroupsTeamMember>()
						.eq(PromotionGroupsTeamMember::getId, member.getId())
						.set(PromotionGroupsTeamMember::getDisabled, false));
	}

	private String encodeMemberInfo(String headimgurl, String nickname) {
		Map<String, String> info = new LinkedHashMap<>();
		info.put("headimgurl", headimgurl == null ? "" : headimgurl);
		info.put("nickname", nickname == null || nickname.isEmpty() ? "用户" : nickname);
		try {
			return objectMapper.writeValueAsString(info);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}
}
