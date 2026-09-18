package cn.shopex.ecshopx.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchJobNames;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class InvoiceQueryJobDispatchPublisherImplTest {

	@Test
	void publish_delegatesToDispatchFacadeWithInvoiceQueryJobNameAndSlowQueue() {
		DispatchFacade facade = Mockito.mock(DispatchFacade.class);
		InvoiceQueryJobDispatchPublisherImpl impl = new InvoiceQueryJobDispatchPublisherImpl(facade);

		Map<String, Object> jobData = new LinkedHashMap<>();
		jobData.put("invoice_id", 3L);
		jobData.put("company_id", 9L);
		jobData.put("order_id", "O1");
		jobData.put("invoice_apply_bn", "BN1");

		impl.publish(jobData);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		ArgumentCaptor<DispatchOptions> optionsCaptor = ArgumentCaptor.forClass(DispatchOptions.class);
		ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);

		verify(facade).dispatchJob(nameCaptor.capture(), payloadCaptor.capture(), optionsCaptor.capture());

		assertEquals(OrdersDispatchJobNames.INVOICE_QUERY_JOB, nameCaptor.getValue());
		assertEquals(3L, payloadCaptor.getValue().get("invoice_id"));
		assertEquals(9L, payloadCaptor.getValue().get("company_id"));

		DispatchOptions opts = optionsCaptor.getValue();
		assertEquals(DispatchMode.ASYNC, opts.mode());
		assertEquals(DispatchDriverType.REDIS, opts.driverOverride());
		assertEquals("slow", opts.queue());
		assertNull(opts.delay());
		assertEquals(RetryPolicy.platformDefault(), opts.retryPolicy());
	}
}
