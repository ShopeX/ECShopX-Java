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
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import cn.shopex.ecshopx.goods.dispatch.MedicineItemsSubmitAuditDispatchPublisher;
import cn.shopex.ecshopx.goods.domain.ItemsMedicine;
import cn.shopex.ecshopx.goods.service.items.MedicineSubmitAuditPayload;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MedicineItemsSubmitAuditDispatchPublisherImpl implements MedicineItemsSubmitAuditDispatchPublisher {

	private final DispatchFacade dispatchFacade;

	public MedicineItemsSubmitAuditDispatchPublisherImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public void publishMedicineItemSubmitAudit(MedicineSubmitAuditPayload payload) {
		ItemsMedicine m = payload.medicineRow();
		LinkedHashMap<String, Object> medicineData = new LinkedHashMap<>();
		medicineData.put("medicine_type", m.getMedicineType());
		medicineData.put("common_name", str(m.getCommonName()));
		medicineData.put("dosage", str(m.getDosage()));
		medicineData.put("spec", str(m.getSpec()));
		medicineData.put("packing_spec", str(m.getPackingSpec()));
		medicineData.put("manufacturer", str(m.getManufacturer()));
		medicineData.put("approval_number", str(m.getApprovalNumber()));
		medicineData.put("unit", str(m.getUnit()));
		medicineData.put("is_prescription", m.getIsPrescription());
		medicineData.put("special_common_name", str(m.getSpecialCommonName()));
		medicineData.put("special_spec", str(m.getSpecialSpec()));

		Map<String, Object> map = new LinkedHashMap<>();
		map.put("company_id", payload.companyId());
		map.put("item_id", payload.itemId());
		map.put("item_name", str(payload.itemName()));
		map.put("barcode", str(payload.barcode()));
		map.put("price", payload.price());
		map.put("medicine_data", medicineData);

		dispatchFacade.dispatchJob(
				GoodsBundleDispatchJobNames.MEDICINE_ITEMS_SUBMIT_AUDIT,
				map,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));
	}

	private static String str(String s) {
		if (s == null) {
			return "";
		}
		return s;
	}
}
