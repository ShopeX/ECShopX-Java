package cn.shopex.ecshopx.youshu.dispatch;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.youshu.service.YoushuNormalOrderDeliverySrDataSyncService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NormalOrderDeliveryYoushuDispatchListenerTest {

	@Test
	void onEvent_invokesSyncServiceWithCompanyAndOrder() {
		YoushuNormalOrderDeliverySrDataSyncService sync = mock(YoushuNormalOrderDeliverySrDataSyncService.class);
		NormalOrderDeliveryYoushuDispatchListener listener = new NormalOrderDeliveryYoushuDispatchListener(sync);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 7L);
		payload.put("order_id", 99L);

		listener.onEvent(payload);

		verify(sync).syncOrderAfterNormalDelivery(7L, 99L);
	}

	@Test
	void onEvent_throwsBadRequestWhenMissingKeys() {
		YoushuNormalOrderDeliverySrDataSyncService sync = mock(YoushuNormalOrderDeliverySrDataSyncService.class);
		NormalOrderDeliveryYoushuDispatchListener listener = new NormalOrderDeliveryYoushuDispatchListener(sync);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("order_id", 1L);

		assertThrows(BadRequestException.class, () -> listener.onEvent(payload));
		verifyNoInteractions(sync);
	}

	@Test
	void onEvent_whenMissingOrderId_throwsBadRequest() {
		YoushuNormalOrderDeliverySrDataSyncService sync = mock(YoushuNormalOrderDeliverySrDataSyncService.class);
		NormalOrderDeliveryYoushuDispatchListener listener = new NormalOrderDeliveryYoushuDispatchListener(sync);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);

		assertThrows(BadRequestException.class, () -> listener.onEvent(payload));
		verifyNoInteractions(sync);
	}
}
