package cn.shopex.ecshopx.goods.service.items;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.repository.SupplierLinkedItemsSyncPatch;
import cn.shopex.ecshopx.promotions.service.ItemCreatePromotionGuardService;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsRepository;
import cn.shopex.ecshopx.supplier.service.SupplierItemsAttrPersistenceService;
import cn.shopex.ecshopx.espier.service.upload.IntroHtmlDataImageUploadService;
import cn.shopex.ecshopx.point.service.PointMemberRuleReadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SupplierItemsAddServiceSyncPlatformIfNeededTest {

	@Mock
	private IntroHtmlDataImageUploadService introHtmlDataImageUploadService;
	@Mock
	private PointMemberRuleReadService pointMemberRuleReadService;
	@Mock
	private ItemSpecParamsResolver itemSpecParamsResolver;
	@Mock
	private NormalItemTypeHandler normalItemTypeHandler;
	@Mock
	private ServicesItemTypeHandler servicesItemTypeHandler;
	@Mock
	private SupplierItemsRepository supplierItemsRepository;
	@Mock
	private ItemsRepository itemsRepository;
	@Mock
	private ItemStoreService itemStoreService;
	@Mock
	private SupplierItemStoreService supplierItemStoreService;
	@Mock
	private SupplierItemsAttrPersistenceService supplierItemsAttrPersistenceService;
	@Mock
	private ItemCreatePromotionGuardService itemCreatePromotionGuardService;

	private SupplierItemsAddService sut;

	@BeforeEach
	void setUp() {
		sut = new SupplierItemsAddService(
				introHtmlDataImageUploadService,
				pointMemberRuleReadService,
				itemSpecParamsResolver,
				normalItemTypeHandler,
				servicesItemTypeHandler,
				supplierItemsRepository,
				itemsRepository,
				itemStoreService,
				supplierItemStoreService,
				supplierItemsAttrPersistenceService,
				itemCreatePromotionGuardService,
				new ObjectMapper());
	}

	@Test
	void syncPlatformIfNeeded_updatesPhpWhitelistFieldsOnAllLinkedRows() {
		Items shop = new Items();
		shop.setItemId(6001L);
		shop.setDistributorId(88);
		Items pool = new Items();
		pool.setItemId(5001L);
		pool.setDistributorId(0);
		when(itemsRepository.listByCompanyIdAndSupplierItemIds(7001L, List.of(88L))).thenReturn(List.of(shop, pool));

		ItemsCreateContext ctx = new ItemsCreateContext(7001L, "supplier", "normal", false, true);
		Map<String, Object> sku = Map.of(
				"store", 42,
				"pics", List.of("https://img/a.jpg"),
				"approve_status", "onsale",
				"item_name", "测试商品",
				"cost_price", "12.00",
				"start_num", 2,
				"audit_status", "processing");
		ReflectionTestUtils.invokeMethod(sut, "syncPlatformIfNeeded", 88L, sku, ctx);

		ArgumentCaptor<SupplierLinkedItemsSyncPatch> patchCaptor = ArgumentCaptor.forClass(SupplierLinkedItemsSyncPatch.class);
		verify(itemsRepository).updateLinkedItemsOnSupplierEdit(eq(7001L), eq(List.of(6001L, 5001L)), patchCaptor.capture());
		SupplierLinkedItemsSyncPatch patch = patchCaptor.getValue();
		org.junit.jupiter.api.Assertions.assertEquals(42, patch.store());
		org.junit.jupiter.api.Assertions.assertEquals("[\"https://img/a.jpg\"]", patch.pics());
		org.junit.jupiter.api.Assertions.assertEquals("onsale", patch.approveStatus());
		org.junit.jupiter.api.Assertions.assertEquals("测试商品", patch.itemName());
		org.junit.jupiter.api.Assertions.assertEquals(1200, patch.costPrice());
		org.junit.jupiter.api.Assertions.assertEquals(2, patch.startNum());
		org.junit.jupiter.api.Assertions.assertEquals("processing", patch.auditStatus());
		org.junit.jupiter.api.Assertions.assertEquals("", patch.auditReason());
		org.junit.jupiter.api.Assertions.assertNull(patch.auditDate());
		verify(itemsRepository, never()).updateByItemId(anyLong(), anyLong(), any());
		verify(itemStoreService, times(2)).saveItemStore(anyLong(), eq(42), eq(0L));
	}

	@Test
	void syncPlatformIfNeeded_convertsYuanCostPriceWithDecimalsToFen() {
		Items pool = new Items();
		pool.setItemId(5001L);
		pool.setDistributorId(0);
		when(itemsRepository.listByCompanyIdAndSupplierItemIds(7001L, List.of(88L))).thenReturn(List.of(pool));

		ItemsCreateContext ctx = new ItemsCreateContext(7001L, "supplier", "normal", false, true);
		Map<String, Object> sku = Map.of(
				"store", 10,
				"pics", "[]",
				"approve_status", "instock",
				"item_name", "A",
				"cost_price", "9.24",
				"audit_status", "approved");
		ReflectionTestUtils.invokeMethod(sut, "syncPlatformIfNeeded", 88L, sku, ctx);

		ArgumentCaptor<SupplierLinkedItemsSyncPatch> patchCaptor = ArgumentCaptor.forClass(SupplierLinkedItemsSyncPatch.class);
		verify(itemsRepository).updateLinkedItemsOnSupplierEdit(eq(7001L), eq(List.of(5001L)), patchCaptor.capture());
		org.junit.jupiter.api.Assertions.assertEquals(924, patchCaptor.getValue().costPrice());
	}

	@Test
	void syncPlatformIfNeeded_skipsAuditFieldsWhenNotResetStatus() {
		Items pool = new Items();
		pool.setItemId(5001L);
		pool.setDistributorId(0);
		when(itemsRepository.listByCompanyIdAndSupplierItemIds(7001L, List.of(88L))).thenReturn(List.of(pool));

		ItemsCreateContext ctx = new ItemsCreateContext(7001L, "supplier", "normal", false, true);
		Map<String, Object> sku = Map.of(
				"store", 10,
				"pics", "[]",
				"approve_status", "instock",
				"item_name", "A",
				"audit_status", "approved");
		ReflectionTestUtils.invokeMethod(sut, "syncPlatformIfNeeded", 88L, sku, ctx);

		ArgumentCaptor<SupplierLinkedItemsSyncPatch> patchCaptor = ArgumentCaptor.forClass(SupplierLinkedItemsSyncPatch.class);
		verify(itemsRepository).updateLinkedItemsOnSupplierEdit(eq(7001L), eq(List.of(5001L)), patchCaptor.capture());
		org.junit.jupiter.api.Assertions.assertNull(patchCaptor.getValue().auditStatus());
	}
}
