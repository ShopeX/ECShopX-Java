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

package cn.shopex.ecshopx.config;

import cn.shopex.ecshopx.common.dispatch.GoodsBundleDispatchJobNames;
import cn.shopex.ecshopx.dispatch.DispatchRegistry;
import cn.shopex.ecshopx.goods.dispatch.MedicineItemsSubmitAuditJobHandler;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MedicineItemsSubmitAuditJobRegistrationConfig {

	private final DispatchRegistry dispatchRegistry;
	private final MedicineItemsSubmitAuditJobHandler medicineItemsSubmitAuditJobHandler;

	public MedicineItemsSubmitAuditJobRegistrationConfig(
			DispatchRegistry dispatchRegistry, MedicineItemsSubmitAuditJobHandler medicineItemsSubmitAuditJobHandler) {
		this.dispatchRegistry = dispatchRegistry;
		this.medicineItemsSubmitAuditJobHandler = medicineItemsSubmitAuditJobHandler;
	}

	@PostConstruct
	public void registerMedicineItemsSubmitAuditJob() {
		dispatchRegistry.registerJob(
				GoodsBundleDispatchJobNames.MEDICINE_ITEMS_SUBMIT_AUDIT, medicineItemsSubmitAuditJobHandler);
	}
}
