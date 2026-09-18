package cn.shopex.ecshopx.config;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.HfpayDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class HfpayPopularizeWithdrawEventDispatchPublisherImplTest {

	@Test
	void publish_invokesDispatchFacadeWithEvent270NamesAndPayloadShape() {
		DispatchFacade facade = mock(DispatchFacade.class);
		HfpayPopularizeWithdrawEventDispatchPublisherImpl publisher =
				new HfpayPopularizeWithdrawEventDispatchPublisherImpl(facade);

		publisher.publishPopularizeWithdrawAfterMerchantTradePersist("MT-99");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(facade)
				.publishEvent(
						eq(HfpayDispatchEventNames.EVENT_HFPAY_POPULARIZE_WITHDRAW),
						payloadCaptor.capture(),
						eq(DispatchOptions.eventDefaults()));

		Map<String, Object> payload = payloadCaptor.getValue();
		@SuppressWarnings("unchecked")
		Map<String, Object> entities = (Map<String, Object>) payload.get("entities");
		org.junit.jupiter.api.Assertions.assertEquals(1, entities.size());
		org.junit.jupiter.api.Assertions.assertEquals("MT-99", entities.get("merchant_trade_id"));
	}
}
