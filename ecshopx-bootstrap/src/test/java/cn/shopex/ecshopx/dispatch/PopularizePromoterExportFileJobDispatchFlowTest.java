package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.config.EspierExportFileJobDispatchHandler;
import cn.shopex.ecshopx.popularize.dispatch.PopularizeOrderExportFileJobTypeHandler;
import cn.shopex.ecshopx.popularize.dispatch.PopularizePromoterExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.popularize.dispatch.PopularizePromoterExportFileJobTypeHandler;
import cn.shopex.ecshopx.popularize.dispatch.PopularizePromoterExportFileJobTypes;
import cn.shopex.ecshopx.popularize.dispatch.PopularizeStaticExportFileJobTypeHandler;
import cn.shopex.ecshopx.popularize.service.export.PopularizeOrderExportFileJobHandler;
import cn.shopex.ecshopx.popularize.service.export.PopularizeOrderExportJobContext;
import cn.shopex.ecshopx.popularize.service.export.PopularizeStaticExportFileJobHandler;
import cn.shopex.ecshopx.popularize.service.export.PopularizeStaticExportJobContext;
import cn.shopex.ecshopx.popularize.service.export.PromoterExportFileJobHandler;
import cn.shopex.ecshopx.popularize.service.export.PromoterExportJobContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PopularizePromoterExportFileJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesPromoterExportHandler() {
		PromoterExportFileJobHandler jobHandler = mock(PromoterExportFileJobHandler.class);
		PopularizePromoterExportFileJobTypeHandler typeHandler = new PopularizePromoterExportFileJobTypeHandler(jobHandler);
		EspierExportFileJobDispatchHandler router = new EspierExportFileJobDispatchHandler(List.of(typeHandler));

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB_POPULARIZE_PROMOTER_LIST, router);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 701L;
		long operatorId = 702L;
		PromoterExportJobContext ctx =
				new PromoterExportJobContext(companyId, operatorId, "13800138000", null, true);
		PopularizePromoterExportFileJobDispatchPublisher publisher =
				new PopularizePromoterExportFileJobDispatchPublisher(facade);
		publisher.enqueuePopularizePromoterExport(ctx);

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals(PopularizePromoterExportFileJobDispatchPublisher.POPULARIZE_PROMOTER_EXPORT_JOB_QUEUE, msg.queue());
		assertNull(msg.delay());
		assertEquals(EspierDispatchJobNames.EXPORT_FILE_JOB_POPULARIZE_PROMOTER_LIST, msg.messageName());
		assertNull(msg.listenerName());
		Map<String, Object> p = msg.payload();
		assertEquals(PopularizePromoterExportFileJobTypes.TYPE_POPULARIZE_PROMOTER_EXPORT, String.valueOf(p.get("type")));
		assertEquals(companyId, asLong(p.get("company_id")));
		assertEquals(operatorId, asLong(p.get("operator_id")));
		Object rawParams = p.get("params");
		assertInstanceOf(Map.class, rawParams);
		@SuppressWarnings("unchecked")
		Map<String, Object> params = (Map<String, Object>) rawParams;
		assertEquals(companyId, asLong(params.get("company_id")));
		assertEquals("13800138000", String.valueOf(params.get("mobile")));
		assertNull(params.get("username"));
		assertEquals(1, asLong(params.get("datapass_block")));

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
								c ->
										c.companyId() == companyId
												&& c.operatorId() == operatorId
												&& "13800138000".equals(c.mobile())
												&& c.username() == null
												&& c.datapassBlock()));
	}

	@Test
	void dispatchJob_publishPayloadMatchesPopularizePromoterEnvelope() {
		DispatchFacade facade = mock(DispatchFacade.class);
		PopularizePromoterExportFileJobDispatchPublisher publisher =
				new PopularizePromoterExportFileJobDispatchPublisher(facade);

		PromoterExportJobContext ctx =
				new PromoterExportJobContext(501L, 0L, null, "alice", false);
		publisher.enqueuePopularizePromoterExport(ctx);

		verify(facade)
				.dispatchJob(
						eq(EspierDispatchJobNames.EXPORT_FILE_JOB_POPULARIZE_PROMOTER_LIST),
						argThat(
								pl -> {
									if (!(pl instanceof Map)) {
										return false;
									}
									Map<?, ?> m = (Map<?, ?>) pl;
									if (!PopularizePromoterExportFileJobTypes.TYPE_POPULARIZE_PROMOTER_EXPORT.equals(
											String.valueOf(m.get("type")))) {
										return false;
									}
									if (501L != asLong(m.get("company_id")) || 0L != asLong(m.get("operator_id"))) {
										return false;
									}
									Object rawParams = m.get("params");
									if (!(rawParams instanceof Map)) {
										return false;
									}
									Map<?, ?> pm = (Map<?, ?>) rawParams;
									if (501L != asLong(pm.get("company_id"))) {
										return false;
									}
									if (pm.containsKey("mobile")) {
										return false;
									}
									if (!"alice".equals(String.valueOf(pm.get("username")))) {
										return false;
									}
									if (pm.containsKey("datapass_block")) {
										return false;
									}
									return true;
								}),
						argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.ASYNC
												&& opts.driverOverride() == DispatchDriverType.REDIS
												&& PopularizePromoterExportFileJobDispatchPublisher
														.POPULARIZE_PROMOTER_EXPORT_JOB_QUEUE
														.equals(opts.queue())
												&& opts.delay() == null
												&& RetryPolicy.platformDefault().equals(opts.retryPolicy())));
	}

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesPopularizeOrderExportHandler() {
		PopularizeOrderExportFileJobHandler orderHandler = mock(PopularizeOrderExportFileJobHandler.class);
		PopularizeOrderExportFileJobTypeHandler typeHandler = new PopularizeOrderExportFileJobTypeHandler(orderHandler);
		EspierExportFileJobDispatchHandler router = new EspierExportFileJobDispatchHandler(List.of(typeHandler));

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB_POPULARIZE_ORDER, router);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 801L;
		long operatorId = 802L;
		long merchantId = 803L;
		PopularizeOrderExportJobContext ctx =
				new PopularizeOrderExportJobContext(
						companyId,
						operatorId,
						merchantId,
						"merchant",
						"13900139000",
						null,
						"9",
						List.of(11L, 12L),
						"2024-02-01",
						"2024-02-28",
						true);
		PopularizePromoterExportFileJobDispatchPublisher publisher =
				new PopularizePromoterExportFileJobDispatchPublisher(facade);
		publisher.enqueuePopularizeOrderExport(ctx);

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals(PopularizePromoterExportFileJobDispatchPublisher.POPULARIZE_PROMOTER_EXPORT_JOB_QUEUE, msg.queue());
		assertNull(msg.delay());
		assertEquals(EspierDispatchJobNames.EXPORT_FILE_JOB_POPULARIZE_ORDER, msg.messageName());
		assertNull(msg.listenerName());
		Map<String, Object> p = msg.payload();
		assertEquals(PopularizePromoterExportFileJobTypes.TYPE_POPULARIZE_ORDER_EXPORT, String.valueOf(p.get("type")));
		assertEquals(companyId, asLong(p.get("company_id")));
		assertEquals(operatorId, asLong(p.get("operator_id")));
		assertEquals(merchantId, asLong(p.get("merchant_id")));
		Object rawParams = p.get("params");
		assertInstanceOf(Map.class, rawParams);
		@SuppressWarnings("unchecked")
		Map<String, Object> params = (Map<String, Object>) rawParams;
		assertEquals(companyId, asLong(params.get("company_id")));
		assertEquals("13900139000", String.valueOf(params.get("mobile")));
		assertEquals("9", String.valueOf(params.get("distributor_id")));
		assertEquals(1, asLong(params.get("datapass_block")));
		assertEquals("2024-02-01", String.valueOf(params.get("date_start")));
		assertEquals("2024-02-28", String.valueOf(params.get("date_end")));
		Object dIds = params.get("dIds");
		assertInstanceOf(List.class, dIds);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(orderHandler, times(1))
				.run(
						argThat(
								c ->
										c.companyId() == companyId
												&& c.operatorId() == operatorId
												&& c.merchantId() == merchantId
												&& "".equals(c.operatorType())
												&& "13900139000".equals(c.mobile())
												&& c.username() == null
												&& "9".equals(c.distributorIdRaw())
												&& c.dIds() != null
												&& c.dIds().size() == 2
												&& c.dIds().get(0) == 11L
												&& c.dIds().get(1) == 12L
												&& "2024-02-01".equals(c.dateStart())
												&& "2024-02-28".equals(c.dateEnd())
												&& c.datapassBlock()));
	}

	@Test
	void dispatchJob_publishPayloadMatchesPopularizeOrderEnvelope() {
		DispatchFacade facade = mock(DispatchFacade.class);
		PopularizePromoterExportFileJobDispatchPublisher publisher =
				new PopularizePromoterExportFileJobDispatchPublisher(facade);

		PopularizeOrderExportJobContext ctx =
				new PopularizeOrderExportJobContext(
						601L, 602L, 603L, "merchant", null, "bob", null, null, "2024-06-01", "2024-06-30", false);
		publisher.enqueuePopularizeOrderExport(ctx);

		verify(facade)
				.dispatchJob(
						eq(EspierDispatchJobNames.EXPORT_FILE_JOB_POPULARIZE_ORDER),
						argThat(
								pl -> {
									if (!(pl instanceof Map)) {
										return false;
									}
									Map<?, ?> m = (Map<?, ?>) pl;
									if (!PopularizePromoterExportFileJobTypes.TYPE_POPULARIZE_ORDER_EXPORT.equals(
											String.valueOf(m.get("type")))) {
										return false;
									}
									if (601L != asLong(m.get("company_id"))
											|| 602L != asLong(m.get("operator_id"))
											|| 603L != asLong(m.get("merchant_id"))) {
										return false;
									}
									Object rawParams = m.get("params");
									if (!(rawParams instanceof Map)) {
										return false;
									}
									Map<?, ?> pm = (Map<?, ?>) rawParams;
									if (601L != asLong(pm.get("company_id"))) {
										return false;
									}
									if (pm.containsKey("mobile")) {
										return false;
									}
									if (!"bob".equals(String.valueOf(pm.get("username")))) {
										return false;
									}
									if (pm.containsKey("distributor_id") || pm.containsKey("dIds") || pm.containsKey("datapass_block")) {
										return false;
									}
									if (!"2024-06-01".equals(String.valueOf(pm.get("date_start")))
											|| !"2024-06-30".equals(String.valueOf(pm.get("date_end")))) {
										return false;
									}
									return true;
								}),
						argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.ASYNC
												&& opts.driverOverride() == DispatchDriverType.REDIS
												&& PopularizePromoterExportFileJobDispatchPublisher
														.POPULARIZE_PROMOTER_EXPORT_JOB_QUEUE
														.equals(opts.queue())
												&& opts.delay() == null
												&& RetryPolicy.platformDefault().equals(opts.retryPolicy())));
	}

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesPopularizeStaticExportHandler() {
		PopularizeStaticExportFileJobHandler staticHandler = mock(PopularizeStaticExportFileJobHandler.class);
		PopularizeStaticExportFileJobTypeHandler typeHandler = new PopularizeStaticExportFileJobTypeHandler(staticHandler);
		EspierExportFileJobDispatchHandler router = new EspierExportFileJobDispatchHandler(List.of(typeHandler));

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB_POPULARIZE_STATIC, router);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 901L;
		long operatorId = 902L;
		long merchantId = 903L;
		PopularizeStaticExportJobContext ctx =
				new PopularizeStaticExportJobContext(
						companyId,
						operatorId,
						merchantId,
						"merchant",
						"13700137000",
						null,
						"7",
						List.of(21L, 22L),
						"2025-03-01",
						"2025-03-31",
						true);
		PopularizePromoterExportFileJobDispatchPublisher publisher =
				new PopularizePromoterExportFileJobDispatchPublisher(facade);
		publisher.enqueuePopularizeStaticExport(ctx);

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(DispatchMessageType.JOB, msg.messageType());
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals(PopularizePromoterExportFileJobDispatchPublisher.POPULARIZE_PROMOTER_EXPORT_JOB_QUEUE, msg.queue());
		assertNull(msg.delay());
		assertEquals(EspierDispatchJobNames.EXPORT_FILE_JOB_POPULARIZE_STATIC, msg.messageName());
		assertNull(msg.listenerName());
		Map<String, Object> p = msg.payload();
		assertEquals(PopularizePromoterExportFileJobTypes.TYPE_POPULARIZE_STATIC_EXPORT, String.valueOf(p.get("type")));
		assertEquals(companyId, asLong(p.get("company_id")));
		assertEquals(operatorId, asLong(p.get("operator_id")));
		assertEquals(merchantId, asLong(p.get("merchant_id")));
		Object rawParams = p.get("params");
		assertInstanceOf(Map.class, rawParams);
		@SuppressWarnings("unchecked")
		Map<String, Object> params = (Map<String, Object>) rawParams;
		assertEquals(companyId, asLong(params.get("company_id")));
		assertEquals("13700137000", String.valueOf(params.get("mobile")));
		assertEquals("7", String.valueOf(params.get("distributor_id")));
		assertEquals(1, asLong(params.get("datapass_block")));
		assertEquals("2025-03-01", String.valueOf(params.get("date_start")));
		assertEquals("2025-03-31", String.valueOf(params.get("date_end")));
		Object dIds = params.get("dIds");
		assertInstanceOf(List.class, dIds);

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(staticHandler, times(1))
				.run(
						argThat(
								c ->
										c.companyId() == companyId
												&& c.operatorId() == operatorId
												&& c.merchantId() == merchantId
												&& "".equals(c.operatorType())
												&& "13700137000".equals(c.mobile())
												&& c.username() == null
												&& "7".equals(c.distributorIdRaw())
												&& c.dIds() != null
												&& c.dIds().size() == 2
												&& c.dIds().get(0) == 21L
												&& c.dIds().get(1) == 22L
												&& "2025-03-01".equals(c.dateStart())
												&& "2025-03-31".equals(c.dateEnd())
												&& c.datapassBlock()));
	}

	@Test
	void dispatchJob_publishPayloadMatchesPopularizeStaticEnvelope() {
		DispatchFacade facade = mock(DispatchFacade.class);
		PopularizePromoterExportFileJobDispatchPublisher publisher =
				new PopularizePromoterExportFileJobDispatchPublisher(facade);

		PopularizeStaticExportJobContext ctx =
				new PopularizeStaticExportJobContext(
						701L, 702L, 703L, "merchant", null, "carol", null, null, "2025-04-01", "2025-04-30", false);
		publisher.enqueuePopularizeStaticExport(ctx);

		verify(facade)
				.dispatchJob(
						eq(EspierDispatchJobNames.EXPORT_FILE_JOB_POPULARIZE_STATIC),
						argThat(
								pl -> {
									if (!(pl instanceof Map)) {
										return false;
									}
									Map<?, ?> m = (Map<?, ?>) pl;
									if (!PopularizePromoterExportFileJobTypes.TYPE_POPULARIZE_STATIC_EXPORT.equals(
											String.valueOf(m.get("type")))) {
										return false;
									}
									if (701L != asLong(m.get("company_id"))
											|| 702L != asLong(m.get("operator_id"))
											|| 703L != asLong(m.get("merchant_id"))) {
										return false;
									}
									Object rawParams = m.get("params");
									if (!(rawParams instanceof Map)) {
										return false;
									}
									Map<?, ?> pm = (Map<?, ?>) rawParams;
									if (701L != asLong(pm.get("company_id"))) {
										return false;
									}
									if (pm.containsKey("mobile")) {
										return false;
									}
									if (!"carol".equals(String.valueOf(pm.get("username")))) {
										return false;
									}
									if (pm.containsKey("distributor_id") || pm.containsKey("dIds") || pm.containsKey("datapass_block")) {
										return false;
									}
									if (!"2025-04-01".equals(String.valueOf(pm.get("date_start")))
											|| !"2025-04-30".equals(String.valueOf(pm.get("date_end")))) {
										return false;
									}
									return true;
								}),
						argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.ASYNC
												&& opts.driverOverride() == DispatchDriverType.REDIS
												&& PopularizePromoterExportFileJobDispatchPublisher
														.POPULARIZE_PROMOTER_EXPORT_JOB_QUEUE
														.equals(opts.queue())
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
