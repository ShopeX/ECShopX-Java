package cn.shopex.ecshopx.goods.service.jushuitan;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.distribution.mapper.DistributorItemsMapper;
import cn.shopex.ecshopx.goods.dispatch.ItemStoreUpdatedEventBusPublisher;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.service.items.ItemStoreService;
import cn.shopex.ecshopx.goods.service.items.ItemWarningStoreRedisAccessor;
import cn.shopex.ecshopx.orders.mapper.JushuitanOrderFrozenQuantityMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class JushuitanInventoryPersistItemStoreDispatchPublishProbeTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Items.class);
	}

	private static final long COMPANY_ID = 9L;
	private static final long ITEM_ID = 55L;
	private static final String SKU_BN = "probe-sku-bn";

	@Mock
	private ItemsMapper itemsMapper;

	@Mock
	private JushuitanOrderFrozenQuantityMapper jushuitanOrderFrozenQuantityMapper;

	@Mock
	private ItemStoreService itemStoreService;

	@InjectMocks
	private JushuitanInventoryPersistService jushuitanInventoryPersistService;

	@Test
	void persistInventoriesFromJushuitan_invokesSaveItemStore_withExpectedStoreAfterFrozenDeduction() {
		Items row = new Items();
		row.setCompanyId(COMPANY_ID);
		row.setItemBn(SKU_BN);
		row.setItemId(ITEM_ID);
		when(itemsMapper.selectList(any())).thenReturn(List.of(row));
		when(jushuitanOrderFrozenQuantityMapper.sumFrozenNumForNotPayOrdersAfterCancelDeadline(
						eq(COMPANY_ID), eq(ITEM_ID), anyLong()))
				.thenReturn(2L);
		when(itemsMapper.update(any(), any())).thenReturn(1);

		Map<String, Object> inv = new LinkedHashMap<>();
		inv.put("sku_id", SKU_BN);
		inv.put("qty", 10);
		inv.put("virtual_qty", 0);
		inv.put("purchase_qty", 0);
		inv.put("return_qty", 0);
		inv.put("in_qty", 0);
		inv.put("order_lock", 0);

		double qty = 10.0;
		double freez = 2.0;
		int expectedStore = (int) Math.max(0L, Math.round(qty - freez));

		jushuitanInventoryPersistService.persistInventoriesFromJushuitan(COMPANY_ID, List.of(inv));

		verify(itemStoreService).saveItemStore(ITEM_ID, expectedStore, 0L);
	}

	@Test
	void persistInventoriesFromJushuitan_whenSaveItemStoreCalled_publisherReceivesPublishWithSameTriple() {
		ItemStoreUpdatedEventBusPublisher publisher = mock(ItemStoreUpdatedEventBusPublisher.class);
		@SuppressWarnings("unchecked")
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		ValueOperations<String, String> valueOps = mock(ValueOperations.class);
		when(redisTemplate.opsForValue()).thenReturn(valueOps);

		ItemStoreService realItemStoreService = new ItemStoreService(
				redisTemplate,
				mock(ItemWarningStoreRedisAccessor.class),
				mock(ItemsMapper.class),
				mock(DistributorItemsMapper.class),
				publisher);

		JushuitanInventoryPersistService persistService =
				new JushuitanInventoryPersistService(itemsMapper, jushuitanOrderFrozenQuantityMapper, realItemStoreService);

		Items row = new Items();
		row.setCompanyId(COMPANY_ID);
		row.setItemBn(SKU_BN);
		row.setItemId(ITEM_ID);
		when(itemsMapper.selectList(any())).thenReturn(List.of(row));
		when(jushuitanOrderFrozenQuantityMapper.sumFrozenNumForNotPayOrdersAfterCancelDeadline(
						eq(COMPANY_ID), eq(ITEM_ID), anyLong()))
				.thenReturn(3L);
		when(itemsMapper.update(any(), any())).thenReturn(1);

		Map<String, Object> inv = new LinkedHashMap<>();
		inv.put("sku_id", SKU_BN);
		inv.put("qty", 20);
		inv.put("virtual_qty", 1);
		inv.put("purchase_qty", 0);
		inv.put("return_qty", 0);
		inv.put("in_qty", 0);
		inv.put("order_lock", 2);

		double storeD = 20.0 + 1.0 + 0.0 + 0.0 + 0.0 - 2.0 - 3.0;
		int expectedStore = (int) Math.max(0L, Math.round(storeD));

		persistService.persistInventoriesFromJushuitan(COMPANY_ID, List.of(inv));

		verify(publisher).publish(ITEM_ID, expectedStore, 0L);
	}
}
