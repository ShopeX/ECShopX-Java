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
class TradeRefundPushMarketingCenterProcessorTest {

	@Mock private MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient;

	private EmbeddedDatabase database;
	private JdbcTemplate jdbcTemplate;
	private TradeRefundPushMarketingCenterProcessor processor;

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
						+ "order_id BIGINT PRIMARY KEY,"
						+ "company_id BIGINT NOT NULL,"
						+ "salesman_id BIGINT DEFAULT 0,"
						+ "bind_salesman_id BIGINT DEFAULT 0,"
						+ "chat_id VARCHAR(255))");
		jdbcTemplate.execute(
				"CREATE TABLE aftersales ("
						+ "aftersales_bn BIGINT PRIMARY KEY,"
						+ "company_id BIGINT NOT NULL,"
						+ "order_id BIGINT NOT NULL,"
						+ "salesman_id BIGINT DEFAULT 0)");
		processor = new TradeRefundPushMarketingCenterProcessor(jdbcTemplate, marketingCenterOpenApiSignedFormClient);
	}

	@AfterEach
	void tearDown() {
		database.shutdown();
	}

	@Test
	void handle_returnsEarly_whenSalesAndChatAllMissing() {
		long companyId = 10L;
		long orderId = 200L;
		long bn = 30L;
		jdbcTemplate.update(
				"INSERT INTO orders_normal_orders(order_id, company_id, salesman_id, bind_salesman_id, chat_id) VALUES (?,?,?,?,?)",
				orderId,
				companyId,
				0L,
				0L,
				"");
		jdbcTemplate.update(
				"INSERT INTO aftersales(aftersales_bn, company_id, order_id, salesman_id) VALUES (?,?,?,?)",
				bn,
				companyId,
				orderId,
				0L);

		Map<String, Object> refund = new LinkedHashMap<>();
		refund.put("company_id", companyId);
		refund.put("order_id", orderId);
		refund.put("aftersales_bn", bn);

		processor.handle(refund);

		verify(marketingCenterOpenApiSignedFormClient, never()).basicsAftersalesProccess(anyLong(), any());
		verify(marketingCenterOpenApiSignedFormClient, never()).basicsOrderProccess(anyLong(), any());
	}

	@Test
	void handle_callsOpenApi_whenBindingPresent() {
		long companyId = 77L;
		long orderId = 501L;
		long bn = 902L;
		jdbcTemplate.update(
				"INSERT INTO orders_normal_orders(order_id, company_id, salesman_id, bind_salesman_id, chat_id) VALUES (?,?,?,?,?)",
				orderId,
				companyId,
				0L,
				405L,
				"");
		jdbcTemplate.update(
				"INSERT INTO aftersales(aftersales_bn, company_id, order_id, salesman_id) VALUES (?,?,?,?)",
				bn,
				companyId,
				orderId,
				0L);

		Map<String, Object> refund = new LinkedHashMap<>();
		refund.put("company_id", companyId);
		refund.put("order_id", orderId);
		refund.put("aftersales_bn", bn);

		when(marketingCenterOpenApiSignedFormClient.basicsAftersalesProccess(eq(companyId), any()))
				.thenReturn(Map.of());

		processor.handle(refund);

		verify(marketingCenterOpenApiSignedFormClient).basicsAftersalesProccess(eq(companyId), any());
	}
}
