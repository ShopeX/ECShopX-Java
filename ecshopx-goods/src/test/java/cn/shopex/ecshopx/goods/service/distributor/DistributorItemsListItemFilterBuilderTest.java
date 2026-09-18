package cn.shopex.ecshopx.goods.service.distributor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelTagsRepository;
import cn.shopex.ecshopx.goods.service.items.ItemStoreService;
import cn.shopex.ecshopx.goods.service.items.ItemsCategoryItemIdResolver;
import cn.shopex.ecshopx.supplier.repository.SupplierOperatorQueryRepository;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributorItemsListItemFilterBuilderTest {

	@Mock
	private ItemsRelTagsRepository itemsRelTagsRepository;
	@Mock
	private SupplierOperatorQueryRepository supplierOperatorQueryRepository;
	@Mock
	private ItemsCategoryItemIdResolver itemsCategoryItemIdResolver;
	@Mock
	private ItemStoreService itemStoreService;
	@Mock
	private ItemsListQueryRepository itemsListQueryRepository;

	private DistributorItemsListItemFilterBuilder sut;

	@BeforeEach
	void setUp() {
		sut = new DistributorItemsListItemFilterBuilder(
				itemsRelTagsRepository,
				supplierOperatorQueryRepository,
				itemsCategoryItemIdResolver,
				itemStoreService,
				itemsListQueryRepository);
	}

	@Test
	void build_itemHolderSupplier_requiresApprovedAuditStatus() {
		Map<String, Object> merged = Map.of(
				"distributor_id", 285L,
				"item_holder", "supplier");

		Map<String, Object> filter = sut.build(141L, merged).orElseThrow();

		assertEquals(285L, filter.get("distributor_id"));
		assertEquals(Boolean.TRUE, filter.get(ItemsListQueryRepository.KEY_ITEM_SOURCE_SUPPLIER_MODE));
	}

	@Test
	void build_itemSourceSupplier_matchesGoodsItemsList() {
		Map<String, Object> merged = Map.of(
				"distributor_id", 285L,
				"item_source", "supplier");

		Map<String, Object> filter = sut.build(141L, merged).orElseThrow();

		assertEquals(Boolean.TRUE, filter.get(ItemsListQueryRepository.KEY_ITEM_SOURCE_SUPPLIER_MODE));
	}

	@Test
	void build_itemSourcePlatform_restrictsToPlatformOwnedGoods() {
		Map<String, Object> merged = Map.of(
				"distributor_id", 285L,
				"item_source", "platform");

		Map<String, Object> filter = sut.build(141L, merged).orElseThrow();

		assertEquals(Boolean.TRUE, filter.get(ItemsListQueryRepository.KEY_ITEM_SOURCE_NON_SUPPLIER_MODE));
		assertEquals(0, filter.get(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ));
		assertFalse(filter.containsKey(ItemsListQueryRepository.KEY_ITEM_SOURCE_SUPPLIER_MODE));
		assertFalse(filter.containsKey(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_GT0_ONLY));
	}

	@Test
	void build_itemHolderSelf_restrictsToPlatformOwnedGoods() {
		Map<String, Object> merged = Map.of(
				"distributor_id", 285L,
				"item_holder", "self");

		Map<String, Object> filter = sut.build(141L, merged).orElseThrow();

		assertEquals(Boolean.TRUE, filter.get(ItemsListQueryRepository.KEY_ITEM_SOURCE_NON_SUPPLIER_MODE));
		assertEquals(0, filter.get(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ));
	}

	@Test
	void build_itemHolderDistributor_restrictsToMerchantOwnedGoods() {
		Map<String, Object> merged = Map.of(
				"distributor_id", 285L,
				"item_holder", "distributor");

		Map<String, Object> filter = sut.build(141L, merged).orElseThrow();

		assertEquals(Boolean.TRUE, filter.get(ItemsListQueryRepository.KEY_ITEM_SOURCE_NON_SUPPLIER_MODE));
		assertEquals(Boolean.TRUE, filter.get(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_GT0_ONLY));
		assertFalse(filter.containsKey(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ));
	}

	@Test
	void build_itemHolderPlatform_eqSelf() {
		Map<String, Object> merged = Map.of(
				"distributor_id", 285L,
				"item_holder", "platform");

		Map<String, Object> filter = sut.build(141L, merged).orElseThrow();

		assertEquals(Boolean.TRUE, filter.get(ItemsListQueryRepository.KEY_ITEM_SOURCE_NON_SUPPLIER_MODE));
		assertEquals(0, filter.get(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ));
	}

	@Test
	void resolveListItemSource_itemHolderOverridesItemSource() {
		Map<String, Object> merged = new HashMap<>();
		merged.put("item_holder", "supplier");
		merged.put("item_source", "platform");

		assertEquals("supplier", DistributorItemsListItemFilterBuilder.resolveListItemSource(merged));
	}

	@Test
	void build_isCanSaleTrue_setsDistCanSaleFilter() {
		Map<String, Object> merged = Map.of(
				"distributor_id", 269L,
				"is_can_sale", "true");

		Map<String, Object> filter = sut.build(141L, merged).orElseThrow();

		assertEquals(Boolean.TRUE, filter.get("__dist_is_can_sale_filter"));
	}

	@Test
	void build_isCanSaleFalse_setsDistCanSaleFilter() {
		Map<String, Object> merged = Map.of(
				"distributor_id", 269L,
				"is_can_sale", "false");

		Map<String, Object> filter = sut.build(141L, merged).orElseThrow();

		assertEquals(Boolean.FALSE, filter.get("__dist_is_can_sale_filter"));
	}

	@Test
	void build_withoutItemSourceOrHolder_doesNotRestrictBySupplier() {
		Map<String, Object> merged = Map.of("distributor_id", 285L);

		Map<String, Object> filter = sut.build(141L, merged).orElseThrow();

		assertFalse(filter.containsKey(ItemsListQueryRepository.KEY_ITEM_SOURCE_SUPPLIER_MODE));
		assertFalse(filter.containsKey(ItemsListQueryRepository.KEY_ITEM_SOURCE_NON_SUPPLIER_MODE));
		assertTrue(filter.containsKey("is_default_true"));
	}
}
