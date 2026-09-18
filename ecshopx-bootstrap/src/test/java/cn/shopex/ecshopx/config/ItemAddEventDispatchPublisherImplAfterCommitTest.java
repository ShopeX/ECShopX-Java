package cn.shopex.ecshopx.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.GoodsDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

class ItemAddEventDispatchPublisherImplAfterCommitTest {

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clear();
		}
	}

	@Test
	void schedulePublish_registersAfterCommit_andPublishEventInvokedOnlyAfterCommit() {
		DispatchFacade facade = mock(DispatchFacade.class);
		ItemAddEventDispatchPublisherImpl publisher = new ItemAddEventDispatchPublisherImpl(facade);

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

		tt.executeWithoutResult(
				st -> {
					publisher.schedulePublishAfterCommit(99L, 7L);
					verifyNoInteractions(facade);
				});

		Map<String, Object> expectedPayload = new LinkedHashMap<>();
		expectedPayload.put("item_id", 99L);
		expectedPayload.put("company_id", 7L);
		verify(facade, times(1))
				.publishEvent(
						eq(GoodsDispatchEventNames.EVENT_ITEM_ADD),
						eq(expectedPayload),
						eq(
								new DispatchOptions(
										DispatchMode.ASYNC,
										DispatchDriverType.REDIS,
										null,
										null,
										RetryPolicy.platformDefault())));
	}

	@Test
	void schedulePublish_whenNoTransaction_invokesPublishEventImmediately() {
		DispatchFacade facade = mock(DispatchFacade.class);
		ItemAddEventDispatchPublisherImpl publisher = new ItemAddEventDispatchPublisherImpl(facade);

		publisher.schedulePublishAfterCommit(12L, 34L);

		Map<String, Object> expectedPayload = new LinkedHashMap<>();
		expectedPayload.put("item_id", 12L);
		expectedPayload.put("company_id", 34L);
		verify(facade, times(1))
				.publishEvent(
						eq(GoodsDispatchEventNames.EVENT_ITEM_ADD),
						eq(expectedPayload),
						eq(
								new DispatchOptions(
										DispatchMode.ASYNC,
										DispatchDriverType.REDIS,
										null,
										null,
										RetryPolicy.platformDefault())));
	}
}
