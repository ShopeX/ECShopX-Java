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

package cn.shopex.ecshopx.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ecshopx.payment.doumen-intl")
public class DoumenIntlProperties {

	/** 网关 base URI，对应 DOUMEN_INTL_BASE_URL */
	private String baseUrl = "";

	/** checkout / 退款 notificationUrl，对应 DOUMEN_INTL_NOTIFY_URL */
	private String notifyUrl = "";

	/** 保留项；业务未消费，对应 DOUMEN_INTL_SANDBOX */
	private boolean sandbox = true;

	/** Schedule 间隔分钟，默认 115 */
	private int tokenRefreshIntervalMinutes = 115;

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl == null ? "" : baseUrl;
	}

	public String getNotifyUrl() {
		return notifyUrl;
	}

	public void setNotifyUrl(String notifyUrl) {
		this.notifyUrl = notifyUrl == null ? "" : notifyUrl;
	}

	public boolean isSandbox() {
		return sandbox;
	}

	public void setSandbox(boolean sandbox) {
		this.sandbox = sandbox;
	}

	public int getTokenRefreshIntervalMinutes() {
		return tokenRefreshIntervalMinutes;
	}

	public void setTokenRefreshIntervalMinutes(int tokenRefreshIntervalMinutes) {
		this.tokenRefreshIntervalMinutes = tokenRefreshIntervalMinutes;
	}
}
