package cn.shopex.ecshopx.orders.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdminOrderCanApplyCancelResolverTest {

	@Test
	@DisplayName("FAILS after shop reject allows re-cancel without repeat_cancel")
	void failsAfterRejectAllowsReapplyWithoutRepeatCancel() {
		Map<String, Object> orderInfo = new LinkedHashMap<>();
		orderInfo.put("order_status", "PAYED");
		orderInfo.put("cancel_status", "FAILS");

		Map<String, Object> cancelData =
				Map.of("refund_status", "SHOP_CHECK_FAILS", "progress", 4, "cancel_from", "shop");

		AdminOrderCanApplyCancelResolver.apply(orderInfo, cancelData, false);

		assertEquals(1, orderInfo.get("can_apply_cancel"));
	}

	@Test
	@DisplayName("WAIT_PROCESS blocks cancel re-apply")
	void waitProcessBlocksCancel() {
		Map<String, Object> orderInfo = new LinkedHashMap<>();
		orderInfo.put("order_status", "PAYED");
		orderInfo.put("cancel_status", "WAIT_PROCESS");

		AdminOrderCanApplyCancelResolver.apply(orderInfo, Map.of("progress", 1), false);

		assertEquals(0, orderInfo.get("can_apply_cancel"));
	}
}
