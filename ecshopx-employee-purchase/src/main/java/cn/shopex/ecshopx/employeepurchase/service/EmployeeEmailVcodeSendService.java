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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.employeepurchase.mapper.EmployeeFrontCheckQueryMapper;
import cn.shopex.ecshopx.employeepurchase.mapper.dto.EmployeeEmailVcodeEnterpriseRow;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmployeeEmailVcodeSendService {

	/**
	 * 整串匹配的业务邮箱正则（与站点既有宽松规则一致），大小写不敏感。
	 */
	private static final Pattern LEGACY_EMAIL_PATTERN = Pattern.compile(
			"^([a-z0-9]*[-_.]?[a-z0-9]+)*@([a-z0-9]*[-_]?[a-z0-9]+)+\\.[a-z]{2,3}(\\.[a-z]{2})?$",
			Pattern.CASE_INSENSITIVE);

	private final EmployeeFrontCheckQueryMapper employeeFrontCheckQueryMapper;
	private final EmployeePurchaseEmailVcodeRedisService emailVcodeRedisService;
	private final EmployeeEmailVcodeMailGateway mailGateway;

	public EmployeeEmailVcodeSendService(
			EmployeeFrontCheckQueryMapper employeeFrontCheckQueryMapper,
			EmployeePurchaseEmailVcodeRedisService emailVcodeRedisService,
			EmployeeEmailVcodeMailGateway mailGateway) {
		this.employeeFrontCheckQueryMapper = employeeFrontCheckQueryMapper;
		this.emailVcodeRedisService = emailVcodeRedisService;
		this.mailGateway = mailGateway;
	}

	public boolean sendEmailVcode(
			long companyId, String email, int distributorId, long enterpriseId, String clientIp) {
		String trimmedEmail = email == null ? "" : email.trim();
		if (!StringUtils.hasText(trimmedEmail)) {
			throw new ResourceException("收件邮箱格式不正确");
		}
		long atCount = trimmedEmail.chars().filter(ch -> ch == '@').count();
		if (atCount != 1) {
			throw new ResourceException("收件邮箱格式不正确");
		}
		if (!LEGACY_EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
			throw new ResourceException("收件邮箱格式不正确");
		}

		int at = trimmedEmail.indexOf('@');
		String suffix = trimmedEmail.substring(at);

		Integer distParam = distributorId > 0 ? distributorId : null;
		Long entParam = enterpriseId > 0 ? enterpriseId : null;

		EmployeeEmailVcodeEnterpriseRow row =
				employeeFrontCheckQueryMapper.selectFirstEnterpriseForEmailVcode(companyId, suffix, distParam, entParam);
		if (row == null) {
			throw new ResourceException("企业不存在");
		}

		String authType = row.getAuthType();
		if (authType == null || !StringUtils.hasText(authType.trim())) {
			throw new ResourceException("请选择其他验证方式");
		}
		if (!"email".equals(authType.trim())) {
			throw new ResourceException("请选择其他验证方式");
		}

		String relayHost = row.getRelayHost();
		String smtpPort = row.getSmtpPort();
		String emailUser = row.getEmailUser();
		String emailPassword = row.getEmailPassword();
		if (!StringUtils.hasText(relayHost)
				|| !StringUtils.hasText(smtpPort)
				|| !StringUtils.hasText(emailUser)
				|| !StringUtils.hasText(emailPassword)) {
			throw new ResourceException("企业发件箱配置错误");
		}

		try {
			Integer.parseInt(smtpPort.trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("企业发件箱配置错误");
		}

		String limitEmail = trimmedEmail.toLowerCase(Locale.ROOT);
		if (emailVcodeRedisService.hasSendCooldown(companyId, limitEmail)) {
			throw new ResourceException("发送过于频繁，请稍后再试");
		}
		if (emailVcodeRedisService.nextDailyEmailSendCount(companyId, limitEmail) > 5) {
			throw new ResourceException("该邮箱今日发送次数已达上限");
		}
		String limitIp = clientIp == null ? "" : clientIp.trim();
		if (StringUtils.hasText(limitIp)
				&& emailVcodeRedisService.nextDailyIpSendCount(companyId, limitIp) > 200) {
			throw new ResourceException("请求过于频繁，请稍后再试");
		}

		emailVcodeRedisService.rememberSendCooldown(companyId, limitEmail);
		String vcode = String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 1000000));
		emailVcodeRedisService.storeVcodeWithTtl(trimmedEmail, vcode, 1800L);

		String body =
				"<p>尊敬的用户:</p>\n<p style=\"text-indent: 2em;\">您的验证码是:"
						+ vcode
						+ "位数字，30分钟内有效，请尽快完成验证。</p>\n";
		return mailGateway.send(row, trimmedEmail, body);
	}
}
