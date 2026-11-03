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

package cn.shopex.ecshopx.digios.service;

import cn.shopex.ecshopx.digios.client.DigiOsGatewayClient;
import cn.shopex.ecshopx.digios.client.DigiOsGatewayResult;
import cn.shopex.ecshopx.digios.client.DigiOsUpsertResponse;
import cn.shopex.ecshopx.digios.config.DigiOsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DigiOsStoreProfileSyncService {

	private static final Logger log = LoggerFactory.getLogger(DigiOsStoreProfileSyncService.class);

	private final DigiOsGatewayClient gatewayClient;
	private final DigiOsRegionMapper regionMapper;
	private final DigiOsStoreMappingService mappingService;
	private final DigiOsProperties properties;
	private final ObjectMapper objectMapper;

	public DigiOsStoreProfileSyncService(
			DigiOsGatewayClient gatewayClient,
			DigiOsRegionMapper regionMapper,
			DigiOsStoreMappingService mappingService,
			DigiOsProperties properties,
			ObjectMapper objectMapper) {
		this.gatewayClient = gatewayClient;
		this.regionMapper = regionMapper;
		this.mappingService = mappingService;
		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	public boolean syncCreate(long companyId, Map<String, Object> distributor) {
		Map<String, Object> payload = buildCreatePayload(distributor);
		if (payload == null) {
			return false;
		}
		String shopCode = trim(distributor.get("shop_code"));
		long distributorId = toLong(distributor.get("distributor_id"));

		DigiOsGatewayResult result = gatewayClient.put(companyId, "store.create", payload);
		var createResult = DigiOsUpsertResponse.resolveWithNumericId(result, shopCode);
		if (!createResult.ok()) {
			log.info(
					"digios store profile create failed companyId={} distributorId={} shopCode={} error={} httpStatus={}",
					companyId,
					distributorId,
					shopCode,
					createResult.error(),
					result.httpStatus());
			return false;
		}
		mappingService.saveDigiosStoreId(companyId, distributorId, shopCode, createResult.id());
		log.info(
				"digios store profile create finished companyId={} distributorId={} shopCode={} digiosStoreId={}",
				companyId,
				distributorId,
				shopCode,
				createResult.id());
		return true;
	}

	public boolean syncUpdate(long companyId, Map<String, Object> distributor) {
		long distributorId = toLong(distributor.get("distributor_id"));
		String shopCode = trim(distributor.get("shop_code"));
		Long digiosStoreId = mappingService.findDigiosStoreId(companyId, distributorId);
		if (digiosStoreId == null) {
			log.info(
					"digios skip store profile update, no mapping companyId={} distributorId={} shopCode={}",
					companyId,
					distributorId,
					shopCode);
			return true;
		}
		Map<String, Object> payload = buildUpdatePayload(distributor, digiosStoreId);
		if (payload == null) {
			return false;
		}
		DigiOsGatewayResult result = gatewayClient.post(companyId, "store.update", payload);
		var updateResult = DigiOsUpsertResponse.resolveMutation(result);
		if (!updateResult.ok()) {
			log.info(
					"digios store profile update failed companyId={} distributorId={} shopCode={} error={}",
					companyId,
					distributorId,
					shopCode,
					updateResult.error());
			return false;
		}
		log.info(
				"digios store profile update finished companyId={} distributorId={} shopCode={} digiosStoreId={}",
				companyId,
				distributorId,
				shopCode,
				digiosStoreId);
		return true;
	}

	public Map<String, Object> buildCreatePayload(Map<String, Object> distributor) {
		Map<String, Object> base = buildCommonPayload(distributor);
		if (base == null) {
			return null;
		}
		String shopCode = trim(distributor.get("shop_code"));
		if (!StringUtils.hasText(shopCode)) {
			return null;
		}
		base.put("cd", shopCode);
		return base;
	}

	public Map<String, Object> buildUpdatePayload(Map<String, Object> distributor, long digiosStoreId) {
		Map<String, Object> base = buildCommonPayload(distributor);
		if (base == null) {
			return null;
		}
		base.put("id", digiosStoreId);
		return base;
	}

	private Map<String, Object> buildCommonPayload(Map<String, Object> distributor) {
		String shopCode = trim(distributor.get("shop_code"));
		String name = trim(distributor.get("name"));
		if (!StringUtils.hasText(shopCode) || !StringUtils.hasText(name)) {
			log.info("digios skip store profile sync, missing shop_code or name distributorId={}", distributor.get("distributor_id"));
			return null;
		}
		String areaCd = regionMapper.resolveAreaCd(
				distributor.get("regions_id"),
				stringOrNull(distributor.get("province")),
				stringOrNull(distributor.get("city")),
				stringOrNull(distributor.get("area")));
		if (!StringUtils.hasText(areaCd)) {
			log.info("digios skip store profile sync, areaCd unresolved shopCode={}", shopCode);
			return null;
		}
		String addr = DigiOsStoreProfileMapper.composeAddress(distributor);
		if (!StringUtils.hasText(addr)) {
			log.info("digios skip store profile sync, empty address shopCode={}", shopCode);
			return null;
		}
		String contact = trim(distributor.get("contact"));
		if (!StringUtils.hasText(contact)) {
			log.info("digios skip store profile sync, empty contact shopCode={}", shopCode);
			return null;
		}
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("nm", name);
		payload.put("coCode", properties.getCoCode());
		payload.put("bizModel", properties.getBizModel());
		payload.put(
				"opHours",
				DigiOsStoreProfileMapper.formatOpHours(
						distributor.get("hour"), properties.getDefaultOpHours(), objectMapper));
		payload.put("areaCd", areaCd);
		payload.put("addr", addr);
		payload.put("ctcs", contact);
		payload.put("mb", trim(distributor.get("mobile")));
		payload.put("status", DigiOsStoreProfileMapper.mapStatus(distributor.get("is_valid")));
		payload.put("terCd", properties.getTerCd());
		return payload;
	}

	private static String trim(Object raw) {
		return raw == null ? "" : String.valueOf(raw).trim();
	}

	private static String stringOrNull(Object raw) {
		String s = trim(raw);
		return StringUtils.hasText(s) ? s : null;
	}

	private static long toLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (Exception e) {
			return 0L;
		}
	}
}
