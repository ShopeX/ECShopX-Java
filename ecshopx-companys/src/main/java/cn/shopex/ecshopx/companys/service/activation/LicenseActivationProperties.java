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

package cn.shopex.ecshopx.companys.service.activation;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code license.gateway.*} from configuration (see field Javadoc for keys).
 */
@ConfigurationProperties(prefix = "license.gateway")
public class LicenseActivationProperties {

	private String licenseUrl = "";

	private String independentLicenseUrl = "";

	/**
	 * Property {@code license.gateway.product-name}. Sent as URL-encoded form field {@code product} to the SaaS
	 * license gateway; also sent as form field {@code product} on independent license activation requests.
	 */
	private String productName = "";

	/**
	 * Property {@code license.gateway.independent-product-type}. Sent as form field {@code product_name} only on
	 * independent license activation requests (product type label).
	 */
	private String independentProductType = "";

	private String version = "";

	private String independentShopUrl = "http://localhost";

	public String getLicenseUrl() {
		return licenseUrl;
	}

	public void setLicenseUrl(String licenseUrl) {
		this.licenseUrl = licenseUrl != null ? licenseUrl : "";
	}

	public String getIndependentLicenseUrl() {
		return independentLicenseUrl;
	}

	public void setIndependentLicenseUrl(String independentLicenseUrl) {
		this.independentLicenseUrl = independentLicenseUrl != null ? independentLicenseUrl : "";
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName != null ? productName : "";
	}

	public String getIndependentProductType() {
		return independentProductType;
	}

	public void setIndependentProductType(String independentProductType) {
		this.independentProductType = independentProductType != null ? independentProductType : "";
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version != null ? version : "";
	}

	public String getIndependentShopUrl() {
		return independentShopUrl;
	}

	public void setIndependentShopUrl(String independentShopUrl) {
		this.independentShopUrl = independentShopUrl != null ? independentShopUrl : "http://localhost";
	}
}
