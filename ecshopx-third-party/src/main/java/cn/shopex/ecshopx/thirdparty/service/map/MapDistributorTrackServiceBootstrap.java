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

package cn.shopex.ecshopx.thirdparty.service.map;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.thirdparty.domain.MapConfigService;
import cn.shopex.ecshopx.thirdparty.mapper.MapConfigServiceMapper;
import cn.shopex.ecshopx.thirdparty.service.map.amap.AmapTrackRestClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MapDistributorTrackServiceBootstrap {

	public static final int TYPE_DISTRIBUTOR = 11;
	public static final int STATUS_ENABLE = 1;

	private final MapConfigServiceMapper mapConfigServiceMapper;
	private final AmapTrackRestClient amapTrackRestClient;
	private final ObjectMapper objectMapper;

	public MapDistributorTrackServiceBootstrap(
			MapConfigServiceMapper mapConfigServiceMapper,
			AmapTrackRestClient amapTrackRestClient,
			ObjectMapper objectMapper) {
		this.mapConfigServiceMapper = mapConfigServiceMapper;
		this.amapTrackRestClient = amapTrackRestClient;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> initDistributorService(long companyId, Map<String, Object> configInfo) {
		validateConfigKeys(configInfo);
		long configId = ((Number) configInfo.get("id")).longValue();

		MapConfigService existing = mapConfigServiceMapper.selectOne(new LambdaQueryWrapper<MapConfigService>()
				.eq(MapConfigService::getCompanyId, companyId)
				.eq(MapConfigService::getConfigId, configId)
				.eq(MapConfigService::getType, TYPE_DISTRIBUTOR)
				.last("LIMIT 1"));
		if (existing != null) {
			return serviceRowToMap(existing);
		}

		Object typeObj = configInfo.get("type");
		String type = typeObj == null ? "" : String.valueOf(typeObj).trim();
		if ("amap".equals(type)) {
			String appKey = Objects.toString(configInfo.get("app_key"), "");
			Map<String, Object> created = amapTrackRestClient.addService(appKey, "service_distributor", "");
			String sid = Objects.toString(created.get("sid"), "").trim();
			if (created.isEmpty() || !StringUtils.hasText(sid)) {
				throw new ResourceException("创建失败！");
			}
			String serviceDataJson;
			try {
				serviceDataJson = objectMapper.writeValueAsString(
						Map.of("sid", sid, "name", "service_distributor", "description", ""));
			} catch (JsonProcessingException e) {
				throw new ResourceException("创建失败！");
			}
			int now = (int) Instant.now().getEpochSecond();
			MapConfigService entity = new MapConfigService();
			entity.setCompanyId(companyId);
			entity.setConfigId(configId);
			entity.setType(TYPE_DISTRIBUTOR);
			entity.setServiceId(sid);
			entity.setServiceData(serviceDataJson);
			entity.setStatus(STATUS_ENABLE);
			entity.setCreated(now);
			entity.setUpdated(now);
			mapConfigServiceMapper.insert(entity);
			return serviceRowToMap(entity);
		}

		return Collections.emptyMap();
	}

	private static void validateConfigKeys(Map<String, Object> configInfo) {
		if (!configInfo.containsKey("company_id")
				|| configInfo.get("company_id") == null
				|| "".equals(Objects.toString(configInfo.get("company_id"), "").trim())) {
			throw new BadRequestException("地图配置信息有误！");
		}
		if (!configInfo.containsKey("id") || configInfo.get("id") == null) {
			throw new BadRequestException("地图配置信息有误！");
		}
		if (!configInfo.containsKey("app_key") || configInfo.get("app_key") == null) {
			throw new BadRequestException("地图配置信息有误！");
		}
		String appKeyStr = Objects.toString(configInfo.get("app_key"), "");
		if (!StringUtils.hasText(appKeyStr)) {
			throw new BadRequestException("地图配置信息有误！");
		}
		if (!configInfo.containsKey("type") || !StringUtils.hasText(Objects.toString(configInfo.get("type"), ""))) {
			throw new BadRequestException("地图配置信息有误！");
		}
	}

	private static Map<String, Object> serviceRowToMap(MapConfigService row) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", row.getId());
		m.put("service_id", row.getServiceId() != null ? row.getServiceId() : "");
		return m;
	}
}
