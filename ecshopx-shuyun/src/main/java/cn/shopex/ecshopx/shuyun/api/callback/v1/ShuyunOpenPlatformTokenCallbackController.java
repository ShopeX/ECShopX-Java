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

package cn.shopex.ecshopx.shuyun.api.callback.v1;

import cn.shopex.ecshopx.shuyun.service.openplatform.OpenPlatformConfigService;
import cn.shopex.ecshopx.shuyun.service.openplatform.TokenCallbackService;
import cn.shopex.ecshopx.shuyun.service.openplatform.TrafficAuditWriter;
import cn.shopex.ecshopx.shuyun.domain.CompanyShuyunOpenPlatformConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C1 Token 回调（不验签）。default + third 双路径。
 * 响应体为 PHP 原样 {@code {code,msg,data}}，不加 ApiResult 包装。
 */
@RestController("shuyunOpenPlatformTokenCallbackV1")
@RequestMapping("/api/v1")
public class ShuyunOpenPlatformTokenCallbackController {

	private static final Logger log = LoggerFactory.getLogger(ShuyunOpenPlatformTokenCallbackController.class);

	private final TokenCallbackService tokenCallbackService;
	private final TrafficAuditWriter trafficAuditWriter;
	private final OpenPlatformConfigService openPlatformConfigService;
	private final ObjectMapper objectMapper;

	public ShuyunOpenPlatformTokenCallbackController(
			TokenCallbackService tokenCallbackService,
			TrafficAuditWriter trafficAuditWriter,
			OpenPlatformConfigService openPlatformConfigService,
			ObjectMapper objectMapper) {
		this.tokenCallbackService = tokenCallbackService;
		this.trafficAuditWriter = trafficAuditWriter;
		this.openPlatformConfigService = openPlatformConfigService;
		this.objectMapper = objectMapper;
	}

	@PostMapping(
			value = {
				"/shuyun/open-platform/callback/token",
				"/third/shuyun/open-platform/callback/token"
			},
			name = "数云开放平台 Token 回调",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> token(HttpServletRequest request) throws IOException {
		String rawBody = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
		Map<String, Object> payload = tokenCallbackService.handle(rawBody);
		String responseJson;
		try {
			responseJson = objectMapper.writeValueAsString(payload);
		} catch (Exception e) {
			responseJson = String.valueOf(payload);
		}
		recordAudit(request, rawBody, payload, responseJson);
		return ResponseEntity.ok(payload);
	}

	private void recordAudit(
			HttpServletRequest request, String rawBody, Map<String, Object> payload, String responseJson) {
		try {
			long companyId = resolveCompanyId(rawBody);
			Object codeObj = payload.get("code");
			int code = codeObj instanceof Number n ? n.intValue() : 0;
			String outcome = code == 200 ? "success" : "business_error";
			String err = code == 200 ? null : String.valueOf(payload.get("msg"));
			trafficAuditWriter.writeInboundToken(
					companyId,
					"in_tk_" + UUID.randomUUID().toString().replace("-", ""),
					headersJson(request),
					rawBody,
					responseJson,
					200,
					outcome,
					err);
		} catch (Exception e) {
			log.debug("token inbound audit skip: {}", e.getMessage());
		}
	}

	private long resolveCompanyId(String rawBody) {
		try {
			JsonNode root = objectMapper.readTree(rawBody);
			String appId = null;
			if (root != null && root.isArray() && !root.isEmpty()) {
				JsonNode first = root.get(0);
				if (first != null && first.has("appId")) {
					appId = first.get("appId").asText("");
				}
			} else if (root != null && root.isObject() && root.has("appId")) {
				appId = root.get("appId").asText("");
			}
			if (StringUtils.hasText(appId)) {
				CompanyShuyunOpenPlatformConfig row = openPlatformConfigService.findByAppId(appId);
				if (row != null && row.getCompanyId() != null) {
					return row.getCompanyId();
				}
			}
		} catch (Exception ignored) {
			// fall through
		}
		return 0L;
	}

	private String headersJson(HttpServletRequest request) {
		Map<String, String> headers = new LinkedHashMap<>();
		Enumeration<String> names = request.getHeaderNames();
		if (names != null) {
			while (names.hasMoreElements()) {
				String name = names.nextElement();
				headers.put(name, request.getHeader(name));
			}
		}
		try {
			return objectMapper.writeValueAsString(headers);
		} catch (Exception e) {
			return "{}";
		}
	}
}
