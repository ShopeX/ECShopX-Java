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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsActivity;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsRelGoods;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeam;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeamMember;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsTeamMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsTeamMemberMapper;
import cn.shopex.ecshopx.promotions.service.PromotionGroupsRelGoodsReadService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromotionGroupsActivityCheckCreateGroupOrderService {

	private static final long TEAM_STATUS_SUCCESS = 2L;
	private static final long TEAM_STATUS_FAIL = 3L;

	private final PromotionGroupsActivityMapper promotionGroupsActivityMapper;
	private final PromotionGroupsTeamMapper promotionGroupsTeamMapper;
	private final PromotionGroupsTeamMemberMapper promotionGroupsTeamMemberMapper;
	private final OrderAssociationsMapper orderAssociationsMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final StringRedisTemplate companysRedisTemplate;
	private final JdbcTemplate jdbcTemplate;
	private final PromotionGroupsRelGoodsReadService promotionGroupsRelGoodsReadService;

	public PromotionGroupsActivityCheckCreateGroupOrderService(
			PromotionGroupsActivityMapper promotionGroupsActivityMapper,
			PromotionGroupsTeamMapper promotionGroupsTeamMapper,
			PromotionGroupsTeamMemberMapper promotionGroupsTeamMemberMapper,
			OrderAssociationsMapper orderAssociationsMapper,
			NormalOrdersMapper normalOrdersMapper,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			JdbcTemplate jdbcTemplate,
			PromotionGroupsRelGoodsReadService promotionGroupsRelGoodsReadService) {
		this.promotionGroupsActivityMapper = promotionGroupsActivityMapper;
		this.promotionGroupsTeamMapper = promotionGroupsTeamMapper;
		this.promotionGroupsTeamMemberMapper = promotionGroupsTeamMemberMapper;
		this.orderAssociationsMapper = orderAssociationsMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.companysRedisTemplate = companysRedisTemplate;
		this.jdbcTemplate = jdbcTemplate;
		this.promotionGroupsRelGoodsReadService = promotionGroupsRelGoodsReadService;
	}

	public PromotionGroupsActivity checkCreateGroupOrder(Map<String, Object> params) {
		return checkCreateGroupOrder(params, null);
	}

	public PromotionGroupsActivity checkCreateGroupOrder(
			Map<String, Object> params, Map<String, Object> orderData) {
		long companyId =
				longVal(
						params.get("company_id"),
						longVal(orderData != null ? orderData.get("company_id") : null, 0L));
		long userId = longVal(params.get("user_id"), 0L);
		long actId = longVal(params.get("bargain_id"), 0L);
		if (actId <= 0L) {
			throw new ResourceException("拼团id必填");
		}
		PromotionGroupsActivity groupInfo =
				promotionGroupsActivityMapper.selectOne(
						new LambdaQueryWrapper<PromotionGroupsActivity>()
								.eq(PromotionGroupsActivity::getGroupsActivityId, actId)
								.eq(PromotionGroupsActivity::getCompanyId, companyId)
								.eq(PromotionGroupsActivity::getDisabled, false)
								.last("LIMIT 1"));
		if (groupInfo == null) {
			throw new ResourceException("该活动不存在");
		}
		long store = groupInfo.getStore() == null ? 0L : groupInfo.getStore();
		if (store == 0L) {
			throw new ResourceException("拼团商品已售完，期待您的下次参与");
		}
		long now = System.currentTimeMillis() / 1000L;
		long endTime = groupInfo.getEndTime() == null ? 0L : groupInfo.getEndTime();
		if (endTime < now) {
			throw new ResourceException("拼团活动已过期，期待您的下次参与");
		}
		long limitBuyNum = groupInfo.getLimitBuyNum() == null ? 0L : groupInfo.getLimitBuyNum();
		if (limitBuyNum != 0L && isGroupsActivityLimitReached(companyId, actId, userId, limitBuyNum)) {
			throw new ResourceException("拼团次数已达上限");
		}
		long itemId = resolveFirstItemId(params, orderData);
		if (promotionGroupsRelGoodsReadService.hasRelRows(companyId, actId)) {
			if (itemId <= 0L) {
				throw new ResourceException("该规格未参与拼团");
			}
			Optional<PromotionGroupsRelGoods> rel =
					promotionGroupsRelGoodsReadService.findByActivityAndItem(companyId, actId, itemId);
			if (rel.isEmpty()) {
				throw new ResourceException("该规格未参与拼团");
			}
			if (readGroupItemStore(actId, itemId, true) <= 0L) {
				throw new ResourceException("拼团库存不足");
			}
		} else {
			if (itemId > 0L) {
				long goodsId = groupInfo.getGoodsId() == null ? 0L : groupInfo.getGoodsId();
				if (goodsId > 0L && itemId != goodsId) {
					throw new ResourceException("该规格未参与拼团");
				}
			}
			if (readGroupItemStore(actId, itemId, false) <= 0L) {
				throw new ResourceException("商品库存不足");
			}
		}
		String teamId = stringVal(params.get("team_id"));
		if (StringUtils.hasText(teamId)) {
			validateJoinTeam(companyId, userId, actId, teamId);
		}
		return groupInfo;
	}

	private void validateJoinTeam(long companyId, long userId, long actId, String teamId) {
		PromotionGroupsTeam team =
				promotionGroupsTeamMapper.selectOne(
						new LambdaQueryWrapper<PromotionGroupsTeam>()
								.eq(PromotionGroupsTeam::getTeamId, teamId)
								.eq(PromotionGroupsTeam::getCompanyId, companyId)
								.eq(PromotionGroupsTeam::getDisabled, false)
								.last("LIMIT 1"));
		if (team == null) {
			throw new ResourceException("拼团活动已结束，记得下次及时参团哦～");
		}
		long teamStatus = team.getTeamStatus() == null ? 0L : team.getTeamStatus();
		if (teamStatus == TEAM_STATUS_SUCCESS) {
			throw new ResourceException("该拼团已成功，无法参团！");
		}
		if (teamStatus == TEAM_STATUS_FAIL) {
			throw new ResourceException("拼团活动已结束，记得下次及时参团哦～");
		}
		PromotionGroupsTeamMember member =
				promotionGroupsTeamMemberMapper.selectOne(
						new LambdaQueryWrapper<PromotionGroupsTeamMember>()
								.eq(PromotionGroupsTeamMember::getTeamId, teamId)
								.eq(PromotionGroupsTeamMember::getMemberId, userId)
								.eq(PromotionGroupsTeamMember::getCompanyId, companyId)
								.eq(PromotionGroupsTeamMember::getActId, actId)
								.last("LIMIT 1"));
		if (member == null) {
			return;
		}
		if (!Boolean.TRUE.equals(member.getDisabled())) {
			throw new ResourceException("您已拼团，请到我的拼团查看");
		}
		String orderId = member.getOrderId();
		if (!StringUtils.hasText(orderId)) {
			return;
		}
		long orderIdNum = longVal(orderId, 0L);
		if (orderIdNum <= 0L) {
			return;
		}
		OrderAssociations order =
				orderAssociationsMapper.selectOne(
						new LambdaQueryWrapper<OrderAssociations>()
								.eq(OrderAssociations::getCompanyId, companyId)
								.eq(OrderAssociations::getOrderId, orderIdNum)
								.last("LIMIT 1"));
		if (order != null && "NOTPAY".equals(stringVal(order.getOrderStatus()))) {
			throw new ResourceException("您已拼团，请到未支付订单支付");
		}
	}

	private boolean isGroupsActivityLimitReached(long companyId, long actId, long userId, long limitBuyNum) {
		List<String> statuses =
				List.of("DONE", "WAIT_GROUPS_SUCCESS", "PAYED", "WAIT_BUYER_CONFIRM");
		Long count =
				normalOrdersMapper.selectCount(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getUserId, userId)
								.eq(NormalOrders::getOrderClass, "groups")
								.eq(NormalOrders::getActId, actId)
								.in(NormalOrders::getOrderStatus, statuses));
		long hasCount = count == null ? 0L : count;
		long failNum = countGroupsFailOrders(companyId, actId, userId);
		hasCount = hasCount - failNum;
		return hasCount >= limitBuyNum;
	}

	private long countGroupsFailOrders(long companyId, long actId, long userId) {
		List<String> orderIds =
				jdbcTemplate.query(
						"""
						SELECT DISTINCT pgtm.order_id
						FROM promotion_groups_team pgt
						INNER JOIN promotion_groups_team_member pgtm ON pgt.team_id = pgtm.team_id
						WHERE pgt.act_id = ?
						  AND pgt.team_status = ?
						  AND pgtm.member_id = ?
						  AND pgtm.company_id = ?
						""",
						(rs, rowNum) -> rs.getString("order_id"),
						actId,
						TEAM_STATUS_FAIL,
						userId,
						companyId);
		if (orderIds == null || orderIds.isEmpty()) {
			return 0L;
		}
		List<Long> orderIdNums = new java.util.ArrayList<>();
		for (String oid : orderIds) {
			long parsed = longVal(oid, 0L);
			if (parsed > 0L) {
				orderIdNums.add(parsed);
			}
		}
		if (orderIdNums.isEmpty()) {
			return 0L;
		}
		Long failCount =
				normalOrdersMapper.selectCount(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getUserId, userId)
								.eq(NormalOrders::getOrderClass, "groups")
								.eq(NormalOrders::getActId, actId)
								.in(NormalOrders::getOrderId, orderIdNums));
		return Math.max(failCount == null ? 0L : failCount, 0L);
	}

	private long readGroupItemStore(long actId, long itemId, boolean multi) {
		String key =
				multi
						? "group_item_store:" + actId + ":" + itemId
						: "group_item_store:" + actId;
		String raw = companysRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			return 0L;
		}
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	@SuppressWarnings("unchecked")
	private static long resolveFirstItemId(Map<String, Object> params, Map<String, Object> orderData) {
		long fromParams = firstItemIdFromItems(params != null ? params.get("items") : null);
		if (fromParams > 0L) {
			return fromParams;
		}
		if (params != null) {
			long single = longVal(params.get("item_id"), 0L);
			if (single > 0L) {
				return single;
			}
		}
		if (orderData != null) {
			long fromOrderData = firstItemIdFromItems(orderData.get("items"));
			if (fromOrderData > 0L) {
				return fromOrderData;
			}
		}
		return 0L;
	}

	@SuppressWarnings("unchecked")
	private static long firstItemIdFromItems(Object itemsRaw) {
		if (!(itemsRaw instanceof List<?> list) || list.isEmpty()) {
			return 0L;
		}
		Object first = list.get(0);
		if (!(first instanceof Map<?, ?> row)) {
			return 0L;
		}
		return longVal(((Map<String, Object>) row).get("item_id"), 0L);
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
