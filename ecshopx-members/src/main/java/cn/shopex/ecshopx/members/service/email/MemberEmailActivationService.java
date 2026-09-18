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

package cn.shopex.ecshopx.members.service.email;

import cn.shopex.ecshopx.common.crypto.MailSettingPasswordCodec;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.setting.CompanySettingReadService;
import cn.shopex.ecshopx.companys.service.setting.MailSettingRedisService;
import cn.shopex.ecshopx.members.domain.MemberEmailActivationToken;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MemberEmailActivationTokenMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.mail.MessagingException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * 邮箱激活：一次性链接 token（DB 存 SHA-256 哈希），与 PHP
 * {@code MemberEmailActivationService} 对齐。
 */
@Service
public class MemberEmailActivationService {

	private static final Logger log = LoggerFactory.getLogger(MemberEmailActivationService.class);

	/** H5 激活落地页路径（相对 activationBaseUrl 根） */
	public static final String ACTIVATION_EMAIL_PATH = "/subpages/auth/email-activate";

	private static final String DEFAULT_BRAND = "ECShopX";

	private static final String ACTIVATION_SUBJECT = "验证你的邮箱";

	private final MembersMapper membersMapper;
	private final MemberEmailActivationTokenMapper activationTokenMapper;
	private final MemberEmailVerificationService memberEmailVerificationService;
	private final MailSettingRedisService mailSettingRedisService;
	private final MailSettingPasswordCodec mailSettingPasswordCodec;
	private final CompanySettingReadService companySettingReadService;
	private final int activationTokenTtl;

	public MemberEmailActivationService(
			MembersMapper membersMapper,
			MemberEmailActivationTokenMapper activationTokenMapper,
			MemberEmailVerificationService memberEmailVerificationService,
			MailSettingRedisService mailSettingRedisService,
			MailSettingPasswordCodec mailSettingPasswordCodec,
			CompanySettingReadService companySettingReadService,
			@Value("${common.member-email-activation-token-ttl:172800}") int activationTokenTtl) {
		this.membersMapper = membersMapper;
		this.activationTokenMapper = activationTokenMapper;
		this.memberEmailVerificationService = memberEmailVerificationService;
		this.mailSettingRedisService = mailSettingRedisService;
		this.mailSettingPasswordCodec = mailSettingPasswordCodec;
		this.companySettingReadService = companySettingReadService;
		this.activationTokenTtl = activationTokenTtl;
	}

	/**
	 * 向未激活会员发送含激活链接的邮件（注册成功 / purpose=activate 重发）。
	 */
	public void sendActivationLinkEmail(
			long companyId, String email, String clientIp, String deviceId, String activationBaseUrl) {
		String normalized = memberEmailVerificationService.normalizeEmail(email);
		if (!MemberEmailVerificationService.isValidEmail(normalized)) {
			throw new ResourceException("邮箱格式不正确");
		}
		Members member = membersMapper.selectOne(new LambdaQueryWrapper<Members>()
				.eq(Members::getCompanyId, companyId)
				.eq(Members::getLoginEmail, normalized)
				.last("LIMIT 1"));
		if (member == null || member.getEmailVerifiedAt() != null) {
			throw new ResourceException("无法发送激活邮件，请确认邮箱已注册且尚未激活");
		}

		String base = rtrimSlashes(activationBaseUrl);
		if (base.isEmpty()) {
			throw new ResourceException("请配置激活页基础地址（activation_base_url）");
		}

		memberEmailVerificationService.assertCanSendEmailPurpose(
				companyId, MemberEmailVerificationService.PURPOSE_ACTIVATE, normalized, clientIp, deviceId);

		String plain = createToken(companyId, member.getUserId());
		String url = base + ACTIVATION_EMAIL_PATH + "?token="
				+ URLEncoder.encode(plain, StandardCharsets.UTF_8) + "&company_id=" + companyId;
		sendActivationEmail(companyId, normalized, url);

		memberEmailVerificationService.setPurposeSendCooldown(
				companyId, MemberEmailVerificationService.PURPOSE_ACTIVATE, normalized);
	}

	private void invalidatePendingForUser(long companyId, long userId) {
		activationTokenMapper.delete(new LambdaQueryWrapper<MemberEmailActivationToken>()
				.eq(MemberEmailActivationToken::getCompanyId, companyId)
				.eq(MemberEmailActivationToken::getUserId, userId)
				.isNull(MemberEmailActivationToken::getUsedAt));
	}

	private String createToken(long companyId, long userId) {
		invalidatePendingForUser(companyId, userId);

		byte[] random = new byte[32];
		new SecureRandom().nextBytes(random);
		String plain = HexFormat.of().formatHex(random);
		String hash = DigestUtils.sha256Hex(plain);

		long ttl = activationTokenTtl > 0 ? activationTokenTtl : 172800;
		long now = Instant.now().getEpochSecond();

		MemberEmailActivationToken token = new MemberEmailActivationToken();
		token.setCompanyId(companyId);
		token.setUserId(userId);
		token.setTokenHash(hash);
		token.setExpiresAt(now + ttl);
		token.setUsedAt(null);
		token.setCreatedAt(now);
		activationTokenMapper.insert(token);

		return plain;
	}

	/**
	 * 校验激活 token（对齐 PHP {@code MemberEmailActivationService::validateToken}）：
	 * 哈希匹配 + 未消费 + 未过期；失败返回 null。返回 {@code user_id}。
	 */
	public Long validateToken(long companyId, String plainToken) {
		if (plainToken == null || plainToken.isEmpty()) {
			return null;
		}
		String hash = DigestUtils.sha256Hex(plainToken);
		MemberEmailActivationToken row = activationTokenMapper.selectOne(
				new LambdaQueryWrapper<MemberEmailActivationToken>()
						.eq(MemberEmailActivationToken::getCompanyId, companyId)
						.eq(MemberEmailActivationToken::getTokenHash, hash)
						.last("LIMIT 1"));
		if (row == null || row.getUsedAt() != null) {
			return null;
		}
		if (row.getExpiresAt() != null && row.getExpiresAt() < Instant.now().getEpochSecond()) {
			return null;
		}
		return row.getUserId();
	}

	/**
	 * 消费激活 token（写 {@code used_at}；对齐 PHP {@code MemberEmailActivationService::consumeToken}）。
	 */
	public void consumeToken(long companyId, String plainToken) {
		if (plainToken == null || plainToken.isEmpty()) {
			return;
		}
		String hash = DigestUtils.sha256Hex(plainToken);
		activationTokenMapper.update(
				null,
				new LambdaUpdateWrapper<MemberEmailActivationToken>()
						.eq(MemberEmailActivationToken::getCompanyId, companyId)
						.eq(MemberEmailActivationToken::getTokenHash, hash)
						.isNull(MemberEmailActivationToken::getUsedAt)
						.set(MemberEmailActivationToken::getUsedAt, Instant.now().getEpochSecond()));
	}

	private void sendActivationEmail(long companyId, String to, String activationUrlWithToken) {
		Map<String, Object> config = mailSettingRedisService.readMailSetting(companyId);
		String smtpPort = asString(config.get(MailSettingRedisService.EMAIL_SMTP_PORT));
		String relayHost = asString(config.get(MailSettingRedisService.EMAIL_RELAY_HOST));
		if (smtpPort.isEmpty() || relayHost.isEmpty()) {
			throw new ResourceException("店铺邮件未配置，无法发送");
		}
		int port;
		try {
			port = Integer.parseInt(smtpPort.trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("店铺邮件未配置，无法发送");
		}

		String user = asString(config.get(MailSettingRedisService.EMAIL_USER)).trim();
		String sender = asString(config.get(MailSettingRedisService.EMAIL_SENDER)).trim();
		String password = mailSettingPasswordCodec.plainForSmtp(
				asString(config.get(MailSettingRedisService.EMAIL_PASSWORD)));

		String brand = resolveBrand(companyId);
		String body = buildActivationHtml(brand, activationUrlWithToken);

		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender.setHost(relayHost.trim().replaceFirst("(?i)^(ssl|smtps|smtp)://", ""));
		mailSender.setPort(port);
		if (!user.isEmpty()) {
			mailSender.setUsername(user);
			mailSender.setPassword(password);
		}
		mailSender.getJavaMailProperties().put("mail.smtp.auth", "true");
		mailSender.getJavaMailProperties().put("mail.smtp.connectiontimeout", "30000");
		mailSender.getJavaMailProperties().put("mail.smtp.timeout", "30000");
		if (port == 465) {
			mailSender.getJavaMailProperties().put("mail.smtp.ssl.enable", "true");
		} else if (port == 587) {
			mailSender.getJavaMailProperties().put("mail.smtp.starttls.enable", "true");
			mailSender.getJavaMailProperties().put("mail.smtp.starttls.required", "true");
		}

		try {
			var message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
			helper.setTo(to);
			helper.setSubject(ACTIVATION_SUBJECT);
			helper.setText(body, true);
			if (!user.isEmpty()) {
				helper.setFrom(user, sender.isEmpty() ? user : sender);
			} else if (!sender.isEmpty()) {
				helper.setFrom(sender);
			}
			mailSender.send(message);
		} catch (MailException | MessagingException | UnsupportedEncodingException e) {
			log.warn("MemberEmailActivationService: SMTP failed companyId={} to={}", companyId, to, e);
			throw new ResourceException("邮件发送失败");
		}
	}

	private String resolveBrand(long companyId) {
		try {
			Map<String, Object> setting = companySettingReadService.getCompanySetting(companyId, "zh-CN");
			Object brand = setting == null ? null : setting.get("brand_name");
			String name = brand == null ? "" : String.valueOf(brand).trim();
			return name.isEmpty() ? DEFAULT_BRAND : name;
		} catch (RuntimeException e) {
			log.warn("MemberEmailActivationService: resolve brand failed companyId={}", companyId, e);
			return DEFAULT_BRAND;
		}
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

	private static String asString(Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	private static String buildActivationHtml(String brand, String activationUrl) {
		String escapedBrand = escapeHtml(brand);
		String escapedUrl = escapeHtml(activationUrl);
		return """
				<!DOCTYPE html>
				<html lang="zh-CN">
				<head><meta charset="utf-8"><title>验证你的邮箱</title></head>
				<body style="margin:0;padding:24px;background:#f3f4f6;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif;color:#111111;">
				<div style="max-width:560px;margin:0 auto;background:#ffffff;border-radius:12px;padding:32px 28px;box-sizing:border-box;">
				<div style="text-align:left;font-size:16px;font-weight:600;margin-bottom:28px;">%s</div>
				<h1 style="text-align:left;font-size:22px;font-weight:700;margin:0 0 20px;line-height:1.3;">验证你的邮箱</h1>
				<p style="text-align:left;font-size:16px;line-height:1.55;margin:0 0 28px;color:#333333;">成为会员只差一步，点击下面的「验证邮箱」按钮：</p>
				<div style="text-align:left;">
				<a href="%s" style="display:inline-block;padding:14px 32px;border-radius:8px;background:#000000;color:#ffffff;text-decoration:none;font-weight:700;font-size:16px;">验证邮箱</a>
				</div>
				</div>
				</body>
				</html>
				""".formatted(escapedBrand, escapedUrl);
	}

	private static String escapeHtml(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
				.replace("\"", "&quot;").replace("'", "&#39;");
	}
}
