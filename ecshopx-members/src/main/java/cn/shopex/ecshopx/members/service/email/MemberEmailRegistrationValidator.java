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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.service.setting.MailSettingRedisService;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 邮箱注册前的邮件配置就绪校验，对齐 PHP {@code MailSettingEmailRegistrationValidator}：
 * SMTP 端口/主机/发件人/用户/密码 + H5 激活域名均须配置，且发件人须为合法邮箱。
 */
@Service
public class MemberEmailRegistrationValidator {

	private final MailSettingRedisService mailSettingRedisService;
	private final MailSettingActivationBaseUrlResolver mailSettingActivationBaseUrlResolver;

	public MemberEmailRegistrationValidator(
			MailSettingRedisService mailSettingRedisService,
			MailSettingActivationBaseUrlResolver mailSettingActivationBaseUrlResolver) {
		this.mailSettingRedisService = mailSettingRedisService;
		this.mailSettingActivationBaseUrlResolver = mailSettingActivationBaseUrlResolver;
	}

	public void assertReadyForMemberEmailRegistration(long companyId) {
		Map<String, Object> cfg = mailSettingRedisService.readMailSetting(companyId);
		String port = trimToEmpty(cfg.get(MailSettingRedisService.EMAIL_SMTP_PORT));
		String host = trimToEmpty(cfg.get(MailSettingRedisService.EMAIL_RELAY_HOST));
		String sender = trimToEmpty(cfg.get(MailSettingRedisService.EMAIL_SENDER));
		String user = trimToEmpty(cfg.get(MailSettingRedisService.EMAIL_USER));
		String password = String.valueOf(
				cfg.getOrDefault(MailSettingRedisService.EMAIL_PASSWORD, ""));
		String h5Domain = mailSettingActivationBaseUrlResolver.getH5ActivationBaseUrl(companyId);

		if (port.isEmpty()
				|| host.isEmpty()
				|| sender.isEmpty()
				|| user.isEmpty()
				|| password.isEmpty()
				|| h5Domain.isEmpty()) {
			throw new ResourceException("邮件激活配置缺失");
		}
		if (!MemberEmailVerificationService.isValidEmail(sender)) {
			throw new ResourceException("邮件激活配置缺失");
		}
	}

	private static String trimToEmpty(Object value) {
		return value == null ? "" : String.valueOf(value).trim();
	}
}
