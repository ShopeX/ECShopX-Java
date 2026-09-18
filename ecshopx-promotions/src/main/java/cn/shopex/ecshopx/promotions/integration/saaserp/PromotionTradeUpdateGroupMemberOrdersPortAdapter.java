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

package cn.shopex.ecshopx.promotions.integration.saaserp;

import cn.shopex.ecshopx.common.saaserp.TradeUpdateGroupMemberOrdersPort;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeamMember;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsTeamMemberMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PromotionTradeUpdateGroupMemberOrdersPortAdapter implements TradeUpdateGroupMemberOrdersPort {

	private static final int TEAM_ORDER_PAGE_LIMIT = 10_000;

	private final PromotionGroupsTeamMemberMapper promotionGroupsTeamMemberMapper;

	public PromotionTradeUpdateGroupMemberOrdersPortAdapter(
			PromotionGroupsTeamMemberMapper promotionGroupsTeamMemberMapper) {
		this.promotionGroupsTeamMemberMapper = promotionGroupsTeamMemberMapper;
	}

	@Override
	public List<LinkedHashMap<String, Object>> listPaidTeamOrderRowsForLeader(
			long companyId, String leaderOrderId, long memberId) {
		PromotionGroupsTeamMember teamRow =
				promotionGroupsTeamMemberMapper.selectOne(
						new LambdaQueryWrapper<PromotionGroupsTeamMember>()
								.eq(PromotionGroupsTeamMember::getCompanyId, companyId)
								.eq(PromotionGroupsTeamMember::getOrderId, leaderOrderId)
								.eq(PromotionGroupsTeamMember::getMemberId, memberId)
								.last("LIMIT 1"));
		if (teamRow == null || !StringUtils.hasText(teamRow.getTeamId())) {
			return List.of();
		}
		String teamId = teamRow.getTeamId();
		List<LinkedHashMap<String, Object>> rawList =
				promotionGroupsTeamMemberMapper.selectTeamMemberOrderListPage(
						companyId, teamId, 0L, 0L, null, 0, TEAM_ORDER_PAGE_LIMIT);
		if (rawList == null || rawList.isEmpty()) {
			return List.of();
		}
		List<LinkedHashMap<String, Object>> paid = new ArrayList<>();
		for (LinkedHashMap<String, Object> row : rawList) {
			if (row != null && "PAYED".equals(stringify(row.get("o_order_status")))) {
				paid.add(row);
			}
		}
		return paid;
	}

	private static String stringify(Object o) {
		return o == null ? "" : o.toString();
	}
}
