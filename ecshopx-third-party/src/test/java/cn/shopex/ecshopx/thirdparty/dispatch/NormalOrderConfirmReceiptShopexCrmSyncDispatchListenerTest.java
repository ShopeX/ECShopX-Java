package cn.shopex.ecshopx.thirdparty.dispatch;

import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.thirdparty.service.shopexcrm.NormalOrderConfirmReceiptShopexCrmSyncExecutionService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NormalOrderConfirmReceiptShopexCrmSyncDispatchListenerTest {

	@Mock
	private NormalOrderConfirmReceiptShopexCrmSyncExecutionService executionService;

	private NormalOrderConfirmReceiptShopexCrmSyncDispatchListener listener;

	@BeforeEach
	void setUp() {
		listener = new NormalOrderConfirmReceiptShopexCrmSyncDispatchListener(executionService);
	}

	@Test
	void onEvent_delegatesToExecutionService() {
		Map<String, Object> payload = Map.of("company_id", 7L, "order_id", 99L);
		listener.onEvent(payload);
		verify(executionService).executeAfterNormalOrderConfirmReceipt(payload);
	}
}
