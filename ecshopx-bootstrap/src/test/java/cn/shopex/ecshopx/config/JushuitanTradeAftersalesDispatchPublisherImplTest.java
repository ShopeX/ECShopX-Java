package cn.shopex.ecshopx.config;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.SystemLinkDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JushuitanTradeAftersalesDispatchPublisherImplTest {

	@Test
	void publish_delegatesToDispatchFacadeWithEventEnvelope() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		JushuitanTradeAftersalesDispatchPublisherImpl impl = new JushuitanTradeAftersalesDispatchPublisherImpl(dispatchFacade);
		Map<String, Object> payload = Map.of("company_id", 42L, "order_id", 7L);

		impl.publish(payload);

		verify(dispatchFacade)
				.publishEvent(
						eq(SystemLinkDispatchEventNames.EVENT_JUSHUITAN_TRADE_AFTERSALES),
						eq(payload),
						argThat(
								opts ->
										opts != null
												&& opts.mode() == DispatchMode.SYNC
												&& opts.driverOverride() == null
												&& opts.queue() == null
												&& opts.delay() == null));
	}
}
