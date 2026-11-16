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

package cn.shopex.ecshopx.goods.service.items;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsPriceStoreStatusPatch;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.search.sync.GoodsSearchIndexDirtyFanout;
import cn.shopex.ecshopx.promotions.service.ItemCreatePromotionGuardService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemsPriceStoreStatusWriteServiceSearchIndexHookTest {

	@Mock
	private SupplierItemsAdminBatchUpdateService supplierItemsAdminBatchUpdateService;

	@Mock
	private ItemsRepository itemsRepository;

	@Mock
	private ItemCreatePromotionGuardService itemCreatePromotionGuardService;

	@Mock
	private ItemsTableLangSyncAfterMainUpdateService itemsTableLangSyncAfterMainUpdateService;

	@Mock
	private GoodsSearchIndexDirtyFanout goodsSearchIndexDirtyFanout;

	@InjectMocks
	private ItemsPriceStoreStatusWriteService service;

	@Test
	void platformPriceUpdate_fansOutSearchIndexForDefaultItem() {
		Items row = new Items();
		row.setItemId(42L);
		row.setGoodsId(100L);
		row.setDefaultItemId(99L);
		row.setIsGift(false);
		row.setPrice(1000);
		when(itemsRepository.selectOneForPriceCheckAndPromotion(eq(38L), eq(42L), isNull())).thenReturn(row);
		when(itemsRepository.updateByItemsPriceStoreFilter(
						eq(38L), eq(42L), isNull(), isNull(), any(ItemsPriceStoreStatusPatch.class)))
				.thenReturn(1);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("item_id", 42L);
		body.put("price", "12.00");

		service.updateFromRequest(38L, "admin", body);

		verify(itemCreatePromotionGuardService).checkItemPrice(eq(38L), eq(List.of(100L)), any());
		verify(goodsSearchIndexDirtyFanout).scheduleUpsertForDefaultItem(38L, 99L);
	}

	@Test
	void platformAuditStatusUpdate_fansOutSearchIndexByGoodsId() {
		Items sku = new Items();
		sku.setItemId(42L);
		sku.setGoodsId(100L);
		sku.setDefaultItemId(99L);
		when(itemsRepository.listByCompanyIdAndGoodsId(38L, 100L, false)).thenReturn(List.of(sku));
		when(itemsRepository.updateByItemsPriceStoreFilter(
						eq(38L),
						isNull(),
						eq(100L),
						anyList(),
						any(ItemsPriceStoreStatusPatch.class)))
				.thenReturn(1);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("goods_id", 100L);
		body.put("audit_status", "processing");

		service.updateFromRequest(38L, "admin", body);

		verify(goodsSearchIndexDirtyFanout).scheduleUpsertForDefaultItem(38L, 99L);
	}
}
