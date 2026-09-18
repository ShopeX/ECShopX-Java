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

package cn.shopex.ecshopx.members.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 信任登录默认模板（对齐 PHP {@code config/trustlogin.php}）。
 *
 * <p>YAML {@code application-members.yml} 仅在被 {@code spring.config.import} 时才会覆盖本类内置模板。
 * 未导入时 {@link #snapshotAsResponseMap()} 仍返回微信 + 海外四渠道，保证 list merge 能补齐缺 type 行。
 */
@Data
@ConfigurationProperties(prefix = "ecshopx.trustlogin.default", ignoreUnknownFields = true)
public class TrustLoginDefaultProperties {

	private List<Map<String, Object>> standard = builtinStandard();

	private List<Map<String, Object>> touch = builtinTouch();

	/**
	 * Returns a new map with keys {@code standard} and {@code touch}, whose values are deep copies
	 * of the configured lists and row maps. Empty / unbound lists fall back to PHP builtin template.
	 */
	public Map<String, Object> snapshotAsResponseMap() {
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("standard", copyRowList(firstNonEmpty(standard, builtinStandard())));
		out.put("touch", copyRowList(firstNonEmpty(touch, builtinTouch())));
		return out;
	}

	/** PHP {@code config/trustlogin.php} {@code standard} 段。 */
	static List<Map<String, Object>> builtinStandard() {
		List<Map<String, Object>> rows = new ArrayList<>();
		rows.add(row("weixin", "微信"));
		return rows;
	}

	/** PHP {@code config/trustlogin.php} {@code touch} 段：weixin + apple/google/facebook/line。 */
	static List<Map<String, Object>> builtinTouch() {
		List<Map<String, Object>> rows = new ArrayList<>();
		rows.add(row("weixin", "微信"));
		rows.add(row("apple", "Apple"));
		rows.add(row("google", "Google"));
		rows.add(row("facebook", "Facebook"));
		rows.add(row("line", "Line"));
		return rows;
	}

	private static Map<String, Object> row(String type, String name) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("type", type);
		row.put("app_id", "");
		row.put("secret", "");
		row.put("name", name);
		row.put("status", "false");
		row.put("extra_config", "");
		return row;
	}

	private static List<Map<String, Object>> firstNonEmpty(
			List<Map<String, Object>> configured, List<Map<String, Object>> builtin) {
		return configured == null || configured.isEmpty() ? builtin : configured;
	}

	private static List<Map<String, Object>> copyRowList(List<Map<String, Object>> source) {
		if (source == null || source.isEmpty()) {
			return new ArrayList<>();
		}
		List<Map<String, Object>> rows = new ArrayList<>(source.size());
		for (Map<String, Object> row : source) {
			if (row == null) {
				rows.add(new LinkedHashMap<>());
			} else {
				rows.add(new LinkedHashMap<>(row));
			}
		}
		return rows;
	}
}
