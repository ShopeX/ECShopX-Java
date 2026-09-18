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

package cn.shopex.ecshopx.espier.service.config;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 绑定 {@code ecshopx.request-field.*}，作为默认字段与必须开启字段的唯一配置来源。
 */
@Data
@ConfigurationProperties(prefix = "ecshopx.request-field")
public class RequestFieldProperties {

	/**
	 * 对应数云 OEM 开关；为 true 时走 shuyun 分支配置。
	 */
	private boolean oemShuyun;

	private ShuyunSection shuyun = new ShuyunSection();

	private StandardSection standard = new StandardSection();

	@Data
	public static class ShuyunSection {
		/**
		 * 模块类型（字符串键，如 "2"）→ key_name → 字段信息。
		 */
		private Map<String, Map<String, Map<String, Object>>> defaults = new LinkedHashMap<>();

		private Map<String, String> mustStartRequired = new LinkedHashMap<>();
	}

	@Data
	public static class StandardSection {
		/**
		 * 语言（如 zh_cn）→ 模块类型 → key_name → 字段信息。
		 */
		private Map<String, Map<String, Map<String, Map<String, Object>>>> defaults = new LinkedHashMap<>();

		private Map<String, String> mustStartRequired = new LinkedHashMap<>();
	}
}
