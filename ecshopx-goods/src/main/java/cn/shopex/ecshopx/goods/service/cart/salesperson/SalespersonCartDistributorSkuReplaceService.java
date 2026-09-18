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

package cn.shopex.ecshopx.goods.service.cart.salesperson;

import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.distribution.repository.DistributorItemsRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class SalespersonCartDistributorSkuReplaceService {

	private final DistributorItemsRepository distributorItemsRepository;

	public SalespersonCartDistributorSkuReplaceService(DistributorItemsRepository distributorItemsRepository) {
		this.distributorItemsRepository = distributorItemsRepository;
	}

	/**
	 * 与 {@code DistributorItemsRelListCoreService#applyDistributorSkuReplace} 在 {@code replaceApprove=false} 时
	 * 对行字段的写入规则一致。
	 */
	public void apply(long companyId, long distributorId, List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		List<Long> itemIds = rows.stream().map(r -> longVal(r.get("item_id"))).filter(id -> id > 0).distinct().toList();
		List<DistributorItems> rels = distributorItemsRepository.listByDistributorAndItemIds(companyId, distributorId, itemIds);
		Map<Long, DistributorItems> byItem = rels.stream().collect(Collectors.toMap(DistributorItems::getItemId, x -> x, (a, b) -> a));

		for (Map<String, Object> row : rows) {
			long itemId = longVal(row.get("item_id"));
			DistributorItems di = byItem.get(itemId);
			row.put("distributor_id", distributorId);
			if (di != null) {
				row.put("distributor_store", di.getStore() != null ? di.getStore().intValue() : -1);
				boolean totalStore = Boolean.TRUE.equals(di.getIsTotalStore());
				if (!totalStore) {
					if (di.getStore() != null) {
						row.put("store", di.getStore());
					}
					if (di.getPrice() != null) {
						row.put("price", di.getPrice().intValue());
					}
				}
				row.put("goods_can_sale", di.getGoodsCanSale());
				row.put("is_can_sale", di.getIsCanSale());
				row.put("is_total_store", totalStore);
			} else {
				row.put("distributor_store", -1);
				row.put("is_can_sale", false);
				row.put("goods_can_sale", false);
				row.put("is_total_store", false);
			}
		}
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
