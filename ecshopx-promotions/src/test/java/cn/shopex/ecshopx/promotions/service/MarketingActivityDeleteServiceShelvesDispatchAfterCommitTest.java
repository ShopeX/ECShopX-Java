package cn.shopex.ecshopx.promotions.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.SalespersonItemsShelvesJobDispatchPublisher;
import cn.shopex.ecshopx.promotions.domain.MarketingActivity;
import cn.shopex.ecshopx.promotions.event.MarketingActivityCommittedEvent;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityItemsMapper;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.MarketingGiftItemsMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionsItemsTagMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

class MarketingActivityDeleteServiceShelvesDispatchAfterCommitTest {

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
	void deleteMarketingActivity_whenPhysicalDeleteAndTransactionCommits_invokesShelvesPublishBeforeCommittedEvent() {
		MarketingActivity row = new MarketingActivity();
		row.setCompanyId(COMPANY_ID);
		row.setMarketingId(MARKETING_ID);
		row.setMarketingType(MARKETING_TYPE);
		row.setStartTime(2_000);
		row.setEndTime(3_000);

		MarketingActivityItemListActivityQuerySupport querySupport =
				mock(MarketingActivityItemListActivityQuerySupport.class);
		when(querySupport.loadActivityRow(eq(COMPANY_ID), eq(MARKETING_ID))).thenReturn(Optional.of(row));
		Map<String, Object> snapshotStub = new LinkedHashMap<>();
		snapshotStub.put("status", "waiting");
		when(querySupport.buildActivityPayloadForItemList(same(row))).thenReturn(snapshotStub);

		MarketingActivityListMultiLangReadService listMultiLang = mock(MarketingActivityListMultiLangReadService.class);
		doNothing()
				.when(listMultiLang)
				.applyListTranslations(eq(COMPANY_ID), anyList(), anyString());

		MarketingActivityMapper marketingActivityMapper = mock(MarketingActivityMapper.class);
		MarketingActivityItemsMapper marketingActivityItemsMapper = mock(MarketingActivityItemsMapper.class);
		MarketingGiftItemsMapper marketingGiftItemsMapper = mock(MarketingGiftItemsMapper.class);
		PromotionsItemsTagMapper promotionsItemsTagMapper = mock(PromotionsItemsTagMapper.class);
		when(marketingActivityMapper.delete(any())).thenReturn(1);
		when(marketingActivityItemsMapper.delete(any())).thenReturn(1);
		when(marketingGiftItemsMapper.delete(any())).thenReturn(1);
		when(promotionsItemsTagMapper.delete(any())).thenReturn(1);

		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		SalespersonItemsShelvesJobDispatchPublisher shelvesPublisher =
				mock(SalespersonItemsShelvesJobDispatchPublisher.class);
		MessageSource messageSource = mock(MessageSource.class);
		when(messageSource.getMessage(anyString(), any(), anyString(), any(Locale.class)))
				.thenAnswer(invocation -> invocation.getArgument(2));

		MarketingActivityDeleteService svc =
				new MarketingActivityDeleteService(
						querySupport,
						listMultiLang,
						marketingActivityMapper,
						marketingActivityItemsMapper,
						marketingGiftItemsMapper,
						promotionsItemsTagMapper,
						applicationEventPublisher,
						shelvesPublisher,
						messageSource);

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

		tt.executeWithoutResult(
				st -> {
					Object out = svc.deleteMarketingActivity(COMPANY_ID, MARKETING_ID, true, "zh-CN");
					assertNotNull(out);
					verifyNoInteractions(shelvesPublisher);
					verifyNoInteractions(applicationEventPublisher);
				});

		InOrder order = inOrder(shelvesPublisher, applicationEventPublisher);
		order.verify(shelvesPublisher).publish(eq(COMPANY_ID), eq(MARKETING_ID), eq(MARKETING_TYPE));
		ArgumentCaptor<MarketingActivityCommittedEvent> eventCaptor =
				ArgumentCaptor.forClass(MarketingActivityCommittedEvent.class);
		order.verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
		verifyNoMoreInteractions(shelvesPublisher);
		verifyNoMoreInteractions(applicationEventPublisher);

		MarketingActivityCommittedEvent published = eventCaptor.getValue();
		assertFalse(published.dispatchSalespersonItemsShelves());
		assertFalse(published.savePromotionItemTag());
	}
}
