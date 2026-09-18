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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorListQueryService {

	private final DistributorMapper distributorMapper;

	public DistributorListQueryService(DistributorMapper distributorMapper) {
		this.distributorMapper = distributorMapper;
	}

	/**
	 * Maps trimmed shop_code values to distributor_id for the company (includes rows regardless of {@code is_valid}),
	 * aligned with the legacy distributor original list semantics.
	 */
	public Map<String, Long> mapShopCodeToDistributorId(long companyId, Collection<String> shopCodes) {
		if (shopCodes == null || shopCodes.isEmpty()) {
			return Collections.emptyMap();
		}
		LinkedHashSet<String> codes = new LinkedHashSet<>();
		for (String sc : shopCodes) {
			if (sc == null) {
				continue;
			}
			String t = sc.trim();
			if (!t.isEmpty()) {
				codes.add(t);
			}
		}
		if (codes.isEmpty()) {
			return Collections.emptyMap();
		}
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId)
				.in(Distributor::getShopCode, codes)
				.select(Distributor::getShopCode, Distributor::getDistributorId);
		List<Distributor> rows = distributorMapper.selectList(w);
		return rows.stream()
				.filter(d -> d.getShopCode() != null && d.getDistributorId() != null)
				.collect(
						Collectors.toMap(
								d -> String.valueOf(d.getShopCode()).trim(),
								Distributor::getDistributorId,
								(a, b) -> a,
								LinkedHashMap::new));
	}

	/**
	 * 商户下「有效」店铺 id（is_valid = true）。
	 */
	public List<Long> listValidDistributorIdsForMerchant(long companyId, long merchantId) {
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId).eq(Distributor::getMerchantId, merchantId).eq(Distributor::getIsValid, "true");
		return distributorMapper.selectList(w).stream().map(Distributor::getDistributorId).collect(Collectors.toList());
	}

	/**
	 * 商户下有效店铺 id，条数上限与后台列表场景一致。
	 */
	public List<Long> listValidDistributorIdsForMerchantCapped(long companyId, long merchantId, int maxCount) {
		if (maxCount <= 0) {
			throw new IllegalArgumentException("maxCount must be positive");
		}
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId)
				.eq(Distributor::getMerchantId, merchantId)
				.eq(Distributor::getIsValid, "true")
				.last("LIMIT " + maxCount);
		return distributorMapper.selectList(w).stream().map(Distributor::getDistributorId).collect(Collectors.toList());
	}

	/**
	 * 商户下有效店铺 id，按创建时间倒序，条数上限由调用方指定（运营端导购列表等场景）。
	 */
	public List<Long> listValidDistributorIdsForMerchantOrdered(long companyId, long merchantId, int maxCount) {
		if (maxCount <= 0) {
			throw new IllegalArgumentException("maxCount must be positive");
		}
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId)
				.eq(Distributor::getMerchantId, merchantId)
				.eq(Distributor::getIsValid, "true")
				.orderByDesc(Distributor::getCreated)
				.last("LIMIT " + maxCount);
		return distributorMapper.selectList(w).stream().map(Distributor::getDistributorId).collect(Collectors.toList());
	}

	/**
	 * 公司下有效店铺 id（{@code is_valid = 'true'}），与推广商品类目链路一致。
	 */
	public List<Long> listValidDistributorIdsForCompany(long companyId) {
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId).eq(Distributor::getIsValid, "true");
		return distributorMapper.selectList(w).stream().map(Distributor::getDistributorId).collect(Collectors.toList());
	}

	/**
	 * 公司下全部店铺 id（含禁用，用于 all_distributor）。
	 */
	public List<Long> listAllDistributorIdsForCompany(long companyId) {
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId);
		return distributorMapper.selectList(w).stream().map(Distributor::getDistributorId).collect(Collectors.toList());
	}

	public List<Long> intersectSorted(List<Long> a, List<Long> b) {
		if (a == null || a.isEmpty() || b == null || b.isEmpty()) {
			return List.of();
		}
		Set<Long> bs = new LinkedHashSet<>(b);
		List<Long> out = new ArrayList<>();
		for (Long x : a) {
			if (x != null && bs.contains(x)) {
				out.add(x);
			}
		}
		return out;
	}

	/**
	 * 按公司 + 店铺名称模糊匹配（{@code %}/{@code _} 已转义）返回店铺 id 列表。
	 */
	public List<Long> listDistributorIdsByCompanyAndNameContains(long companyId, String nameContains) {
		if (!StringUtils.hasText(nameContains)) {
			return List.of();
		}
		String escaped = escapeSqlLike(nameContains.trim());
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId)
				.apply("name LIKE CONCAT('%', {0}, '%') ESCAPE '\\\\'", escaped)
				.select(Distributor::getDistributorId);
		return distributorMapper.selectList(w).stream().map(Distributor::getDistributorId).toList();
	}

	/**
	 * 有效店铺（{@code is_valid = 'true'}），按创建时间倒序；{@code shopCodes} 非空时限定编号集合，否则取公司下全部有效店铺。
	 */
	public List<Distributor> listValidByCompanyAndShopCodesOrderedByCreatedDesc(long companyId, List<String> shopCodes) {
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId).eq(Distributor::getIsValid, "true");
		if (shopCodes != null && !shopCodes.isEmpty()) {
			w.in(Distributor::getShopCode, shopCodes);
		}
		w.orderByDesc(Distributor::getCreated);
		return distributorMapper.selectList(w);
	}

	public List<Distributor> listByIdsAndCompany(long companyId, List<Long> distributorIds) {
		if (distributorIds == null || distributorIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId).in(Distributor::getDistributorId, distributorIds);
		return distributorMapper.selectList(w);
	}

	/**
	 * 公司下未删除店铺：{@code is_valid} 为 null 或经 trim/lowercase 后不为 {@code delete}。
	 */
	public List<Long> listNonDeletedDistributorIdsForCompany(long companyId) {
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId)
				.and(q -> q.isNull(Distributor::getIsValid).or().apply("LOWER(TRIM(is_valid)) <> {0}", "delete"));
		return distributorMapper.selectList(w).stream().map(Distributor::getDistributorId).toList();
	}

	/**
	 * 按公司 + 店铺 id 集分页查询名称与编号列，页码从 1 开始。
	 */
	public List<Map<String, Object>> listDistributionNameRowsByIdsPaged(long companyId, List<Long> distributorIds, int page,
			int pageSize) {
		if (distributorIds == null || distributorIds.isEmpty()) {
			return List.of();
		}
		int p = Math.max(page, 1);
		int ps = pageSize;
		if (ps <= 0) {
			return List.of();
		}
		long offset = (long) (p - 1) * ps;
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId)
				.in(Distributor::getDistributorId, distributorIds)
				.select(Distributor::getName, Distributor::getShopCode, Distributor::getDistributorId)
				.last("LIMIT " + offset + ", " + ps);
		List<Distributor> list = distributorMapper.selectList(w);
		List<Map<String, Object>> out = new ArrayList<>();
		for (Distributor d : list) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("name", d.getName());
			m.put("shop_code", d.getShopCode());
			m.put("distributor_id", d.getDistributorId());
			out.add(m);
		}
		return out;
	}

	/**
	 * AdaPay 管理端：按公司查询店铺 id 与名称，分页与排序默认与后台「原始列表」场景一致（第 1 页、每页 100 条、按创建时间倒序）。
	 */
	public List<Map<String, Object>> listDistributorIdAndNameByCompanyPaged(long companyId, int page, int pageSize) {
		int p = Math.max(page, 1);
		int ps = pageSize;
		if (ps <= 0) {
			return List.of();
		}
		long offset = (long) (p - 1) * ps;
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId)
				.orderByDesc(Distributor::getCreated)
				.select(Distributor::getDistributorId, Distributor::getName)
				.last("LIMIT " + offset + ", " + ps);
		List<Distributor> rows = distributorMapper.selectList(w);
		List<Map<String, Object>> out = new ArrayList<>();
		for (Distributor entity : rows) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("distributor_id", entity.getDistributorId());
			m.put("name", entity.getName());
			out.add(m);
		}
		return out;
	}

	public static String escapeSqlLike(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	/**
	 * Lists distributors where {@code distributor_id} column equals the raw string (trimmed), including comma-separated
	 * values stored as a single column value; ordered by {@code created} descending, capped at 1000 rows.
	 */
	public List<Distributor> listByCompanyAndDistributorIdRawExact(long companyId, String distributorIdRaw) {
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<>();
		w.eq(Distributor::getCompanyId, companyId)
				.apply("distributor_id = {0}", distributorIdRaw == null ? "" : distributorIdRaw.trim())
				.orderByDesc(Distributor::getCreated)
				.last("LIMIT 1000");
		return distributorMapper.selectList(w);
	}
}
