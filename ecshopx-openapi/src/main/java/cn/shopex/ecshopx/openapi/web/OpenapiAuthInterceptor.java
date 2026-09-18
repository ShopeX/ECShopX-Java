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

package cn.shopex.ecshopx.openapi.web;

import cn.shopex.ecshopx.common.openapi.OpenapiAuthAttributes;
import cn.shopex.ecshopx.common.openapi.OpenapiDeveloperLookupPort;
import cn.shopex.ecshopx.common.openapi.OpenapiEnvelope;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiSignSupport;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiRequestParamCollector;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class OpenapiAuthInterceptor implements HandlerInterceptor {

	private static final long TIMESTAMP_TOLERANCE_SECONDS = 600L;

	private final OpenapiDeveloperLookupPort developerLookupPort;
	private final OpenapiRequestParamCollector paramCollector;
	private final ObjectMapper objectMapper;

	@Value("${openapi.debug:0}")
	private int debug;

	@Value("${openapi.system-company-id:${common.system-companys-id:1}}")
	private long systemCompanyId;

	public OpenapiAuthInterceptor(
			OpenapiDeveloperLookupPort developerLookupPort,
			OpenapiRequestParamCollector paramCollector,
			ObjectMapper objectMapper) {
		this.developerLookupPort = developerLookupPort;
		this.paramCollector = paramCollector;
		this.objectMapper = objectMapper;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		if (OpenapiPathPatterns.isInternalPath(request.getRequestURI())) {
			return true;
		}
		if (!OpenapiPathPatterns.isPublicOpenapiPath(request.getRequestURI())) {
			return true;
		}

		Map<String, Object> params = paramCollector.collect(request);
		try {
			String version = OpenapiRequestParamCollector.requiredString(params, "version");
			String timestamp = OpenapiRequestParamCollector.requiredString(params, "timestamp");
			String appKey = OpenapiRequestParamCollector.requiredString(params, "app_key");
			if (!StringUtils.hasText(version)) {
				throw authException("版本号必填", OpenapiErrorCode.VALIDATION_MISSING_PARAMS);
			}
			if (!StringUtils.hasText(timestamp)) {
				throw authException("timestamp必填", OpenapiErrorCode.VALIDATION_MISSING_PARAMS);
			}
			if (!StringUtils.hasText(appKey)) {
				throw authException("app_key必填", OpenapiErrorCode.VALIDATION_MISSING_PARAMS);
			}

			if (debug == 1) {
				OpenapiAuthAttributes.setAuth(request, systemCompanyId);
				return true;
			}

			if (!OpenapiRequestParamCollector.timestampWithinTolerance(timestamp, TIMESTAMP_TOLERANCE_SECONDS)) {
				throw authException("timestamp 不合法", OpenapiErrorCode.VALIDATION_TIMESTAMP_ERROR);
			}

			String sign = OpenapiRequestParamCollector.requiredString(params, "sign");
			if (!StringUtils.hasText(sign)) {
				throw authException("缺少 sign", OpenapiErrorCode.SIGN_ERROR);
			}

			var credentials =
					developerLookupPort
							.findByAppKey(appKey)
							.orElseThrow(
									() -> authException("app_key 不正确", OpenapiErrorCode.VALIDATION_APPKEY_ERROR));

			Map<String, Object> signParams = OpenapiRequestParamCollector.paramsForSign(params);
			if (!OpenapiSignSupport.signMatches(signParams, credentials.appSecret(), sign)) {
				throw authException("sign 不合法", OpenapiErrorCode.SIGN_ERROR);
			}

			OpenapiAuthAttributes.setAuth(request, credentials.companyId());
			return true;
		} catch (OpenapiAuthException ex) {
			writeAuthFailure(response, ex.getCode(), ex.getMessage(), params);
			return false;
		}
	}

	private static OpenapiAuthException authException(String message, String code) {
		return new OpenapiAuthException(message, code);
	}

	private void writeAuthFailure(
			HttpServletResponse response, String code, String message, Map<String, Object> params)
			throws Exception {
		OpenapiEnvelope body = OpenapiEnvelope.fail(code, message, new LinkedHashMap<>(params));
		response.setStatus(HttpServletResponse.SC_OK);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), body);
	}

	private static final class OpenapiAuthException extends RuntimeException {
		private final String code;

		OpenapiAuthException(String message, String code) {
			super(message);
			this.code = code;
		}

		String getCode() {
			return code;
		}
	}
}
