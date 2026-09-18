package cn.shopex.ecshopx.promotions.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess;
import cn.shopex.ecshopx.promotions.domain.PromotionGroupsRelGoods;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsRelGoodsMapper;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class PromotionGroupsRelGoodsWriteServiceReplaceRelGoodsTest {

	private static final long COMPANY_ID = 1L;
	private static final long ACT_ID = 42L;
	private static final int NOW = 1_700_000_000;

	private PromotionGroupsRelGoodsMapper relGoodsMapper;
	private StringRedisTemplate redisTemplate;
	private ValueOperations<String, String> valueOps;
	private PromotionGroupsRelGoodsWriteService writeService;

	@BeforeEach
	void setUp() {
		MessageSource messageSource = mock(MessageSource.class);
		relGoodsMapper = mock(PromotionGroupsRelGoodsMapper.class);
		redisTemplate = mock(StringRedisTemplate.class);
		valueOps = mock(ValueOperations.class);
		when(redisTemplate.opsForValue()).thenReturn(valueOps);

		writeService =
				new PromotionGroupsRelGoodsWriteService(
						relGoodsMapper,
						mock(PromotionGroupsRelGoodsReadService.class),
						mock(MarketingActivityCatalogAccess.class),
						mock(PromotionGroupsActivityGroupParamValidationService.class),
						messageSource,
						redisTemplate);
	}

	@Test
	void replaceRelGoods_deletesRedisKeysForRemovedSkus() {
		PromotionGroupsRelGoods kept = relRow(100L);
		PromotionGroupsRelGoods removed = relRow(200L);
		when(relGoodsMapper.selectList(any())).thenReturn(List.of(kept, removed));

		List<PromotionGroupsRelGoodsWriteService.NormalizedGroupSkuItem> items =
				List.of(
						new PromotionGroupsRelGoodsWriteService.NormalizedGroupSkuItem(
								100L, 990L, 10L, "Kept SKU", "pic-a", "Red / L"));

		writeService.replaceRelGoods(ACT_ID, COMPANY_ID, items, NOW);

		verify(redisTemplate).delete("group_item_store:" + ACT_ID + ":200");
		verify(redisTemplate).delete("group_item_store:" + ACT_ID);
		verify(valueOps).set(eq("group_item_store:" + ACT_ID + ":100"), eq("10"));
	}

	private static PromotionGroupsRelGoods relRow(long itemId) {
		PromotionGroupsRelGoods row = new PromotionGroupsRelGoods();
		row.setId(itemId);
		row.setCompanyId(COMPANY_ID);
		row.setGroupsActivityId(ACT_ID);
		row.setItemId(itemId);
		return row;
	}
}
