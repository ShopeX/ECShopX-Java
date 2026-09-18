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

package cn.shopex.ecshopx.hfpay.service.cashrecord;

import cn.shopex.ecshopx.hfpay.domain.HfpayCashRecord;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HfpayCashRecordRowConverter {

	private static final DateTimeFormatter HFPAY_ADMIN_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private HfpayCashRecordRowConverter() {}

	public static Map<String, Object> columnNamesData(HfpayCashRecord e) {
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map.put("hfpay_cash_record_id", e.getHfpayCashRecordId());
		map.put("company_id", e.getCompanyId());
		map.put("distributor_id", e.getDistributorId());
		map.put("order_id", e.getOrderId());
		map.put("user_id", e.getUserId());
		map.put("operator_id", e.getOperatorId());
		map.put("user_cust_id", e.getUserCustId());
		map.put("trans_amt", e.getTransAmt());
		map.put("cash_type", e.getCashType());
		map.put("bind_card_id", e.getBindCardId());
		map.put("real_trans_amt", e.getRealTransAmt());
		map.put("fee_amt", e.getFeeAmt());
		map.put("cash_status", e.getCashStatus());
		map.put("resp_code", e.getRespCode());
		map.put("resp_desc", e.getRespDesc());
		map.put("hf_order_id", e.getHfOrderId());
		map.put("hf_order_date", e.getHfOrderDate());
		map.put("created_at", formatTs(e.getCreatedAt()));
		map.put("updated_at", formatTs(e.getUpdatedAt()));
		return map;
	}

	private static String formatTs(LocalDateTime t) {
		return t == null ? null : HFPAY_ADMIN_TS.format(t);
	}
}
