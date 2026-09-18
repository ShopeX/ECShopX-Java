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

package cn.shopex.ecshopx.merchant.service.wxapp;

import cn.shopex.ecshopx.merchant.config.MerchantWxappJwtProperties;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class MerchantWxappJwtIssuer {

	private final MerchantWxappJwtProperties jwtProperties;

	public MerchantWxappJwtIssuer(MerchantWxappJwtProperties jwtProperties) {
		this.jwtProperties = jwtProperties;
	}

	public String issueToken(long accountId, long companyId, String mobile) throws JOSEException {
		byte[] secret = jwtProperties.resolveSecretBytes();
		if (secret.length == 0) {
			throw new IllegalStateException("ecshopx.merchant.wxapp.jwt.secret-base64 must be configured");
		}
		String sub = "merchantaccount_" + accountId;
		Map<String, Object> custom = new LinkedHashMap<>();
		custom.put("id", sub);
		custom.put("account_id", accountId);
		custom.put("company_id", companyId);
		custom.put("mobile", mobile);
		custom.put("operator_type", "user");

		Date now = new Date();
		long expMs = now.getTime() + (long) jwtProperties.getTtlMinutes() * 60_000L;
		JWTClaimsSet.Builder b = new JWTClaimsSet.Builder()
				.issuer(jwtProperties.getIssuer())
				.subject(sub)
				.jwtID(UUID.randomUUID().toString())
				.issueTime(now)
				.notBeforeTime(now)
				.expirationTime(new Date(expMs));
		Set<String> reserved = Set.of("iss", "sub", "aud", "exp", "nbf", "iat", "jti");
		for (Map.Entry<String, Object> e : custom.entrySet()) {
			if (e.getKey() != null && e.getValue() != null && !reserved.contains(e.getKey())) {
				b.claim(e.getKey(), e.getValue());
			}
		}
		JWTClaimsSet claims = b.build();
		SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
		jwt.sign(new MACSigner(secret));
		return jwt.serialize();
	}
}
