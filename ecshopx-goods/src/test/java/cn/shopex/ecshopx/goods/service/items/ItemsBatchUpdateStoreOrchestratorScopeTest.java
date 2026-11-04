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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemsBatchUpdateStoreOrchestratorScopeTest {

	@Mock SupplierItemsRepository supplierItemsRepository;
	@Mock ItemsRepository itemsRepository;
	@Mock ItemStoreService itemStoreService;
	@Mock SupplierItemStoreService supplierItemStoreService;

	ItemsBatchUpdateStoreOrchestrator orchestrator;

	@BeforeEach
	void setUp() {
		orchestrator =
				new ItemsBatchUpdateStoreOrchestrator(
						supplierItemsRepository,
						itemsRepository,
						itemStoreService,
						supplierItemStoreService);
	}

	@Test
	void batchUpdate_nonDefault_usesCompanyScopedUpdate_andSkipsRedisWhenNoRow() {
		when(itemsRepository.updateStoreByItemIds(eq(100L), eq(List.of(42L)), eq(9))).thenReturn(0);

		orchestrator.batchUpdate(
				100L, "admin", List.of(new ItemStoreBatchRow(42L, 9, false)));

		verify(itemsRepository).updateStoreByItemIds(eq(100L), eq(List.of(42L)), eq(9));
		verify(itemsRepository, never()).updateSingleItemStoreIfExists(anyLong(), anyInt());
		verify(itemStoreService, never()).saveItemStore(anyLong(), anyInt(), anyLong());
	}

	@Test
	void batchUpdate_nonDefault_savesRedisOnlyWhenCompanyUpdateHits() {
		when(itemsRepository.updateStoreByItemIds(eq(100L), eq(List.of(42L)), eq(9))).thenReturn(1);

		orchestrator.batchUpdate(
				100L, "admin", List.of(new ItemStoreBatchRow(42L, 9, false)));

		verify(itemsRepository).updateStoreByItemIds(eq(100L), eq(List.of(42L)), eq(9));
		verify(itemsRepository, never()).updateSingleItemStoreIfExists(anyLong(), anyInt());
		verify(itemStoreService).saveItemStore(eq(42L), eq(9), eq(0L));
	}
}
