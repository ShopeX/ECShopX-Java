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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DigiOsStoreAdjustmentSyncDispatchService {

	private static final Logger log = LoggerFactory.getLogger(DigiOsStoreAdjustmentSyncDispatchService.class);

	private final DigiOsConfigService configService;
	private final DigiOsStoreAdjustmentSyncService adjustmentSyncService;

	public DigiOsStoreAdjustmentSyncDispatchService(
			DigiOsConfigService configService, DigiOsStoreAdjustmentSyncService adjustmentSyncService) {
		this.configService = configService;
		this.adjustmentSyncService = adjustmentSyncService;
	}

	public void dispatchOnDelivery(long companyId, long orderId) {
		if (companyId < 1L || orderId < 1L) {
			return;
		}
		if (!configService.resolve(companyId).enabled()) {
			return;
		}
		log.info("digios store adjustment delivery dispatched companyId={} orderId={}", companyId, orderId);
		adjustmentSyncService.syncOnDelivery(companyId, orderId);
	}

	public void dispatchOnAftersalesReturn(long companyId, long aftersalesBn) {
		if (companyId < 1L || aftersalesBn < 1L) {
			return;
		}
		if (!configService.resolve(companyId).enabled()) {
			return;
		}
		log.info(
				"digios store adjustment aftersales dispatched companyId={} aftersalesBn={}",
				companyId,
				aftersalesBn);
		adjustmentSyncService.syncOnAftersalesReturn(companyId, aftersalesBn);
	}
}
