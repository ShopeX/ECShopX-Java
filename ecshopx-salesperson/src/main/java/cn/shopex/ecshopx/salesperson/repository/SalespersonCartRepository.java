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

package cn.shopex.ecshopx.salesperson.repository;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.salesperson.domain.SalespersonCart;
import cn.shopex.ecshopx.salesperson.mapper.SalespersonCartMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class SalespersonCartRepository {

	private final SalespersonCartMapper mapper;

	public SalespersonCartRepository(SalespersonCartMapper mapper) {
		this.mapper = mapper;
	}

	public SalespersonCart getInfo(long salespersonId, long itemId, long distributorId, long companyId) {
		LambdaQueryWrapper<SalespersonCart> w = new LambdaQueryWrapper<>();
		w.eq(SalespersonCart::getSalespersonId, salespersonId)
				.eq(SalespersonCart::getItemId, itemId)
				.eq(SalespersonCart::getDistributorId, distributorId)
				.eq(SalespersonCart::getCompanyId, companyId)
				.last("LIMIT 1");
		return mapper.selectOne(w);
	}

	public void deleteByBusinessKey(long salespersonId, long itemId, long distributorId, long companyId) {
		LambdaQueryWrapper<SalespersonCart> w = new LambdaQueryWrapper<>();
		w.eq(SalespersonCart::getSalespersonId, salespersonId)
				.eq(SalespersonCart::getItemId, itemId)
				.eq(SalespersonCart::getDistributorId, distributorId)
				.eq(SalespersonCart::getCompanyId, companyId);
		mapper.delete(w);
	}

	public List<Map<String, Object>> listForGetCartdataList(long companyId, long salespersonId, long distributorId) {
		return listForGetCartdataList(companyId, salespersonId, distributorId, false);
	}

	public List<Map<String, Object>> listForGetCartdataList(
			long companyId, long salespersonId, long distributorId, boolean onlyChecked) {
		LambdaQueryWrapper<SalespersonCart> w = new LambdaQueryWrapper<>();
		w.eq(SalespersonCart::getCompanyId, companyId)
				.eq(SalespersonCart::getSalespersonId, salespersonId)
				.eq(SalespersonCart::getDistributorId, distributorId);
		if (onlyChecked) {
			w.eq(SalespersonCart::getIsChecked, true);
		}
		w.orderByAsc(SalespersonCart::getCartId);
		List<SalespersonCart> rows = mapper.selectList(w);
		if (rows == null || rows.isEmpty()) {
			return Collections.emptyList();
		}
		return rows.stream().map(this::toColumnMapWithShopId).collect(Collectors.toList());
	}

	public Map<String, Object> countCart(long companyId, long salespersonId) {
		Map<String, Object> row = mapper.selectCartCountAggregate(companyId, salespersonId);
		int cartCount;
		int itemCount;
		if (row == null || !row.containsKey("cart_count")) {
			cartCount = 0;
			itemCount = 0;
		} else {
			cartCount = aggregateCountInt(row.get("cart_count"));
			itemCount = aggregateCountInt(row.get("item_count"));
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("cart_count", cartCount);
		out.put("item_count", itemCount);
		return out;
	}

	public void updateIsCheckedByCompanyAndCartIds(long companyId, List<Long> cartIds, boolean isChecked) {
		LambdaUpdateWrapper<SalespersonCart> wrapper = new LambdaUpdateWrapper<>();
		wrapper.eq(SalespersonCart::getCompanyId, companyId);
		if (cartIds.size() == 1) {
			wrapper.eq(SalespersonCart::getCartId, cartIds.get(0));
		} else {
			wrapper.in(SalespersonCart::getCartId, cartIds);
		}
		wrapper.set(SalespersonCart::getIsChecked, isChecked);
		mapper.update(null, wrapper);
	}

	public Map<String, Object> create(Map<String, Object> data) {
		SalespersonCart row = new SalespersonCart();
		row.setSalespersonId(longFromObject(data.get("salesperson_id"), 0L));
		row.setItemId(longFromObject(data.get("item_id"), 0L));
		row.setCompanyId(longFromObject(data.get("company_id"), 0L));
		row.setDistributorId(longFromObject(data.get("distributor_id"), 0L));
		row.setNum(longFromObject(data.get("num"), 1L));
		row.setIsChecked(booleanFromObject(data.get("is_checked"), true));
		String pkg = stringFromObject(data.get("package_items"));
		row.setPackageItems(pkg != null && !pkg.isEmpty() ? pkg : "");
		row.setSpecialType(normalizeSpecialType(data.get("special_type")));
		mapper.insert(row);
		return toColumnMap(row);
	}

	public Map<String, Object> updateOneBy(Map<String, Object> filter, Map<String, Object> data) {
		SalespersonCart existing =
				getInfo(
						longFromObject(filter.get("salesperson_id"), 0L),
						longFromObject(filter.get("item_id"), 0L),
						longFromObject(filter.get("distributor_id"), 0L),
						longFromObject(filter.get("company_id"), 0L));
		if (existing == null) {
			throw new ResourceException("未查询到更新数据");
		}
		if (data.containsKey("num")) {
			existing.setNum(longFromObject(data.get("num"), 0L));
		}
		if (data.containsKey("is_checked")) {
			existing.setIsChecked(booleanFromObject(data.get("is_checked"), Boolean.TRUE.equals(existing.getIsChecked())));
		}
		int n = mapper.updateById(existing);
		if (n <= 0) {
			throw new ResourceException("未查询到更新数据");
		}
		SalespersonCart updated = mapper.selectById(existing.getCartId());
		return toColumnMap(updated);
	}

	private Map<String, Object> toColumnMapWithShopId(SalespersonCart e) {
		Map<String, Object> m = toColumnMap(e);
		Long did = e.getDistributorId();
		m.put("shop_id", did != null ? did : 0L);
		return m;
	}

	private static Map<String, Object> toColumnMap(SalespersonCart e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("cart_id", e.getCartId());
		m.put("salesperson_id", e.getSalespersonId());
		m.put("item_id", e.getItemId());
		m.put("package_items", e.getPackageItems());
		m.put("num", e.getNum());
		m.put("company_id", e.getCompanyId());
		m.put("is_checked", e.getIsChecked());
		m.put("distributor_id", e.getDistributorId());
		m.put("special_type", e.getSpecialType() != null ? e.getSpecialType() : "normal");
		return m;
	}

	private static int aggregateCountInt(Object v) {
		if (v == null) {
			return 0;
		}
		if (v instanceof Number n) {
			long lv = n.longValue();
			if (lv == 0) {
				return 0;
			}
			return (int) Math.min(Math.max(lv, (long) Integer.MIN_VALUE), (long) Integer.MAX_VALUE);
		}
		if (v instanceof String s) {
			s = s.trim();
			if (s.isEmpty() || "0".equals(s)) {
				return 0;
			}
			long lv;
			try {
				lv = Long.parseLong(s);
			} catch (NumberFormatException e) {
				return 0;
			}
			if (lv == 0) {
				return 0;
			}
			return (int) Math.min(Math.max(lv, (long) Integer.MIN_VALUE), (long) Integer.MAX_VALUE);
		}
		String s = v.toString().trim();
		if (s.isEmpty() || "0".equals(s)) {
			return 0;
		}
		long lv;
		try {
			lv = Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0;
		}
		if (lv == 0) {
			return 0;
		}
		return (int) Math.min(Math.max(lv, (long) Integer.MIN_VALUE), (long) Integer.MAX_VALUE);
	}

	private static long longFromObject(Object v, long defaultIfNull) {
		if (v == null) {
			return defaultIfNull;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return defaultIfNull;
		}
		return Long.parseLong(s);
	}

	private static String stringFromObject(Object v) {
		return v == null ? null : v.toString();
	}

	private static String normalizeSpecialType(Object v) {
		if (v == null) {
			return "normal";
		}
		if (v instanceof String s) {
			return s.isBlank() ? "normal" : s;
		}
		String s = v.toString().trim();
		return s.isEmpty() ? "normal" : s;
	}

	private static boolean booleanFromObject(Object v, boolean defaultVal) {
		if (v == null) {
			return defaultVal;
		}
		if (v instanceof Boolean b) {
			return b;
		}
		if (v instanceof Number n) {
			return n.intValue() != 0;
		}
		String s = v.toString().trim();
		if ("1".equals(s) || "true".equalsIgnoreCase(s)) {
			return true;
		}
		if ("0".equals(s) || "false".equalsIgnoreCase(s)) {
			return false;
		}
		return defaultVal;
	}
}
