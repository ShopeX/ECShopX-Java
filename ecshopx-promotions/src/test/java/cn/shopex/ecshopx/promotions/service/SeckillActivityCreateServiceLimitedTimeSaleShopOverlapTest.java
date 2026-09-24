package cn.shopex.ecshopx.promotions.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.SavePromotionItemTagJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.SalespersonItemsShelvesJobDispatchPublisher;
import cn.shopex.ecshopx.promotions.domain.SeckillActivity;
import cn.shopex.ecshopx.promotions.domain.SeckillRelGoods;
import cn.shopex.ecshopx.promotions.mapper.PromotionsItemsTagMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillRelGoodsMapper;
import cn.shopex.ecshopx.promotions.service.multilang.SeckillActivityOutsideMultiLangWriteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class SeckillActivityCreateServiceLimitedTimeSaleShopOverlapTest {

	private static final long COMPANY_ID = 11L;
	private static final long SECKILL_ID = 901L;
	private static final long OTHER_SECKILL_ID = 888L;
	private static final long SHOP_ID = 3100L;

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clear();
		}
	}

	@Test
	void createLimitedTimeSale_sameShopDifferentActivity_doesNotRejectByShopMutex_andStillGuardsItemOverlap() {
		MarketingActivityCrossPromotionGuardService guard = mock(MarketingActivityCrossPromotionGuardService.class);
		doNothing().when(guard).checkActivityValidBySecKill(anyMap());
		SeckillActivityMapper seckillActivityMapper = mock(SeckillActivityMapper.class);
		when(seckillActivityMapper.selectList(any())).thenReturn(List.of(overlappingShopActivity()));
		when(seckillActivityMapper.insert(isA(SeckillActivity.class)))
				.thenAnswer(
						invocation -> {
							SeckillActivity entity = invocation.getArgument(0);
							entity.setSeckillId(SECKILL_ID);
							return 1;
						});

		SeckillActivityCreateService svc = newService(guard, seckillActivityMapper, mockRelMapper(), false);
		initSync();

		Map<String, Object> row =
				assertDoesNotThrow(() -> svc.createSeckillActivity(limitedTimeSaleParams(null), "zh-CN"));
		assertNotNull(row);
		verify(guard).checkActivityValidBySecKill(anyMap());
	}

	@Test
	void updateLimitedTimeSale_sameShopDifferentActivity_doesNotRejectByShopMutex_andStillGuardsItemOverlap() {
		int nowSec = (int) (System.currentTimeMillis() / 1000L);
		int release = nowSec + 3_600;
		int start = nowSec + 7_200;
		int end = nowSec + 86_400;

		MarketingActivityCrossPromotionGuardService guard = mock(MarketingActivityCrossPromotionGuardService.class);
		doNothing().when(guard).checkActivityValidBySecKill(anyMap());

		SeckillActivity existing = new SeckillActivity();
		existing.setCompanyId(COMPANY_ID);
		existing.setSeckillId(SECKILL_ID);
		existing.setDisabled(false);
		existing.setActivityReleaseTime(release);
		existing.setActivityStartTime(start);
		existing.setActivityEndTime(end);
		existing.setSeckillType("limited_time_sale");
		existing.setActivityName("old-name");
		existing.setValidityPeriod(5);
		existing.setLimitTotalMoney(0L);
		existing.setLimitMoney(0L);
		existing.setUseBound(1);
		existing.setItemType("normal");
		existing.setDistributorId("," + SHOP_ID + ",");

		SeckillActivityMapper seckillActivityMapper = mock(SeckillActivityMapper.class);
		when(seckillActivityMapper.selectOne(any())).thenReturn(existing);
		when(seckillActivityMapper.selectList(any())).thenReturn(List.of(overlappingShopActivity()));
		when(seckillActivityMapper.updateById(isA(SeckillActivity.class))).thenReturn(1);
		when(seckillActivityMapper.selectById(eq(SECKILL_ID))).thenReturn(existing);

		SeckillActivityCreateService svc = newService(guard, seckillActivityMapper, mockRelMapper(), true);
		initSync();

		Map<String, Object> params = limitedTimeSaleParams(SECKILL_ID);
		params.put("activity_start_time", start);
		params.put("activity_end_time", end);
		params.put("activity_release_time", release);
		Map<String, Object> row = assertDoesNotThrow(() -> svc.updateSeckillActivity(params, "zh-CN"));
		assertNotNull(row);
		verify(guard).checkActivityValidBySecKill(anyMap());
	}

	private static SeckillActivity overlappingShopActivity() {
		SeckillActivity other = new SeckillActivity();
		other.setCompanyId(COMPANY_ID);
		other.setSeckillId(OTHER_SECKILL_ID);
		other.setDisabled(false);
		other.setSeckillType("limited_time_sale");
		other.setDistributorId("," + SHOP_ID + ",");
		other.setActivityStartTime(1_000);
		other.setActivityEndTime(2_000_000_000);
		other.setActivityName("existing-shop-sale");
		return other;
	}

	private static Map<String, Object> limitedTimeSaleParams(Long seckillId) {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", COMPANY_ID);
		if (seckillId != null) {
			params.put("seckill_id", seckillId);
		}
		params.put("activity_name", "act-name");
		params.put("activity_start_time", 1_000);
		params.put("activity_end_time", 2_000);
		params.put("activity_release_time", 500);
		params.put("validity_period", 5);
		params.put("seckill_type", "limited_time_sale");
		params.put("use_bound", 1);
		params.put("source_id", 0L);
		params.put("distributor_id", String.valueOf(SHOP_ID));
		params.put("limit_total_money", 0L);
		params.put("limit_money", 0L);
		Map<String, Object> itemRow = new LinkedHashMap<>();
		itemRow.put("item_id", 100L);
		itemRow.put("item_title", "t");
		itemRow.put("activity_price", "9.99");
		itemRow.put("activity_store", 10);
		itemRow.put("limit_num", 5);
		params.put("items", List.of(itemRow));
		return params;
	}

	private static SeckillRelGoodsMapper mockRelMapper() {
		SeckillRelGoodsMapper seckillRelGoodsMapper = mock(SeckillRelGoodsMapper.class);
		when(seckillRelGoodsMapper.insert(isA(SeckillRelGoods.class))).thenReturn(1);
		return seckillRelGoodsMapper;
	}

	private static SeckillActivityCreateService newService(
			MarketingActivityCrossPromotionGuardService guard,
			SeckillActivityMapper seckillActivityMapper,
			SeckillRelGoodsMapper seckillRelGoodsMapper,
			boolean update) {
		MessageSource messageSource = mock(MessageSource.class);
		when(messageSource.getMessage(anyString(), nullable(Object[].class), any(Locale.class)))
				.thenAnswer(invocation -> String.valueOf(invocation.getArgument(0)));

		var catalogAccess = mock(cn.shopex.ecshopx.common.goods.MarketingActivityCatalogAccess.class);
		when(catalogAccess.anyGiftItem(anyLong(), anyList())).thenReturn(false);
		Map<String, Object> skuRow = new LinkedHashMap<>();
		skuRow.put("item_id", 100L);
		skuRow.put("default_item_id", 100L);
		skuRow.put("item_name", "n");
		skuRow.put("pics", "");
		skuRow.put("type", "normal");
		when(catalogAccess.loadSkuItemsList(eq(COMPANY_ID), anyList())).thenReturn(Map.of("list", List.of(skuRow)));

		PromotionsItemsTagMapper promotionsItemsTagMapper = mock(PromotionsItemsTagMapper.class);
		SeckillActivityOutsideMultiLangWriteService multiLang = mock(SeckillActivityOutsideMultiLangWriteService.class);
		if (update) {
			doNothing().when(multiLang).updateForSeckill(eq(SECKILL_ID), eq(COMPANY_ID), anyMap(), anyString());
		} else {
			doNothing().when(multiLang).addForNewSeckill(eq(SECKILL_ID), eq(COMPANY_ID), anyMap(), anyString());
		}

		SeckillActivityItemStoreWriteService itemStore = mock(SeckillActivityItemStoreWriteService.class);
		doNothing().when(itemStore).hsetStore(anyLong(), anyLong(), anyLong(), anyInt());
		doNothing().when(itemStore).expireAtEndPlusOneDay(anyLong(), anyLong(), any(Integer.class));

		SavePromotionItemTagJobDispatchPublisher tagPublisher = mock(SavePromotionItemTagJobDispatchPublisher.class);
		doNothing()
				.when(tagPublisher)
				.publishSavePromotionItemTag(
						anyLong(), anyLong(), anyString(), anyInt(), anyInt(), anyString(), anyList(), anyMap());

		return new SeckillActivityCreateService(
				messageSource,
				catalogAccess,
				guard,
				seckillActivityMapper,
				seckillRelGoodsMapper,
				promotionsItemsTagMapper,
				multiLang,
				itemStore,
				mock(ApplicationEventPublisher.class),
				new ObjectMapper(),
				tagPublisher,
				mock(SalespersonItemsShelvesJobDispatchPublisher.class));
	}

	private static void initSync() {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.initSynchronization();
		}
	}
}
