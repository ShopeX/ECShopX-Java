package cn.shopex.ecshopx.config;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.SystemLinkDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Default {@link TradeRefundDispatchPublisherImpl} remains SYNC {@link DispatchOptions#eventDefaults()}
 * for shared injections (Admin cancel, wxapp, apply flows); partial cancel uses {@link
 * TradeRefundAsyncFanOutDispatchPublisher} instead.
 */
@DisplayName("TradeRefund default publisher: unchanged SYNC event defaults")
class TradeRefundDispatchPublisherImplTest {

	@Test
	void publish_delegatesToDispatchFacadeWithSyncEventDefaults() {
		DispatchFacade facade = mock(DispatchFacade.class);
		TradeRefundDispatchPublisherImpl impl = new TradeRefundDispatchPublisherImpl(facade);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 9L);
		payload.put("order_id", 100L);

		impl.publish(payload);

		verify(facade)
				.publishEvent(
						eq(SystemLinkDispatchEventNames.EVENT_TRADE_REFUND),
						eq(payload),
						argThat(
								opts -> {
									DispatchOptions d = (DispatchOptions) opts;
									return d.mode() == DispatchMode.SYNC
											&& d.driverOverride() == null
											&& d.queue() == null
											&& d.delay() == null;
								}));
	}
}
