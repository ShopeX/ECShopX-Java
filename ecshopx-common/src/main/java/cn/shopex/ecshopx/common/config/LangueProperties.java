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

package cn.shopex.ecshopx.common.config;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "langue")
public class LangueProperties {

	private List<String> list = new ArrayList<>(List.of("zh-CN", "en-CN", "ar-SA", "zh-TW"));

	private String defaultLang = "zh-CN";

	public List<String> getList() {
		return list;
	}

	public void setList(List<String> list) {
		this.list = list != null ? list : new ArrayList<>();
	}

	public String getDefaultLang() {
		return defaultLang;
	}

	public void setDefaultLang(String defaultLang) {
		this.defaultLang = defaultLang;
	}

	/**
	 * 将请求中的语言标签解析为配置支持的形式：空/非法时回落 {@link #getDefaultLang()}；
	 * 在 {@link #list} 中（忽略大小写）时返回 list 中的规范字符串。
	 */
	public String resolveToSupportedTag(String raw) {
		String def = effectiveDefaultLang();
		if (!StringUtils.hasText(raw)) {
			return def;
		}
		String t = raw.trim();
		List<String> supported = list;
		if (supported != null) {
			for (String lang : supported) {
				if (lang != null && lang.equalsIgnoreCase(t)) {
					return lang;
				}
			}
		}
		return def;
	}

	public boolean isDefaultLang(String requestLang) {
		String def = effectiveDefaultLang();
		String req = requestLang == null ? "" : requestLang.trim();
		return def.toLowerCase(Locale.ROOT).equals(req.toLowerCase(Locale.ROOT));
	}

	private String effectiveDefaultLang() {
		return defaultLang == null || defaultLang.isBlank() ? "zh-CN" : defaultLang.trim();
	}

	@PostConstruct
	void validateConfiguration() {
		String def = getDefaultLang();
		if (def == null || def.isBlank()) {
			throw new IllegalStateException("langue.default-lang must be non-blank");
		}
		if (list == null || list.isEmpty()) {
			return;
		}
		boolean ok = list.stream().anyMatch(l -> l != null && l.equalsIgnoreCase(def.trim()));
		if (!ok) {
			throw new IllegalStateException("langue.default-lang must be one of langue.list: " + def);
		}
	}
}
