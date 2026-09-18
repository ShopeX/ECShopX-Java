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

package cn.shopex.ecshopx.common.port.weixin;

import java.security.PrivateKey;
import java.util.Objects;

/**
 * 商户 v3 鉴权与签名用物料。
 */
public final class WechatMerchantV3ApiMaterial {

	private final String mchId;
	private final String certSerialNoHex;
	private final PrivateKey apiClientPrivateKey;

	public WechatMerchantV3ApiMaterial(
			String mchId, String certSerialNoHex, PrivateKey apiClientPrivateKey) {
		this.mchId = Objects.requireNonNull(mchId, "mchId");
		this.certSerialNoHex = Objects.requireNonNull(certSerialNoHex, "certSerialNoHex");
		this.apiClientPrivateKey = Objects.requireNonNull(apiClientPrivateKey, "apiClientPrivateKey");
	}

	public String getMchId() {
		return mchId;
	}

	public String getCertSerialNoHex() {
		return certSerialNoHex;
	}

	public PrivateKey getApiClientPrivateKey() {
		return apiClientPrivateKey;
	}
}
