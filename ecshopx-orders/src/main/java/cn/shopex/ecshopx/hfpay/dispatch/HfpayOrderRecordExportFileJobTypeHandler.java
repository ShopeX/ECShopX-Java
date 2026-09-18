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

package cn.shopex.ecshopx.hfpay.dispatch;

import cn.shopex.ecshopx.common.dispatch.ExportFileJobTypeHandler;
import cn.shopex.ecshopx.hfpay.service.export.HfpayOrderRecordExportFileJobHandler;
import cn.shopex.ecshopx.hfpay.service.export.HfpayOrderRecordExportFileJobTypes;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class HfpayOrderRecordExportFileJobTypeHandler implements ExportFileJobTypeHandler {

	private final HfpayOrderRecordExportFileJobHandler jobHandler;

	public HfpayOrderRecordExportFileJobTypeHandler(HfpayOrderRecordExportFileJobHandler jobHandler) {
		this.jobHandler = jobHandler;
	}

	@Override
	public String exportType() {
		return HfpayOrderRecordExportFileJobTypes.TYPE_HFPAY_ORDER_RECORD;
	}

	@Override
	public void handle(Map<String, Object> payload) {
		long companyId = extractRequiredLong(payload, "company_id");
		long operatorId = extractRequiredLong(payload, "operator_id");
		LinkedHashMap<String, Object> filter = extractFilterMap(payload.get("filter"));
		jobHandler.run(companyId, operatorId, filter);
	}

	private static LinkedHashMap<String, Object> extractFilterMap(Object raw) {
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		if (raw instanceof Map<?, ?> m) {
			for (Map.Entry<?, ?> e : m.entrySet()) {
				out.put(String.valueOf(e.getKey()), e.getValue());
			}
		}
		return out;
	}

	private static long extractRequiredLong(Map<String, Object> payload, String key) {
		Object v = payload.get(key);
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}
}
