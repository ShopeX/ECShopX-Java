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

package cn.shopex.ecshopx.payment.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public final class PaymentConfigJsonSupport {

	private static final Logger log = LoggerFactory.getLogger(PaymentConfigJsonSupport.class);

	private PaymentConfigJsonSupport() {
	}

	public static Map<String, Object> parseObjectMap(ObjectMapper objectMapper, String raw) {
		if (!StringUtils.hasText(raw)) {
			return new LinkedHashMap<>();
		}
		try {
			Map<String, Object> m = objectMapper.readValue(raw, new TypeReference<>() {
			});
			if (m == null) {
				return new LinkedHashMap<>();
			}
			return new LinkedHashMap<>(m);
		} catch (Exception e) {
			log.warn(
					"payment config json parse failed: {} {}",
					e.getClass().getSimpleName(),
					e.getMessage() != null ? e.getMessage() : "");
			return new LinkedHashMap<>();
		}
	}

	public static boolean normalizeIsOpen(Object value) {
		if (value instanceof Boolean b) {
			return b;
		}
		if (value instanceof String s) {
			String lower = s.trim().toLowerCase();
			return "true".equals(lower) || "1".equals(lower) || "yes".equals(lower) || "on".equals(lower);
		}
		if (value instanceof Number n) {
			return n.intValue() != 0;
		}
		return false;
	}

	/**
	 * 判断必填凭证字段是否缺失：null、空白字符串视为缺失，{@code "0"} 视为有效值。
	 */
	public static boolean isRequiredCredentialMissing(Object value) {
		if (value == null) {
			return true;
		}
		if (value instanceof String s) {
			if ("0".equals(s)) {
				return false;
			}
			return !StringUtils.hasText(s);
		}
		if (value instanceof Number n) {
			return n.doubleValue() == 0.0;
		}
		if (value instanceof Boolean b) {
			return !b;
		}
		return false;
	}
}
