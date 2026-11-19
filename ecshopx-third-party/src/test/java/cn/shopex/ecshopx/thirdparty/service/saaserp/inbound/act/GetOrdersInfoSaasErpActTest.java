package cn.shopex.ecshopx.thirdparty.service.saaserp.inbound.act;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpInboundResult;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpNormalOrderLookupPort;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpOrderStructPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetOrdersInfoSaasErpActTest {

	private static final long COMPANY_ID = 42L;

	@Mock
	private SaasErpNormalOrderLookupPort normalOrderLookupPort;

	@Mock
	private SaasErpOrderStructPort orderStructPort;

	private GetOrdersInfoSaasErpAct handler;

	@BeforeEach
	void setUp() {
		handler = new GetOrdersInfoSaasErpAct(normalOrderLookupPort, orderStructPort, new ObjectMapper());
	}

	@Test
	void actIsGetOrdersInfo() {
		assertEquals("get_orders_info", handler.act());
	}

	@Test
	void missingOrderIdFailsWithoutLookup() {
		SaasErpInboundResult result = handler.handle(Map.of(), COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("订单号缺少！", result.msg());
		assertNull(result.info());
		verify(normalOrderLookupPort, never()).find(anyLong(), anyString());
	}

	@Test
	void blankOrderIdFailsWithoutLookup() {
		SaasErpInboundResult result = handler.handle(Map.of("order_id", "   "), COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("订单号缺少！", result.msg());
		verify(normalOrderLookupPort, never()).find(anyLong(), anyString());
	}

	@Test
	void orderNotFoundFails() {
		when(normalOrderLookupPort.find(COMPANY_ID, "O1")).thenReturn(Optional.empty());

		SaasErpInboundResult result = handler.handle(Map.of("order_id", "O1"), COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("未找到订单", result.msg());
		verify(orderStructPort, never()).getOrderStruct(anyLong(), anyString(), anyString());
	}

	@Test
	void forbiddenOrderClassFails() {
		when(normalOrderLookupPort.find(COMPANY_ID, "O1"))
				.thenReturn(Optional.of(Map.of("order_class", "pointsmall")));

		SaasErpInboundResult result = handler.handle(Map.of("order_id", "O1"), COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("pointsmall类型订单禁止同步", result.msg());
		verify(orderStructPort, never()).getOrderStruct(anyLong(), anyString(), anyString());
	}

	@Test
	void groupsOrderUsesNormalGroupsSourceType() {
		when(normalOrderLookupPort.find(COMPANY_ID, "G1"))
				.thenReturn(Optional.of(Map.of("order_class", "groups")));
		Map<String, Object> structFromPort = new LinkedHashMap<>();
		structFromPort.put("order_id", "G1");
		when(orderStructPort.getOrderStruct(eq(COMPANY_ID), eq("G1"), eq("normal_groups")))
				.thenReturn(Optional.of(structFromPort));

		handler.handle(Map.of("order_id", "G1"), COMPANY_ID);

		ArgumentCaptor<String> sourceTypeCaptor = ArgumentCaptor.forClass(String.class);
		verify(orderStructPort).getOrderStruct(eq(COMPANY_ID), eq("G1"), sourceTypeCaptor.capture());
		assertEquals("normal_groups", sourceTypeCaptor.getValue());
	}

	@Test
	void emptyStructFails() {
		when(normalOrderLookupPort.find(COMPANY_ID, "O1"))
				.thenReturn(Optional.of(Map.of("order_class", "normal")));
		when(orderStructPort.getOrderStruct(COMPANY_ID, "O1", "normal")).thenReturn(Optional.empty());

		SaasErpInboundResult result = handler.handle(Map.of("order_id", "O1"), COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("获取订单信息失败", result.msg());
	}

	@Test
	void successRewrapsPromotionDetailsAndDoesNotMutatePortMap() {
		when(normalOrderLookupPort.find(COMPANY_ID, "O1"))
				.thenReturn(Optional.of(Map.of("order_class", "normal")));
		Map<String, Object> structFromPort = new LinkedHashMap<>();
		structFromPort.put("promotion_details", "[{\"p\":1}]");
		when(orderStructPort.getOrderStruct(COMPANY_ID, "O1", "normal"))
				.thenReturn(Optional.of(structFromPort));

		SaasErpInboundResult result = handler.handle(Map.of("order_id", "O1"), COMPANY_ID);

		assertEquals("true", result.status());
		assertEquals("操作成功", result.msg());
		assertInstanceOf(Map.class, result.info());
		@SuppressWarnings("unchecked")
		Map<String, Object> info = (Map<String, Object>) result.info();
		@SuppressWarnings("unchecked")
		Map<String, Object> trade = (Map<String, Object>) info.get("trade");
		assertEquals("{\"promotiondetail\":[{\"p\":1}]}", trade.get("promotion_details"));
		assertEquals("[{\"p\":1}]", structFromPort.get("promotion_details"));
	}
}
