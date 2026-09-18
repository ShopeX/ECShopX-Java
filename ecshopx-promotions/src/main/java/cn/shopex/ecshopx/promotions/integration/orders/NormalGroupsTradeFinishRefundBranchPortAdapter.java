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

package cn.shopex.ecshopx.promotions.integration.orders;

import cn.shopex.ecshopx.common.orders.port.NormalGroupsTradeFinishRefundBranchPort;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeam;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeamMember;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsTeamMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsTeamMemberMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class NormalGroupsTradeFinishRefundBranchPortAdapter implements NormalGroupsTradeFinishRefundBranchPort {

	private static final long TEAM_STATUS_FORMED = 2L;

	private final PromotionGroupsTeamMemberMapper promotionGroupsTeamMemberMapper;
	private final PromotionGroupsTeamMapper promotionGroupsTeamMapper;

	public NormalGroupsTradeFinishRefundBranchPortAdapter(
			PromotionGroupsTeamMemberMapper promotionGroupsTeamMemberMapper,
			PromotionGroupsTeamMapper promotionGroupsTeamMapper) {
		this.promotionGroupsTeamMemberMapper = promotionGroupsTeamMemberMapper;
		this.promotionGroupsTeamMapper = promotionGroupsTeamMapper;
	}

	@Override
	public boolean shouldRefundDisabledMemberInFormedTeam(long companyId, long orderId, long userId) {
		PromotionGroupsTeamMember member =
				promotionGroupsTeamMemberMapper.selectOne(
						new LambdaQueryWrapper<PromotionGroupsTeamMember>()
								.eq(PromotionGroupsTeamMember::getCompanyId, companyId)
								.eq(PromotionGroupsTeamMember::getOrderId, String.valueOf(orderId))
								.eq(PromotionGroupsTeamMember::getMemberId, userId)
								.last("LIMIT 1"));
		if (member == null || !Boolean.TRUE.equals(member.getDisabled())) {
			return false;
		}
		if (!StringUtils.hasText(member.getTeamId())) {
			return false;
		}
		PromotionGroupsTeam team =
				promotionGroupsTeamMapper.selectOne(
						new LambdaQueryWrapper<PromotionGroupsTeam>()
								.eq(PromotionGroupsTeam::getCompanyId, companyId)
								.eq(PromotionGroupsTeam::getTeamId, member.getTeamId())
								.last("LIMIT 1"));
		return team != null
				&& team.getTeamStatus() != null
				&& team.getTeamStatus().longValue() == TEAM_STATUS_FORMED;
	}
}
