package cn.shopex.ecshopx.promotions.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.SavePromotionItemTagJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.SalespersonItemsShelvesJobDispatchPublisher;
import cn.shopex.ecshopx.promotions.domain.MarketingActivity;
import cn.shopex.ecshopx.promotions.event.MarketingActivityCommittedEvent;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityCategoryMapper;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityMapper;
import cn.shopex.ecshopx.promotions.service.multilang.MarketingActivityMultiLangWriteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

class MarketingActivityCreatePersistenceServiceShelvesDispatchAfterCommitTest {

	private static final long COMPANY_ID = 11L;
	private static final long MARKETING_ID = 901L;
	private static final String MARKETING_TYPE = "full_minus";

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clear();
		}
	}

	@Test
	void createInTransaction_whenTransactionCommits_invokesSalespersonItemsShelvesJobPublishOnceAfterCommit() {
		MarketingActivityMapper marketingActivityMapper = mock(MarketingActivityMapper.class);
		when(marketingActivityMapper.insert(isA(MarketingActivity.class)))
				.thenAnswer(
						invocation -> {
							MarketingActivity entity = invocation.getArgument(0);
							entity.setMarketingId(MARKETING_ID);
							return 1;
						});

		MarketingActivityCategoryMapper categoryMapper = mock(MarketingActivityCategoryMapper.class);
		MarketingActivityMultiLangWriteService multiLang = mock(MarketingActivityMultiLangWriteService.class);
		doNothing()
				.when(multiLang)
				.addForNewActivity(eq(MARKETING_ID), eq(COMPANY_ID), isA(Map.class), eq("zh-CN"));

		MarketingActivityCreateItemRelService itemRelService = mock(MarketingActivityCreateItemRelService.class);
		doNothing().when(itemRelService).createMarketingItemRel(isA(Map.class), isA(Map.class));
		doNothing().when(itemRelService).createMarketingGiftItemRel(isA(Map.class), isA(Map.class));

		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		SalespersonItemsShelvesJobDispatchPublisher shelvesPublisher =
				mock(SalespersonItemsShelvesJobDispatchPublisher.class);
		SavePromotionItemTagJobDispatchPublisher savePromotionItemTagJobDispatchPublisher =
				mock(SavePromotionItemTagJobDispatchPublisher.class);
		doNothing()
				.when(savePromotionItemTagJobDispatchPublisher)
				.publishSavePromotionItemTag(
						anyLong(), anyLong(), anyString(), anyInt(), anyInt(), anyString(), anyList(), anyMap());

		MarketingActivityCreatePersistenceService svc =
				new MarketingActivityCreatePersistenceService(
						marketingActivityMapper,
						categoryMapper,
						multiLang,
						itemRelService,
						applicationEventPublisher,
						shelvesPublisher,
						savePromotionItemTagJobDispatchPublisher,
						new ObjectMapper());

		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(org.mockito.ArgumentMatchers.any()))
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
				.commit(org.mockito.ArgumentMatchers.any(TransactionStatus.class));

		TransactionTemplate tt = new TransactionTemplate(txMgr);
		Map<String, Object> params = baselineCreateParams();

		tt.executeWithoutResult(
				st -> {
					Map<String, Object> row = svc.createInTransaction(params, "zh-CN");
					assertNotNull(row);
					verifyNoInteractions(shelvesPublisher);
					verifyNoInteractions(applicationEventPublisher);
					verify(savePromotionItemTagJobDispatchPublisher)
							.publishSavePromotionItemTag(
									eq(COMPANY_ID),
									eq(MARKETING_ID),
									eq(MARKETING_TYPE),
									eq(1_000),
									eq(2_000),
									eq("normal"),
									eq(List.of()),
									eq(Map.of()));
				});

		verifyNoMoreInteractions(savePromotionItemTagJobDispatchPublisher);
		verify(shelvesPublisher).publish(eq(COMPANY_ID), eq(MARKETING_ID), eq(MARKETING_TYPE));
		verifyNoMoreInteractions(shelvesPublisher);

		ArgumentCaptor<MarketingActivityCommittedEvent> eventCaptor =
				ArgumentCaptor.forClass(MarketingActivityCommittedEvent.class);
		verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
		MarketingActivityCommittedEvent published = eventCaptor.getValue();
		assertFalse(published.dispatchSalespersonItemsShelves());
		assertFalse(published.savePromotionItemTag());
	}

	private static Map<String, Object> baselineCreateParams() {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", COMPANY_ID);
		params.put("marketing_type", MARKETING_TYPE);
		params.put("marketing_name", "act-name");
		params.put("marketing_desc", "desc");
		params.put("start_time", 1_000);
		params.put("end_time", 2_000);
		params.put("use_bound", 0);
		params.put("used_platform", 0);
		params.put("use_shop", 0);
		params.put("condition_type", "totalfee");
		params.put("item_type", "normal");
		return params;
	}
}
