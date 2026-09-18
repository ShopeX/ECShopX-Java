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

import cn.shopex.ecshopx.distribution.domain.DistributorGeofence;
import cn.shopex.ecshopx.distribution.service.dto.DistributorGeofenceJoinRow;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DistributorGeofenceResponseMapper {

	private final ObjectMapper objectMapper;

	public DistributorGeofenceResponseMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> handleData(DistributorGeofence row) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", row.getId());
		m.put("company_id", row.getCompanyId());
		m.put("distributor_id", row.getDistributorId());
		m.put("config_service_local_id", row.getConfigServiceLocalId());
		m.put("geofence_id", row.getGeofenceId() != null ? row.getGeofenceId() : "");
		m.put("geofence_data", decodeGeofenceDataJson(row.getGeofenceData()));
		m.put("status", row.getStatus());
		m.put("created", row.getCreated());
		m.put("updated", row.getUpdated());
		return m;
	}

	public Map<String, Object> toListItem(DistributorGeofenceJoinRow row) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", row.getId());
		m.put("company_id", row.getCompanyId());
		m.put("distributor_id", row.getDistributorId());
		m.put("config_service_local_id", row.getConfigServiceLocalId());
		m.put("geofence_id", row.getGeofenceId() != null ? row.getGeofenceId() : "");
		m.put("status", row.getStatus());
		m.put("created", row.getCreated());
		m.put("updated", row.getUpdated());
		m.put("service_id", row.getServiceId());
		m.put("app_key", row.getAppKey());
		m.put("config_type", row.getConfigType());
		LinkedHashMap<String, Object> geofenceData = decodeGeofenceDataJson(row.getGeofenceData());
		m.put("geofence_data", geofenceData);
		if (StringUtils.hasText(row.getConfigType())) {
			m.put("geofence_list", new ArrayList<>());
			if ("amap".equals(row.getConfigType())) {
				String geofenceType =
						geofenceData.get("type") == null ? null : String.valueOf(geofenceData.get("type")).trim();
				if ("polygon".equals(geofenceType)) {
					List<Map<String, Object>> builtList = buildAmapPolygonGeofenceList(geofenceData);
					m.put("geofence_list", builtList);
				}
			}
		}
		return m;
	}

	private List<Map<String, Object>> buildAmapPolygonGeofenceList(LinkedHashMap<String, Object> geofenceData) {
		List<Map<String, Object>> builtList = new ArrayList<>();
		Object paramsObj = geofenceData.get("params");
		String points = "";
		if (paramsObj instanceof Map<?, ?> paramsMap) {
			Object p = paramsMap.get("points");
			points = p == null ? "" : String.valueOf(p);
		}
		if (!StringUtils.hasText(points)) {
			return builtList;
		}
		for (String seg : points.split(";")) {
			String piece = seg == null ? "" : seg.trim();
			if (!StringUtils.hasText(piece)) {
				continue;
			}
			String[] lngLatParts = piece.split(",", -1);
			if (lngLatParts.length < 2) {
				continue;
			}
			String lng = lngLatParts[0].trim();
			String lat = lngLatParts[1].trim();
			LinkedHashMap<String, Object> point = new LinkedHashMap<>();
			point.put("lng", lng);
			point.put("lat", lat);
			builtList.add(point);
		}
		return builtList;
	}

	private LinkedHashMap<String, Object> decodeGeofenceDataJson(String rawGeofenceData) {
		if (!StringUtils.hasText(rawGeofenceData)) {
			return new LinkedHashMap<>();
		}
		try {
			Map<String, Object> parsed =
					objectMapper.readValue(rawGeofenceData, new TypeReference<Map<String, Object>>() {});
			return new LinkedHashMap<>(parsed);
		} catch (Exception e) {
			return new LinkedHashMap<>();
		}
	}
}
