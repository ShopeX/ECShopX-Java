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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.distribution.domain.PickupLocation;
import cn.shopex.ecshopx.distribution.mapper.PickupLocationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PickupLocationListService {

	private final PickupLocationMapper pickupLocationMapper;
	private final DistributorBatchApiRowQueryService distributorBatchApiRowQueryService;
	private final DistributorListOutsideLangReadService distributorListOutsideLangReadService;
	private final ObjectMapper objectMapper;
	private final MessageSource messageSource;

	public PickupLocationListService(
			PickupLocationMapper pickupLocationMapper,
			DistributorBatchApiRowQueryService distributorBatchApiRowQueryService,
			DistributorListOutsideLangReadService distributorListOutsideLangReadService,
			ObjectMapper objectMapper,
			MessageSource messageSource) {
		this.pickupLocationMapper = pickupLocationMapper;
		this.distributorBatchApiRowQueryService = distributorBatchApiRowQueryService;
		this.distributorListOutsideLangReadService = distributorListOutsideLangReadService;
		this.objectMapper = objectMapper;
		this.messageSource = messageSource;
	}

	public Map<String, Object> getPickupLocationList(
			long companyId,
			String operatorType,
			String distributorIdParam,
			String pageRaw,
			String pageSizeRaw,
			String name,
			String province,
			String city,
			String area,
			String address,
			String relDistributorIdRaw,
			String requestLangTag) {
		int page = parsePositiveIntWithDefault(pageRaw, 1);
		int pageSize = parsePositiveIntWithDefault(pageSizeRaw, 20);

		LambdaQueryWrapper<PickupLocation> w = new LambdaQueryWrapper<>();
		w.eq(PickupLocation::getCompanyId, companyId);

		// 与 PHP PickupLocation::getPickupLocationList 对齐（ECX-9649）：
		// - 店铺端传了 rel_distributor_id：按关联店铺查，去掉 distributor_id（可见平台/店铺创建且已关联的）
		// - 店铺端未传：按 distributor_id=当前登录店铺（仅自建）
		// - 平台端传了 rel：按参数 rel 查，不带 distributor_id
		// - 平台端未传：仅总部自建（distributor_id=0）
		long relLead = 0L;
		if (relDistributorIdRaw != null) {
			String s = relDistributorIdRaw.trim();
			if (StringUtils.hasText(s) && !"0".equals(s)) {
				long parsedRel = LeadingNumberParser.parseAsLong(s);
				if (parsedRel > 0L) {
					relLead = parsedRel;
				}
			}
		}
		boolean hasRel = relLead > 0L;
		boolean isDistributor = "distributor".equals(operatorType);
		Long currentShopId = parseOptionalLong(distributorIdParam);

		if (isDistributor) {
			if (hasRel) {
				// 店铺端：强制用当前登录店铺，防止查他店关联；不再叠加 distributor_id
				long relShopId = currentShopId != null && currentShopId > 0L ? currentShopId : relLead;
				w.eq(PickupLocation::getRelDistributorId, relShopId);
			} else if (currentShopId != null) {
				w.eq(PickupLocation::getDistributorId, currentShopId);
			}
		} else if (hasRel) {
			w.eq(PickupLocation::getRelDistributorId, relLead);
		} else {
			w.eq(PickupLocation::getDistributorId, 0L);
		}

		if (name != null && StringUtils.hasText(name.trim())) {
			String escaped = escapeLike(name.trim());
			w.like(PickupLocation::getName, "%" + escaped + "%");
		}
		if (province != null && StringUtils.hasText(province.trim())) {
			w.eq(PickupLocation::getProvince, province.trim());
		}
		if (city != null && StringUtils.hasText(city.trim())) {
			w.eq(PickupLocation::getCity, city.trim());
		}
		if (area != null && StringUtils.hasText(area.trim())) {
			w.eq(PickupLocation::getArea, area.trim());
		}
		if (address != null && StringUtils.hasText(address.trim())) {
			String escaped = escapeLike(address.trim());
			w.like(PickupLocation::getAddress, "%" + escaped + "%");
		}

		w.orderByDesc(PickupLocation::getCreated);

		Page<PickupLocation> p = new Page<>(page, pageSize);
		pickupLocationMapper.selectPage(p, w);
		List<PickupLocation> records = p.getRecords();
		long totalCount = p.getTotal();

		List<Map<String, Object>> listMaps = new ArrayList<>();
		for (PickupLocation e : records) {
			listMaps.add(toListRow(e));
		}

		if (listMaps.isEmpty()) {
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("total_count", totalCount);
			data.put("list", List.of());
			return data;
		}

		List<Long> relIds = listMaps.stream()
				.map(m -> (Long) m.get("rel_distributor_id"))
				.filter(Objects::nonNull)
				.filter(id -> id > 0L)
				.distinct()
				.toList();
		Map<Long, Map<String, Object>> byDid =
				distributorBatchApiRowQueryService.loadByCompanyAndDistributorIds(companyId, relIds);

		List<Map<String, Object>> distRowsForLang = new ArrayList<>();
		for (Long relId : relIds) {
			Map<String, Object> distRow = new LinkedHashMap<>();
			distRow.put("distributor_id", relId);
			Map<String, Object> apiRow = byDid.get(relId);
			Object nm = apiRow != null ? apiRow.get("name") : null;
			distRow.put("name", nm != null ? nm : "");
			distRowsForLang.add(distRow);
		}
		distributorListOutsideLangReadService.applyLangMaps(companyId, requestLangTag, distRowsForLang);

		String langKey = StringUtils.hasText(requestLangTag) ? requestLangTag.trim() : "zh-CN";
		Map<Long, String> relNameById = distRowsForLang.stream()
				.filter(dr -> dr.get("distributor_id") instanceof Number n && n.longValue() > 0L)
				.collect(
						Collectors.toMap(
								dr -> ((Number) dr.get("distributor_id")).longValue(),
								dr -> resolveDisplayName(dr, langKey),
								(a, b) -> a,
								LinkedHashMap::new));

		for (Map<String, Object> row : listMaps) {
			Long rid = (Long) row.get("rel_distributor_id");
			if (rid != null && relNameById.containsKey(rid)) {
				row.put("rel_distributor_name", relNameById.get(rid));
			}
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("total_count", totalCount);
		data.put("list", listMaps);
		return data;
	}

	public Object getPickupLocationInfo(
			long companyId,
			String operatorType,
			String distributorIdParam,
			long pickupLocationId) {
		LambdaQueryWrapper<PickupLocation> w = new LambdaQueryWrapper<>();
		applyCompanyAndDistributorScopeForPickupLocation(w, companyId, operatorType, distributorIdParam);
		w.eq(PickupLocation::getId, pickupLocationId);

		PickupLocation entity = pickupLocationMapper.selectOne(w);
		if (entity == null) {
			return Collections.emptyList();
		}
		return toListRow(entity);
	}

	public void delPickupLocation(
			long companyId,
			String operatorType,
			String distributorIdParam,
			long pickupLocationId) {
		LambdaQueryWrapper<PickupLocation> w = new LambdaQueryWrapper<>();
		applyCompanyAndDistributorScopeForPickupLocation(w, companyId, operatorType, distributorIdParam);
		w.eq(PickupLocation::getId, pickupLocationId);
		pickupLocationMapper.delete(w);
	}

	private void applyCompanyAndDistributorScopeForPickupLocation(
			LambdaQueryWrapper<PickupLocation> w,
			long companyId,
			String operatorType,
			String distributorIdParam) {
		w.eq(PickupLocation::getCompanyId, companyId);
		if (!"distributor".equals(operatorType)) {
			w.eq(PickupLocation::getDistributorId, 0L);
		} else {
			if (distributorIdParam == null || !StringUtils.hasText(distributorIdParam.trim())) {
				w.isNull(PickupLocation::getDistributorId);
			} else {
				try {
					long parsedLong = Long.parseLong(distributorIdParam.trim());
					w.eq(PickupLocation::getDistributorId, parsedLong);
				} catch (NumberFormatException ex) {
					throw new BadRequestException(
							this.messageSource.getMessage(
									"distribution.pickupLocation.relDistributor.invalidDistributorId",
									null,
									LocaleContextHolder.getLocale()));
				}
			}
		}
	}

	private static String resolveDisplayName(Map<String, Object> distRow, String langKey) {
		Object nameLang = distRow.get("name_lang");
		if (nameLang instanceof Map<?, ?> nm) {
			Object v = nm.get(langKey);
			if (v instanceof String s && StringUtils.hasText(s)) {
				return s;
			}
		}
		return Objects.toString(distRow.get("name"), "");
	}

	private Map<String, Object> toListRow(PickupLocation e) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", e.getId());
		row.put("company_id", e.getCompanyId());
		row.put("distributor_id", e.getDistributorId());
		row.put("rel_distributor_id", e.getRelDistributorId());
		row.put("name", e.getName());
		row.put("lng", e.getLng());
		row.put("lat", e.getLat());
		row.put("province", e.getProvince());
		row.put("city", e.getCity());
		row.put("area", e.getArea());
		row.put("address", e.getAddress());
		row.put("contract_phone", e.getContractPhone() == null ? "" : e.getContractPhone());

		String hoursJson = e.getHours();
		Object hoursVal;
		if (hoursJson == null || !StringUtils.hasText(hoursJson.trim())) {
			hoursVal = null;
		} else {
			try {
				hoursVal = objectMapper.readValue(hoursJson.trim(), Object.class);
			} catch (JsonProcessingException ex) {
				hoursVal = null;
			}
		}
		row.put("hours", hoursVal);

		row.put("workdays", decodeWorkdays(e.getWorkdays()));
		row.put("wait_pickup_days", e.getWaitPickupDays());
		row.put("latest_pickup_time", e.getLatestPickupTime());
		row.put("created", e.getCreated());
		row.put("updated", e.getUpdated());
		return row;
	}

	private static List<String> decodeWorkdays(String workdaysRaw) {
		String v = workdaysRaw == null ? "" : workdaysRaw;
		while (v.length() > 0 && v.charAt(0) == ',') {
			v = v.substring(1);
		}
		while (v.length() > 0 && v.charAt(v.length() - 1) == ',') {
			v = v.substring(0, v.length() - 1);
		}
		String[] parts = v.split(",", -1);
		List<String> out = new ArrayList<>();
		for (String part : parts) {
			out.add(part == null ? "" : part.trim());
		}
		return out;
	}

	private static Long parseOptionalLong(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static int parsePositiveIntWithDefault(String raw, int defaultVal) {
		if (!StringUtils.hasText(raw)) {
			return defaultVal;
		}
		try {
			int v = Integer.parseInt(raw.trim());
			return v < 1 ? defaultVal : v;
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}

	private static String escapeLike(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
}
