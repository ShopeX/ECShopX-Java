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
import cn.shopex.ecshopx.members.domain.MemberPasswordResetToken;
import cn.shopex.ecshopx.members.mapper.MemberPasswordResetTokenMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.mail.MessagingException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
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
 * 邮箱找回密码：一次性链接 token（DB 存 SHA-256 哈希，TTL 1200s），对齐 PHP
 * {@code MemberPasswordResetService}。
 */
@Service
public class MemberPasswordResetService {

	private static final Logger log = LoggerFactory.getLogger(MemberPasswordResetService.class);

	private static final String DEFAULT_BRAND = "ECShopX";

	private static final String RESET_SUBJECT = "重置你的密码";

	private final MemberPasswordResetTokenMapper resetTokenMapper;
	private final MailSettingRedisService mailSettingRedisService;
	private final MailSettingPasswordCodec mailSettingPasswordCodec;
	private final CompanySettingReadService companySettingReadService;
	private final int resetTokenTtl;

	public MemberPasswordResetService(
			MemberPasswordResetTokenMapper resetTokenMapper,
			MailSettingRedisService mailSettingRedisService,
			MailSettingPasswordCodec mailSettingPasswordCodec,
			CompanySettingReadService companySettingReadService,
			@Value("${common.member-email-reset-token-ttl:1200}") int resetTokenTtl) {
		this.resetTokenMapper = resetTokenMapper;
		this.mailSettingRedisService = mailSettingRedisService;
		this.mailSettingPasswordCodec = mailSettingPasswordCodec;
		this.companySettingReadService = companySettingReadService;
		this.resetTokenTtl = resetTokenTtl;
	}

	private void invalidatePendingForUser(long companyId, long userId) {
		resetTokenMapper.delete(new LambdaQueryWrapper<MemberPasswordResetToken>()
				.eq(MemberPasswordResetToken::getCompanyId, companyId)
				.eq(MemberPasswordResetToken::getUserId, userId)
				.isNull(MemberPasswordResetToken::getUsedAt));
	}

	/**
	 * @return 明文 token（放入邮件链接）
	 */
	public String createToken(long companyId, long userId) {
		invalidatePendingForUser(companyId, userId);

		byte[] random = new byte[32];
		new SecureRandom().nextBytes(random);
		String plain = HexFormat.of().formatHex(random);
		String hash = DigestUtils.sha256Hex(plain);

		long ttl = resetTokenTtl > 0 ? resetTokenTtl : 1200;
		long now = Instant.now().getEpochSecond();

		MemberPasswordResetToken token = new MemberPasswordResetToken();
		token.setCompanyId(companyId);
		token.setUserId(userId);
		token.setTokenHash(hash);
		token.setExpiresAt(now + ttl);
		token.setUsedAt(null);
		token.setCreatedAt(now);
		resetTokenMapper.insert(token);

		return plain;
	}

	/**
	 * 校验重置 token（对齐 PHP {@code MemberPasswordResetService::validateToken}）：
	 * 哈希匹配 + 未消费 + 未过期；失败返回 null。返回 {@code user_id}。
	 */
	public Long validateToken(long companyId, String plainToken) {
		if (plainToken == null || plainToken.isEmpty()) {
			return null;
		}
		String hash = DigestUtils.sha256Hex(plainToken);
		MemberPasswordResetToken row = resetTokenMapper.selectOne(
				new LambdaQueryWrapper<MemberPasswordResetToken>()
						.eq(MemberPasswordResetToken::getCompanyId, companyId)
						.eq(MemberPasswordResetToken::getTokenHash, hash)
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
	 * 消费重置 token（写 {@code used_at}；对齐 PHP {@code MemberPasswordResetService::consumeToken}）。
	 */
	public void consumeToken(long companyId, String plainToken) {
		if (plainToken == null || plainToken.isEmpty()) {
			return;
		}
		String hash = DigestUtils.sha256Hex(plainToken);
		resetTokenMapper.update(
				null,
				new LambdaUpdateWrapper<MemberPasswordResetToken>()
						.eq(MemberPasswordResetToken::getCompanyId, companyId)
						.eq(MemberPasswordResetToken::getTokenHash, hash)
						.isNull(MemberPasswordResetToken::getUsedAt)
						.set(MemberPasswordResetToken::getUsedAt, Instant.now().getEpochSecond()));
	}

	/**
	 * 邮件内 href 仅允许 http(s) 绝对 URL，防止 javascript: 等协议（对齐 PHP
	 * {@code MemberPasswordResetService::isAllowedResetEmailUrl}）。
	 */
	public static boolean isAllowedResetEmailUrl(String url) {
		if (url == null) {
			return false;
		}
		String trimmed = url.trim();
		if (trimmed.isEmpty()) {
			return false;
		}
		try {
			URI uri = new URI(trimmed);
			if (!uri.isAbsolute() || uri.getHost() == null) {
				return false;
			}
			String scheme = uri.getScheme();
			return scheme != null && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
		} catch (URISyntaxException e) {
			return false;
		}
	}

	public void sendResetEmail(long companyId, String email, String resetUrlWithToken) {
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

		if (!isAllowedResetEmailUrl(resetUrlWithToken)) {
			throw new ResourceException("重置链接无效，请重新申请");
		}

		String user = asString(config.get(MailSettingRedisService.EMAIL_USER)).trim();
		String sender = asString(config.get(MailSettingRedisService.EMAIL_SENDER)).trim();
		String password = mailSettingPasswordCodec.plainForSmtp(
				asString(config.get(MailSettingRedisService.EMAIL_PASSWORD)));

		String brand = resolveBrand(companyId);
		int ttlSeconds = resetTokenTtl > 0 ? resetTokenTtl : 1200;
		int ttlMinutes = Math.max(1, (int) Math.ceil(ttlSeconds / 60.0));
		String body = buildResetHtml(brand, resetUrlWithToken, ttlMinutes);

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
			helper.setTo(email);
			helper.setSubject(RESET_SUBJECT);
			helper.setText(body, true);
			if (!user.isEmpty()) {
				helper.setFrom(user, sender.isEmpty() ? user : sender);
			} else if (!sender.isEmpty()) {
				helper.setFrom(sender);
			}
			mailSender.send(message);
		} catch (MailException | MessagingException | UnsupportedEncodingException e) {
			log.warn("MemberPasswordResetService: SMTP failed companyId={} to={}", companyId, email, e);
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
			log.warn("MemberPasswordResetService: resolve brand failed companyId={}", companyId, e);
			return DEFAULT_BRAND;
		}
	}

	private static String asString(Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	private static String buildResetHtml(String brand, String resetUrl, int ttlMinutes) {
		String escapedBrand = escapeHtml(brand);
		String escapedUrl = escapeHtml(resetUrl);
		return """
				<!DOCTYPE html>
				<html lang="zh-CN">
				<head><meta charset="utf-8"><title>重置你的密码</title></head>
				<body style="margin:0;padding:24px;background:#f3f4f6;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif;color:#111111;">
				<div style="max-width:560px;margin:0 auto;background:#ffffff;border-radius:12px;padding:32px 28px;box-sizing:border-box;">
				<div style="text-align:left;font-size:16px;font-weight:600;margin-bottom:28px;">%s</div>
				<h1 style="text-align:left;font-size:22px;font-weight:700;margin:0 0 20px;line-height:1.3;">重置你的密码</h1>
				<p style="text-align:left;font-size:16px;line-height:1.55;margin:0 0 28px;color:#333333;">我们收到了重置密码的请求，点击下面的「重置密码」按钮设置新密码：</p>
				<div style="text-align:left;">
				<a href="%s" style="display:inline-block;padding:14px 32px;border-radius:8px;background:#000000;color:#ffffff;text-decoration:none;font-weight:700;font-size:16px;">重置密码</a>
				</div>
				<div style="height:1px;background:#e5e7eb;margin:24px 0 20px;"></div>
				<p style="text-align:left;font-size:14px;line-height:1.6;margin:0 0 12px;color:#6b7280;">此链接将在 %d 分钟后过期。</p>
				<p style="text-align:left;font-size:14px;line-height:1.6;margin:0;color:#6b7280;">如果您没有请求重置密码，可以放心忽略此邮件。</p>
				</div>
				</body>
				</html>
				""".formatted(escapedBrand, escapedUrl, ttlMinutes);
	}

	private static String escapeHtml(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
				.replace("\"", "&quot;").replace("'", "&#39;");
	}
}
