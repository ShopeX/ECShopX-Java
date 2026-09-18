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

package cn.shopex.ecshopx.merchant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.Base64;

@ConfigurationProperties(prefix = "ecshopx.merchant.wxapp.jwt")
public class MerchantWxappJwtProperties {

	private String issuer = "ecshopx";

	private String secretBase64 = "";

	private int ttlMinutes = 60;

	private int leewaySeconds = 0;

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public String getSecretBase64() {
		return secretBase64;
	}

	public void setSecretBase64(String secretBase64) {
		this.secretBase64 = secretBase64;
	}

	public int getTtlMinutes() {
		return ttlMinutes;
	}

	public void setTtlMinutes(int ttlMinutes) {
		this.ttlMinutes = ttlMinutes;
	}

	public int getLeewaySeconds() {
		return leewaySeconds;
	}

	public void setLeewaySeconds(int leewaySeconds) {
		this.leewaySeconds = leewaySeconds;
	}

	/**
	 * Strict Base64 decode; if the key is shorter than 256 bits, zero-pad to 32 bytes for Nimbus MACSigner.
	 */
	public byte[] resolveSecretBytes() {
		String s = secretBase64 != null ? secretBase64.trim() : "";
		if (s.isEmpty()) {
			return new byte[0];
		}
		byte[] raw = Base64.getDecoder().decode(s);
		if (raw.length < 32) {
			return Arrays.copyOf(raw, 32);
		}
		return raw;
	}
}
