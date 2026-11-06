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

package cn.shopex.ecshopx.merchant.service.wxapp;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.merchant.service.MerchantCreateParamValidator;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collection;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MerchantWxappSettlementApplySaveParamValidator {

	public void validate(String step, Map<String, Object> params) {
		switch (step) {
			case "1" -> validateStep1(params);
			case "2" -> validateStep2(params);
			case "3" -> validateStep3(params);
			default -> throw new ResourceException("入驻信息填写步骤错误");
		}
	}

	private void validateStep1(Map<String, Object> params) {
		requireNonEmptyMainCategorySelection(params.get("main_category_selection"));
		Object st = params.get("settled_type");
		if (!StringUtils.hasText(scalarToString(st))) {
			throw new ResourceException("入驻类型必填");
		}
		String settled = scalarToString(st).trim();
		if (!"enterprise".equals(settled) && !"soletrader".equals(settled)) {
			throw new ResourceException("入驻类型必填");
		}
	}

	private void validateStep2(Map<String, Object> params) {
		requireNonBlank(params.get("license_registered_address"), "营业执照注册地址必填");
		requireNonBlank(params.get("license_legal_representative"), "法定代表人必填");
		requireNonBlank(params.get("merchant_name"), "商户名称必填");
		requireSize18(params.get("social_credit_code_id"), "统一社会信用代码必须是18位");
		requireNonBlank(params.get("contact_name"), "联系人姓名必填");
		requireNonBlank(params.get("contact_mobile"), "联系人手机号必填");
		String mobile = scalarToString(params.get("contact_mobile")).trim();
		if (!MerchantCreateParamValidator.isCnMobile(mobile)) {
			throw new ResourceException("请填写正确的手机号码");
		}
		requireSize18(params.get("legal_cert_id"), "身份证号码必须是18位");
		requireNonBlank(params.get("license_url"), "营业执照必填");
		requireNonBlank(params.get("legal_certid_front_url"), "手持身份证正面必填");
		requireNonBlank(params.get("legal_cert_id_back_url"), "手持身份证反面必填");
		requireNonBlank(params.get("bank_acct_type"), "银行账户类型必填");
		requireNonBlank(params.get("card_id_mask"), "结算银行账号必填");
		String bankAcct = scalarToString(params.get("bank_acct_type")).trim();
		if ("1".equals(bankAcct)) {
			requireNonBlank(params.get("bank_name"), "开户银行必填");
		} else if ("2".equals(bankAcct)) {
			requireNonBlank(params.get("bank_mobile"), "银行预留手机号必填");
			String bm = scalarToString(params.get("bank_mobile")).trim();
			if (!MerchantCreateParamValidator.isCnMobile(bm)) {
				throw new ResourceException("银行预留手机号格式不正确，请确认后再重试");
			}
		} else {
			throw new ResourceException("银行账户类型必填");
		}
	}

	private void validateStep3(Map<String, Object> params) {
		Object submit = params.get("submit");
		boolean ok = Boolean.TRUE.equals(submit)
				|| (submit instanceof String s && "true".equalsIgnoreCase(s.trim()));
		if (!ok) {
			throw new ResourceException("请确认提交审核");
		}
	}

	private static void requireNonEmptyMainCategorySelection(Object v) {
		if (v == null) {
			throw new ResourceException("商户类目必填");
		}
		if (v instanceof Collection<?> c && c.isEmpty()) {
			throw new ResourceException("商户类目必填");
		}
		if (v instanceof JsonNode n) {
			if (n.isNull() || (n.isArray() && n.isEmpty())) {
				throw new ResourceException("商户类目必填");
			}
		}
	}

	private static void requireNonBlank(Object v, String message) {
		if (v == null) {
			throw new ResourceException(message);
		}
		if (v instanceof String s) {
			if (!StringUtils.hasText(s.trim())) {
				throw new ResourceException(message);
			}
			return;
		}
		if (v instanceof JsonNode n) {
			if (n.isNull() || n.isMissingNode()) {
				throw new ResourceException(message);
			}
			if (n.isTextual() && !StringUtils.hasText(n.asText().trim())) {
				throw new ResourceException(message);
			}
			return;
		}
		String t = String.valueOf(v).trim();
		if (!StringUtils.hasText(t)) {
			throw new ResourceException(message);
		}
	}

	private static void requireSize18(Object v, String message) {
		if (v == null) {
			throw new ResourceException(message);
		}
		String s;
		if (v instanceof JsonNode n && n.isTextual()) {
			s = n.asText().trim();
		} else {
			s = String.valueOf(v).trim();
		}
		if (s.length() != 18) {
			throw new ResourceException(message);
		}
	}

	private static String scalarToString(Object v) {
		if (v == null) {
			return "";
		}
		if (v instanceof JsonNode n && n.isTextual()) {
			return n.asText();
		}
		return String.valueOf(v);
	}
}
