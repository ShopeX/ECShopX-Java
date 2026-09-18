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

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册 Spring MVC 拦截器。
 * <p>
 * 拦截器按注册顺序执行，排列如下：
 * <ol>
 *   <li>{@link EspierAgreementRateLimitInterceptor} — 安装协议接口限流</li>
 *   <li>{@link AdminAuthInterceptor} — 运营端 JWT 认证</li>
 *   <li>{@link FrontNoAuthInterceptor} — H5 端可选 JWT / 匿名 company_id</li>
 *   <li>{@link FrontAuthInterceptor} — H5 端 JWT 认证</li>
 *   <li>{@link ActivatedInterceptor} — 激活 / 权限检查</li>
 *   <li>{@link DataPassInterceptor} — 敏感数据放行审计</li>
 *   <li>{@link ShopLogInterceptor} — 操作日志</li>
 * </ol>
 */
@Configuration
public class MiddlewareWebMvcConfigurer implements WebMvcConfigurer {

	private final AdminAuthInterceptor adminAuthInterceptor;
	private final FrontNoAuthInterceptor frontNoAuthInterceptor;
	private final FrontAuthInterceptor frontAuthInterceptor;
	private final ActivatedInterceptor activatedInterceptor;
	private final ShopLogInterceptor shopLogInterceptor;
	private final DataPassInterceptor dataPassInterceptor;
	private final EspierAgreementRateLimitInterceptor espierAgreementRateLimitInterceptor;

	public MiddlewareWebMvcConfigurer(
			AdminAuthInterceptor adminAuthInterceptor,
			FrontNoAuthInterceptor frontNoAuthInterceptor,
			FrontAuthInterceptor frontAuthInterceptor,
			ActivatedInterceptor activatedInterceptor,
			ShopLogInterceptor shopLogInterceptor,
			DataPassInterceptor dataPassInterceptor,
			EspierAgreementRateLimitInterceptor espierAgreementRateLimitInterceptor) {
		this.adminAuthInterceptor = adminAuthInterceptor;
		this.frontNoAuthInterceptor = frontNoAuthInterceptor;
		this.frontAuthInterceptor = frontAuthInterceptor;
		this.activatedInterceptor = activatedInterceptor;
		this.shopLogInterceptor = shopLogInterceptor;
		this.dataPassInterceptor = dataPassInterceptor;
		this.espierAgreementRateLimitInterceptor = espierAgreementRateLimitInterceptor;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(espierAgreementRateLimitInterceptor)
				.addPathPatterns("/api/v1/espier/system/agreement")
				.order(5);
		registry.addInterceptor(adminAuthInterceptor)
				.addPathPatterns("/api/**")
				.excludePathPatterns(
						"/api/v1/operator/oauth/login",
						"/api/v1/operator/shuyun/login",
						"/api/v1/operator/login",
						"/api/v1/operator/resetpassword",
						"/api/v1/operator/images/code",
						"/api/v1/operator/app/image/code",
						"/api/v1/operator/authorizeurl",
						"/api/v1/operator/oauth/logout",
						"/api/v1/operator/sms/code",
						"/api/v1/operator/basic",
						"/api/v1/operator/credential",
						"/api/v1/third/saascert/cert/validate",
						"/api/v1/third/saascert/matrix/callback/**")
				.order(10);
		registry.addInterceptor(frontNoAuthInterceptor)
				.addPathPatterns("/api/**")
				.order(15);
		registry.addInterceptor(frontAuthInterceptor)
				.addPathPatterns("/api/**")
				.order(20);
		registry.addInterceptor(activatedInterceptor)
				.addPathPatterns("/api/**")
				.order(100);
		registry.addInterceptor(dataPassInterceptor)
				.addPathPatterns("/api/**")
				.order(200);
		registry.addInterceptor(shopLogInterceptor)
				.addPathPatterns("/api/**")
				.order(300);
	}
}
