package cn.shopex.ecshopx.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchJobNames;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class InvoicePushOmsJobDispatchPublisherImplTest {

	@Test
	void publish_delegatesToDispatchFacadeWithExpectedJobNameAndOptions() {
		DispatchFacade facade = Mockito.mock(DispatchFacade.class);
		InvoicePushOmsJobDispatchPublisherImpl impl = new InvoicePushOmsJobDispatchPublisherImpl(facade);

		impl.publish(42L, 7L);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		ArgumentCaptor<DispatchOptions> optionsCaptor = ArgumentCaptor.forClass(DispatchOptions.class);
		ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);

		verify(facade).dispatchJob(nameCaptor.capture(), payloadCaptor.capture(), optionsCaptor.capture());

		assertEquals(OrdersDispatchJobNames.INVOICE_PUSH_OMS_JOB, nameCaptor.getValue());
		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals(42L, payload.get("invoice_id"));
		assertEquals(7L, payload.get("company_id"));

		DispatchOptions opts = optionsCaptor.getValue();
		assertEquals(DispatchMode.ASYNC, opts.mode());
		assertEquals(DispatchDriverType.REDIS, opts.driverOverride());
		assertEquals("default", opts.queue());
		assertNull(opts.delay());
	}
}
