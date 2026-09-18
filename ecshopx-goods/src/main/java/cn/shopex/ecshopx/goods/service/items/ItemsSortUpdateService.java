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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.supplier.domain.SupplierItems;
import cn.shopex.ecshopx.supplier.repository.SupplierItemsRepository;
import org.springframework.stereotype.Service;

@Service
public class ItemsSortUpdateService {

	private final ItemsRepository itemsRepository;
	private final SupplierItemsRepository supplierItemsRepository;

	public ItemsSortUpdateService(ItemsRepository itemsRepository, SupplierItemsRepository supplierItemsRepository) {
		this.itemsRepository = itemsRepository;
		this.supplierItemsRepository = supplierItemsRepository;
	}

	public void setItemsSort(long companyId, long itemId, int sort, String operateSourceOrNull) {
		String os = operateSourceOrNull == null ? "" : operateSourceOrNull;
		if ("supplier".equals(os)) {
			SupplierItems si = supplierItemsRepository.findByItemId(itemId);
			if (si == null) {
				throw new ResourceException("请确认您的商品信息后再提交。");
			}
			if (si.getCompanyId() == null || si.getCompanyId() != companyId) {
				throw new ResourceException("请确认您的商品信息后再提交。");
			}
			SupplierItems patch = new SupplierItems();
			patch.setSort(sort);
			supplierItemsRepository.updateOneByItemId(itemId, companyId, patch);
			return;
		}

		Items row = itemsRepository.findByItemId(itemId);
		if (row == null) {
			throw new ResourceException("请确认您的商品信息后再提交。");
		}
		if (row.getCompanyId() == null || row.getCompanyId() != companyId) {
			throw new ResourceException("请确认您的商品信息后再提交。");
		}
		itemsRepository.updateSortByItemIdIfExists(itemId, sort);
	}
}
