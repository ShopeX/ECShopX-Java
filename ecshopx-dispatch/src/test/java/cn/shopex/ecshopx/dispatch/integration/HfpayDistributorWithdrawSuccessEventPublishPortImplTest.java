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
class HfpayDistributorWithdrawSuccessEventPublishPortImplTest {

	@Test
	@DisplayName("event:296: publish delegates to DispatchFacade with entities payload shape")
	void publish_invokesDispatchFacadeWithEvent296AndPayloadShape() {
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		HfpayDistributorWithdrawSuccessEventPublishPortImpl port =
				new HfpayDistributorWithdrawSuccessEventPublishPortImpl(dispatchFacade);
		port.publishSyncAfterScheduleWithdrawSuccess(10L, 7L, 3L, 1000, "ORD-X");
		verify(dispatchFacade)
				.publishEvent(
						eq(HfpayDispatchEventNames.EVENT_HFPAY_DISTRIBUTOR_WITHDRAW_SUCCESS),
						argThat(
								m ->
										m != null
												&& m.get("entities") instanceof Map<?, ?> ent
												&& Long.valueOf(10L).equals(toLong(ent.get("hfpay_cash_record_id")))
												&& Long.valueOf(7L).equals(toLong(ent.get("company_id")))
												&& Long.valueOf(3L).equals(toLong(ent.get("distributor_id")))
												&& Integer.valueOf(1000).equals(toInt(ent.get("trans_amt")))
												&& "ORD-X".equals(String.valueOf(ent.get("order_id")))),
						eq(DispatchOptions.eventDefaults()));
	}

	private static Long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return null;
	}

	private static Integer toInt(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		return null;
	}
}
