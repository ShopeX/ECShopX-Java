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

package cn.shopex.ecshopx.distribution.integration;

import cn.shopex.ecshopx.common.dispatch.DistributionEditEventDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.DistributorUpdateEventDispatchPublisher;
import cn.shopex.ecshopx.common.distribution.SelfDistributorDisplayUpdatePort;
import cn.shopex.ecshopx.distribution.repository.DistributorWriteRepository;
import cn.shopex.ecshopx.distribution.service.DistributorUpdateService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class SelfDistributorDisplayUpdatePortImpl implements SelfDistributorDisplayUpdatePort {

	private final DistributorWriteRepository distributorWriteRepository;
	private final DistributorUpdateService distributorUpdateService;
	private final DistributionEditEventDispatchPublisher distributionEditEventDispatchPublisher;
	private final DistributorUpdateEventDispatchPublisher distributorUpdateEventDispatchPublisher;

	public SelfDistributorDisplayUpdatePortImpl(
			DistributorWriteRepository distributorWriteRepository,
			DistributorUpdateService distributorUpdateService,
			DistributionEditEventDispatchPublisher distributionEditEventDispatchPublisher,
			DistributorUpdateEventDispatchPublisher distributorUpdateEventDispatchPublisher) {
		this.distributorWriteRepository = distributorWriteRepository;
		this.distributorUpdateService = distributorUpdateService;
		this.distributionEditEventDispatchPublisher = distributionEditEventDispatchPublisher;
		this.distributorUpdateEventDispatchPublisher = distributorUpdateEventDispatchPublisher;
	}

	@Override
	public void syncSelfDistributorBrandAndLogoIfPresent(
			long companyId, String brandName, String logo, String requestLangTag) {
		Optional<Long> idOpt = distributorWriteRepository.findDistributorSelfId(companyId);
		if (idOpt.isEmpty()) {
			return;
		}
		long distributorId = idOpt.get();
		if (distributorId <= 0L) {
			return;
		}
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("company_id", companyId);
		merged.put("name", brandName == null ? "" : brandName);
		merged.put("logo", logo == null ? "" : logo);
		Map<String, Object> row =
				distributorUpdateService.performUpdateAndEvents(merged, distributorId, requestLangTag);
		scheduleDistributionEditAndDistributorUpdateDispatch(row);
	}

	/** Same ordering as {@code DistributorUpdateOrchestrator#update} after-commit publishes; immediate if no tx sync. */
	private void scheduleDistributionEditAndDistributorUpdateDispatch(Map<String, Object> row) {
		Map<String, Object> rowSnapshot = new LinkedHashMap<>(row);
		Runnable publishBoth = () -> {
			distributionEditEventDispatchPublisher.publish(rowSnapshot);
			distributorUpdateEventDispatchPublisher.publish(rowSnapshot);
		};
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					publishBoth.run();
				}
			});
		} else {
			publishBoth.run();
		}
	}
}
