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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SaasCertBindRelationReadService {

	private static final String MATRIX_CALLBACK_API = "api/third/saascert/matrix/callback";

	private final StringRedisTemplate prismRedisTemplate;
	private final ObjectMapper objectMapper;
	private final CompanyPassportUidByCompanyIdPort companyPassportUidByCompanyIdPort;
	private final String matrixRelationUrl;
	private final String certiBaseUrl;

	public SaasCertBindRelationReadService(
			@Qualifier("prismRedisTemplate") StringRedisTemplate prismRedisTemplate,
			ObjectMapper objectMapper,
			CompanyPassportUidByCompanyIdPort companyPassportUidByCompanyIdPort,
			@Value("${common.matrix-relation-url:https://iframe.shopex.cn/?}") String matrixRelationUrl,
			@Value("${common.certi-base-url:}") String certiBaseUrl) {
		this.prismRedisTemplate = prismRedisTemplate;
		this.objectMapper = objectMapper;
		this.companyPassportUidByCompanyIdPort = companyPassportUidByCompanyIdPort;
		this.matrixRelationUrl = matrixRelationUrl;
		this.certiBaseUrl = certiBaseUrl;
	}

	public String buildApplyBindRelationUrl(long companyId) {
		CertSetting certSetting = loadCertSetting(companyId);
		String baseUrl = certiBaseUrl == null ? "" : certiBaseUrl.replaceAll("/+$", "");

		Map<String, String> params = new LinkedHashMap<>();
		params.put("certi_id", certSetting.certId());
		params.put("node_id", certSetting.nodeId());
		params.put("sess_id", md5Hex(certSetting.nodeId()));

		Map<String, String> signParams = new LinkedHashMap<>(params);
		params.put("certi_ac", makeShopexAc(signParams, certSetting.token()));
		params.put("source", "apply");
		params.put("bind_type", "shopex");
		params.put("api_url", baseUrl + "/api/thirdparty/saaserp");
		params.put("callback", baseUrl + "/" + MATRIX_CALLBACK_API + "/" + companyId);

		return appendQueryParams(matrixRelationUrl, params);
	}

	public String buildAcceptBindRelationUrl(long companyId) {
		CertSetting certSetting = loadCertSetting(companyId);
		String baseUrl = certiBaseUrl == null ? "" : certiBaseUrl.replaceAll("/+$", "");

		Map<String, String> params = new LinkedHashMap<>();
		params.put("certi_id", certSetting.certId());
		params.put("node_id", certSetting.nodeId());
		params.put("sess_id", md5Hex(certSetting.nodeId()));

		Map<String, String> signParams = new LinkedHashMap<>(params);
		params.put("certi_ac", makeShopexAc(signParams, certSetting.token()));
		params.put("source", "accept");
		params.put("api_url", baseUrl + "/api/thirdparty/saaserp");
		params.put("callback", baseUrl + "/" + MATRIX_CALLBACK_API + "/" + companyId);

		return appendQueryParams(matrixRelationUrl, params);
	}

	private CertSetting loadCertSetting(long companyId) {
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
				// keep empty defaults
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
				// best-effort
			}
		}

		return new CertSetting(certId, nodeId, token);
	}

	private static String appendQueryParams(String baseUrl, Map<String, String> params) {
		StringBuilder joined = new StringBuilder();
		for (Map.Entry<String, String> e : params.entrySet()) {
			if (!joined.isEmpty()) {
				joined.append('&');
			}
			joined.append(e.getKey())
					.append('=')
					.append(rawUrlEncode(e.getValue() == null ? "" : e.getValue()));
		}
		return baseUrl + joined;
	}

	private static String makeShopexAc(Map<String, String> tempArr, String token) {
		TreeMap<String, String> sorted = new TreeMap<>(tempArr);
		StringBuilder str = new StringBuilder();
		for (Map.Entry<String, String> e : sorted.entrySet()) {
			if ("certi_ac".equals(e.getKey())) {
				continue;
			}
			str.append(e.getValue() == null ? "" : e.getValue());
		}
		return md5Hex(str + (token == null ? "" : token));
	}

	private static String rawUrlEncode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
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

	private static String md5Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] d = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : d) {
				hex.append(String.format("%02x", b & 0xFF));
			}
			return hex.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
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

	private record CertSetting(String certId, String nodeId, String token) {}
}
