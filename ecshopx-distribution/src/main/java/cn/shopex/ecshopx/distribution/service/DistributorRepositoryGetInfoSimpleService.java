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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Loads a single distributor row for API payloads. Multi-language overlay is not applied; {@code regions_id} and
 * {@code regions} are shaped only by {@link DistributorRowMaps#toApiRow}.
 */
@Service
public class DistributorRepositoryGetInfoSimpleService {

	/**
	 * {@code is_valid} may be stored as tinyint {@code 1} or as string {@code true}/{@code 1}; both mean an active store.
	 */
	private static final String SQL_IS_VALID_ACTIVE =
			"(is_valid = 1 OR LOWER(TRIM(CAST(is_valid AS CHAR))) IN ('true','1'))";

	public record ResolvedDistributorForShop(
			Distributor distributor, boolean useVirtualMainDistributorIdInClientMap) {
	}

	private final DistributorMapper distributorMapper;
	private final ObjectMapper objectMapper;

	public DistributorRepositoryGetInfoSimpleService(DistributorMapper distributorMapper, ObjectMapper objectMapper) {
		this.distributorMapper = distributorMapper;
		this.objectMapper = objectMapper;
	}

	/**
	 * Resolves {@code shop_code} for an active distributor row (see {@link #SQL_IS_VALID_ACTIVE}).
	 */
	public Optional<String> findShopCodeForValidDistributor(long companyId, long distributorId) {
		if (distributorId <= 0L) {
			return Optional.empty();
		}
		Distributor entity = distributorMapper.selectOne(new LambdaQueryWrapper<Distributor>()
				.eq(Distributor::getCompanyId, companyId)
				.eq(Distributor::getDistributorId, distributorId)
				.apply(SQL_IS_VALID_ACTIVE)
				.last("LIMIT 1"));
		if (entity == null) {
			return Optional.empty();
		}
		String code = entity.getShopCode();
		if (!StringUtils.hasText(code)) {
			return Optional.empty();
		}
		return Optional.of(code.trim());
	}

	public Map<String, Object> getInfoSimpleByShopCode(long companyId, String shopCode) {
		if (!StringUtils.hasText(shopCode)) {
			return Collections.emptyMap();
		}
		Distributor entity = distributorMapper.selectOne(new LambdaQueryWrapper<Distributor>()
				.eq(Distributor::getCompanyId, companyId)
				.eq(Distributor::getShopCode, shopCode.trim())
				.last("LIMIT 1"));
		if (entity == null) {
			return Collections.emptyMap();
		}
		return new LinkedHashMap<>(DistributorRowMaps.toApiRow(entity, objectMapper));
	}

	public Map<String, Object> getInfoSimpleByDistributorId(long companyId, long distributorId) {
		if (distributorId <= 0L) {
			return Collections.emptyMap();
		}
		Distributor entity = distributorMapper.selectOne(new LambdaQueryWrapper<Distributor>()
				.eq(Distributor::getCompanyId, companyId)
				.eq(Distributor::getDistributorId, distributorId)
				.last("LIMIT 1"));
		if (entity == null) {
			return Collections.emptyMap();
		}
		return new LinkedHashMap<>(DistributorRowMaps.toApiRow(entity, objectMapper));
	}

	/**
	 * Batch name lookup for admin member list display fields. Missing ids are omitted (callers default to "").
	 */
	public Map<Long, String> getNameByDistributorIds(long companyId, Collection<Long> distributorIds) {
		if (distributorIds == null || distributorIds.isEmpty()) {
			return Collections.emptyMap();
		}
		List<Long> ids =
				distributorIds.stream().filter(Objects::nonNull).filter(id -> id > 0L).distinct().toList();
		if (ids.isEmpty()) {
			return Collections.emptyMap();
		}
		List<Distributor> rows =
				distributorMapper.selectList(
						new LambdaQueryWrapper<Distributor>()
								.eq(Distributor::getCompanyId, companyId)
								.in(Distributor::getDistributorId, ids)
								.select(Distributor::getDistributorId, Distributor::getName));
		Map<Long, String> out = new LinkedHashMap<>();
		for (Distributor d : rows) {
			if (d.getDistributorId() == null) {
				continue;
			}
			out.put(d.getDistributorId(), d.getName() == null ? "" : d.getName());
		}
		return out;
	}

	/**
	 * H5 offline after-sales head store: {@code offline_aftersales = 1}, filtered by distributor id when positive,
	 * otherwise self-operated main ({@code distributor_self = 1}).
	 */
	public Optional<Map<String, Object>> findH5OfflineAftersalesHeadRow(
			long companyId, Long distributorIdFilterPositiveOrNull) {
		LambdaQueryWrapper<Distributor> w = new LambdaQueryWrapper<Distributor>()
				.eq(Distributor::getCompanyId, companyId)
				.eq(Distributor::getOfflineAftersales, 1);
		if (distributorIdFilterPositiveOrNull != null && distributorIdFilterPositiveOrNull > 0L) {
			w.eq(Distributor::getDistributorId, distributorIdFilterPositiveOrNull);
		} else {
			w.eq(Distributor::getDistributorSelf, 1);
		}
		w.last("LIMIT 1");
		Distributor entity = distributorMapper.selectOne(w);
		if (entity == null) {
			return Optional.empty();
		}
		return Optional.of(new LinkedHashMap<>(DistributorRowMaps.toApiRow(entity, objectMapper)));
	}

	public Object getInfoSimple(long companyId, String distributorIdRaw) {
		if (distributorIdRaw == null || distributorIdRaw.isBlank()) {
			return Collections.emptyList();
		}
		long did;
		try {
			did = Long.parseLong(distributorIdRaw.trim());
		} catch (NumberFormatException e) {
			return Collections.emptyList();
		}
		if (did < 0L) {
			return Collections.emptyList();
		}
		Distributor entity = distributorMapper.selectOne(new LambdaQueryWrapper<Distributor>()
				.eq(Distributor::getCompanyId, companyId)
				.eq(Distributor::getDistributorId, did)
				.last("LIMIT 1"));
		if (entity == null) {
			return Collections.emptyList();
		}
		Map<String, Object> row = DistributorRowMaps.toApiRow(entity, objectMapper);
		return row;
	}

	/**
	 * Resolves distributor rows from repeated {@code distributor_id} query parameters, matching API list lookup
	 * semantics for add-salesman flows.
	 */
	public Map<String, Object> resolveDistributorForAddSalesman(long companyId, String[] distributorIdParameterValues) {
		if (distributorIdParameterValues == null || distributorIdParameterValues.length == 0) {
			return Collections.emptyMap();
		}
		List<String> nonBlank = new ArrayList<>();
		for (String s : distributorIdParameterValues) {
			if (StringUtils.hasText(s)) {
				nonBlank.add(s.trim());
			}
		}
		if (nonBlank.isEmpty()) {
			return Collections.emptyMap();
		}
		Distributor entity;
		if (nonBlank.size() == 1) {
			String t = nonBlank.get(0);
			long did;
			try {
				did = Long.parseLong(t);
			} catch (NumberFormatException e) {
				return Collections.emptyMap();
			}
			if (did < 0L) {
				return Collections.emptyMap();
			}
			entity = distributorMapper.selectOne(new LambdaQueryWrapper<Distributor>()
					.eq(Distributor::getCompanyId, companyId)
					.eq(Distributor::getDistributorId, did)
					.last("LIMIT 1"));
		} else {
			List<Long> ids = new ArrayList<>();
			for (String t : nonBlank) {
				try {
					long id = Long.parseLong(t.trim());
					if (id >= 0L) {
						ids.add(id);
					}
				} catch (NumberFormatException e) {
					// skip non-numeric fragment
				}
			}
			if (ids.isEmpty()) {
				return Collections.emptyMap();
			}
			entity = distributorMapper.selectOne(new LambdaQueryWrapper<Distributor>()
					.eq(Distributor::getCompanyId, companyId)
					.in(Distributor::getDistributorId, ids)
					.last("LIMIT 1"));
		}
		if (entity == null) {
			return Collections.emptyMap();
		}
		return new LinkedHashMap<>(DistributorRowMaps.toApiRow(entity, objectMapper));
	}

	/**
	 * Resolves the distributor row used for company shop payloads: optional explicit id (valid rows only), else
	 * default store, else self-operated main store with optional virtual {@code distributor_id} in client maps.
	 */
	public Optional<ResolvedDistributorForShop> resolveForCompanyShop(long companyId, long distributorId) {
		if (distributorId > 0L) {
			Distributor entity = distributorMapper.selectOne(new LambdaQueryWrapper<Distributor>()
					.eq(Distributor::getCompanyId, companyId)
					.eq(Distributor::getDistributorId, distributorId)
					.apply(SQL_IS_VALID_ACTIVE)
					.last("LIMIT 1"));
			if (entity != null) {
				return Optional.of(new ResolvedDistributorForShop(entity, false));
			}
		}

		Distributor defaultEntity = distributorMapper.selectOne(new LambdaQueryWrapper<Distributor>()
				.eq(Distributor::getCompanyId, companyId)
				.eq(Distributor::getIsDefault, 1)
				.last("LIMIT 1"));

		if (defaultEntity == null) {
			Distributor mainEntity = distributorMapper.selectOne(new LambdaQueryWrapper<Distributor>()
					.eq(Distributor::getCompanyId, companyId)
					.eq(Distributor::getDistributorSelf, 1)
					.last("LIMIT 1"));
			if (mainEntity != null) {
				return Optional.of(new ResolvedDistributorForShop(mainEntity, true));
			}
			return Optional.empty();
		}

		Distributor chosen = defaultEntity;
		if (shouldReplaceDefaultWithMain(chosen)) {
			Distributor mainEntity = distributorMapper.selectOne(new LambdaQueryWrapper<Distributor>()
					.eq(Distributor::getCompanyId, companyId)
					.eq(Distributor::getDistributorSelf, 1)
					.last("LIMIT 1"));
			if (mainEntity != null) {
				return Optional.of(new ResolvedDistributorForShop(mainEntity, true));
			}
			return Optional.empty();
		}
		return Optional.of(new ResolvedDistributorForShop(chosen, false));
	}

	/**
	 * Resolves distributor (shop) info for a company: when {@code distributorId} is positive, loads that row if valid;
	 * otherwise falls back to the company default store, then to the self-operated main store when the default is
	 * missing, invalid, or not self-operated—setting {@code distributor_id} to 0 for the virtual main result.
	 * Returns {@link Collections#emptyMap()} when no applicable store exists.
	 */
	public Map<String, Object> getDistributorInfoForCompanyShop(long companyId, long distributorId) {
		Optional<ResolvedDistributorForShop> r = resolveForCompanyShop(companyId, distributorId);
		if (r.isEmpty()) {
			return Collections.emptyMap();
		}
		LinkedHashMap<String, Object> m =
				new LinkedHashMap<>(DistributorRowMaps.toApiRow(r.get().distributor(), objectMapper));
		if (r.get().useVirtualMainDistributorIdInClientMap()) {
			m.put("distributor_id", 0L);
		}
		return m;
	}

	private static boolean shouldReplaceDefaultWithMain(Distributor d) {
		String isValid = d.getIsValid();
		if ("false".equalsIgnoreCase(isValid == null ? "" : isValid) || Boolean.FALSE.equals(isValid)) {
			return true;
		}
		Integer self = d.getDistributorSelf();
		if (self == null) {
			return false;
		}
		return self == 0;
	}

	/**
	 * Single {@link Distributor} entity for the tenant and id, including rows that fail the active {@code is_valid}
	 * predicate.
	 */
	public Optional<Distributor> findDistributorEntityByCompanyAndDistributorId(long companyId, long distributorId) {
		if (distributorId <= 0L) {
			return Optional.empty();
		}
		Distributor entity = distributorMapper.selectOne(new LambdaQueryWrapper<Distributor>()
				.eq(Distributor::getCompanyId, companyId)
				.eq(Distributor::getDistributorId, distributorId)
				.last("LIMIT 1"));
		if (entity == null) {
			return Optional.empty();
		}
		return Optional.of(entity);
	}

	/**
	 * Loads a distributor row by company and distributor id without filtering on {@code is_valid}.
	 */
	public Optional<Map<String, Object>> getDistributorApiRowByCompanyAndDistributorId(long companyId, long distributorId) {
		if (distributorId <= 0L) {
			return Optional.empty();
		}
		Distributor entity = distributorMapper.selectOne(new LambdaQueryWrapper<Distributor>()
				.eq(Distributor::getCompanyId, companyId)
				.eq(Distributor::getDistributorId, distributorId)
				.last("LIMIT 1"));
		if (entity == null) {
			return Optional.empty();
		}
		return Optional.of(new LinkedHashMap<>(DistributorRowMaps.toApiRow(entity, objectMapper)));
	}

	/**
	 * 对齐简易店铺列表行字段（单企业、多 distributor_id），供详情等接口复用。
	 */
	public List<Map<String, Object>> listEasylistsByDistributorIds(long companyId, List<Long> distributorIds) {
		if (distributorIds == null || distributorIds.isEmpty()) {
			return Collections.emptyList();
		}
		List<Distributor> rows = distributorMapper.selectList(new LambdaQueryWrapper<Distributor>()
				.eq(Distributor::getCompanyId, companyId)
				.in(Distributor::getDistributorId, distributorIds)
				.orderByAsc(Distributor::getDistributorId));
		List<Map<String, Object>> out = new ArrayList<>(rows.size());
		for (Distributor d : rows) {
			out.add(toEasylistsRow(d));
		}
		return out;
	}

	private static Map<String, Object> toEasylistsRow(Distributor d) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("address", d.getAddress() != null ? d.getAddress() : "");
		m.put("name", d.getName() != null ? d.getName() : "");
		m.put("shop_code", d.getShopCode() != null ? d.getShopCode() : "");
		m.put("mobile", d.getMobile() != null ? d.getMobile() : "");
		m.put("contact", d.getContact() != null ? d.getContact() : "");
		m.put("distributor_id", d.getDistributorId() != null ? d.getDistributorId() : 0L);
		m.put("logo", d.getLogo());
		m.put("hour", d.getHour() != null ? d.getHour() : "");
		long parent = d.getShopId() != null ? d.getShopId() : 0L;
		m.put("parent_distributor_id", parent);
		m.put("lng", d.getLng() != null ? d.getLng() : "");
		m.put("lat", d.getLat() != null ? d.getLat() : "");
		boolean isDist = d.getIsDistributor() == null || Boolean.TRUE.equals(d.getIsDistributor());
		m.put("is_distributor", isDist);
		int def = d.getIsDefault() != null ? d.getIsDefault() : 0;
		m.put("is_default", def != 0);
		return m;
	}
}
