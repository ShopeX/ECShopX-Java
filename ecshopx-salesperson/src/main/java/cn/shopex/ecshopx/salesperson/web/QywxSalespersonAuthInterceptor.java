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

package cn.shopex.ecshopx.salesperson.web;

import cn.shopex.ecshopx.common.annotation.QywxSalespersonAuth;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.web.QywxSalespersonAuthAttributes;
import cn.shopex.ecshopx.salesperson.service.WxappSalespersonSessionAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class QywxSalespersonAuthInterceptor implements HandlerInterceptor {

	private static final int CODE_BAD_REQUEST = 400001;

	private static final int CODE_UNAUTHORIZED = 401001;

	private static final int CODE_FORBIDDEN = 403001;

	private final WxappSalespersonSessionAuthService wxappSalespersonSessionAuthService;

	private final ObjectMapper objectMapper;

	public QywxSalespersonAuthInterceptor(
			WxappSalespersonSessionAuthService wxappSalespersonSessionAuthService, ObjectMapper objectMapper) {
		this.wxappSalespersonSessionAuthService = wxappSalespersonSessionAuthService;
		this.objectMapper = objectMapper;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		if (!(handler instanceof HandlerMethod hm)) {
			return true;
		}
		QywxSalespersonAuth ann = hm.getMethodAnnotation(QywxSalespersonAuth.class);
		if (ann == null) {
			ann = hm.getBeanType().getAnnotation(QywxSalespersonAuth.class);
		}
		if (ann == null) {
			return true;
		}
		try {
			Map<String, Object> map = wxappSalespersonSessionAuthService.authenticate(request);
			request.setAttribute(QywxSalespersonAuthAttributes.REQUEST_ATTR, map);
			return true;
		} catch (BadRequestException ex) {
			String msg = ex.getMessage();
			writeDingoJson(
					response,
					msg != null ? msg : "",
					CODE_BAD_REQUEST,
					HttpServletResponse.SC_BAD_REQUEST);
			return false;
		} catch (UnauthorizedException ex) {
			String msg = ex.getMessage();
			writeDingoJson(
					response,
					msg != null && !msg.isEmpty() ? msg : "Unable to authenticate wxapp user.",
					CODE_UNAUTHORIZED,
					HttpServletResponse.SC_UNAUTHORIZED);
			return false;
		} catch (ForbiddenException ex) {
			String msg = ex.getMessage();
			writeDingoJson(
					response,
					msg != null && !msg.isEmpty() ? msg : "无权限",
					CODE_FORBIDDEN,
					HttpServletResponse.SC_FORBIDDEN);
			return false;
		}
	}

	private void writeDingoJson(HttpServletResponse response, String message, int code, int statusCode)
			throws IOException {
		response.setStatus(statusCode);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		Map<String, Object> data = new HashMap<>();
		data.put("message", message);
		data.put("code", code);
		data.put("status_code", statusCode);
		objectMapper.writeValue(response.getOutputStream(), Map.of("data", data));
	}
}
