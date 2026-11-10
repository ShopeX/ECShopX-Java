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

package cn.shopex.ecshopx.goods.search.sync;

import cn.shopex.ecshopx.common.port.goods.GoodsSearchIndexDirtyByItemIdsPort;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsRelGoods;
import cn.shopex.ecshopx.promotions.domain.SeckillRelGoods;
import cn.shopex.ecshopx.promotions.event.SeckillActivityCommittedEvent;
import cn.shopex.ecshopx.promotions.event.SeckillActivityEndCommittedEvent;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsRelGoodsMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillRelGoodsMapper;
import cn.shopex.ecshopx.promotions.service.event.PromotionGroupActivityCommittedEvent;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 秒杀/拼团写路径提交后扇出搜索索引 upsert（不挂钩等级折扣变更）。 */
@Component
public class GoodsSearchIndexPromotionSyncListener {

	private static final Logger log = LoggerFactory.getLogger(GoodsSearchIndexPromotionSyncListener.class);

	private final GoodsSearchIndexDirtyByItemIdsPort dirtyByItemIdsPort;
	private final SeckillRelGoodsMapper seckillRelGoodsMapper;
	private final PromotionGroupsRelGoodsMapper promotionGroupsRelGoodsMapper;

	public GoodsSearchIndexPromotionSyncListener(
			GoodsSearchIndexDirtyByItemIdsPort dirtyByItemIdsPort,
			SeckillRelGoodsMapper seckillRelGoodsMapper,
			PromotionGroupsRelGoodsMapper promotionGroupsRelGoodsMapper) {
		this.dirtyByItemIdsPort = dirtyByItemIdsPort;
		this.seckillRelGoodsMapper = seckillRelGoodsMapper;
		this.promotionGroupsRelGoodsMapper = promotionGroupsRelGoodsMapper;
	}

	@EventListener
	public void onSeckillActivityCommitted(SeckillActivityCommittedEvent event) {
		scheduleSafely(event.companyId(), event.itemIds());
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onSeckillActivityEnded(SeckillActivityEndCommittedEvent event) {
		List<Long> itemIds =
				seckillRelGoodsMapper
						.selectList(
								new LambdaQueryWrapper<SeckillRelGoods>()
										.eq(SeckillRelGoods::getCompanyId, event.companyId())
										.eq(SeckillRelGoods::getSeckillId, event.seckillId()))
						.stream()
						.map(SeckillRelGoods::getItemId)
						.filter(id -> id != null && id > 0)
						.toList();
		scheduleSafely(event.companyId(), itemIds);
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onPromotionGroupCommitted(PromotionGroupActivityCommittedEvent event) {
		List<Long> itemIds =
				promotionGroupsRelGoodsMapper
						.selectList(
								new LambdaQueryWrapper<PromotionGroupsRelGoods>()
										.eq(PromotionGroupsRelGoods::getCompanyId, event.getCompanyId())
										.eq(PromotionGroupsRelGoods::getGroupsActivityId, event.getGroupsActivityId()))
						.stream()
						.map(PromotionGroupsRelGoods::getItemId)
						.filter(id -> id != null && id > 0)
						.toList();
		scheduleSafely(event.getCompanyId(), itemIds);
	}

	private void scheduleSafely(long companyId, List<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return;
		}
		try {
			dirtyByItemIdsPort.scheduleUpsertForItemIds(companyId, itemIds);
		} catch (RuntimeException e) {
			log.warn("schedule search index upsert for promotion items failed companyId={}", companyId, e);
		}
	}
}
