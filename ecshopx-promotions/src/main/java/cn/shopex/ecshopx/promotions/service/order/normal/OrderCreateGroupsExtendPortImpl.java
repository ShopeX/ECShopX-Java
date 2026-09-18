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

package cn.shopex.ecshopx.promotions.service.order.normal;

import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.common.order.normal.OrderCreateGroupsExtendPort;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsActivity;
import cn.shopex.ecshopx.promotions.service.PromotionGroupsTeamJoinService;
import cn.shopex.ecshopx.promotions.service.PromotionGroupsTeamMemberInfoResolver;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderCreateGroupsExtendPortImpl implements OrderCreateGroupsExtendPort {

	private final PromotionGroupsActivityCheckCreateGroupOrderService checkCreateGroupOrderService;
	private final PromotionGroupsTeamJoinService promotionGroupsTeamJoinService;
	private final PromotionGroupsTeamMemberInfoResolver promotionGroupsTeamMemberInfoResolver;

	public OrderCreateGroupsExtendPortImpl(
			PromotionGroupsActivityCheckCreateGroupOrderService checkCreateGroupOrderService,
			PromotionGroupsTeamJoinService promotionGroupsTeamJoinService,
			PromotionGroupsTeamMemberInfoResolver promotionGroupsTeamMemberInfoResolver) {
		this.checkCreateGroupOrderService = checkCreateGroupOrderService;
		this.promotionGroupsTeamJoinService = promotionGroupsTeamJoinService;
		this.promotionGroupsTeamMemberInfoResolver = promotionGroupsTeamMemberInfoResolver;
	}

	@Override
	public void applyAfterOrderInsert(NormalOrderCreateParams p) {
		Map<String, Object> pr = p.getParams();
		if (!"normal_groups".equals(stringVal(pr.get("order_type")))) {
			return;
		}
		Map<String, Object> od = p.getOrderData();
		if (od == null) {
			return;
		}
		long companyId = longVal(pr.get("company_id"), 0L);
		long userId = longVal(pr.get("user_id"), 0L);
		long actId = longVal(pr.get("bargain_id"), 0L);
		long orderId = longVal(od.get("order_id"), 0L);
		if (companyId <= 0L || userId <= 0L || actId <= 0L || orderId <= 0L) {
			return;
		}

		PromotionGroupsActivity groupInfo =
				checkCreateGroupOrderService.checkCreateGroupOrder(pr, p.getOrderData());
		String teamId = stringVal(pr.get("team_id"));
		if (!StringUtils.hasText(teamId)) {
			long now = System.currentTimeMillis() / 1000L;
			long limitHours = groupInfo.getLimitTime() == null ? 0L : groupInfo.getLimitTime();
			long limitEnd = now + limitHours * 3600L;
			long actEnd = groupInfo.getEndTime() == null ? limitEnd : groupInfo.getEndTime();
			long endTime = limitEnd < actEnd ? limitEnd : actEnd;
			teamId =
					promotionGroupsTeamJoinService.createGroupsTeam(
							companyId, actId, userId, now, endTime);
		}

		PromotionGroupsTeamMemberInfoResolver.Profile profile =
				promotionGroupsTeamMemberInfoResolver.resolve(companyId, userId);
		promotionGroupsTeamJoinService.createGroupsTeamMember(
				teamId,
				companyId,
				actId,
				userId,
				String.valueOf(orderId),
				profile.headimgurl(),
				profile.nickname());

		od.put("team_id", teamId);
		Map<String, Object> res = p.getOrdersInsertResult();
		if (res != null) {
			res.put("team_id", teamId);
		}
	}

	private static long longVal(Object v, long def) {
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}
}
