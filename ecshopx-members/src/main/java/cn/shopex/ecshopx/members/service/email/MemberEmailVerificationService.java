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
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * 邮箱验证码：与短信分 Redis 命名空间；TTL、60s 冷却、按邮箱日限、IP/设备限流。
 *
 * <p>复用 PHP key 形态（{@code members} 连接），验证码 6 位默认 600s；发信读取
 * {@code mailSetting:{companyId}}（{@code companys} 连接）。</p>
 */
@Service
public class MemberEmailVerificationService {

	private static final Logger log = LoggerFactory.getLogger(MemberEmailVerificationService.class);

	public static final String PURPOSE_ACTIVATE = "activate";

	public static final String PURPOSE_LOGIN = "login";

	private static final String DEFAULT_BRAND = "ECShopX";

	private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

	private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

	private final StringRedisTemplate membersRedis;
	private final MailSettingRedisService mailSettingRedisService;
	private final MailSettingPasswordCodec mailSettingPasswordCodec;
	private final CompanySettingReadService companySettingReadService;
	private final int memberEmailVcodeTtl;
	private final int memberEmailSendLimitPerDay;
	private final int memberEmailIpLimitPerDay;
	private final int memberEmailDeviceLimitPerDay;
	private final int memberEmailActivationCooldownSeconds;

	public MemberEmailVerificationService(
			@Qualifier("membersStringRedisTemplate") StringRedisTemplate membersRedis,
			MailSettingRedisService mailSettingRedisService,
			MailSettingPasswordCodec mailSettingPasswordCodec,
			CompanySettingReadService companySettingReadService,
			@Value("${common.member-email-vcode-ttl:600}") int memberEmailVcodeTtl,
			@Value("${common.member-email-send-limit-per-day:${common.sms-send-limit:5}}") int memberEmailSendLimitPerDay,
			@Value("${common.member-email-ip-limit-per-day:200}") int memberEmailIpLimitPerDay,
			@Value("${common.member-email-device-limit-per-day:200}") int memberEmailDeviceLimitPerDay,
			@Value("${common.member-email-activation-cooldown-seconds:90}") int memberEmailActivationCooldownSeconds) {
		this.membersRedis = membersRedis;
		this.mailSettingRedisService = mailSettingRedisService;
		this.mailSettingPasswordCodec = mailSettingPasswordCodec;
		this.companySettingReadService = companySettingReadService;
		this.memberEmailVcodeTtl = memberEmailVcodeTtl;
		this.memberEmailSendLimitPerDay = memberEmailSendLimitPerDay;
		this.memberEmailIpLimitPerDay = memberEmailIpLimitPerDay;
		this.memberEmailDeviceLimitPerDay = memberEmailDeviceLimitPerDay;
		this.memberEmailActivationCooldownSeconds = memberEmailActivationCooldownSeconds;
	}

	public String normalizeEmail(String email) {
		return email == null ? "" : email.trim().toLowerCase();
	}

	public boolean verifyCode(long companyId, String email, String purpose, String code) {
		String normalized = normalizeEmail(email);
		String expect = membersRedis.opsForValue().get(codeKey(companyId, purpose, normalized));
		return expect != null && constantTimeEquals(expect, code);
	}

	public boolean consumeCode(long companyId, String email, String purpose, String code) {
		if (!verifyCode(companyId, email, purpose, code)) {
			return false;
		}
		String normalized = normalizeEmail(email);
		membersRedis.delete(codeKey(companyId, purpose, normalized));
		return true;
	}

	/**
	 * 生成并发送 6 位验证码邮件。仅支持登录用途；返回值明文验证码仅供测试（生产不返回）。
	 */
	public String sendVerificationCode(
			long companyId, String email, String purpose, String clientIp, String deviceId) {
		String normalized = normalizeEmail(email);
		if (!isValidEmail(normalized)) {
			throw new ResourceException("邮箱格式不正确");
		}
		if (!PURPOSE_LOGIN.equals(purpose)) {
			throw new ResourceException("邮箱验证码用途无效");
		}

		assertCanSendEmailPurpose(companyId, purpose, normalized, clientIp, deviceId);

		String code = String.valueOf(ThreadLocalRandom.current().nextInt(100_000, 1_000_000));
		int ttl = memberEmailVcodeTtl > 0 ? memberEmailVcodeTtl : 600;
		membersRedis.opsForValue().set(codeKey(companyId, purpose, normalized), code, Duration.ofSeconds(ttl));
		setPurposeSendCooldown(companyId, purpose, normalized);

		sendMail(companyId, normalized, purpose, code, ttl);

		return code;
	}

	/**
	 * 发登录 6 位码或激活链接邮件前的频控：登录 60s 冷却；激活链接为
	 * {@code member_email_activation_cooldown_seconds}（默认 90s）+ 日限/IP/设备限流。
	 */
	public void assertCanSendEmailPurpose(
			long companyId, String purpose, String normalizedEmail, String clientIp, String deviceId) {
		if (Boolean.TRUE.equals(membersRedis.hasKey(cooldownKey(companyId, purpose, normalizedEmail)))) {
			throw new ResourceException("发送过于频繁，请稍后再试");
		}
		assertRateLimits(companyId, purpose, normalizedEmail, clientIp, deviceId);
	}

	public void setPurposeSendCooldown(long companyId, String purpose, String normalizedEmail) {
		int ttl = 60;
		if (PURPOSE_ACTIVATE.equals(purpose)) {
			ttl = memberEmailActivationCooldownSeconds > 0 ? memberEmailActivationCooldownSeconds : 90;
		}
		membersRedis.opsForValue().set(cooldownKey(companyId, purpose, normalizedEmail), "1", Duration.ofSeconds(ttl));
	}

	private void assertRateLimits(long companyId, String purpose, String email, String clientIp, String deviceId) {
		String day = LocalDate.now(ZONE).format(DAY_FORMAT);

		String emailKey = "yzmemail:" + companyId + ":" + day + ":" + purpose + ":" + DigestUtils.sha1Hex(email);
		long emailCount = incrementAndExpireIfNew(emailKey);
		if (emailCount > memberEmailSendLimitPerDay) {
			throw new ResourceException("该邮箱今日发送次数已达上限");
		}

		if (clientIp != null && !clientIp.isEmpty()) {
			String ipKey = "member:email:ip:" + companyId + ":" + day + ":" + DigestUtils.sha1Hex(clientIp);
			long ipCount = incrementAndExpireIfNew(ipKey);
			if (ipCount > memberEmailIpLimitPerDay) {
				throw new ResourceException("请求过于频繁，请稍后再试");
			}
		}

		if (deviceId != null && !deviceId.isEmpty()) {
			String devKey = "member:email:dev:" + companyId + ":" + day + ":" + DigestUtils.sha1Hex(deviceId);
			long devCount = incrementAndExpireIfNew(devKey);
			if (devCount > memberEmailDeviceLimitPerDay) {
				throw new ResourceException("请求过于频繁，请稍后再试");
			}
		}
	}

	private long incrementAndExpireIfNew(String key) {
		Long count = membersRedis.opsForValue().increment(key);
		if (count != null && count == 1L) {
			membersRedis.expire(key, Duration.ofHours(24));
		}
		return count == null ? 0L : count;
	}

	private void sendMail(long companyId, String to, String purpose, String code, int ttlSeconds) {
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
		String subjectBrand = brand.replace("\r", "").replace("\n", "").replaceAll("<[^>]*>", "");
		String subject = "登录到" + subjectBrand;
		int ttlMinutes = Math.max(1, (int) Math.ceil(ttlSeconds / 60.0));
		String body = buildLoginVcodeHtml(brand, code, ttlMinutes);

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
			helper.setSubject(subject);
			helper.setText(body, true);
			if (!user.isEmpty()) {
				helper.setFrom(user, sender.isEmpty() ? user : sender);
			} else if (!sender.isEmpty()) {
				helper.setFrom(sender);
			}
			mailSender.send(message);
		} catch (MailException | MessagingException | UnsupportedEncodingException e) {
			log.warn(
					"MemberEmailVerificationService: SMTP failed companyId={} purpose={} to={}",
					companyId,
					purpose,
					to,
					e);
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
			log.warn("MemberEmailVerificationService: resolve brand failed companyId={}", companyId, e);
			return DEFAULT_BRAND;
		}
	}

	private static String codeKey(long companyId, String purpose, String normalizedEmail) {
		return "member:email:vcode:" + DigestUtils.sha1Hex(String.valueOf(companyId))
				+ ":" + purpose + ":" + DigestUtils.sha1Hex(normalizedEmail);
	}

	private static String cooldownKey(long companyId, String purpose, String normalizedEmail) {
		return "member:email:vcode_cd:" + DigestUtils.sha1Hex(String.valueOf(companyId))
				+ ":" + purpose + ":" + DigestUtils.sha1Hex(normalizedEmail);
	}

	private static String asString(Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	public static boolean isValidEmail(String email) {
		if (email == null || email.isEmpty()) {
			return false;
		}
		try {
			InternetAddress address = new InternetAddress(email);
			address.validate();
			return true;
		} catch (AddressException e) {
			return false;
		}
	}

	private static boolean constantTimeEquals(String a, String b) {
		if (a == null || b == null || a.length() != b.length()) {
			return false;
		}
		int result = 0;
		for (int i = 0; i < a.length(); i++) {
			result |= a.charAt(i) ^ b.charAt(i);
		}
		return result == 0;
	}

	private static String buildLoginVcodeHtml(String brand, String code, int ttlMinutes) {
		String escapedBrand = escapeHtml(brand);
		String headline = "登录到" + escapedBrand;
		String intro = "您的一次性验证码是：";
		String expireLine = "此验证码将在 " + ttlMinutes + " 分钟后过期。";
		String footerHint = "如果您没有请求登录，您可以放心忽略此邮件。可能是其他人误输入了您的邮箱地址。";
		return """
				<!DOCTYPE html>
				<html lang="zh-CN">
				<head><meta charset="utf-8"><title>%s</title></head>
				<body style="margin:0;padding:24px;background:#f3f4f6;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif;color:#111111;">
				<div style="max-width:560px;margin:0 auto;background:#ffffff;border-radius:12px;padding:32px 28px;box-sizing:border-box;">
				<div style="text-align:left;font-size:16px;font-weight:600;margin-bottom:28px;">%s</div>
				<h1 style="text-align:left;font-size:22px;font-weight:700;margin:0 0 20px;line-height:1.3;">%s</h1>
				<p style="text-align:left;font-size:16px;line-height:1.55;margin:0 0 20px;color:#333333;">%s</p>
				<div style="text-align:left;font-size:36px;font-weight:700;letter-spacing:0.08em;line-height:1.2;margin:0 0 28px;">%s</div>
				<div style="height:1px;background:#e5e7eb;margin:0 0 20px;"></div>
				<p style="text-align:left;font-size:14px;line-height:1.6;margin:0 0 12px;color:#6b7280;">%s</p>
				<p style="text-align:left;font-size:14px;line-height:1.6;margin:0;color:#6b7280;">%s</p>
				</div>
				</body>
				</html>
				""".formatted(escapedBrand, escapedBrand, headline, intro, code, expireLine, footerHint);
	}

	private static String escapeHtml(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
				.replace("\"", "&quot;").replace("'", "&#39;");
	}
}
