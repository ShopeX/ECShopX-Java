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

package cn.shopex.ecshopx.hfpay.service.statistics;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayAcouJsonPostClient;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayPaymentSettingService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HfpayAcouQry001BalanceFenService {

	private final HfPayPaymentSettingService paymentSettingService;
	private final HfPayAcouJsonPostClient acouJsonPostClient;

	public HfpayAcouQry001BalanceFenService(
			HfPayPaymentSettingService paymentSettingService, HfPayAcouJsonPostClient acouJsonPostClient) {
		this.paymentSettingService = paymentSettingService;
		this.acouJsonPostClient = acouJsonPostClient;
	}

	public long queryBalanceFenOrZero(long companyId, String userCustId, String acctId) {
		String user = paymentSettingNonBlankString(userCustId);
		String acct = paymentSettingNonBlankString(acctId);
		if (!StringUtils.hasText(user) || !StringUtils.hasText(acct)) {
			return 0L;
		}
		Map<String, Object> setting;
		try {
			setting = paymentSettingService.loadForCompany(companyId);
		} catch (ResourceException e) {
			return 0L;
		} catch (RuntimeException e) {
			return 0L;
		}
		String merCustId = paymentSettingNonBlankString(setting.get("mer_cust_id"));
		if (!StringUtils.hasText(merCustId)) {
			return 0L;
		}
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("version", 10);
		payload.put("mer_cust_id", merCustId);
		payload.put("user_cust_id", user);
		payload.put("acct_id", acct);
		Map<String, Object> qryResult;
		try {
			qryResult = acouJsonPostClient.qry001(setting, payload);
		} catch (ResourceException e) {
			return 0L;
		} catch (RuntimeException e) {
			return 0L;
		}
		if (qryResult == null) {
			return 0L;
		}
		String code = qryResult.get("resp_code") == null ? "" : String.valueOf(qryResult.get("resp_code")).trim();
		if (!"C00000".equals(code)) {
			return 0L;
		}
		return parseBalanceFen(qryResult.get("balance"));
	}

	/**
	 * Same rules as {@code HfpayCompanyDayTotalStatisticsService.paymentSettingNonBlankString} — values must not become the literal {@code "null"} via {@link String#valueOf(Object)}.
	 */
	private static String paymentSettingNonBlankString(Object v) {
		if (v == null) {
			return "";
		}
		String s = String.valueOf(v).trim();
		if (!StringUtils.hasText(s) || "null".equals(s)) {
			return "";
		}
		return s;
	}

	private static long parseBalanceFen(Object balanceRaw) {
		if (balanceRaw == null) {
			return 0L;
		}
		String s = String.valueOf(balanceRaw).trim();
		if (!StringUtils.hasText(s)) {
			return 0L;
		}
		try {
			BigDecimal yuan = new BigDecimal(s);
			return yuan.multiply(new BigDecimal("100")).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
		} catch (Exception e) {
			return 0L;
		}
	}
}
