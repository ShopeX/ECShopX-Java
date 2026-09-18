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

package cn.shopex.ecshopx.workwechat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ecshopx.work-wechat")
public class WorkWechatModuleProperties {

	/** 店务 H5 基址，可与环境变量 DIS_WORKWECHAT_H5_BASEURI 对齐。 */
	private String disWorkwechatH5BaseUri = "";

	/**
	 * 店务 H5 网页授权完成后的回调基址；可由环境变量 {@code DIS_WORKWECHAT_H5_AUTHURI} 注入。
	 */
	private String disWorkwechatH5AuthUri = "";
}
