package cn.shopex.ecshopx.goods.service.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemAvailableStoreResolverTest {

	@Mock
	private SupplierItemStoreService supplierItemStoreService;

	private ItemAvailableStoreResolver resolver;

	@BeforeEach
	void setUp() {
		resolver = new ItemAvailableStoreResolver(supplierItemStoreService);
	}

	@Test
	void logisticsSupplier_usesSupplierItemStore() {
		when(supplierItemStoreService.resolveSupplierItemStore(1L, 88L)).thenReturn(7);
		Map<String, Object> ctx = ctx(9L, 88L, 100, 50, true);

		assertEquals(7, resolver.resolveAvailable(1L, "logistics", ctx));
	}

	@Test
	void logisticsSupplier_missingSupplierItemId_fallsBackToLogisticsStore() {
		Map<String, Object> ctx = ctx(9L, 0L, 100, 12, true);

		assertEquals(12, resolver.resolveAvailable(1L, "logistics", ctx));
	}

	@Test
	void logisticsNonSupplier_usesStore() {
		Map<String, Object> ctx = ctx(0L, 0L, 33, 10, true);

		assertEquals(33, resolver.resolveAvailable(1L, "logistics", ctx));
	}

	@Test
	void nonLogistics_isTotalStoreFalse_usesStore() {
		Map<String, Object> ctx = ctx(9L, 88L, 15, 8, false);

		assertEquals(15, resolver.resolveAvailable(1L, "ziti", ctx));
	}

	@Test
	void nonLogistics_localStorePreferStoreMinusLogistics() {
		Map<String, Object> ctx = ctx(9L, 88L, 20, 8, true);

		assertEquals(12, resolver.resolveAvailable(1L, "ziti", ctx));
	}

	@Test
	void nonLogistics_negativeLocal_fallsBackToStore() {
		Map<String, Object> ctx = ctx(9L, 88L, 5, 10, true);

		assertEquals(5, resolver.resolveAvailable(1L, "merchant", ctx));
	}

	@Test
	void cartDisplay_takesMaxOfLogisticsAndLocal() {
		when(supplierItemStoreService.resolveSupplierItemStore(1L, 88L)).thenReturn(7);
		Map<String, Object> ctx = ctx(9L, 88L, 20, 8, true);

		// logistics=7 (supplier), local=12 (20-8) → max=12
		assertEquals(12, resolver.resolveAvailableForCartDisplay(1L, ctx));
	}

	@Test
	void cartDisplay_zeroWhenBothPathsEmpty() {
		when(supplierItemStoreService.resolveSupplierItemStore(1L, 88L)).thenReturn(0);
		Map<String, Object> ctx = ctx(9L, 88L, 0, 0, true);

		assertEquals(0, resolver.resolveAvailableForCartDisplay(1L, ctx));
	}

	private static Map<String, Object> ctx(
			long supplierId, long supplierItemId, int store, int logisticsStore, boolean totalStore) {
		Map<String, Object> m = new HashMap<>();
		m.put("supplier_id", supplierId);
		m.put("supplier_item_id", supplierItemId);
		m.put("store", store);
		m.put("logistics_store", logisticsStore);
		m.put("is_total_store", totalStore);
		return m;
	}
}
