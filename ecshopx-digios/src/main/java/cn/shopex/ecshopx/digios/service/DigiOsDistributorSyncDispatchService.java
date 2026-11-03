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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DigiOsDistributorSyncDispatchService {

	private static final Logger log = LoggerFactory.getLogger(DigiOsDistributorSyncDispatchService.class);

	private final DigiOsConfigService configService;
	private final DigiOsDistributorLoadService loadService;
	private final DigiOsDistributorSyncService syncService;

	public DigiOsDistributorSyncDispatchService(
			DigiOsConfigService configService,
			DigiOsDistributorLoadService loadService,
			DigiOsDistributorSyncService syncService) {
		this.configService = configService;
		this.loadService = loadService;
		this.syncService = syncService;
	}

	public void dispatchCreateIfEnabled(long companyId, long distributorId) {
		if (companyId < 1 || distributorId < 1) {
			return;
		}
		if (!configService.resolve(companyId).enabled()) {
			return;
		}
		Map<String, Object> distributor = loadService.loadDistributor(companyId, distributorId);
		if (distributor.isEmpty()) {
			log.info("digios skip create sync, distributor not found companyId={} distributorId={}", companyId, distributorId);
			return;
		}
		log.info("digios distributor create sync dispatched companyId={} distributorId={}", companyId, distributorId);
		syncService.syncOnCreate(companyId, distributor);
	}

	public void dispatchUpdateIfEnabled(long companyId, long distributorId) {
		if (companyId < 1 || distributorId < 1) {
			return;
		}
		if (!configService.resolve(companyId).enabled()) {
			return;
		}
		Map<String, Object> distributor = loadService.loadDistributor(companyId, distributorId);
		if (distributor.isEmpty()) {
			return;
		}
		log.info("digios distributor update sync dispatched companyId={} distributorId={}", companyId, distributorId);
		syncService.syncOnUpdate(companyId, distributor);
	}
}
