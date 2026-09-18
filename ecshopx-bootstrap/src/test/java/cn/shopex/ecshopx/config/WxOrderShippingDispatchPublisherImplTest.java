package cn.shopex.ecshopx.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WxOrderShippingDispatchPublisherImplTest {

	@Mock
	private DispatchFacade dispatchFacade;

	@Test
	void publish_withoutDelay_delegatesToDispatchFacadeWithNullDelay() {
		WxOrderShippingDispatchPublisherImpl impl = new WxOrderShippingDispatchPublisherImpl(dispatchFacade);
		Map<String, Object> payload = Map.of("orderId", 42L);

		impl.publish(payload);

		ArgumentCaptor<DispatchOptions> optionsCaptor = ArgumentCaptor.forClass(DispatchOptions.class);
		verify(dispatchFacade)
				.publishEvent(eq(OrdersDispatchEventNames.EVENT_WX_ORDER_SHIPPING), eq(payload), optionsCaptor.capture());

		DispatchOptions opts = optionsCaptor.getValue();
		assertEquals(DispatchMode.ASYNC, opts.mode());
		assertEquals(DispatchDriverType.REDIS, opts.driverOverride());
		assertNull(opts.delay());
	}

	@Test
	void publish_withDelay_delegatesToDispatchFacadeWithDelay() {
		WxOrderShippingDispatchPublisherImpl impl = new WxOrderShippingDispatchPublisherImpl(dispatchFacade);
		Map<String, Object> payload = Map.of("orderId", 42L);
		Duration delay = Duration.ofSeconds(3);

		impl.publish(payload, delay);

		ArgumentCaptor<DispatchOptions> optionsCaptor = ArgumentCaptor.forClass(DispatchOptions.class);
		verify(dispatchFacade)
				.publishEvent(eq(OrdersDispatchEventNames.EVENT_WX_ORDER_SHIPPING), anyMap(), optionsCaptor.capture());

		assertEquals(Duration.ofSeconds(3), optionsCaptor.getValue().delay());
	}
}
