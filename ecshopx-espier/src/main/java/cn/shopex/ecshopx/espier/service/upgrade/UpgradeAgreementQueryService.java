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

package cn.shopex.ecshopx.espier.service.upgrade;

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class UpgradeAgreementQueryService {

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;
	private final String baseUri;
	private final String agreementPath;
	private final String docsPath;
	private final String patchPath;
	private final String platformVersion;
	private final String platformComposerJsonPath;
	private final String secret;

	public UpgradeAgreementQueryService(
			@Qualifier("espierShopexUsercenterRestTemplate") RestTemplate restTemplate,
			ObjectMapper objectMapper,
			@Value("${espier.shopex-usercenter.base-uri}") String baseUri,
			@Value("${espier.shopex-usercenter.agreement-path}") String agreementPath,
			@Value("${espier.shopex-usercenter.docs-path}") String docsPath,
			@Value("${espier.shopex-usercenter.patch-path}") String patchPath,
			@Value("${espier.platform-version}") String platformVersion,
			@Value("${espier.platform-composer-json-path:}") String platformComposerJsonPath,
			@Value("${espier.shopex-usercenter.secret}") String secret) {
		this.restTemplate = restTemplate;
		this.objectMapper = objectMapper;
		this.baseUri = baseUri;
		this.agreementPath = agreementPath;
		this.docsPath = docsPath;
		this.patchPath = patchPath;
		this.platformVersion = platformVersion;
		this.platformComposerJsonPath = platformComposerJsonPath;
		this.secret = secret;
	}

	static boolean remoteNewerThanLocalForUpgrade(String remote, String localSecond) {
		ComparableVersion pkgCv = new ComparableVersion(remote);
		ComparableVersion selfCv = new ComparableVersion(localSecond);
		return pkgCv.compareTo(selfCv) > 0;
	}

	public Map<String, Object> detectVersion() {
		JsonNode root = fetchPatchSignedPostJsonRoot();
		JsonNode data = root.get("data");
		if (data == null || !data.isObject()) {
			throw new ResourceException("版本获取失败");
		}
		JsonNode packageNode = data.get("package");
		if (packageNode == null || !packageNode.isObject()) {
			throw new ResourceException("版本获取失败");
		}
		JsonNode verNode = packageNode.get("version");
		String packageVersion;
		if (verNode == null || verNode.isNull()) {
			packageVersion = "";
		} else {
			packageVersion = verNode.asText();
		}
		String remoteRaw = packageVersion == null ? "" : packageVersion;
		String remote = remoteRaw.trim();
		if (isInvalidPackageVersion(remote)) {
			throw new ResourceException("版本获取失败");
		}
		Map<String, Object> result =
				objectMapper.convertValue(packageNode, new TypeReference<LinkedHashMap<String, Object>>() {});
		String localDisplay = resolveLocalPlatformVersionForDetectVersion();
		String localSecond = localDisplay;
		boolean upgradeStatus = remoteNewerThanLocalForUpgrade(remote, localSecond);
		result.put("upgrade_status", upgradeStatus);
		result.put("local_version", localDisplay);
		return result;
	}

	JsonNode fetchPatchSignedPostJsonRoot() {
		long timestamp = Instant.now().getEpochSecond();
		Map<String, Object> signParams = new LinkedHashMap<>();
		signParams.put("timestamp", timestamp);
		String sign = buildSign(signParams);

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("timestamp", Long.toString(timestamp));
		form.add("sign", sign);

		URI uri = URI.create(baseUri.replaceAll("/$", "") + patchPath);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
		String body;
		try {
			body = restTemplate.postForObject(uri, entity, String.class);
		} catch (RestClientException ex) {
			throw new ResourceException("获取升级版本失败，请联系管理人员");
		}

		if (body == null || body.isBlank()) {
			throw new ResourceException("获取升级版本失败，请联系管理人员");
		}
		JsonNode root;
		try {
			root = objectMapper.readTree(body);
		} catch (JsonProcessingException e) {
			throw new ResourceException("获取升级版本失败，请联系管理人员");
		}
		if (root == null || !root.isObject()) {
			throw new ResourceException("获取升级版本失败，请联系管理人员");
		}
		JsonNode statusNode = root.path("status");
		if (!statusNode.isMissingNode() && !"success".equals(statusNode.asText())) {
			throw new ResourceException("获取升级版本失败，请联系管理人员");
		}
		return root;
	}

	String resolveLocalPlatformVersionForDetectVersion() {
		String fromComposer = readComposerVersionField(platformComposerJsonPath);
		if (fromComposer != null && !fromComposer.isBlank()) {
			return fromComposer.trim();
		}
		if (platformVersion != null && !platformVersion.isBlank()) {
			return platformVersion.trim();
		}
		return "-";
	}

	private String readComposerVersionField(String pathProp) {
		if (pathProp == null || pathProp.isBlank()) {
			return null;
		}
		Path path = Paths.get(pathProp.trim());
		if (!Files.isRegularFile(path)) {
			return null;
		}
		try {
			byte[] bytes = Files.readAllBytes(path);
			JsonNode root = objectMapper.readTree(bytes);
			if (root == null || !root.isObject()) {
				return null;
			}
			JsonNode ver = root.get("version");
			if (ver == null || ver.isNull()) {
				return null;
			}
			return ver.asText();
		} catch (IOException e) {
			return null;
		}
	}

	private static boolean isInvalidPackageVersion(String v) {
		if (v == null || v.isEmpty() || v.isBlank()) {
			return true;
		}
		return "0".equals(v);
	}

	public Object getAgreement() {
		long timestamp = Instant.now().getEpochSecond();
		Map<String, Object> signParams = new LinkedHashMap<>();
		signParams.put("timestamp", timestamp);
		String sign = buildSign(signParams);

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("timestamp", Long.toString(timestamp));
		form.add("sign", sign);

		URI uri = URI.create(baseUri.replaceAll("/$", "") + agreementPath);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
		String body = restTemplate.postForObject(uri, entity, String.class);

		if (body == null || body.isBlank()) {
			return Collections.emptyList();
		}
		JsonNode root;
		try {
			root = objectMapper.readTree(body);
		} catch (JsonProcessingException e) {
			return Collections.emptyList();
		}
		if (root == null || !root.isObject()) {
			return Collections.emptyList();
		}
		JsonNode data = root.get("data");
		if (data == null || !data.isObject()) {
			return Collections.emptyList();
		}
		JsonNode agreementNode = data.get("agreement");
		if (agreementNode == null || agreementNode.isNull()) {
			return Collections.emptyList();
		}
		return objectMapper.convertValue(agreementNode, Object.class);
	}

	public Object changelog() {
		long timestamp = Instant.now().getEpochSecond();
		Map<String, Object> signParams = new LinkedHashMap<>();
		signParams.put("timestamp", timestamp);
		String sign = buildSign(signParams);

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("timestamp", Long.toString(timestamp));
		form.add("sign", sign);

		URI uri = URI.create(baseUri.replaceAll("/$", "") + docsPath);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
		String body = restTemplate.postForObject(uri, entity, String.class);

		if (body == null || body.isBlank()) {
			return Collections.emptyList();
		}
		JsonNode root;
		try {
			root = objectMapper.readTree(body);
		} catch (JsonProcessingException e) {
			return Collections.emptyList();
		}
		if (root == null || root.isNull()) {
			return Collections.emptyList();
		}
		if (!root.isObject()) {
			return objectMapper.convertValue(root, Object.class);
		}
		JsonNode statusNode = root.get("status");
		if (statusNode != null && !statusNode.isNull() && !"success".equals(statusNode.asText())) {
			return Collections.emptyList();
		}
		return objectMapper.convertValue(root, Object.class);
	}

	String buildSign(Map<String, ?> paramsForAssemble) {
		String assembled = assembleParams(paramsForAssemble);
		return DigestUtils.md5DigestAsHex((secret + assembled + secret).getBytes(StandardCharsets.UTF_8))
				.toUpperCase(Locale.ROOT);
	}

	String assembleParams(Map<String, ?> params) {
		TreeMap<String, Object> sorted = new TreeMap<>();
		for (Map.Entry<String, ?> e : params.entrySet()) {
			sorted.put(e.getKey(), e.getValue());
		}
		StringBuilder sign = new StringBuilder();
		for (Map.Entry<String, Object> e : sorted.entrySet()) {
			Object val = e.getValue();
			if (val == null) {
				continue;
			}
			String valueString;
			if (val instanceof Boolean b) {
				valueString = b ? "1" : "0";
			} else if (val instanceof Collection<?> || val instanceof Map<?, ?> || val.getClass().isArray()) {
				try {
					valueString = objectMapper.writeValueAsString(val);
				} catch (JsonProcessingException ex) {
					valueString = String.valueOf(val);
				}
			} else {
				valueString = String.valueOf(val);
			}
			sign.append(e.getKey()).append(valueString);
		}
		return sign.toString();
	}
}
