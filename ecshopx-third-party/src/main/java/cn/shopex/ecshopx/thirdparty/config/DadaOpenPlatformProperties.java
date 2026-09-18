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

@Data
@ConfigurationProperties(prefix = "ecshopx.thirdparty.dada")
public class DadaOpenPlatformProperties {

	/** When false (default), outbound HTTP is disabled and the NoOp port is active. */
	private boolean httpEnabled = false;

	private boolean online = false;

	private String appKey = "";

	private String appSecret = "";

	private String hostOnline = "https://newopen.imdada.cn";

	private String hostSandbox = "http://newopen.qa.imdada.cn";

	private String sandboxSourceId = "1239307635";
}
