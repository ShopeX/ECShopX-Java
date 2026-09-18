package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.config.OrderListExportFileJobDispatchHandler;
import cn.shopex.ecshopx.config.OrderListExportFileJobDispatchPublisherImpl;
import cn.shopex.ecshopx.orders.service.invoice.export.InvoiceExportFileJobHandler;
import cn.shopex.ecshopx.orders.service.invoice.export.InvoiceExportJobContext;
import cn.shopex.ecshopx.orders.service.orderexport.OrderExportFileJobHandler;
import cn.shopex.ecshopx.orders.service.orderexport.OrderExportJobContext;
import cn.shopex.ecshopx.orders.service.rights.export.RightsExportFileJobHandler;
import cn.shopex.ecshopx.orders.service.rights.export.RightsExportJobContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

class OrderListExportFileJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesOrderExportHandler() {
		OrderExportFileJobHandler jobHandler = mock(OrderExportFileJobHandler.class);
		InvoiceExportFileJobHandler invoiceHandler = mock(InvoiceExportFileJobHandler.class);
		RightsExportFileJobHandler rightsHandler = mock(RightsExportFileJobHandler.class);
		OrderListExportFileJobDispatchHandler handler =
				new OrderListExportFileJobDispatchHandler(jobHandler, invoiceHandler, rightsHandler);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB_ORDER_LIST, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 501L;
		long operatorId = 502L;
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("pay_sn", "X1");

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", "normal_order");
		payload.put("company_id", companyId);
		payload.put("operator_id", operatorId);
		payload.put("filter", filter);

		facade.dispatchJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_ORDER_LIST,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertEquals(EspierDispatchJobNames.EXPORT_FILE_JOB_ORDER_LIST, msg.messageName());
		assertNull(msg.listenerName());
		assertTrue(msg.payload() instanceof Map);
		Map<?, ?> pl = (Map<?, ?>) msg.payload();
		assertEquals("normal_order", String.valueOf(pl.get("type")));
		assertEquals(companyId, asLong(pl.get("company_id")));
		assertEquals(operatorId, asLong(pl.get("operator_id")));
		assertTrue(pl.get("filter") instanceof Map);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(jobHandler, times(1))
				.run(
						argThat(
								ctx -> {
									if (!(ctx instanceof OrderExportJobContext)) {
										return false;
									}
									OrderExportJobContext c = (OrderExportJobContext) ctx;
									if (c.companyId() != companyId || c.operatorId() != operatorId) {
										return false;
									}
									if (!"normal_order".equals(c.exportType())) {
										return false;
									}
									LinkedHashMap<String, Object> f = c.filter();
									return companyId == asLong(f.get("company_id"))
											&& "X1".equals(String.valueOf(f.get("pay_sn")));
								}));
		verifyNoInteractions(invoiceHandler);
		verifyNoInteractions(rightsHandler);
	}

	@Test
	void dispatchJob_async_invoiceType_enqueuesAndConsumerInvokesInvoiceExportHandler() {
		OrderExportFileJobHandler orderHandler = mock(OrderExportFileJobHandler.class);
		InvoiceExportFileJobHandler invoiceHandler = mock(InvoiceExportFileJobHandler.class);
		RightsExportFileJobHandler rightsHandler = mock(RightsExportFileJobHandler.class);
		OrderListExportFileJobDispatchHandler handler =
				new OrderListExportFileJobDispatchHandler(orderHandler, invoiceHandler, rightsHandler);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB_ORDER_LIST, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 701L;
		long operatorId = 702L;
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("order_type", "normal");

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", "invoice");
		payload.put("company_id", companyId);
		payload.put("operator_id", operatorId);
		payload.put("filter", filter);

		facade.dispatchJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_ORDER_LIST,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(invoiceHandler, times(1))
				.run(
						argThat(
								ctx -> {
									if (!(ctx instanceof InvoiceExportJobContext)) {
										return false;
									}
									InvoiceExportJobContext c = (InvoiceExportJobContext) ctx;
									if (c.companyId() != companyId || c.operatorId() != operatorId) {
										return false;
									}
									if (!"normal".equals(c.orderType())) {
										return false;
									}
									LinkedHashMap<String, Object> f = c.filter();
									return companyId == asLong(f.get("company_id"))
											&& "normal".equals(String.valueOf(f.get("order_type")));
								}));
		verifyNoInteractions(orderHandler);
		verifyNoInteractions(rightsHandler);
	}

	@Test
	void dispatchJob_async_rightType_enqueuesAndConsumerInvokesRightsExportHandler() {
		OrderExportFileJobHandler orderHandler = mock(OrderExportFileJobHandler.class);
		InvoiceExportFileJobHandler invoiceHandler = mock(InvoiceExportFileJobHandler.class);
		RightsExportFileJobHandler rightsHandler = mock(RightsExportFileJobHandler.class);
		OrderListExportFileJobDispatchHandler handler =
				new OrderListExportFileJobDispatchHandler(orderHandler, invoiceHandler, rightsHandler);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB_ORDER_LIST, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 711L;
		long operatorId = 712L;
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("datapass_block", "blk1");

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", "right");
		payload.put("company_id", companyId);
		payload.put("operator_id", operatorId);
		payload.put("filter", filter);

		facade.dispatchJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_ORDER_LIST,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(rightsHandler, times(1))
				.run(
						argThat(
								ctx -> {
									if (!(ctx instanceof RightsExportJobContext)) {
										return false;
									}
									RightsExportJobContext c = (RightsExportJobContext) ctx;
									if (c.companyId() != companyId || c.operatorId() != operatorId) {
										return false;
									}
									LinkedHashMap<String, Object> f = c.filter();
									return companyId == asLong(f.get("company_id"))
											&& "blk1".equals(String.valueOf(f.get("datapass_block")));
								}));
		verifyNoInteractions(orderHandler);
		verifyNoInteractions(invoiceHandler);
	}

	@Test
	void dispatchJob_twoMessages_orderAndInvoice_eachHitsCorrectHandler() {
		OrderExportFileJobHandler orderHandler = mock(OrderExportFileJobHandler.class);
		InvoiceExportFileJobHandler invoiceHandler = mock(InvoiceExportFileJobHandler.class);
		RightsExportFileJobHandler rightsHandler = mock(RightsExportFileJobHandler.class);
		OrderListExportFileJobDispatchHandler handler =
				new OrderListExportFileJobDispatchHandler(orderHandler, invoiceHandler, rightsHandler);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB_ORDER_LIST, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		LinkedHashMap<String, Object> orderFilter = new LinkedHashMap<>();
		orderFilter.put("company_id", 801L);
		LinkedHashMap<String, Object> orderPayload = new LinkedHashMap<>();
		orderPayload.put("type", "normal_order");
		orderPayload.put("company_id", 801L);
		orderPayload.put("operator_id", 802L);
		orderPayload.put("filter", orderFilter);

		LinkedHashMap<String, Object> invFilter = new LinkedHashMap<>();
		invFilter.put("company_id", 901L);
		invFilter.put("order_type", "normal");
		LinkedHashMap<String, Object> invPayload = new LinkedHashMap<>();
		invPayload.put("type", "invoice");
		invPayload.put("company_id", 901L);
		invPayload.put("operator_id", 902L);
		invPayload.put("filter", invFilter);

		DispatchOptions opts =
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault());
		facade.dispatchJob(EspierDispatchJobNames.EXPORT_FILE_JOB_ORDER_LIST, orderPayload, opts);
		facade.dispatchJob(EspierDispatchJobNames.EXPORT_FILE_JOB_ORDER_LIST, invPayload, opts);

		assertEquals(2, captured.size());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(captured.get(0), 1);
		runtime.consume(captured.get(1), 1);

		verify(orderHandler, times(1))
				.run(
						argThat(
								ctx -> {
									if (!(ctx instanceof OrderExportJobContext)) {
										return false;
									}
									return ((OrderExportJobContext) ctx).companyId() == 801L;
								}));
		verify(invoiceHandler, times(1))
				.run(
						argThat(
								ctx -> {
									if (!(ctx instanceof InvoiceExportJobContext)) {
										return false;
									}
									return ((InvoiceExportJobContext) ctx).companyId() == 901L;
								}));
		verifyNoInteractions(rightsHandler);
	}

	@Test
	void dispatchJob_publishPayloadMatchesOrderListEnvelope() {
		DispatchFacade facade = mock(DispatchFacade.class);
		Environment env = mock(Environment.class);
		when(env.acceptsProfiles(Profiles.of("local"))).thenReturn(false);
		OrderListExportFileJobDispatchPublisherImpl publisher =
				new OrderListExportFileJobDispatchPublisherImpl(facade, env);

		LinkedHashMap<String, Object> exportFilter = new LinkedHashMap<>();
		exportFilter.put("company_id", 601L);
		exportFilter.put("pay_sn", "SN1");

		publisher.enqueueOrderListExport(601L, 602L, "service_order", exportFilter);

		verify(facade)
				.dispatchJob(
						eq(EspierDispatchJobNames.EXPORT_FILE_JOB_ORDER_LIST),
						argThat(
								p -> {
									if (!(p instanceof Map)) {
										return false;
									}
									Map<?, ?> m = (Map<?, ?>) p;
									if (!"service_order".equals(String.valueOf(m.get("type")))) {
										return false;
									}
									if (601L != asLong(m.get("company_id")) || 602L != asLong(m.get("operator_id"))) {
										return false;
									}
									Object rawFilter = m.get("filter");
									if (!(rawFilter instanceof Map)) {
										return false;
									}
									Map<?, ?> fm = (Map<?, ?>) rawFilter;
									if (601L != asLong(fm.get("company_id"))) {
										return false;
									}
									return "SN1".equals(String.valueOf(fm.get("pay_sn")));
								}),
						argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.ASYNC
												&& opts.driverOverride() == DispatchDriverType.REDIS
												&& "slow".equals(opts.queue())
												&& opts.delay() == null
												&& RetryPolicy.platformDefault().equals(opts.retryPolicy())));
	}

	@Test
	void dispatchJob_publishInvoicePayloadUsesTypeInvoice() {
		DispatchFacade facade = mock(DispatchFacade.class);
		Environment env = mock(Environment.class);
		when(env.acceptsProfiles(Profiles.of("local"))).thenReturn(false);
		OrderListExportFileJobDispatchPublisherImpl publisher =
				new OrderListExportFileJobDispatchPublisherImpl(facade, env);

		LinkedHashMap<String, Object> exportFilter = new LinkedHashMap<>();
		exportFilter.put("company_id", 601L);
		exportFilter.put("order_type", "normal");

		publisher.enqueueInvoiceExport(601L, 602L, exportFilter);

		verify(facade)
				.dispatchJob(
						eq(EspierDispatchJobNames.EXPORT_FILE_JOB_ORDER_LIST),
						argThat(
								p -> {
									if (!(p instanceof Map)) {
										return false;
									}
									Map<?, ?> m = (Map<?, ?>) p;
									if (!"invoice".equals(String.valueOf(m.get("type")))) {
										return false;
									}
									if (601L != asLong(m.get("company_id")) || 602L != asLong(m.get("operator_id"))) {
										return false;
									}
									Object rawFilter = m.get("filter");
									if (!(rawFilter instanceof Map)) {
										return false;
									}
									Map<?, ?> fm = (Map<?, ?>) rawFilter;
									return "normal".equals(String.valueOf(fm.get("order_type")));
								}),
						argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.ASYNC
												&& opts.driverOverride() == DispatchDriverType.REDIS
												&& "slow".equals(opts.queue())
												&& opts.delay() == null
												&& RetryPolicy.platformDefault().equals(opts.retryPolicy())));
	}

	@Test
	void dispatchJob_publishRightsPayloadUsesTypeRight() {
		DispatchFacade facade = mock(DispatchFacade.class);
		Environment env = mock(Environment.class);
		when(env.acceptsProfiles(Profiles.of("local"))).thenReturn(false);
		OrderListExportFileJobDispatchPublisherImpl publisher =
				new OrderListExportFileJobDispatchPublisherImpl(facade, env);

		LinkedHashMap<String, Object> exportFilter = new LinkedHashMap<>();
		exportFilter.put("company_id", 601L);
		exportFilter.put("user_id", 99L);

		publisher.enqueueRightsExport(601L, 602L, exportFilter);

		verify(facade)
				.dispatchJob(
						eq(EspierDispatchJobNames.EXPORT_FILE_JOB_ORDER_LIST),
						argThat(
								p -> {
									if (!(p instanceof Map)) {
										return false;
									}
									Map<?, ?> m = (Map<?, ?>) p;
									if (!"right".equals(String.valueOf(m.get("type")))) {
										return false;
									}
									if (601L != asLong(m.get("company_id")) || 602L != asLong(m.get("operator_id"))) {
										return false;
									}
									Object rawFilter = m.get("filter");
									if (!(rawFilter instanceof Map)) {
										return false;
									}
									Map<?, ?> fm = (Map<?, ?>) rawFilter;
									return 99L == asLong(fm.get("user_id"));
								}),
						argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.ASYNC
												&& opts.driverOverride() == DispatchDriverType.REDIS
												&& "slow".equals(opts.queue())
												&& opts.delay() == null
												&& RetryPolicy.platformDefault().equals(opts.retryPolicy())));
	}

	@Test
	void dispatchJob_publishRights_localProfile_usesSyncOptions() {
		DispatchFacade facade = mock(DispatchFacade.class);
		Environment env = mock(Environment.class);
		when(env.acceptsProfiles(Profiles.of("local"))).thenReturn(true);
		OrderListExportFileJobDispatchPublisherImpl publisher =
				new OrderListExportFileJobDispatchPublisherImpl(facade, env);

		LinkedHashMap<String, Object> exportFilter = new LinkedHashMap<>();
		exportFilter.put("company_id", 640L);

		publisher.enqueueRightsExport(640L, 641L, exportFilter);

		verify(facade)
				.dispatchJob(
						eq(EspierDispatchJobNames.EXPORT_FILE_JOB_ORDER_LIST),
						argThat(
								p -> {
									if (!(p instanceof Map)) {
										return false;
									}
									Map<?, ?> m = (Map<?, ?>) p;
									return "right".equals(String.valueOf(m.get("type")))
											&& 640L == asLong(m.get("company_id"))
											&& 641L == asLong(m.get("operator_id"));
								}),
						argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.SYNC
												&& opts.driverOverride() == DispatchDriverType.SYNC
												&& opts.queue() == null
												&& opts.delay() == null
												&& RetryPolicy.platformDefault().equals(opts.retryPolicy())));
	}

	private static long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}
}
