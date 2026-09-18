package cn.shopex.ecshopx.goods.service.items;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.inventory.ItemInventoryLineContext;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemInventoryOrchestratorServiceTest {

	@Mock
	private ItemStoreService itemStoreService;
	@Mock
	private SupplierItemStoreService supplierItemStoreService;
	@Mock
	private ItemsMapper itemsMapper;

	private ItemInventoryOrchestratorService service;

	@BeforeEach
	void setUp() {
		service = new ItemInventoryOrchestratorService(
				itemStoreService,
				supplierItemStoreService,
				new InventoryDeductTargetResolver(),
				itemsMapper);
	}

	@Test
	@DisplayName("is_total_store=true：扣 itemId key，回写 items（对齐 PHP）")
	void totalStoreDeductsPlainItemKeyOntoItems() {
		Items item = new Items();
		item.setItemId(16L);
		item.setDistributorId(3008);
		item.setSupplierId(0);
		item.setSupplierItemId(0);
		when(itemsMapper.selectOne(org.mockito.ArgumentMatchers.<LambdaQueryWrapper<Items>>any())).thenReturn(item);
		when(itemStoreService.minusItemStore(16L, 1, 0L, false, 1L)).thenReturn(true);

		ItemInventoryLineContext ctx =
				ItemInventoryLineContext.of(1L, 16L, 0L, 3008L, true, "logistics", 0L);
		assertTrue(service.minusItemStore(ctx, 1));

		verify(itemStoreService).minusItemStore(eq(16L), eq(1), eq(0L), eq(false), eq(1L));
	}

	@Test
	@DisplayName("is_total_store=false：扣请求店前缀 key，回写 distributor_items")
	void shopIndependentWritesDistributorItems() {
		Items item = new Items();
		item.setItemId(16L);
		item.setDistributorId(0);
		when(itemsMapper.selectOne(org.mockito.ArgumentMatchers.<LambdaQueryWrapper<Items>>any())).thenReturn(item);
		when(itemStoreService.minusItemStore(16L, 2, 3008L, true, 1L)).thenReturn(true);

		ItemInventoryLineContext ctx =
				ItemInventoryLineContext.of(1L, 16L, 0L, 3008L, false, "ziti", 0L);
		assertTrue(service.minusItemStore(ctx, 2));

		verify(itemStoreService).minusItemStore(eq(16L), eq(2), eq(3008L), eq(true), eq(1L));
	}

	@Test
	@DisplayName("平台商品 is_total_store=true：扣 itemId key，回写 items")
	void platformHqDeductsPlainItemKey() {
		Items item = new Items();
		item.setItemId(16L);
		item.setDistributorId(0);
		when(itemsMapper.selectOne(org.mockito.ArgumentMatchers.<LambdaQueryWrapper<Items>>any())).thenReturn(item);
		when(itemStoreService.minusItemStore(16L, 1, 0L, false, 1L)).thenReturn(true);

		ItemInventoryLineContext ctx =
				ItemInventoryLineContext.of(1L, 16L, 0L, 3008L, true, "ziti", 0L);
		assertTrue(service.minusItemStore(ctx, 1));

		verify(itemStoreService).minusItemStore(eq(16L), eq(1), eq(0L), eq(false), eq(1L));
	}
}
