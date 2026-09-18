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

package cn.shopex.ecshopx.orders.service.invoice;

import cn.shopex.ecshopx.companys.service.setting.MailSettingRedisService;
import jakarta.mail.MessagingException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

@Service
public class SendInvoiceEmailJobService {

	private static final Logger log = LoggerFactory.getLogger(SendInvoiceEmailJobService.class);

	private static final String DEFAULT_SUBJECT = "您的电子发票已生成";

	private final MailSettingRedisService mailSettingRedisService;

	public SendInvoiceEmailJobService(MailSettingRedisService mailSettingRedisService) {
		this.mailSettingRedisService = mailSettingRedisService;
	}

	public void execute(String toEmail, String invoiceFileUrl, long companyId, String subjectOrNull) {
		String subject =
				StringUtils.hasText(subjectOrNull) ? subjectOrNull.trim() : DEFAULT_SUBJECT;
		try {
			Map<String, Object> cfg = mailSettingRedisService.readMailSetting(companyId);
			String portStr =
					String.valueOf(cfg.getOrDefault(MailSettingRedisService.EMAIL_SMTP_PORT, "")).trim();
			String host =
					String.valueOf(cfg.getOrDefault(MailSettingRedisService.EMAIL_RELAY_HOST, "")).trim();
			String sender =
					String.valueOf(cfg.getOrDefault(MailSettingRedisService.EMAIL_SENDER, "")).trim();
			String user = String.valueOf(cfg.getOrDefault(MailSettingRedisService.EMAIL_USER, "")).trim();
			String password =
					String.valueOf(cfg.getOrDefault(MailSettingRedisService.EMAIL_PASSWORD, ""));

			if (host.isEmpty()) {
				log.warn("发送发票邮件失败: SMTP 主机未配置 companyId={}", companyId);
				return;
			}
			int port;
			try {
				port = Integer.parseInt(portStr.isEmpty() ? "0" : portStr);
			} catch (NumberFormatException e) {
				log.warn("发送发票邮件失败: SMTP 端口无效 companyId={} port={}", companyId, portStr, e);
				return;
			}
			if (port <= 0) {
				log.warn("发送发票邮件失败: SMTP 端口无效 companyId={} port={}", companyId, port);
				return;
			}

			String safeUrl = HtmlUtils.htmlEscape(invoiceFileUrl);
			String html = "<p>请点击下载：<a href=\"" + safeUrl + "\">发票文件</a></p>";

			JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
			mailSender.setHost(host.replaceFirst("(?i)^(ssl|smtps|smtp)://", ""));
			mailSender.setPort(port);
			mailSender.setUsername(user);
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
			helper.setFrom(sender);
			helper.setSubject(subject);
			helper.setText(html, true);
			mailSender.send(message);
		} catch (MailException | MessagingException e) {
			log.error("发送发票邮件失败", e);
		} catch (Exception e) {
			log.error("发送发票邮件失败", e);
		}
	}
}
