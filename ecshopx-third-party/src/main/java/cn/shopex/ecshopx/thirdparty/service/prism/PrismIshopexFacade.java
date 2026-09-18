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

package cn.shopex.ecshopx.thirdparty.service.prism;

import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PrismIshopexFacade {

	private static final Logger log = LoggerFactory.getLogger(PrismIshopexFacade.class);

	private final PrismIshopexHttpClient prismIshopexHttpClient;
	private final ObjectMapper objectMapper;

	public PrismIshopexFacade(PrismIshopexHttpClient prismIshopexHttpClient, ObjectMapper objectMapper) {
		this.prismIshopexHttpClient = prismIshopexHttpClient;
		this.objectMapper = objectMapper;
	}

	public void opaYdleadsCreate(Map<String, Object> params) {
		if (params == null) {
			return;
		}
		try {
			log.info("Prism opaYdleadsCreate params===>{}", params);
			if (!prismIshopexHttpClient.isConfigured()) {
				log.warn("Prism Ishopex client is not configured; skip opaYdleadsCreate");
				return;
			}
			LinkedHashMap<String, Object> body = new LinkedHashMap<>(params);
			String raw = prismIshopexHttpClient.postSignedForm("/opa/ydleads/create", body);
			log.debug("Prism opaYdleadsCreate result:{}", raw);
		} catch (RuntimeException e) {
			log.warn("Prism opaYdleadsCreate failed: {}", e.getMessage());
		}
	}

	/**
	 * Validates the account against Prism Nirvana passport ({@code /yunqiaccount/passport/getinfo}).
	 *
	 * @throws ResourceException when the gateway reports failure (business rule, HTTP 422 in API layer).
	 */
	public void assertYunqiPassportOk(String phone) {
		if (!StringUtils.hasText(phone)) {
			throw new ResourceException("管理员账号错误，请稍后重试~");
		}
		if (!prismIshopexHttpClient.isConfigured()) {
			throw new ResourceException("管理员账号错误，请稍后重试~");
		}
		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("login_name", phone.trim());
		String raw;
		try {
			raw = prismIshopexHttpClient.postSignedForm("/yunqiaccount/passport/getinfo", body);
		} catch (RuntimeException e) {
			log.warn("Prism yunqiaccount getinfo failed: {}", e.getMessage());
			throw new ResourceException("管理员账号错误，请稍后重试~");
		}
		if (!StringUtils.hasText(raw)) {
			throw new ResourceException("管理员账号错误，请稍后重试~");
		}
		try {
			JsonNode root = objectMapper.readTree(raw);
			String status = text(root, "status");
			if ("failure".equalsIgnoreCase(status)) {
				throw new ResourceException("管理员账号错误，请稍后重试~");
			}
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.warn("Prism yunqiaccount getinfo parse failed: {}", e.getMessage());
			throw new ResourceException("管理员账号错误，请稍后重试~");
		}
	}

	private static String text(JsonNode n, String field) {
		if (n == null || !n.has(field) || n.get(field).isNull()) {
			return "";
		}
		return n.get(field).asText("");
	}

	/** SaaS 线上开通回传 Prism {@code /online/callback}。 */
	public void onlineOpenCallback(String issueId, String authorizeUrl) {
		if (issueId == null || issueId.isBlank() || authorizeUrl == null || authorizeUrl.isBlank()) {
			return;
		}
		if (!prismIshopexHttpClient.isConfigured()) {
			log.debug("Prism Ishopex client not configured; skip onlineOpenCallback");
			return;
		}
		try {
			java.util.LinkedHashMap<String, Object> body = new java.util.LinkedHashMap<>();
			body.put("issue_id", issueId);
			body.put("issue_status", "success");
			body.put("url", authorizeUrl);
			String raw = prismIshopexHttpClient.postSignedForm("/online/callback", body);
			log.debug("Prism onlineOpenCallback result: {}", raw);
		} catch (RuntimeException e) {
			log.warn("Prism onlineOpenCallback failed: {}", e.getMessage());
		}
	}
}
