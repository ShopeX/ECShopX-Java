package cn.shopex.ecshopx.promotions.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionGroupsRelGoodsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class GroupItemStoreServiceMultiSkuTest {

	private static final long COMPANY_ID = 1L;
	private static final long ACT_ID = 42L;
	private static final long ITEM_ID = 100L;

	private StringRedisTemplate redisTemplate;
	private ValueOperations<String, String> valueOps;
	private PromotionGroupsActivityMapper activityMapper;
	private PromotionGroupsRelGoodsMapper relGoodsMapper;
	private PromotionGroupsRelGoodsReadService relGoodsReadService;
	private GroupItemStoreService service;

	@BeforeEach
	void setUp() {
		redisTemplate = mock(StringRedisTemplate.class);
		valueOps = mock(ValueOperations.class);
		when(redisTemplate.opsForValue()).thenReturn(valueOps);
		activityMapper = mock(PromotionGroupsActivityMapper.class);
		relGoodsMapper = mock(PromotionGroupsRelGoodsMapper.class);
		relGoodsReadService = mock(PromotionGroupsRelGoodsReadService.class);
		service =
				new GroupItemStoreService(
						redisTemplate, activityMapper, relGoodsMapper, relGoodsReadService);
	}

	@Test
	void minusGroupItemStore_multiSku_decrementsPerSkuRedisKey() {
		when(relGoodsReadService.hasRelRows(COMPANY_ID, ACT_ID)).thenReturn(true);
		String key = "group_item_store:" + ACT_ID + ":" + ITEM_ID;
		when(valueOps.increment(key, -2L)).thenReturn(8L);

		assertTrue(service.minusGroupItemStore(COMPANY_ID, ACT_ID, ITEM_ID, 2));

		verify(valueOps).increment(key, -2L);
		verify(relGoodsMapper).update(any(), any());
		verify(activityMapper).update(any(), any());
	}

	@Test
	void minusGroupItemStore_multiSku_insufficientStockRollsBackAndReturnsFalse() {
		when(relGoodsReadService.hasRelRows(COMPANY_ID, ACT_ID)).thenReturn(true);
		String key = "group_item_store:" + ACT_ID + ":" + ITEM_ID;
		when(valueOps.increment(key, -3L)).thenReturn(-1L);

		assertFalse(service.minusGroupItemStore(COMPANY_ID, ACT_ID, ITEM_ID, 3));

		verify(valueOps).increment(key, 3L);
	}

	@Test
	void plusGroupItemStore_multiSku_incrementsPerSkuRedisKey() {
		when(relGoodsReadService.hasRelRows(COMPANY_ID, ACT_ID)).thenReturn(true);
		String key = "group_item_store:" + ACT_ID + ":" + ITEM_ID;
		when(valueOps.increment(key, 2L)).thenReturn(12L);

		service.plusGroupItemStore(COMPANY_ID, ACT_ID, ITEM_ID, 2);

		verify(valueOps).increment(key, 2L);
		verify(relGoodsMapper).update(any(), any());
		verify(activityMapper).update(any(), any());
	}

	@Test
	void minusGroupItemStore_legacyTwoArg_throwsWhenActHasRelRows() {
		when(relGoodsReadService.hasRelRowsByActId(ACT_ID)).thenReturn(true);

		assertThrows(ResourceException.class, () -> service.minusGroupItemStore(ACT_ID, 1));
	}

	@Test
	void plusGroupItemStore_multiSku_throwsWhenItemIdMissing() {
		when(relGoodsReadService.hasRelRows(COMPANY_ID, ACT_ID)).thenReturn(true);

		assertThrows(
				ResourceException.class,
				() -> service.plusGroupItemStore(COMPANY_ID, ACT_ID, 0L, 1));
	}

	@Test
	void minusGroupItemStore_legacyUsesActLevelKey() {
		when(relGoodsReadService.hasRelRows(COMPANY_ID, ACT_ID)).thenReturn(false);
		String key = "group_item_store:" + ACT_ID;
		when(valueOps.increment(key, -1L)).thenReturn(5L);

		assertTrue(service.minusGroupItemStore(COMPANY_ID, ACT_ID, ITEM_ID, 1));

		verify(valueOps).increment(eq(key), eq(-1L));
		verify(activityMapper).update(any(), any());
	}
}
