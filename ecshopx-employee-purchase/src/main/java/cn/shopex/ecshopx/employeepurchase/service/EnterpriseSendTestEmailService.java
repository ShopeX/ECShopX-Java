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

package cn.shopex.ecshopx.employeepurchase.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.domain.EnterpriseEmailBox;
import cn.shopex.ecshopx.employeepurchase.mapper.EnterpriseEmailBoxMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EnterpriseSendTestEmailService {

	private static final Logger log = LoggerFactory.getLogger(EnterpriseSendTestEmailService.class);

	private final EnterpriseEmailBoxMapper enterpriseEmailBoxMapper;

	public EnterpriseSendTestEmailService(EnterpriseEmailBoxMapper enterpriseEmailBoxMapper) {
		this.enterpriseEmailBoxMapper = enterpriseEmailBoxMapper;
	}

	public Map<String, Object> sendTestEmail(Map<String, Object> merged, Map<String, Object> operatorJwt) {
		long companyId = readCompanyId(operatorJwt);
		long enterpriseId = readEnterpriseId(merged);
		String toEmail = readValidatedEmail(merged);

		EnterpriseEmailBox box = enterpriseEmailBoxMapper.selectOne(
				new LambdaQueryWrapper<EnterpriseEmailBox>()
						.eq(EnterpriseEmailBox::getCompanyId, companyId)
						.eq(EnterpriseEmailBox::getEnterpriseId, enterpriseId));
		if (box == null) {
			throw new ResourceException("企业未配置发件邮箱");
		}

		int port;
		try {
			String sp = box.getSmtpPort();
			if (sp == null || sp.trim().isEmpty()) {
				throw new NumberFormatException("empty smtp port");
			}
			port = Integer.parseInt(sp.trim());
		} catch (NumberFormatException e) {
			log.warn("sendTestEmail: invalid smtp_port for companyId={} enterpriseId={}", companyId, enterpriseId);
			return Map.of("status", false);
		}

		String relayHost = box.getRelayHost();
		if (relayHost == null || relayHost.trim().isEmpty()) {
			log.warn("sendTestEmail: empty relay_host for companyId={} enterpriseId={}", companyId, enterpriseId);
			return Map.of("status", false);
		}

		String fromUser = box.getUser();
		if (fromUser == null || fromUser.trim().isEmpty()) {
			log.warn("sendTestEmail: empty user for companyId={} enterpriseId={}", companyId, enterpriseId);
			return Map.of("status", false);
		}
		fromUser = fromUser.trim();

		String password = box.getPassword() != null ? box.getPassword() : "";

		try {
			JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
			mailSender.setHost(relayHost.trim().replaceFirst("(?i)^(ssl|smtps|smtp)://", ""));
			mailSender.setPort(port);
			mailSender.setUsername(fromUser);
			mailSender.setPassword(password);
			mailSender.getJavaMailProperties().put("mail.smtp.auth", "true");
			mailSender.getJavaMailProperties().put("mail.smtp.connectiontimeout", "30000");
			mailSender.getJavaMailProperties().put("mail.smtp.timeout", "30000");
			if (port == 465) {
				mailSender.getJavaMailProperties().put("mail.smtp.ssl.enable", "true");
			} else if (port == 587) {
				mailSender.getJavaMailProperties().put("mail.smtp.starttls.enable", "true");
				mailSender.getJavaMailProperties().put("mail.smtp.starttls.required", "true");
			}

			var message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
			helper.setTo(toEmail);
			helper.setSubject("发件测试");
			helper.setText("<p>邮箱配置成功。</p>\n", true);
			helper.setFrom(fromUser);
			mailSender.send(message);
			log.info(
					"sendTestEmail: sent test mail to {} for companyId={} enterpriseId={}",
					toEmail,
					companyId,
					enterpriseId);
			return Map.of("status", true);
		} catch (MailException | MessagingException e) {
			log.warn(
					"sendTestEmail: SMTP failed for companyId={} enterpriseId={} to={}",
					companyId,
					enterpriseId,
					toEmail,
					e);
			return Map.of("status", false);
		}
	}

	private static long readCompanyId(Map<String, Object> operatorJwt) {
		Object co = operatorJwt.get("company_id");
		if (co == null) {
			throw new ForbiddenException("未激活");
		}
		long companyId = toLong(co);
		if (companyId <= 0) {
			throw new ForbiddenException("未激活");
		}
		return companyId;
	}

	private static long toLong(Object co) {
		if (co instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(co.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static long readEnterpriseId(Map<String, Object> merged) {
		Object v = merged.get("enterprise_id");
		if (v == null) {
			throw new BadRequestException("企业ID不能为空");
		}
		if (!(v instanceof String) && !(v instanceof Number)) {
			throw new BadRequestException("企业ID不能为空");
		}
		if (v instanceof String s) {
			String t = s.trim();
			if (t.isEmpty()) {
				throw new BadRequestException("企业ID不能为空");
			}
			try {
				return Long.parseLong(t);
			} catch (NumberFormatException e) {
				throw new BadRequestException("企业ID不能为空");
			}
		}
		return ((Number) v).longValue();
	}

	private static String readValidatedEmail(Map<String, Object> merged) {
		Object value = merged.get("email");
		String s = Objects.toString(value, "").trim();
		if (s.isEmpty()) {
			throw new BadRequestException("收件邮箱格式不正确");
		}
		try {
			InternetAddress addr = new InternetAddress(s);
			addr.validate();
			return s;
		} catch (AddressException e) {
			throw new BadRequestException("收件邮箱格式不正确");
		}
	}
}
