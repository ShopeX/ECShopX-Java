package cn.shopex.ecshopx.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.PromotionsDispatchJobNames;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class CancelSeckillPlatTicketJobDispatchPublisherImplTest {

	@Test
	void enqueue_dispatchesJobWithSeckillQueueAnd300sDelay() {
		DispatchFacade facade = Mockito.mock(DispatchFacade.class);
		CancelSeckillPlatTicketJobDispatchPublisherImpl impl = new CancelSeckillPlatTicketJobDispatchPublisherImpl(facade);

		impl.enqueueCancelSeckillPlatTicket("ticket-k", "seckill-k", "store_7", 4, "1001");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		ArgumentCaptor<DispatchOptions> optionsCaptor = ArgumentCaptor.forClass(DispatchOptions.class);
		ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);

		verify(facade).dispatchJob(nameCaptor.capture(), payloadCaptor.capture(), optionsCaptor.capture());

		assertEquals(PromotionsDispatchJobNames.CANCEL_SECKILL_PLAT_TICKET, nameCaptor.getValue());
		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals("ticket-k", payload.get("ticketkey"));
		assertEquals("seckill-k", payload.get("seckillkey"));
		assertEquals("store_7", payload.get("productkey"));
		assertEquals(4, payload.get("num"));
		assertEquals("1001", payload.get("userId"));

		DispatchOptions opts = optionsCaptor.getValue();
		assertEquals(DispatchMode.ASYNC, opts.mode());
		assertEquals(DispatchDriverType.REDIS, opts.driverOverride());
		assertEquals("seckill", opts.queue());
		assertEquals(Duration.ofSeconds(300), opts.delay());
	}
}
