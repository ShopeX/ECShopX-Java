package cn.shopex.ecshopx.thirdparty.dispatch;

import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.thirdparty.service.dmcrm.OrderDeliveryDmCrmOnNormalOrderDeliveryProcessor;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderDeliveryDmCrmOnNormalOrderDeliveryDispatchListenerTest {

	@Mock
	private OrderDeliveryDmCrmOnNormalOrderDeliveryProcessor processor;

	@Test
	void onEvent_delegatesToProcessorHandle() {
		OrderDeliveryDmCrmOnNormalOrderDeliveryDispatchListener listener =
				new OrderDeliveryDmCrmOnNormalOrderDeliveryDispatchListener(processor);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 3L);
		listener.onEvent(payload);
		verify(processor).handle(payload);
	}
}
