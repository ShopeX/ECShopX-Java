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

import cn.shopex.ecshopx.common.exception.ResourceException;
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
public class IndependentLicenseCertReadService {

	private final StringRedisTemplate prismRedisTemplate;
	private final ObjectMapper objectMapper;

	public IndependentLicenseCertReadService(
			@Qualifier("prismRedisTemplate") StringRedisTemplate prismRedisTemplate,
			ObjectMapper objectMapper) {
		this.prismRedisTemplate = prismRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public Map<String, String> getCertSetting(String shopexUid, long companyId) {
		if (!StringUtils.hasText(shopexUid)) {
			throw new ResourceException("证书配置无效");
		}
		String key = "prism:" + sha1Hex(shopexUid + "_" + companyId + "_SaasCert");
		String raw = prismRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			throw new ResourceException("证书配置无效");
		}
		try {
			JsonNode n = objectMapper.readTree(raw);
			String nodeId = textOrEmpty(n, "node_id");
			String certId = textOrEmpty(n, "cert_id");
			if (!StringUtils.hasText(nodeId) || !StringUtils.hasText(certId)) {
				throw new ResourceException("证书配置无效");
			}
			Map<String, String> m = new LinkedHashMap<>();
			m.put("node_id", nodeId);
			m.put("cert_id", certId);
			return m;
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("证书配置无效");
		}
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
				hex.append(String.format("%02x", b & 0xFF));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
