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
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsRelGoods;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsRelGoodsMapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class GroupItemStoreService {

	private final StringRedisTemplate companysRedisTemplate;
	private final PromotionGroupsActivityMapper promotionGroupsActivityMapper;
	private final PromotionGroupsRelGoodsMapper promotionGroupsRelGoodsMapper;
	private final PromotionGroupsRelGoodsReadService promotionGroupsRelGoodsReadService;

	public GroupItemStoreService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			PromotionGroupsActivityMapper promotionGroupsActivityMapper,
			PromotionGroupsRelGoodsMapper promotionGroupsRelGoodsMapper,
			PromotionGroupsRelGoodsReadService promotionGroupsRelGoodsReadService) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.promotionGroupsActivityMapper = promotionGroupsActivityMapper;
		this.promotionGroupsRelGoodsMapper = promotionGroupsRelGoodsMapper;
		this.promotionGroupsRelGoodsReadService = promotionGroupsRelGoodsReadService;
	}

	@Deprecated
	public boolean minusGroupItemStore(long actId, int num) {
		if (actId <= 0L || num <= 0) {
			return true;
		}
		if (promotionGroupsRelGoodsReadService.hasRelRowsByActId(actId)) {
			throw new ResourceException("该规格未参与拼团");
		}
		String key = legacyRedisStoreKey(actId);
		Long store = companysRedisTemplate.opsForValue().increment(key, -num);
		if (store == null || store < 0L) {
			companysRedisTemplate.opsForValue().increment(key, num);
			return false;
		}
		promotionGroupsActivityMapper.update(
				null,
				new UpdateWrapper<PromotionGroupsActivity>()
						.eq("groups_activity_id", actId)
						.set("store", store));
		return true;
	}

	public boolean minusGroupItemStore(long companyId, long actId, long itemId, int num) {
		if (actId <= 0L || num <= 0) {
			return true;
		}
		boolean multi = promotionGroupsRelGoodsReadService.hasRelRows(companyId, actId);
		if (multi && itemId <= 0L) {
			throw new ResourceException("该规格未参与拼团");
		}
		String key = resolveStoreKey(multi, actId, itemId);
		Long store = companysRedisTemplate.opsForValue().increment(key, -num);
		if (store == null || store < 0L) {
			companysRedisTemplate.opsForValue().increment(key, num);
			return false;
		}
		if (multi) {
			promotionGroupsRelGoodsMapper.update(
					null,
					new UpdateWrapper<PromotionGroupsRelGoods>()
							.eq("company_id", companyId)
							.eq("groups_activity_id", actId)
							.eq("item_id", itemId)
							.set("activity_store", store));
			promotionGroupsActivityMapper.update(
					null,
					new UpdateWrapper<PromotionGroupsActivity>()
							.eq("groups_activity_id", actId)
							.setSql("store = store - " + num));
		} else {
			promotionGroupsActivityMapper.update(
					null,
					new UpdateWrapper<PromotionGroupsActivity>()
							.eq("groups_activity_id", actId)
							.set("store", store));
		}
		return true;
	}

	public void plusGroupItemStore(long companyId, long actId, long itemId, int num) {
		if (actId <= 0L || num <= 0) {
			return;
		}
		boolean multi = promotionGroupsRelGoodsReadService.hasRelRows(companyId, actId);
		if (multi && itemId <= 0L) {
			throw new ResourceException("该规格未参与拼团");
		}
		String key = resolveStoreKey(multi, actId, itemId);
		Long store = companysRedisTemplate.opsForValue().increment(key, num);
		if (store == null) {
			return;
		}
		if (multi) {
			promotionGroupsRelGoodsMapper.update(
					null,
					new UpdateWrapper<PromotionGroupsRelGoods>()
							.eq("company_id", companyId)
							.eq("groups_activity_id", actId)
							.eq("item_id", itemId)
							.set("activity_store", store));
			promotionGroupsActivityMapper.update(
					null,
					new UpdateWrapper<PromotionGroupsActivity>()
							.eq("groups_activity_id", actId)
							.setSql("store = store + " + num));
		} else {
			promotionGroupsActivityMapper.update(
					null,
					new UpdateWrapper<PromotionGroupsActivity>()
							.eq("groups_activity_id", actId)
							.set("store", store));
		}
	}

	private static String resolveStoreKey(boolean multi, long actId, long itemId) {
		return multi ? redisStoreKey(actId, itemId) : legacyRedisStoreKey(actId);
	}

	private static String redisStoreKey(long actId, long itemId) {
		return "group_item_store:" + actId + ":" + itemId;
	}

	private static String legacyRedisStoreKey(long actId) {
		return "group_item_store:" + actId;
	}
}
