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

package cn.shopex.ecshopx.openapi.thirdapi.gateway;

import cn.shopex.ecshopx.common.openapi.OpenapiEnvelope;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiNotImplementedException;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiHandlerForwardRegistry;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiRequestParamCollector;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * OpenAPI 公网网关分发：仅依据 {@link OpenapiHandlerForwardRegistry} 路由，不依赖 openapi_list.csv。
 */
@Service
public class OpenapiGatewayDispatchService {

	private final OpenapiHandlerForwardRegistry forwardRegistry;
	private final OpenapiRequestParamCollector paramCollector;
	private final ObjectMapper objectMapper;

	public OpenapiGatewayDispatchService(
			OpenapiHandlerForwardRegistry forwardRegistry,
			OpenapiRequestParamCollector paramCollector,
			ObjectMapper objectMapper) {
		this.forwardRegistry = forwardRegistry;
		this.paramCollector = paramCollector;
		this.objectMapper = objectMapper;
	}

	public void dispatch(HttpServletRequest request, HttpServletResponse response, String pathMethod)
			throws Exception {
		Map<String, Object> params = paramCollector.collect(request);
		String method = OpenapiRequestParamCollector.resolveMethod(request, params);
		if (!StringUtils.hasText(method)) {
			writeFail(response, OpenapiErrorCode.METHOD_NOT_FOUND, "method 不存在", params);
			return;
		}

		String version = OpenapiRequestParamCollector.normalizeVersion(params.get("version"));
		String httpVerb = request.getMethod().toUpperCase();

		String forwardPath =
				forwardRegistry
						.resolveForwardPath(version, httpVerb, method)
						.orElse(null);
		if (forwardPath == null) {
			writeFail(response, OpenapiErrorCode.METHOD_NOT_FOUND, "API 方法不存在", params);
			return;
		}

		RequestDispatcher dispatcher = request.getRequestDispatcher(forwardPath);
		if (dispatcher == null) {
			throw new OpenapiNotImplementedException(version, method);
		}
		dispatcher.forward(request, response);
	}

	public void writeFail(HttpServletResponse response, String code, String message, Object data)
			throws Exception {
		OpenapiEnvelope body = OpenapiEnvelope.fail(code, message, data);
		response.setStatus(HttpServletResponse.SC_OK);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), body);
	}

	static final class OpenapiGatewayException extends RuntimeException {
		private final String code;
		private final Object data;

		OpenapiGatewayException(String code, String message, Object data) {
			super(message);
			this.code = code;
			this.data = data;
		}

		String getCode() {
			return code;
		}

		Object getData() {
			return data;
		}
	}
}
