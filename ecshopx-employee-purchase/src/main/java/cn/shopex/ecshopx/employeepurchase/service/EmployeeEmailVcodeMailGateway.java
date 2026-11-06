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

import cn.shopex.ecshopx.employeepurchase.mapper.dto.EmployeeEmailVcodeEnterpriseRow;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class EmployeeEmailVcodeMailGateway {

	private static final Logger log = LoggerFactory.getLogger(EmployeeEmailVcodeMailGateway.class);

	public boolean send(EmployeeEmailVcodeEnterpriseRow row, String to, String htmlBody) {
		int port;
		try {
			port = Integer.parseInt(row.getSmtpPort().trim());
		} catch (NumberFormatException e) {
			return false;
		}
		try {
			JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
			mailSender.setHost(row.getRelayHost().trim().replaceFirst("(?i)^(ssl|smtps|smtp)://", ""));
			mailSender.setPort(port);
			mailSender.setUsername(row.getEmailUser().trim());
			mailSender.setPassword(row.getEmailPassword());
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
			helper.setTo(to);
			helper.setSubject("企业员工验证");
			helper.setText(htmlBody, true);
			helper.setFrom(row.getEmailUser().trim());
			mailSender.send(message);
			return true;
		} catch (MailException | MessagingException e) {
			log.warn("sendEmailVcode: SMTP failed to={}", to, e);
			return false;
		}
	}
}
