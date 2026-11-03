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

import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DigiOsAccountSyncDispatchService {

	private static final Logger log = LoggerFactory.getLogger(DigiOsAccountSyncDispatchService.class);

	/** G-D1：Operator 路径仅 staff；distributor 不再同步。 */
	private static final Set<String> SYNC_OPERATOR_TYPES = Set.of("staff");

	private final DigiOsConfigService configService;
	private final DigiOsOperatorLoadService operatorLoadService;
	private final DigiOsPosAccountLoadService posAccountLoadService;
	private final DigiOsAccountSyncService accountSyncService;

	public DigiOsAccountSyncDispatchService(
			DigiOsConfigService configService,
			DigiOsOperatorLoadService operatorLoadService,
			DigiOsPosAccountLoadService posAccountLoadService,
			DigiOsAccountSyncService accountSyncService) {
		this.configService = configService;
		this.operatorLoadService = operatorLoadService;
		this.posAccountLoadService = posAccountLoadService;
		this.accountSyncService = accountSyncService;
	}

	public void dispatchIfEnabled(long companyId, long operatorId, String operatorTypeHint) {
		if (companyId < 1 || operatorId < 1) {
			return;
		}
		if (!configService.resolve(companyId).enabled()) {
			return;
		}
		String operatorType = operatorTypeHint == null ? "" : operatorTypeHint.trim();
		if (!SYNC_OPERATOR_TYPES.contains(operatorType)) {
			return;
		}
		Map<String, Object> operator = operatorLoadService.loadOperator(companyId, operatorId);
		if (operator.isEmpty()) {
			log.info("digios account sync skip operator not found companyId={} operatorId={}", companyId, operatorId);
			return;
		}
		log.info("digios account sync dispatched companyId={} operatorId={}", companyId, operatorId);
		accountSyncService.syncCreate(companyId, operator);
	}

	public void dispatchPosIfEnabled(long companyId, long posStoreAccountId) {
		if (companyId < 1 || posStoreAccountId < 1) {
			return;
		}
		if (!configService.resolve(companyId).enabled()) {
			return;
		}
		Map<String, Object> pos = posAccountLoadService.loadPosAccount(companyId, posStoreAccountId);
		if (pos.isEmpty()) {
			log.info(
					"digios account sync skip pos account not found companyId={} posStoreAccountId={}",
					companyId,
					posStoreAccountId);
			return;
		}
		log.info(
				"digios pos account sync dispatched companyId={} posStoreAccountId={}",
				companyId,
				posStoreAccountId);
		accountSyncService.syncCreatePos(companyId, pos);
	}
}
