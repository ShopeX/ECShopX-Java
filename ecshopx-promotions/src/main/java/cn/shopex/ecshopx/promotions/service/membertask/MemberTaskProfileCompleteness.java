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

package cn.shopex.ecshopx.promotions.service.membertask;

import java.util.Collection;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * 现网会员必填资料是否齐全：开启且 is_required 的字段均有值（对齐 config_request_fields module_type=1）。
 */
public final class MemberTaskProfileCompleteness {

	private MemberTaskProfileCompleteness() {}

	public static boolean isComplete(
			Map<String, Map<String, Object>> openFieldsByKeyName, Map<String, Object> memberInfo) {
		if (openFieldsByKeyName == null || openFieldsByKeyName.isEmpty()) {
			return false;
		}
		if (memberInfo == null || memberInfo.isEmpty()) {
			return false;
		}
		for (Map.Entry<String, Map<String, Object>> e : openFieldsByKeyName.entrySet()) {
			String key = e.getKey();
			Map<String, Object> field = e.getValue();
			if (field == null || key == null || key.isBlank()) {
				continue;
			}
			if (intVal(field.get("is_required"), 0) == 0) {
				continue;
			}
			Object value = resolveValue(key, field, memberInfo);
			if (isEmpty(value)) {
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	static Object resolveValue(String key, Map<String, Object> field, Map<String, Object> memberInfo) {
		if (intVal(field.get("is_default"), 0) != 0) {
			return memberInfo.get(key);
		}
		Object otherParams = memberInfo.get("other_params");
		if (!(otherParams instanceof Map<?, ?> op)) {
			return null;
		}
		Object customData = op.get("custom_data");
		if (!(customData instanceof Map<?, ?> cd)) {
			return null;
		}
		return ((Map<String, Object>) cd).get(key);
	}

	static boolean isEmpty(Object rawValue) {
		if (rawValue == null) {
			return true;
		}
		if (rawValue instanceof String s) {
			return !StringUtils.hasText(s.trim());
		}
		if (rawValue instanceof Collection<?> c) {
			return c.isEmpty();
		}
		return false;
	}

	private static int intVal(Object raw, int defaultValue) {
		if (raw == null) {
			return defaultValue;
		}
		if (raw instanceof Boolean b) {
			return b ? 1 : 0;
		}
		if (raw instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(raw.toString().trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
}
