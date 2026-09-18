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

package cn.shopex.ecshopx.thirdparty.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Shuyun Open Gateway (user info by code, OEM sub-callback, etc.). */
@Data
@ConfigurationProperties(prefix = "ecshopx.thirdparty.shuyun")
public class ShuyunGatewayProperties {

	/** API host (e.g. https://qa-uapi.shuyun.com), no trailing slash. */
	private String baseUrl = "";

	/** Partner app id ({@code u_appId}). */
	private String appKey = "";

	/** Partner app secret used for request signature. */
	private String appSecret = "";
}
