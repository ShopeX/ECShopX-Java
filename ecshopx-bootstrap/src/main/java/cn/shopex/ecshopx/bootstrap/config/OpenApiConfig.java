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

package cn.shopex.ecshopx.bootstrap.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.lang.reflect.Method;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / springdoc 分组与鉴权面 SecurityScheme 基线配置。
 */
@Configuration
public class OpenApiConfig {

	public static final String GROUP_ADMIN = "admin";

	public static final String GROUP_FRONT = "front";

	public static final String GROUP_OPENAPI = "openapi";

	public static final String SCHEME_ADMIN_BEARER = "AdminBearer";

	public static final String SCHEME_FRONT_TOKEN = "FrontToken";

	public static final String SCHEME_OPENAPI_SIGN = "OpenapiSign";

	private static final String PKG_ADMIN = ".api.admin.";

	private static final String PKG_FRONT = ".api.front.";

	private static final String PKG_OPENAPI_MODULE = ".ecshopx.openapi";

	@Bean
	public OpenAPI ecshopxOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("ECShopX API")
						.description(
								"ECShopX 多模块 REST API。\n\n"
										+ "- **admin**：管理端接口，鉴权面 AdminBearer（Operator JWT）。\n"
										+ "- **front**：C 端 / H5 接口，鉴权面 FrontToken（会员 JWT）。\n"
										+ "- **openapi**：开放 API，鉴权面 OpenapiSign（app_key / sign 等，契约阶段以占位为主）。")
						.version("v1"))
				.components(new Components()
						.addSecuritySchemes(
								SCHEME_ADMIN_BEARER,
								new SecurityScheme()
										.type(SecurityScheme.Type.HTTP)
										.scheme("bearer")
										.bearerFormat("JWT")
										.description("管理端 Operator JWT：Header `Authorization: Bearer {token}`"))
						.addSecuritySchemes(
								SCHEME_FRONT_TOKEN,
								new SecurityScheme()
										.type(SecurityScheme.Type.HTTP)
										.scheme("bearer")
										.bearerFormat("JWT")
										.description("C 端会员 JWT：Header `Authorization: Bearer {token}`"))
						.addSecuritySchemes(
								SCHEME_OPENAPI_SIGN,
								new SecurityScheme()
										.type(SecurityScheme.Type.APIKEY)
										.in(SecurityScheme.In.QUERY)
										.name("sign")
										.description("开放 API 签名鉴权占位；运行期通常还需 app_key、timestamp 等查询参数。")));
	}

	@Bean
	public GroupedOpenApi adminGroupedOpenApi() {
		return GroupedOpenApi.builder()
				.group(GROUP_ADMIN)
				.displayName("Admin")
				.addOpenApiMethodFilter(OpenApiConfig::isAdminController)
				.addOperationCustomizer((operation, handlerMethod) -> {
					operation.addSecurityItem(new SecurityRequirement().addList(SCHEME_ADMIN_BEARER));
					return operation;
				})
				.build();
	}

	@Bean
	public GroupedOpenApi frontGroupedOpenApi() {
		return GroupedOpenApi.builder()
				.group(GROUP_FRONT)
				.displayName("Front")
				.addOpenApiMethodFilter(OpenApiConfig::isFrontController)
				.addOperationCustomizer((operation, handlerMethod) -> {
					operation.addSecurityItem(new SecurityRequirement().addList(SCHEME_FRONT_TOKEN));
					return operation;
				})
				.build();
	}

	@Bean
	public GroupedOpenApi openapiGroupedOpenApi() {
		return GroupedOpenApi.builder()
				.group(GROUP_OPENAPI)
				.displayName("OpenAPI")
				.pathsToMatch("/api/openapi/**")
				.addOpenApiMethodFilter(OpenApiConfig::isOpenapiController)
				.addOperationCustomizer((operation, handlerMethod) -> {
					operation.addSecurityItem(new SecurityRequirement().addList(SCHEME_OPENAPI_SIGN));
					return operation;
				})
				.build();
	}

	private static boolean isAdminController(Method method) {
		return method.getDeclaringClass().getPackageName().contains(PKG_ADMIN);
	}

	private static boolean isFrontController(Method method) {
		return method.getDeclaringClass().getPackageName().contains(PKG_FRONT);
	}

	private static boolean isOpenapiController(Method method) {
		String pkg = method.getDeclaringClass().getPackageName();
		return pkg.contains(PKG_OPENAPI_MODULE);
	}
}