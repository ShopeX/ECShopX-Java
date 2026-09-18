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

import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.supplier.domain.SupplierItems;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ItemsBatchUpdateStoreOrchestrator {

	private final SupplierItemsRepository supplierItemsRepository;
	private final ItemsRepository itemsRepository;
	private final ItemStoreService itemStoreService;
	private final SupplierItemStoreService supplierItemStoreService;

	public ItemsBatchUpdateStoreOrchestrator(
			SupplierItemsRepository supplierItemsRepository,
			ItemsRepository itemsRepository,
			ItemStoreService itemStoreService,
			SupplierItemStoreService supplierItemStoreService) {
		this.supplierItemsRepository = supplierItemsRepository;
		this.itemsRepository = itemsRepository;
		this.itemStoreService = itemStoreService;
		this.supplierItemStoreService = supplierItemStoreService;
	}

	public void batchUpdate(long companyId, String operatorType, List<ItemStoreBatchRow> rows) {
		String ot = operatorType != null ? operatorType.trim() : "";
		boolean supplier = "supplier".equals(ot);
		for (ItemStoreBatchRow row : rows) {
			if (supplier) {
				supplierItemsRepository.updateStoreByCompanyAndProductKey(
						companyId, row.itemId(), row.treatItemIdAsDefaultItemId(), row.store());
				List<SupplierItems> supplierRows =
						supplierItemsRepository.listByCompanyAndProductKey(
								companyId, row.itemId(), row.treatItemIdAsDefaultItemId());
				for (SupplierItems si : supplierRows) {
					if (si.getItemId() != null) {
						supplierItemStoreService.saveSupplierItemStore(si.getItemId(), row.store());
					}
				}
			} else {
				if (row.treatItemIdAsDefaultItemId()) {
					itemsRepository.updateStoreByCompanyAndDefaultItemId(companyId, row.itemId(), row.store());
					List<Items> siblings = itemsRepository.listByDefaultItemIdAndCompany(row.itemId(), companyId);
					for (Items it : siblings) {
						itemStoreService.saveItemStore(it.getItemId(), row.store(), 0L);
					}
				} else {
					itemsRepository.updateSingleItemStoreIfExists(row.itemId(), row.store());
					itemStoreService.saveItemStore(row.itemId(), row.store(), 0L);
				}
			}
		}
	}
}
