package cn.shopex.ecshopx.openapi.thirdapi.v2.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.openapi.OpenapiAuthAttributes;
import cn.shopex.ecshopx.common.openapi.OpenapiEnabledLogisticsListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderCancelPort;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderCancelReasonsPort;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderConfirmCancelPort;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderDetailPort;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderIncrListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderListV2Port;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderSoldListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderWriteoffPort;
import cn.shopex.ecshopx.common.openapi.OpenapiShippingTemplatesListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiTradeListPort;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderControllerWriteoffTest {

	@Mock private OpenapiOrderWriteoffPort orderWriteoffPort;
	@Mock private OpenapiOrderIncrListPort orderIncrListPort;
	@Mock private OpenapiOrderSoldListPort orderSoldListPort;
	@Mock private OpenapiOrderListV2Port orderListV2Port;
	@Mock private OpenapiOrderDetailPort orderDetailPort;
	@Mock private OpenapiOrderCancelReasonsPort cancelReasonsPort;
	@Mock private OpenapiOrderCancelPort orderCancelPort;
	@Mock private OpenapiEnabledLogisticsListPort enabledLogisticsListPort;
	@Mock private OpenapiOrderConfirmCancelPort confirmCancelPort;
	@Mock private OpenapiShippingTemplatesListPort shippingTemplatesListPort;
	@Mock private OpenapiTradeListPort tradeListPort;
	@Mock private HttpServletRequest request;

	private OrderController controller;

	@BeforeEach
	void setUp() {
		controller =
				new OrderController(
						orderWriteoffPort,
						orderIncrListPort,
						orderSoldListPort,
						orderListV2Port,
						orderDetailPort,
						cancelReasonsPort,
						orderCancelPort,
						enabledLogisticsListPort,
						confirmCancelPort,
						shippingTemplatesListPort,
						tradeListPort);
		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", 9L);
		when(request.getAttribute(OpenapiAuthAttributes.REQUEST_ATTRIBUTE)).thenReturn(auth);
	}

	@Test
	void orderWriteoff_delegatesToPort() {
		Map<String, Object> body = Map.of("order_id", 501L);
		Map<String, Object> result = controller.orderWriteoff(request, null, null, body);
		verify(orderWriteoffPort).executeWriteoff(9L, "501", null);
		assertEquals(true, result.get("status"));
	}
}
