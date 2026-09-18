package cn.shopex.ecshopx.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import cn.shopex.ecshopx.common.dispatch.HfpayDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class HfpayProfitSharingEventDispatchPublisherImpl267And295PublishTest {

	@Test
	void publishProfitSharingAfterCommit_invokesPublishEventFor267Then295WithSamePayloadShape() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		HfpayProfitSharingEventDispatchPublisherImpl publisher =
				new HfpayProfitSharingEventDispatchPublisherImpl(dispatchFacade);

		publisher.publishProfitSharingAfterCommit(100L, List.of(9L));

		var inOrder = inOrder(dispatchFacade);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> cap267 = ArgumentCaptor.forClass(Map.class);
		inOrder
				.verify(dispatchFacade)
				.publishEvent(
						eq(HfpayDispatchEventNames.EVENT_HFPAY_PROFIT_SHARING),
						cap267.capture(),
						eq(DispatchOptions.eventDefaults()));
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> cap295 = ArgumentCaptor.forClass(Map.class);
		inOrder
				.verify(dispatchFacade)
				.publishEvent(
						eq(HfpayDispatchEventNames.EVENT_HFPAY_PROFIT_SHARING_TRADE_RECORD),
						cap295.capture(),
						eq(DispatchOptions.eventDefaults()));
		inOrder.verifyNoMoreInteractions();

		assertPayloadShape(cap267.getValue(), 100L, List.of(9L));
		assertPayloadShape(cap295.getValue(), 100L, List.of(9L));
	}

	private static void assertPayloadShape(Map<String, Object> payload, long orderId, List<Long> sharingIds) {
		Object entObj = payload.get("entities");
		assertInstanceOf(Map.class, entObj);
		@SuppressWarnings("unchecked")
		Map<String, Object> entities = (Map<String, Object>) entObj;
		assertEquals(orderId, ((Number) entities.get("order_id")).longValue());
		Object listObj = entities.get("order_profit_sharing_id");
		assertInstanceOf(List.class, listObj);
		@SuppressWarnings("unchecked")
		List<?> idList = (List<?>) listObj;
		List<Long> normalized = new ArrayList<>();
		for (Object o : idList) {
			normalized.add(((Number) o).longValue());
		}
		assertEquals(sharingIds, normalized);
	}
}
