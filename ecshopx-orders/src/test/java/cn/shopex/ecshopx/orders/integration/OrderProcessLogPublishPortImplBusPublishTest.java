package cn.shopex.ecshopx.orders.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.integration.OrderProcessLogPublishPortImpl;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@DisplayName("OrderProcessLogPublishPortImpl Bus publish")
class OrderProcessLogPublishPortImplBusPublishTest {

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clear();
		}
	}

	@Test
	void publish_whenNoTransaction_invokesDispatchFacadePublishEventOnceWithEventNameAndPayload() {
		DispatchFacade facade = mock(DispatchFacade.class);
		OrderProcessLogPublishPortImpl port = new OrderProcessLogPublishPortImpl(facade);

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("order_id", 7001L);
		entities.put("company_id", 7002L);
		entities.put("remarks", "订单售后");

		port.publish(entities);

		verify(facade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						eq(entities),
						eq(DispatchOptions.oplQueuedAfterCommit()));
	}

	@Test
	void publish_whenTransactionCommits_invokesPublishEventAfterCommitNotBefore() {
		DispatchFacade facade = mock(DispatchFacade.class);
		OrderProcessLogPublishPortImpl port = new OrderProcessLogPublishPortImpl(facade);

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

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("order_id", 8001L);
		entities.put("company_id", 8002L);
		entities.put("remarks", "订单售后");

		tt.executeWithoutResult(
				st -> {
					port.publish(entities);
					verifyNoInteractions(facade);
				});

		verify(facade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						eq(entities),
						eq(DispatchOptions.oplQueuedAfterCommit()));
	}
}
