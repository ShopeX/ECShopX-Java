package cn.shopex.ecshopx.orders.dispatch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderPassRefundService;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrdersNormalOrderCancelAutoPassDispatchListenerTest {

	@Mock
	AdminOrderPassRefundService adminOrderPassRefundService;

	@Mock
	AftersalesRefundService aftersalesRefundService;

	@Mock
	OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;

	@Mock
	NormalOrdersMapper normalOrdersMapper;

	@InjectMocks
	OrdersNormalOrderCancelAutoPassDispatchListener listener;

	@Test
	@DisplayName("normal_order_cancel listener: gates pass delegates passRefund once")
	void onEvent_whenGatesPass_invokesPassRefundOnce() {
		long companyId = 7L;
		long orderId = 99L;
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", companyId);
		payload.put("order_id", orderId);
		payload.put("source", "admin_normal_order_full_cancel");

		Map<String, Object> platform = new HashMap<>();
		platform.put("auto_aftersales", Boolean.TRUE);
		when(orderValiditySettingRedisReadService.readPlatformSetting(companyId)).thenReturn(platform);

		NormalOrders order = new NormalOrders();
		order.setCompanyId(companyId);
		order.setOrderId(orderId);
		order.setOrderStatus("PAYED");
		order.setCancelStatus("WAIT_PROCESS");
		order.setOrderType("normal");
		order.setSupplierId(0);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);

		AftersalesRefund refund = new AftersalesRefund();
		refund.setRefundStatus("READY");
		refund.setSupplierId(0L);
		when(aftersalesRefundService.listReadyCancelRefundsAsc(companyId, orderId, 0L))
				.thenReturn(List.of(refund))
				.thenReturn(List.of());

		listener.onEvent(payload);

		verify(adminOrderPassRefundService, times(1)).passRefund(anyMap(), eq(refund), anyMap());
	}

	@Test
	@DisplayName("auto pass: 两张 READY 依次 passRefund 两次")
	void onEvent_twoReadyRefunds_passesBothInAscOrder() {
		long companyId = 7L;
		long orderId = 99L;
		Map<String, Object> payload = Map.of("company_id", companyId, "order_id", orderId);

		Map<String, Object> platform = new HashMap<>();
		platform.put("auto_aftersales", Boolean.TRUE);
		when(orderValiditySettingRedisReadService.readPlatformSetting(companyId)).thenReturn(platform);

		NormalOrders order = new NormalOrders();
		order.setOrderStatus("PAYED");
		order.setCancelStatus("WAIT_PROCESS");
		order.setOrderType("normal");
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);

		AftersalesRefund r1 = new AftersalesRefund();
		r1.setRefundBn(1L);
		r1.setRefundStatus("READY");
		r1.setSupplierId(101L);
		AftersalesRefund r2 = new AftersalesRefund();
		r2.setRefundBn(2L);
		r2.setRefundStatus("READY");
		r2.setSupplierId(102L);
		when(aftersalesRefundService.listReadyCancelRefundsAsc(companyId, orderId, 0L))
				.thenReturn(List.of(r1, r2))
				.thenReturn(List.of(r2))
				.thenReturn(List.of());

		listener.onEvent(payload);

		verify(adminOrderPassRefundService, times(2)).passRefund(anyMap(), any(), anyMap());
		verify(adminOrderPassRefundService).passRefund(anyMap(), eq(r1), anyMap());
		verify(adminOrderPassRefundService).passRefund(anyMap(), eq(r2), anyMap());
	}

	@Test
	@DisplayName("normal_order_cancel listener: auto_aftersales off skips passRefund")
	void onEvent_whenAutoAftersalesDisabled_neverCallsPassRefund() {
		long companyId = 7L;
		Map<String, Object> payload = Map.of("company_id", companyId, "order_id", 1L);
		Map<String, Object> platform = new HashMap<>();
		platform.put("auto_aftersales", Boolean.FALSE);
		when(orderValiditySettingRedisReadService.readPlatformSetting(companyId)).thenReturn(platform);

		listener.onEvent(payload);

		verify(adminOrderPassRefundService, never()).passRefund(anyMap(), any(), anyMap());
		verify(normalOrdersMapper, never()).selectOne(any());
	}
}
