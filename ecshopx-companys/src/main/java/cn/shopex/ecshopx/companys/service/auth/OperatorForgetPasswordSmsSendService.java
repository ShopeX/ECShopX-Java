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

package cn.shopex.ecshopx.companys.service.auth;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.config.OperatorSmsGatewayProperties;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.companys.service.OperatorSmsVerifyService;
import cn.shopex.ecshopx.companys.service.OperatorsImageVcodeService;
import cn.shopex.ecshopx.companys.service.operator.ShopexOperatorSmsHttpClient;
import cn.shopex.ecshopx.companys.service.operator.sms.CompanySmsSendPort;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class OperatorForgetPasswordSmsSendService {

	private static final Pattern MOBILE = Pattern.compile("^1\\d{10}$");

	private final OperatorsImageVcodeService operatorsImageVcodeService;
	private final OperatorsMapper operatorsMapper;
	private final SensitiveFieldEncryptor sensitiveFieldEncryptor;
	private final OperatorSmsVerifyService operatorSmsVerifyService;
	private final StringRedisTemplate companysRedisTemplate;
	private final OperatorSmsGatewayProperties operatorSmsGatewayProperties;
	private final ShopexOperatorSmsHttpClient shopexOperatorSmsHttpClient;
	private final CompanySmsSendPort companySmsSendPort;
	private final SecureRandom secureRandom = new SecureRandom();

	public OperatorForgetPasswordSmsSendService(
			OperatorsImageVcodeService operatorsImageVcodeService,
			OperatorsMapper operatorsMapper,
			SensitiveFieldEncryptor sensitiveFieldEncryptor,
			OperatorSmsVerifyService operatorSmsVerifyService,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			OperatorSmsGatewayProperties operatorSmsGatewayProperties,
			ShopexOperatorSmsHttpClient shopexOperatorSmsHttpClient,
			CompanySmsSendPort companySmsSendPort) {
		this.operatorsImageVcodeService = operatorsImageVcodeService;
		this.operatorsMapper = operatorsMapper;
		this.sensitiveFieldEncryptor = sensitiveFieldEncryptor;
		this.operatorSmsVerifyService = operatorSmsVerifyService;
		this.companysRedisTemplate = companysRedisTemplate;
		this.operatorSmsGatewayProperties = operatorSmsGatewayProperties;
		this.shopexOperatorSmsHttpClient = shopexOperatorSmsHttpClient;
		this.companySmsSendPort = companySmsSendPort;
	}

	public void sendForgetSmsCode(String mobile, String token, String yzm) {
		String m = mobile != null ? mobile.trim() : "";
		if (m.isEmpty() || !MOBILE.matcher(m).matches()) {
			throw new BadRequestException("手机号码错误");
		}

		String tok = token != null ? token.trim() : "";
		if (tok.isEmpty()) {
			throw new BadRequestException("请输入token");
		}
		String imageCode = yzm != null ? yzm.trim() : "";
		if (imageCode.isEmpty()) {
			throw new BadRequestException("请输入vcode");
		}

		if (!operatorsImageVcodeService.checkImageVcode(tok, imageCode, "forget")) {
			throw new ResourceException("验证码错误");
		}

		String enc = sensitiveFieldEncryptor.encrypt(m);
		Map<String, Object> row = operatorsMapper.getOperatorByMobile(enc, "staff");
		if (row == null || row.isEmpty()) {
			throw new ResourceException("没有找到该手机号码");
		}

		long companyId = parsePositiveCompanyId(row.get("company_id"));

		long ttl = operatorSmsVerifyService.getForgetSmsCodeTtlSeconds(m);
		if (ttl > 240L) {
			throw new ResourceException("请" + (ttl - 240L) + "秒后重试发送验证码");
		}

		enforceDailyCap(m);

		int n = secureRandom.nextInt(1_000_000);
		String vcode = String.format("%06d", n);
		String smsBody = buildSmsVcodeBody(vcode);

		operatorSmsVerifyService.storeForgetVerificationCode(m, vcode);

		boolean sent =
				operatorSmsGatewayProperties.hasEntCredentials()
						? shopexOperatorSmsHttpClient.sendMobileCode(m, vcode, smsBody)
						: companySmsSendPort.sendVerificationCode(companyId, m, vcode, smsBody);
		if (!sent) {
			throw new ResourceException("短信发送失败，请稍后重试");
		}
	}

	private static long parsePositiveCompanyId(Object raw) {
		if (raw == null) {
			throw new ResourceException("账号信息异常，请稍后重试");
		}
		long id;
		if (raw instanceof Number num) {
			id = num.longValue();
		} else {
			try {
				id = Long.parseLong(raw.toString().trim());
			} catch (NumberFormatException e) {
				throw new ResourceException("账号信息异常，请稍后重试");
			}
		}
		if (id <= 0L) {
			throw new ResourceException("账号信息异常，请稍后重试");
		}
		return id;
	}

	private void enforceDailyCap(String mobilePlain) {
		String key = "send-sms-day:" + mobilePlain;
		Long count = companysRedisTemplate.opsForValue().increment(key);
		if (count != null && count == 1L) {
			companysRedisTemplate.expire(key, Duration.ofSeconds(secondsToEndOfLocalDay()));
		}
		if (count != null && count > 5L) {
			throw new ResourceException("今日发送短信次数已达5次");
		}
	}

	private static long secondsToEndOfLocalDay() {
		ZoneId zone = ZoneId.systemDefault();
		ZonedDateTime now = ZonedDateTime.now(zone);
		ZonedDateTime end = LocalDate.now(zone).atTime(23, 59, 59).atZone(zone);
		long sec = Duration.between(now, end).getSeconds();
		return Math.max(1L, sec);
	}

	private static String buildSmsVcodeBody(String vcode) {
		return "您的验证码是" + vcode + "，有效期为30分钟";
	}
}
