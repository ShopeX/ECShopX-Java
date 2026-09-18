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

import cn.shopex.ecshopx.common.annotation.DataPass;
import cn.shopex.ecshopx.companys.service.datapass.ShopOperatorDatapassApplyService;
import cn.shopex.ecshopx.espier.security.OperatorJwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 敏感数据放行审计。
 * <p>
 * 非豁免账号访问时，若未获得 datapass 授权，在 request 上设置 {@code x-datapass-block} 属性；
 * 授权通过时记录访问日志。
 */
@Component
public class DataPassInterceptor implements HandlerInterceptor {

	private static final Logger log = LoggerFactory.getLogger(DataPassInterceptor.class);

	private final ShopOperatorDatapassApplyService shopOperatorDatapassApplyService;

	public DataPassInterceptor(ShopOperatorDatapassApplyService shopOperatorDatapassApplyService) {
		this.shopOperatorDatapassApplyService = shopOperatorDatapassApplyService;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (!(handler instanceof HandlerMethod hm)) {
			return true;
		}
		DataPass dataPass = hm.getMethodAnnotation(DataPass.class);
		if (dataPass == null) {
			return true;
		}
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> user = (Map<String, Object>) request.getAttribute(
					OperatorJwtAuthenticationFilter.OPERATOR_JWT_USER_DATA);
			if (user == null) {
				return true;
			}
			String pathAlias =
					StringUtils.hasText(dataPass.pathAlias())
							? dataPass.pathAlias()
							: hm.getBeanType().getSimpleName() + "." + hm.getMethod().getName();
			shopOperatorDatapassApplyService.apply(request, user, pathAlias);
		} catch (Exception e) {
			log.debug("datapass skip: {}", e.getMessage());
		}
		return true;
	}
}
