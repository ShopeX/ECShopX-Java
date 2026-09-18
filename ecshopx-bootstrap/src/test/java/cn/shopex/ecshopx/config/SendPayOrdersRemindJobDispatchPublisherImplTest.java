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

class SendPayOrdersRemindJobDispatchPublisherImplTest {

	@Test
	void publish_invokesDispatchFacadeWithExpectedJobNameQueueDelay() {
		DispatchFacade facade = Mockito.mock(DispatchFacade.class);
		SendPayOrdersRemindJobDispatchPublisherImpl impl = new SendPayOrdersRemindJobDispatchPublisherImpl(facade);

		impl.publish(Map.of("order_id", 1L, "company_id", 2L));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		ArgumentCaptor<DispatchOptions> optionsCaptor = ArgumentCaptor.forClass(DispatchOptions.class);
		ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);

		verify(facade).dispatchJob(nameCaptor.capture(), payloadCaptor.capture(), optionsCaptor.capture());

		assertEquals(OrdersDispatchJobNames.SEND_PAY_ORDERS_REMIND_JOB, nameCaptor.getValue());
		Map<String, Object> payload = payloadCaptor.getValue();
		@SuppressWarnings("unchecked")
		Map<String, Object> inner = (Map<String, Object>) payload.get("orderData");
		assertEquals(1L, inner.get("order_id"));
		assertEquals(2L, inner.get("company_id"));

		DispatchOptions opts = optionsCaptor.getValue();
		assertEquals(DispatchMode.ASYNC, opts.mode());
		assertEquals(DispatchDriverType.REDIS, opts.driverOverride());
		assertEquals("default", opts.queue());
		assertEquals(Duration.ofSeconds(300), opts.delay());
	}
}
