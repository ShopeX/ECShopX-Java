package cn.shopex.ecshopx.goods.service.items;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemsUpdateOrchestratorDistributorSyncTest {

	@Mock
	private PlatformItemsAddService platformItemsAddService;

	@Mock
	private SupplierItemsAddService supplierItemsAddService;

	@Mock
	private DistributorGoodsSyncService distributorGoodsSyncService;

	@Mock
	private SupplierItemsReviewGoodsService supplierItemsReviewGoodsService;

	@InjectMocks
	private ItemsUpdateOrchestrator sut;

	@Test
	void platformPath_afterAddItems_callsSyncGoodsWithCompanyAndReturnedItemId() {
		Map<String, Object> params = new HashMap<>();
		params.put("company_id", 7001L);
		params.put("operator_type", "admin");
		when(platformItemsAddService.addItemsTransactional(any())).thenReturn(88_001L);

		sut.updateItems(params, 1L, false);

		verify(distributorGoodsSyncService).syncGoods(7001L, 88_001L);
		verifyNoInteractions(supplierItemsAddService);
	}

	@Test
	void platformBranch_invokesPlatformAddItemsTransactionalExactlyOnce_andNeverSupplierPath() {
		Map<String, Object> params = new HashMap<>();
		params.put("company_id", 7001L);
		params.put("operator_type", "admin");
		when(platformItemsAddService.addItemsTransactional(any())).thenReturn(88_001L);

		sut.updateItems(params, 1L, false);

		verify(platformItemsAddService, times(1)).addItemsTransactional(any());
		verify(supplierItemsAddService, never()).addItemsTransactional(any());
		verify(distributorGoodsSyncService).syncGoods(7001L, 88_001L);
	}

	@Test
	void supplierOperatorPath_addsItemsOnly_neverSyncGoods() {
		Map<String, Object> params = new HashMap<>();
		params.put("operator_type", "supplier");
		params.put("operator_id", 99L);

		sut.updateItems(params, 1L, false);

		verify(supplierItemsAddService).addItemsTransactional(any());
		verify(distributorGoodsSyncService, never()).syncGoods(anyLong(), anyLong());
		verifyNoInteractions(platformItemsAddService);
	}

	@Test
	void supplierGoodsPath_neverSyncGoods() {
		Map<String, Object> params = new HashMap<>();
		params.put("company_id", 7001L);
		params.put("audit_status", "approved");

		sut.updateItems(params, 42L, true);

		verify(supplierItemsAddService).addItemsTransactional(any());
		verify(supplierItemsReviewGoodsService).reviewGoods(any(), eq(42L));
		verify(distributorGoodsSyncService, never()).syncGoods(anyLong(), anyLong());
		verifyNoInteractions(platformItemsAddService);
	}
}
