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

package cn.shopex.ecshopx.goods.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchHandler;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.repository.ItemsMedicineRepository;
import cn.shopex.ecshopx.thirdparty.service.kuaizhen580.MedicineSyncHttpClient;
import cn.shopex.ecshopx.thirdparty.service.kuaizhen580.MedicineSyncMedicineItem;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MedicineItemsSubmitAuditJobHandler implements DispatchHandler {

	private static final Logger log = LoggerFactory.getLogger(MedicineItemsSubmitAuditJobHandler.class);

	private final MedicineSyncHttpClient medicineSyncHttpClient;
	private final ItemsMedicineRepository itemsMedicineRepository;

	public MedicineItemsSubmitAuditJobHandler(
			MedicineSyncHttpClient medicineSyncHttpClient, ItemsMedicineRepository itemsMedicineRepository) {
		this.medicineSyncHttpClient = medicineSyncHttpClient;
		this.itemsMedicineRepository = itemsMedicineRepository;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void handle(Map<String, Object> payload) {
		long companyId = toLong(payload.get("company_id"));
		long itemId = toLong(payload.get("item_id"));
		String itemName = str(payload.get("item_name"));
		String barcode = str(payload.get("barcode"));
		int price = payload.get("price") instanceof Number n ? n.intValue() : 0;
		Map<String, Object> med = (Map<String, Object>) payload.get("medicine_data");

		Integer medicineType = intOrNull(med.get("medicine_type"));
		Integer isPrescriptionRaw = intOrNull(med.get("is_prescription"));
		int isPrescriptionForApi = Integer.valueOf(1).equals(isPrescriptionRaw) ? 0 : 1;
		String priceStr = String.valueOf(price);

		MedicineSyncMedicineItem item =
				new MedicineSyncMedicineItem(
						medicineType,
						str(med.get("common_name")),
						itemName,
						str(med.get("dosage")),
						str(med.get("spec")),
						str(med.get("packing_spec")),
						str(med.get("manufacturer")),
						str(med.get("approval_number")),
						str(med.get("unit")),
						itemId,
						barcode,
						isPrescriptionForApi,
						priceStr,
						"",
						str(med.get("special_common_name")),
						str(med.get("special_spec")));
		try {
			medicineSyncHttpClient.medicineSync(companyId, List.of(item));
			log.debug("MedicineItemsSubmitAudit itemId={} submitted", itemId);
		} catch (Exception e) {
			String auditMsg =
					(e instanceof ResourceException re && StringUtils.hasText(re.getMessage()))
							? re.getMessage()
							: "快诊580药品同步失败";
			itemsMedicineRepository.updateAuditResultByItemId(itemId, 0, auditMsg);
			log.debug("MedicineItemsSubmitAudit itemId={} error: {}", itemId, auditMsg);
		}
	}

	private static Integer intOrNull(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}
}
