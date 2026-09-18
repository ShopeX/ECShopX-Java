package cn.shopex.ecshopx.youshu.dispatch;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.youshu.service.YoushuNormalOrderCancelSrDataSyncService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NormalOrderCancelYoushuDispatchListenerTest {

	@Test
	void onEvent_invokes_sync_service_with_company_and_order_ids() {
		YoushuNormalOrderCancelSrDataSyncService sync = mock(YoushuNormalOrderCancelSrDataSyncService.class);
		NormalOrderCancelYoushuDispatchListener listener = new NormalOrderCancelYoushuDispatchListener(sync);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 7L);
		payload.put("order_id", 99L);
		payload.put("source", "admin_normal_order_full_cancel");

		listener.onEvent(payload);

		verify(sync).syncOrderAfterNormalCancel(7L, 99L);
	}

	@Test
	void onEvent_throws_bad_request_when_company_id_missing() {
		YoushuNormalOrderCancelSrDataSyncService sync = mock(YoushuNormalOrderCancelSrDataSyncService.class);
		NormalOrderCancelYoushuDispatchListener listener = new NormalOrderCancelYoushuDispatchListener(sync);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("order_id", 1L);

		assertThrows(BadRequestException.class, () -> listener.onEvent(payload));
		verifyNoInteractions(sync);
	}

	@Test
	void onEvent_throws_bad_request_when_order_id_missing() {
		YoushuNormalOrderCancelSrDataSyncService sync = mock(YoushuNormalOrderCancelSrDataSyncService.class);
		NormalOrderCancelYoushuDispatchListener listener = new NormalOrderCancelYoushuDispatchListener(sync);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);

		assertThrows(BadRequestException.class, () -> listener.onEvent(payload));
		verifyNoInteractions(sync);
	}
}
