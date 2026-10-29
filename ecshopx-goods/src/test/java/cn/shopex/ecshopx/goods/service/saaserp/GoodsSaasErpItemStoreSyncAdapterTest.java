/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.goods.service.saaserp;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.distribution.mapper.DistributorItemsMapper;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.mapper.ItemsMapper;
import cn.shopex.ecshopx.goods.service.items.ItemStoreService;
import cn.shopex.ecshopx.promotions.service.WdtErpSyncInventoryPromotionSupport;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoodsSaasErpItemStoreSyncAdapterTest {

	private static final long companyId = 10L;
	private static final long itemId = 100L;
	private static final long distributorId = 7L;

	@Mock
	ItemsMapper itemsMapper;

	@Mock
	DistributorMapper distributorMapper;

	@Mock
	DistributorItemsMapper distributorItemsMapper;

	@Mock
	ItemStoreService itemStoreService;

	@Mock
	WdtErpSyncInventoryPromotionSupport wdtErpSyncInventoryPromotionSupport;

	GoodsSaasErpItemStoreSyncAdapter adapter;

	@BeforeAll
	static void initMybatisPlusMetadata() {
		MybatisConfiguration configuration = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
		TableInfoHelper.initTableInfo(assistant, Items.class);
		TableInfoHelper.initTableInfo(assistant, Distributor.class);
		TableInfoHelper.initTableInfo(assistant, DistributorItems.class);
	}

	@BeforeEach
	void setUp() {
		adapter = new GoodsSaasErpItemStoreSyncAdapter(
				itemsMapper,
				distributorMapper,
				distributorItemsMapper,
				itemStoreService,
				wdtErpSyncInventoryPromotionSupport);
		lenient()
				.when(wdtErpSyncInventoryPromotionSupport.listActiveActivityItemBns(companyId))
				.thenReturn(Set.of());
	}

	@Test
	void sync_whenNoMatchingItems_throwsGoodsNotExist() {
		when(itemsMapper.selectList(any())).thenReturn(List.of());

		assertThatThrownBy(() -> adapter.sync(companyId, List.of(Map.of("product_bn", "MISSING", "product_store", 1))))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("商品不存在");
	}

	@Test
	void sync_whenBnOnActivePromotion_doesNotCallSaveItemStore() {
		Items item = item("ACT-BN", itemId);
		when(itemsMapper.selectList(any())).thenReturn(List.of(item));
		when(wdtErpSyncInventoryPromotionSupport.listActiveActivityItemBns(companyId))
				.thenReturn(Set.of("ACT-BN"));

		adapter.sync(companyId, List.of(Map.of("product_bn", "ACT-BN", "product_store", 3)));

		verify(itemStoreService, never()).saveItemStore(any(Long.class), any(Integer.class), any(Long.class));
	}

	@Test
	void sync_withStoreCode_callsSaveItemStoreWithDistributorId() {
		Items item = item("SKU-A", itemId);
		when(itemsMapper.selectList(any())).thenReturn(List.of(item));
		Distributor distributor = new Distributor();
		distributor.setDistributorId(distributorId);
		when(distributorMapper.selectOne(any())).thenReturn(distributor);
		DistributorItems distributorItem = new DistributorItems();
		distributorItem.setIsTotalStore(false);
		when(distributorItemsMapper.selectOne(any())).thenReturn(distributorItem);
		when(distributorItemsMapper.update(any(), any())).thenReturn(1);

		adapter.sync(
				companyId,
				List.of(Map.of("product_bn", "SKU-A", "product_store", 5, "store_code", "SHOP-1")));

		verify(itemStoreService).saveItemStore(itemId, 5, distributorId);
	}

	@Test
	void sync_withoutStoreCode_callsSaveItemStoreWithZeroDistributorId() {
		Items item = item("SKU-B", itemId);
		when(itemsMapper.selectList(any())).thenReturn(List.of(item));
		when(itemsMapper.update(any(), any())).thenReturn(1);

		adapter.sync(companyId, List.of(Map.of("product_bn", "SKU-B", "product_store", 8)));

		verify(itemStoreService).saveItemStore(itemId, 8, 0L);
	}

	@Test
	void sync_whenOneRowFails_doesNotThrow() {
		Items itemA = item("SKU-OK", itemId);
		Items itemB = item("SKU-FAIL", 200L);
		when(itemsMapper.selectList(any())).thenReturn(List.of(itemA, itemB));
		when(itemsMapper.update(any(), any())).thenReturn(1);
		doThrow(new RuntimeException("redis down"))
				.when(itemStoreService)
				.saveItemStore(eq(200L), eq(2), eq(0L));

		assertThatCode(() -> adapter.sync(
						companyId,
						List.of(
								Map.of("product_bn", "SKU-FAIL", "product_store", 2),
								Map.of("product_bn", "SKU-OK", "product_store", 9))))
				.doesNotThrowAnyException();

		verify(itemStoreService).saveItemStore(itemId, 9, 0L);
	}

	private static Items item(String bn, long id) {
		Items item = new Items();
		item.setItemBn(bn);
		item.setItemId(id);
		item.setCompanyId(companyId);
		return item;
	}
}
