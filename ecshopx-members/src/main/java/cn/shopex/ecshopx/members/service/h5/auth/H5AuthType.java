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

package cn.shopex.ecshopx.members.service.h5.auth;

import org.springframework.util.StringUtils;

public enum H5AuthType {
	LOCAL("local"),
	WXAPP("wxapp"),
	OAUTH("oauth"),
	WX_OFFIACCOUNT("wx_offiaccount"),
	PC_WXQRCODE("pc_wxqrcode"),
	ALIAPP("aliapp"),
	SOCIAL_OAUTH("social_oauth"),
	UNKNOWN("unknown");

	private final String code;

	H5AuthType(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}

	public static H5AuthType fromCredentials(java.util.Map<String, Object> credentials) {
		Object raw = credentials.get("auth_type");
		String s = raw == null ? "" : String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return LOCAL;
		}
		for (H5AuthType t : values()) {
			if (t != UNKNOWN && t.code.equals(s)) {
				return t;
			}
		}
		return UNKNOWN;
	}
}
