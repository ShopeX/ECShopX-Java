package cn.shopex.ecshopx.goods.service.items;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemRelAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsBarcodeRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelCatsRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import cn.shopex.ecshopx.supplier.domain.SupplierItems;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsRepository;
import cn.shopex.ecshopx.supplier.service.SupplierItemsAttrPersistenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SupplierItemsReviewGoodsServiceShopBarcodeDualWriteTest {

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
	void syncToShop_dualWritesItemsBarcodeWithDistributorId() {
		when(itemsCategoryDistributorIdResolver.resolveProductModel(7001L)).thenReturn("platform");

		SupplierItems supplier = new SupplierItems();
		supplier.setCompanyId(7001L);
		supplier.setGoodsId(100L);
		supplier.setSupplierId(9);
		supplier.setIsMarket(1);
		supplier.setAuditStatus("approved");
		supplier.setDefaultItemId(42L);
		supplier.setItemId(42L);
		supplier.setBarcode("690088");
		supplier.setIsDefault(true);

		when(supplierItemsRepository.findByItemId(42L)).thenReturn(supplier);
		when(supplierItemsRepository.listByCompanyIdAndGoodsId(7001L, 100L)).thenReturn(List.of(supplier));
		when(itemsRepository.getBySupplierItemIdCompanyAndDistributor(100L, 7001L, 88L)).thenReturn(null);
		when(itemsRepository.getBySupplierItemIdCompanyAndDistributor(42L, 7001L, 88L)).thenReturn(null);
		when(supplierItemsAttrPersistenceService.listByCompanyIdAndItemIdAndAttributeType(anyLong(), anyLong(), any()))
				.thenReturn(List.of());
		doAnswer(invocation -> {
			Items row = invocation.getArgument(0);
			row.setItemId(9101L);
			return null;
		}).when(itemsRepository).insert(any());

		sut.syncToShop(7001L, 88L, 42L);

		verify(itemsBarcodeRepository).saveBarcode(eq(7001L), eq(88L), eq(9101L), eq(9101L), eq("690088"));
	}
}
