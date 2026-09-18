package cn.shopex.ecshopx.promotions.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.SavePromotionItemTagJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.SalespersonItemsShelvesJobDispatchPublisher;
import cn.shopex.ecshopx.promotions.domain.SeckillActivity;
import cn.shopex.ecshopx.promotions.domain.SeckillRelGoods;
import cn.shopex.ecshopx.promotions.event.SeckillActivityCommittedEvent;
import cn.shopex.ecshopx.promotions.mapper.PromotionsItemsTagMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.SeckillRelGoodsMapper;
import cn.shopex.ecshopx.promotions.service.multilang.SeckillActivityOutsideMultiLangWriteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

class SeckillActivityCreateServiceShelvesDispatchAfterCommitTest {

	private static final long COMPANY_ID = 11L;
	private static final long SECKILL_ID = 901L;

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clear();
		}
	}

	@Test
	void createSeckillActivity_whenTransactionCommits_invokesSalespersonItemsShelvesPublishOnceAfterCommit_withSeckillActivityType() {
		runCreateWithSeckillTypeAndAssertActivityType("normal", "seckill");
	}

	@Test
	void createSeckillActivity_whenTransactionCommits_withLimitedTimeSale_invokesPublishWithLimitedTimeSaleActivityType() {
		runCreateWithSeckillTypeAndAssertActivityType("limited_time_sale", "limited_time_sale");
	}

	private void runCreateWithSeckillTypeAndAssertActivityType(String seckillTypeParam, String expectedBusActivityType) {
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

		var guard = mock(MarketingActivityCrossPromotionGuardService.class);
		doNothing().when(guard).checkActivityValidBySecKill(anyMap());

		SeckillActivityMapper seckillActivityMapper = mock(SeckillActivityMapper.class);
		when(seckillActivityMapper.insert(isA(SeckillActivity.class)))
				.thenAnswer(
						invocation -> {
							SeckillActivity entity = invocation.getArgument(0);
							entity.setSeckillId(SECKILL_ID);
							return 1;
						});

		SeckillRelGoodsMapper seckillRelGoodsMapper = mock(SeckillRelGoodsMapper.class);
		when(seckillRelGoodsMapper.insert(isA(SeckillRelGoods.class))).thenReturn(1);

		PromotionsItemsTagMapper promotionsItemsTagMapper = mock(PromotionsItemsTagMapper.class);
		SeckillActivityOutsideMultiLangWriteService multiLang = mock(SeckillActivityOutsideMultiLangWriteService.class);
		doNothing().when(multiLang).addForNewSeckill(eq(SECKILL_ID), eq(COMPANY_ID), anyMap(), anyString());

		SeckillActivityItemStoreWriteService itemStore = mock(SeckillActivityItemStoreWriteService.class);
		doNothing().when(itemStore).hsetStore(anyLong(), anyLong(), anyLong(), anyInt());
		doNothing().when(itemStore).expireAtEndPlusOneDay(anyLong(), anyLong(), any(Integer.class));

		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		SavePromotionItemTagJobDispatchPublisher tagPublisher = mock(SavePromotionItemTagJobDispatchPublisher.class);
		doNothing()
				.when(tagPublisher)
				.publishSavePromotionItemTag(
						anyLong(), anyLong(), anyString(), anyInt(), anyInt(), anyString(), anyList(), anyMap());
		SalespersonItemsShelvesJobDispatchPublisher shelvesPublisher =
				mock(SalespersonItemsShelvesJobDispatchPublisher.class);

		SeckillActivityCreateService svc =
				new SeckillActivityCreateService(
						messageSource,
						catalogAccess,
						guard,
						seckillActivityMapper,
						seckillRelGoodsMapper,
						promotionsItemsTagMapper,
						multiLang,
						itemStore,
						applicationEventPublisher,
						new ObjectMapper(),
						tagPublisher,
						shelvesPublisher);

		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(any()))
				.thenAnswer(
						inv -> {
							if (!TransactionSynchronizationManager.isSynchronizationActive()) {
								TransactionSynchronizationManager.initSynchronization();
							}
							return new SimpleTransactionStatus(true);
						});
		doAnswer(
				inv -> {
					if (TransactionSynchronizationManager.isSynchronizationActive()) {
						for (TransactionSynchronization synchronization :
								TransactionSynchronizationManager.getSynchronizations()) {
							synchronization.afterCommit();
						}
						TransactionSynchronizationManager.clear();
					}
					return null;
				})
				.when(txMgr)
				.commit(any(TransactionStatus.class));

		TransactionTemplate tt = new TransactionTemplate(txMgr);
		Map<String, Object> params = baselineCreateParams(seckillTypeParam);

		tt.executeWithoutResult(
				st -> {
					Map<String, Object> row = svc.createSeckillActivity(params, "zh-CN");
					assertNotNull(row);
					verify(tagPublisher)
							.publishSavePromotionItemTag(
									eq(COMPANY_ID),
									eq(SECKILL_ID),
									eq(seckillTypeParam),
									eq(500),
									eq(2_000),
									eq("normal"),
									eq(List.of(100L)),
									eq(Map.of(100L, new BigDecimal("9.99"))));
					verifyNoInteractions(shelvesPublisher);
					verifyNoInteractions(applicationEventPublisher);
				});

		verify(shelvesPublisher).publish(eq(COMPANY_ID), eq(SECKILL_ID), eq(expectedBusActivityType));
		verifyNoMoreInteractions(shelvesPublisher);

		ArgumentCaptor<SeckillActivityCommittedEvent> eventCaptor =
				ArgumentCaptor.forClass(SeckillActivityCommittedEvent.class);
		verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
		SeckillActivityCommittedEvent published = eventCaptor.getValue();
		assertFalse(published.dispatchSalespersonItemsShelves());
	}

	private static Map<String, Object> baselineCreateParams(String seckillType) {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", COMPANY_ID);
		params.put("activity_name", "act-name");
		params.put("activity_start_time", 1_000);
		params.put("activity_end_time", 2_000);
		params.put("activity_release_time", 500);
		params.put("validity_period", 5);
		params.put("seckill_type", seckillType);
		params.put("use_bound", 1);
		params.put("source_id", 0L);
		Map<String, Object> itemRow = new LinkedHashMap<>();
		itemRow.put("item_id", 100L);
		itemRow.put("item_title", "t");
		itemRow.put("activity_price", "9.99");
		itemRow.put("activity_store", 10);
		itemRow.put("limit_num", 5);
		params.put("items", List.of(itemRow));
		return params;
	}
}
