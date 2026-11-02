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

package cn.shopex.ecshopx.companys.service.posauth;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** ECX-10173 F-060 / SSOT §4.2 固定三角色权限矩阵。 */
public final class PosPermissionMatrix {

	private static final String[] CODES = {
		"cashier",
		"refund",
		"day_close",
		"shift_handover",
		"member_query",
		"stock_check",
		"report",
		"store_setting"
	};

	private static final Set<String> CLERK =
			Set.of("cashier", "shift_handover", "member_query");

	private PosPermissionMatrix() {}

	public static Map<String, Boolean> permissionsForRole(String role) {
		Map<String, Boolean> out = new LinkedHashMap<>();
		for (String code : CODES) {
			out.put(code, allowed(role, code));
		}
		return out;
	}

	public static String roleName(String role) {
		if ("store_manager".equals(role)) {
			return "店长";
		}
		if ("supervisor".equals(role)) {
			return "主管";
		}
		if ("clerk".equals(role)) {
			return "店员";
		}
		return role == null ? "" : role;
	}

	public static Map<String, String> docPolicyForRole(String role) {
		Map<String, String> p = new LinkedHashMap<>();
		p.put("view", "store_all");
		if ("clerk".equals(role)) {
			p.put("operate", "own_only");
		} else {
			p.put("operate", "store_all");
		}
		return p;
	}

	private static boolean allowed(String role, String code) {
		if ("store_manager".equals(role)) {
			return true;
		}
		if ("supervisor".equals(role)) {
			return !"store_setting".equals(code);
		}
		if ("clerk".equals(role)) {
			return CLERK.contains(code);
		}
		return false;
	}
}
