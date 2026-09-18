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

package cn.shopex.ecshopx.thirdparty.service.ome;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ShopUpdateSendOmeExecutionService {

	private static final Logger log = LoggerFactory.getLogger(ShopUpdateSendOmeExecutionService.class);

	private final ShopexErpOpenApiSettingReadPort shopexErpOpenApiSettingReadPort;
	private final OmeDistributorShopStructPort omeDistributorShopStructPort;
	private final OmeOpenApiShopUpdatePort omeOpenApiShopUpdatePort;

	public ShopUpdateSendOmeExecutionService(
			ShopexErpOpenApiSettingReadPort shopexErpOpenApiSettingReadPort,
			OmeDistributorShopStructPort omeDistributorShopStructPort,
			OmeOpenApiShopUpdatePort omeOpenApiShopUpdatePort) {
		this.shopexErpOpenApiSettingReadPort = shopexErpOpenApiSettingReadPort;
		this.omeDistributorShopStructPort = omeDistributorShopStructPort;
		this.omeOpenApiShopUpdatePort = omeOpenApiShopUpdatePort;
	}

	public void executeFromDispatchPayload(Map<String, Object> payload) {
		try {
			run(payload);
		} catch (Exception e) {
			log.debug("shop update OME dispatch failed: {}", e.getMessage(), e);
		}
	}

	private void run(Map<String, Object> payload) {
		Object rawEntities = payload == null ? null : payload.get("entities");
		if (!(rawEntities instanceof Map<?, ?> rawMap)) {
			log.debug("shop update OME skipped: missing entities map");
			return;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> entities = (Map<String, Object>) rawMap;
		Long companyId = longObject(entities.get("company_id"));
		Long distributorId = longObject(entities.get("distributor_id"));
		if (companyId == null || companyId <= 0L || distributorId == null || distributorId <= 0L) {
			log.debug("shop update OME skipped: invalid company_id or distributor_id");
			return;
		}
		var settingOpt = shopexErpOpenApiSettingReadPort.loadParsed(companyId);
		if (settingOpt.isEmpty()) {
			log.debug("shop update OME skipped: no ERP setting companyId={}", companyId);
			return;
		}
		Map<String, Object> setting = settingOpt.get();
		if (!Boolean.TRUE.equals(setting.get("is_openapi_open"))) {
			log.debug("shop update OME skipped: openapi not enabled companyId={}", companyId);
			return;
		}
		var structOpt = omeDistributorShopStructPort.tryBuildShopUpdate(companyId, distributorId);
		if (structOpt.isEmpty()) {
			log.debug("shop update OME skipped: empty shop struct companyId={} distributorId={}", companyId, distributorId);
			return;
		}
		Map<String, Object> shopStruct = structOpt.get();
		Map<String, Object> response = omeOpenApiShopUpdatePort.callShopUpdate(companyId, shopStruct);
		log.debug("shop update OME shop.update companyId={} distributorId={} rsp={}", companyId, distributorId, response);
	}

	private static Long longObject(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			return null;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
