package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchJobNames;
import cn.shopex.ecshopx.orders.dispatch.GenerateStatementsJobHandler;
import cn.shopex.ecshopx.orders.statement.StatementPeriodValue;
import cn.shopex.ecshopx.orders.statement.generate.StatementGenerateJobInliner;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GenerateStatementsJobDispatchFlowTest {

	@Test
	@DisplayName("GenerateStatements: dispatch async to slow queue and consumer invokes inliner")
	void dispatchJob_async_enqueuesOnSlowQueue_andConsumerInvokesInliner() {
		StatementGenerateJobInliner inliner = Mockito.mock(StatementGenerateJobInliner.class);
		Clock clock = Clock.fixed(Instant.parse("2025-06-15T00:00:00Z"), ZoneId.of("UTC"));
		GenerateStatementsJobHandler handler = new GenerateStatementsJobHandler(inliner, clock);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerJob(OrdersDispatchJobNames.GENERATE_STATEMENTS_JOB, handler);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core =
				DispatchCore.asyncReady(
						registry,
						new SyncDispatchDriver(registry),
						java.util.Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);
		payload.put("distributor_id", 9L);
		payload.put("supplier_id", 0L);
		payload.put("period", List.of(1, "day"));
		payload.put("last_end_time", 1000L);
		payload.put("merchant_type", "distributor");
		facade.dispatchJob(
				OrdersDispatchJobNames.GENERATE_STATEMENTS_JOB,
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
		assertNull(msg.delay());
		assertEquals(OrdersDispatchJobNames.GENERATE_STATEMENTS_JOB, msg.messageName());
		assertNull(msg.listenerName());
		assertEquals(1L, msg.payload().get("company_id"));
		assertEquals(9L, msg.payload().get("distributor_id"));
		assertEquals(0L, msg.payload().get("supplier_id"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						Mockito.mock(FailedJobRecorder.class),
						Mockito.mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);

		verify(inliner)
				.runJob(
						eq(clock),
						eq(1L),
						eq(9L),
						any(StatementPeriodValue.class),
						eq(1000L),
						eq("distributor"));
	}
}
