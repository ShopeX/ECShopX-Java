package cn.shopex.ecshopx.orders.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
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
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Thin facade slice: shop/wxapp apply-style order-process log map is published through {@link
 * OrderProcessLogPublishPortImpl} such that {@link DispatchFacade#publishEvent} runs after transaction
 * commit (same transaction semantics as {@link OrderProcessLogPublishPortImplBusPublishTest}).
 */
class AftersalesShopApplyOrderProcessLogPortImplFacadeProbeTest {

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clear();
		}
	}

	private static PlatformTransactionManager newCommitFiresAfterSynchronizationsTxManager() {
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
		lenient().doNothing().when(txMgr).rollback(any());
		return txMgr;
	}

	@Test
	void applyHandlePayload_viaPortImpl_registersPublishEventForAfterCommitWhenTransactionActive() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		OrderProcessLogPublishPortImpl port = new OrderProcessLogPublishPortImpl(dispatchFacade);

		PlatformTransactionManager txMgr = newCommitFiresAfterSynchronizationsTxManager();
		TransactionTemplate tx = new TransactionTemplate(txMgr);

		Map<String, Object> sampleMap = new LinkedHashMap<>();
		sampleMap.put("order_id", 100L);
		sampleMap.put("company_id", 10L);
		sampleMap.put("supplier_id", 3L);
		sampleMap.put("operator_type", "user");
		sampleMap.put("operator_id", 20L);
		sampleMap.put("remarks", "订单售后");
		sampleMap.put("detail", "售后单号：9001 后台申请售后，申请原因：probe reason");
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("order_id", 100L);
		params.put("company_id", 10L);
		params.put("reason", "probe reason");
		sampleMap.put("params", params);

		tx.executeWithoutResult(
				status -> {
					port.publish(sampleMap);
					verifyNoInteractions(dispatchFacade);
				});

		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						eq(sampleMap),
						eq(DispatchOptions.oplQueuedAfterCommit()));
	}
}
