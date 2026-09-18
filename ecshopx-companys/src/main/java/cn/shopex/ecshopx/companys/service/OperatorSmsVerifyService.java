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

package cn.shopex.ecshopx.companys.service;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class OperatorSmsVerifyService {

	private static final String OPERATOR_SMS_VERIFY_REDIS_KEY_TEMPLATE = "operator-sms:verify:%s:%s";

	private static final String ADMIN_FORGET_REDIS_KEY_PREFIX = "admin-forget:";

	private static final int FORGET_SMS_TTL_SECONDS = 300;

	private final StringRedisTemplate companysRedisTemplate;

	public OperatorSmsVerifyService(@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.companysRedisTemplate = companysRedisTemplate;
	}

	public void storeVerifyCode(String type, String mobile, String code) {
		String key = String.format(OPERATOR_SMS_VERIFY_REDIS_KEY_TEMPLATE, type, mobile);
		companysRedisTemplate.opsForValue().set(key, code, Duration.ofSeconds(180));
	}

	public boolean checkVerifyCode(String mobile, String scene, String vcode) {
		String key = String.format(OPERATOR_SMS_VERIFY_REDIS_KEY_TEMPLATE, scene, mobile);
		String redisValue = companysRedisTemplate.opsForValue().get(key);
		return Objects.equals(redisValue, vcode);
	}

	public void storeForgetVerificationCode(String mobile, String code) {
		if (mobile == null) {
			return;
		}
		String m = mobile.trim();
		if (m.isEmpty()) {
			return;
		}
		companysRedisTemplate
				.opsForValue()
				.set(ADMIN_FORGET_REDIS_KEY_PREFIX + m, code, Duration.ofSeconds(FORGET_SMS_TTL_SECONDS));
	}

	public long getForgetSmsCodeTtlSeconds(String mobile) {
		if (mobile == null) {
			return -2L;
		}
		String m = mobile.trim();
		if (m.isEmpty()) {
			return -2L;
		}
		Long ttl = companysRedisTemplate.getExpire(ADMIN_FORGET_REDIS_KEY_PREFIX + m, TimeUnit.SECONDS);
		if (ttl == null) {
			return -2L;
		}
		return ttl;
	}

	public boolean verifyForgetCodeAndDelete(String mobile, String vcode) {
		if (mobile == null || mobile.isBlank()) {
			return false;
		}
		String m = mobile.trim();
		String key = ADMIN_FORGET_REDIS_KEY_PREFIX + m;
		String redisValue = companysRedisTemplate.opsForValue().get(key);
		if (Objects.equals(redisValue, vcode)) {
			companysRedisTemplate.delete(key);
			return true;
		}
		return false;
	}
}
