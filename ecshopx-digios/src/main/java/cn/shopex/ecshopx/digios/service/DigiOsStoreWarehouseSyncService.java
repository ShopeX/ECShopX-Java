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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DigiOsStoreWarehouseSyncService {

	private static final Logger log = LoggerFactory.getLogger(DigiOsStoreWarehouseSyncService.class);

	private final DigiOsConfigService configService;
	private final DigiOsRegionMapper regionMapper;
	private final DigiOsGatewayClient gatewayClient;
	private final DigiOsProperties properties;

	public DigiOsStoreWarehouseSyncService(
			DigiOsConfigService configService,
			DigiOsRegionMapper regionMapper,
			DigiOsGatewayClient gatewayClient,
			DigiOsProperties properties) {
		this.configService = configService;
		this.regionMapper = regionMapper;
		this.gatewayClient = gatewayClient;
		this.properties = properties;
	}

	public boolean sync(long companyId, Map<String, Object> distributor) {
		if (!configService.resolve(companyId).enabled()) {
			return true;
		}
		String shopCode = trim(distributor.get("shop_code"));
		String name = trim(distributor.get("name"));
		if (!StringUtils.hasText(shopCode) || !StringUtils.hasText(name)) {
			log.info("digios skip warehouse sync, missing shop_code or name companyId={}", companyId);
			return true;
		}
		WarehousePayloads payloads = buildPayloads(distributor, properties.getCoCode());
		if (payloads == null) {
			return false;
		}

		DigiOsGatewayResult warehouseResult = gatewayClient.put(companyId, "warehouse.upsert", payloads.warehouse);
		var warehouseParsed = DigiOsUpsertResponse.resolveWithNumericId(warehouseResult, shopCode);
		if (!warehouseParsed.ok()) {
			log.info(
					"digios warehouse sync failed companyId={} shopCode={} error={}",
					companyId,
					shopCode,
					warehouseParsed.error());
			return false;
		}

		for (Map<String, Object> logicPayload : payloads.logic) {
			String logicCd = trim(logicPayload.get("cd"));
			DigiOsGatewayResult logicResult = gatewayClient.put(companyId, "logic.upsert", logicPayload);
			var logicParsed = DigiOsUpsertResponse.resolveWithNumericId(logicResult, logicCd);
			if (!logicParsed.ok()) {
				log.info(
						"digios logic warehouse sync failed companyId={} shopCode={} logicCd={} error={}",
						companyId,
						shopCode,
						logicCd,
						logicParsed.error());
				return false;
			}
		}
		log.info("digios store warehouse sync finished companyId={} shopCode={}", companyId, shopCode);
		return true;
	}

	public WarehousePayloads buildPayloads(Map<String, Object> distributor, String coCode) {
		Map<String, Object> warehouse = buildWarehousePayload(distributor, coCode);
		if (warehouse == null) {
			return null;
		}
		String shopCode = trim(distributor.get("shop_code"));
		String name = trim(distributor.get("name"));
		List<Map<String, Object>> logic = new ArrayList<>();
		logic.add(logicPayload(shopCode, name + "正品仓", 0, shopCode));
		logic.add(logicPayload(shopCode + "-1", name + "次品仓", 1, shopCode));
		return new WarehousePayloads(warehouse, logic);
	}

	public Map<String, Object> buildWarehousePayload(Map<String, Object> distributor, String coCode) {
		String shopCode = trim(distributor.get("shop_code"));
		String name = trim(distributor.get("name"));
		if (!StringUtils.hasText(shopCode) || !StringUtils.hasText(name)) {
			return null;
		}
		String areaCd = regionMapper.resolveAreaCd(
				distributor.get("regions_id"),
				stringOrNull(distributor.get("province")),
				stringOrNull(distributor.get("city")),
				stringOrNull(distributor.get("area")));
		if (!StringUtils.hasText(areaCd)) {
			log.info("digios skip warehouse sync, areaCd unresolved shopCode={}", shopCode);
			return null;
		}
		String addr = (trim(distributor.get("address")) + " " + trim(distributor.get("house_number"))).trim();
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("cd", shopCode);
		payload.put("nm", name);
		payload.put("coCode", StringUtils.hasText(coCode) ? coCode : properties.getCoCode());
		payload.put("whTyp", 1);
		payload.put("tmlCd", "ECX");
		payload.put("areaCd", areaCd);
		payload.put("addr", addr);
		payload.put("ctcs", trim(distributor.get("contact")));
		payload.put("mb", trim(distributor.get("mobile")));
		return payload;
	}

	private static Map<String, Object> logicPayload(String cd, String nm, int typ, String wareCd) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("cd", cd);
		m.put("nm", nm);
		m.put("typ", typ);
		m.put("wareCd", wareCd);
		m.put("def", 0);
		return m;
	}

	public record WarehousePayloads(Map<String, Object> warehouse, List<Map<String, Object>> logic) {}

	private static String trim(Object raw) {
		return raw == null ? "" : String.valueOf(raw).trim();
	}

	private static String stringOrNull(Object raw) {
		String s = trim(raw);
		return StringUtils.hasText(s) ? s : null;
	}
}
