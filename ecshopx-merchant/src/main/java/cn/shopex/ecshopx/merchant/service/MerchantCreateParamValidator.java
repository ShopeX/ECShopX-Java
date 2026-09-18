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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MerchantCreateParamValidator {

	private static final Pattern CN_MOBILE = Pattern.compile("^1[3456789]\\d{9}$");

	private final ObjectMapper objectMapper;

	public MerchantCreateParamValidator(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public void validateAndNormalizeRegions(Map<String, Object> params) {
		validateActionRules(params);
		processRegions(params);
		validateOptionalBankMobile(params);
	}

	private void validateActionRules(Map<String, Object> params) {
		Object rawMerchantTypeId = params.get("merchant_type_id");
		if (isMissingScalar(rawMerchantTypeId)) {
			throw new ResourceException("请选择正确的经营范围");
		}
		long mtid = toPositiveLong(rawMerchantTypeId, "请选择正确的经营范围");
		if (mtid < 1L) {
			throw new ResourceException("请选择正确的经营范围");
		}

		Object rawSettledType = params.get("settled_type");
		if (isMissingScalar(rawSettledType)) {
			throw new ResourceException("请选择正确的入驻类型");
		}
		String settledType = String.valueOf(rawSettledType).trim();
		if (!"enterprise".equals(settledType) && !"soletrader".equals(settledType)) {
			throw new ResourceException("请选择正确的入驻类型");
		}

		requireNonBlankString(params.get("merchant_name"), "商户名称必填");
		requireSizeString(params.get("social_credit_code_id"), 18, "统一社会信用代码必须是18位");

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
		requireSizeString(params.get("legal_mobile"), 11, "手机号码必须是11位");

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
		requireSizeString(params.get("mobile"), 11, "生成账号的手机号必须是11位");

		Object rawSms = params.get("settled_succ_sendsms");
		if (isMissingScalar(rawSms)) {
			throw new ResourceException("短信发送时间必填");
		}
		String smsNorm = MerchantCreateParamNormalizer.normalizeScalarToString(rawSms);
		if (smsNorm == null || (!"1".equals(smsNorm) && !"2".equals(smsNorm))) {
			throw new ResourceException("短信发送时间必填");
		}
	}

	private void validateOptionalBankMobile(Map<String, Object> params) {
		String bankAcctNorm = MerchantCreateParamNormalizer.normalizeScalarToString(params.get("bank_acct_type"));
		if (!"2".equals(bankAcctNorm)) {
			return;
		}
		Object rawBankMobile = params.get("bank_mobile");
		if (rawBankMobile == null) {
			return;
		}
		String bm = String.valueOf(rawBankMobile).trim();
		if (bm.isEmpty()) {
			return;
		}
		if (!CN_MOBILE.matcher(bm).matches()) {
			throw new ResourceException("银行预留手机号格式不正确，请确认后再重试");
		}
	}

	private void processRegions(Map<String, Object> params) {
		List<String> regionNames = normalizeRegionNameList(params.get("regions"));
		for (int i = 0; i < regionNames.size() && i < 3; i++) {
			String v = regionNames.get(i);
			if (i == 0) {
				params.put("province", v);
			} else if (i == 1) {
				params.put("city", v);
			} else {
				params.put("area", v);
			}
		}
		List<Object> regionIds = normalizeRegionIdList(params.get("regions_id"));
		try {
			params.put("regions_id", objectMapper.writeValueAsString(regionIds));
		} catch (Exception e) {
			throw new ResourceException("区域必填");
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

	private List<String> normalizeRegionNameList(Object raw) {
		if (raw instanceof JsonNode node) {
			if (node.isArray()) {
				List<String> out = new ArrayList<>();
				for (JsonNode n : node) {
					out.add(n == null || n.isNull() ? "" : String.valueOf(n.asText()));
				}
				return out;
			}
			if (node.isObject()) {
				return mapObjectToOrderedStringList(node);
			}
			throw new ResourceException("区域必填");
		}
		if (raw instanceof Collection<?> c) {
			List<String> out = new ArrayList<>();
			for (Object o : c) {
				out.add(o == null ? "" : String.valueOf(o));
			}
			return out;
		}
		if (raw instanceof Object[] arr) {
			List<String> out = new ArrayList<>();
			for (Object o : arr) {
				out.add(o == null ? "" : String.valueOf(o));
			}
			return out;
		}
		throw new ResourceException("区域必填");
	}

	private List<String> mapObjectToOrderedStringList(JsonNode objectNode) {
		Map<Integer, String> sorted = new TreeMap<>();
		var it = objectNode.fields();
		while (it.hasNext()) {
			var e = it.next();
			int k;
			try {
				k = Integer.parseInt(e.getKey().trim());
			} catch (NumberFormatException ex) {
				throw new ResourceException("区域必填");
			}
			JsonNode val = e.getValue();
			sorted.put(k, val == null || val.isNull() ? "" : val.asText());
		}
		return new ArrayList<>(sorted.values());
	}

	private List<Object> normalizeRegionIdList(Object raw) {
		if (raw instanceof JsonNode node) {
			if (!node.isArray()) {
				throw new ResourceException("区域必填");
			}
			List<Object> out = new ArrayList<>();
			for (JsonNode n : node) {
				if (n == null || n.isNull()) {
					out.add(null);
				} else if (n.isNumber()) {
					out.add(n.numberValue());
				} else {
					out.add(n.asText());
				}
			}
			return out;
		}
		if (raw instanceof Collection<?> c) {
			return new ArrayList<>(c);
		}
		if (raw instanceof Object[] arr) {
			List<Object> out = new ArrayList<>(arr.length);
			for (Object o : arr) {
				out.add(o);
			}
			return out;
		}
		throw new ResourceException("区域必填");
	}

	public static boolean isCnMobile(String mobile) {
		if (!StringUtils.hasText(mobile)) {
			return false;
		}
		return CN_MOBILE.matcher(mobile.trim()).matches();
	}
}
