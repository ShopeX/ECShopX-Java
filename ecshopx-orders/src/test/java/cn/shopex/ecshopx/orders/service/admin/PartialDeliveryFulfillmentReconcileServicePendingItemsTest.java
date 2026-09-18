package cn.shopex.ecshopx.orders.service.admin;

import static org.assertj.core.api.Assertions.assertThat;

import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PartialDeliveryFulfillmentReconcileServicePendingItemsTest {

	@Test
	@DisplayName("已取消未发货明细不计入待发货")
	void cancelledUnshippedItem_isNotPending() {
		NormalOrdersItems cancelled = new NormalOrdersItems();
		cancelled.setNum(1);
		cancelled.setDeliveryItemNum(0);
		cancelled.setCancelItemNum(1);
		cancelled.setDeliveryStatus("PENDING");

		NormalOrdersItems shipped = new NormalOrdersItems();
		shipped.setNum(1);
		shipped.setDeliveryItemNum(1);
		shipped.setCancelItemNum(0);
		shipped.setDeliveryStatus("DONE");

		assertThat(
						PartialDeliveryFulfillmentReconcileService.hasPendingShippableItems(
								List.of(cancelled, shipped)))
				.isFalse();
		assertThat(
						PartialDeliveryFulfillmentReconcileService.allItemsProcessedFromMaps(
								List.of(
										java.util.Map.of(
												"num", 1, "delivery_item_num", 0, "cancel_item_num", 1),
										java.util.Map.of(
												"num", 1, "delivery_item_num", 1, "cancel_item_num", 0))))
				.isTrue();
	}

	@Test
	@DisplayName("未取消且未发货明细仍待发货")
	void uncancelledUnshippedItem_isPending() {
		NormalOrdersItems pending = new NormalOrdersItems();
		pending.setNum(1);
		pending.setDeliveryItemNum(0);
		pending.setCancelItemNum(0);
		pending.setDeliveryStatus("PENDING");

		assertThat(PartialDeliveryFulfillmentReconcileService.hasPendingShippableItems(List.of(pending)))
				.isTrue();
	}
}
