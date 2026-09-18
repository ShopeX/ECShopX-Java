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

package cn.shopex.ecshopx.popularize.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ecshopx.popularize.common-defaults")
public class PopularizeCommonImageDefaultsProperties {

	private String distributionDefaultBanner = "";

	private String distributionDefaultWeapp = "";

	private String distributionDefaultPoster = "";

	private String qrcodeBgImg = "";

	private String promoterQrcodeBgImg = "";

	public String getDistributionDefaultBanner() {
		return distributionDefaultBanner;
	}

	public void setDistributionDefaultBanner(String distributionDefaultBanner) {
		this.distributionDefaultBanner = distributionDefaultBanner;
	}

	public String getDistributionDefaultWeapp() {
		return distributionDefaultWeapp;
	}

	public void setDistributionDefaultWeapp(String distributionDefaultWeapp) {
		this.distributionDefaultWeapp = distributionDefaultWeapp;
	}

	public String getDistributionDefaultPoster() {
		return distributionDefaultPoster;
	}

	public void setDistributionDefaultPoster(String distributionDefaultPoster) {
		this.distributionDefaultPoster = distributionDefaultPoster;
	}

	public String getQrcodeBgImg() {
		return qrcodeBgImg;
	}

	public void setQrcodeBgImg(String qrcodeBgImg) {
		this.qrcodeBgImg = qrcodeBgImg;
	}

	public String getPromoterQrcodeBgImg() {
		return promoterQrcodeBgImg;
	}

	public void setPromoterQrcodeBgImg(String promoterQrcodeBgImg) {
		this.promoterQrcodeBgImg = promoterQrcodeBgImg;
	}
}
