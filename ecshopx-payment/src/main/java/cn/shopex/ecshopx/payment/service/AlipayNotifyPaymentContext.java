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

package cn.shopex.ecshopx.payment.service;

import java.util.Collections;
import java.util.Map;

/**
 * 支付宝异步通知 Loader 产出的上下文（非 Spring Bean，不可变）。
 */
public final class AlipayNotifyPaymentContext {

	private final Map<String, String> encodedParams;
	private final Map<String, String> returnData;
	private final long companyId;
	private final String outTradeNo;
	private final long distributorIdForSetting;

	public AlipayNotifyPaymentContext(
			Map<String, String> encodedParams,
			Map<String, String> returnData,
			long companyId,
			String outTradeNo,
			long distributorIdForSetting) {
		this.encodedParams = Collections.unmodifiableMap(new java.util.LinkedHashMap<>(encodedParams));
		this.returnData = Collections.unmodifiableMap(new java.util.LinkedHashMap<>(returnData));
		this.companyId = companyId;
		this.outTradeNo = outTradeNo;
		this.distributorIdForSetting = distributorIdForSetting;
	}

	public Map<String, String> getEncodedParams() {
		return encodedParams;
	}

	public Map<String, String> getReturnData() {
		return returnData;
	}

	public long getCompanyId() {
		return companyId;
	}

	public String getOutTradeNo() {
		return outTradeNo;
	}

	public long getDistributorIdForSetting() {
		return distributorIdForSetting;
	}
}
