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
		requirePositiveIntMerchantType(params.get("merchant_type_id"));
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
		requireNonBlank(params.get("merchant_name"), "商户名称必填");
		requireSize18(params.get("social_credit_code_id"), "统一社会信用代码必须是18位");
		if (!isPresentRegionsValue(params.get("regions_id"))) {
			throw new ResourceException("区域必填");
		}
		if (!isPresentRegionsValue(params.get("regions"))) {
			throw new ResourceException("区域必填");
		}
		requireNonBlank(params.get("address"), "详细地址必填");
		requireNonBlank(params.get("legal_name"), "姓名必填");
		requireSize18(params.get("legal_cert_id"), "身份证号码必须是18位");
		requireNonBlank(params.get("legal_mobile"), "手机号码必填是11位");
	}

	private void validateStep3(Map<String, Object> params) {
		requireNonBlank(params.get("license_url"), "营业执照必填");
		requireNonBlank(params.get("legal_certid_front_url"), "手持身份证正面必填");
		requireNonBlank(params.get("legal_cert_id_back_url"), "手持身份证反面必填");
	}

	private static void requirePositiveIntMerchantType(Object v) {
		if (v == null) {
			throw new ResourceException("经营范围必填");
		}
		long id;
		if (v instanceof Number n) {
			id = n.longValue();
		} else {
			String s = String.valueOf(v).trim();
			if (!StringUtils.hasText(s)) {
				throw new ResourceException("经营范围必填");
			}
			try {
				id = Long.parseLong(s);
			} catch (NumberFormatException e) {
				throw new ResourceException("经营范围必填");
			}
		}
		if (id < 1L) {
			throw new ResourceException("经营范围必填");
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

	private static boolean isPresentRegionsValue(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof String s) {
			return StringUtils.hasText(s.trim());
		}
		if (v instanceof Collection<?> c) {
			return !c.isEmpty();
		}
		if (v instanceof Map<?, ?> m) {
			return !m.isEmpty();
		}
		if (v instanceof Object[] a) {
			return a.length > 0;
		}
		if (v instanceof JsonNode n) {
			if (n.isNull() || n.isMissingNode()) {
				return false;
			}
			if (n.isArray()) {
				return n.size() > 0;
			}
			if (n.isTextual()) {
				return StringUtils.hasText(n.asText().trim());
			}
			return true;
		}
		return true;
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
