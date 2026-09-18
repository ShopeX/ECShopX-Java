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

import cn.shopex.ecshopx.common.distribution.CompanyMapGeocodePort;
import cn.shopex.ecshopx.common.distribution.CompanyMapGeocodePort.GeocodeLatLng;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.api.admin.v1.dto.CreatePickupLocationRequest;
import cn.shopex.ecshopx.distribution.domain.PickupLocation;
import cn.shopex.ecshopx.distribution.mapper.PickupLocationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PickupLocationCreateService {

	private final PickupLocationMapper pickupLocationMapper;

	private final CompanyMapGeocodePort companyMapGeocodePort;

	private final ObjectMapper objectMapper;

	private final MessageSource messageSource;

	public PickupLocationCreateService(
			PickupLocationMapper pickupLocationMapper,
			CompanyMapGeocodePort companyMapGeocodePort,
			ObjectMapper objectMapper,
			MessageSource messageSource) {
		this.pickupLocationMapper = pickupLocationMapper;
		this.companyMapGeocodePort = companyMapGeocodePort;
		this.objectMapper = objectMapper;
		this.messageSource = messageSource;
	}

	public Map<String, Object> createPickupLocation(
			long companyId, String operatorType, CreatePickupLocationRequest req) {
		Locale locale = LocaleContextHolder.getLocale();
		validateBusinessHours(req.getHours(), locale);

		String phone = req.getContractPhone();
		if (StringUtils.hasText(req.getAreaCode())) {
			phone = req.getAreaCode() + "-" + phone;
		}

		String workdaysStored = buildWorkdaysString(req.getWorkdays());

		GeocodeLatLng coords = companyMapGeocodePort.geocode(companyId, req.getCity(), req.getAddress());

		long distributorId = resolveDistributorId(operatorType, req.getDistributorId());

		long now = System.currentTimeMillis() / 1000L;
		String hoursJson;
		try {
			hoursJson = objectMapper.writeValueAsString(req.getHours());
		} catch (Exception e) {
			throw new ResourceException(
					messageSource.getMessage("distribution.pickupLocation.entityCorrupt", null, locale));
		}

		PickupLocation entity = new PickupLocation();
		entity.setCompanyId(companyId);
		entity.setDistributorId(distributorId);
		entity.setRelDistributorId(distributorId);
		entity.setName(req.getName());
		entity.setLng(coords.lng());
		entity.setLat(coords.lat());
		entity.setProvince(req.getProvince());
		entity.setCity(req.getCity());
		entity.setArea(req.getArea());
		entity.setAddress(req.getAddress());
		entity.setContractPhone(phone);
		entity.setHours(hoursJson);
		entity.setWorkdays(workdaysStored);
		entity.setWaitPickupDays(req.getWaitPickupDays());
		entity.setLatestPickupTime(req.getLatestPickupTime());
		entity.setCreated(now);
		entity.setUpdated(now);

		pickupLocationMapper.insert(entity);

		return toResponseMap(entity, locale);
	}

	public Map<String, Object> updatePickupLocation(
			long companyId, long distributorScopeId, long pickupLocationId, CreatePickupLocationRequest req) {
		Locale locale = LocaleContextHolder.getLocale();

		validateBusinessHours(req.getHours(), locale);

		String phone = req.getContractPhone();
		if (StringUtils.hasText(req.getAreaCode())) {
			phone = req.getAreaCode() + "-" + phone;
		}

		String workdaysStored = buildWorkdaysString(req.getWorkdays());

		GeocodeLatLng coords = companyMapGeocodePort.geocode(companyId, req.getCity(), req.getAddress());

		LambdaQueryWrapper<PickupLocation> q =
				new LambdaQueryWrapper<PickupLocation>()
						.eq(PickupLocation::getCompanyId, companyId)
						.eq(PickupLocation::getDistributorId, distributorScopeId)
						.eq(PickupLocation::getId, pickupLocationId);
		PickupLocation entity = pickupLocationMapper.selectOne(q);
		if (entity == null) {
			throw new ResourceException(
					messageSource.getMessage("distribution.repositories.no_update_data_found", null, locale));
		}

		String hoursJson;
		try {
			hoursJson = objectMapper.writeValueAsString(req.getHours());
		} catch (Exception e) {
			throw new ResourceException(
					messageSource.getMessage("distribution.pickupLocation.entityCorrupt", null, locale));
		}

		entity.setName(req.getName());
		entity.setLng(coords.lng());
		entity.setLat(coords.lat());
		entity.setProvince(req.getProvince());
		entity.setCity(req.getCity());
		entity.setArea(req.getArea());
		entity.setAddress(req.getAddress());
		entity.setContractPhone(phone);
		entity.setHours(hoursJson);
		entity.setWorkdays(workdaysStored);
		entity.setWaitPickupDays(req.getWaitPickupDays());
		entity.setLatestPickupTime(req.getLatestPickupTime());
		entity.setUpdated(System.currentTimeMillis() / 1000L);

		int rows = pickupLocationMapper.updateById(entity);
		if (rows == 0) {
			throw new ResourceException(
					messageSource.getMessage("distribution.repositories.no_update_data_found", null, locale));
		}

		return toResponseMap(entity, locale);
	}

	private long resolveDistributorId(String operatorType, Long bodyDistributorId) {
		if (!"distributor".equals(operatorType)) {
			return 0L;
		}
		return bodyDistributorId == null ? 0L : bodyDistributorId;
	}

	private void validateBusinessHours(List<List<String>> hours, Locale locale) {
		Map<Integer, List<String>> hoursByStartKey = new LinkedHashMap<>();
		for (List<String> slot : hours) {
			int startKey = parseHHmmToMinutesFromMidnight(slot.get(0));
			if (hoursByStartKey.containsKey(startKey)) {
				throw new ResourceException(
						messageSource.getMessage("distribution.pickupLocation.businessHoursDuplicate", null, locale));
			}
			hoursByStartKey.put(startKey, slot);
		}
		List<Map.Entry<Integer, List<String>>> sorted = new ArrayList<>(hoursByStartKey.entrySet());
		sorted.sort(Comparator.comparingInt(Map.Entry::getKey));
		int previousEndMinutes = -1;
		for (Map.Entry<Integer, List<String>> e : sorted) {
			List<String> slot = e.getValue();
			int startMinutes = parseHHmmToMinutesFromMidnight(slot.get(0));
			int endMinutes = parseHHmmToMinutesFromMidnight(slot.get(1));
			if (startMinutes <= previousEndMinutes) {
				throw new ResourceException(
						messageSource.getMessage("distribution.pickupLocation.businessHoursDuplicate", null, locale));
			}
			previousEndMinutes = endMinutes;
		}
	}

	private static int parseHHmmToMinutesFromMidnight(String hhmm) {
		int colon = hhmm.indexOf(':');
		int h = Integer.parseInt(hhmm.substring(0, colon));
		int m = Integer.parseInt(hhmm.substring(colon + 1));
		return h * 60 + m;
	}

	private static String buildWorkdaysString(List<JsonNode> workdays) {
		Set<Integer> kept = new LinkedHashSet<>();
		for (JsonNode n : workdays) {
			Integer v = parseWorkdayInt(n);
			if (v != null && v >= 1 && v <= 7) {
				kept.add(v);
			}
		}
		if (kept.isEmpty()) {
			return ",";
		}
		List<Integer> sorted = new ArrayList<>(kept);
		sorted.sort(Integer::compareTo);
		StringBuilder sb = new StringBuilder(",");
		for (int d : sorted) {
			sb.append(d).append(",");
		}
		return sb.toString();
	}

	private static Integer parseWorkdayInt(JsonNode n) {
		if (n == null || n.isNull()) {
			return null;
		}
		if (n.isInt()) {
			return n.asInt();
		}
		if (n.isIntegralNumber()) {
			return n.intValue();
		}
		if (n.isTextual()) {
			String s = n.asText();
			if (!StringUtils.hasText(s)) {
				return null;
			}
			try {
				return Integer.parseInt(s.trim());
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	private Map<String, Object> toResponseMap(PickupLocation entity, Locale locale) {
		List<?> hoursParsed;
		try {
			hoursParsed =
					objectMapper.readValue(entity.getHours(), new TypeReference<List<List<String>>>() {});
		} catch (Exception e) {
			throw new ResourceException(
					messageSource.getMessage("distribution.pickupLocation.entityCorrupt", null, locale));
		}

		List<String> workdayParts = new ArrayList<>();
		String wd = entity.getWorkdays();
		if (StringUtils.hasText(wd)) {
			for (String p : wd.split(",")) {
				if (StringUtils.hasText(p)) {
					workdayParts.add(p.trim());
				}
			}
		}

		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", entity.getId());
		row.put("company_id", entity.getCompanyId());
		row.put("distributor_id", entity.getDistributorId());
		row.put("rel_distributor_id", entity.getRelDistributorId());
		row.put("name", entity.getName());
		row.put("lng", entity.getLng());
		row.put("lat", entity.getLat());
		row.put("province", entity.getProvince());
		row.put("city", entity.getCity());
		row.put("area", entity.getArea());
		row.put("address", entity.getAddress());
		row.put("contract_phone", entity.getContractPhone());
		row.put("hours", hoursParsed);
		row.put("workdays", workdayParts);
		row.put("wait_pickup_days", entity.getWaitPickupDays());
		row.put("latest_pickup_time", entity.getLatestPickupTime());
		row.put("created", entity.getCreated());
		row.put("updated", entity.getUpdated());
		return row;
	}
}
