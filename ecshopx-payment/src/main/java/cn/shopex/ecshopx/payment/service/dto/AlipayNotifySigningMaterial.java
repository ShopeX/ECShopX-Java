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

package cn.shopex.ecshopx.payment.service.dto;

/**
 * 支付宝异步通知验签与配置解析结果（非 Spring Bean）。
 */
public final class AlipayNotifySigningMaterial {

	private final String appId;
	private final String alipayPublicKey;
	private final String rsaPrivateKey;

	public AlipayNotifySigningMaterial(String appId, String alipayPublicKey, String rsaPrivateKey) {
		this.appId = appId;
		this.alipayPublicKey = alipayPublicKey;
		this.rsaPrivateKey = rsaPrivateKey;
	}

	public String getAppId() {
		return appId;
	}

	public String getAlipayPublicKey() {
		return alipayPublicKey;
	}

	public String getRsaPrivateKey() {
		return rsaPrivateKey;
	}
}
