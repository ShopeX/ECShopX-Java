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

package cn.shopex.ecshopx.ali.service.h5;

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.alipay.easysdk.kernel.Config;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AlipayMiniEasySdkFactory {

	private static final String SIGN_KEY = "key";
	private static final String SIGN_CERT = "cert";

	public Config buildConfig(Map<String, Object> settingRow) {
		String authorizerAppid = str(settingRow, "authorizer_appid");
		String merchantPrivateKey = str(settingRow, "merchant_private_key");
		if (!StringUtils.hasText(authorizerAppid) && !StringUtils.hasText(merchantPrivateKey)) {
			throw new ResourceException("当前账号未配置支付宝小程序");
		}

		String apiSignMethod = str(settingRow, "api_sign_method");
		if (!SIGN_KEY.equals(apiSignMethod) && !SIGN_CERT.equals(apiSignMethod)) {
			throw new ResourceException("ApiSignMethod 类型错误");
		}

		Config options = new Config();
		options.protocol = "https";
		options.gatewayHost = "openapi.alipay.com";
		options.signType = "RSA2";
		options.appId = authorizerAppid;
		options.merchantPrivateKey = merchantPrivateKey;
		options.notifyUrl = str(settingRow, "notify_url");
		options.encryptKey = str(settingRow, "encrypt_key");

		if (SIGN_CERT.equals(apiSignMethod)) {
			options.alipayCertPath = str(settingRow, "alipay_cert_path");
			options.alipayRootCertPath = str(settingRow, "alipay_root_cert_path");
			options.merchantCertPath = str(settingRow, "merchant_cert_path");
		} else {
			options.alipayPublicKey = str(settingRow, "alipay_public_key");
		}

		return options;
	}

	private static String str(Map<String, Object> row, String key) {
		Object o = row.get(key);
		if (o == null) {
			return "";
		}
		return String.valueOf(o).trim();
	}
}
