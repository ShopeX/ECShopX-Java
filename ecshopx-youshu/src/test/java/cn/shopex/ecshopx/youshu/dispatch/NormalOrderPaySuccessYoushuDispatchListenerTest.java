package cn.shopex.ecshopx.youshu.dispatch;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.youshu.service.YoushuNormalOrderPaySuccessSrDataSyncService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NormalOrderPaySuccessYoushuDispatchListenerTest {

	@Test
	void onEvent_invokesSyncServiceWithCompanyAndOrder() {
		YoushuNormalOrderPaySuccessSrDataSyncService sync = mock(YoushuNormalOrderPaySuccessSrDataSyncService.class);
		NormalOrderPaySuccessYoushuDispatchListener listener = new NormalOrderPaySuccessYoushuDispatchListener(sync);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 7L);
		payload.put("order_id", 99L);
		payload.put("pay_type", "alipay");

		listener.onEvent(payload);

		verify(sync).syncOrderAfterNormalPaySuccess(7L, 99L);
	}

	@Test
	void onEvent_throwsBadRequestWhenMissingKeys() {
		YoushuNormalOrderPaySuccessSrDataSyncService sync = mock(YoushuNormalOrderPaySuccessSrDataSyncService.class);
		NormalOrderPaySuccessYoushuDispatchListener listener = new NormalOrderPaySuccessYoushuDispatchListener(sync);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("order_id", 1L);

		assertThrows(BadRequestException.class, () -> listener.onEvent(payload));
		verifyNoInteractions(sync);
	}

	@Test
	void onEvent_whenMissingOrderId_throwsBadRequest() {
		YoushuNormalOrderPaySuccessSrDataSyncService sync = mock(YoushuNormalOrderPaySuccessSrDataSyncService.class);
		NormalOrderPaySuccessYoushuDispatchListener listener = new NormalOrderPaySuccessYoushuDispatchListener(sync);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);

		assertThrows(BadRequestException.class, () -> listener.onEvent(payload));
		verifyNoInteractions(sync);
	}
}
