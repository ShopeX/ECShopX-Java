package cn.shopex.ecshopx.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchJobNames;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class ConsumptionOrderJobDispatchPublisherImplTest {

	@Test
	void publish_delegatesToDispatchFacadeWithConsumptionOrderJobNameAndSlowQueue() {
		DispatchFacade facade = Mockito.mock(DispatchFacade.class);
		ConsumptionOrderJobDispatchPublisherImpl impl = new ConsumptionOrderJobDispatchPublisherImpl(facade);

		Map<String, Object> jobData = Map.of("orderType", "normal", "pageSize", "100");

		impl.publish(jobData);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		ArgumentCaptor<DispatchOptions> optionsCaptor = ArgumentCaptor.forClass(DispatchOptions.class);
		ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);

		verify(facade).dispatchJob(nameCaptor.capture(), payloadCaptor.capture(), optionsCaptor.capture());

		assertEquals(OrdersDispatchJobNames.CONSUMPTION_ORDER_JOB, nameCaptor.getValue());
		assertEquals("normal", payloadCaptor.getValue().get("orderType"));
		assertEquals("100", payloadCaptor.getValue().get("pageSize"));

		DispatchOptions opts = optionsCaptor.getValue();
		assertEquals(DispatchMode.ASYNC, opts.mode());
		assertEquals(DispatchDriverType.REDIS, opts.driverOverride());
		assertEquals("slow", opts.queue());
		assertNull(opts.delay());
		assertEquals(RetryPolicy.platformDefault(), opts.retryPolicy());
	}

	@Test
	void publish_rejectsNullJobData() {
		DispatchFacade facade = Mockito.mock(DispatchFacade.class);
		ConsumptionOrderJobDispatchPublisherImpl impl = new ConsumptionOrderJobDispatchPublisherImpl(facade);
		assertThrows(IllegalArgumentException.class, () -> impl.publish(null));
	}
}
