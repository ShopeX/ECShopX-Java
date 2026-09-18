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

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ecshopx.h5.local")
public class H5LocalProperties {

	private boolean systemIsSaas = false;

	private String systemCompanysId = "";

	private String systemMainCompanysId = "";

	private boolean oemShuyun = false;

	private boolean transferMode = false;

	/**
	 * Optional base URL for 数云静默入会查询（与部署环境对齐；未配置时静默登录桥接返回空）。
	 */
	private String shuyunSilentSearchBaseUrl = "";

	public String getShuyunSilentSearchBaseUrl() {
		return shuyunSilentSearchBaseUrl;
	}

	public void setShuyunSilentSearchBaseUrl(String shuyunSilentSearchBaseUrl) {
		this.shuyunSilentSearchBaseUrl = shuyunSilentSearchBaseUrl;
	}

	public boolean isSystemIsSaas() {
		return systemIsSaas;
	}

	public void setSystemIsSaas(boolean systemIsSaas) {
		this.systemIsSaas = systemIsSaas;
	}

	public String getSystemCompanysId() {
		return systemCompanysId;
	}

	public void setSystemCompanysId(String systemCompanysId) {
		this.systemCompanysId = systemCompanysId;
	}

	public String getSystemMainCompanysId() {
		return systemMainCompanysId;
	}

	public void setSystemMainCompanysId(String systemMainCompanysId) {
		this.systemMainCompanysId = systemMainCompanysId;
	}

	public boolean isOemShuyun() {
		return oemShuyun;
	}

	public void setOemShuyun(boolean oemShuyun) {
		this.oemShuyun = oemShuyun;
	}

	public boolean isTransferMode() {
		return transferMode;
	}

	public void setTransferMode(boolean transferMode) {
		this.transferMode = transferMode;
	}
}
