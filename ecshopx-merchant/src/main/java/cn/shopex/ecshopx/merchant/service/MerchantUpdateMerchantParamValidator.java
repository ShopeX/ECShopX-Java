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
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collection;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MerchantUpdateMerchantParamValidator {

	public void validateAndNormalizeForUpdate(Map<String, Object> params) {
		validateRequiredRules(params);
		normalizeAuditGoods(params);
	}

	private void normalizeAuditGoods(Map<String, Object> params) {
		String n = MerchantCreateParamNormalizer.normalizeScalarToString(params.get("audit_goods"));
		params.put("audit_goods", !"false".equals(n));
	}

	private void validateRequiredRules(Map<String, Object> params) {
		Object rawMerchantTypeId = params.get("merchant_type_id");
		if (isMissingScalar(rawMerchantTypeId)) {
			throw new ResourceException("请选择正确的经营范围");
		}
		long mtid = toPositiveLong(rawMerchantTypeId, "请选择正确的经营范围");
		if (mtid < 1L) {
			throw new ResourceException("请选择正确的经营范围");
		}

		Object rid = params.get("regions_id");
		if (isEmptyRegionsValue(rid)) {
			throw new ResourceException("区域必填");
		}
		Object reg = params.get("regions");
		if (isEmptyRegionsValue(reg)) {
			throw new ResourceException("区域必填");
		}

		requireNonBlankString(params.get("address"), "详细地址必填");
		requireNonBlankString(params.get("legal_name"), "姓名必填");
		requireSizeString(params.get("legal_cert_id"), 18, "身份证号码必须是18位");

		Object rawLegalMobile = params.get("legal_mobile");
		if (rawLegalMobile == null) {
			throw new ResourceException("手机号必填");
		}
		String lm = String.valueOf(rawLegalMobile).trim();
		if (lm.isEmpty()) {
			throw new ResourceException("手机号必填");
		}

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

	private static long toPositiveLong(Object v, String message) {
		try {
			if (v instanceof Number n) {
				return n.longValue();
			}
			return Long.parseLong(String.valueOf(v).trim());
		} catch (Exception e) {
			throw new ResourceException(message);
		}
	}

	private static boolean isEmptyRegionsValue(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof String s) {
			return !StringUtils.hasText(s);
		}
		if (v instanceof Object[] arr) {
			return arr.length == 0;
		}
		if (v instanceof Collection<?> c) {
			return c.isEmpty();
		}
		if (v instanceof JsonNode node) {
			return node.isNull() || (node.isArray() && node.isEmpty()) || (node.isObject() && !node.fieldNames().hasNext());
		}
		return false;
	}
}
