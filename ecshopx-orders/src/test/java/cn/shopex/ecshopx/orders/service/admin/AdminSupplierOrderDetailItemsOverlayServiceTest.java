package cn.shopex.ecshopx.orders.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.companys.service.setting.TradeCancelSettingRedisService;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdminSupplierOrderDetailItemsOverlayServiceTest {

	@Test
	@DisplayName("Supplier overlay rebuilds app_info for PAYED/PENDING sub-order after main PARTAIL cancel reject")
	void supplierOverlayRebuildsCancelAndDeliveryButtons() {
		SupplierOrderMapper supplierOrderMapper = mock(SupplierOrderMapper.class);
		AdminOrderDetailStatusAppApplier statusApplier = new AdminOrderDetailStatusAppApplier();
		TradeCancelSettingRedisService tradeCancelSettingRedisService = mock(TradeCancelSettingRedisService.class);
		when(tradeCancelSettingRedisService.getCancelSetting(1L)).thenReturn(Map.of("repeat_cancel", false));

		SupplierOrder supplierOrder = new SupplierOrder();
		supplierOrder.setOrderStatus("PAYED");
		supplierOrder.setDeliveryStatus("PENDING");
		supplierOrder.setCancelStatus("FAILS");
		when(supplierOrderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(supplierOrder);

		AdminSupplierOrderDetailItemsOverlayService service =
				new AdminSupplierOrderDetailItemsOverlayService(
						supplierOrderMapper, statusApplier, tradeCancelSettingRedisService);

		Map<String, Object> orderInfo = new LinkedHashMap<>();
		orderInfo.put("order_id", "5353503000105097");
		orderInfo.put("company_id", 1L);
		orderInfo.put("order_status", "PAYED");
		orderInfo.put("delivery_status", "PARTAIL");
		orderInfo.put("cancel_status", "FAILS");
		orderInfo.put("order_status_des", "PAYED_PARTAIL");
		orderInfo.put("receipt_type", "logistics");
		orderInfo.put("left_aftersales_num", 1);
		orderInfo.put("update_time", 1);
		orderInfo.put("end_time", 0);
		orderInfo.put("order_type", "normal");
		orderInfo.put("order_class", "normal");
		orderInfo.put(
				"app_info",
				OrderAppAttachAdminBuilder.buildAppInfo(
						"PAYED",
						"logistics",
						Map.of(
								"order_status_des", "PAYED_PARTAIL",
								"left_aftersales_num", 1,
								"update_time", 1,
								"end_time", 0,
								"order_class", "normal"),
						""));
		orderInfo.put(
				"items",
				List.of(
						Map.of("supplier_id", 9, "item_name", "A"),
						Map.of("supplier_id", 10, "item_name", "B")));

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("orderInfo", orderInfo);
		result.put(
				"cancelData",
				Map.of("cancel_from", "shop", "refund_status", "SHOP_CHECK_FAILS", "progress", 4));

		Map<String, Object> jwt = Map.of("operator_type", "supplier", "operator_id", 9);
		service.apply(jwt, result);

		@SuppressWarnings("unchecked")
		Map<String, Object> appInfo = (Map<String, Object>) orderInfo.get("app_info");
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> buttons = (List<Map<String, Object>>) appInfo.get("buttons");
		List<String> types = buttons.stream().map(b -> String.valueOf(b.get("type"))).toList();

		assertEquals("PAYED", orderInfo.get("order_status_des"));
		assertTrue(types.contains("cancel"), "supplier should see cancel button: " + types);
		assertTrue(types.contains("delivery"), "supplier should see delivery button: " + types);
		assertEquals(1, orderInfo.get("can_apply_cancel"));
	}
}
