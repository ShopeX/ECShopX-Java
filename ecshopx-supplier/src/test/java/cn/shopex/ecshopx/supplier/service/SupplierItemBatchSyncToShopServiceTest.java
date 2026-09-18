package cn.shopex.ecshopx.supplier.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.integration.SupplierItemsSyncToShopExecutor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SupplierItemBatchSyncToShopServiceTest {

	@Mock
	private SupplierItemsSyncToShopExecutor supplierItemsSyncToShopExecutor;

	@InjectMocks
	private SupplierItemBatchSyncToShopService sut;

	@Test
	void batchSyncToShop_deduplicatesItemIds() {
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("operator_type", "distributor");
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("item_ids", "10,20,10");

		List<Long> out = sut.batchSyncToShop(7001L, 88L, jwt, body);

		verify(supplierItemsSyncToShopExecutor).syncToShop(7001L, 88L, 10L);
		verify(supplierItemsSyncToShopExecutor).syncToShop(7001L, 88L, 20L);
		verify(supplierItemsSyncToShopExecutor, times(2)).syncToShop(eq(7001L), eq(88L), org.mockito.ArgumentMatchers.anyLong());
		org.junit.jupiter.api.Assertions.assertEquals(List.of(10L, 20L), out);
	}
}
