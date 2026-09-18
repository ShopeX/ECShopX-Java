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

import java.util.Map;

final class WxpayNotifyPaymentContext {

	private final Map<String, String> notifyParams;
	private final Map<String, String> returnData;
	private final long companyId;
	private final long distributorIdForSetting;
	private final String apiKey;

	WxpayNotifyPaymentContext(
			Map<String, String> notifyParams,
			Map<String, String> returnData,
			long companyId,
			long distributorIdForSetting,
			String apiKey) {
		this.notifyParams = notifyParams;
		this.returnData = returnData;
		this.companyId = companyId;
		this.distributorIdForSetting = distributorIdForSetting;
		this.apiKey = apiKey;
	}

	Map<String, String> getNotifyParams() {
		return notifyParams;
	}

	Map<String, String> getReturnData() {
		return returnData;
	}

	long getCompanyId() {
		return companyId;
	}

	long getDistributorIdForSetting() {
		return distributorIdForSetting;
	}

	String getApiKey() {
		return apiKey;
	}
}
