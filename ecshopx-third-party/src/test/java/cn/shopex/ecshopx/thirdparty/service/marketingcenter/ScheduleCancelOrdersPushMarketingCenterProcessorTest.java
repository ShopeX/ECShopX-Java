package cn.shopex.ecshopx.thirdparty.service.marketingcenter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

@ExtendWith(MockitoExtension.class)
class ScheduleCancelOrdersPushMarketingCenterProcessorTest {

	@Mock
	private MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient;

	private EmbeddedDatabase database;
	private JdbcTemplate jdbcTemplate;
	private ScheduleCancelOrdersPushMarketingCenterProcessor processor;

	@BeforeEach
	void setUp() {
		database =
				new EmbeddedDatabaseBuilder()
						.generateUniqueName(true)
						.setType(EmbeddedDatabaseType.H2)
						.ignoreFailedDrops(true)
						.build();
		jdbcTemplate = new JdbcTemplate(database);
		jdbcTemplate.execute(
				"CREATE TABLE orders_normal_orders ("
						+ "order_id BIGINT NOT NULL,"
						+ "company_id BIGINT NOT NULL,"
						+ "salesman_id BIGINT DEFAULT 0,"
						+ "bind_salesman_id BIGINT DEFAULT 0,"
						+ "chat_id VARCHAR(255),"
						+ "PRIMARY KEY (company_id, order_id))");
		processor = new ScheduleCancelOrdersPushMarketingCenterProcessor(
				marketingCenterOpenApiSignedFormClient, jdbcTemplate);
	}

	@AfterEach
	void tearDown() {
		database.shutdown();
	}

	@Test
	void handle_whenSourceNotCron_skipsOutbound() {
		long companyId = 10L;
		long orderId = 200L;
		jdbcTemplate.update(
				"INSERT INTO orders_normal_orders(order_id, company_id, salesman_id, bind_salesman_id, chat_id) VALUES (?,?,?,?,?)",
				orderId,
				companyId,
				99L,
				0L,
				"");

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("order_id", orderId);
		payload.put("source", "admin_normal_order_full_cancel");

		processor.handle(payload);

		verify(marketingCenterOpenApiSignedFormClient, never()).basicsOrderProccess(anyLong(), any());
	}

	@Test
	void handle_whenSalesmanMissing_skipsOutbound() {
		long companyId = 10L;
		long orderId = 200L;
		jdbcTemplate.update(
				"INSERT INTO orders_normal_orders(order_id, company_id, salesman_id, bind_salesman_id, chat_id) VALUES (?,?,?,?,?)",
				orderId,
				companyId,
				0L,
				0L,
				"");

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("order_id", orderId);
		payload.put("source", ScheduleCancelOrdersPushMarketingCenterProcessor.CRON_SCHEDULE_CANCEL_SOURCE);

		processor.handle(payload);

		verify(marketingCenterOpenApiSignedFormClient, never()).basicsOrderProccess(anyLong(), any());
	}

	@Test
	void handle_whenEligible_callsBasicsOrderProccess() {
		long companyId = 77L;
		long orderId = 501L;
		jdbcTemplate.update(
				"INSERT INTO orders_normal_orders(order_id, company_id, salesman_id, bind_salesman_id, chat_id) VALUES (?,?,?,?,?)",
				orderId,
				companyId,
				405L,
				0L,
				"");

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("order_id", orderId);
		payload.put("source", ScheduleCancelOrdersPushMarketingCenterProcessor.CRON_SCHEDULE_CANCEL_SOURCE);

		when(marketingCenterOpenApiSignedFormClient.basicsOrderProccess(eq(companyId), any())).thenReturn(Map.of());

		processor.handle(payload);

		verify(marketingCenterOpenApiSignedFormClient).basicsOrderProccess(eq(companyId), any());
	}
}
