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

package cn.shopex.ecshopx.openapi.thirdapi;

import cn.shopex.ecshopx.common.openapi.OpenapiMethodDescriptor;
import cn.shopex.ecshopx.common.openapi.OpenapiSignSupport;
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.openapi.web.OpenapiPathPatterns;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;

@Component
public class OpenapiRequestParamCollector {

	private final ObjectMapper objectMapper;

	public OpenapiRequestParamCollector(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> collect(HttpServletRequest request) {
		Map<String, Object> merged = new LinkedHashMap<>(FlexibleHttpServletParameterMap.toObjectMap(request));
		mergeJsonBody(request, merged);
		merged.putIfAbsent("method", resolveMethodFromPath(request));
		return merged;
	}

	public static String resolveMethodFromPath(HttpServletRequest request) {
		String uri = request.getRequestURI();
		if (!StringUtils.hasText(uri)) {
			return null;
		}
		String prefix = OpenapiPathPatterns.PUBLIC_PREFIX;
		if (!uri.startsWith(prefix)) {
			return null;
		}
		String tail = uri.substring(prefix.length());
		if (tail.startsWith("/")) {
			tail = tail.substring(1);
		}
		return tail.isEmpty() ? null : tail;
	}

	public static String resolveMethod(HttpServletRequest request, Map<String, Object> params) {
		Object fromParams = params.get("method");
		if (fromParams != null && StringUtils.hasText(String.valueOf(fromParams))) {
			return String.valueOf(fromParams).trim();
		}
		return resolveMethodFromPath(request);
	}

	public static String normalizeVersion(Object raw) {
		if (raw == null) {
			return "1.0";
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty()) {
			return "1.0";
		}
		if (s.startsWith("v") || s.startsWith("V")) {
			s = s.substring(1);
		}
		if (s.matches("\\d+")) {
			return s + ".0";
		}
		return s;
	}

	public static String versionToJavaSegment(String version) {
		int dot = version.indexOf('.');
		String major = dot > 0 ? version.substring(0, dot) : version;
		return "v" + major;
	}

	public static boolean httpVerbMatches(OpenapiMethodDescriptor descriptor, String requestMethod) {
		return descriptor.httpVerb().equalsIgnoreCase(requestMethod);
	}

	public static Map<String, Object> paramsForSign(Map<String, Object> params) {
		Map<String, Object> copy = new LinkedHashMap<>(params);
		copy.remove("sign");
		return copy;
	}

	public static String requiredString(Map<String, Object> params, String key) {
		Object raw = params.get(key);
		if (raw == null || !StringUtils.hasText(String.valueOf(raw))) {
			return null;
		}
		return String.valueOf(raw).trim();
	}

	private void mergeJsonBody(HttpServletRequest request, Map<String, Object> merged) {
		String contentType = request.getContentType();
		if (contentType == null || !contentType.toLowerCase().contains("json")) {
			return;
		}
		byte[] body = readBody(request);
		if (body.length == 0) {
			return;
		}
		try {
			Map<String, Object> json =
					objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
			if (json != null) {
				json.forEach(
						(k, v) -> {
							if (v != null) {
								merged.put(k, v);
							}
						});
			}
		} catch (IOException ignored) {
			// 非 JSON body 忽略
		}
	}

	private static byte[] readBody(HttpServletRequest request) {
		if (request instanceof ContentCachingRequestWrapper wrapper) {
			return wrapper.getContentAsByteArray();
		}
		return new byte[0];
	}

	public static long parseTimestampSeconds(Object raw) {
		if (raw == null) {
			throw new IllegalArgumentException("timestamp 不合法");
		}
		String s = String.valueOf(raw).trim();
		if (s.matches("\\d{10}")) {
			return Long.parseLong(s);
		}
		try {
			return java.time.Instant.parse(s.replace(' ', 'T')).getEpochSecond();
		} catch (Exception e) {
			try {
				return java.time.LocalDateTime.parse(
								s,
								java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
						.atZone(java.time.ZoneId.systemDefault())
						.toEpochSecond();
			} catch (Exception ex) {
				throw new IllegalArgumentException("timestamp 不合法");
			}
		}
	}

	public static boolean timestampWithinTolerance(Object raw, long toleranceSeconds) {
		long ts = parseTimestampSeconds(raw);
		long now = System.currentTimeMillis() / 1000L;
		return Math.abs(now - ts) <= toleranceSeconds;
	}

	public static boolean verifySign(Map<String, Object> params, String token, String sign) {
		return OpenapiSignSupport.signMatches(paramsForSign(params), token, sign);
	}
}
