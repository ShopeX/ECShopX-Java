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

package cn.shopex.ecshopx.digios.config;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 对齐 PHP {@code config/digios.php}。 */
@Data
@ConfigurationProperties(prefix = "ecshopx.digios")
public class DigiOsProperties {

	private boolean enabled = false;

	private String baseUrl = "https://hobbits.ishopex.cn";

	private String clientId = "";

	private String appSecret = "";

	private int timeoutSeconds = 10;

	private int connectTimeoutSeconds = 5;

	private String tokenPath = "/gateway/token";

	private int tokenExpireSkewSeconds = 120;

	private String coCode = "Shopex001";

	private int bizModel = 0;

	private String terCd = "ECX";

	private String defaultOpHours = "10:00,22:00";

	/** classpath 或文件路径，默认 classpath:digios_regions.json */
	private String regionsJson = "classpath:digios_regions.json";

	/** 入站 stock_push 单次行数上限，对齐 PHP digios.stock_push_max_rows */
	private int stockPushMaxRows = 200;

	private String storeAdjustmentDocOutbound = "code_000008";

	private String storeAdjustmentDocInbound = "code_000009";

	private Map<String, String> methods = defaultMethods();

	private static Map<String, String> defaultMethods() {
		Map<String, String> m = new LinkedHashMap<>();
		m.put("warehouse.upsert", "/gateway/openapi/v1/warehouse");
		m.put("logic.upsert", "/gateway/openapi/v1/logic");
		m.put("store.create", "/gateway/openapi/v1/store");
		m.put("store.update", "/gateway/openapi/v1/store");
		m.put("account.upsert", "/gateway/openapi/v1/account");
		m.put("store.adjustment", "/gateway/openapi/v1/inventory/store/adjustment");
		return m;
	}
}
