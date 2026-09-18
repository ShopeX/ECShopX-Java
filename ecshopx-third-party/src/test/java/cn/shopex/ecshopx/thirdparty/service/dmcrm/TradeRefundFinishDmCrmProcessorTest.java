package cn.shopex.ecshopx.thirdparty.service.dmcrm;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class TradeRefundFinishDmCrmProcessorTest {

	private static final long COMPANY_ID = 501L;
	private static final long ORDER_ID = 9002L;

	@Mock
	private DmCrmSettingReadPort dmCrmSettingReadPort;

	@Mock
	private JdbcTemplate jdbcTemplate;

	@Mock
	private DmCrmTradeRefundFinishOrderSyncPort dmCrmTradeRefundFinishOrderSyncPort;

	private TradeRefundFinishDmCrmProcessor processor;

	@BeforeEach
	void setUp() {
		processor = new TradeRefundFinishDmCrmProcessor(dmCrmSettingReadPort, jdbcTemplate, dmCrmTradeRefundFinishOrderSyncPort);
	}

	@Test
	void handle_whenDmCrmClosed_skipsPortAndQueries() {
		when(dmCrmSettingReadPort.isPointIntegrationOpen(COMPANY_ID)).thenReturn(false);
		Map<String, Object> payload = basePayload();
		processor.handle(payload);
		verifyNoInteractions(jdbcTemplate);
		verify(dmCrmTradeRefundFinishOrderSyncPort, never()).syncAfter(anyLong(), any(), any());
		verify(dmCrmTradeRefundFinishOrderSyncPort, never()).syncForwardAfter(anyLong(), any(), any());
	}

	@Test
	void handle_whenOrderMissing_skipsPort() throws SQLException {
		when(dmCrmSettingReadPort.isPointIntegrationOpen(COMPANY_ID)).thenReturn(true);
		when(jdbcTemplate.queryForObject(
						contains("orders_normal_orders"),
						ArgumentMatchers.<RowMapper<Map<String, Object>>>any(),
						eq(COMPANY_ID),
						eq(ORDER_ID)))
				.thenThrow(new EmptyResultDataAccessException(1));
		processor.handle(basePayload());
		verify(dmCrmTradeRefundFinishOrderSyncPort, never()).syncAfter(anyLong(), any(), any());
		verify(dmCrmTradeRefundFinishOrderSyncPort, never()).syncForwardAfter(anyLong(), any(), any());
	}

	@Test
	void handle_whenNoDmPointPreIdButPointSignals_skipsPort() throws SQLException {
		when(dmCrmSettingReadPort.isPointIntegrationOpen(COMPANY_ID)).thenReturn(true);
		stubOrderRow(null, 1, 0, "normal", 0L, 0L);
		processor.handle(basePayload());
		verify(dmCrmTradeRefundFinishOrderSyncPort, never()).syncAfter(anyLong(), any(), any());
		verify(dmCrmTradeRefundFinishOrderSyncPort, never()).syncForwardAfter(anyLong(), any(), any());
		verify(jdbcTemplate, never())
				.query(
						contains("orders_normal_orders_items"),
						ArgumentMatchers.<RowMapper<Map<String, Object>>>any(),
						eq(COMPANY_ID),
						eq(ORDER_ID));
	}

	@Test
	void handle_whenAftersalesRefundGoods_invokesSyncAfterWithOrderStatus2() throws SQLException {
		when(dmCrmSettingReadPort.isPointIntegrationOpen(COMPANY_ID)).thenReturn(true);
		stubOrderRow("pre-1", 0, 0, "normal", 0L, 0L);
		stubOrderItemsOneRow(77L);
		when(jdbcTemplate.queryForObject(
						contains("FROM aftersales WHERE"),
						ArgumentMatchers.<RowMapper<String>>any(),
						eq(COMPANY_ID),
						eq(888001L)))
				.thenAnswer(
						invocation -> {
							RowMapper<String> rm = invocation.getArgument(1);
							ResultSet rs = mock(ResultSet.class);
							when(rs.getObject("aftersales_type")).thenReturn("REFUND_GOODS");
							return rm.mapRow(rs, 0);
						});
		stubAftersalesDetailsOneRow(77L, 100);

		Map<String, Object> payload = basePayload();
		payload.put("aftersales_bn", 888001L);
		payload.put("refund_point", 5);
		payload.put("return_freight", 0);
		payload.put("refunded_fee", 100);

		processor.handle(payload);

		verify(dmCrmTradeRefundFinishOrderSyncPort, times(1))
				.syncAfter(
						eq(COMPANY_ID),
						eq(String.valueOf(ORDER_ID)),
						argThat(m -> Integer.valueOf(2).equals(m.get("orderStatus"))));
		verify(dmCrmTradeRefundFinishOrderSyncPort, never()).syncForwardAfter(anyLong(), any(), any());
	}

	@Test
	void handle_whenAftersalesOnlyRefund_invokesSyncForwardAfterWithOrderStatus5() throws SQLException {
		when(dmCrmSettingReadPort.isPointIntegrationOpen(COMPANY_ID)).thenReturn(true);
		stubOrderRow("pre-1", 0, 0, "normal", 0L, 0L);
		stubOrderItemsOneRow(12L);
		when(jdbcTemplate.queryForObject(
						contains("FROM aftersales WHERE"),
						ArgumentMatchers.<RowMapper<String>>any(),
						eq(COMPANY_ID),
						eq(888002L)))
				.thenAnswer(
						invocation -> {
							RowMapper<String> rm = invocation.getArgument(1);
							ResultSet rs = mock(ResultSet.class);
							when(rs.getObject("aftersales_type")).thenReturn("ONLY_REFUND");
							return rm.mapRow(rs, 0);
						});
		stubAftersalesDetailsOneRow(12L, 50);

		Map<String, Object> payload = basePayload();
		payload.put("aftersales_bn", 888002L);
		payload.put("refunded_fee", 50);

		processor.handle(payload);

		verify(dmCrmTradeRefundFinishOrderSyncPort, never()).syncAfter(anyLong(), any(), any());
		verify(dmCrmTradeRefundFinishOrderSyncPort, times(1))
				.syncForwardAfter(
						eq(COMPANY_ID),
						eq(String.valueOf(ORDER_ID)),
						argThat(m -> Integer.valueOf(5).equals(m.get("orderStatus"))));
	}

	@Test
	void handle_whenPresales_invokesSyncForwardAfterWithOrderStatus5() throws SQLException {
		when(dmCrmSettingReadPort.isPointIntegrationOpen(COMPANY_ID)).thenReturn(true);
		stubOrderRow("pre-1", 0, 0, "normal", 0L, 0L);
		stubOrderItemsOneRow(33L);

		Map<String, Object> payload = basePayload();
		payload.remove("aftersales_bn");

		processor.handle(payload);

		verify(dmCrmTradeRefundFinishOrderSyncPort, times(1))
				.syncForwardAfter(
						eq(COMPANY_ID),
						eq(String.valueOf(ORDER_ID)),
						argThat(m -> Integer.valueOf(5).equals(m.get("orderStatus")) && m.containsKey("items")));
	}

	@Test
	void handle_whenPortThrows_swallowsAndDoesNotPropagate() throws SQLException {
		when(dmCrmSettingReadPort.isPointIntegrationOpen(COMPANY_ID)).thenReturn(true);
		stubOrderRow("pre-1", 0, 0, "normal", 0L, 0L);
		stubOrderItemsOneRow(33L);
		doThrow(new RuntimeException("boom"))
				.when(dmCrmTradeRefundFinishOrderSyncPort)
				.syncForwardAfter(anyLong(), any(), any());

		Map<String, Object> payload = basePayload();
		payload.remove("aftersales_bn");

		assertDoesNotThrow(() -> processor.handle(payload));
	}

	private Map<String, Object> basePayload() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("company_id", COMPANY_ID);
		m.put("order_id", ORDER_ID);
		m.put("refund_bn", 111L);
		m.put("aftersales_bn", "");
		m.put("refunded_fee", 10);
		m.put("refund_fee", 10);
		m.put("refund_point", 0);
		m.put("return_freight", 0);
		m.put("freight", 0);
		return m;
	}

	private void stubOrderRow(
			String dmPointPreid, int pointFee, int pointUse, String orderClass, long salesmanId, long distId)
			throws SQLException {
		when(jdbcTemplate.queryForObject(
						contains("orders_normal_orders"),
						ArgumentMatchers.<RowMapper<Map<String, Object>>>any(),
						eq(COMPANY_ID),
						eq(ORDER_ID)))
				.thenAnswer(
						invocation -> {
							RowMapper<Map<String, Object>> rm = invocation.getArgument(1);
							ResultSet rs = mock(ResultSet.class);
							when(rs.getObject("order_id")).thenReturn(ORDER_ID);
							when(rs.getObject("company_id")).thenReturn(COMPANY_ID);
							when(rs.getObject("dm_point_preid")).thenReturn(dmPointPreid);
							when(rs.getObject("point_fee")).thenReturn(pointFee);
							when(rs.getObject("point_use")).thenReturn(pointUse);
							when(rs.getObject("order_class")).thenReturn(orderClass);
							when(rs.getObject("salesman_id")).thenReturn(salesmanId);
							when(rs.getObject("sale_salesman_distributor_id")).thenReturn(distId);
							when(rs.getObject("user_id")).thenReturn(7001L);
							when(rs.getObject("receiver_name")).thenReturn("n");
							when(rs.getObject("receiver_mobile")).thenReturn("m");
							when(rs.getObject("receiver_zip")).thenReturn("");
							when(rs.getObject("receiver_address")).thenReturn("addr");
							when(rs.getObject("create_time")).thenReturn(1);
							when(rs.getObject("freight_fee")).thenReturn(100);
							when(rs.getObject("item_fee")).thenReturn("1000");
							when(rs.getObject("total_fee")).thenReturn("1000");
							when(rs.getObject("remark")).thenReturn("");
							return rm.mapRow(rs, 0);
						});
	}

	private void stubOrderItemsOneRow(long itemId) throws SQLException {
		when(jdbcTemplate.query(
						contains("orders_normal_orders_items"),
						ArgumentMatchers.<RowMapper<Map<String, Object>>>any(),
						eq(COMPANY_ID),
						eq(ORDER_ID)))
				.thenAnswer(
						invocation -> {
							RowMapper<Map<String, Object>> rm = invocation.getArgument(1);
							ResultSet rs = mock(ResultSet.class);
							when(rs.getObject("item_id")).thenReturn(itemId);
							when(rs.getObject("goods_id")).thenReturn(1L);
							when(rs.getObject("item_bn")).thenReturn("ibn");
							when(rs.getObject("goods_bn")).thenReturn("gbn");
							when(rs.getObject("item_name")).thenReturn("nm");
							when(rs.getObject("num")).thenReturn(1);
							when(rs.getObject("item_fee")).thenReturn(200);
							when(rs.getObject("total_fee")).thenReturn(200);
							when(rs.getObject("market_price")).thenReturn(200);
							when(rs.getObject("price")).thenReturn(200);
							when(rs.getObject("order_item_type")).thenReturn("normal");
							return List.of(rm.mapRow(rs, 0));
						});
	}

	private void stubAftersalesDetailsOneRow(long itemId, int refundFee) throws SQLException {
		when(jdbcTemplate.query(
						contains("aftersales_detail"),
						ArgumentMatchers.<RowMapper<Map<String, Object>>>any(),
						eq(COMPANY_ID),
						anyLong()))
				.thenAnswer(
						invocation -> {
							RowMapper<Map<String, Object>> rm = invocation.getArgument(1);
							ResultSet rs = mock(ResultSet.class);
							when(rs.getObject("item_id")).thenReturn(itemId);
							when(rs.getObject("num")).thenReturn(1);
							when(rs.getObject("refund_fee")).thenReturn(refundFee);
							when(rs.getObject("refund_point")).thenReturn(0);
							when(rs.getObject("item_bn")).thenReturn("ibn");
							when(rs.getObject("item_name")).thenReturn("nm");
							return List.of(rm.mapRow(rs, 0));
						});
	}
}
