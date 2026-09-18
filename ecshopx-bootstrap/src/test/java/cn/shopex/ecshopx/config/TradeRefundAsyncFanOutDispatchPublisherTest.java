package cn.shopex.ecshopx.config;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.SystemLinkDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TradeRefund async fan-out publisher (partial-cancel path envelope)")
class TradeRefundAsyncFanOutDispatchPublisherTest {

	@Test
	void publish_delegatesToDispatchFacadeWithAsyncRedisEnvelope() {
		DispatchFacade facade = mock(DispatchFacade.class);
		TradeRefundAsyncFanOutDispatchPublisher publisher = new TradeRefundAsyncFanOutDispatchPublisher(facade);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);
		payload.put("order_id", 2L);
		payload.put("aftersales_bn", 2026050711111111L);

		publisher.publish(payload);

		verify(facade)
				.publishEvent(
						eq(SystemLinkDispatchEventNames.EVENT_TRADE_REFUND),
						eq(payload),
						argThat(
								opts -> {
									DispatchOptions d = (DispatchOptions) opts;
									return d.mode() == DispatchMode.ASYNC
											&& d.driverOverride() == DispatchDriverType.REDIS
											&& d.queue() == null
											&& d.delay() == null;
								}));
	}
}
