package cn.shopex.ecshopx.promotions.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.SalespersonItemsShelvesJobDispatchPublisher;
import cn.shopex.ecshopx.promotions.domain.MarketingActivity;
import cn.shopex.ecshopx.promotions.event.MarketingActivityCommittedEvent;
import cn.shopex.ecshopx.promotions.domain.MarketingActivityItems;
import cn.shopex.ecshopx.promotions.domain.PromotionsItemsTag;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityItemsMapper;
import cn.shopex.ecshopx.promotions.mapper.MarketingActivityMapper;
import cn.shopex.ecshopx.promotions.mapper.MarketingGiftItemsMapper;
import cn.shopex.ecshopx.promotions.mapper.PromotionsItemsTagMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.Locale;
import java.util.Optional;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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

class MarketingActivityDeleteServiceEndActivityShelvesDispatchAfterCommitTest {

	private static final long COMPANY_ID = 11L;
	private static final long MARKETING_ID = 901L;
	private static final String MARKETING_TYPE = "full_minus";

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), MarketingActivityItems.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), PromotionsItemsTag.class);
	}

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clear();
		}
	}

	@Test
	void deleteMarketingActivity_whenEndActivityBranchAndTransactionCommits_invokesShelvesPublishBeforeCommittedEvent() {
		MarketingActivity row = new MarketingActivity();
		row.setCompanyId(COMPANY_ID);
		row.setMarketingId(MARKETING_ID);
		row.setMarketingType(MARKETING_TYPE);
		row.setStartTime(2_000);
		row.setEndTime(3_000);

		MarketingActivityItemListActivityQuerySupport querySupport =
				mock(MarketingActivityItemListActivityQuerySupport.class);
		when(querySupport.loadActivityRow(eq(COMPANY_ID), eq(MARKETING_ID))).thenReturn(Optional.of(row));

		MarketingActivityListMultiLangReadService listMultiLang = mock(MarketingActivityListMultiLangReadService.class);

		MarketingActivityMapper marketingActivityMapper = mock(MarketingActivityMapper.class);
		when(marketingActivityMapper.updateById(any(MarketingActivity.class))).thenReturn(1);

		MarketingActivityItemsMapper marketingActivityItemsMapper = mock(MarketingActivityItemsMapper.class);
		when(marketingActivityItemsMapper.update(eq(null), any())).thenReturn(1);

		MarketingGiftItemsMapper marketingGiftItemsMapper = mock(MarketingGiftItemsMapper.class);
		PromotionsItemsTagMapper promotionsItemsTagMapper = mock(PromotionsItemsTagMapper.class);
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
					Object out = svc.deleteMarketingActivity(COMPANY_ID, MARKETING_ID, false, "zh-CN");
					assertEquals(Boolean.TRUE, out);
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
