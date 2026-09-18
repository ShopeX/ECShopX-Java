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

package cn.shopex.ecshopx.hfpay.service.profit;

import cn.shopex.ecshopx.hfpay.service.payment.HfPayAcouJsonPostClient;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayOrderApplyIdGenerator;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayPaymentSettingService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 汇付 pay006 延时分账确认；对齐 PHP HfpayService::pay006。
 */
@Service
@RequiredArgsConstructor
public class HfpayProfitSplitConfirmService {

	private static final DateTimeFormatter ORDER_DATE = DateTimeFormatter.BASIC_ISO_DATE;

	private final HfPayPaymentSettingService hfPayPaymentSettingService;
	private final HfPayAcouJsonPostClient hfPayAcouJsonPostClient;
	private final HfPayOrderApplyIdGenerator hfPayOrderApplyIdGenerator;

	public Map<String, Object> pay006(
			long companyId,
			String orgOrderId,
			String orgOrderDateYmd,
			String orgTransType,
			String transAmtYuan,
			String divDetailsJson) {
		Map<String, Object> setting = hfPayPaymentSettingService.loadForCompany(companyId);
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("version", "10");
		String mer = String.valueOf(setting.get("mer_cust_id")).trim();
		payload.put("mer_cust_id", mer);
		payload.put("order_date", LocalDate.now(ZoneId.systemDefault()).format(ORDER_DATE));
		payload.put("order_id", hfPayOrderApplyIdGenerator.nextOrderId());
		payload.put("org_order_id", str(orgOrderId));
		payload.put("org_order_date", str(orgOrderDateYmd));
		payload.put("org_trans_type", str(orgTransType));
		payload.put("trans_amt", str(transAmtYuan));
		payload.put("div_details", str(divDetailsJson));
		return hfPayAcouJsonPostClient.pay006(setting, payload);
	}

	private static String str(String s) {
		return s == null ? "" : s.trim();
	}

	public String extractRespCode(Map<String, Object> res) {
		if (res == null || res.get("resp_code") == null) {
			return "";
		}
		return String.valueOf(res.get("resp_code")).trim();
	}
}
