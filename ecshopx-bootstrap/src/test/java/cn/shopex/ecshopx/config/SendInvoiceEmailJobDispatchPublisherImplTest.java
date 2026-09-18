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

class SendInvoiceEmailJobDispatchPublisherImplTest {

	@Test
	void publish_delegatesToDispatchFacadeWithExpectedJobNameAndOptions() {
		DispatchFacade facade = Mockito.mock(DispatchFacade.class);
		SendInvoiceEmailJobDispatchPublisherImpl impl = new SendInvoiceEmailJobDispatchPublisherImpl(facade);

		impl.publish("u@x.com", "https://host/path", 7L);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		ArgumentCaptor<DispatchOptions> optionsCaptor = ArgumentCaptor.forClass(DispatchOptions.class);
		ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);

		verify(facade).dispatchJob(nameCaptor.capture(), payloadCaptor.capture(), optionsCaptor.capture());

		assertEquals(OrdersDispatchJobNames.SEND_INVOICE_EMAIL_JOB, nameCaptor.getValue());
		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals("u@x.com", payload.get("email"));
		assertEquals("https://host/path", payload.get("invoice_file_url"));
		assertEquals(7L, payload.get("company_id"));
		assertNull(payload.get("subject"));

		DispatchOptions opts = optionsCaptor.getValue();
		assertEquals(DispatchMode.ASYNC, opts.mode());
		assertEquals(DispatchDriverType.REDIS, opts.driverOverride());
		assertEquals("default", opts.queue());
		assertNull(opts.delay());
	}
}
