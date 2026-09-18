package cn.shopex.ecshopx.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchJobNames;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class GenerateStatementsJobDispatchPublisherImplTest {

	@Test
	void publish_delegatesToDispatchFacadeWithExpectedJobNameQueueAndPayload() {
		DispatchFacade facade = Mockito.mock(DispatchFacade.class);
		GenerateStatementsJobDispatchPublisherImpl impl =
				new GenerateStatementsJobDispatchPublisherImpl(facade);

		impl.publish(7L, 3L, 0L, 2, "week", 99L, "distributor");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		ArgumentCaptor<DispatchOptions> optionsCaptor = ArgumentCaptor.forClass(DispatchOptions.class);
		ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);

		verify(facade).dispatchJob(nameCaptor.capture(), payloadCaptor.capture(), optionsCaptor.capture());

		assertEquals(OrdersDispatchJobNames.GENERATE_STATEMENTS_JOB, nameCaptor.getValue());
		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals(7L, payload.get("company_id"));
		assertEquals(3L, payload.get("distributor_id"));
		assertEquals(0L, payload.get("supplier_id"));
		assertEquals(List.of(2, "week"), payload.get("period"));
		assertEquals(99L, payload.get("last_end_time"));
		assertEquals("distributor", payload.get("merchant_type"));

		DispatchOptions opts = optionsCaptor.getValue();
		assertEquals(DispatchMode.ASYNC, opts.mode());
		assertEquals(DispatchDriverType.REDIS, opts.driverOverride());
		assertEquals("slow", opts.queue());
		assertNull(opts.delay());
	}
}
