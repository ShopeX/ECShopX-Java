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

package cn.shopex.ecshopx.espier.security;

import cn.shopex.ecshopx.members.service.wxapp.WxappMemberAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置。
 * <p>
 * 认证逻辑已迁移到注解 + 拦截器（{@code @AdminAuth}、{@code @FrontAuth}、{@code @FrontNoAuth}、
 * {@code @FrontMerchantAuth}、{@code @AdminLog}），仅保留小程序 session 链和兜底 permitAll 链。
 */
@Configuration
@EnableWebSecurity
public class EspierSecurityConfig {

	@Bean
	@Order(-2)
	public SecurityFilterChain wxappMemberSessionSecurityFilterChain(
			HttpSecurity http, WxappMemberAuthService wxappMemberAuthService, ObjectMapper objectMapper)
			throws Exception {
		WxappMemberSessionAuthenticationFilter wxappMemberSessionAuthenticationFilter =
				new WxappMemberSessionAuthenticationFilter(wxappMemberAuthService, objectMapper);
		http.securityMatcher("/api/v1/wxapp/**")
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(a -> a.anyRequest().authenticated())
				.addFilterBefore(wxappMemberSessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	@Order(100)
	public SecurityFilterChain defaultPermitAllSecurityFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher("/api/**")
				.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(a -> a.anyRequest().permitAll());
		return http.build();
	}
}
