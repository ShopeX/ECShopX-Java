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
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.DistributorGeofence;
import cn.shopex.ecshopx.distribution.mapper.DistributorGeofenceMapper;
import cn.shopex.ecshopx.thirdparty.service.map.MapDefaultConfigQueryService;
import cn.shopex.ecshopx.thirdparty.service.map.MapDistributorTrackServiceBootstrap;
import cn.shopex.ecshopx.thirdparty.service.map.amap.AmapTrackRestClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorGeofenceSaveService {

	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final DistributorGeofenceMapper distributorGeofenceMapper;
	private final ObjectMapper objectMapper;
	private final MapDefaultConfigQueryService mapDefaultConfigQueryService;
	private final MapDistributorTrackServiceBootstrap mapDistributorTrackServiceBootstrap;
	private final AmapTrackRestClient amapTrackRestClient;
	private final DistributorGeofenceResponseMapper distributorGeofenceResponseMapper;

	public DistributorGeofenceSaveService(
			DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService,
			DistributorGeofenceMapper distributorGeofenceMapper,
			ObjectMapper objectMapper,
			MapDefaultConfigQueryService mapDefaultConfigQueryService,
			MapDistributorTrackServiceBootstrap mapDistributorTrackServiceBootstrap,
			AmapTrackRestClient amapTrackRestClient,
			DistributorGeofenceResponseMapper distributorGeofenceResponseMapper) {
		this.distributorRepositoryGetInfoSimpleService = distributorRepositoryGetInfoSimpleService;
		this.distributorGeofenceMapper = distributorGeofenceMapper;
		this.objectMapper = objectMapper;
		this.mapDefaultConfigQueryService = mapDefaultConfigQueryService;
		this.mapDistributorTrackServiceBootstrap = mapDistributorTrackServiceBootstrap;
		this.amapTrackRestClient = amapTrackRestClient;
		this.distributorGeofenceResponseMapper = distributorGeofenceResponseMapper;
	}

	public Map<String, Object> save(
			long companyId,
			long distributorId,
			Long distributorGeofenceIdOrNull,
			List<?> dataRaw,
			String typeFromBodyOrNull) {
		Object info = distributorRepositoryGetInfoSimpleService.getInfoSimple(companyId, Long.toString(distributorId));
		if (info == null
				|| (info instanceof Collection<?> c && c.isEmpty())
				|| (info instanceof Map<?, ?> m && m.isEmpty())) {
			throw new ResourceException("店铺不存在！");
		}

		if (dataRaw == null || !(dataRaw instanceof List<?>)) {
			throw new BadRequestException("围栏信息有误！");
		}
		if (dataRaw.size() < 3) {
			throw new BadRequestException("围栏顶点个数不能少于3个！");
		}

		StringJoiner pointJoiner = new StringJoiner(";");
		for (Object elt : dataRaw) {
			if (!(elt instanceof Map<?, ?> rawPt)) {
				throw new BadRequestException("围栏信息有误！");
			}
			Object lng = rawPt.get("lng");
			Object lat = rawPt.get("lat");
			if (isEmptyCoordinateValue(lng) || isEmptyCoordinateValue(lat)) {
				throw new BadRequestException("围栏的经纬度不能为空！");
			}
			pointJoiner.add(coordToString(lng) + "," + coordToString(lat));
		}
		String points = pointJoiner.toString();

		Map<String, Object> configInfo = mapDefaultConfigQueryService.loadConfigInfoMap(companyId);
		if (configInfo == null
				|| configInfo.isEmpty()
				|| !StringUtils.hasText((String) configInfo.get("type"))) {
			throw new ResourceException("地图信息有误！");
		}
		Map<String, Object> serviceInfo = mapDistributorTrackServiceBootstrap.initDistributorService(companyId, configInfo);
		if (serviceInfo == null
				|| serviceInfo.isEmpty()
				|| serviceInfo.get("id") == null
				|| !(serviceInfo.get("id") instanceof Number)) {
			throw new ResourceException("服务初始化失败！");
		}
		String serviceSid = Objects.toString(serviceInfo.get("service_id"), "");
		String appKey = Objects.toString(configInfo.get("app_key"), "");

		String fenceType = typeFromBodyOrNull != null && StringUtils.hasText(typeFromBodyOrNull)
				? typeFromBodyOrNull.trim()
				: "polygon";
		String name = String.format(
				"geofence_distributor_%s_%d%d",
				Long.toString(distributorId),
				Instant.now().getEpochSecond(),
				ThreadLocalRandom.current().nextInt(0, 1000));
		Map<String, Object> geofenceInfo = new LinkedHashMap<>();
		geofenceInfo.put("geofence_id", null);
		geofenceInfo.put("type", fenceType);
		geofenceInfo.put("name", name);
		geofenceInfo.put("params", Map.of("points", points));

		switch (fenceType) {
			case "polygon":
				break;
			default:
				throw new BadRequestException("围栏类型有误！");
		}

		if (distributorGeofenceIdOrNull != null) {
			return saveUpdateBranch(
					companyId,
					distributorId,
					distributorGeofenceIdOrNull,
					points,
					configInfo,
					serviceSid,
					appKey,
					geofenceInfo,
					name);
		}
		return saveInsertBranch(companyId, distributorId, points, configInfo, serviceSid, appKey, geofenceInfo, name, serviceInfo);
	}

	private Map<String, Object> saveUpdateBranch(
			long companyId,
			long distributorId,
			long distributorGeofenceId,
			String points,
			Map<String, Object> configInfo,
			String serviceSid,
			String appKey,
			Map<String, Object> geofenceInfo,
			String name) {
		DistributorGeofence existing = distributorGeofenceMapper.selectOne(new LambdaQueryWrapper<DistributorGeofence>()
				.eq(DistributorGeofence::getId, distributorGeofenceId)
				.eq(DistributorGeofence::getCompanyId, companyId)
				.eq(DistributorGeofence::getDistributorId, distributorId)
				.last("LIMIT 1"));
		if (existing == null) {
			throw new ForbiddenException("该围栏信息不属于当前店铺！");
		}

		Map<String, Object> dbDecoded = decodeDbGeofenceData(existing.getGeofenceData());
		Object dbName = dbDecoded.get("name");
		if (dbName != null && StringUtils.hasText(String.valueOf(dbName))) {
			geofenceInfo.put("name", String.valueOf(dbName).trim());
		}
		geofenceInfo.put("geofence_id", existing.getGeofenceId() != null ? existing.getGeofenceId() : "");

		JsonNode dbNode = objectMapper.valueToTree(dbDecoded);
		JsonNode reqNode = objectMapper.valueToTree(geofenceInfo);
		if (dbNode.equals(reqNode)) {
			return distributorGeofenceResponseMapper.handleData(existing);
		}

		if ("amap".equals(configInfo.get("type"))) {
			String gfid = geofenceInfo.get("geofence_id") == null ? "" : String.valueOf(geofenceInfo.get("geofence_id"));
			String apiName = Objects.toString(geofenceInfo.get("name"), "");
			if (!StringUtils.hasText(apiName)) {
				apiName = name;
			}
			boolean ok = amapTrackRestClient.updatePolygonGeofence(appKey, serviceSid, gfid, apiName, null, points);
			if (!ok) {
				throw new ResourceException("更新围栏失败！");
			}
		}

		String resultingGfid = geofenceInfo.get("geofence_id") == null ? "" : String.valueOf(geofenceInfo.get("geofence_id"));
		try {
			existing.setGeofenceId(resultingGfid);
			existing.setGeofenceData(objectMapper.writeValueAsString(geofenceInfo));
			existing.setStatus(1);
			existing.setUpdated((int) Instant.now().getEpochSecond());
		} catch (JsonProcessingException e) {
			throw new ResourceException("更新围栏失败！");
		}
		distributorGeofenceMapper.updateById(existing);
		return distributorGeofenceResponseMapper.handleData(existing);
	}

	private Map<String, Object> saveInsertBranch(
			long companyId,
			long distributorId,
			String points,
			Map<String, Object> configInfo,
			String serviceSid,
			String appKey,
			Map<String, Object> geofenceInfo,
			String name,
			Map<String, Object> serviceInfo) {
		String newGfid;
		if ("amap".equals(configInfo.get("type"))) {
			Map<String, Object> addRes = amapTrackRestClient.addPolygonGeofence(appKey, serviceSid, name, null, points);
			Object gfidObj = addRes.get("gfid");
			if (gfidObj == null || !StringUtils.hasText(String.valueOf(gfidObj))) {
				throw new ResourceException("创建围栏失败！");
			}
			newGfid = String.valueOf(gfidObj).trim();
		} else {
			newGfid = "";
		}
		geofenceInfo.put("geofence_id", newGfid);

		long configServiceLocalId = ((Number) serviceInfo.get("id")).longValue();
		int now = (int) Instant.now().getEpochSecond();
		DistributorGeofence row = new DistributorGeofence();
		row.setCompanyId(companyId);
		row.setDistributorId(distributorId);
		row.setConfigServiceLocalId(configServiceLocalId);
		row.setGeofenceId(newGfid);
		try {
			row.setGeofenceData(objectMapper.writeValueAsString(geofenceInfo));
		} catch (JsonProcessingException e) {
			throw new ResourceException("创建围栏失败！");
		}
		row.setStatus(1);
		row.setCreated(now);
		row.setUpdated(now);
		distributorGeofenceMapper.insert(row);
		return distributorGeofenceResponseMapper.handleData(row);
	}

	private Map<String, Object> decodeDbGeofenceData(String raw) {
		if (!StringUtils.hasText(raw)) {
			return new LinkedHashMap<>();
		}
		try {
			Map<String, Object> m = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
			return m != null ? new LinkedHashMap<>(m) : new LinkedHashMap<>();
		} catch (Exception e) {
			return new LinkedHashMap<>();
		}
	}

	private static boolean isEmptyCoordinateValue(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof Number n) {
			return n.doubleValue() == 0.0;
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return true;
		}
		return "0".equals(s);
	}

	private static String coordToString(Object v) {
		if (v instanceof Number n) {
			return String.valueOf(n);
		}
		return v == null ? "" : v.toString().trim();
	}
}
