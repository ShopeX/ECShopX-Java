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

package cn.shopex.ecshopx.promotions.integration.order;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.order.PartialCancelOrderItemRow;
import cn.shopex.ecshopx.common.port.order.PartialCancelPromotionRestorePort;
import cn.shopex.ecshopx.orders.domain.OrderPromotions;
import cn.shopex.ecshopx.orders.mapper.OrderPromotionsMapper;
import cn.shopex.ecshopx.promotions.domain.LimitPersonPromotions;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsActivity;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsTeamMember;
import cn.shopex.ecshopx.promotions.mapper.LimitPersonPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsTeamMemberMapper;
import cn.shopex.ecshopx.promotions.service.GroupItemStoreService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class PartialCancelPromotionRestorePortImpl implements PartialCancelPromotionRestorePort {

	private final PromotionGroupsTeamMemberMapper promotionGroupsTeamMemberMapper;
	private final PromotionGroupsActivityMapper promotionGroupsActivityMapper;
	private final LimitPersonPromotionsMapper limitPersonPromotionsMapper;
	private final OrderPromotionsMapper orderPromotionsMapper;
	private final StringRedisTemplate companysRedisTemplate;
	private final GroupItemStoreService groupItemStoreService;

	public PartialCancelPromotionRestorePortImpl(
			PromotionGroupsTeamMemberMapper promotionGroupsTeamMemberMapper,
			PromotionGroupsActivityMapper promotionGroupsActivityMapper,
			LimitPersonPromotionsMapper limitPersonPromotionsMapper,
			OrderPromotionsMapper orderPromotionsMapper,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			GroupItemStoreService groupItemStoreService) {
		this.promotionGroupsTeamMemberMapper = promotionGroupsTeamMemberMapper;
		this.promotionGroupsActivityMapper = promotionGroupsActivityMapper;
		this.limitPersonPromotionsMapper = limitPersonPromotionsMapper;
		this.orderPromotionsMapper = orderPromotionsMapper;
		this.companysRedisTemplate = companysRedisTemplate;
		this.groupItemStoreService = groupItemStoreService;
	}

	@Override
	public void restoreGroupItemStore(long companyId, long orderId, long orderUserId, long itemId, int cancelItemNum) {
		if (cancelItemNum <= 0) {
			return;
		}
		PromotionGroupsTeamMember member =
				promotionGroupsTeamMemberMapper.selectOne(
						new LambdaQueryWrapper<PromotionGroupsTeamMember>()
								.eq(PromotionGroupsTeamMember::getCompanyId, companyId)
								.eq(PromotionGroupsTeamMember::getOrderId, String.valueOf(orderId))
								.eq(PromotionGroupsTeamMember::getMemberId, orderUserId)
								.last("LIMIT 1"));
		if (member == null || member.getActId() == null) {
			throw new ResourceException("拼团订单部分取消恢复失败：活动数据不存在");
		}
		long actId = member.getActId();
		groupItemStoreService.plusGroupItemStore(companyId, actId, itemId, cancelItemNum);
	}

	@Override
	public void reduceLimitPerson(long companyId, long userId, long itemId, int number) {
		if (number <= 0) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		LimitPersonPromotions row =
				limitPersonPromotionsMapper.selectOne(
						new LambdaQueryWrapper<LimitPersonPromotions>()
								.eq(LimitPersonPromotions::getCompanyId, companyId)
								.eq(LimitPersonPromotions::getUserId, userId)
								.eq(LimitPersonPromotions::getItemId, itemId)
								.lt(LimitPersonPromotions::getStartTime, now)
								.gt(LimitPersonPromotions::getEndTime, now)
								.orderByDesc(LimitPersonPromotions::getId)
								.last("LIMIT 1"));
		if (row == null) {
			return;
		}
		long cur = row.getNumber() == null ? 0L : row.getNumber();
		long next = cur > number ? cur - number : 0L;
		limitPersonPromotionsMapper.update(
				null,
				new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<LimitPersonPromotions>()
						.eq(LimitPersonPromotions::getId, row.getId())
						.set(LimitPersonPromotions::getNumber, next));
	}

	@Override
	public void restoreLimitedTimeSaleUserBuys(
			long companyId, long orderId, List<PartialCancelOrderItemRow> rowsWithCancel) {
		if (rowsWithCancel == null || rowsWithCancel.isEmpty()) {
			return;
		}
		List<OrderPromotions> promos =
				orderPromotionsMapper.selectList(
						new LambdaQueryWrapper<OrderPromotions>()
								.eq(OrderPromotions::getMoid, orderId)
								.eq(OrderPromotions::getActivityType, "limited_time_sale"));
		if (promos == null || promos.isEmpty()) {
			return;
		}
		Map<Long, OrderPromotions> byItem = new HashMap<>();
		for (OrderPromotions p : promos) {
			if (p.getItemId() != null && p.getItemId() > 0L) {
				byItem.putIfAbsent(p.getItemId(), p);
			}
		}
		if (byItem.isEmpty()) {
			throw new ResourceException("订单促销数据异常");
		}
		for (PartialCancelOrderItemRow row : rowsWithCancel) {
			OrderPromotions promotion = byItem.get(row.itemId());
			if (promotion == null) {
				continue;
			}
			Long activityId = promotion.getActivityId();
			Long uid = promotion.getUserId();
			Long iid = promotion.getItemId();
			Long cid = promotion.getCompanyId();
			if (activityId == null || uid == null || iid == null || cid == null) {
				throw new ResourceException("订单促销数据异常");
			}
			int cancelNum = row.cancelItemNum();
			int totalFee = cancelNum * row.priceFen();
			applySeckillBuyDataHash(
					cid, activityId, uid, iid, -cancelNum, -totalFee);
		}
	}

	private void applySeckillBuyDataHash(
			long companyId, long seckillId, long userId, long itemId, int storeDelta, int priceDelta) {
		String hashKey = "seckill_buy_data:" + companyId;
		String buyStoreKey = "user_buy_store:" + seckillId + ":" + userId + ":" + itemId;
		String buyPriceKey = "user_buy_price:" + seckillId + ":" + userId + ":" + itemId;
		String buyTotalStore = "user_buy_total_store:" + seckillId + ":" + userId;
		String buyTotalPrice = "user_buy_total_price:" + seckillId + ":" + userId;
		companysRedisTemplate.opsForHash().increment(hashKey, buyStoreKey, storeDelta);
		companysRedisTemplate.opsForHash().increment(hashKey, buyPriceKey, priceDelta);
		companysRedisTemplate.opsForHash().increment(hashKey, buyTotalStore, storeDelta);
		companysRedisTemplate.opsForHash().increment(hashKey, buyTotalPrice, priceDelta);
	}
}
