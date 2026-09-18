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

import cn.shopex.ecshopx.common.orders.port.NormalGroupsTradeFinishOnPayPort;
import cn.shopex.ecshopx.orders.service.group.GroupPromotionOrderPayedService;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsActivity;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeam;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeamMember;
import cn.shopex.ecshopx.promotions.service.PromotionGroupsTeamJoinService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NormalGroupsTradeFinishOnPayPortImpl implements NormalGroupsTradeFinishOnPayPort {

	private static final long TEAM_STATUS_SUCCESS = 2L;

	private final PromotionGroupsTeamJoinService promotionGroupsTeamJoinService;
	private final GroupPromotionOrderPayedService groupPromotionOrderPayedService;

	public NormalGroupsTradeFinishOnPayPortImpl(
			PromotionGroupsTeamJoinService promotionGroupsTeamJoinService,
			GroupPromotionOrderPayedService groupPromotionOrderPayedService) {
		this.promotionGroupsTeamJoinService = promotionGroupsTeamJoinService;
		this.groupPromotionOrderPayedService = groupPromotionOrderPayedService;
	}

	@Override
	public void onTradePaySuccess(long companyId, long userId, long orderId) {
		PromotionGroupsTeamMember member =
				promotionGroupsTeamJoinService.findMember(companyId, orderId, userId);
		if (member == null || !Boolean.TRUE.equals(member.getDisabled())) {
			return;
		}
		PromotionGroupsTeam team = promotionGroupsTeamJoinService.findTeam(member.getTeamId());
		if (team == null) {
			return;
		}
		long teamStatus = team.getTeamStatus() == null ? 0L : team.getTeamStatus();
		if (teamStatus == TEAM_STATUS_SUCCESS) {
			return;
		}

		promotionGroupsTeamJoinService.enableMember(member);

		PromotionGroupsActivity activity =
				promotionGroupsTeamJoinService.loadActivity(companyId, member.getActId());
		long personNum = activity.getPersonNum() == null ? 0L : activity.getPersonNum();
		PromotionGroupsTeam updated =
				promotionGroupsTeamJoinService.incrementJoinNum(member.getTeamId(), personNum);

		long updatedStatus = updated.getTeamStatus() == null ? 0L : updated.getTeamStatus();
		if (updatedStatus != TEAM_STATUS_SUCCESS) {
			return;
		}

		List<PromotionGroupsTeamMember> successList =
				promotionGroupsTeamJoinService.listSuccessMembers(member.getTeamId());
		for (PromotionGroupsTeamMember row : successList) {
			long mid = row.getMemberId() == null ? 0L : row.getMemberId();
			if (mid <= 0L) {
				continue;
			}
			long oid = parseOrderId(row.getOrderId());
			if (oid <= 0L) {
				continue;
			}
			groupPromotionOrderPayedService.markNormalGroupOrderPayedAndPublishErpSync(
					companyId, mid, oid);
		}
	}

	private static long parseOrderId(String orderId) {
		if (orderId == null || orderId.isBlank()) {
			return 0L;
		}
		try {
			return Long.parseLong(orderId.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
