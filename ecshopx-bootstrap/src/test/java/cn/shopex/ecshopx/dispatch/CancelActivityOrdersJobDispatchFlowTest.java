package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.CommunityDispatchJobNames;
import cn.shopex.ecshopx.common.port.orders.CommunityActivityCancelOrderRow;
import cn.shopex.ecshopx.config.CancelActivityOrdersJobDispatchPublisherImpl;
import cn.shopex.ecshopx.orders.dispatch.CancelActivityOrdersJobHandler;
import cn.shopex.ecshopx.orders.service.CommunityActivityCancelOrderExecutor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CancelActivityOrdersJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andHandlerInvokesCancelExecutor() {
		CommunityActivityCancelOrderExecutor executor = mock(CommunityActivityCancelOrderExecutor.class);
		CancelActivityOrdersJobHandler handler = new CancelActivityOrdersJobHandler(executor);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CommunityDispatchJobNames.CANCEL_ACTIVITY_ORDERS_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> row1 = new LinkedHashMap<>();
		row1.put("company_id", 1L);
		row1.put("order_id", 101L);
		row1.put("cancel_reason", "已成团，取消未支付订单");
		row1.put("user_id", 7L);
		row1.put("mobile", "13800000001");
		row1.put("cancel_from", "chief");
		row1.put("chief_id", 55L);
		Map<String, Object> row2 = new LinkedHashMap<>();
		row2.put("company_id", 1L);
		row2.put("order_id", 102L);
		row2.put("cancel_reason", "已成团，取消未支付订单");
		row2.put("user_id", 8L);
		row2.put("mobile", "13800000002");
		row2.put("cancel_from", "chief");
		row2.put("chief_id", 55L);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("rows", List.of(row1, row2));

		facade.dispatchJob(
				CommunityDispatchJobNames.CANCEL_ACTIVITY_ORDERS_JOB,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals("slow", msg.queue());
		assertEquals(CommunityDispatchJobNames.CANCEL_ACTIVITY_ORDERS_JOB, msg.messageName());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		ArgumentCaptor<CommunityActivityCancelOrderRow> rowCap = ArgumentCaptor.forClass(CommunityActivityCancelOrderRow.class);
		verify(executor, times(2)).execute(rowCap.capture());
		List<CommunityActivityCancelOrderRow> executed = rowCap.getAllValues();
		assertEquals(101L, executed.get(0).getOrderId());
		assertEquals("chief", executed.get(0).getCancelFrom());
		assertEquals(55L, executed.get(0).getChiefId().longValue());
		assertEquals(102L, executed.get(1).getOrderId());
	}

	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void dispatchJob_async_payloadMatchesCancelActivityOrdersPublisherEnvelope() {
		DispatchFacade facade = mock(DispatchFacade.class);

		CancelActivityOrdersJobDispatchPublisherImpl publisher = new CancelActivityOrdersJobDispatchPublisherImpl(facade);

		CommunityActivityCancelOrderRow r1 = new CommunityActivityCancelOrderRow();
		r1.setCompanyId(9L);
		r1.setOrderId(2001L);
		r1.setCancelReason("成团失败，取消订单并退款");
		r1.setUserId(3L);
		r1.setMobile("13900000001");
		r1.setCancelFrom("chief");
		r1.setChiefId(12L);
		CommunityActivityCancelOrderRow r2 = new CommunityActivityCancelOrderRow();
		r2.setCompanyId(9L);
		r2.setOrderId(2002L);
		r2.setCancelReason("成团失败，取消订单并退款");
		r2.setUserId(4L);
		r2.setMobile("13900000002");
		r2.setCancelFrom("chief");
		r2.setChiefId(12L);

		publisher.publishBatch(List.of(r1, r2));

		ArgumentCaptor<Map<String, Object>> payloadCap = ArgumentCaptor.forClass(Map.class);
		ArgumentCaptor<DispatchOptions> optsCap = ArgumentCaptor.forClass(DispatchOptions.class);
		verify(facade)
				.dispatchJob(eq(CommunityDispatchJobNames.CANCEL_ACTIVITY_ORDERS_JOB), payloadCap.capture(), optsCap.capture());

		DispatchOptions opts = optsCap.getValue();
		assertEquals(DispatchMode.ASYNC, opts.mode());
		assertEquals(DispatchDriverType.REDIS, opts.driverOverride());
		assertEquals("slow", opts.queue());
		assertEquals(RetryPolicy.platformDefault(), opts.retryPolicy());

		Map<String, Object> p = payloadCap.getValue();
		assertEquals(1, p.size());
		Object rowsObj = p.get("rows");
		assertInstanceOf(List.class, rowsObj);
		List<?> rows = (List<?>) rowsObj;
		assertEquals(2, rows.size());
		Map<String, Object> m0 = (Map<String, Object>) rows.get(0);
		assertEquals(9L, ((Number) m0.get("company_id")).longValue());
		assertEquals(2001L, ((Number) m0.get("order_id")).longValue());
		assertEquals("成团失败，取消订单并退款", m0.get("cancel_reason"));
		assertEquals(3L, ((Number) m0.get("user_id")).longValue());
		assertEquals("13900000001", m0.get("mobile"));
		assertEquals("chief", m0.get("cancel_from"));
		assertEquals(12L, ((Number) m0.get("chief_id")).longValue());
	}

	@Test
	void runtimeConsume_handCraftedCancelActivityOrdersJob_invokesHandlerWithoutDispatchFacade() {
		CommunityActivityCancelOrderExecutor executor = mock(CommunityActivityCancelOrderExecutor.class);
		CancelActivityOrdersJobHandler handler = new CancelActivityOrdersJobHandler(executor);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(CommunityDispatchJobNames.CANCEL_ACTIVITY_ORDERS_JOB, handler);

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("company_id", 3L);
		row.put("order_id", 5001L);
		row.put("cancel_reason", "成团失败，取消订单并退款");
		row.put("user_id", 9L);
		row.put("mobile", "13800000000");
		row.put("cancel_from", "system");
		Map<String, Object> payload = Map.of("rows", List.of(row));

		DispatchMessage msg =
				new DispatchMessage(
						DispatchMessageType.JOB,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						CommunityDispatchJobNames.CANCEL_ACTIVITY_ORDERS_JOB,
						payload,
						"slow",
						null,
						RetryPolicy.platformDefault(),
						Instant.now(),
						"trace-cancel-activity-orders-consumer-schedule-finish",
						null);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		ArgumentCaptor<CommunityActivityCancelOrderRow> rowCap = ArgumentCaptor.forClass(CommunityActivityCancelOrderRow.class);
		verify(executor, times(1)).execute(rowCap.capture());
		CommunityActivityCancelOrderRow executed = rowCap.getValue();
		assertEquals("system", executed.getCancelFrom());
		assertNull(executed.getChiefId());
		assertEquals(5001L, executed.getOrderId());
	}

	/**
	 * Chief manual {@code fail} / {@code success} status updates assemble cancel rows with fixed reason copy; those
	 * batches must still route through {@code dispatchJob} as async Redis jobs on {@code slow}.
	 */
	@Test
	@SuppressWarnings({"unchecked", "rawtypes"})
	void chiefManualActivityFailOrSuccess_publishBatchDispatchesAsyncOnSlowWithExpectedCancelReasons() {
		DispatchFacade facade = mock(DispatchFacade.class);
		CancelActivityOrdersJobDispatchPublisherImpl publisher = new CancelActivityOrdersJobDispatchPublisherImpl(facade);

		CommunityActivityCancelOrderRow failRow = new CommunityActivityCancelOrderRow();
		failRow.setCompanyId(1L);
		failRow.setOrderId(9001L);
		failRow.setCancelReason("成团失败，取消订单并退款");
		failRow.setUserId(11L);
		failRow.setMobile("13700000001");
		failRow.setCancelFrom("chief");
		failRow.setChiefId(77L);
		publisher.publishBatch(List.of(failRow));

		ArgumentCaptor<Map<String, Object>> failPayload = ArgumentCaptor.forClass(Map.class);
		ArgumentCaptor<DispatchOptions> failOpts = ArgumentCaptor.forClass(DispatchOptions.class);
		verify(facade)
				.dispatchJob(eq(CommunityDispatchJobNames.CANCEL_ACTIVITY_ORDERS_JOB), failPayload.capture(), failOpts.capture());
		assertEquals(DispatchMode.ASYNC, failOpts.getValue().mode());
		assertEquals("slow", failOpts.getValue().queue());
		Map<String, Object> failP = failPayload.getValue();
		List<?> failRows = (List<?>) failP.get("rows");
		assertEquals(1, failRows.size());
		assertEquals("成团失败，取消订单并退款", ((Map<?, ?>) failRows.get(0)).get("cancel_reason"));

		reset(facade);

		CommunityActivityCancelOrderRow successRow = new CommunityActivityCancelOrderRow();
		successRow.setCompanyId(1L);
		successRow.setOrderId(9002L);
		successRow.setCancelReason("已成团，取消未支付订单");
		successRow.setUserId(12L);
		successRow.setMobile("13700000002");
		successRow.setCancelFrom("chief");
		successRow.setChiefId(77L);
		publisher.publishBatch(List.of(successRow));

		ArgumentCaptor<Map<String, Object>> okPayload = ArgumentCaptor.forClass(Map.class);
		ArgumentCaptor<DispatchOptions> okOpts = ArgumentCaptor.forClass(DispatchOptions.class);
		verify(facade)
				.dispatchJob(eq(CommunityDispatchJobNames.CANCEL_ACTIVITY_ORDERS_JOB), okPayload.capture(), okOpts.capture());
		assertEquals(DispatchMode.ASYNC, okOpts.getValue().mode());
		assertEquals(DispatchDriverType.REDIS, okOpts.getValue().driverOverride());
		Map<String, Object> okP = okPayload.getValue();
		List<?> okRows = (List<?>) okP.get("rows");
		assertEquals(1, okRows.size());
		assertEquals("已成团，取消未支付订单", ((Map<?, ?>) okRows.get(0)).get("cancel_reason"));
	}
}
