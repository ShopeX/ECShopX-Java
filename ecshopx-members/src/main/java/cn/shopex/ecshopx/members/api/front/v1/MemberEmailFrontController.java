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

package cn.shopex.ecshopx.members.api.front.v1;

import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.annotation.FrontAuth;
import cn.shopex.ecshopx.common.annotation.FrontNoAuth;
import cn.shopex.ecshopx.common.core.domain.ApiResult;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.service.admin.AdminMemberRegisterSettingService;
import cn.shopex.ecshopx.members.service.email.MailSettingActivationBaseUrlResolver;
import cn.shopex.ecshopx.members.service.email.MemberEmailActivationService;
import cn.shopex.ecshopx.members.service.email.MemberEmailRegisterService;
import cn.shopex.ecshopx.members.service.email.MemberEmailVerificationService;
import cn.shopex.ecshopx.members.service.email.MemberPasswordPolicyService;
import cn.shopex.ecshopx.members.service.email.MemberPasswordResetService;
import cn.shopex.ecshopx.members.service.h5.H5LoginRequestAssembler;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端会员邮箱通道（frontnoauth）：C1 发验证码/激活邮件、C2 重发激活邮件、C3 邮箱注册。
 *
 * <p>对齐 PHP {@code MembersBundle/Http/FrontApi/V1/Action/Members.php} 的
 * {@code sendMemberEmailCode} / {@code resendActivationEmail} / {@code registerMemberByEmail}
 * 行为、文案与 Redis key。</p>
 */
@DingoResponse(
		resource = DingoResponse.ResourceStyle.DINGO,
		badRequest = DingoResponse.BadRequestStyle.DINGO_400,
		unauthorized = true,
		notFound = false)
@FrontAuth
@RestController("membersFrontV1MemberEmail")
@RequiredArgsConstructor
@RequestMapping("/api/v1/h5app")
public class MemberEmailFrontController {

	private static final Logger log = LoggerFactory.getLogger(MemberEmailFrontController.class);

	private final H5LoginRequestAssembler h5LoginRequestAssembler;
	private final AdminMemberRegisterSettingService adminMemberRegisterSettingService;
	private final MemberEmailVerificationService memberEmailVerificationService;
	private final MemberEmailActivationService memberEmailActivationService;
	private final MailSettingActivationBaseUrlResolver mailSettingActivationBaseUrlResolver;
	private final MemberEmailRegisterService memberEmailRegisterService;
	private final MemberPasswordResetService memberPasswordResetService;
	private final MemberPasswordPolicyService memberPasswordPolicyService;
	private final MembersMapper membersMapper;

	@Value("${common.h5-base-url:}")
	private String commonH5BaseUrl;

	/**
	 * C1 — 发邮箱验证码（purpose=login）或激活链接邮件（purpose=activate）。
	 */
	@FrontNoAuth
	@PostMapping(
			value = "/wxapp/member/email/code",
			name = "会员邮箱验证码/激活邮件",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> sendCode(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged =
				h5LoginRequestAssembler.merge(request, body == null ? Collections.emptyMap() : body);
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveFrontNoAuthTenantCompanyId(request, claims, merged);

		String purpose = trimToNull(merged.get("purpose"));
		if (!MemberEmailVerificationService.PURPOSE_ACTIVATE.equals(purpose)
				&& !MemberEmailVerificationService.PURPOSE_LOGIN.equals(purpose)) {
			throw new ResourceException("邮箱验证码用途无效");
		}

		if (MemberEmailVerificationService.PURPOSE_ACTIVATE.equals(purpose)) {
			String email = memberEmailVerificationService.normalizeEmail(
					trimToNull(merged.get("email")));
			Members member = selectUnverifiedMember(companyId, email);
			if (member == null) {
				throw new ResourceException("无法发送激活邮件，请确认邮箱已注册且尚未激活");
			}
		}

		String imgType = MemberEmailVerificationService.PURPOSE_ACTIVATE.equals(purpose)
				? "sign"
				: "login";
		verifyImageVcode(merged, companyId, imgType);

		String clientIp = clientIp(request);
		String deviceId = request.getHeader("X-Client-Device-Id");

		if (MemberEmailVerificationService.PURPOSE_ACTIVATE.equals(purpose)) {
			String base = resolveActivationBaseUrl(merged, companyId);
			memberEmailActivationService.sendActivationLinkEmail(
					companyId, trimToNull(merged.get("email")), clientIp, deviceId, base);
			return ApiResult.ok(Map.of("message", "激活邮件已发送，请查收"));
		}

		memberEmailVerificationService.sendVerificationCode(
				companyId, trimToNull(merged.get("email")), MemberEmailVerificationService.PURPOSE_LOGIN,
				clientIp, deviceId);
		return ApiResult.ok(Map.of("message", "验证码已发送"));
	}

	/**
	 * C2 — 重发激活链接邮件（仅已注册且未激活会员；冷却超频文案映射）。
	 */
	@FrontNoAuth
	@PostMapping(
			value = "/wxapp/member/email/activation/resend",
			name = "重发激活邮件",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> resendActivation(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged =
				h5LoginRequestAssembler.merge(request, body == null ? Collections.emptyMap() : body);
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveFrontNoAuthTenantCompanyId(request, claims, merged);

		String email = memberEmailVerificationService.normalizeEmail(trimToNull(merged.get("email")));
		if (!MemberEmailVerificationService.isValidEmail(email)) {
			throw new ResourceException("邮箱格式不正确");
		}
		if (selectUnverifiedMember(companyId, email) == null) {
			throw new ResourceException("无法发送激活邮件，请确认邮箱已注册且尚未激活");
		}

		verifyImageVcode(merged, companyId, "sign");

		String base = resolveActivationBaseUrl(merged, companyId);
		String clientIp = clientIp(request);
		String deviceId = request.getHeader("X-Client-Device-Id");

		try {
			memberEmailActivationService.sendActivationLinkEmail(
					companyId, email, clientIp, deviceId, base);
		} catch (ResourceException e) {
			if ("发送过于频繁，请稍后再试".equals(e.getMessage())) {
				throw new ResourceException("请勿频繁请求，稍后再做尝试");
			}
			throw e;
		}
		return ApiResult.ok(Map.of("message", "激活邮件已发送，请查收"));
	}

	/**
	 * C3 — 邮箱注册：密码策略、两次密码一致、图形码（sign）、邮件配置就绪、查重、建会员 + 异步入队激活邮件。
	 */
	@FrontNoAuth
	@PostMapping(
			value = "/wxapp/member/email/register",
			name = "邮箱注册",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> registerByEmail(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged =
				h5LoginRequestAssembler.merge(request, body == null ? Collections.emptyMap() : body);
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveFrontNoAuthTenantCompanyId(request, claims, merged);

		String email = trimToNull(merged.get("email"));
		String password = trimToNull(merged.get("password"));
		if (email == null || password == null) {
			throw new ResourceException("请填写邮箱与密码");
		}
		Object confirmRaw = merged.get("password_confirmation");
		if (confirmRaw == null) {
			confirmRaw = merged.get("password_confirm");
		}
		String passwordConfirm = trimToNull(confirmRaw);
		if (passwordConfirm == null) {
			throw new ResourceException("请填写确认密码");
		}
		if (!password.equals(passwordConfirm)) {
			throw new ResourceException("两次输入的密码不一致");
		}

		verifyImageVcodeForRegister(merged, companyId);

		Map<String, Object> params = new java.util.HashMap<>(merged);
		params.put("company_id", companyId);
		params.put("api_from", claims != null ? claims.get("api_from") : null);
		params.put("unionid", claims != null ? claims.get("unionid") : null);
		params.put("open_id", claims != null ? claims.get("open_id") : null);
		params.put("client_ip", clientIp(request));
		String deviceId = request.getHeader("X-Client-Device-Id");
		if (deviceId != null) {
			params.put("device_id", deviceId);
		}

		Map<String, Object> result = memberEmailRegisterService.registerByEmail(companyId, params);
		return ApiResult.ok(Map.of(
				"message", "注册成功，请查收邮件中的激活链接完成验证后再登录",
				"activation_email_queued",
				Boolean.TRUE.equals(result.get("activation_email_queued"))));
	}

	/**
	 * C4 — 邮箱激活：校验 token + 消费 + 写 email_verified_at。
	 */
	@FrontNoAuth
	@PostMapping(
			value = "/wxapp/member/email/activate",
			name = "邮箱激活",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> activateMemberByEmail(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged =
				h5LoginRequestAssembler.merge(request, body == null ? Collections.emptyMap() : body);
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveFrontNoAuthTenantCompanyId(request, claims, merged);

		String plainToken = trimToNull(merged.get("token"));
		if (plainToken == null) {
			throw new ResourceException("请提供激活链接中的 token");
		}

		Long userId = memberEmailActivationService.validateToken(companyId, plainToken);
		if (userId == null) {
			throw new ResourceException("激活链接无效或已过期");
		}

		Members member = membersMapper.selectOne(new LambdaQueryWrapper<Members>()
				.eq(Members::getCompanyId, companyId)
				.eq(Members::getUserId, userId)
				.last("LIMIT 1"));
		if (member == null) {
			throw new ResourceException("该邮箱未注册");
		}
		if (member.getEmailVerifiedAt() != null) {
			throw new ResourceException("该邮箱已激活");
		}

		memberEmailActivationService.consumeToken(companyId, plainToken);

		long now = Instant.now().getEpochSecond();
		membersMapper.update(null, new LambdaUpdateWrapper<Members>()
				.eq(Members::getCompanyId, companyId)
				.eq(Members::getUserId, userId)
				.set(Members::getEmailVerifiedAt, now));

		return ApiResult.ok(Map.of("message", "邮箱已激活，请使用密码登录"));
	}

	/**
	 * C5 — 重置密码请求：发送含 token 的链接（防枚举，统一文案）。
	 */
	@FrontNoAuth
	@PostMapping(
			value = "/wxapp/member/email/password/reset-request",
			name = "重置密码请求",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> requestMemberPasswordResetEmail(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged =
				h5LoginRequestAssembler.merge(request, body == null ? Collections.emptyMap() : body);
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveFrontNoAuthTenantCompanyId(request, claims, merged);

		String email = memberEmailVerificationService.normalizeEmail(trimToNull(merged.get("email")));
		if (!MemberEmailVerificationService.isValidEmail(email)) {
			throw new ResourceException("邮箱格式不正确");
		}

		Members member = membersMapper.selectOne(new LambdaQueryWrapper<Members>()
				.eq(Members::getCompanyId, companyId)
				.eq(Members::getLoginEmail, email)
				.last("LIMIT 1"));
		if (member != null) {
			try {
				String plain = memberPasswordResetService.createToken(companyId, member.getUserId());
				String resetBase = trimToNull(merged.get("reset_base_url"));
				if (resetBase == null) {
					resetBase = commonH5BaseUrl == null ? "" : commonH5BaseUrl.trim();
				}
				if (resetBase.isEmpty()) {
					throw new ResourceException("请配置重置页基础地址");
				}
				String url = rtrimSlashes(resetBase) + "/reset-password?token="
						+ java.net.URLEncoder.encode(plain, java.nio.charset.StandardCharsets.UTF_8)
						+ "&company_id=" + companyId;
				memberPasswordResetService.sendResetEmail(companyId, email, url);
			} catch (RuntimeException e) {
				// 防枚举：不向前端暴露会员是否存在的信息
				log.warn("C5 reset email send failed companyId={} email={}", companyId, email, e);
			}
		}

		return ApiResult.ok(Map.of("message", "若邮箱已注册，您将收到重置邮件"));
	}

	/**
	 * C6 — 使用邮件中的 token 重置密码。
	 */
	@FrontNoAuth
	@PostMapping(
			value = "/wxapp/member/email/password/reset",
			name = "重置密码",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ApiResult<Map<String, Object>> resetMemberPasswordByEmailToken(
			HttpServletRequest request, @FlexibleBody(required = false) Map<String, Object> body) {
		Map<String, Object> merged =
				h5LoginRequestAssembler.merge(request, body == null ? Collections.emptyMap() : body);
		Map<String, Object> claims = readH5AuthClaimsMap(request);
		long companyId = resolveFrontNoAuthTenantCompanyId(request, claims, merged);

		String token = trimToNull(merged.get("token"));
		if (token == null) {
			throw new ResourceException("请提供重置链接中的 token");
		}
		String password = trimToNull(merged.get("password"));
		if (password == null) {
			throw new ResourceException("请填写邮箱与密码");
		}
		Object confirmRaw = merged.get("password_confirmation");
		if (confirmRaw == null) {
			confirmRaw = merged.get("password_confirm");
		}
		String passwordConfirm = trimToNull(confirmRaw);
		if (passwordConfirm == null) {
			throw new ResourceException("请填写确认密码");
		}
		if (!password.equals(passwordConfirm)) {
			throw new ResourceException("两次输入的密码不一致");
		}

		// 密码策略
		memberPasswordPolicyService.validateOrFail(password);

		Long userId = memberPasswordResetService.validateToken(companyId, token);
		if (userId == null) {
			throw new ResourceException("链接无效或已过期");
		}

		memberPasswordResetService.consumeToken(companyId, token);

		Members member = membersMapper.selectOne(new LambdaQueryWrapper<Members>()
				.eq(Members::getCompanyId, companyId)
				.eq(Members::getUserId, userId)
				.last("LIMIT 1"));
		if (member == null) {
			throw new ResourceException("该邮箱未注册");
		}

		String encoded = org.springframework.security.crypto.factory.PasswordEncoderFactories
				.createDelegatingPasswordEncoder().encode(password);
		membersMapper.update(null, new LambdaUpdateWrapper<Members>()
				.eq(Members::getCompanyId, companyId)
				.eq(Members::getUserId, userId)
				.set(Members::getPassword, encoded));

		return ApiResult.ok(Map.of("message", "ok"));
	}

	private void verifyImageVcodeForRegister(Map<String, Object> merged, long companyId) {
		String token = trimToNull(merged.get("token"));
		if (token == null) {
			throw new ResourceException("请输入图片验证码token");
		}
		String yzm = trimToNull(merged.get("yzm"));
		if (yzm == null) {
			throw new ResourceException("请输入图片验证码");
		}
		if (!adminMemberRegisterSettingService.verifyAndConsumeMemberImageVcode(token, companyId, yzm, "sign")) {
			throw new ResourceException("图片验证码错误");
		}
	}

	private void verifyImageVcode(Map<String, Object> merged, long companyId, String type) {
		String token = trimToNull(merged.get("token"));
		String yzm = trimToNull(merged.get("yzm"));
		if (!adminMemberRegisterSettingService.verifyAndConsumeMemberImageVcode(token, companyId, yzm, type)) {
			throw new ResourceException("图片验证码错误");
		}
	}

	private Members selectUnverifiedMember(long companyId, String email) {
		if (email == null || email.isEmpty()) {
			return null;
		}
		return membersMapper.selectOne(new LambdaQueryWrapper<Members>()
				.eq(Members::getCompanyId, companyId)
				.eq(Members::getLoginEmail, email)
				.last("LIMIT 1"));
	}

	/**
	 * 激活链接 base 地址回退顺序（对齐 PHP）：请求 {@code activation_base_url} →
	 * {@code mailSetting:{companyId}} 的 {@code EMAIL_ACTIVATION_H5_DOMAIN} →
	 * {@code common.h5_base_url}。
	 */
	private String resolveActivationBaseUrl(Map<String, Object> merged, long companyId) {
		String base = trimToNull(merged.get("activation_base_url"));
		if (base == null || base.isEmpty()) {
			base = mailSettingActivationBaseUrlResolver.getH5ActivationBaseUrl(companyId);
		}
		if (base == null || base.isEmpty()) {
			base = commonH5BaseUrl == null ? "" : commonH5BaseUrl.trim();
		}
		return base;
	}

	/**
	 * 无鉴权租户解析（对齐 PHP {@code resolveFrontNoAuthTenantCompanyId}）：
	 * auth claims company_id → 请求属性 → body/query company_id 覆盖；最终无有效值抛错。
	 */
	private static long resolveFrontNoAuthTenantCompanyId(
			HttpServletRequest request, Map<String, Object> claims, Map<String, Object> merged) {
		long authCompanyId = parsePositiveLongOrZero(claims != null ? claims.get("company_id") : null);
		if (authCompanyId <= 0L) {
			authCompanyId = parsePositiveLongOrZero(request.getAttribute(H5FrontAuthAttributes.H5_COMPANY_ID));
		}
		long bodyCompanyId = parsePositiveLongOrZero(merged != null ? merged.get("company_id") : null);
		long companyId = bodyCompanyId > 0L ? bodyCompanyId : authCompanyId;
		if (companyId <= 0L) {
			throw new ResourceException("请提供激活链接中的 company_id（与邮件 URL 中一致）");
		}
		return companyId;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readH5AuthClaimsMap(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (raw instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		return Collections.emptyMap();
	}

	private static String clientIp(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		if (StringUtils.hasText(xff)) {
			return xff.split(",")[0].trim();
		}
		return request.getRemoteAddr() != null ? request.getRemoteAddr() : "";
	}

	private static long parsePositiveLongOrZero(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v instanceof String s && StringUtils.hasText(s)) {
			try {
				return Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String trimToNull(Object raw) {
		if (raw == null) {
			return null;
		}
		String s = String.valueOf(raw).trim();
		return s.isEmpty() ? null : s;
	}

	private static String rtrimSlashes(String value) {
		if (value == null) {
			return "";
		}
		String s = value.trim();
		int end = s.length();
		while (end > 0 && s.charAt(end - 1) == '/') {
			end--;
		}
		return s.substring(0, end);
	}
}
