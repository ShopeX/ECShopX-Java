package cn.shopex.ecshopx.dispatch.integration;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.HfpayDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HfpayRefundSuccessEventPublishPortImplTest {

	@Test
	@DisplayName("event:294: publish delegates to DispatchFacade with sync event defaults")
	void publish_delegatesToDispatchFacade_withEvent294AndSyncDefaults() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		HfpayRefundSuccessEventPublishPortImpl port = new HfpayRefundSuccessEventPublishPortImpl(dispatchFacade);
		port.publishSyncOnGatewayRefundSuccess("62001", 52001L);
		verify(dispatchFacade)
				.publishEvent(
						eq(HfpayDispatchEventNames.EVENT_HFPAY_REFUND_SUCCESS),
						argThat(
								m ->
										m != null
												&& "62001".equals(String.valueOf(m.get("order_id")))
												&& m.get("refund_bn") instanceof Long
												&& ((Long) m.get("refund_bn")) == 52001L),
						eq(DispatchOptions.eventDefaults()));
	}
}
