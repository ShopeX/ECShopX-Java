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

package cn.shopex.ecshopx.payment.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.util.StringUtils;

final class WxpayNotifyAttachParser {

	private WxpayNotifyAttachParser() {
	}

	static Map<String, String> parseAttach(String attachRaw) {
		if (!StringUtils.hasText(attachRaw)) {
			return new LinkedHashMap<>();
		}
		String decoded;
		try {
			decoded = URLDecoder.decode(attachRaw, StandardCharsets.UTF_8);
		} catch (IllegalArgumentException e) {
			throw new BadRequestException("回传参数无法解码");
		}
		return parseQueryString(decoded);
	}

	private static Map<String, String> parseQueryString(String qs) {
		Map<String, String> m = new LinkedHashMap<>();
		if (!StringUtils.hasText(qs)) {
			return m;
		}
		for (String part : qs.split("&")) {
			if (!StringUtils.hasText(part)) {
				continue;
			}
			int eq = part.indexOf('=');
			try {
				if (eq < 0) {
					String k = URLDecoder.decode(part, StandardCharsets.UTF_8);
					m.put(k, "");
				} else {
					String k = URLDecoder.decode(part.substring(0, eq), StandardCharsets.UTF_8);
					String v = URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8);
					m.put(k, v);
				}
			} catch (IllegalArgumentException e) {
				throw new BadRequestException("回传参数格式错误");
			}
		}
		return m;
	}
}
