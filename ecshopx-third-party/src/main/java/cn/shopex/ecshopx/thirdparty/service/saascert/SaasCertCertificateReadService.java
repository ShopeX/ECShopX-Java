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

package cn.shopex.ecshopx.thirdparty.service.saascert;

import cn.shopex.ecshopx.common.port.companys.CompanyPassportUidByCompanyIdPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SaasCertCertificateReadService {

	private final StringRedisTemplate prismRedisTemplate;
	private final ObjectMapper objectMapper;
	private final CompanyPassportUidByCompanyIdPort companyPassportUidByCompanyIdPort;

	public SaasCertCertificateReadService(
			@Qualifier("prismRedisTemplate") StringRedisTemplate prismRedisTemplate,
			ObjectMapper objectMapper,
			CompanyPassportUidByCompanyIdPort companyPassportUidByCompanyIdPort) {
		this.prismRedisTemplate = prismRedisTemplate;
		this.objectMapper = objectMapper;
		this.companyPassportUidByCompanyIdPort = companyPassportUidByCompanyIdPort;
	}

	/**
	 * @return payload for {@code {"data": ...}} — never includes {@code token}; includes {@code shopex_uid}.
	 */
	public Map<String, Object> buildCertificatePayload(long companyId) {
		String shopexUid =
				companyPassportUidByCompanyIdPort.findPassportUid(companyId).orElse("").trim();
		String primaryKey = certRedisKey(shopexUid, companyId);
		String raw = prismRedisTemplate.opsForValue().get(primaryKey);

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
				// keep empty defaults (parity with tolerant PHP decode paths)
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

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("cert_id", certId);
		data.put("node_id", nodeId);
		if (StringUtils.hasText(nodeId)) {
			data.put("company_id", companyId);
		}
		data.put("shopex_uid", shopexUid);
		return data;
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
