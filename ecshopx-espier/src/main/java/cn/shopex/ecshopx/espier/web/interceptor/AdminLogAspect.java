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

import cn.shopex.ecshopx.common.annotation.AdminLog;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.companys.service.OperatorLogsWriteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AdminLogAspect {

	private static final Logger log = LoggerFactory.getLogger(AdminLogAspect.class);

	private final ObjectMapper objectMapper;
	private final OperatorLogsWriteService operatorLogsWriteService;

	public AdminLogAspect(ObjectMapper objectMapper, OperatorLogsWriteService operatorLogsWriteService) {
		this.objectMapper = objectMapper;
		this.operatorLogsWriteService = operatorLogsWriteService;
	}

	@Around("@annotation(adminLog)")
	public Object around(ProceedingJoinPoint pjp, AdminLog adminLog) throws Throwable {
		Object result = pjp.proceed();
		try {
			if (result instanceof ResponseEntity<?> re && re.getStatusCode().is2xxSuccessful()) {
				ServletRequestAttributes attrs =
						(ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
				if (attrs != null) {
					HttpServletRequest request = attrs.getRequest();
					if ("POST".equalsIgnoreCase(request.getMethod())
							&& "/api/v1/operator/getLevel".equals(request.getServletPath())) {
						try {
							recordShopLoginAuditForGetLevel(request);
						} catch (Exception e) {
							log.debug("getLevel login audit skip: {}", e.getMessage());
						}
					}
				}
				Object body = re.getBody();
				if (body instanceof ApiResult<?> apiResult && apiResult.getData() instanceof Map<?, ?> dataMap) {
					if (dataMap.containsKey("token")) {
						recordLoginLog(body);
					}
				}
			}
		} catch (Exception e) {
			log.debug("login audit skip: {}", e.getMessage());
		}
		return result;
	}

	private void recordShopLoginAuditForGetLevel(HttpServletRequest request) throws Exception {
		Map<String, Object> ctx = new HashMap<>();
		ctx.put("request_uri", request.getRequestURI());
		ctx.put("ip", request.getRemoteAddr());
		ctx.put("log_type", "login");
		ctx.put("operator_name", "登录失败");
		ctx.put("params", objectMapper.writeValueAsString(Map.of("username", "", "logintype", "")));
		operatorLogsWriteService.addLogs(ctx);
	}

	private void recordLoginLog(Object responseBody) {
		ServletRequestAttributes attrs =
				(ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attrs == null) {
			return;
		}
		HttpServletRequest request = attrs.getRequest();
		Map<String, Object> ctx = new HashMap<>();
		ctx.put("request_uri", request.getRequestURI());
		ctx.put("ip", request.getRemoteAddr());
		try {
			ctx.put("params", objectMapper.writeValueAsString(responseBody));
		} catch (Exception e) {
			ctx.put("params", "");
		}
		ctx.put("operator_name", "login");
		operatorLogsWriteService.addLogs(ctx);
	}
}
