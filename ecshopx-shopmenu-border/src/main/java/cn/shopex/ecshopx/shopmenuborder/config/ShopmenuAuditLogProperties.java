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

package cn.shopex.ecshopx.shopmenuborder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "shopmenu")
public class ShopmenuAuditLogProperties {

	/** 导入时 is_show 为 false 的审计日志路径（相对路径相对进程工作目录）。 */
	private String isShowAuditLogPath = "logs/shopmenu_is_show_hidden.log";

	public String getIsShowAuditLogPath() {
		return isShowAuditLogPath;
	}

	public void setIsShowAuditLogPath(String isShowAuditLogPath) {
		this.isShowAuditLogPath = isShowAuditLogPath;
	}
}
