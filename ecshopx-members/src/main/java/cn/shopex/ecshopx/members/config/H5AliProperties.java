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

package cn.shopex.ecshopx.members.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "ecshopx.h5.ali")
public class H5AliProperties {

	/** companyId 字符串 → 支付宝小程序 app_id（用于 oauth.token） */
	private Map<String, String> miniAppIdByCompanyId = new LinkedHashMap<>();

	/** companyId → 应用私钥或 PKCS8 文本（由运维配置；未配置则授权换 token 失败） */
	private Map<String, String> appPrivateKeyByCompanyId = new LinkedHashMap<>();

	/**
	 * companyId 字符串 → 支付宝开放平台为小程序配置的 AES 密钥（Base64），用于解密 getPhoneNumber 返回的
	 * {@code response} 密文。
	 */
	private Map<String, String> aesDecryptKeyByCompanyId = new LinkedHashMap<>();

	/**
	 * companyId 字符串 → 支付宝 RSA 公钥（验签用，PEM 或单行文本）。未配置时跳过验签（仅解密），生产环境建议配置。
	 */
	private Map<String, String> alipayRsaPublicKeyByCompanyId = new LinkedHashMap<>();

	public Map<String, String> getMiniAppIdByCompanyId() {
		return miniAppIdByCompanyId;
	}

	public void setMiniAppIdByCompanyId(Map<String, String> miniAppIdByCompanyId) {
		this.miniAppIdByCompanyId = miniAppIdByCompanyId;
	}

	public Map<String, String> getAppPrivateKeyByCompanyId() {
		return appPrivateKeyByCompanyId;
	}

	public void setAppPrivateKeyByCompanyId(Map<String, String> appPrivateKeyByCompanyId) {
		this.appPrivateKeyByCompanyId = appPrivateKeyByCompanyId;
	}

	public Map<String, String> getAesDecryptKeyByCompanyId() {
		return aesDecryptKeyByCompanyId;
	}

	public void setAesDecryptKeyByCompanyId(Map<String, String> aesDecryptKeyByCompanyId) {
		this.aesDecryptKeyByCompanyId = aesDecryptKeyByCompanyId;
	}

	public Map<String, String> getAlipayRsaPublicKeyByCompanyId() {
		return alipayRsaPublicKeyByCompanyId;
	}

	public void setAlipayRsaPublicKeyByCompanyId(Map<String, String> alipayRsaPublicKeyByCompanyId) {
		this.alipayRsaPublicKeyByCompanyId = alipayRsaPublicKeyByCompanyId;
	}
}
