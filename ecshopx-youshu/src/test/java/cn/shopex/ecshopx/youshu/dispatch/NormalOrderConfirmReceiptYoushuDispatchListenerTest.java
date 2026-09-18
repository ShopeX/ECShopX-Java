package cn.shopex.ecshopx.youshu.dispatch;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.youshu.service.YoushuNormalOrderConfirmReceiptSrDataSyncService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NormalOrderConfirmReceiptYoushuDispatchListenerTest {

	@Mock
	private YoushuNormalOrderConfirmReceiptSrDataSyncService sync;

	private NormalOrderConfirmReceiptYoushuDispatchListener listener;

	@BeforeEach
	void setUp() {
		listener = new NormalOrderConfirmReceiptYoushuDispatchListener(sync);
	}

	@Test
	void onEvent_invokesSyncService_withCompanyAndOrderFromPayload() {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 7L);
		payload.put("order_id", 99L);

		listener.onEvent(payload);

		verify(sync).syncOrderAfterNormalOrderConfirmReceipt(7L, 99L);
	}

	@Test
	void onEvent_throwsBadRequest_whenMissingCompanyOrOrder() {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("order_id", 1L);

		assertThrows(BadRequestException.class, () -> listener.onEvent(payload));
		verifyNoInteractions(sync);
	}

	@Test
	void onEvent_whenMissingOrderId_throwsBadRequest() {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 1L);

		assertThrows(BadRequestException.class, () -> listener.onEvent(payload));
		verifyNoInteractions(sync);
	}
}
