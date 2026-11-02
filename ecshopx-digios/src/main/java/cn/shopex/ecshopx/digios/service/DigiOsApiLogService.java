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

package cn.shopex.ecshopx.digios.service;

import cn.shopex.ecshopx.digios.domain.DigiOsApiLog;
import cn.shopex.ecshopx.digios.mapper.DigiOsApiLogMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DigiOsApiLogService {

	private final DigiOsApiLogMapper apiLogMapper;
	private final ObjectMapper objectMapper;

	public DigiOsApiLogService(DigiOsApiLogMapper apiLogMapper, ObjectMapper objectMapper) {
		this.apiLogMapper = apiLogMapper;
		this.objectMapper = objectMapper;
	}

	public void writeInbound(
			long companyId,
			String method,
			Integer httpStatus,
			String outcome,
			int runtimeMs,
			Object requestBody,
			Object responseBody,
			String errorMessage) {
		persistLog(companyId, "in", method, httpStatus, outcome, runtimeMs, requestBody, responseBody, errorMessage);
	}

	public void writeOutbound(
			long companyId,
			String method,
			Integer httpStatus,
			String outcome,
			int runtimeMs,
			Object requestBody,
			Object responseBody,
			String errorMessage) {
		persistLog(companyId, "out", method, httpStatus, outcome, runtimeMs, requestBody, responseBody, errorMessage);
	}

	private void persistLog(
			long companyId,
			String direction,
			String method,
			Integer httpStatus,
			String outcome,
			int runtimeMs,
			Object requestBody,
			Object responseBody,
			String errorMessage) {
		DigiOsApiLog row = new DigiOsApiLog();
		row.setCompanyId(companyId);
		row.setDirection(direction);
		row.setMethod(method);
		row.setHttpStatus(httpStatus);
		row.setOutcome(outcome);
		row.setRuntimeMs(runtimeMs);
		row.setRequestBody(toJson(requestBody));
		row.setResponseBody(toJson(responseBody));
		if (StringUtils.hasText(errorMessage) && errorMessage.length() > 1024) {
			errorMessage = errorMessage.substring(0, 1024);
		}
		row.setErrorMessage(errorMessage);
		row.setCreated((int) Instant.now().getEpochSecond());
		apiLogMapper.insert(row);
	}

	private String toJson(Object body) {
		if (body == null) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(body);
		} catch (JsonProcessingException e) {
			return String.valueOf(body);
		}
	}

	@SuppressWarnings("unchecked")
	static Map<String, Object> redactSecrets(Map<String, Object> payload) {
		if (payload == null) {
			return Map.of();
		}
		return payload;
	}
}
