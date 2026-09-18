package cn.shopex.ecshopx.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;

import cn.shopex.ecshopx.common.dispatch.DistributionDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributionEditEventDispatchPublisherImplDualPublishFacadeProbeTest {

	@Mock
	private DispatchFacade dispatchFacade;

	@Test
	@SuppressWarnings("unchecked")
	void publish_invokesDispatchFacadePublishEventTwice_with282Then290AndSamePayloadShape() {
		DistributionEditEventDispatchPublisherImpl impl = new DistributionEditEventDispatchPublisherImpl(dispatchFacade);
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("distributor_id", 42L);
		row.put("company_id", 7L);
		impl.publish(row);

		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		var order = inOrder(dispatchFacade);
		order.verify(dispatchFacade)
				.publishEvent(
						eq(DistributionDispatchEventNames.EVENT_DISTRIBUTION_EDIT),
						payloadCaptor.capture(),
						any(DispatchOptions.class));
		Map<String, Object> firstPayload = payloadCaptor.getValue();
		order.verify(dispatchFacade)
				.publishEvent(
						eq(DistributionDispatchEventNames.EVENT_DISTRIBUTION_EDIT_CSV290),
						payloadCaptor.capture(),
						any(DispatchOptions.class));
		Map<String, Object> secondPayload = payloadCaptor.getValue();

		assertSame(firstPayload, secondPayload);
		Map<String, Object> inner = new LinkedHashMap<>(row);
		Map<String, Object> expected = new LinkedHashMap<>();
		expected.put("entities", inner);
		assertEquals(expected, firstPayload);
	}
}
