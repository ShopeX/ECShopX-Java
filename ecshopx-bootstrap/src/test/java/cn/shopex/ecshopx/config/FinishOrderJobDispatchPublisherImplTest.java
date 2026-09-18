package cn.shopex.ecshopx.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

class FinishOrderJobDispatchPublisherImplTest {

	@Test
	void publish_delegatesToDispatchFacadeWithFinishOrderJobNameAndSlowQueue() {
		DispatchFacade facade = Mockito.mock(DispatchFacade.class);
		FinishOrderJobDispatchPublisherImpl impl = new FinishOrderJobDispatchPublisherImpl(facade);

		Map<String, Object> jobData = Map.of();

		impl.publish(jobData);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		ArgumentCaptor<DispatchOptions> optionsCaptor = ArgumentCaptor.forClass(DispatchOptions.class);
		ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);

		verify(facade).dispatchJob(nameCaptor.capture(), payloadCaptor.capture(), optionsCaptor.capture());

		assertEquals(OrdersDispatchJobNames.FINISH_ORDER_JOB, nameCaptor.getValue());
		assertTrue(payloadCaptor.getValue().isEmpty());

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
		FinishOrderJobDispatchPublisherImpl impl = new FinishOrderJobDispatchPublisherImpl(facade);
		assertThrows(IllegalArgumentException.class, () -> impl.publish(null));
	}
}
