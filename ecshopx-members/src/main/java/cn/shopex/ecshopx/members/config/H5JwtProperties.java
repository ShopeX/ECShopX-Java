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

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@ConfigurationProperties(prefix = "ecshopx.h5.jwt")
public class H5JwtProperties {

	private String issuer = "ecshopx";

	/** Standard Base64 (RFC 4648) of the signing key; bind from {@code JWT_SECRET} or YAML. */
	private String secretBase64 = "";

	private int ttlMinutes = 60;

	/** 刷新允许窗口（分钟），自 iat 起算；与运营端 JWT_REFRESH_TTL 语义一致。 */
	private int refreshTtlMinutes = 20160;

	private int leewaySeconds = 0;

	/** Seconds after invalidation before the blacklist blocks the token; aligns with {@code JWT_BLACKLIST_GRACE_PERIOD}. */
	private int blacklistGracePeriodSeconds = 0;

	private List<String> requiredClaims = List.of("iss", "iat", "exp", "nbf", "sub", "jti");

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

	public int getRefreshTtlMinutes() {
		return refreshTtlMinutes;
	}

	public void setRefreshTtlMinutes(int refreshTtlMinutes) {
		this.refreshTtlMinutes = refreshTtlMinutes;
	}

	public int getLeewaySeconds() {
		return leewaySeconds;
	}

	public void setLeewaySeconds(int leewaySeconds) {
		this.leewaySeconds = leewaySeconds;
	}

	public int getBlacklistGracePeriodSeconds() {
		return blacklistGracePeriodSeconds;
	}

	public void setBlacklistGracePeriodSeconds(int blacklistGracePeriodSeconds) {
		this.blacklistGracePeriodSeconds = blacklistGracePeriodSeconds;
	}

	public List<String> getRequiredClaims() {
		return requiredClaims;
	}

	public void setRequiredClaims(List<String> requiredClaims) {
		this.requiredClaims = requiredClaims;
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

	@PostConstruct
	void validate() {
		if (requiredClaims == null || !requiredClaims.containsAll(List.of("iss", "iat", "exp", "nbf", "sub", "jti"))) {
			throw new IllegalStateException("ecshopx.h5.jwt.required-claims must include iss,iat,exp,nbf,sub,jti");
		}
		if (blacklistGracePeriodSeconds < 0) {
			throw new IllegalStateException("ecshopx.h5.jwt.blacklist-grace-period-seconds must be >= 0");
		}
	}
}
