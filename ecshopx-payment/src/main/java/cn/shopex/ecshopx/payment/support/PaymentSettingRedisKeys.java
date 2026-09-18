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

package cn.shopex.ecshopx.payment.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Redis key layout aligned with legacy payment config storage:
 * WeChat: {@code (distributorId?) + "wxPaymentSetting:" + sha1(companyId)}
 * Alipay: {@code (distributorId?) + "alipayPaymentSetting:" + sha1(companyId)}
 */
public final class PaymentSettingRedisKeys {

	private PaymentSettingRedisKeys() {
	}

	public static String wxpayRedisKey(long companyId, long distributorId) {
		String base = "wxPaymentSetting:" + sha1HexOfCompanyId(companyId);
		return distributorId > 0 ? (distributorId + base) : base;
	}

	public static String alipayRedisKey(long companyId, long distributorId) {
		String base = "alipayPaymentSetting:" + sha1HexOfCompanyId(companyId);
		return distributorId > 0 ? (distributorId + base) : base;
	}

	public static String paypalRedisKey(long companyId, long distributorId) {
		String base = "paypalPaymentSetting:" + sha1HexOfCompanyId(companyId);
		return distributorId > 0 ? (distributorId + base) : base;
	}

	/**
	 * 斗门国际收银台配置：平台级，不含 distributor 分桶（对齐 PHP
	 * {@code doumenIntlPaymentSetting:{sha1(company_id)}}）。
	 */
	public static String doumenIntlPaymentSettingKey(long companyId) {
		return "doumenIntlPaymentSetting:" + sha1HexOfCompanyId(companyId);
	}

	/** Gateway token 缓存：{@code doumen_intl:token:{accessCode}}。 */
	public static String doumenIntlTokenKey(String accessCode) {
		return "doumen_intl:token:" + accessCode;
	}

	/** Token 调度水位：unix timestamp 字符串。 */
	public static String doumenIntlTokenRefreshLastScheduledRunKey() {
		return "doumen_intl:token_refresh:last_scheduled_run";
	}

	public static String hfPaymentSettingKey(long companyId) {
		return "hfPaymentSetting:" + sha1HexOfCompanyId(companyId);
	}

	public static String adapaySettingKey(long companyId) {
		return "adaPaySetting:" + sha1HexOfCompanyId(companyId);
	}

	public static String offlinePaySettingKey(long companyId, String lang) {
		String l = lang != null && !lang.isEmpty() ? lang : "zh-CN";
		return "offline_paySetting:" + sha1HexOfCompanyId(companyId) + ":" + l;
	}

	public static String chinaumsPaymentSettingKey(long companyId, String subKey) {
		String base = "chinaumsPaymentSetting:" + sha1HexOfCompanyId(companyId);
		if (subKey == null || subKey.isEmpty()) {
			return base;
		}
		return base + "_" + subKey;
	}

	public static String bspaySettingKey(long companyId) {
		return "bspaySetting:" + sha1HexOfCompanyId(companyId);
	}

	public static String icbcPaymentSettingKey(long companyId) {
		return "icbcPaymentSetting:" + sha1HexOfCompanyId(companyId);
	}

	public static String paymentTypeOpenConfigKey(long companyId) {
		return "paymentTypeOpenConfig:" + sha1HexOfCompanyId(companyId);
	}

	public static String wechatPaymentCompanyByAppIdKey(String appId) {
		return "wechatPayment:companyId:" + appId;
	}

	public static String wechatAppPaymentCompanyByAppIdKey(String appAppId) {
		return "wechatAppPayment:companyId:" + appAppId;
	}

	public static String wechatServicerPaymentCompanyByAppIdKey(String servicerAppId) {
		return "wechatServicerPayment:companyId:" + servicerAppId;
	}

	public static String hfPaymentCompanyByMerCustIdKey(String merCustId) {
		return "hfPayment:companyId:" + merCustId;
	}

	public static String offlinePayNameLangKey(long companyId, String lang) {
		String l = lang != null && !lang.isEmpty() ? lang : "zh-CN";
		return companyId + "_lang_pay_name_offline_pay:" + l;
	}

	private static String sha1HexOfCompanyId(long companyId) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(String.valueOf(companyId).getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-1 not available", e);
		}
	}
}
