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

import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.companys.service.activation.ActivatedMiddlewareService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * activated 中间件：菜单权限、企业激活、店铺权限上下文注入。
 */
@Component
public class ActivatedInterceptor implements HandlerInterceptor {

	private final ActivatedMiddlewareService activatedMiddlewareService;

	public ActivatedInterceptor(ActivatedMiddlewareService activatedMiddlewareService) {
		this.activatedMiddlewareService = activatedMiddlewareService;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (!(handler instanceof HandlerMethod hm)) {
			return true;
		}
		if (!hasActivated(hm)) {
			return true;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> user = (Map<String, Object>) request.getAttribute(
				OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
		activatedMiddlewareService.handle(request, hm, user);
		return true;
	}

	private static boolean hasActivated(HandlerMethod hm) {
		if (hm.getMethodAnnotation(Activated.class) != null) {
			return true;
		}
		return hm.getBeanType().isAnnotationPresent(Activated.class);
	}
}
