package cn.shopex.ecshopx.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchJobNames;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class RefundByOrderUpdateOrderStatusJobDispatchPublisherImplTest {

	@Test
	void publish_delegatesToDispatchFacadeWithExpectedJobNameAndOptions() {
		DispatchFacade facade = Mockito.mock(DispatchFacade.class);
		RefundByOrderUpdateOrderStatusJobDispatchPublisherImpl impl =
				new RefundByOrderUpdateOrderStatusJobDispatchPublisherImpl(facade);

		impl.publish(42L, 7L, "service_groups");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		ArgumentCaptor<DispatchOptions> optionsCaptor = ArgumentCaptor.forClass(DispatchOptions.class);
		ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);

		verify(facade).dispatchJob(nameCaptor.capture(), payloadCaptor.capture(), optionsCaptor.capture());

		assertEquals(OrdersDispatchJobNames.REFUND_BY_ORDER_UPDATE_ORDER_STATUS_JOB, nameCaptor.getValue());
		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals(42L, payload.get("order_id"));
		assertEquals(7L, payload.get("company_id"));
		assertEquals("service_groups", payload.get("order_type"));

		DispatchOptions opts = optionsCaptor.getValue();
		assertEquals(DispatchMode.ASYNC, opts.mode());
		assertEquals(DispatchDriverType.REDIS, opts.driverOverride());
		assertEquals("default", opts.queue());
		assertEquals(Duration.ofSeconds(5), opts.delay());
	}
}
