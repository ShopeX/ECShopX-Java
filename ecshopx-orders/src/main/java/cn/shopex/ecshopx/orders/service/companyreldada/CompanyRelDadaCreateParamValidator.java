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

package cn.shopex.ecshopx.orders.service.companyreldada;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CompanyRelDadaCreateParamValidator {

	private static final Pattern CN_MOBILE = Pattern.compile("^1[3-9]\\d{9}$");

	private static final Pattern EMAIL_SIMPLE =
			Pattern.compile("^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");

	public NormalizedCompanyRelDadaCreateParams validateAndNormalize(Map<String, Object> merged) {
		Object statusRaw = firstMerged(merged.get("status"));
		if (statusRaw == null || isBlankScalar(statusRaw)) {
			throw new BadRequestException("开通状态必填");
		}
		Boolean status = parseBooleanEquivalence(statusRaw);
		if (status == null) {
			throw new BadRequestException("开通状态参数类型错误");
		}

		String mobile = requireTrimmedString(merged, "mobile", "商户手机号必填");
		if (!CN_MOBILE.matcher(mobile).matches()) {
			throw new BadRequestException("请输入正确的手机号");
		}

		String cityName = requireTrimmedString(merged, "city_name", "商户城市名称不能为空");
		if (cityName.length() > 255) {
			throw new BadRequestException("商户城市名称不能超过255个字符");
		}

		String enterpriseName = requireTrimmedString(merged, "enterprise_name", "企业全称不能为空");
		if (enterpriseName.length() > 50) {
			throw new BadRequestException("企业全称不能超过50个字符");
		}

		String enterpriseAddress = requireTrimmedString(merged, "enterprise_address", "企业地址不能为空");
		if (enterpriseAddress.length() > 100) {
			throw new BadRequestException("企业地址不能超过100个字符");
		}

		String contactName = requireTrimmedString(merged, "contact_name", "联系人姓名不能为空");
		if (contactName.length() > 50) {
			throw new BadRequestException("联系人姓名不能超过50个字符");
		}

		String contactPhone = requireTrimmedString(merged, "contact_phone", "联系人电话不能为空");
		if (!CN_MOBILE.matcher(contactPhone).matches()) {
			throw new BadRequestException("请输入正确的联系人电话");
		}

		String emailRaw = strFromMerged(merged, "email");
		if (!StringUtils.hasText(emailRaw)) {
			throw new BadRequestException("邮箱地址必填");
		}
		String email = emailRaw.trim();
		if (!EMAIL_SIMPLE.matcher(email).matches()) {
			throw new BadRequestException("邮箱地址格式错误");
		}

		Object freightRaw = firstMerged(merged.get("freight_type"));
		if (freightRaw == null || isBlankScalar(freightRaw)) {
			throw new BadRequestException("运费承担方不能为空");
		}
		Boolean freightType = parseBooleanEquivalence(freightRaw);
		if (freightType == null) {
			throw new BadRequestException("运费承担方类型错误");
		}

		Object isOpenRaw = firstMerged(merged.get("is_open"));
		if (isOpenRaw == null || isBlankScalar(isOpenRaw)) {
			throw new BadRequestException("是否开启同城配不能为空");
		}
		Boolean isOpen = parseBooleanEquivalence(isOpenRaw);
		if (isOpen == null) {
			throw new BadRequestException("是否开启参数类型错误");
		}

		String sourceIdOrNull = null;
		if (Boolean.TRUE.equals(status)) {
			String sid = strFromMerged(merged, "source_id");
			if (!StringUtils.hasText(sid)) {
				throw new BadRequestException("达达商户ID必填");
			}
			sid = sid.trim();
			if (sid.length() > 50) {
				throw new BadRequestException("达达商户ID最大长度50");
			}
			sourceIdOrNull = sid;
		} else {
			String sid = strFromMerged(merged, "source_id");
			if (StringUtils.hasText(sid)) {
				sourceIdOrNull = sid.trim();
				if (sourceIdOrNull.isEmpty()) {
					sourceIdOrNull = null;
				}
			}
		}

		NormalizedCompanyRelDadaCreateParams out = new NormalizedCompanyRelDadaCreateParams();
		out.setStatus(status);
		out.setMobile(mobile);
		out.setCityName(cityName);
		out.setEnterpriseName(enterpriseName);
		out.setEnterpriseAddress(enterpriseAddress);
		out.setContactName(contactName);
		out.setContactPhone(contactPhone);
		out.setEmail(email);
		out.setFreightType(freightType);
		out.setIsOpen(isOpen);
		out.setSourceIdOrNull(sourceIdOrNull);
		return out;
	}

	private static String requireTrimmedString(Map<String, Object> merged, String key, String missingMsg) {
		String s = strFromMerged(merged, key);
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException(missingMsg);
		}
		return s.trim();
	}

	private static String strFromMerged(Map<String, Object> merged, String key) {
		Object o = firstMerged(merged.get(key));
		if (o == null) {
			return null;
		}
		return String.valueOf(o);
	}

	private static Object firstMerged(Object v) {
		if (v == null) {
			return null;
		}
		if (v instanceof String[] a) {
			return a.length == 0 ? null : a[0];
		}
		if (v instanceof Object[] a) {
			return a.length == 0 ? null : a[0];
		}
		if (v instanceof List<?> list) {
			return list.isEmpty() ? null : list.get(0);
		}
		return v;
	}

	private static boolean isBlankScalar(Object raw) {
		if (raw instanceof String s) {
			return !StringUtils.hasText(s.trim());
		}
		return false;
	}

	private static Boolean parseBooleanEquivalence(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof Number n) {
			long v = n.longValue();
			if (v == 1L) {
				return true;
			}
			if (v == 0L) {
				return false;
			}
			return null;
		}
		String s = String.valueOf(raw).trim().toLowerCase();
		if (s.isEmpty()) {
			return null;
		}
		if ("true".equals(s) || "false".equals(s)) {
			return null;
		}
		if ("1".equals(s)) {
			return true;
		}
		if ("0".equals(s)) {
			return false;
		}
		return null;
	}
}
