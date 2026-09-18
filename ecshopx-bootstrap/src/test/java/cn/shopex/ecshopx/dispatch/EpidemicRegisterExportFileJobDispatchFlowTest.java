package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.config.EpidemicRegisterExportFileJobDispatchPublisherImpl;
import cn.shopex.ecshopx.config.EspierExportFileJobDispatchHandler;
import cn.shopex.ecshopx.goods.dispatch.EpidemicRegisterExportFileJobTypeHandler;
import cn.shopex.ecshopx.goods.dispatch.EpidemicRegisterExportFileJobTypes;
import cn.shopex.ecshopx.goods.service.export.EpidemicRegisterExportFileJobHandler;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

class EpidemicRegisterExportFileJobDispatchFlowTest {

	@Test
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesEpidemicRegisterExportFileJobHandler() {
		EpidemicRegisterExportFileJobHandler jobHandler = mock(EpidemicRegisterExportFileJobHandler.class);
		EpidemicRegisterExportFileJobTypeHandler typeHandler = new EpidemicRegisterExportFileJobTypeHandler(jobHandler);
		EspierExportFileJobDispatchHandler router = new EspierExportFileJobDispatchHandler(List.of(typeHandler));

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(EspierDispatchJobNames.EXPORT_FILE_JOB_EPIDEMIC_REGISTER, router);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		long companyId = 101L;
		long operatorId = 202L;
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("distributor_id", 303L);
		filter.put("order_time|gte", 1000);
		filter.put("order_time|lte", 2000);
		filter.put("datapass_block", "1");

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", EpidemicRegisterExportFileJobTypes.TYPE_EPIDEMIC_REGISTER);
		payload.put("company_id", companyId);
		payload.put("operator_id", operatorId);
		payload.put("filter", new LinkedHashMap<>(filter));

		facade.dispatchJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_EPIDEMIC_REGISTER,
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
		assertEquals(EspierDispatchJobNames.EXPORT_FILE_JOB_EPIDEMIC_REGISTER, msg.messageName());
		assertNull(msg.listenerName());
		Map<String, Object> got = msg.payload();
		assertEquals(EpidemicRegisterExportFileJobTypes.TYPE_EPIDEMIC_REGISTER, got.get("type"));
		assertEquals(companyId, asLong(got.get("company_id")));
		assertEquals(operatorId, asLong(got.get("operator_id")));
		@SuppressWarnings("unchecked")
		Map<String, Object> nested = (Map<String, Object>) got.get("filter");
		assertEquals(303L, asLong(nested.get("distributor_id")));
		assertEquals(1000, asInt(nested.get("order_time|gte")));
		assertEquals(2000, asInt(nested.get("order_time|lte")));
		assertEquals("1", nested.get("datapass_block"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(jobHandler)
				.run(
						argThat(
								f ->
										f.getCompanyId() == companyId
												&& f.getDistributorIdEq() != null
												&& f.getDistributorIdEq() == 303L
												&& Integer.valueOf(1000).equals(f.getOrderTimeGte())
												&& Integer.valueOf(2000).equals(f.getOrderTimeLte())),
						eq(operatorId),
						eq(true));
	}

	@Test
	void dispatchJob_publishPayloadMatchesEpidemicRegisterExportFileJobEnvelope() {
		DispatchFacade facade = mock(DispatchFacade.class);
		Environment environment = mock(Environment.class);
		when(environment.acceptsProfiles(Profiles.of("local"))).thenReturn(false);
		EpidemicRegisterExportFileJobDispatchPublisherImpl publisher =
				new EpidemicRegisterExportFileJobDispatchPublisherImpl(facade, environment);

		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", 501L);
		filter.put("distributor_ids", List.of(1L, 2L));
		filter.put("datapass_block", "");

		publisher.publish(501L, 503L, filter);

		verify(facade)
				.dispatchJob(
						eq(EspierDispatchJobNames.EXPORT_FILE_JOB_EPIDEMIC_REGISTER),
						argThat(
								p -> {
									if (!(p instanceof Map)) {
										return false;
									}
									Map<?, ?> m = (Map<?, ?>) p;
									if (!EpidemicRegisterExportFileJobTypes.TYPE_EPIDEMIC_REGISTER.equals(
											String.valueOf(m.get("type")))) {
										return false;
									}
									if (501L != asLong(m.get("company_id")) || 503L != asLong(m.get("operator_id"))) {
										return false;
									}
									Object rawFilter = m.get("filter");
									if (!(rawFilter instanceof Map)) {
										return false;
									}
									Map<?, ?> fm = (Map<?, ?>) rawFilter;
									Object ids = fm.get("distributor_ids");
									if (!(ids instanceof List<?> list) || list.size() != 2) {
										return false;
									}
									return "".equals(fm.get("datapass_block"));
								}),
						argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.ASYNC
												&& opts.driverOverride() == DispatchDriverType.REDIS
												&& "slow".equals(opts.queue())
												&& opts.delay() == null));
	}

	private static long asLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}

	private static int asInt(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(String.valueOf(v).trim());
	}
}
