package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.DatacubeDispatchJobNames;
import cn.shopex.ecshopx.config.GoodsDataJobDispatchPublisherImpl;
import cn.shopex.ecshopx.datacube.dispatch.GoodsDataJobHandler;
import cn.shopex.ecshopx.datacube.service.goodsdata.AdminGoodsDataFilter;
import cn.shopex.ecshopx.datacube.service.goodsdata.GoodsDataCsvExportService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class GoodsDataJobDispatchFlowTest {

	@Test
	@DisplayName("dispatch enqueues slow queue without delay and consumer invokes GoodsDataCsvExportService#runExport")
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesGoodsDataCsvExportService() {
		GoodsDataCsvExportService exportService = Mockito.mock(GoodsDataCsvExportService.class);
		GoodsDataJobHandler handler = new GoodsDataJobHandler(exportService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(DatacubeDispatchJobNames.GOODS_DATA_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 100L);
		payload.put("date_start", "2025-03-01");
		payload.put("date_end", "2025-03-07");
		payload.put("order_class_restrict_to_value", false);
		payload.put("order_class_value", "");
		payload.put("act_ids", List.of(5L, 6L));
		payload.put("merchant_id", 200L);
		payload.put("operator_id", 300L);

		facade.dispatchJob(
				DatacubeDispatchJobNames.GOODS_DATA_JOB,
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
		assertEquals(DispatchMode.ASYNC, msg.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, msg.driverType());
		assertEquals("slow", msg.queue());
		assertNull(msg.delay());
		assertEquals(DatacubeDispatchJobNames.GOODS_DATA_JOB, msg.messageName());
		assertNull(msg.listenerName());

		Map<String, Object> got = msg.payload();
		assertEquals(100L, asLong(got.get("company_id")));
		assertEquals("2025-03-01", got.get("date_start"));
		assertEquals("2025-03-07", got.get("date_end"));
		assertEquals(false, got.get("order_class_restrict_to_value"));
		assertEquals("", got.get("order_class_value"));
		@SuppressWarnings("unchecked")
		List<Number> actIds = (List<Number>) got.get("act_ids");
		assertEquals(2, actIds.size());
		assertEquals(5L, actIds.get(0).longValue());
		assertEquals(200L, asLong(got.get("merchant_id")));
		assertEquals(300L, asLong(got.get("operator_id")));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		ArgumentCaptor<AdminGoodsDataFilter> captor = ArgumentCaptor.forClass(AdminGoodsDataFilter.class);
		verify(exportService).runExport(captor.capture());
		AdminGoodsDataFilter f = captor.getValue();
		assertEquals(100L, f.companyId());
		assertEquals("2025-03-01", f.dateStart());
		assertEquals("2025-03-07", f.dateEnd());
		assertFalse(f.orderClassRestrictToValue());
		assertEquals("", f.orderClassValue());
		assertEquals(List.of(5L, 6L), f.actIdsForIn());
		assertEquals(200L, f.merchantIdOrNull());
		assertEquals(300L, f.operatorId());
	}

	@Test
	void dispatchJob_publishPayloadMatchesGoodsDataJobEnvelope() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		GoodsDataJobDispatchPublisherImpl publisher = new GoodsDataJobDispatchPublisherImpl(dispatchFacade);

		AdminGoodsDataFilter filter =
				new AdminGoodsDataFilter(
						42L,
						"2025-01-01",
						"2025-01-31",
						true,
						"retail",
						List.of(1L, 2L),
						99L,
						7L);

		publisher.enqueue(filter);

		verify(dispatchFacade)
				.dispatchJob(
						eq(DatacubeDispatchJobNames.GOODS_DATA_JOB),
						argThat(
								m -> {
									if (42L != asLong(m.get("company_id"))) {
										return false;
									}
									if (!"2025-01-01".equals(m.get("date_start"))
											|| !"2025-01-31".equals(m.get("date_end"))) {
										return false;
									}
									if (!Boolean.TRUE.equals(m.get("order_class_restrict_to_value"))) {
										return false;
									}
									if (!"retail".equals(m.get("order_class_value"))) {
										return false;
									}
									Object rawAct = m.get("act_ids");
									if (!(rawAct instanceof List<?> list) || list.size() != 2) {
										return false;
									}
									if (1L != asLong(list.get(0)) || 2L != asLong(list.get(1))) {
										return false;
									}
									if (99L != asLong(m.get("merchant_id")) || 7L != asLong(m.get("operator_id"))) {
										return false;
									}
									return true;
								}),
						argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.ASYNC
												&& opts.driverOverride() == DispatchDriverType.REDIS
												&& "slow".equals(opts.queue())
												&& opts.delay() == null));
	}

	@Test
	@DisplayName("super-admin export payload uses query company id, operator 0, no merchant_id")
	void superAdminGoodsDataExport_dispatchPayload_usesQueryCompanyId_operatorIdZero_andNoMerchantKey() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		GoodsDataJobDispatchPublisherImpl publisher = new GoodsDataJobDispatchPublisherImpl(dispatchFacade);

		long companyIdFromQuery = 88L;
		AdminGoodsDataFilter filter =
				new AdminGoodsDataFilter(
						companyIdFromQuery,
						"2024-01-01",
						"2024-01-02",
						false,
						"",
						List.of(),
						null,
						0L);

		publisher.enqueue(filter);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(dispatchFacade)
				.dispatchJob(
						eq(DatacubeDispatchJobNames.GOODS_DATA_JOB),
						payloadCaptor.capture(),
						argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.ASYNC
												&& opts.driverOverride() == DispatchDriverType.REDIS
												&& "slow".equals(opts.queue())
												&& opts.delay() == null));

		Map<String, Object> m = payloadCaptor.getValue();
		assertNotNull(m);
		assertEquals(companyIdFromQuery, asLong(m.get("company_id")));
		assertEquals("2024-01-01", m.get("date_start"));
		assertEquals("2024-01-02", m.get("date_end"));
		assertEquals(false, m.get("order_class_restrict_to_value"));
		assertEquals("", m.get("order_class_value"));
		Object rawAct = m.get("act_ids");
		assertNotNull(rawAct);
		assertEquals(List.of(), rawAct);
		assertFalse(m.containsKey("merchant_id"));
		assertEquals(0L, asLong(m.get("operator_id")));
	}

	private static long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}
}
