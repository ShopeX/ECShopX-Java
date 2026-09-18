package cn.shopex.ecshopx.thirdparty.service.marketingcenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

@ExtendWith(MockitoExtension.class)
class OrderDeliveryPushMarketingCenterOnNormalOrderDeliveryProcessorTest {

	@Mock
	private MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient;

	@Mock
	private OrderAddPushMarketingCenterSalesDataFormatter salesDataFormatter;

	private EmbeddedDatabase database;
	private JdbcTemplate jdbcTemplate;
	private OrderDeliveryPushMarketingCenterOnNormalOrderDeliveryProcessor processor;

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
						+ "sale_salesman_distributor_id BIGINT DEFAULT 0,"
						+ "bind_salesman_distributor_id BIGINT DEFAULT 0,"
						+ "title VARCHAR(500),"
						+ "mobile VARCHAR(50),"
						+ "user_id BIGINT,"
						+ "total_fee VARCHAR(50),"
						+ "order_status VARCHAR(50),"
						+ "pay_type VARCHAR(50),"
						+ "order_class VARCHAR(50),"
						+ "order_type VARCHAR(50),"
						+ "shop_id BIGINT,"
						+ "distributor_id BIGINT,"
						+ "receipt_type VARCHAR(50),"
						+ "receiver_name VARCHAR(200),"
						+ "receiver_mobile VARCHAR(50),"
						+ "receiver_state VARCHAR(100),"
						+ "receiver_city VARCHAR(100),"
						+ "receiver_district VARCHAR(100),"
						+ "receiver_address VARCHAR(500),"
						+ "create_time INT,"
						+ "auto_cancel_time VARCHAR(50),"
						+ "freight_fee INT,"
						+ "item_fee VARCHAR(50),"
						+ "discount_fee INT,"
						+ "PRIMARY KEY (company_id, order_id))");
		jdbcTemplate.execute(
				"CREATE TABLE orders_normal_orders_items ("
						+ "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
						+ "company_id BIGINT NOT NULL,"
						+ "order_id BIGINT NOT NULL,"
						+ "item_id BIGINT,"
						+ "goods_id BIGINT,"
						+ "item_bn VARCHAR(100),"
						+ "goods_bn VARCHAR(100),"
						+ "item_name VARCHAR(500),"
						+ "item_unit VARCHAR(50),"
						+ "pic VARCHAR(500),"
						+ "num INT,"
						+ "price INT,"
						+ "market_price INT,"
						+ "total_fee INT,"
						+ "item_fee INT,"
						+ "cost_fee INT,"
						+ "commission_fee INT,"
						+ "shop_id BIGINT,"
						+ "distributor_id BIGINT,"
						+ "order_item_type VARCHAR(50),"
						+ "fee_type VARCHAR(20),"
						+ "fee_rate REAL,"
						+ "fee_symbol VARCHAR(10),"
						+ "item_spec_desc VARCHAR(500))");
		processor = new OrderDeliveryPushMarketingCenterOnNormalOrderDeliveryProcessor(
				marketingCenterOpenApiSignedFormClient, jdbcTemplate, salesDataFormatter);
	}

	@AfterEach
	void tearDown() {
		database.shutdown();
	}

	@Test
	void handle_whenOrderMissing_noOutboundCall() {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 10L);
		payload.put("order_id", 999L);

		processor.handle(payload);

		verify(marketingCenterOpenApiSignedFormClient, never()).basicsOrderProccess(anyLong(), any());
		verify(salesDataFormatter, never()).format(anyLong(), any());
	}

	@Test
	void handle_whenSalesmanIdZero_noOutboundCall() {
		long companyId = 10L;
		long orderId = 200L;
		jdbcTemplate.update(
				"INSERT INTO orders_normal_orders(order_id, company_id, salesman_id, bind_salesman_id, chat_id,"
						+ " sale_salesman_distributor_id, bind_salesman_distributor_id, title, mobile, user_id, total_fee,"
						+ " order_status, pay_type, order_class, order_type, shop_id, distributor_id, receipt_type,"
						+ " receiver_name, receiver_mobile, receiver_state, receiver_city, receiver_district, receiver_address,"
						+ " create_time, auto_cancel_time, freight_fee, item_fee, discount_fee) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
				orderId,
				companyId,
				0L,
				0L,
				"",
				0L,
				0L,
				"t",
				"m",
				1L,
				"100",
				"NOTPAY",
				"wxpay",
				"normal",
				"normal",
				0L,
				0L,
				"logistics",
				"n",
				"m",
				"s",
				"c",
				"d",
				"a",
				0,
				"",
				0,
				"0",
				0);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("order_id", orderId);

		processor.handle(payload);

		verify(marketingCenterOpenApiSignedFormClient, never()).basicsOrderProccess(anyLong(), any());
		verify(salesDataFormatter, never()).format(anyLong(), any());
	}

	@Test
	void handle_whenSalesmanPresent_invokesBasicsOrderProccess() {
		long companyId = 77L;
		long orderId = 501L;
		insertEligibleOrderRow(companyId, orderId, 405L);
		jdbcTemplate.update(
				"INSERT INTO orders_normal_orders_items(company_id, order_id, item_id, goods_id, item_bn, goods_bn,"
						+ " item_name, item_unit, pic, num, price, market_price, total_fee, item_fee, cost_fee, commission_fee,"
						+ " shop_id, distributor_id, order_item_type, fee_type, fee_rate, fee_symbol, item_spec_desc) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
				companyId,
				orderId,
				1L,
				2L,
				"bn",
				"gbn",
				"Item",
				"件",
				"",
				1,
				100,
				120,
				100,
				100,
				0,
				0,
				0L,
				0L,
				"normal",
				"CNY",
				1.0f,
				"￥",
				"");

		when(salesDataFormatter.format(eq(companyId), any()))
				.thenAnswer(
						invocation -> {
							@SuppressWarnings("unchecked")
							Map<String, Object> d = new LinkedHashMap<>((Map<String, Object>) invocation.getArgument(1));
							return Optional.of(d);
						});

		when(marketingCenterOpenApiSignedFormClient.basicsOrderProccess(eq(companyId), any())).thenReturn(Map.of());

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("order_id", orderId);

		processor.handle(payload);

		verify(salesDataFormatter, times(1)).format(eq(companyId), any());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(marketingCenterOpenApiSignedFormClient).basicsOrderProccess(eq(companyId), captor.capture());
		Map<String, Object> sent = captor.getValue();
		assertEquals("1", sent.get("order_source"));
		assertInstanceOf(List.class, sent.get("items"));
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> itemsOut = (List<Map<String, Object>>) sent.get("items");
		assertEquals(1, itemsOut.size());
		assertEquals("Item", itemsOut.get(0).get("item_name"));
	}

	private void insertEligibleOrderRow(long companyId, long orderId, long salesmanId) {
		jdbcTemplate.update(
				"INSERT INTO orders_normal_orders(order_id, company_id, salesman_id, bind_salesman_id, chat_id,"
						+ " sale_salesman_distributor_id, bind_salesman_distributor_id, title, mobile, user_id, total_fee,"
						+ " order_status, pay_type, order_class, order_type, shop_id, distributor_id, receipt_type,"
						+ " receiver_name, receiver_mobile, receiver_state, receiver_city, receiver_district, receiver_address,"
						+ " create_time, auto_cancel_time, freight_fee, item_fee, discount_fee) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
				orderId,
				companyId,
				salesmanId,
				0L,
				"",
				0L,
				0L,
				"t",
				"m",
				1L,
				"100",
				"NOTPAY",
				"deposit",
				"normal",
				"normal",
				0L,
				0L,
				"logistics",
				"n",
				"m",
				"s",
				"c",
				"d",
				"a",
				0,
				"",
				0,
				"0",
				0);
	}
}
