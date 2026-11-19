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

package cn.shopex.ecshopx.thirdparty.service.saaserp.inbound;

import cn.shopex.ecshopx.common.port.companys.CompanyPassportUidByCompanyIdPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SaasErpCompanyResolver {

	private final CompanyPassportUidByCompanyIdPort companyPassportUidByCompanyIdPort;
	private final StringRedisTemplate prismRedisTemplate;
	private final ObjectMapper objectMapper;
	private final long systemCompanyId;

	public SaasErpCompanyResolver(
			CompanyPassportUidByCompanyIdPort companyPassportUidByCompanyIdPort,
			@Qualifier("prismRedisTemplate") StringRedisTemplate prismRedisTemplate,
			ObjectMapper objectMapper,
			@Value("${common.system-companys-id:0}") long systemCompanyId) {
		this.companyPassportUidByCompanyIdPort = companyPassportUidByCompanyIdPort;
		this.prismRedisTemplate = prismRedisTemplate;
		this.objectMapper = objectMapper;
		this.systemCompanyId = systemCompanyId;
	}

	public record Resolved(long companyId, String token) {}

	public Resolved resolve(String toNodeId) {
		long companyId = systemCompanyId;
		if (StringUtils.hasText(toNodeId)) {
			String nodeRaw = prismRedisTemplate.opsForValue().get(nodeRedisKey(toNodeId));
			if (StringUtils.hasText(nodeRaw)) {
				try {
					JsonNode nodeJson = objectMapper.readTree(nodeRaw);
					if (nodeJson != null
							&& nodeJson.isObject()
							&& nodeJson.has("company_id")
							&& !nodeJson.get("company_id").isNull()) {
						companyId = nodeJson.get("company_id").asLong();
					}
				} catch (Exception ignored) {
					// treat as missing company_id → systemCompanyId
				}
			}
		}

		String passportUid =
				companyPassportUidByCompanyIdPort
						.findPassportUid(companyId)
						.orElse("")
						.trim();
		String certId = "";
		String nodeId = "";
		String token = "";
		String companyRaw =
				prismRedisTemplate.opsForValue().get(certRedisKey(passportUid, companyId));
		if (StringUtils.hasText(companyRaw)) {
			try {
				JsonNode certJson = objectMapper.readTree(companyRaw);
				if (certJson != null && certJson.isObject()) {
					certId = textOrEmpty(certJson, "cert_id");
					nodeId = textOrEmpty(certJson, "node_id");
					token = textOrEmpty(certJson, "token");
				}
			} catch (Exception ignored) {
				// certificate treated as empty
			}
		}

		if (StringUtils.hasText(nodeId)) {
			Map<String, Object> forNode = new LinkedHashMap<>();
			forNode.put("cert_id", certId);
			forNode.put("node_id", nodeId);
			forNode.put("token", token);
			forNode.put("company_id", companyId);
			try {
				String json = objectMapper.writeValueAsString(forNode);
				prismRedisTemplate.opsForValue().set(nodeRedisKey(nodeId), json);
			} catch (Exception ignored) {
				// best-effort mirror of PHP setCertSettingByNode
			}
		}

		return new Resolved(companyId, token);
	}

	private static String textOrEmpty(JsonNode n, String field) {
		if (n == null || !n.has(field) || n.get(field).isNull()) {
			return "";
		}
		return n.get(field).asText("");
	}

	private static String certRedisKey(String passportUid, long companyId) {
		return "prism:" + sha1Hex(passportUid + "_" + companyId + "_SaasCert");
	}

	private static String nodeRedisKey(String nodeId) {
		return "prism:" + sha1Hex(nodeId + "_SaasCert");
	}

	private static String sha1Hex(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : d) {
				hex.append(String.format("%02x", b & 0xFF));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
