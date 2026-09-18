package cn.shopex.ecshopx.shuyun.service.openplatform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TradeSyncServiceMappingTest {

	@Test
	void mapOrderStatus_doneAndCancel() {
		assertEquals("TRADE_FINISHED", TradeSyncService.mapOrderStatus("DONE", ""));
		assertEquals("TRADE_CLOSED", TradeSyncService.mapOrderStatus("CANCEL", ""));
		assertEquals("TRADE_CLOSED_ALL_REFUND", TradeSyncService.mapOrderStatus("REFUND_SUCCESS", ""));
	}

	@Test
	void mapOrderStatus_payedDependsOnDelivery() {
		assertEquals("WAIT_SELLER_SEND_GOODS", TradeSyncService.mapOrderStatus("PAYED", "PENDING"));
		assertEquals("WAIT_BUYER_CONFIRM_GOODS", TradeSyncService.mapOrderStatus("PAYED", "DONE"));
		assertEquals("SELLER_CONSIGNED_PART", TradeSyncService.mapOrderStatus("REVIEW_PASS", "PARTAIL"));
	}

	@Test
	void mapDeliveryType() {
		assertEquals("SY_SELFLIFT", TradeSyncService.mapDeliveryType("ziti"));
		assertEquals("SY_INTRA_CITY_SERVICE", TradeSyncService.mapDeliveryType("dada"));
		assertEquals("SY_NONE", TradeSyncService.mapDeliveryType("merchant"));
		assertEquals("SY_EXPRESS", TradeSyncService.mapDeliveryType("express"));
	}

	@Test
	void fenToYuan() {
		assertEquals(12.34, TradeSyncService.fenToYuan(1234), 0.001);
		assertEquals(0.01, TradeSyncService.fenToYuan(1), 0.001);
	}

	@Test
	void tradeSyncDedupeKeyShape() {
		String k = OrderSyncDispatchCacheKeys.tradeSyncDedupeKey(1L, "O123");
		assertTrue(k.startsWith("shuyun_open_platform:dispatch_dedupe:trade_sync:1:"));
		assertEquals(40, k.substring(k.lastIndexOf(':') + 1).length());
	}
}
