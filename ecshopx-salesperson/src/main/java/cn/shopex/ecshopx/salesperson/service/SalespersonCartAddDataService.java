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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.DistributorItems;
import cn.shopex.ecshopx.distribution.mapper.DistributorItemsMapper;
import cn.shopex.ecshopx.salesperson.domain.SalespersonCart;
import cn.shopex.ecshopx.salesperson.domain.SalespersonItemCartRow;
import cn.shopex.ecshopx.salesperson.repository.SalespersonCartRepository;
import cn.shopex.ecshopx.salesperson.repository.SalespersonItemCartReadRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SalespersonCartAddDataService {

	private final SalespersonItemCartReadRepository itemCartReadRepository;

	private final DistributorItemsMapper distributorItemsMapper;

	private final SalespersonCartRepository salespersonCartRepository;

	public SalespersonCartAddDataService(
			SalespersonItemCartReadRepository itemCartReadRepository,
			DistributorItemsMapper distributorItemsMapper,
			SalespersonCartRepository salespersonCartRepository) {
		this.itemCartReadRepository = itemCartReadRepository;
		this.distributorItemsMapper = distributorItemsMapper;
		this.salespersonCartRepository = salespersonCartRepository;
	}

	public Map<String, Object> addCartdata(
			Map<String, Object> filter, Map<String, Object> params, boolean isAccumulate) {
		checkAddCartParams(filter, params);
		Map<String, Object> checkedParams = checkAddCartItems(filter, params);

		long salespersonId = longVal(filter.get("salesperson_id"));
		long itemId = longVal(filter.get("item_id"));
		long distributorId = longVal(filter.get("distributor_id"));
		long companyId = longVal(filter.get("company_id"));
		long num = numFromParams(checkedParams);

		SalespersonCart cartInfo = salespersonCartRepository.getInfo(salespersonId, itemId, distributorId, companyId);

		if (cartInfo == null && num <= 0) {
			throw new ResourceException("加入购物车的数据有误");
		}
		if (cartInfo != null && num <= 0) {
			salespersonCartRepository.deleteByBusinessKey(salespersonId, itemId, distributorId, companyId);
			return new LinkedHashMap<>();
		}
		if (cartInfo != null) {
			long existingNum = cartInfo.getNum() == null ? 0L : cartInfo.getNum();
			long newNum = isAccumulate ? existingNum + num : num;
			Map<String, Object> updateData = new LinkedHashMap<>();
			updateData.put("num", newNum);
			if (checkedParams.containsKey("is_checked")) {
				updateData.put("is_checked", checkedParams.get("is_checked"));
			}
			return salespersonCartRepository.updateOneBy(filter, updateData);
		}

		Map<String, Object> createData = new LinkedHashMap<>(filter);
		createData.putAll(checkedParams);
		return salespersonCartRepository.create(createData);
	}

	private void checkAddCartParams(Map<String, Object> filter, Map<String, Object> params) {
		Map<String, Object> merged = new LinkedHashMap<>(filter);
		merged.putAll(params);
		requirePresent(merged, "salesperson_id", "导购员信息有误");
		requirePresent(merged, "distributor_id", "店铺信息有误");
		requirePresent(merged, "company_id", "企业信息有误");
		requirePresent(merged, "item_id", "购物车商品有误");
	}

	private Map<String, Object> checkAddCartItems(Map<String, Object> filter, Map<String, Object> params) {
		long num = numFromParams(params);
		long companyId = longVal(filter.get("company_id"));
		long itemId = longVal(filter.get("item_id"));
		long distributorId = longVal(filter.get("distributor_id"));

		CheckedItem checked = resolveCheckedItem(companyId, itemId, distributorId);
		if (checked.companyId != companyId) {
			throw new ResourceException("无效商品");
		}
		if (checked.store < num) {
			throw new ResourceException("库存不足");
		}
		Map<String, Object> out = new LinkedHashMap<>(params);
		out.put("special_type", checked.specialType);
		return out;
	}

	private CheckedItem resolveCheckedItem(long companyId, long itemId, long distributorId) {
		if (distributorId != 0L) {
			SalespersonItemCartRow item = itemCartReadRepository.getByItemIdAndCompany(itemId, companyId);
			if (item == null) {
				throw new ResourceException("无效商品");
			}
			DistributorItems distRow = findDistributorItemRow(companyId, itemId, distributorId);
			if (distRow == null) {
				long itemDist = item.getDistributorId() == null ? 0L : item.getDistributorId().longValue();
				if (itemDist != 0L && itemDist == distributorId) {
					return snapshotFromItem(item);
				}
				throw new ResourceException("无效商品");
			}
			return mergeShopItem(item, distRow);
		}
		SalespersonItemCartRow item = itemCartReadRepository.getByItemIdAndCompany(itemId, companyId);
		if (item == null) {
			throw new ResourceException("无效商品");
		}
		return snapshotFromItem(item);
	}

	private DistributorItems findDistributorItemRow(long companyId, long itemId, long distributorId) {
		if (distributorId <= 0L) {
			return null;
		}
		return distributorItemsMapper.selectOne(
				new LambdaQueryWrapper<DistributorItems>()
						.eq(DistributorItems::getCompanyId, companyId)
						.eq(DistributorItems::getDistributorId, distributorId)
						.eq(DistributorItems::getItemId, itemId)
						.last("LIMIT 1"));
	}

	private static CheckedItem mergeShopItem(SalespersonItemCartRow item, DistributorItems distRow) {
		long companyId = item.getCompanyId() == null ? 0L : item.getCompanyId();
		boolean replaceStore =
				distRow.getIsTotalStore() == null || !Boolean.TRUE.equals(distRow.getIsTotalStore());
		long store;
		if (replaceStore) {
			store = distRow.getStore() == null ? 0L : distRow.getStore();
		} else {
			store = item.getStore() == null ? 0L : item.getStore().longValue();
		}
		String specialType = normalizeSpecialType(item.getSpecialType());
		return new CheckedItem(companyId, store, specialType);
	}

	private static CheckedItem snapshotFromItem(SalespersonItemCartRow item) {
		long companyId = item.getCompanyId() == null ? 0L : item.getCompanyId();
		long store = item.getStore() == null ? 0L : item.getStore().longValue();
		return new CheckedItem(companyId, store, normalizeSpecialType(item.getSpecialType()));
	}

	private static String normalizeSpecialType(String s) {
		if (s == null || s.isBlank()) {
			return "normal";
		}
		return s;
	}

	private static void requirePresent(Map<String, Object> map, String key, String msg) {
		if (!map.containsKey(key)) {
			throw new ResourceException(msg);
		}
		Object v = map.get(key);
		if (v == null) {
			throw new ResourceException(msg);
		}
		if (v instanceof String s && s.isEmpty()) {
			throw new ResourceException(msg);
		}
		if (v instanceof Collection<?> c && c.isEmpty()) {
			throw new ResourceException(msg);
		}
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long numFromParams(Map<String, Object> params) {
		Object n = params.get("num");
		if (n == null) {
			return 0L;
		}
		if (n instanceof Number num) {
			return num.longValue();
		}
		String s = n.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static final class CheckedItem {
		private final long companyId;
		private final long store;
		private final String specialType;

		private CheckedItem(long companyId, long store, String specialType) {
			this.companyId = companyId;
			this.store = store;
			this.specialType = specialType;
		}
	}
}
