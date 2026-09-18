package cn.shopex.ecshopx.supplier.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.integration.SupplierItemsSyncToPoolExecutor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SupplierItemBatchSyncToPoolServiceTest {

	@Mock
	private SupplierItemsSyncToPoolExecutor supplierItemsSyncToPoolExecutor;

	@InjectMocks
	private SupplierItemBatchSyncToPoolService sut;

	@Test
	void batchSyncToPool_deduplicatesItemIds() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("item_ids", "10,20,10");

		List<Long> out = sut.batchSyncToPool(7001L, body);

		verify(supplierItemsSyncToPoolExecutor).syncToPool(7001L, 10L);
		verify(supplierItemsSyncToPoolExecutor).syncToPool(7001L, 20L);
		verify(supplierItemsSyncToPoolExecutor, times(2)).syncToPool(eq(7001L), org.mockito.ArgumentMatchers.anyLong());
		org.junit.jupiter.api.Assertions.assertEquals(List.of(10L, 20L), out);
	}
}
