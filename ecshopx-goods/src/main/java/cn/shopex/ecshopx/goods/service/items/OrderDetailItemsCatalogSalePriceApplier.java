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
import cn.shopex.ecshopx.supplier.domain.Supplier;
import cn.shopex.ecshopx.supplier.mapper.SupplierMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OrderDetailItemsCatalogSalePriceApplier {

	private final ItemsRepository itemsRepository;
	private final SupplierMapper supplierMapper;

	public OrderDetailItemsCatalogSalePriceApplier(
			ItemsRepository itemsRepository, SupplierMapper supplierMapper) {
		this.itemsRepository = itemsRepository;
		this.supplierMapper = supplierMapper;
	}

	public void apply(long companyId, List<Map<String, Object>> orderItems) {
		if (orderItems == null || orderItems.isEmpty()) {
			return;
		}
		LinkedHashSet<Long> itemIdSet = new LinkedHashSet<>();
		for (Map<String, Object> line : orderItems) {
			Long itemId = parseItemId(line.get("item_id"));
			if (itemId != null && itemId > 0L) {
				itemIdSet.add(itemId);
			}
		}
		if (itemIdSet.isEmpty()) {
			return;
		}
		List<Long> itemIds = new ArrayList<>(itemIdSet);
		List<Items> rows = itemsRepository.listByCompanyAndItemIdsPreservingOrder(companyId, itemIds);
		Map<Long, Items> catalogByItemId = new HashMap<>();
		LinkedHashSet<Long> supplierOperatorIds = new LinkedHashSet<>();
		for (Items row : rows) {
			if (row.getItemId() != null) {
				catalogByItemId.put(row.getItemId(), row);
				Integer sid = row.getSupplierId();
				if (sid != null && sid > 0) {
					supplierOperatorIds.add(sid.longValue());
				}
			}
		}
		Map<Long, String> supplierNameByOperatorId = loadSupplierNames(supplierOperatorIds);
		for (Map<String, Object> line : orderItems) {
			Long itemId = parseItemId(line.get("item_id"));
			if (itemId == null) {
				continue;
			}
			Items catalog = catalogByItemId.get(itemId);
			if (catalog == null) {
				continue;
			}
			if (catalog.getPrice() != null) {
				line.put("sale_price", String.valueOf(catalog.getPrice()));
			}
			line.put("item_holder", "self");
			line.put("supplier_name", "");
			Integer catalogSupplierId = catalog.getSupplierId();
			if (catalogSupplierId != null && catalogSupplierId > 0) {
				line.put("item_holder", "supplier");
				String name = supplierNameByOperatorId.get(catalogSupplierId.longValue());
				if (StringUtils.hasText(name)) {
					line.put("supplier_name", name);
				}
			} else if (catalog.getDistributorId() != null && catalog.getDistributorId() > 0) {
				line.put("item_holder", "distributor");
			}
		}
	}

	private Map<Long, String> loadSupplierNames(LinkedHashSet<Long> operatorIds) {
		if (operatorIds.isEmpty()) {
			return Map.of();
		}
		List<Supplier> suppliers =
				supplierMapper.selectList(
						new LambdaQueryWrapper<Supplier>().in(Supplier::getOperatorId, operatorIds));
		Map<Long, String> out = new HashMap<>();
		for (Supplier s : suppliers) {
			if (s.getOperatorId() != null && StringUtils.hasText(s.getSupplierName())) {
				out.put(s.getOperatorId(), s.getSupplierName());
			}
		}
		return out;
	}

	private static Long parseItemId(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
