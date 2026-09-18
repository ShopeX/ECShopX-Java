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

package cn.shopex.ecshopx.common.cron.merchant;

import java.util.Objects;

/**
 * 供微信商户打款单轮询任务使用的 orders_merchant_trade 行只读片段。
 */
public final class CronMerchantPaymentTradeRow {

	private final String merchantTradeId;
	private final long companyId;
	private final String relSceneId;
	private final String relSceneName;
	private final String paymentNo;

	public CronMerchantPaymentTradeRow(
			String merchantTradeId,
			long companyId,
			String relSceneId,
			String relSceneName,
			String paymentNo) {
		this.merchantTradeId = Objects.requireNonNull(merchantTradeId, "merchantTradeId");
		this.companyId = companyId;
		this.relSceneId = relSceneId;
		this.relSceneName = relSceneName;
		this.paymentNo = paymentNo;
	}

	public String getMerchantTradeId() {
		return merchantTradeId;
	}

	public long getCompanyId() {
		return companyId;
	}

	public String getRelSceneId() {
		return relSceneId;
	}

	public String getRelSceneName() {
		return relSceneName;
	}

	public String getPaymentNo() {
		return paymentNo;
	}
}
