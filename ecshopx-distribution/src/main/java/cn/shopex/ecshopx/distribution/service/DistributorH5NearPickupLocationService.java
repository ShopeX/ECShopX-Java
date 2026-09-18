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

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.distribution.mapper.PickupLocationMapper;
import cn.shopex.ecshopx.distribution.service.pickup.DistributorPickupStoreHoursCompat;
import cn.shopex.ecshopx.orders.service.nostores.DistributorNostoresCartDistributorIdsReadService;
import cn.shopex.ecshopx.orders.service.nostores.dto.NostoresScopedDistributorFilter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorH5NearPickupLocationService {

	private final PickupLocationMapper pickupLocationMapper;
	private final DistributorMapper distributorMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final ObjectMapper objectMapper;
	private final DistributorNostoresCartDistributorIdsReadService distributorNostoresCartDistributorIdsReadService;
	private final DistributorPickupStoreHoursCompat distributorPickupStoreHoursCompat;

	public DistributorH5NearPickupLocationService(
			PickupLocationMapper pickupLocationMapper,
			DistributorMapper distributorMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			ObjectMapper objectMapper,
			DistributorNostoresCartDistributorIdsReadService distributorNostoresCartDistributorIdsReadService,
			DistributorPickupStoreHoursCompat distributorPickupStoreHoursCompat) {
		this.pickupLocationMapper = pickupLocationMapper;
		this.distributorMapper = distributorMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.objectMapper = objectMapper;
		this.distributorNostoresCartDistributorIdsReadService = distributorNostoresCartDistributorIdsReadService;
		this.distributorPickupStoreHoursCompat = distributorPickupStoreHoursCompat;
	}

	public Map<String, Object> getNearPickupLocation(
			long companyId,
			long userId,
			String lngRaw,
			String latRaw,
			long distributorIdFromQuery,
			String isNostoresRaw,
			String cartType,
			String seckillId,
			String seckillTicket,
			String iscrossborder,
			String bargainIdRaw) {
		List<Long> distributorIds;
		if (isNostoresTruthy(isNostoresRaw)) {
			long cartDistributorShopId = distributorIdFromQuery > 0L ? distributorIdFromQuery : 0L;
			String ct =
					(cartType == null || !StringUtils.hasText(cartType.trim())) ? "cart" : cartType.trim();
			distributorIds =
					distributorNostoresCartDistributorIdsReadService.listDistributorIdsByNostoresCart(
							companyId,
							userId,
							NostoresScopedDistributorFilter.forCompanyZitiOnly(companyId),
							ct,
							"normal",
							seckillId,
							seckillTicket,
							iscrossborder,
							bargainIdRaw,
							cartDistributorShopId);
			if (distributorIds == null || distributorIds.isEmpty()) {
				return emptyPayload();
			}
		} else {
			distributorIds = new ArrayList<>();
			if (distributorIdFromQuery > 0L) {
				distributorIds.add(distributorIdFromQuery);
			} else {
				Distributor self =
						distributorMapper.selectOne(
								new LambdaQueryWrapper<Distributor>()
										.eq(Distributor::getCompanyId, companyId)
										.eq(Distributor::getDistributorSelf, 1)
										.last("LIMIT 1"));
				if (self == null || self.getDistributorId() == null) {
					return emptyPayload();
				}
				distributorIds.add(self.getDistributorId());
			}
		}

		BigDecimal lngBd = parseOptionalCoordinate(lngRaw, true);
		BigDecimal latBd = parseOptionalCoordinate(latRaw, false);
		boolean useDistance = hasTruthyLngLat(lngBd, latBd);
		BigDecimal lngForSql = lngBd != null ? lngBd : BigDecimal.ZERO;
		BigDecimal latForSql = latBd != null ? latBd : BigDecimal.ZERO;

		long total = pickupLocationMapper.countH5NearByRelDistributorIds(companyId, distributorIds);
		List<Map<String, Object>> list = new ArrayList<>();
		if (total > 0L) {
			list.addAll(
					pickupLocationMapper.selectH5NearByRelDistributorIds(
							companyId, distributorIds, lngForSql, latForSql, useDistance));
			for (Map<String, Object> row : list) {
				normalizePickupRow(row);
			}
		}

		Distributor anchor =
				distributorMapper.selectOne(
						new LambdaQueryWrapper<Distributor>()
								.eq(Distributor::getCompanyId, companyId)
								.eq(Distributor::getDistributorId, distributorIds.get(0))
								.last("LIMIT 1"));
		if (anchor != null && StringUtils.hasText(anchor.getAddress())) {
			Map<String, Object> storeItem = buildStoreAddressItem(anchor, lngBd, latBd, useDistance);
			list.add(0, storeItem);
			total = total + 1;
			if (useDistance && list.stream().anyMatch(r -> r.get("distance") != null)) {
				list.sort(Comparator.comparing(this::distanceOrMax));
			}
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", total);
		out.put("list", list);
		return out;
	}

	private static boolean isNostoresTruthy(String raw) {
		if (raw == null) {
			return false;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return false;
		}
		try {
			return new BigDecimal(t).compareTo(BigDecimal.ONE) == 0;
		} catch (NumberFormatException e) {
			return "1".equals(t);
		}
	}

	private static BigDecimal parseOptionalCoordinate(String raw, boolean longitude) {
		if (raw == null) {
			return null;
		}
		String trim = raw.trim();
		if (!StringUtils.hasText(trim)) {
			return null;
		}
		try {
			BigDecimal v = new BigDecimal(trim);
			if (longitude) {
				if (v.compareTo(new BigDecimal("-180")) < 0 || v.compareTo(new BigDecimal("180")) > 0) {
					throw new ResourceException("请授权当前所在位置或传入正确的经纬度");
				}
			} else {
				if (v.compareTo(new BigDecimal("-90")) < 0 || v.compareTo(new BigDecimal("90")) > 0) {
					throw new ResourceException("请授权当前所在位置或传入正确的经纬度");
				}
			}
			return v;
		} catch (NumberFormatException e) {
			throw new ResourceException("请授权当前所在位置或传入正确的经纬度");
		}
	}

	private static boolean hasTruthyLngLat(BigDecimal lng, BigDecimal lat) {
		return lng != null
				&& lat != null
				&& hasTruthyCoordinate(lng)
				&& hasTruthyCoordinate(lat);
	}

	private double distanceOrMax(Map<String, Object> row) {
		Object d = row.get("distance");
		if (d == null) {
			return Double.MAX_VALUE;
		}
		if (d instanceof Number n) {
			return n.doubleValue();
		}
		try {
			return Double.parseDouble(d.toString());
		} catch (NumberFormatException e) {
			return Double.MAX_VALUE;
		}
	}

	private Map<String, Object> buildStoreAddressItem(
			Distributor d, BigDecimal userLng, BigDecimal userLat, boolean useDistance) {
		long distributorId = d.getDistributorId() == null ? 0L : d.getDistributorId();
		String mobile = d.getMobile();
		if (mobile != null) {
			mobile = sensitiveFieldEncryptor.decrypt(mobile);
		}
		List<List<String>> formattedHours =
				distributorPickupStoreHoursCompat.formatDistributorHour(d.getHour());
		List<String> workdays =
				distributorPickupStoreHoursCompat.extractWorkdaysFromHours(formattedHours);
		Map<String, Object> storeItem = new LinkedHashMap<>();
		storeItem.put("id", String.valueOf(-distributorId));
		storeItem.put("company_id", d.getCompanyId() == null ? "" : String.valueOf(d.getCompanyId()));
		storeItem.put("distributor_id", String.valueOf(distributorId));
		storeItem.put("rel_distributor_id", String.valueOf(distributorId));
		storeItem.put("name", Objects.toString(d.getName(), ""));
		storeItem.put("lng", Objects.toString(d.getLng(), ""));
		storeItem.put("lat", Objects.toString(d.getLat(), ""));
		storeItem.put("province", Objects.toString(d.getProvince(), ""));
		storeItem.put("city", Objects.toString(d.getCity(), ""));
		storeItem.put("area", Objects.toString(d.getArea(), ""));
		storeItem.put("address", Objects.toString(d.getAddress(), ""));
		storeItem.put("contract_phone", mobile == null ? "" : mobile);
		storeItem.put("hours", formattedHours);
		storeItem.put("workdays", workdays);
		storeItem.put("wait_pickup_days", "0");
		storeItem.put("latest_pickup_time", null);
		storeItem.put("created", String.valueOf(System.currentTimeMillis() / 1000));
		storeItem.put("updated", String.valueOf(System.currentTimeMillis() / 1000));
		storeItem.put("is_store_address", Boolean.TRUE);
		if (useDistance) {
			Double dist = haversineKm(userLat, userLng, d.getLat(), d.getLng());
			if (dist != null) {
				storeItem.put("distance", dist);
			}
		}
		return storeItem;
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

	private void normalizePickupRow(Map<String, Object> row) {
		Object h = row.get("hours");
		if (h instanceof String s && StringUtils.hasText(s.trim())) {
			try {
				row.put("hours", objectMapper.readValue(s.trim(), Object.class));
			} catch (JsonProcessingException e) {
				row.put("hours", null);
			}
		}
		Object wd = row.get("workdays");
		if (wd instanceof String s) {
			row.put("workdays", decodeWorkdays(s));
		}
	}

	private static Double haversineKm(BigDecimal userLat, BigDecimal userLng, String lat2, String lng2) {
		if (!StringUtils.hasText(lat2) || !StringUtils.hasText(lng2)) {
			return null;
		}
		try {
			double latA = userLat.doubleValue();
			double lngA = userLng.doubleValue();
			double latB = Double.parseDouble(lat2.trim());
			double lngB = Double.parseDouble(lng2.trim());
			final double earthRadius = 6371.0;
			double dLat = Math.toRadians(latB - latA);
			double dLng = Math.toRadians(lngB - lngA);
			double a =
					Math.sin(dLat / 2) * Math.sin(dLat / 2)
							+ Math.cos(Math.toRadians(latA))
									* Math.cos(Math.toRadians(latB))
									* Math.sin(dLng / 2)
									* Math.sin(dLng / 2);
			double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
			return BigDecimal.valueOf(earthRadius * c).setScale(2, RoundingMode.HALF_UP).doubleValue();
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Map<String, Object> emptyPayload() {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", 0L);
		out.put("list", Collections.emptyList());
		return out;
	}

	private static boolean hasTruthyCoordinate(BigDecimal v) {
		if (v == null) {
			return false;
		}
		String plain = v.stripTrailingZeros().toPlainString();
		return !plain.isEmpty() && !"0".equals(plain);
	}
}
