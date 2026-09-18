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

package cn.shopex.ecshopx.adapay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "adapay.callback")
public class AdapayCallbackProperties {

	/**
	 * AdaPay RSA public key material (PEM block or raw Base64 body without headers).
	 */
	private String rsaPublicKey = "";

	/**
	 * Optional HTTP base URL for SettleAccount.delete / SettleAccount.create outbound calls.
	 * When empty, {@link cn.shopex.ecshopx.adapay.service.callback.AdapaySubMerchantSettleAccountGateway} returns a
	 * local no-op success envelope so callbacks can proceed without remote wiring.
	 */
	private String settleOutboundBaseUrl = "";

	public String getRsaPublicKey() {
		return rsaPublicKey;
	}

	public void setRsaPublicKey(String rsaPublicKey) {
		this.rsaPublicKey = rsaPublicKey;
	}

	public String getSettleOutboundBaseUrl() {
		return settleOutboundBaseUrl;
	}

	public void setSettleOutboundBaseUrl(String settleOutboundBaseUrl) {
		this.settleOutboundBaseUrl = settleOutboundBaseUrl;
	}
}
