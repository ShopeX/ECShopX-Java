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

package cn.shopex.ecshopx.companys.service.setting;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class H5WebUrlMobileEncryptionService {

	private static final String[] KEYS_WITH_USER_DATA = {"arranged", "classhour", "mycoach"};

	public Map<String, Object> applyMobileUserDataQuery(String mobile, Map<String, Object> urlSetting) {
		if (!StringUtils.hasText(mobile) || urlSetting == null) {
			return urlSetting;
		}
		String suffix =
				"&userData="
						+ URLEncoder.encode(
								Base64.getEncoder()
										.encodeToString(mobile.getBytes(StandardCharsets.UTF_8)),
								StandardCharsets.UTF_8);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>(urlSetting);
		for (String k : KEYS_WITH_USER_DATA) {
			if (!out.containsKey(k)) {
				continue;
			}
			Object v = out.get(k);
			if (v instanceof String s) {
				out.put(k, s + suffix);
			} else if (v == null) {
				out.put(k, suffix);
			}
		}
		return out;
	}
}
