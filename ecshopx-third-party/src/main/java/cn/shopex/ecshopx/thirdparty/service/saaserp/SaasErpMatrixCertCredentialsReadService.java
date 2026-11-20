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

package cn.shopex.ecshopx.thirdparty.service.saaserp;

import cn.shopex.ecshopx.common.port.companys.CompanyPassportUidByCompanyIdPort;
import cn.shopex.ecshopx.thirdparty.service.saascert.SaasCertErpBindReadService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Loads Matrix outbound credentials: ERP bind node ({@code SaasErpBind:sha1(companyId)}) plus prism
 * SaaS cert JSON ({@code cert_id}/{@code node_id}/{@code token}).
 */
@Service
public class SaasErpMatrixCertCredentialsReadService {

	private final StringRedisTemplate prismRedisTemplate;
	private final ObjectMapper objectMapper;
	private final CompanyPassportUidByCompanyIdPort companyPassportUidByCompanyIdPort;
	private final SaasCertErpBindReadService saasCertErpBindReadService;

	public SaasErpMatrixCertCredentialsReadService(
			@Qualifier("prismRedisTemplate") StringRedisTemplate prismRedisTemplate,
			ObjectMapper objectMapper,
			CompanyPassportUidByCompanyIdPort companyPassportUidByCompanyIdPort,
			SaasCertErpBindReadService saasCertErpBindReadService) {
		this.prismRedisTemplate = prismRedisTemplate;
		this.objectMapper = objectMapper;
		this.companyPassportUidByCompanyIdPort = companyPassportUidByCompanyIdPort;
		this.saasCertErpBindReadService = saasCertErpBindReadService;
	}

	public Optional<SaasErpMatrixCertCredentials> findCredentials(long companyId) {
		if (companyId <= 0) {
			return Optional.empty();
		}
		Optional<String> erpBind = saasCertErpBindReadService.resolveErpBindNodeId(companyId);
		if (erpBind.isEmpty()) {
			return Optional.empty();
		}
		String erpBindNodeId = erpBind.get();
		String shopexUid =
				companyPassportUidByCompanyIdPort.findPassportUid(companyId).orElse("").trim();
		String raw = prismRedisTemplate.opsForValue().get(certRedisKey(shopexUid, companyId));
		String certId = "";
		String nodeId = "";
		String token = "";
		if (StringUtils.hasText(raw)) {
			try {
				JsonNode n = objectMapper.readTree(raw);
				if (n != null && n.isObject()) {
					certId = textOrEmpty(n, "cert_id");
					nodeId = textOrEmpty(n, "node_id");
					token = textOrEmpty(n, "token");
				}
			} catch (Exception ignored) {
				// keep empty defaults
			}
		}
		if (!StringUtils.hasText(certId) || !StringUtils.hasText(nodeId) || !StringUtils.hasText(token)) {
			return Optional.empty();
		}
		return Optional.of(
				new SaasErpMatrixCertCredentials(certId, nodeId, token, erpBindNodeId.trim()));
	}

	static String certRedisKey(String passportUid, long companyId) {
		return "prism:" + sha1Hex(passportUid + "_" + companyId + "_SaasCert");
	}

	private static String textOrEmpty(JsonNode n, String field) {
		if (n == null || !n.has(field) || n.get(field).isNull()) {
			return "";
		}
		return n.get(field).asText("");
	}

	private static String sha1Hex(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : d) {
				hex.append(String.format("%02x", b));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
