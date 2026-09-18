package cn.shopex.ecshopx.promotions.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsRelGoods;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsRelGoodsMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.data.redis.core.StringRedisTemplate;

class PromotionGroupsRelGoodsWriteServiceCleanupTest {

	private static final long COMPANY_ID = 1L;
	private static final long ACT_ID = 42L;

	private PromotionGroupsRelGoodsMapper relGoodsMapper;
	private PromotionGroupsRelGoodsReadService relGoodsReadService;
	private StringRedisTemplate redisTemplate;
	private PromotionGroupsRelGoodsWriteService writeService;

	@BeforeEach
	void setUp() {
		relGoodsMapper = mock(PromotionGroupsRelGoodsMapper.class);
		relGoodsReadService = mock(PromotionGroupsRelGoodsReadService.class);
		redisTemplate = mock(StringRedisTemplate.class);
		writeService =
				new PromotionGroupsRelGoodsWriteService(
						relGoodsMapper,
						relGoodsReadService,
						mock(MarketingActivityCatalogAccess.class),
						mock(PromotionGroupsActivityGroupParamValidationService.class),
						mock(MessageSource.class),
						redisTemplate);
	}

	@Test
	void cleanupRelRowsAndRedisKeys_deletesPerSkuAndLegacyKeysAndRelRows() {
		when(relGoodsReadService.listByActivityId(COMPANY_ID, ACT_ID))
				.thenReturn(List.of(relRow(100L), relRow(200L)));

		writeService.cleanupRelRowsAndRedisKeys(COMPANY_ID, ACT_ID);

		verify(redisTemplate).delete("group_item_store:" + ACT_ID + ":100");
		verify(redisTemplate).delete("group_item_store:" + ACT_ID + ":200");
		verify(redisTemplate).delete("group_item_store:" + ACT_ID);
		verify(relGoodsMapper).delete(any());
	}

	@Test
	void cleanupRelRowsAndRedisKeys_deletesLegacyKeyWhenNoRelRows() {
		when(relGoodsReadService.listByActivityId(COMPANY_ID, ACT_ID)).thenReturn(List.of());

		writeService.cleanupRelRowsAndRedisKeys(COMPANY_ID, ACT_ID);

		verify(redisTemplate).delete("group_item_store:" + ACT_ID);
	}

	private static PromotionGroupsRelGoods relRow(long itemId) {
		PromotionGroupsRelGoods row = new PromotionGroupsRelGoods();
		row.setCompanyId(COMPANY_ID);
		row.setGroupsActivityId(ACT_ID);
		row.setItemId(itemId);
		return row;
	}
}
