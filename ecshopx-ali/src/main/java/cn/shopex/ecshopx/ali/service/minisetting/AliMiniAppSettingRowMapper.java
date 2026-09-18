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

package cn.shopex.ecshopx.ali.service.minisetting;

import cn.shopex.ecshopx.ali.domain.AliMiniAppSetting;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AliMiniAppSettingRowMapper {

	private AliMiniAppSettingRowMapper() {}

	public static Map<String, Object> toSnakeRow(AliMiniAppSetting e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("setting_id", e.getSettingId());
		m.put("company_id", e.getCompanyId());
		m.put("authorizer_appid", e.getAuthorizerAppid());
		m.put("merchant_private_key", e.getMerchantPrivateKey());
		m.put("api_sign_method", e.getApiSignMethod());
		m.put("alipay_cert_path", e.getAlipayCertPath());
		m.put("alipay_root_cert_path", e.getAlipayRootCertPath());
		m.put("merchant_cert_path", e.getMerchantCertPath());
		m.put("alipay_public_key", e.getAlipayPublicKey());
		m.put("notify_url", e.getNotifyUrl());
		m.put("encrypt_key", e.getEncryptKey());
		return m;
	}
}
