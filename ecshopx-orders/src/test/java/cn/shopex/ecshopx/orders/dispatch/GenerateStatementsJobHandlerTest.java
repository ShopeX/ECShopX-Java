package cn.shopex.ecshopx.orders.dispatch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.orders.statement.StatementPeriodValue;
import cn.shopex.ecshopx.orders.statement.generate.StatementGenerateJobInliner;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GenerateStatementsJobHandlerTest {

	private static final Clock CLOCK =
			Clock.fixed(Instant.parse("2025-06-15T00:00:00Z"), ZoneId.of("UTC"));

	@Test
	void whenPayloadInvalidPeriod_skipsInliner() {
		StatementGenerateJobInliner inliner = Mockito.mock(StatementGenerateJobInliner.class);
		GenerateStatementsJobHandler handler = new GenerateStatementsJobHandler(inliner, CLOCK);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);
		payload.put("distributor_id", 1L);
		payload.put("supplier_id", 0L);
		payload.put("period", List.of(1, "year"));
		payload.put("last_end_time", 10L);
		payload.put("merchant_type", "distributor");
		handler.handle(payload);

		verify(inliner, never())
				.runJob(
						any(Clock.class),
						anyLong(),
						anyLong(),
						any(StatementPeriodValue.class),
						anyLong(),
						any());
	}

	@Test
	void whenValidDistributorPayload_invokesRunJobWithExpectedArgs() {
		StatementGenerateJobInliner inliner = Mockito.mock(StatementGenerateJobInliner.class);
		GenerateStatementsJobHandler handler = new GenerateStatementsJobHandler(inliner, CLOCK);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 5L);
		payload.put("distributor_id", 11L);
		payload.put("supplier_id", 0L);
		payload.put("period", List.of(1, "day"));
		payload.put("last_end_time", 200L);
		payload.put("merchant_type", "distributor");
		handler.handle(payload);

		verify(inliner)
				.runJob(
						eq(CLOCK),
						eq(5L),
						eq(11L),
						eq(new StatementPeriodValue(1, "day")),
						eq(200L),
						eq("distributor"));
	}

	@Test
	void whenValidSupplierPayload_invokesRunJobWithExpectedArgs() {
		StatementGenerateJobInliner inliner = Mockito.mock(StatementGenerateJobInliner.class);
		GenerateStatementsJobHandler handler = new GenerateStatementsJobHandler(inliner, CLOCK);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 8L);
		payload.put("distributor_id", 0L);
		payload.put("supplier_id", 22L);
		payload.put("period", List.of(3, "month"));
		payload.put("last_end_time", 300L);
		payload.put("merchant_type", "supplier");
		handler.handle(payload);

		verify(inliner)
				.runJob(
						eq(CLOCK),
						eq(8L),
						eq(22L),
						eq(new StatementPeriodValue(3, "month")),
						eq(300L),
						eq("supplier"));
	}
}
