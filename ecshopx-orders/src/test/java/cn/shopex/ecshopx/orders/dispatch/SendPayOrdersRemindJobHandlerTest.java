package cn.shopex.ecshopx.orders.dispatch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.port.orders.SendPayOrdersRemindWxaTemplatePort;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SendPayOrdersRemindJobHandlerTest {

	private final OrderAssociationsMapper orderAssociationsMapper = mock(OrderAssociationsMapper.class);
	private final SendPayOrdersRemindWxaTemplatePort port = mock(SendPayOrdersRemindWxaTemplatePort.class);
	private final SendPayOrdersRemindJobHandler handler =
			new SendPayOrdersRemindJobHandler(orderAssociationsMapper, port);

	@Test
	void whenOrderMissing_skipsPort() {
		when(orderAssociationsMapper.selectOne(any())).thenReturn(null);
		Map<String, Object> payload = payload(sampleOrderData());
		handler.handle(payload);
		verify(port, never()).sendPayOrdersRemind(any());
	}

	@Test
	void whenOrderStatusNotNotPay_skipsPort() {
		OrderAssociations row = new OrderAssociations();
		row.setOrderStatus("PAYED");
		when(orderAssociationsMapper.selectOne(any())).thenReturn(row);
		handler.handle(payload(sampleOrderData()));
		verify(port, never()).sendPayOrdersRemind(any());
	}

	@Test
	void whenNotPay_invokesPortWithOrderDataFromPayload() {
		OrderAssociations row = new OrderAssociations();
		row.setOrderStatus("NOTPAY");
		when(orderAssociationsMapper.selectOne(any())).thenReturn(row);
		Map<String, Object> orderData = sampleOrderData();
		handler.handle(payload(orderData));
		verify(port).sendPayOrdersRemind(orderData);
	}

	private static Map<String, Object> sampleOrderData() {
		Map<String, Object> orderData = new LinkedHashMap<>();
		orderData.put("order_id", 501L);
		orderData.put("company_id", 9L);
		orderData.put("title", "Item A");
		return orderData;
	}

	private static Map<String, Object> payload(Map<String, Object> orderData) {
		return Map.of("orderData", orderData);
	}
}
