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

package cn.shopex.ecshopx.orders.service.refund;

import cn.shopex.ecshopx.orders.domain.RefundErrorLogs;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RefundErrorLogsRowResponseSupport {

	private RefundErrorLogsRowResponseSupport() {
	}

	public static Map<String, Object> row(RefundErrorLogs e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("company_id", e.getCompanyId());
		m.put("order_id", e.getOrderId());
		m.put("supplier_id", e.getSupplierId());
		m.put("wxa_appid", e.getWxaAppid());
		m.put("data_json", e.getDataJson());
		m.put("status", e.getStatus());
		m.put("error_code", e.getErrorCode());
		m.put("error_desc", e.getErrorDesc());
		m.put("is_resubmit", e.getIsResubmit());
		m.put("create_time", e.getCreateTime());
		m.put("update_time", e.getUpdateTime());
		m.put("merchant_id", e.getMerchantId());
		m.put("distributor_id", e.getDistributorId());
		return m;
	}
}
