package cn.shopex.ecshopx.thirdparty.service.saaserp.inbound.act;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpDeliveryOrderPort;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpInboundResult;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpSepDeliveryPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateDeliverySaasErpActTest {

	private static final long COMPANY_ID = 99L;

	@Mock
	private SaasErpDeliveryOrderPort deliveryOrderPort;

	@Mock
	private SaasErpSepDeliveryPort sepDeliveryPort;

	private CreateDeliverySaasErpAct handler;

	@BeforeEach
	void setUp() {
		handler = new CreateDeliverySaasErpAct(deliveryOrderPort, sepDeliveryPort, new ObjectMapper());
	}

	@Test
	void actIsOmeCreateDelivery() {
		assertEquals("ome_create_delivery", handler.act());
	}

	@Test
	void missingOrderIdFailsFirst() {
		SaasErpInboundResult result =
				handler.handle(Map.of("logi_no", "L1", "struct", "[]", "delivery_type", "delivery"), COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("订单号缺少！", result.msg());
		verify(deliveryOrderPort, never()).load(anyLong(), anyString());
		verify(sepDeliveryPort, never()).deliver(any());
	}

	@Test
	void missingLogiNoFailsSecond() {
		SaasErpInboundResult result =
				handler.handle(Map.of("order_id", "O1", "struct", "[]", "delivery_type", "delivery"), COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("缺少物流单号", result.msg());
		verify(deliveryOrderPort, never()).load(anyLong(), anyString());
	}

	@Test
	void missingStructFailsThird() {
		SaasErpInboundResult result =
				handler.handle(Map.of("order_id", "O1", "logi_no", "LN1", "delivery_type", "delivery"), COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("缺少发货商品", result.msg());
		verify(deliveryOrderPort, never()).load(anyLong(), anyString());
	}

	@Test
	void missingLogiCodeFailsFourthForDeliveryType() {
		SaasErpInboundResult result = handler.handle(
				Map.of(
						"order_id", "O1",
						"logi_no", "LN1",
						"struct", "[{\"product_bn\":\"A\",\"number\":1}]",
						"delivery_type", "delivery"),
				COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("缺少物流编码", result.msg());
		verify(deliveryOrderPort, never()).load(anyLong(), anyString());
	}

	@Test
	void missingDeliveryTypeFailsWithEmptySuffix() {
		SaasErpInboundResult result = handler.handle(
				Map.of("order_id", "O1", "logi_no", "LN1", "struct", "[{\"product_bn\":\"A\",\"number\":1}]"),
				COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("不支持发货类型：", result.msg());
		verify(deliveryOrderPort, never()).load(anyLong(), anyString());
	}

	@Test
	void unsupportedDeliveryTypeFails() {
		SaasErpInboundResult result = handler.handle(
				Map.of(
						"order_id", "O1",
						"logi_no", "LN1",
						"struct", "[{\"product_bn\":\"A\",\"number\":1}]",
						"delivery_type", "exchange"),
				COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("不支持发货类型：exchange", result.msg());
		verify(deliveryOrderPort, never()).load(anyLong(), anyString());
	}

	@Test
	void returnTypeSucceedsWithoutPorts() {
		SaasErpInboundResult result = handler.handle(
				Map.of(
						"order_id", "O1",
						"logi_no", "LN1",
						"struct", "[{\"product_bn\":\"A\",\"number\":1}]",
						"delivery_type", "return"),
				COMPANY_ID);

		assertEquals("true", result.status());
		assertEquals(Boolean.TRUE, result.info());
		verify(deliveryOrderPort, never()).load(anyLong(), anyString());
		verify(sepDeliveryPort, never()).deliver(any());
	}

	@Test
	void orderNotFoundFails() {
		when(deliveryOrderPort.load(COMPANY_ID, "O1")).thenReturn(Optional.empty());

		SaasErpInboundResult result = handler.handle(deliveryParams("O1"), COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("此订单不存在", result.msg());
		verify(sepDeliveryPort, never()).deliver(any());
	}

	@Test
	void orderAlreadyDeliveredFails() {
		when(deliveryOrderPort.load(COMPANY_ID, "O1"))
				.thenReturn(Optional.of(Map.of("delivery_status", "DONE", "order_id", "O1", "items", List.of())));

		SaasErpInboundResult result = handler.handle(deliveryParams("O1"), COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("订单已发货，请勿重复发货", result.msg());
		verify(sepDeliveryPort, never()).deliver(any());
	}

	@Test
	void wrongPendingProductsFails() {
		Map<String, Object> pendingOther = itemRow("OTHER", "PENDING", 0, 2);
		when(deliveryOrderPort.load(COMPANY_ID, "O1"))
				.thenReturn(Optional.of(orderWithItems(List.of(pendingOther))));

		SaasErpInboundResult result = handler.handle(deliveryParams("O1"), COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("发货商品有误", result.msg());
		verify(sepDeliveryPort, never()).deliver(any());
	}

	@Test
	void allLinesDoneFailsOrderAlreadyShippedComplete() {
		Map<String, Object> done = itemRow("A", "DONE", 2, 2);
		when(deliveryOrderPort.load(COMPANY_ID, "O1"))
				.thenReturn(Optional.of(orderWithItems(List.of(done))));

		SaasErpInboundResult result = handler.handle(deliveryParams("O1"), COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("订单已经发货完成", result.msg());
		verify(sepDeliveryPort, never()).deliver(any());
	}

	@Test
	void deliverReturnsFalsyFails() {
		Map<String, Object> pending = itemRow("A", "PENDING", 0, 2);
		when(deliveryOrderPort.load(COMPANY_ID, "O1"))
				.thenReturn(Optional.of(orderWithItems(List.of(pending))));
		when(sepDeliveryPort.deliver(any())).thenReturn(Boolean.FALSE);

		SaasErpInboundResult result = handler.handle(deliveryParams("O1"), COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("操作失败", result.msg());
	}

	@Test
	void runtimeExceptionFormatsMsgWithLineAndArrow() {
		Map<String, Object> pending = itemRow("A", "PENDING", 0, 2);
		when(deliveryOrderPort.load(COMPANY_ID, "O1"))
				.thenReturn(Optional.of(orderWithItems(List.of(pending))));
		when(sepDeliveryPort.deliver(any())).thenThrow(new RuntimeException("sep boom"));

		SaasErpInboundResult result = handler.handle(deliveryParams("O1"), COMPANY_ID);

		assertEquals("fail", result.status());
		assertTrue(result.msg().contains(",msg=>sep boom"));
		assertTrue(result.msg().matches("\\d+,msg=>sep boom"));
	}

	@Test
	void successfulDeliveryBuildsSepParams() throws Exception {
		Map<String, Object> pending = itemRow("A", "PENDING", 0, 2);
		Map<String, Object> order = orderWithItems(List.of(pending));
		order.put("company_id", 77L);
		when(deliveryOrderPort.load(COMPANY_ID, "O1")).thenReturn(Optional.of(order));
		Map<String, Object> deliverResult = Map.of("ok", 1);
		when(sepDeliveryPort.deliver(any())).thenReturn(deliverResult);

		SaasErpInboundResult result = handler.handle(deliveryParams("O1"), COMPANY_ID);

		assertEquals("true", result.status());
		assertEquals("操作成功", result.msg());
		assertEquals(deliverResult, result.info());

		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(sepDeliveryPort).deliver(captor.capture());
		Map<String, Object> sepParams = captor.getValue();
		assertEquals(77L, sepParams.get("company_id"));
		assertEquals("sep", sepParams.get("delivery_type"));
		assertEquals("new", sepParams.get("type"));
		assertEquals("O1", String.valueOf(sepParams.get("order_id")));
		assertEquals("LN1", sepParams.get("delivery_code"));
		assertEquals("SF", sepParams.get("delivery_corp"));
		assertNull(sepParams.get("logi_name"));
		assertNull(sepParams.get("ship_mobile"));
		assertTrue(sepParams.get("sepInfo") instanceof String);
		ObjectMapper mapper = new ObjectMapper();
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> sepInfo =
				mapper.readValue((String) sepParams.get("sepInfo"), List.class);
		assertEquals(1, sepInfo.size());
		assertEquals("A", sepInfo.get(0).get("item_bn"));
		assertEquals("LN1", sepInfo.get(0).get("delivery_code"));
		assertEquals("SF", sepInfo.get(0).get("delivery_corp"));
		Object deliveryNum = sepInfo.get(0).get("delivery_num");
		assertTrue(deliveryNum instanceof Number, "delivery_num should be numeric");
		assertEquals(1, ((Number) deliveryNum).intValue());
		assertEquals("1", String.valueOf(deliveryNum).trim());
	}

	@Test
	void stringNumberFromErpYieldsWholeIntDeliveryNum() throws Exception {
		Map<String, Object> pending = itemRow("KC26011700000010", "PENDING", 0, 1);
		pending.put("id", 6271L);
		when(deliveryOrderPort.load(COMPANY_ID, "5441653000350261"))
				.thenReturn(Optional.of(orderWithItems(List.of(pending))));
		when(sepDeliveryPort.deliver(any())).thenReturn(Boolean.TRUE);

		Map<String, String> params = new LinkedHashMap<>();
		params.put("order_id", "5441653000350261");
		params.put("logi_no", "SF123456789");
		params.put("logi_code", "SF");
		params.put(
				"struct",
				"[{\"item_id\":\"\",\"item_type\":\"\",\"product_bn\":\"KC26011700000010\","
						+ "\"product_name\":\"jacket\",\"number\":\"1\"}]");
		params.put("delivery_type", "delivery");

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertEquals("true", result.status());
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(sepDeliveryPort).deliver(captor.capture());
		ObjectMapper mapper = new ObjectMapper();
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> sepInfo =
				mapper.readValue((String) captor.getValue().get("sepInfo"), List.class);
		Object deliveryNum = sepInfo.get(0).get("delivery_num");
		assertEquals("1", String.valueOf(deliveryNum).trim());
		assertTrue(String.valueOf(deliveryNum).trim().matches("^[1-9][0-9]*$"));
	}

	private static Map<String, String> deliveryParams(String orderId) {
		return Map.of(
				"order_id", orderId,
				"logi_no", "LN1",
				"logi_code", "SF",
				"struct", "[{\"product_bn\":\"A\",\"number\":1}]",
				"delivery_type", "delivery");
	}

	private static Map<String, Object> orderWithItems(List<Map<String, Object>> items) {
		Map<String, Object> order = new LinkedHashMap<>();
		order.put("delivery_status", "PENDING");
		order.put("order_id", "O1");
		order.put("company_id", COMPANY_ID);
		order.put("items", new ArrayList<>(items));
		return order;
	}

	private static Map<String, Object> itemRow(String bn, String status, int delivered, int num) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("item_bn", bn);
		row.put("delivery_status", status);
		row.put("delivery_item_num", delivered);
		row.put("num", num);
		return row;
	}
}
