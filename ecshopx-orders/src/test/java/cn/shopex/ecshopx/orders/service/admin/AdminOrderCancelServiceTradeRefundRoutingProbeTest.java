package cn.shopex.ecshopx.orders.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class AdminOrderCancelServiceTradeRefundRoutingProbeTest {

	@Test
	void cancelOrder_whenDeliveryPending_delegatesToAdminNormalOrderFullCancelServiceOnce() {
		OrderAssociationsMapper orderAssociationsMapper = mock(OrderAssociationsMapper.class);
		SupplierOrderMapper supplierOrderMapper = mock(SupplierOrderMapper.class);
		OrderCancelReasonConfig orderCancelReasonConfig = new OrderCancelReasonConfig();
		AdminNormalOrderFullCancelService full = mock(AdminNormalOrderFullCancelService.class);
		AdminNormalOrderPartialCancelService partial = mock(AdminNormalOrderPartialCancelService.class);
		AdminOrderCancelService svc =
				new AdminOrderCancelService(
						orderAssociationsMapper,
						supplierOrderMapper,
						orderCancelReasonConfig,
						full,
						partial,
						mock(PlatformSelfSubCancelSupport.class));

		OrderAssociations assoc = new OrderAssociations();
		assoc.setCompanyId(1L);
		assoc.setOrderId(10L);
		assoc.setUserId(99L);
		assoc.setMobile("13900000000");
		assoc.setOrderType("normal");
		assoc.setDeliveryStatus("PENDING");
		when(orderAssociationsMapper.selectOne(any())).thenReturn(assoc);

		Map<String, Object> req = new LinkedHashMap<>();
		req.put("cancel_reason", 1);
		when(full.execute(
						eq(1L),
						eq("shop"),
						eq(2L),
						eq(0L),
						eq(99L),
						eq("13900000000"),
						eq(10L),
						anyString(),
						anyMap(),
						eq("shop")))
				.thenReturn(Map.of("cancel_id", "55"));

		Map<String, Object> out = svc.cancelOrder(1L, "shop", 2L, "10", req);

		assertThat(out).containsEntry("cancel_id", "55");
		ArgumentCaptor<Map<String, Object>> paramsCaptor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		verify(full, times(1))
				.execute(
						eq(1L),
						eq("shop"),
						eq(2L),
						eq(0L),
						eq(99L),
						eq("13900000000"),
						eq(10L),
						anyString(),
						paramsCaptor.capture(),
						eq("shop"));
		assertThat(paramsCaptor.getValue().get("cancel_from")).isEqualTo("shop");
		assertThat(paramsCaptor.getValue().get("cancel_reason")).isEqualTo(1);
		verify(partial, never()).execute(anyLong(), anyString(), anyLong(), anyLong(), anyLong(), anyLong(), anyString());
	}

	@Test
	void cancelOrder_whenDeliveryPartail_delegatesToAdminNormalOrderPartialCancelServiceOnce() {
		OrderAssociationsMapper orderAssociationsMapper = mock(OrderAssociationsMapper.class);
		SupplierOrderMapper supplierOrderMapper = mock(SupplierOrderMapper.class);
		OrderCancelReasonConfig orderCancelReasonConfig = new OrderCancelReasonConfig();
		AdminNormalOrderFullCancelService full = mock(AdminNormalOrderFullCancelService.class);
		AdminNormalOrderPartialCancelService partial = mock(AdminNormalOrderPartialCancelService.class);
		AdminOrderCancelService svc =
				new AdminOrderCancelService(
						orderAssociationsMapper,
						supplierOrderMapper,
						orderCancelReasonConfig,
						full,
						partial,
						mock(PlatformSelfSubCancelSupport.class));

		OrderAssociations assoc = new OrderAssociations();
		assoc.setCompanyId(1L);
		assoc.setOrderId(20L);
		assoc.setUserId(101L);
		assoc.setMobile("13811112222");
		assoc.setOrderType("normal");
		assoc.setDeliveryStatus("PARTAIL");
		when(orderAssociationsMapper.selectOne(any())).thenReturn(assoc);

		when(partial.execute(eq(1L), eq("shop"), eq(3L), eq(0L), eq(101L), eq(20L), anyString()))
				.thenReturn(Map.of("aftersales_bn", 9001L));

		Map<String, Object> out = svc.cancelOrder(1L, "shop", 3L, "20", Map.of("cancel_reason", 2));

		assertThat(out).containsEntry("aftersales_bn", 9001L);
		verify(partial, times(1)).execute(eq(1L), eq("shop"), eq(3L), eq(0L), eq(101L), eq(20L), anyString());
		verify(full, never())
				.execute(
						anyLong(),
						anyString(),
						anyLong(),
						anyLong(),
						anyLong(),
						anyString(),
						anyLong(),
						anyString(),
						anyMap(),
						anyString());
	}

	@Test
	void cancelOrder_whenMainPartailButSupplierPending_usesFullCancelForSupplier() {
		OrderAssociationsMapper orderAssociationsMapper = mock(OrderAssociationsMapper.class);
		SupplierOrderMapper supplierOrderMapper = mock(SupplierOrderMapper.class);
		OrderCancelReasonConfig orderCancelReasonConfig = new OrderCancelReasonConfig();
		AdminNormalOrderFullCancelService full = mock(AdminNormalOrderFullCancelService.class);
		AdminNormalOrderPartialCancelService partial = mock(AdminNormalOrderPartialCancelService.class);
		AdminOrderCancelService svc =
				new AdminOrderCancelService(
						orderAssociationsMapper,
						supplierOrderMapper,
						orderCancelReasonConfig,
						full,
						partial,
						mock(PlatformSelfSubCancelSupport.class));

		OrderAssociations assoc = new OrderAssociations();
		assoc.setCompanyId(1L);
		assoc.setOrderId(5353005000105097L);
		assoc.setUserId(101L);
		assoc.setMobile("13811112222");
		assoc.setOrderType("normal");
		assoc.setDeliveryStatus("PARTAIL");
		when(orderAssociationsMapper.selectOne(any())).thenReturn(assoc);

		SupplierOrder supplierOrder = new SupplierOrder();
		supplierOrder.setSupplierId(39);
		supplierOrder.setDeliveryStatus("PENDING");
		when(supplierOrderMapper.selectOne(any())).thenReturn(supplierOrder);

		when(full.execute(
						eq(1L),
						eq("supplier"),
						eq(39L),
						eq(39L),
						eq(101L),
						eq("13811112222"),
						eq(5353005000105097L),
						anyString(),
						anyMap(),
						eq("shop")))
				.thenReturn(Map.of("cancel_id", "77"));

		Map<String, Object> out =
				svc.cancelOrder(1L, "supplier", 39L, "5353005000105097", Map.of("cancel_reason", 1));

		assertThat(out).containsEntry("cancel_id", "77");
		verify(full, times(1))
				.execute(
						eq(1L),
						eq("supplier"),
						eq(39L),
						eq(39L),
						eq(101L),
						eq("13811112222"),
						eq(5353005000105097L),
						anyString(),
						anyMap(),
						eq("shop"));
		verify(partial, never()).execute(anyLong(), anyString(), anyLong(), anyLong(), anyLong(), anyLong(), anyString());
	}

	@Test
	void cancelOrder_whenMainPartailWithRemainingUnshipped_usesFullCancelForShop() {
		OrderAssociationsMapper orderAssociationsMapper = mock(OrderAssociationsMapper.class);
		SupplierOrderMapper supplierOrderMapper = mock(SupplierOrderMapper.class);
		OrderCancelReasonConfig orderCancelReasonConfig = new OrderCancelReasonConfig();
		AdminNormalOrderFullCancelService full = mock(AdminNormalOrderFullCancelService.class);
		AdminNormalOrderPartialCancelService partial = mock(AdminNormalOrderPartialCancelService.class);
		PlatformSelfSubCancelSupport platformSupport = mock(PlatformSelfSubCancelSupport.class);
		AdminOrderCancelService svc =
				new AdminOrderCancelService(
						orderAssociationsMapper,
						supplierOrderMapper,
						orderCancelReasonConfig,
						full,
						partial,
						platformSupport);

		OrderAssociations assoc = new OrderAssociations();
		assoc.setCompanyId(1L);
		assoc.setOrderId(5353493000105097L);
		assoc.setUserId(101L);
		assoc.setMobile("13811112222");
		assoc.setOrderType("normal");
		assoc.setDeliveryStatus("PARTAIL");
		when(orderAssociationsMapper.selectOne(any())).thenReturn(assoc);
		when(platformSupport.isShopRemainingFullCancelScope(1L, 5353493000105097L, "PARTAIL"))
				.thenReturn(true);

		when(full.execute(
						eq(1L),
						eq("shop"),
						eq(3L),
						eq(0L),
						eq(101L),
						eq("13811112222"),
						eq(5353493000105097L),
						anyString(),
						anyMap(),
						eq("shop")))
				.thenReturn(Map.of("cancel_id", "91"));

		ArgumentCaptor<Map<String, Object>> paramsCaptor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		Map<String, Object> out =
				svc.cancelOrder(1L, "shop", 3L, "5353493000105097", Map.of("cancel_reason", 1));

		assertThat(out).containsEntry("cancel_id", "91");
		verify(full, times(1))
				.execute(
						eq(1L),
						eq("shop"),
						eq(3L),
						eq(0L),
						eq(101L),
						eq("13811112222"),
						eq(5353493000105097L),
						anyString(),
						paramsCaptor.capture(),
						eq("shop"));
		assertThat(paramsCaptor.getValue())
				.containsEntry(PlatformSelfSubCancelSupport.SHOP_REMAINING_FULL_CANCEL_PARAM, true);
		verify(partial, never()).execute(anyLong(), anyString(), anyLong(), anyLong(), anyLong(), anyLong(), anyString());
	}

	@Test
	void cancelOrder_whenMainPartailButPlatformSelfPending_usesFullCancelForPlatformScope() {
		OrderAssociationsMapper orderAssociationsMapper = mock(OrderAssociationsMapper.class);
		SupplierOrderMapper supplierOrderMapper = mock(SupplierOrderMapper.class);
		OrderCancelReasonConfig orderCancelReasonConfig = new OrderCancelReasonConfig();
		AdminNormalOrderFullCancelService full = mock(AdminNormalOrderFullCancelService.class);
		AdminNormalOrderPartialCancelService partial = mock(AdminNormalOrderPartialCancelService.class);
		PlatformSelfSubCancelSupport platformSupport = mock(PlatformSelfSubCancelSupport.class);
		AdminOrderCancelService svc =
				new AdminOrderCancelService(
						orderAssociationsMapper,
						supplierOrderMapper,
						orderCancelReasonConfig,
						full,
						partial,
						platformSupport);

		OrderAssociations assoc = new OrderAssociations();
		assoc.setCompanyId(1L);
		assoc.setOrderId(5353014000135097L);
		assoc.setUserId(101L);
		assoc.setMobile("13811112222");
		assoc.setOrderType("normal");
		assoc.setDeliveryStatus("PARTAIL");
		when(orderAssociationsMapper.selectOne(any())).thenReturn(assoc);
		when(platformSupport.isShopRemainingFullCancelScope(1L, 5353014000135097L, "PARTAIL")).thenReturn(true);

		when(full.execute(
						eq(1L),
						eq("shop"),
						eq(3L),
						eq(0L),
						eq(101L),
						eq("13811112222"),
						eq(5353014000135097L),
						anyString(),
						anyMap(),
						eq("shop")))
				.thenReturn(Map.of("cancel_id", "88"));

		ArgumentCaptor<Map<String, Object>> paramsCaptor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		Map<String, Object> out =
				svc.cancelOrder(1L, "shop", 3L, "5353014000135097", Map.of("cancel_reason", 1));

		assertThat(out).containsEntry("cancel_id", "88");
		verify(full, times(1))
				.execute(
						eq(1L),
						eq("shop"),
						eq(3L),
						eq(0L),
						eq(101L),
						eq("13811112222"),
						eq(5353014000135097L),
						anyString(),
						paramsCaptor.capture(),
						eq("shop"));
		assertThat(paramsCaptor.getValue())
				.containsEntry(PlatformSelfSubCancelSupport.SHOP_REMAINING_FULL_CANCEL_PARAM, true);
		verify(partial, never()).execute(anyLong(), anyString(), anyLong(), anyLong(), anyLong(), anyLong(), anyString());
	}
}
