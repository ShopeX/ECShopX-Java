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

package cn.shopex.ecshopx.payment.service.settings;

import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * 运营端 GET 微信支付配置：补齐 PHP 侧用于证书回显的字段（Redis 仅存 PEM 正文）。
 */
public final class WxpayPaymentSettingAdminReadSupport {

	static final String CERT_FILE_NAME = "apiclient_cert.pem";
	static final String KEY_FILE_NAME = "apiclient_key.pem";
	private static final String LEGACY_CERT_URL_PREFIX = "/data/httpd/storage/app/wechatPayment/";

	private WxpayPaymentSettingAdminReadSupport() {}

	/** 就地补充 {@code cert_name}/{@code cert_url} 等展示字段，不改变 Redis 存储形态。 */
	public static void enrichAdminDisplayFields(Map<String, Object> data) {
		if (data == null || data.isEmpty()) {
			return;
		}
		String merchantId = stringVal(data.get("merchant_id"));
		if (!PaymentConfigJsonSupport.isRequiredCredentialMissing(data.get("cert"))) {
			data.put("cert_name", CERT_FILE_NAME);
			if (StringUtils.hasText(merchantId)) {
				data.put("cert_url", LEGACY_CERT_URL_PREFIX + merchantId + "/" + CERT_FILE_NAME);
			}
		}
		if (!PaymentConfigJsonSupport.isRequiredCredentialMissing(data.get("cert_key"))) {
			data.put("cert_key_name", KEY_FILE_NAME);
			if (StringUtils.hasText(merchantId)) {
				data.put("cert_key_url", LEGACY_CERT_URL_PREFIX + merchantId + "/" + KEY_FILE_NAME);
			}
		}
	}

	private static String stringVal(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}
}
