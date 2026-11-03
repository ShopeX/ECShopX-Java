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

package cn.shopex.ecshopx.companys.service.posaccount;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.companys.domain.PosStoreAccount;
import cn.shopex.ecshopx.companys.mapper.DistributionDistributorSelfReadMapper;
import cn.shopex.ecshopx.companys.mapper.PosStoreAccountMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PosStoreAccountQueryService {

	private final PosStoreAccountMapper mapper;
	private final DistributionDistributorSelfReadMapper shopMapper;

	public PosStoreAccountQueryService(
			PosStoreAccountMapper mapper, DistributionDistributorSelfReadMapper shopMapper) {
		this.mapper = mapper;
		this.shopMapper = shopMapper;
	}

	public Map<String, Object> list(
			Map<String, Object> jwt,
			String keyword,
			String role,
			Long distributorId,
			String status,
			Integer page,
			Integer pageSize) {
		long companyId = PosStoreAccountScope.companyId(jwt);
		boolean admin = PosStoreAccountScope.isAdmin(jwt);
		Set<Long> allowed = PosStoreAccountScope.requireReadableShops(jwt);
		int p = page == null || page < 1 ? 1 : page;
		int ps = pageSize == null || pageSize < 1 ? 20 : pageSize;
		Map<String, Object> stats = stats(companyId, admin, allowed);
		Page<PosStoreAccount> mp =
				mapper.selectPage(new Page<>(p, ps), listWrapper(jwt, keyword, role, distributorId, status));
		List<PosStoreAccount> records = mp.getRecords();
		Map<Long, Map<String, Object>> shops = loadShops(companyId, records);
		List<Map<String, Object>> list = new ArrayList<>();
		for (PosStoreAccount row : records) {
			list.add(toPublicRow(row, shops.get(row.getDistributorId())));
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("list", list);
		out.put("total_count", mp.getTotal());
		out.put("page", p);
		out.put("pageSize", ps);
		out.put("stats", stats);
		return out;
	}

	private LambdaQueryWrapper<PosStoreAccount> listWrapper(
			Map<String, Object> jwt,
			String keyword,
			String role,
			Long distributorId,
			String status) {
		long companyId = PosStoreAccountScope.companyId(jwt);
		boolean admin = PosStoreAccountScope.isAdmin(jwt);
		Set<Long> allowed = PosStoreAccountScope.requireReadableShops(jwt);
		if (distributorId != null && distributorId > 0) {
			PosStoreAccountScope.assertShopAllowed(jwt, distributorId);
		}
		LambdaQueryWrapper<PosStoreAccount> w = scoped(companyId, admin, allowed);
		if (distributorId != null && distributorId > 0) {
			w.eq(PosStoreAccount::getDistributorId, distributorId);
		}
		if (StringUtils.hasText(role)) {
			w.eq(PosStoreAccount::getRole, role.trim());
		}
		if (StringUtils.hasText(status)) {
			w.eq(PosStoreAccount::getStatus, status.trim());
		}
		if (StringUtils.hasText(keyword)) {
			String k = keyword.trim();
			w.and(q -> q.like(PosStoreAccount::getName, k)
					.or()
					.like(PosStoreAccount::getEmployeeNo, k)
					.or()
					.like(PosStoreAccount::getEmail, k)
					.or()
					.like(PosStoreAccount::getMobile, k));
		}
		w.orderByDesc(PosStoreAccount::getId);
		return w;
	}

	List<PosStoreAccount> listAll(
			Map<String, Object> jwt, String keyword, String role, Long distributorId, String status) {
		return mapper.selectList(listWrapper(jwt, keyword, role, distributorId, status));
	}

	public Map<String, Object> detail(Map<String, Object> jwt, long id) {
		PosStoreAccount row = requireReadable(jwt, id);
		Map<Long, Map<String, Object>> shops =
				loadShops(row.getCompanyId(), List.of(row));
		return toPublicRow(row, shops.get(row.getDistributorId()));
	}

	/**
	 * 订单列表/详情批量回填 POS 经手账号信息（不含密码）。id≤0 或跨公司忽略。
	 *
	 * @return id → 可扩展公开字段 map
	 */
	public Map<Long, Map<String, Object>> mapOrderInfoByIds(long companyId, Collection<Long> ids) {
		if (companyId < 1 || ids == null || ids.isEmpty()) {
			return Map.of();
		}
		List<Long> positive =
				ids.stream().filter(id -> id != null && id > 0L).distinct().collect(Collectors.toList());
		if (positive.isEmpty()) {
			return Map.of();
		}
		List<PosStoreAccount> rows =
				mapper.selectList(
						new LambdaQueryWrapper<PosStoreAccount>()
								.eq(PosStoreAccount::getCompanyId, companyId)
								.in(PosStoreAccount::getId, positive));
		if (rows == null || rows.isEmpty()) {
			return Map.of();
		}
		Map<Long, Map<String, Object>> out = new LinkedHashMap<>();
		for (PosStoreAccount row : rows) {
			if (row.getId() != null) {
				out.put(row.getId(), toOrderInfoRow(row));
			}
		}
		return out;
	}

	/** 订单侧 {@code pos_store_account_info} 对象；无则空对象（一单一账号，字段可扩展）。 */
	public static Map<String, Object> asInfoMap(Map<String, Object> infoOrNull) {
		if (infoOrNull == null || infoOrNull.isEmpty()) {
			return Map.of();
		}
		return infoOrNull;
	}

	/** 订单回填字段；后续扩展加在这里即可。 */
	static Map<String, Object> toOrderInfoRow(PosStoreAccount row) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", row.getId());
		m.put("employee_no", blankToEmpty(row.getEmployeeNo()));
		m.put("name", blankToEmpty(row.getName()));
		m.put("mobile", blankToEmpty(row.getMobile()));
		m.put("role", blankToEmpty(row.getRole()));
		m.put("distributor_id", row.getDistributorId() == null ? 0L : row.getDistributorId());
		m.put("status", blankToEmpty(row.getStatus()));
		return m;
	}

	PosStoreAccount requireReadable(Map<String, Object> jwt, long id) {
		PosStoreAccount row = mapper.selectById(id);
		long companyId = PosStoreAccountScope.companyId(jwt);
		if (row == null || row.getCompanyId() == null || row.getCompanyId() != companyId) {
			throw new ForbiddenException("无访问权限");
		}
		PosStoreAccountScope.assertShopAllowed(jwt, row.getDistributorId());
		return row;
	}

	private Map<String, Object> stats(long companyId, boolean admin, Set<Long> allowed) {
		QueryWrapper<PosStoreAccount> w = new QueryWrapper<>();
		w.select("status", "count(*) AS cnt");
		w.eq("company_id", companyId);
		if (!admin) {
			w.in("distributor_id", allowed);
		}
		w.groupBy("status");
		long enabled = 0;
		long disabled = 0;
		long resigned = 0;
		List<Map<String, Object>> rows = mapper.selectMaps(w);
		if (rows != null) {
			for (Map<String, Object> row : rows) {
				String status = row.get("status") == null ? "" : String.valueOf(row.get("status"));
				long cnt = toLong(row.get("cnt"));
				switch (status) {
					case "enabled" -> enabled = cnt;
					case "disabled" -> disabled = cnt;
					case "resigned" -> resigned = cnt;
					default -> {
					}
				}
			}
		}
		Map<String, Object> stats = new LinkedHashMap<>();
		stats.put("total", enabled + disabled + resigned);
		stats.put("enabled", enabled);
		stats.put("disabled", disabled);
		stats.put("resigned", resigned);
		return stats;
	}

	private static LambdaQueryWrapper<PosStoreAccount> scoped(
			long companyId, boolean admin, Set<Long> allowed) {
		LambdaQueryWrapper<PosStoreAccount> w = new LambdaQueryWrapper<>();
		w.eq(PosStoreAccount::getCompanyId, companyId);
		if (!admin) {
			w.in(PosStoreAccount::getDistributorId, allowed);
		}
		return w;
	}

	private Map<Long, Map<String, Object>> loadShops(long companyId, List<PosStoreAccount> rows) {
		if (rows == null || rows.isEmpty()) {
			return Collections.emptyMap();
		}
		List<Long> ids = new ArrayList<>();
		for (PosStoreAccount row : rows) {
			if (row.getDistributorId() != null && row.getDistributorId() > 0) {
				ids.add(row.getDistributorId());
			}
		}
		if (ids.isEmpty()) {
			return Collections.emptyMap();
		}
		List<Map<String, Object>> found = shopMapper.listDistributorNamesByCompanyAndIds(companyId, ids);
		Map<Long, Map<String, Object>> out = new HashMap<>();
		if (found == null) {
			return out;
		}
		for (Map<String, Object> shop : found) {
			Object id = shop.get("distributor_id");
			if (id instanceof Number n) {
				out.put(n.longValue(), shop);
			}
		}
		return out;
	}

	static Map<String, Object> toPublicRow(PosStoreAccount row, Map<String, Object> shop) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", row.getId());
		m.put("employee_no", blankToEmpty(row.getEmployeeNo()));
		m.put("name", blankToEmpty(row.getName()));
		m.put("email", blankToEmpty(row.getEmail()));
		m.put("mobile", blankToEmpty(row.getMobile()));
		m.put("role", blankToEmpty(row.getRole()));
		m.put("distributor_id", row.getDistributorId());
		m.put("shop_code", shop == null ? "" : blankToEmpty(shop.get("shop_code")));
		m.put("shop_name", shop == null ? "" : blankToEmpty(shop.get("name")));
		m.put("status", blankToEmpty(row.getStatus()));
		m.put("created", row.getCreated());
		m.put("updated", row.getUpdated());
		return m;
	}

	private static String blankToEmpty(Object v) {
		return v == null ? "" : v.toString();
	}

	private static long toLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v == null) {
			return 0L;
		}
		try {
			return Long.parseLong(v.toString());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
