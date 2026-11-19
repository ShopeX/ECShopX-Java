package cn.shopex.ecshopx.thirdparty.service.saaserp.inbound.act;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpInboundResult;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpOrderCancelStatusPort;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateOrderReviewStatusSaasErpActTest {

	private static final long COMPANY_ID = 10L;

	@Mock
	private SaasErpOrderCancelStatusPort orderCancelStatusPort;

	private UpdateOrderReviewStatusSaasErpAct handler;

	@BeforeEach
	void setUp() {
		handler = new UpdateOrderReviewStatusSaasErpAct(orderCancelStatusPort);
	}

	@Test
	void actIsUpdateOrderStatus() {
		assertEquals("update_order_status", handler.act());
	}

	@Test
	void missingOrderIdFailsWithoutPortCall() {
		SaasErpInboundResult result = handler.handle(deadStatusParams(null), COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("订单信息有误", result.msg());
		assertNull(result.info());
		verify(orderCancelStatusPort, never()).findCancelStatus(anyLong(), anyString());
		verifyNoMoreInteractions(orderCancelStatusPort);
	}

	@Test
	void blankOrderIdFailsWithoutPortCall() {
		SaasErpInboundResult result = handler.handle(deadStatusParams("   "), COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("订单信息有误", result.msg());
		verify(orderCancelStatusPort, never()).findCancelStatus(anyLong(), anyString());
		verifyNoMoreInteractions(orderCancelStatusPort);
	}

	@Test
	void emptyCancelStatusFails() {
		when(orderCancelStatusPort.findCancelStatus(COMPANY_ID, "9")).thenReturn(Optional.empty());

		SaasErpInboundResult result = handler.handle(deadStatusParams("9"), COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("订单信息有误", result.msg());
		verify(orderCancelStatusPort).findCancelStatus(COMPANY_ID, "9");
		verifyNoMoreInteractions(orderCancelStatusPort);
	}

	@Test
	void wrongRequestStatusFailsEvenWhenCancelSuccess() {
		when(orderCancelStatusPort.findCancelStatus(COMPANY_ID, "9")).thenReturn(Optional.of("SUCCESS"));

		Map<String, String> params = new LinkedHashMap<>();
		params.put("order_id", "9");
		params.put("status", "alive");
		params.put("type", "status");

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertEquals("fail", result.status());
		assertEquals("订单信息有误", result.msg());
		verify(orderCancelStatusPort).findCancelStatus(COMPANY_ID, "9");
		verifyNoMoreInteractions(orderCancelStatusPort);
	}

	@Test
	void matchingReviewStatusReturnsOkWithStatusTrue() {
		when(orderCancelStatusPort.findCancelStatus(COMPANY_ID, "9")).thenReturn(Optional.of("SUCCESS"));

		SaasErpInboundResult result = handler.handle(deadStatusParams("9"), COMPANY_ID);

		assertEquals("true", result.status());
		assertEquals("操作成功", result.msg());
		assertInstanceOf(Map.class, result.info());
		@SuppressWarnings("unchecked")
		Map<String, Object> info = (Map<String, Object>) result.info();
		assertEquals(Boolean.TRUE, info.get("status"));
		verify(orderCancelStatusPort).findCancelStatus(COMPANY_ID, "9");
		verifyNoMoreInteractions(orderCancelStatusPort);
	}

	private static Map<String, String> deadStatusParams(String orderId) {
		Map<String, String> params = new LinkedHashMap<>();
		if (orderId != null) {
			params.put("order_id", orderId);
		}
		params.put("status", "dead");
		params.put("type", "status");
		return params;
	}
}
