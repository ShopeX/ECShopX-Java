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

package cn.shopex.ecshopx.members.service.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class MemberRowOtherParams {

	private static final ObjectMapper JSON = new ObjectMapper();

	private MemberRowOtherParams() {}

	public static boolean isUploadMember(Map<String, Object> memberRow) {
		if (memberRow == null || memberRow.isEmpty()) {
			return false;
		}
		Object opRaw = memberRow.get("other_params");
		Object decoded = decodeOtherParams(opRaw);
		if (decoded instanceof Map<?, ?> om) {
			Object v = om.get("is_upload_member");
			return Boolean.TRUE.equals(v) || "true".equalsIgnoreCase(String.valueOf(v));
		}
		return false;
	}

	private static Object decodeOtherParams(Object opRaw) {
		if (opRaw == null) {
			return Collections.emptyMap();
		}
		if (opRaw instanceof Map<?, ?> m) {
			return m;
		}
		String s = String.valueOf(opRaw).trim();
		if (!StringUtils.hasText(s) || "[]".equals(s)) {
			return Collections.emptyMap();
		}
		try {
			return JSON.readValue(s, new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			return Collections.emptyMap();
		}
	}
}
