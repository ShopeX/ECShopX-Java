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

package cn.shopex.ecshopx.thirdparty.api.thirdapi.v1;

import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpActHandler;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpInboundResult;
import cn.shopex.ecshopx.common.systemlink.OmsQueueLogWritePort;
import cn.shopex.ecshopx.thirdparty.service.saaserp.inbound.SaasErpAcVerifier;
import cn.shopex.ecshopx.thirdparty.service.saaserp.inbound.SaasErpActDispatcher;
import cn.shopex.ecshopx.thirdparty.service.saaserp.inbound.SaasErpCompanyResolver;
import cn.shopex.ecshopx.thirdparty.service.saaserp.inbound.SaasErpInboundResponseWriter;
import cn.shopex.ecshopx.thirdparty.web.SaasErpInboundParameterMerge;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SaasErpInboundController {

	private final SaasErpAcVerifier verifier;
	private final SaasErpCompanyResolver resolver;
	private final SaasErpActDispatcher dispatcher;
	private final SaasErpInboundResponseWriter responseWriter;
	private final OmsQueueLogWritePort logPort;
	private final ObjectMapper objectMapper;

	public SaasErpInboundController(
			SaasErpAcVerifier verifier,
			SaasErpCompanyResolver resolver,
			SaasErpActDispatcher dispatcher,
			SaasErpInboundResponseWriter responseWriter,
			OmsQueueLogWritePort logPort,
			ObjectMapper objectMapper) {
		this.verifier = verifier;
		this.resolver = resolver;
		this.dispatcher = dispatcher;
		this.responseWriter = responseWriter;
		this.logPort = logPort;
		this.objectMapper = objectMapper;
	}

	@PostMapping("/api/thirdparty/saaserp")
	public void saasErp(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Map<String, Object> body = readBody(request);
		Map<String, String> query = queryParametersOnly(request);
		LinkedHashMap<String, Object> raw = SaasErpInboundParameterMerge.merge(body, query);

		Object toNodeIdObj = raw.get("to_node_id");
		String toNodeId = toNodeIdObj instanceof String s ? s : null;
		SaasErpCompanyResolver.Resolved resolved = resolver.resolve(toNodeId);

		if (!verifier.matches(raw, resolved.token())) {
			writeSignError(response, raw);
			return;
		}

		for (Object value : raw.values()) {
			if (!(value instanceof String)) {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				return;
			}
		}

		Map<String, String> trimmed = new LinkedHashMap<>();
		for (Map.Entry<String, Object> entry : raw.entrySet()) {
			trimmed.put(entry.getKey(), ((String) entry.getValue()).trim());
		}

		String act = trimmed.get("act");
		if (dispatcher.targetOf(act) == null) {
			responseWriter.write(response, SaasErpInboundResult.fail("接口不存在"), 0L);
			return;
		}
		if (dispatcher.isBroken(act)) {
			responseWriter.write(response, SaasErpInboundResult.fail("方法不存在"), 0L);
			return;
		}
		Optional<SaasErpActHandler> handlerOpt = dispatcher.find(act);
		if (handlerOpt.isEmpty()) {
			responseWriter.write(response, SaasErpInboundResult.fail("接口不存在"), 0L);
			return;
		}

		long companyId = resolved.companyId();
		String paramsJson;
		try {
			paramsJson = serializeParamsJson(trimmed);
		} catch (JsonProcessingException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		long logId;
		try {
			logId = logPort.insertResponseFail(companyId, act, paramsJson);
		} catch (RuntimeException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		SaasErpActHandler handler = handlerOpt.get();
		SaasErpInboundResult result = handler.handle(trimmed, companyId);
		responseWriter.write(response, result, logId);
	}

	private Map<String, Object> readBody(HttpServletRequest request) throws IOException {
		String ct = request.getContentType();
		boolean jsonLike = ct != null && ct.toLowerCase().contains("application/json");
		if (jsonLike) {
			byte[] bytes = StreamUtils.copyToByteArray(request.getInputStream());
			if (bytes.length == 0) {
				return new LinkedHashMap<>();
			}
			return objectMapper.readValue(bytes, new TypeReference<LinkedHashMap<String, Object>>() {});
		}
		Map<String, Object> fromForm = new LinkedHashMap<>();
		request.getParameterMap().forEach((key, values) -> {
			if (values != null && values.length > 0) {
				fromForm.put(key, values[0]);
			}
		});
		return fromForm;
	}

	private static Map<String, String> queryParametersOnly(HttpServletRequest request) {
		String qs = request.getQueryString();
		if (qs == null || qs.isEmpty()) {
			return Map.of();
		}
		Map<String, String[]> paramMap = request.getParameterMap();
		Map<String, String> query = new LinkedHashMap<>();
		for (String pair : qs.split("&")) {
			if (pair.isEmpty()) {
				continue;
			}
			int eq = pair.indexOf('=');
			String encodedKey = eq >= 0 ? pair.substring(0, eq) : pair;
			String key = URLDecoder.decode(encodedKey, StandardCharsets.UTF_8);
			if (!paramMap.containsKey(key)) {
				continue;
			}
			String[] values = paramMap.get(key);
			if (values != null && values.length > 0) {
				query.put(key, values[0]);
			}
		}
		return query;
	}

	private void writeSignError(HttpServletResponse response, Map<String, Object> raw) throws IOException {
		Map<String, Object> infoMap = new LinkedHashMap<>(raw);
		Object acVal = infoMap.get("ac");
		String acStr = acVal == null ? "" : String.valueOf(acVal);
		if (acStr.trim().isEmpty()) {
			infoMap.put("ac", "");
		}

		String infoJson = objectMapper.writeValueAsString(infoMap);
		Map<String, Object> envelope = new LinkedHashMap<>();
		envelope.put("result", "fail");
		envelope.put("code", 0);
		envelope.put("shopex_time", Instant.now().getEpochSecond());
		envelope.put("msg", "sign error");
		envelope.put("info", infoJson);

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("application/json");
		response.getWriter().write(objectMapper.writeValueAsString(envelope));
	}

	private String serializeParamsJson(Map<String, String> trimmed) throws JsonProcessingException {
		return objectMapper
				.writer()
				.without(JsonGenerator.Feature.ESCAPE_NON_ASCII)
				.writeValueAsString(trimmed);
	}
}
