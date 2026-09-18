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

import cn.shopex.ecshopx.common.auth.OperatorJwtBlacklistPort;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.OperatorDistributorSelectionService;
import cn.shopex.ecshopx.companys.service.activation.CompanysActivationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Component
public class OperatorJwtAuthenticationFilter extends OncePerRequestFilter {

	/** Request attribute key for JWT claims map after successful authentication. */
	public static final String OPERATOR_JWT_USER_DATA = OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA;

	/** Default message when Authorization is missing or not Bearer (same string as existing admin API clients). */
	private static final String DINGO_BAD_CREDENTIALS_MESSAGE =
			"Failed to authenticate because of bad credentials or an invalid authorization header.";

	private final CompanysActivationService companysActivationService;
	private final ObjectMapper objectMapper;
	private final SecretKey signingKey;
	private final OperatorJwtBlacklistPort operatorJwtBlacklistPort;
	private final OperatorDistributorSelectionService operatorDistributorSelectionService;
	private final ShopAppMemberTokenOperatorEnrichmentService shopAppMemberTokenOperatorEnrichmentService;
	private final RequestMappingHandlerMapping requestMappingHandlerMapping;

	public OperatorJwtAuthenticationFilter(
			CompanysActivationService companysActivationService,
			ObjectMapper objectMapper,
			@Value("${JWT_SECRET:}") String jwtSecret,
			OperatorJwtBlacklistPort operatorJwtBlacklistPort,
			OperatorDistributorSelectionService operatorDistributorSelectionService,
			ShopAppMemberTokenOperatorEnrichmentService shopAppMemberTokenOperatorEnrichmentService,
			@Lazy RequestMappingHandlerMapping requestMappingHandlerMapping) {
		this.companysActivationService = companysActivationService;
		this.objectMapper = objectMapper;
		this.signingKey = buildKey(jwtSecret);
		this.operatorJwtBlacklistPort = operatorJwtBlacklistPort;
		this.operatorDistributorSelectionService = operatorDistributorSelectionService;
		this.shopAppMemberTokenOperatorEnrichmentService = shopAppMemberTokenOperatorEnrichmentService;
		this.requestMappingHandlerMapping = requestMappingHandlerMapping;
	}

	private static SecretKey buildKey(String jwtSecret) {
		if (jwtSecret == null || jwtSecret.isBlank()) {
			throw new IllegalStateException("JWT_SECRET is required");
		}
		byte[] keyBytes;
		try {
			keyBytes = Base64.getDecoder().decode(jwtSecret);
		} catch (IllegalArgumentException e) {
			keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
		}
		return Keys.hmacShaKeyFor(keyBytes);
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getServletPath();
		// 未映射到任何 Controller 的 path：交由 DispatcherServlet 走正常 404 流程，不在此处返回 401
		if (!hasHandler(request)) {
			return true;
		}
		if ("POST".equalsIgnoreCase(request.getMethod()) && path != null && path.equals("/api/v1/operator/login")) {
			return true;
		}
		if ("POST".equalsIgnoreCase(request.getMethod()) && path != null && path.equals("/api/v1/operator/getLevel")) {
			return true;
		}
		if ("GET".equalsIgnoreCase(request.getMethod()) && path != null && path.equals("/api/v1/token/refresh")) {
			return true;
		}
		// Shopex OAuth / 数云 code 登录：匿名换票，与 AdminAuthInterceptor exclude 一致
		if ("POST".equalsIgnoreCase(request.getMethod()) && path != null
				&& (path.equals("/api/v1/operator/oauth/login") || path.equals("/api/v1/operator/shuyun/login"))) {
			return true;
		}
		// 重置密码、图片 / 短信验证码：与 MiddlewareWebMvcConfigurer AdminAuth exclude 一致
		if ("POST".equalsIgnoreCase(request.getMethod()) && path != null
				&& (path.equals("/api/v1/operator/resetpassword")
						|| path.equals("/api/v1/operator/sms/code"))) {
			return true;
		}
		if ("GET".equalsIgnoreCase(request.getMethod()) && path != null
				&& (path.equals("/api/v1/operator/images/code")
						|| path.equals("/api/v1/operator/app/image/code")
						|| path.equals("/api/v1/operator/authorizeurl")
						|| path.equals("/api/v1/operator/oauth/logout")
						|| path.equals("/api/v1/operator/basic")
						|| path.equals("/api/v1/operator/credential"))) {
			// 静态 api_token + retrieveByCredentials，登录前换票，与 MiddlewareWebMvcConfigurer AdminAuth exclude 一致
			return true;
		}
		if ("POST".equalsIgnoreCase(request.getMethod()) && path != null && path.equals("/api/v1/operator/app/sms/code")) {
			return true;
		}
		// 店务微信 OAuth / 绑定等：不要求已携带运营 JWT，由 Controller 做参数与业务校验
		if (path != null && path.startsWith("/api/v1/operator/wechat/")) {
			return true;
		}
		// 企业微信 OAuth / 手机号绑定换 JWT：不要求已携带运营 JWT
		if (path != null && path.startsWith("/api/v1/operator/workwechat/")) {
			return true;
		}
		// 自建应用回调（URL 校验、事件推送）：不携带运营 JWT
		if (path != null && path.startsWith("/api/v1/workwechat/notify/")) {
			return true;
		}
		// 客户联系回调（URL 校验、事件推送）：不携带运营 JWT
		if (path != null && path.startsWith("/api/v1/workwechat/customer/notify/")) {
			return true;
		}
		// 通讯录回调（URL 校验、事件推送）：不携带运营 JWT
		if (path != null && path.startsWith("/api/v1/workwechat/report/notify/")) {
			return true;
		}
		// 支付宝异步通知：不携带运营 JWT
		if ("POST".equalsIgnoreCase(request.getMethod()) && path != null && path.equals("/api/v1/alipay/notify")) {
			return true;
		}
		// 银联商务异步通知：不携带运营 JWT
		if ("POST".equalsIgnoreCase(request.getMethod()) && path != null && path.equals("/api/v1/chinaums/notify")) {
			return true;
		}
		// 斗门国际异步通知：不携带运营 JWT
		if ("POST".equalsIgnoreCase(request.getMethod())
				&& path != null
				&& path.equals("/api/v1/doumen-intl/notify")) {
			return true;
		}
		// AdaPay 第三方异步回调：不携带运营 JWT
		if ("POST".equalsIgnoreCase(request.getMethod()) && path != null && path.equals("/api/v1/adapay/callback")) {
			return true;
		}
		// BSPay 第三方异步回调：不携带运营 JWT
		if ("POST".equalsIgnoreCase(request.getMethod()) && path != null && path.startsWith("/api/v1/bspay/callback/")) {
			return true;
		}
		// 安装协议：安装向导匿名读取，不要求运营 JWT
		if ("POST".equalsIgnoreCase(request.getMethod()) && path != null
				&& path.equals("/api/v1/espier/system/agreement")) {
			return true;
		}
		if ("GET".equalsIgnoreCase(request.getMethod()) && path != null
				&& (path.equals("/api/v1/im/meiqia/distributor") || path.equals("/api/v1/im/meiqia/distributor/"))) {
			return true;
		}
		if ("GET".equalsIgnoreCase(request.getMethod()) && path != null
				&& ("/api/v1/datacube/monitorsWxaCode64".equals(path)
						|| "/api/v1/datacube/monitorsWxaCodeStream".equals(path))) {
			return true;
		}
		// 矩阵绑定回打 / 证书反查：矩阵侧匿名回调，与 PHP saascert 公开路由一致
		if (path != null
				&& (path.equals("/api/v1/third/saascert/cert/validate")
						|| path.startsWith("/api/v1/third/saascert/matrix/callback/"))) {
			return true;
		}
		// OpenAPI 第三方网关（公开 /api/openapi/** 与 internal forward）：走 sign 鉴权，不使用运营 JWT
		if (path != null && path.startsWith("/api/openapi")) {
			return true;
		}
		// 微信 legacy 开放路径（小程序码、支付/开放平台回调等）：不携带运营 JWT
		if (path != null && path.startsWith("/wechatAuth/")) {
			return true;
		}
		// 前台 H5 / 小程序等走独立鉴权与 company_id 解析，不使用运营端 JWT
		return path != null
				&& (path.startsWith("/api/v1/h5app/") || path.startsWith("/api/v1/wxapp/"));
	}

	/** 该请求是否能映射到某个 Controller 方法。映射不到（不存在的 path 或方法不匹配）则不在此处鉴权，交由 DispatcherServlet 返回 404/405。 */
	private boolean hasHandler(HttpServletRequest request) {
		try {
			return requestMappingHandlerMapping.getHandler(request) != null;
		} catch (Exception e) {
			// 路径存在但 HTTP 方法/媒体类型不符时 getHandler 会抛异常（如 405），交由 DispatcherServlet 返回对应错误，不要在此返回 401
			return false;
		}
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (shouldNotFilter(request)) {
			filterChain.doFilter(request, response);
			return;
		}
		String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (auth == null || !auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
			writeUnauthorized(response, DINGO_BAD_CREDENTIALS_MESSAGE);
			return;
		}
		String token = auth.substring(7).trim();
		if (token.isEmpty()) {
			writeUnauthorized(response, DINGO_BAD_CREDENTIALS_MESSAGE);
			return;
		}
		if (operatorJwtBlacklistPort.isBlacklisted(token)) {
			writeUnauthorized(response, "登录验证错误");
			return;
		}
		try {
			Map<String, Object> userData =
					OperatorJwtCompactCodec.verifyAndParse(objectMapper, token, signingKey);
			long nowSec = Instant.now().getEpochSecond();
			if (nowSec > OperatorJwtCompactCodec.claimEpochSeconds(userData, "exp")) {
				writeUnauthorized(response, "登录验证错误");
				return;
			}
			try {
				shopAppMemberTokenOperatorEnrichmentService.enrichIfApplicable(userData);
				companysActivationService.checkUserAuth(userData);
			} catch (ResourceException e) {
				String msg = e.getMessage();
				writeForbidden(response, msg != null && !msg.isEmpty() ? msg : "未登录");
				return;
			}
			companysActivationService.attachOperatorIdFromSessionClaims(userData);
			attachSelectedDistributor(userData);
			request.setAttribute(OPERATOR_JWT_USER_DATA, userData);
		} catch (Exception e) {
			writeUnauthorized(response, "登录验证错误");
			return;
		}
		filterChain.doFilter(request, response);
	}

	private void attachSelectedDistributor(Map<String, Object> userData) {
		if (!"distributor".equals(String.valueOf(userData.get("operator_type")))) {
			return;
		}
		Long operatorId = longOrNull(userData.get("operator_id"));
		if (operatorId == null || operatorId <= 0L) {
			operatorId = longOrNull(userData.get("id"));
		}
		Long companyId = longOrNull(userData.get("company_id"));
		if (operatorId == null || operatorId <= 0L || companyId == null || companyId <= 0L) {
			userData.put("distributor_id", 0L);
			return;
		}
		userData.put(
				"distributor_id",
				operatorDistributorSelectionService.readSelectedDistributorId(operatorId, companyId).orElse(0L));
	}

	private static Long longOrNull(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String text = String.valueOf(raw).trim();
		if (text.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(text);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType("application/json;charset=UTF-8");
		Map<String, Object> data = new HashMap<>();
		data.put("message", message);
		data.put("status_code", HttpServletResponse.SC_UNAUTHORIZED);
		Map<String, Object> body = new HashMap<>();
		body.put("data", data);
		objectMapper.writeValue(response.getOutputStream(), body);
	}

	private void writeForbidden(HttpServletResponse response, String message) throws IOException {
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType("application/json;charset=UTF-8");
		objectMapper.writeValue(response.getOutputStream(), ApiResult.fail(HttpServletResponse.SC_FORBIDDEN, message));
	}
}
