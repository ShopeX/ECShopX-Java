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

package cn.shopex.ecshopx.companys.service.operator;

import cn.shopex.ecshopx.companys.config.OperatorSmsGatewayProperties;
import cn.shopex.ecshopx.companys.service.OperatorSmsVerifyService;
import cn.shopex.ecshopx.companys.service.operator.sms.CompanySmsSendPort;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class OperatorSmsSendOrchestrator {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final StringRedisTemplate companysRedisTemplate;
	private final OperatorSmsGatewayProperties operatorSmsGatewayProperties;
	private final ShopexOperatorSmsHttpClient shopexOperatorSmsHttpClient;
	private final CompanySmsSendPort companySmsSendPort;
	private final OperatorSmsVerifyService operatorSmsVerifyService;

	public OperatorSmsSendOrchestrator(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			OperatorSmsGatewayProperties operatorSmsGatewayProperties,
			ShopexOperatorSmsHttpClient shopexOperatorSmsHttpClient,
			CompanySmsSendPort companySmsSendPort,
			OperatorSmsVerifyService operatorSmsVerifyService) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.operatorSmsGatewayProperties = operatorSmsGatewayProperties;
		this.shopexOperatorSmsHttpClient = shopexOperatorSmsHttpClient;
		this.companySmsSendPort = companySmsSendPort;
		this.operatorSmsVerifyService = operatorSmsVerifyService;
	}

	public boolean sendVerifyCode(long companyId, String mobile, String type) {
		if (!"login".equals(type)) {
			throw new BadRequestException("错误的短信验证类型");
		}

		String sendNumKey = "operator-sms:today-num:" + type + ":" + mobile;
		String lockKey = "operator-sms:lock:" + type + ":" + mobile;

		String sendNumStr = companysRedisTemplate.opsForValue().get(sendNumKey);
		if (sendNumStr != null && !sendNumStr.isEmpty()) {
			try {
				long n = Long.parseLong(sendNumStr.trim());
				if (n >= 5L) {
					throw new ResourceException("验证码发送过于频繁");
				}
			} catch (NumberFormatException e) {
				throw new ResourceException("验证码发送过于频繁");
			}
		}

		Boolean locked = companysRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(60));
		if (Boolean.FALSE.equals(locked)) {
			long ttlSec = companysRedisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
			int n = ttlSec > 0 ? (int) ttlSec : 1;
			n = Math.max(1, n);
			throw new ResourceException("请" + n + "秒后重试发送验证码");
		}

		String verificationCode = String.format("%06d", RANDOM.nextInt(1_000_000));

		boolean sentOk =
				operatorSmsGatewayProperties.hasEntCredentials()
						? shopexOperatorSmsHttpClient.sendMobileCode(mobile, verificationCode)
						: companySmsSendPort.sendVerificationCode(companyId, mobile, verificationCode);

		if (!sentOk) {
			return false;
		}

		if (sendNumStr != null && !sendNumStr.isEmpty()) {
			companysRedisTemplate.opsForValue().increment(sendNumKey);
		} else {
			long ttlEndOfDay = secondsToEndOfLocalDay();
			companysRedisTemplate.opsForValue().set(sendNumKey, "1", Duration.ofSeconds(ttlEndOfDay));
		}

		operatorSmsVerifyService.storeVerifyCode(type, mobile, verificationCode);
		return true;
	}

	private static long secondsToEndOfLocalDay() {
		ZoneId zone = ZoneId.systemDefault();
		ZonedDateTime now = ZonedDateTime.now(zone);
		ZonedDateTime end = LocalDate.now(zone).atTime(23, 59, 59).atZone(zone);
		long sec = Duration.between(now, end).getSeconds();
		return Math.max(1L, sec);
	}
}
