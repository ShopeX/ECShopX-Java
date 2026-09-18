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

package cn.shopex.ecshopx.aliyunsms.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ecshopx.sms")
public class AliyunsmsSmsSceneWhitelistProperties {

	private List<String> b2c = new ArrayList<>();

	private List<String> platform = new ArrayList<>();

	private List<String> standard = new ArrayList<>();

	private List<String> inPurchase = new ArrayList<>();

	public List<String> getB2c() {
		return b2c;
	}

	public void setB2c(List<String> b2c) {
		this.b2c = b2c != null ? new ArrayList<>(b2c) : new ArrayList<>();
	}

	public List<String> getPlatform() {
		return platform;
	}

	public void setPlatform(List<String> platform) {
		this.platform = platform != null ? new ArrayList<>(platform) : new ArrayList<>();
	}

	public List<String> getStandard() {
		return standard;
	}

	public void setStandard(List<String> standard) {
		this.standard = standard != null ? new ArrayList<>(standard) : new ArrayList<>();
	}

	public List<String> getInPurchase() {
		return inPurchase;
	}

	public void setInPurchase(List<String> inPurchase) {
		this.inPurchase = inPurchase != null ? new ArrayList<>(inPurchase) : new ArrayList<>();
	}

	public List<String> resolveSceneNames(String productModel) {
		if (productModel == null || productModel.isBlank()) {
			return List.of();
		}
		String key = productModel.trim();
		return switch (key) {
			case "b2c" -> Collections.unmodifiableList(new ArrayList<>(b2c));
			case "platform" -> Collections.unmodifiableList(new ArrayList<>(platform));
			case "standard" -> Collections.unmodifiableList(new ArrayList<>(standard));
			case "in_purchase" -> Collections.unmodifiableList(new ArrayList<>(inPurchase));
			default -> List.of();
		};
	}
}
