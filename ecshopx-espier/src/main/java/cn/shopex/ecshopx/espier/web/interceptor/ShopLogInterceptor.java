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

package cn.shopex.ecshopx.espier.web.interceptor;

import cn.shopex.ecshopx.common.annotation.ShopLog;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 非 GET 请求完成后写入商家操作日志。
 */
@Component
public class ShopLogInterceptor implements HandlerInterceptor {

	private static final Logger log = LoggerFactory.getLogger(ShopLogInterceptor.class);

	private final OperatorLogsWriteService operatorLogsWriteService;

	public ShopLogInterceptor(OperatorLogsWriteService operatorLogsWriteService) {
		this.operatorLogsWriteService = operatorLogsWriteService;
	}

	@Override
	public void afterCompletion(
			HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
		if (!(handler instanceof HandlerMethod hm)) {
			return;
		}
		if (!hasShopLog(hm)) {
			return;
		}
		if ("GET".equalsIgnoreCase(request.getMethod())) {
			return;
		}
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> user = (Map<String, Object>) request.getAttribute(
					OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
			if (user == null) {
				return;
			}
			Map<String, Object> params = new HashMap<>();
			params.put("company_id", user.get("company_id"));
			params.put("operator_id", user.get("operator_id"));
			params.put("merchant_id", user.getOrDefault("merchant_id", 0));

			String mappingName = resolveMappingName(hm);
			params.put("operator_name", mappingName);
			params.put("request_uri", request.getRequestURI());
			params.put("log_type", "operator");

			String realIp = request.getHeader("X-Forwarded-For");
			if (realIp != null && !realIp.isEmpty()) {
				params.put("ip", realIp.split(",")[0].trim());
			} else {
				params.put("ip", request.getRemoteAddr());
			}
			params.put("params", "");
			operatorLogsWriteService.addLogs(params);
		} catch (Exception e) {
			log.debug("shoplog skip: {}", e.getMessage());
		}
	}

	private static boolean hasShopLog(HandlerMethod hm) {
		if (hm.getMethodAnnotation(ShopLog.class) != null) {
			return true;
		}
		return hm.getBeanType().isAnnotationPresent(ShopLog.class);
	}

	private static String resolveMappingName(HandlerMethod hm) {
		return hm.getBeanType().getSimpleName() + "." + hm.getMethod().getName();
	}
}
