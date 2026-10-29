package cn.shopex.ecshopx.goods.service.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OnsalePlatformStoreSupportTest {

	@Test
	void appendInjectsPlatformStoreFromLogisticsStore() {
		List<Map<String, Object>> list = new ArrayList<>();
		Map<String, Object> row = new HashMap<>();
		row.put("store", 3);
		row.put("logistics_store", 8);
		row.put("is_total_store", false);
		list.add(row);
		OnsalePlatformStoreSupport.appendPlatformStoreForOnsaleSkuList(list, true);
		assertEquals(8, list.get(0).get("platform_store"));
	}

	@Test
	void appendSkippedWhenInjectFalse() {
		List<Map<String, Object>> list = new ArrayList<>();
		Map<String, Object> row = new HashMap<>();
		row.put("store", 5);
		row.put("logistics_store", 9);
		list.add(row);
		OnsalePlatformStoreSupport.appendPlatformStoreForOnsaleSkuList(list, false);
		assertFalse(list.get(0).containsKey("platform_store"));
	}

	@Test
	void headquartersUsesStoreWhenNoLogistics() {
		Map<String, Object> row = Map.of("store", 7, "is_total_store", true);
		assertEquals(7, OnsalePlatformStoreSupport.resolvePlatformStoreForSkuRow(row));
	}

	@Test
	void storeShipmentWithDistributorStoreKeyReturnsZero() {
		Map<String, Object> row = new HashMap<>();
		row.put("store", 5);
		row.put("is_total_store", false);
		row.put("distributor_store", 5);
		assertEquals(0, OnsalePlatformStoreSupport.resolvePlatformStoreForSkuRow(row));
	}
}
