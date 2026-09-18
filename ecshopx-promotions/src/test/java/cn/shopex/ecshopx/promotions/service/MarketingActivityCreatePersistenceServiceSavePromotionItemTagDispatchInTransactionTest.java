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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Pre-commit {@link SavePromotionItemTagJobDispatchPublisher} vs {@code afterCommit} shelves/event ordering.
 */
class MarketingActivityCreatePersistenceServiceSavePromotionItemTagDispatchInTransactionTest {

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
	void createInTransaction_invokesSavePromotionItemTagPublishBeforeRegisteringAfterCommit() {
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
		doAnswer(
				inv -> {
					@SuppressWarnings("unchecked")
					Map<String, Object> p = inv.getArgument(1);
					p.put("_tag_job_item_ids", new ArrayList<>(List.of(101L, 102L)));
					p.put("item_type", "normal");
					return null;
				})
				.when(itemRelService)
				.createMarketingItemRel(isA(Map.class), isA(Map.class));
		doNothing().when(itemRelService).createMarketingGiftItemRel(isA(Map.class), isA(Map.class));

		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		SalespersonItemsShelvesJobDispatchPublisher shelvesPublisher =
				mock(SalespersonItemsShelvesJobDispatchPublisher.class);
		SavePromotionItemTagJobDispatchPublisher tagPublisher = mock(SavePromotionItemTagJobDispatchPublisher.class);
		doNothing()
				.when(tagPublisher)
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
						tagPublisher,
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

		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", COMPANY_ID);
		params.put("marketing_type", MARKETING_TYPE);
		params.put("marketing_name", "act-name");
		params.put("marketing_desc", "desc");
		params.put("start_time", 1_000);
		params.put("end_time", 2_000);
		params.put("use_bound", 1);
		params.put("used_platform", 0);
		params.put("use_shop", 0);
		params.put("condition_type", "totalfee");
		params.put("item_type", "normal");
		params.put("item_ids", List.of(101L, 102L));

		TransactionTemplate tt = new TransactionTemplate(txMgr);
		tt.executeWithoutResult(
				st -> {
					Map<String, Object> row = svc.createInTransaction(params, "zh-CN");
					assertNotNull(row);
					verifyNoInteractions(shelvesPublisher);
					verifyNoInteractions(applicationEventPublisher);
					verify(tagPublisher)
							.publishSavePromotionItemTag(
									eq(COMPANY_ID),
									eq(MARKETING_ID),
									eq(MARKETING_TYPE),
									eq(1_000),
									eq(2_000),
									eq("normal"),
									eq(List.of(101L, 102L)),
									eq(Map.of()));
				});
	}

	@Test
	void updateInTransaction_invokesSavePromotionItemTagPublishBeforeRegisteringAfterCommit() {
		MarketingActivity existing = new MarketingActivity();
		existing.setMarketingId(MARKETING_ID);
		existing.setCompanyId(COMPANY_ID);
		existing.setCreated(100);
		existing.setReleaseTime(0);

		MarketingActivityMapper marketingActivityMapper = mock(MarketingActivityMapper.class);
		when(marketingActivityMapper.updateById(isA(MarketingActivity.class))).thenReturn(1);

		MarketingActivityCategoryMapper categoryMapper = mock(MarketingActivityCategoryMapper.class);
		when(categoryMapper.delete(any())).thenReturn(1);

		MarketingActivityMultiLangWriteService multiLang = mock(MarketingActivityMultiLangWriteService.class);
		doNothing()
				.when(multiLang)
				.addForNewActivity(eq(MARKETING_ID), eq(COMPANY_ID), isA(Map.class), eq("zh-CN"));

		MarketingActivityCreateItemRelService itemRelService = mock(MarketingActivityCreateItemRelService.class);
		doAnswer(
				inv -> {
					@SuppressWarnings("unchecked")
					Map<String, Object> p = inv.getArgument(1);
					p.put("_tag_job_item_ids", new ArrayList<>(List.of(201L)));
					p.put("item_type", "tag");
					return null;
				})
				.when(itemRelService)
				.createMarketingItemRel(isA(Map.class), isA(Map.class));
		doNothing().when(itemRelService).createMarketingGiftItemRel(isA(Map.class), isA(Map.class));

		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		SalespersonItemsShelvesJobDispatchPublisher shelvesPublisher =
				mock(SalespersonItemsShelvesJobDispatchPublisher.class);
		SavePromotionItemTagJobDispatchPublisher tagPublisher = mock(SavePromotionItemTagJobDispatchPublisher.class);
		doNothing()
				.when(tagPublisher)
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
						tagPublisher,
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

		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", COMPANY_ID);
		params.put("marketing_id", MARKETING_ID);
		params.put("marketing_type", MARKETING_TYPE);
		params.put("marketing_name", "act-name");
		params.put("marketing_desc", "desc");
		params.put("start_time", 1_000);
		params.put("end_time", 2_000);
		params.put("use_bound", 3);
		params.put("used_platform", 0);
		params.put("use_shop", 0);
		params.put("condition_type", "totalfee");
		params.put("item_type", "normal");
		params.put("tag_ids", List.of(55L));

		TransactionTemplate tt = new TransactionTemplate(txMgr);
		tt.executeWithoutResult(
				st -> {
					Map<String, Object> row = svc.updateInTransaction(existing, params, "zh-CN");
					assertNotNull(row);
					verifyNoInteractions(shelvesPublisher);
					verifyNoInteractions(applicationEventPublisher);
					verify(tagPublisher)
							.publishSavePromotionItemTag(
									eq(COMPANY_ID),
									eq(MARKETING_ID),
									eq(MARKETING_TYPE),
									eq(1_000),
									eq(2_000),
									eq("tag"),
									eq(List.of(201L)),
									eq(Map.of()));
				});
	}

	@Test
	void updateInTransaction_preCommitTagPublish_andCommittedEventSavePromotionItemTagFalse() {
		MarketingActivity existing = new MarketingActivity();
		existing.setMarketingId(MARKETING_ID);
		existing.setCompanyId(COMPANY_ID);
		existing.setCreated(100);
		existing.setReleaseTime(0);

		MarketingActivityMapper marketingActivityMapper = mock(MarketingActivityMapper.class);
		when(marketingActivityMapper.updateById(isA(MarketingActivity.class))).thenReturn(1);

		MarketingActivityCategoryMapper categoryMapper = mock(MarketingActivityCategoryMapper.class);
		when(categoryMapper.delete(any())).thenReturn(1);

		MarketingActivityMultiLangWriteService multiLang = mock(MarketingActivityMultiLangWriteService.class);
		doNothing()
				.when(multiLang)
				.addForNewActivity(eq(MARKETING_ID), eq(COMPANY_ID), isA(Map.class), eq("zh-CN"));

		MarketingActivityCreateItemRelService itemRelService = mock(MarketingActivityCreateItemRelService.class);
		doAnswer(
				inv -> {
					@SuppressWarnings("unchecked")
					Map<String, Object> p = inv.getArgument(1);
					p.put("_tag_job_item_ids", new ArrayList<>(List.of(201L)));
					p.put("item_type", "tag");
					return null;
				})
				.when(itemRelService)
				.createMarketingItemRel(isA(Map.class), isA(Map.class));
		doNothing().when(itemRelService).createMarketingGiftItemRel(isA(Map.class), isA(Map.class));

		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		SalespersonItemsShelvesJobDispatchPublisher shelvesPublisher =
				mock(SalespersonItemsShelvesJobDispatchPublisher.class);
		SavePromotionItemTagJobDispatchPublisher tagPublisher = mock(SavePromotionItemTagJobDispatchPublisher.class);
		doNothing()
				.when(tagPublisher)
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
						tagPublisher,
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

		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", COMPANY_ID);
		params.put("marketing_id", MARKETING_ID);
		params.put("marketing_type", MARKETING_TYPE);
		params.put("marketing_name", "act-name");
		params.put("marketing_desc", "desc");
		params.put("start_time", 1_000);
		params.put("end_time", 2_000);
		params.put("use_bound", 3);
		params.put("used_platform", 0);
		params.put("use_shop", 0);
		params.put("condition_type", "totalfee");
		params.put("item_type", "normal");
		params.put("tag_ids", List.of(55L));

		TransactionTemplate tt = new TransactionTemplate(txMgr);
		tt.executeWithoutResult(
				st -> {
					Map<String, Object> row = svc.updateInTransaction(existing, params, "zh-CN");
					assertNotNull(row);
					verifyNoInteractions(shelvesPublisher);
					verifyNoInteractions(applicationEventPublisher);
					verify(tagPublisher)
							.publishSavePromotionItemTag(
									eq(COMPANY_ID),
									eq(MARKETING_ID),
									eq(MARKETING_TYPE),
									eq(1_000),
									eq(2_000),
									eq("tag"),
									eq(List.of(201L)),
									eq(Map.of()));
				});

		verifyNoMoreInteractions(tagPublisher);
		ArgumentCaptor<MarketingActivityCommittedEvent> eventCaptor =
				ArgumentCaptor.forClass(MarketingActivityCommittedEvent.class);
		InOrder inOrder = inOrder(shelvesPublisher, applicationEventPublisher);
		inOrder.verify(shelvesPublisher).publish(eq(COMPANY_ID), eq(MARKETING_ID), eq(MARKETING_TYPE));
		inOrder.verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
		assertFalse(eventCaptor.getValue().savePromotionItemTag());
		verifyNoMoreInteractions(shelvesPublisher);
	}
}
