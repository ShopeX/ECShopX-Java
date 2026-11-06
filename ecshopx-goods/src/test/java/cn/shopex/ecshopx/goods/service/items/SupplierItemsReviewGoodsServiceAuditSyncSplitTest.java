package cn.shopex.ecshopx.goods.service.items;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.supplier.domain.SupplierItems;
import cn.shopex.ecshopx.supplier.domain.SupplierItemsAuditPatch;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsRepository;
import cn.shopex.ecshopx.supplier.service.SupplierItemsAttrPersistenceService;
import cn.shopex.ecshopx.goods.repository.ItemRelAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsBarcodeRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelCatsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SupplierItemsReviewGoodsServiceAuditSyncSplitTest {

	@Mock
	private SupplierItemsRepository supplierItemsRepository;

	@Mock
	private ItemsRepository itemsRepository;

	@Mock
	private ItemStoreService itemStoreService;

	@Mock
	private ItemRelAttributesRepository itemRelAttributesRepository;

	@Mock
	private ItemsRelCatsRepository itemsRelCatsRepository;

	@Mock
	private ItemsBarcodeRepository itemsBarcodeRepository;

	@Mock
	private SupplierItemsAttrPersistenceService supplierItemsAttrPersistenceService;

	@Mock
	private ObjectMapper objectMapper;

	@Mock
	private ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver;

	@InjectMocks
	private SupplierItemsReviewGoodsService sut;

	@Test
	void reviewGoods_approved_updatesAuditOnlyWithoutPoolSync() {
		SupplierItems supplier = new SupplierItems();
		supplier.setCompanyId(7001L);
		supplier.setGoodsId(100L);
		supplier.setSupplierId(9);
		supplier.setIsMarket(1);
		supplier.setAuditStatus("processing");
		supplier.setDefaultItemId(42L);
		supplier.setItemId(42L);
		when(supplierItemsRepository.findByItemId(42L)).thenReturn(supplier);

		Map<String, Object> params = new HashMap<>();
		params.put("company_id", 7001L);
		params.put("audit_status", "approved");
		params.put("audit_reason", "ok");

		sut.reviewGoods(params, 42L);

		verify(supplierItemsRepository).updateAuditByGoodsId(eq(7001L), eq(100L), any(SupplierItemsAuditPatch.class));
		verify(itemsRepository, never()).insert(any());
		verify(itemStoreService, never()).saveItemStore(anyLong(), anyInt(), anyLong());
		verify(itemsBarcodeRepository, never()).saveBarcode(anyLong(), anyLong(), anyLong(), anyLong(), any());
	}

	@Test
	void reviewGoods_foreignCompany_rejectsWithoutAuditUpdate() {
		SupplierItems supplier = new SupplierItems();
		supplier.setCompanyId(7002L);
		supplier.setGoodsId(100L);
		when(supplierItemsRepository.findByItemId(42L)).thenReturn(supplier);

		Map<String, Object> params = new HashMap<>();
		params.put("company_id", 7001L);
		params.put("audit_status", "approved");

		assertThatThrownBy(() -> sut.reviewGoods(params, 42L))
				.isInstanceOf(ResourceException.class)
				.hasMessage("商品不存在或无权");

		verify(supplierItemsRepository, never()).updateAuditByGoodsId(anyLong(), anyLong(), any());
	}

	@Test
	void syncToPool_approved_delegatesToPoolSyncWithStore() {
		SupplierItems supplier = new SupplierItems();
		supplier.setCompanyId(7001L);
		supplier.setGoodsId(100L);
		supplier.setSupplierId(9);
		supplier.setIsMarket(1);
		supplier.setAuditStatus("approved");
		supplier.setDefaultItemId(42L);
		supplier.setItemId(42L);
		supplier.setBarcode("690001");
		supplier.setIsDefault(true);
		supplier.setStore(42);
		when(supplierItemsRepository.findByItemId(42L)).thenReturn(supplier);
		when(supplierItemsRepository.listByCompanyIdAndGoodsId(7001L, 100L)).thenReturn(java.util.List.of(supplier));
		when(itemsRepository.getBySupplierItemIdAndCompany(100L, 7001L)).thenReturn(null);
		when(itemsRepository.getBySupplierItemIdAndCompany(42L, 7001L)).thenReturn(null);
		when(supplierItemsAttrPersistenceService.listByCompanyIdAndItemIdAndAttributeType(anyLong(), anyLong(), any()))
				.thenReturn(java.util.List.of());
		doAnswer(invocation -> {
			Items row = invocation.getArgument(0);
			row.setItemId(9001L);
			return null;
		}).when(itemsRepository).insert(any());

		when(itemsCategoryDistributorIdResolver.resolveProductModel(7001L)).thenReturn("standard");

		sut.syncToPool(7001L, 42L);

		verify(itemsRepository).insert(any());
		verify(itemStoreService, times(1)).saveItemStore(eq(9001L), eq(42), eq(0L));
		verify(itemsBarcodeRepository).saveBarcode(eq(7001L), eq(0L), eq(9001L), eq(9001L), eq("690001"));
		verify(supplierItemsRepository).updateIsSyncPlatformByGoodsId(7001L, 100L, 1);
	}
}
