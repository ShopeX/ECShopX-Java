package cn.shopex.ecshopx.thirdparty.service.marketingcenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class TradeFinishPushMarketingCenterProcessorTest {

	private static final long COMPANY_ID = 91L;
	private static final long ORDER_ID = 77001L;

	@Mock private JdbcTemplate jdbcTemplate;
	@Mock private MarketingCenterOpenApiSignedFormClient marketingCenterOpenApiSignedFormClient;

	private TradeFinishPushMarketingCenterProcessor processor;

	@BeforeEach
	void setUp() {
		processor = new TradeFinishPushMarketingCenterProcessor(jdbcTemplate, marketingCenterOpenApiSignedFormClient);
	}

	@Test
	void handle_whenMissingSalesmanId_doesNotCallBasicsOrderPay() throws Exception {
		stubOrderQuery(COMPANY_ID, ORDER_ID, 0L, 0L, "", 0L, 0L);

		Map<String, Object> tradeRow = baseTradePayload();

		processor.handle(tradeRow);

		verify(marketingCenterOpenApiSignedFormClient, never()).basicsOrderPay(anyLong(), any());
	}

	@Test
	void handle_whenFormatSalesDataRejects_doesNotCallBasicsOrderPay() throws Exception {
		stubOrderQuery(COMPANY_ID, ORDER_ID, 500L, 0L, "chat-1", 60L, 0L);
		when(jdbcTemplate.query(
						contains("shop_salesperson"),
						ArgumentMatchers.<ResultSetExtractor<Map<Long, String>>>any(),
						eq(COMPANY_ID),
						eq(500L)))
				.thenAnswer(
						invocation ->
								extractWorkUserids(invocation.getArgument(1), singleRowSalespersonResultSet(500L, "wuid-sales")));
		when(jdbcTemplate.query(
						contains("shop_rel_salesperson"),
						ArgumentMatchers.<ResultSetExtractor<Map<Long, java.util.List<Long>>>>any(),
						eq(COMPANY_ID),
						eq(500L)))
				.thenAnswer(invocation -> extractRelBySalesperson(invocation.getArgument(1), emptyResultSet()));

		Map<String, Object> tradeRow = baseTradePayload();

		processor.handle(tradeRow);

		verify(marketingCenterOpenApiSignedFormClient, never()).basicsOrderPay(anyLong(), any());
	}

	@Test
	void handle_happyPath_callsBasicsOrderPayWithLegacyOpenApiPayloadShape() throws Exception {
		long epoch = 1_704_067_200L;
		stubOrderQuery(COMPANY_ID, ORDER_ID, 500L, 0L, "group-chat", 50L, 0L);
		when(jdbcTemplate.query(
						contains("shop_salesperson"),
						ArgumentMatchers.<ResultSetExtractor<Map<Long, String>>>any(),
						eq(COMPANY_ID),
						eq(500L)))
				.thenAnswer(
						invocation ->
								extractWorkUserids(invocation.getArgument(1), singleRowSalespersonResultSet(500L, "wuid-sales")));
		when(jdbcTemplate.query(
						contains("shop_rel_salesperson"),
						ArgumentMatchers.<ResultSetExtractor<Map<Long, java.util.List<Long>>>>any(),
						eq(COMPANY_ID),
						eq(500L)))
				.thenAnswer(
						invocation ->
								extractRelBySalesperson(
										invocation.getArgument(1), distributorRelResultSet(50L, 500L)));
		when(jdbcTemplate.query(
						contains("distribution_distributor"),
						ArgumentMatchers.<ResultSetExtractor<Map<Long, String>>>any(),
						eq(COMPANY_ID),
						eq(50L)))
				.thenAnswer(
						invocation ->
								extractDistributorCodes(invocation.getArgument(1), distributorCodeResultSet(50L, "BNSC50")));

		Map<String, Object> tradeRow = baseTradePayload();
		tradeRow.put("pay_type", "deposit");
		tradeRow.put("time_expire", String.valueOf(epoch));
		tradeRow.put("transaction_id", "txn-xyz");
		tradeRow.put("total_fee", 1000);
		tradeRow.put("discount_fee", 10);
		tradeRow.put("pay_fee", 990);

		when(marketingCenterOpenApiSignedFormClient.basicsOrderPay(eq(COMPANY_ID), any())).thenReturn(Map.of());

		processor.handle(tradeRow);

		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(marketingCenterOpenApiSignedFormClient).basicsOrderPay(eq(COMPANY_ID), captor.capture());
		Map<String, Object> sent = captor.getValue();

		assertEquals("2", sent.get("pay_type"));
		assertEquals("1", sent.get("order_source"));
		assertEquals("txn-xyz", sent.get("transaction_id"));
		assertEquals("1000", sent.get("total_fee"));
		assertEquals("990", sent.get("pay_fee"));
		assertEquals("wuid-sales", sent.get("sale_salesperson_id"));
		assertEquals("0", sent.get("bind_salesperson_id"));
		assertEquals("BNSC50", sent.get("sale_store_bn"));
		assertEquals("", sent.get("bind_store_bn"));
		assertEquals("91", sent.get("company_id"));
		assertEquals("77001", sent.get("order_id"));
		assertEquals("group-chat", sent.get("chat_id"));
		assertTrue(sent.get("pay_time").toString().matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
	}

	private void stubOrderQuery(
			long companyId,
			long orderId,
			long salesmanId,
			long bindSalesmanId,
			String chatId,
			long saleSalesmanDistributorId,
			long bindSalesmanDistributorId)
			throws SQLException {
		when(jdbcTemplate.queryForObject(
						contains("orders_normal_orders"),
						ArgumentMatchers.<RowMapper<Map<String, Object>>>any(),
						eq(companyId),
						eq(orderId)))
				.thenAnswer(
						invocation -> {
							RowMapper<Map<String, Object>> rm = invocation.getArgument(1);
							ResultSet rs =
									orderRowResultSet(
											orderId,
											companyId,
											salesmanId,
											bindSalesmanId,
											chatId,
											saleSalesmanDistributorId,
											bindSalesmanDistributorId);
							return rm.mapRow(rs, 1);
						});
	}

	private static ResultSet orderRowResultSet(
			long orderId,
			long companyId,
			long salesmanId,
			long bindSalesmanId,
			String chatId,
			long saleSalesmanDistributorId,
			long bindSalesmanDistributorId)
			throws SQLException {
		ResultSet rs = mock(ResultSet.class);
		when(rs.getObject("order_id")).thenReturn(orderId);
		when(rs.getObject("company_id")).thenReturn(companyId);
		when(rs.getObject("salesman_id")).thenReturn(salesmanId);
		when(rs.getObject("bind_salesman_id")).thenReturn(bindSalesmanId);
		when(rs.getObject("chat_id")).thenReturn(chatId);
		when(rs.getObject("sale_salesman_distributor_id")).thenReturn(saleSalesmanDistributorId);
		when(rs.getObject("bind_salesman_distributor_id")).thenReturn(bindSalesmanDistributorId);
		return rs;
	}

	private static ResultSet singleRowSalespersonResultSet(long salespersonId, String workUserid) throws SQLException {
		ResultSet rs = mock(ResultSet.class);
		when(rs.next()).thenReturn(true, false);
		when(rs.getLong("salesperson_id")).thenReturn(salespersonId);
		when(rs.getObject("work_userid")).thenReturn(workUserid);
		return rs;
	}

	private static ResultSet emptyResultSet() throws SQLException {
		ResultSet rs = mock(ResultSet.class);
		when(rs.next()).thenReturn(false);
		return rs;
	}

	private static ResultSet distributorRelResultSet(long shopId, long salespersonId) throws SQLException {
		ResultSet rs = mock(ResultSet.class);
		when(rs.next()).thenReturn(true, false);
		when(rs.getLong("salesperson_id")).thenReturn(salespersonId);
		when(rs.getLong("shop_id")).thenReturn(shopId);
		return rs;
	}

	private static ResultSet distributorCodeResultSet(long distributorId, String shopCode) throws SQLException {
		ResultSet rs = mock(ResultSet.class);
		when(rs.next()).thenReturn(true, false);
		when(rs.getLong("distributor_id")).thenReturn(distributorId);
		when(rs.getObject("shop_code")).thenReturn(shopCode);
		return rs;
	}

	@SuppressWarnings("unchecked")
	private static Map<Long, String> extractWorkUserids(Object extractor, ResultSet rs) throws SQLException {
		return ((ResultSetExtractor<Map<Long, String>>) extractor).extractData(rs);
	}

	@SuppressWarnings("unchecked")
	private static Map<Long, java.util.List<Long>> extractRelBySalesperson(Object extractor, ResultSet rs)
			throws SQLException {
		return ((ResultSetExtractor<Map<Long, java.util.List<Long>>>) extractor).extractData(rs);
	}

	@SuppressWarnings("unchecked")
	private static Map<Long, String> extractDistributorCodes(Object extractor, ResultSet rs) throws SQLException {
		return ((ResultSetExtractor<Map<Long, String>>) extractor).extractData(rs);
	}

	private static Map<String, Object> baseTradePayload() {
		Map<String, Object> tradeRow = new LinkedHashMap<>();
		tradeRow.put("trade_id", "trade-alipay-1");
		tradeRow.put("company_id", COMPANY_ID);
		tradeRow.put("order_id", ORDER_ID);
		tradeRow.put("shop_id", 7L);
		tradeRow.put("distributor_id", 50L);
		tradeRow.put("user_id", 88001L);
		tradeRow.put("mobile", "13800138000");
		tradeRow.put("discount_info", "");
		tradeRow.put("mch_id", "mch1");
		tradeRow.put("total_fee", 0);
		tradeRow.put("discount_fee", 0);
		tradeRow.put("pay_fee", 0);
		tradeRow.put("pay_type", "wxpay");
		tradeRow.put("transaction_id", "");
		tradeRow.put("time_expire", "0");
		tradeRow.put("coupon_fee", 0);
		tradeRow.put("coupon_info", "");
		return tradeRow;
	}
}
