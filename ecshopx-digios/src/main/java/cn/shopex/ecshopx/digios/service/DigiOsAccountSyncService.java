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

import cn.shopex.ecshopx.digios.client.DigiOsAccountResponse;
import cn.shopex.ecshopx.digios.client.DigiOsGatewayClient;
import cn.shopex.ecshopx.digios.client.DigiOsGatewayResult;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
@Service
public class DigiOsAccountSyncService {

	private static final Logger log = LoggerFactory.getLogger(DigiOsAccountSyncService.class);

	/** G-D1：仅保留 staff；distributor 改由 POS 账号同步。 */
	private static final Set<String> SYNC_OPERATOR_TYPES = Set.of("staff");

	private final DigiOsGatewayClient gatewayClient;

	public DigiOsAccountSyncService(DigiOsGatewayClient gatewayClient) {
		this.gatewayClient = gatewayClient;
	}

	public boolean syncCreate(long companyId, Map<String, Object> operator) {
		String operatorType = String.valueOf(operator.getOrDefault("operator_type", ""));
		long operatorId = toLong(operator.get("operator_id"));
		if (!SYNC_OPERATOR_TYPES.contains(operatorType)) {
			log.info(
					"digios skip account sync unsupported operator_type companyId={} operatorId={} type={}",
					companyId,
					operatorId,
					operatorType);
			return true;
		}
		return upsert(companyId, DigiOsAccountMapper.map(operator), "operatorId", operatorId);
	}

	/** POS 新建（含导入新增）同步；mobile 空则 skip（G-D3）。 */
	public boolean syncCreatePos(long companyId, Map<String, Object> posAccount) {
		long posId = toLong(posAccount == null ? null : posAccount.get("id"));
		return upsert(companyId, DigiOsAccountMapper.mapPos(posAccount), "posStoreAccountId", posId);
	}

	public Map<String, String> buildPayload(Map<String, Object> operator) {
		return DigiOsAccountMapper.map(operator);
	}

	public Map<String, String> buildPosPayload(Map<String, Object> posAccount) {
		return DigiOsAccountMapper.mapPos(posAccount);
	}

	private boolean upsert(long companyId, Map<String, String> payload, String idLabel, long id) {
		if (payload == null) {
			log.info("digios skip account sync incomplete fields companyId={} {}={}", companyId, idLabel, id);
			return false;
		}
		Map<String, Object> biz = new java.util.LinkedHashMap<>(payload);
		DigiOsGatewayResult result = gatewayClient.put(companyId, "account.upsert", biz);
		var parsed = DigiOsAccountResponse.resolveCreateResult(result, payload.get("acc"));
		if (!parsed.ok()) {
			log.info(
					"digios account sync failed companyId={} {}={} acc={} error={}",
					companyId,
					idLabel,
					id,
					payload.get("acc"),
					parsed.error());
			return false;
		}
		log.info(
				"digios account sync finished companyId={} {}={} acc={} digiosUsrId={}",
				companyId,
				idLabel,
				id,
				parsed.acc(),
				parsed.usrId());
		return true;
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
