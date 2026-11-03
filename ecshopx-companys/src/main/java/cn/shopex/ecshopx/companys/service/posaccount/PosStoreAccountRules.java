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

package cn.shopex.ecshopx.companys.service.posaccount;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

final class PosStoreAccountRules {

	static final Set<String> ROLES = Set.of("store_manager", "supervisor", "clerk");
	static final Set<String> STATUSES = Set.of("enabled", "disabled", "resigned");
	static final Pattern MOBILE = Pattern.compile("^1[3-9]\\d{9}$");
	static final Pattern EMAIL =
			Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", Pattern.CASE_INSENSITIVE);
	/** DigiOS acc / 员工号：仅英文字母与数字（禁止 - _ . 空格、中文等）。 */
	static final Pattern EMPLOYEE_NO_CHARS = Pattern.compile("^[A-Za-z0-9]+$");
	/** DigiOS nickNm / 姓名：仅中文、英文字母与数字。 */
	static final Pattern NAME_CHARS = Pattern.compile("^[\\u4e00-\\u9fa5A-Za-z0-9]+$");

	private PosStoreAccountRules() {}

	static String requireEmployeeNo(String raw) {
		String v = trim(raw);
		if (!StringUtils.hasText(v)) {
			throw new BadRequestException("员工号必填");
		}
		if (v.length() > 30) {
			throw new BadRequestException("员工号不能超过30个字符");
		}
		if (!EMPLOYEE_NO_CHARS.matcher(v).matches()) {
			throw new BadRequestException("员工号仅支持英文字母和数字");
		}
		return v;
	}

	/** 导入等场景姓名必填。 */
	static String requireName(String raw) {
		String v = trim(raw);
		if (!StringUtils.hasText(v)) {
			throw new BadRequestException("姓名必填");
		}
		return assertName(v);
	}

	static String optionalName(String raw) {
		String v = trim(raw);
		if (!StringUtils.hasText(v)) {
			return "";
		}
		return assertName(v);
	}

	private static String assertName(String v) {
		if (v.length() > 30) {
			throw new BadRequestException("姓名不能超过30个字符");
		}
		if (!NAME_CHARS.matcher(v).matches()) {
			throw new BadRequestException("姓名仅支持中文、英文字母和数字");
		}
		return v;
	}

	static String optionalEmail(String raw) {
		String v = trim(raw);
		if (!v.isEmpty() && !EMAIL.matcher(v).matches()) {
			throw new BadRequestException("邮箱格式不正确");
		}
		return v;
	}

	static String optionalMobile(String raw) {
		String v = trim(raw);
		if (!v.isEmpty() && !MOBILE.matcher(v).matches()) {
			throw new BadRequestException("手机号格式不正确");
		}
		return v;
	}

	static String requireRole(String raw) {
		String v = trim(raw);
		if (!ROLES.contains(v)) {
			throw new BadRequestException("请选择新角色");
		}
		return v;
	}

	static String requireStatus(String raw, String fallback) {
		String v = trim(raw);
		if (v.isEmpty()) {
			return fallback;
		}
		if (!STATUSES.contains(v)) {
			throw new BadRequestException("状态无效");
		}
		return v;
	}

	static String roleLabel(String role) {
		return switch (role == null ? "" : role) {
			case "store_manager" -> "店长";
			case "supervisor" -> "主管";
			case "clerk" -> "店员";
			default -> role == null ? "" : role;
		};
	}

	static String statusLabel(String status) {
		return switch (status == null ? "" : status) {
			case "enabled" -> "启用";
			case "disabled" -> "禁用";
			case "resigned" -> "离职";
			default -> status == null ? "" : status;
		};
	}

	static Long requireDistributorId(Object raw) {
		if (raw instanceof Number n && n.longValue() > 0) {
			return n.longValue();
		}
		if (raw != null) {
			String s = raw.toString().trim();
			if (!s.isEmpty()) {
				try {
					long v = Long.parseLong(s);
					if (v > 0) {
						return v;
					}
				} catch (NumberFormatException ignored) {
				}
			}
		}
		throw new BadRequestException("请选择调入门店");
	}

	static String trim(String raw) {
		return raw == null ? "" : raw.trim();
	}
}
