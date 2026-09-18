package cn.shopex.ecshopx.orders.service.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.RefundFreightAutoZyEventDispatchPublisher;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.service.setting.OrdersErpSettingRedisReadService;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisWriteService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminOrderValiditySettingSetServiceRefundFreightZyDispatchPublishProbeTest {

	@Mock
	private RefundFreightAutoZyEventDispatchPublisher refundFreightAutoZyEventDispatchPublisher;

	@Mock
	private OrderValiditySettingRedisWriteService orderValiditySettingRedisWriteService;

	@Mock
	private OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private OrdersErpSettingRedisReadService ordersErpSettingRedisReadService;

	private AdminOrderValiditySettingSetService service;

	@BeforeEach
	void setUp() {
		service = new AdminOrderValiditySettingSetService(
				orderValiditySettingRedisWriteService,
				orderValiditySettingRedisReadService,
				normalOrdersMapper,
				ordersErpSettingRedisReadService,
				refundFreightAutoZyEventDispatchPublisher);
	}

	@Test
	void setOrderSetting_whenIsRefundFreightOne_publishesBeforeRedisWriteAndSkipsDirectMapper() {
		long companyId = 10L;
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("is_refund_freight", 1);

		when(orderValiditySettingRedisReadService.readPlatformSetting(companyId)).thenReturn(Map.of());

		service.setOrderSetting(companyId, merged);

		InOrder inOrder = inOrder(refundFreightAutoZyEventDispatchPublisher, orderValiditySettingRedisWriteService);
		inOrder.verify(refundFreightAutoZyEventDispatchPublisher).publish(eq(companyId), eq(1));
		inOrder.verify(orderValiditySettingRedisWriteService).writeCompanySettingJson(eq(companyId), any());
	}

	@Test
	void setOrderSetting_whenIsRefundFreightZero_doesNotPublish() {
		long companyId = 11L;
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("is_refund_freight", 0);

		when(orderValiditySettingRedisReadService.readPlatformSetting(companyId)).thenReturn(Map.of());

		service.setOrderSetting(companyId, merged);

		verify(refundFreightAutoZyEventDispatchPublisher, never()).publish(anyLong(), anyInt());
	}
}
