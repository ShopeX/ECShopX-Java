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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * 供应商列表「商品ID」({@code main_item_id})：未同步或未审核通过为 0；已同步且 {@code items.audit_status=approved} 时返回对应 {@code items.item_id}。
 */
public final class SupplierLinkedMainItemIdResolver {

	private SupplierLinkedMainItemIdResolver() {
	}

	/**
	 * @param scopeDistributorId {@code 0} 取平台池（{@code distributor_id=0}）；{@code >0} 取该店铺副本
	 */
	public static Map<Integer, Long> resolve(ItemsRepository itemsRepository, long companyId, Collection<Long> supplierSkuIds,
			long scopeDistributorId) {
		if (supplierSkuIds == null || supplierSkuIds.isEmpty()) {
			return Map.of();
		}
		List<Items> linked = itemsRepository.listByCompanyIdAndSupplierItemIds(companyId, supplierSkuIds);
		Map<Integer, Long> out = new HashMap<>();
		for (Items row : linked) {
			if (!isApprovedLinkedItem(row)) {
				continue;
			}
			Integer supplierItemId = row.getSupplierItemId();
			Long itemId = row.getItemId();
			if (supplierItemId == null || supplierItemId <= 0 || itemId == null || itemId <= 0) {
				continue;
			}
			int dist = row.getDistributorId() != null ? row.getDistributorId() : 0;
			if (scopeDistributorId > 0) {
				if (dist != (int) scopeDistributorId) {
					continue;
				}
			} else if (dist != 0) {
				continue;
			}
			out.putIfAbsent(supplierItemId, itemId);
		}
		return out;
	}

	static boolean isApprovedLinkedItem(Items row) {
		if (row == null) {
			return false;
		}
		String audit = row.getAuditStatus();
		return StringUtils.hasText(audit) && "approved".equalsIgnoreCase(audit.trim());
	}

	static long resolveScopeDistributorId(Map<String, Object> jwt, Map<String, Object> params) {
		if (jwt != null && "distributor".equalsIgnoreCase(str(jwt.get("operator_type")))) {
			long distributorId = toLong(jwt.get("distributor_id"));
			if (distributorId > 0) {
				return distributorId;
			}
		}
		if (params != null) {
			Integer eq = toIntegerBoxed(params.get("distributor_id_eq"));
			if (eq != null && eq > 0) {
				return eq.longValue();
			}
			Object dinRaw = params.get("distributor_id_in");
			if (dinRaw instanceof List<?> din && din.size() == 1) {
				Object only = din.get(0);
				if (only instanceof Number n && n.intValue() > 0) {
					return n.longValue();
				}
			}
		}
		return 0L;
	}

	private static Integer toIntegerBoxed(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}
}
