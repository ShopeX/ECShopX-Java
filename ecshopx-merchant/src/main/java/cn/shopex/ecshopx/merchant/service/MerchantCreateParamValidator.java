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

package cn.shopex.ecshopx.merchant.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MerchantCreateParamValidator {

	private static final Pattern CN_MOBILE = Pattern.compile("^1[3456789]\\d{9}$");

	public void validateAdminCreate(Map<String, Object> params) {
		requireNonEmptyCollection(params.get("main_category_selection"), "商户类目必填");

		Object rawSettledType = params.get("settled_type");
		if (isMissingScalar(rawSettledType)) {
			throw new ResourceException("请选择正确的入驻类型");
		}
		String settledType = String.valueOf(rawSettledType).trim();
		if (!"enterprise".equals(settledType) && !"soletrader".equals(settledType)) {
			throw new ResourceException("请选择正确的入驻类型");
		}

		requireNonBlankString(params.get("license_registered_address"), "营业执照注册地址必填");
		requireNonBlankString(params.get("license_legal_representative"), "法定代表人必填");
		requireNonBlankString(params.get("merchant_name"), "商户名称必填");
		requireSizeString(params.get("social_credit_code_id"), 18, "统一社会信用代码必须是18位");
		requireNonBlankString(params.get("contact_name"), "联系人姓名必填");
		requireSizeString(params.get("contact_mobile"), 11, "联系人手机号必须是11位");
		if (!isCnMobile(String.valueOf(params.get("contact_mobile")).trim())) {
			throw new ResourceException("手机号格式不正确，请确认后再重试");
		}
		requireSizeString(params.get("legal_cert_id"), 18, "身份证号码必须是18位");

		Object rawAuditGoods = params.get("audit_goods");
		if (isMissingScalar(rawAuditGoods)) {
			throw new ResourceException("审核商品必填");
		}
		String agNorm = MerchantCreateParamNormalizer.normalizeScalarToString(rawAuditGoods);
		if (!"true".equals(agNorm) && !"false".equals(agNorm)) {
			throw new ResourceException("审核商品必填");
		}

		requireNonBlankString(params.get("license_url"), "营业执照必填");
		requireNonBlankString(params.get("legal_certid_front_url"), "手持身份证正面必填");
		requireNonBlankString(params.get("legal_cert_id_back_url"), "手持身份证反面必填");

		requireNonBlankString(params.get("bank_acct_type"), "银行账户类型必填");
		requireNonBlankString(params.get("card_id_mask"), "结算银行账号必填");
		String bankAcct = String.valueOf(params.get("bank_acct_type")).trim();
		if ("1".equals(bankAcct)) {
			requireNonBlankString(params.get("bank_name"), "开户银行必填");
		} else if ("2".equals(bankAcct)) {
			requireNonBlankString(params.get("bank_mobile"), "银行预留手机号必填");
			if (!isCnMobile(String.valueOf(params.get("bank_mobile")).trim())) {
				throw new ResourceException("银行预留手机号格式不正确，请确认后再重试");
			}
		} else {
			throw new ResourceException("银行账户类型必填");
		}

		Object rawSms = params.get("settled_succ_sendsms");
		if (isMissingScalar(rawSms)) {
			throw new ResourceException("短信发送时间必填");
		}
		String smsNorm = MerchantCreateParamNormalizer.normalizeScalarToString(rawSms);
		if (smsNorm == null || (!"1".equals(smsNorm) && !"2".equals(smsNorm))) {
			throw new ResourceException("短信发送时间必填");
		}
	}

	private static boolean isMissingScalar(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof String s) {
			return !StringUtils.hasText(s);
		}
		return false;
	}

	private static void requireNonBlankString(Object v, String message) {
		if (v == null) {
			throw new ResourceException(message);
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			throw new ResourceException(message);
		}
	}

	private static void requireSizeString(Object v, int size, String message) {
		if (v == null) {
			throw new ResourceException(message);
		}
		String s = String.valueOf(v).trim();
		if (s.length() != size) {
			throw new ResourceException(message);
		}
	}

	private static void requireNonEmptyCollection(Object v, String message) {
		if (v == null) {
			throw new ResourceException(message);
		}
		if (v instanceof Collection<?> c && c.isEmpty()) {
			throw new ResourceException(message);
		}
	}

	public static boolean isCnMobile(String mobile) {
		if (!StringUtils.hasText(mobile)) {
			return false;
		}
		return CN_MOBILE.matcher(mobile.trim()).matches();
	}
}
