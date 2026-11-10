/**
 * Copyright 2019-2026 ShopeX
 */

package cn.shopex.ecshopx.goods.search.sync;

import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.port.goods.GoodsSearchIndexDirtyByItemIdsPort;
import cn.shopex.ecshopx.promotions.event.SeckillActivityCommittedEvent;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsRelGoodsMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillRelGoodsMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoodsSearchIndexPromotionSyncListenerTest {

	@Mock private GoodsSearchIndexDirtyByItemIdsPort dirtyByItemIdsPort;
	@Mock private SeckillRelGoodsMapper seckillRelGoodsMapper;
	@Mock private PromotionGroupsRelGoodsMapper promotionGroupsRelGoodsMapper;

	private GoodsSearchIndexPromotionSyncListener listener;

	@BeforeEach
	void setUp() {
		listener =
				new GoodsSearchIndexPromotionSyncListener(
						dirtyByItemIdsPort, seckillRelGoodsMapper, promotionGroupsRelGoodsMapper);
	}

	@Test
	void seckillCommitted_schedulesUpsertForItemIds() {
		listener.onSeckillActivityCommitted(new SeckillActivityCommittedEvent(100L, 500L, List.of(42L, 43L), false));
		verify(dirtyByItemIdsPort).scheduleUpsertForItemIds(100L, List.of(42L, 43L));
	}
}
