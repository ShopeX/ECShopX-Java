package cn.shopex.ecshopx.youshu.dispatch;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.youshu.service.YoushuNormalOrderAddSrDataSyncService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NormalOrderAddYoushuDispatchListenerTest {

	@Test
	void onEvent_invokesSyncServiceWithCompanyAndOrder() {
		YoushuNormalOrderAddSrDataSyncService sync = mock(YoushuNormalOrderAddSrDataSyncService.class);
		NormalOrderAddYoushuDispatchListener listener = new NormalOrderAddYoushuDispatchListener(sync);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 7L);
		payload.put("order_id", 99L);
		payload.put("pay_type", "wxpay");

		listener.onEvent(payload);

		verify(sync).syncOrderAfterNormalAdd(7L, 99L);
	}

	@Test
	void onEvent_whenMissingCompanyId_throwsBadRequest() {
		YoushuNormalOrderAddSrDataSyncService sync = mock(YoushuNormalOrderAddSrDataSyncService.class);
		NormalOrderAddYoushuDispatchListener listener = new NormalOrderAddYoushuDispatchListener(sync);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("order_id", 1L);

		assertThrows(BadRequestException.class, () -> listener.onEvent(payload));
		verifyNoInteractions(sync);
	}

	@Test
	void onEvent_whenMissingOrderId_throwsBadRequest() {
		YoushuNormalOrderAddSrDataSyncService sync = mock(YoushuNormalOrderAddSrDataSyncService.class);
		NormalOrderAddYoushuDispatchListener listener = new NormalOrderAddYoushuDispatchListener(sync);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);

		assertThrows(BadRequestException.class, () -> listener.onEvent(payload));
		verifyNoInteractions(sync);
	}
}
