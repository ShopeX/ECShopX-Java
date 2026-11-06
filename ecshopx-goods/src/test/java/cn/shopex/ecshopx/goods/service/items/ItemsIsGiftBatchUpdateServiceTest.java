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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.promotions.service.ItemCreatePromotionGuardService;
import cn.shopex.ecshopx.supplier.domain.SupplierItems;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemsIsGiftBatchUpdateServiceTest {

	@Mock
	private ItemsRepository itemsRepository;

	@Mock
	private SupplierItemsRepository supplierItemsRepository;

	@Mock
	private ItemCreatePromotionGuardService itemCreatePromotionGuardService;

	@InjectMocks
	private ItemsIsGiftBatchUpdateService service;

	@Test
	void supplierSetsGiftWritesSupplierAndLinkedPlatformAndProcessing() {
		SupplierItems sku = new SupplierItems();
		sku.setItemId(9001L);
		sku.setPrice(100);
		when(supplierItemsRepository.listByCompanyIdAndGoodsId(38L, 337L, false)).thenReturn(List.of(sku));

		Items linked = new Items();
		linked.setItemId(5001L);
		when(itemsRepository.listByCompanyIdAndSupplierItemIds(38L, List.of(9001L))).thenReturn(List.of(linked));

		service.batchUpdateItemGift(38L, "supplier", List.of(337L), "true");

		verify(itemCreatePromotionGuardService)
				.checkNotFinishedActivityValid(eq(38L), eq(true), eq(List.of(9001L)), eq(List.of(337L)));
		verify(supplierItemsRepository).updateIsGiftByCompanyAndGoodsId(38L, 337L, true);
		verify(itemsRepository).updateIsGiftByCompanyAndItemIds(38L, List.of(5001L), true);
		verify(supplierItemsRepository).updateAuditStatusByGoodsId(38L, 337L, "processing");
		verify(itemsRepository, never()).updateIsGiftByCompanyAndGoodsId(anyLong(), anyLong(), anyBoolean());
	}

	@Test
	void supplierNonGiftRejectsWhenZeroPriceSkuExists() {
		SupplierItems zero = new SupplierItems();
		zero.setItemId(9001L);
		zero.setPrice(0);
		SupplierItems positive = new SupplierItems();
		positive.setItemId(9002L);
		positive.setPrice(100);
		when(supplierItemsRepository.listByCompanyIdAndGoodsId(38L, 337L, true)).thenReturn(List.of(positive));
		when(supplierItemsRepository.listByCompanyIdAndGoodsId(38L, 337L, false))
				.thenReturn(List.of(zero, positive));

		assertThrows(
				ResourceException.class,
				() -> service.batchUpdateItemGift(38L, "supplier", List.of(337L), "false"));
		verify(supplierItemsRepository, never()).updateIsGiftByCompanyAndGoodsId(anyLong(), anyLong(), anyBoolean());
	}

	@Test
	void setsGiftForEachGoodsId() {
		Items sku = new Items();
		sku.setItemId(1001L);
		when(itemsRepository.listByCompanyIdAndGoodsId(38L, 8472L, false)).thenReturn(List.of(sku));
		when(itemsRepository.listByCompanyIdAndGoodsId(38L, 9000L, false)).thenReturn(List.of(sku));

		service.batchUpdateItemGift(38L, "admin", List.of(8472L, 9000L), "true");

		verify(itemsRepository).updateIsGiftByCompanyAndGoodsId(eq(38L), eq(8472L), eq(true));
		verify(itemsRepository).updateIsGiftByCompanyAndGoodsId(eq(38L), eq(9000L), eq(true));
		verify(supplierItemsRepository, never()).updateIsGiftByCompanyAndGoodsId(anyLong(), anyLong(), anyBoolean());
	}
}
