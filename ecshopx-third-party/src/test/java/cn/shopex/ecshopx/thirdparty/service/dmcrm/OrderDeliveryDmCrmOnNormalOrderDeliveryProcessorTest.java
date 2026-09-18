package cn.shopex.ecshopx.thirdparty.service.dmcrm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderDeliveryDmCrmOnNormalOrderDeliveryProcessorTest {

	@Mock
	private DmCrmSettingReadPort dmCrmSettingReadPort;

	@Mock
	private DmCrmNormalOrderDeliverySyncPort dmCrmNormalOrderDeliverySyncPort;

	private OrderDeliveryDmCrmOnNormalOrderDeliveryProcessor processor;

	@BeforeEach
	void setUp() {
		processor = new OrderDeliveryDmCrmOnNormalOrderDeliveryProcessor(dmCrmSettingReadPort, dmCrmNormalOrderDeliverySyncPort);
	}

	@Test
	void handle_whenCompanyIdMissing_noSyncCall() {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("order_id", 1L);
		processor.handle(payload);
		verify(dmCrmNormalOrderDeliverySyncPort, never()).syncNormalOrderDelivery(anyLong(), anyLong(), any());
	}

	@Test
	void handle_whenOrderIdMissing_noSyncCall() {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 7L);
		processor.handle(payload);
		verify(dmCrmNormalOrderDeliverySyncPort, never()).syncNormalOrderDelivery(anyLong(), anyLong(), any());
	}

	@Test
	void handle_whenPointIntegrationClosed_noSyncCall() {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 7L);
		payload.put("order_id", 99L);
		when(dmCrmSettingReadPort.isPointIntegrationOpen(anyLong())).thenReturn(false);
		processor.handle(payload);
		verify(dmCrmNormalOrderDeliverySyncPort, never()).syncNormalOrderDelivery(anyLong(), anyLong(), any());
	}

	@Test
	void handle_whenOpen_invokesSyncNormalOrderDeliveryOnce() {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 7L);
		payload.put("order_id", 99L);
		when(dmCrmSettingReadPort.isPointIntegrationOpen(7L)).thenReturn(true);
		processor.handle(payload);
		verify(dmCrmNormalOrderDeliverySyncPort, times(1)).syncNormalOrderDelivery(eq(7L), eq(99L), eq(payload));
	}
}
