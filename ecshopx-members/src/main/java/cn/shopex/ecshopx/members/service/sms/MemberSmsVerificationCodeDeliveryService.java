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

package cn.shopex.ecshopx.members.service.sms;

import cn.shopex.ecshopx.companys.service.operator.sms.CompanySmsSendPort;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class MemberSmsVerificationCodeDeliveryService {

	private static final Logger log = LoggerFactory.getLogger(MemberSmsVerificationCodeDeliveryService.class);

	private static final ObjectMapper JSON = new ObjectMapper();

	private static final int SMS_VCODE_TTL_SECONDS = 300;

	private final StringRedisTemplate membersRedis;
	private final CompanySmsSendPort companySmsSendPort;
	private final int smsSendLimitPerDay;

	public MemberSmsVerificationCodeDeliveryService(
			@Qualifier("membersStringRedisTemplate") StringRedisTemplate membersRedis,
			CompanySmsSendPort companySmsSendPort,
			@Value("${common.sms-send-limit:5}") int smsSendLimitPerDay) {
		this.membersRedis = membersRedis;
		this.companySmsSendPort = companySmsSendPort;
		this.smsSendLimitPerDay = smsSendLimitPerDay;
	}

	public void deliver(long companyId, String mobilePlain, String type) {
		String ymd =
				LocalDate.now(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.BASIC_ISO_DATE);
		String countKey = "yzmsend:" + companyId + ":" + ymd + ":" + type + ":" + mobilePlain;
		Long c = membersRedis.opsForValue().increment(countKey);
		if (c != null && c == 1L) {
			membersRedis.expire(countKey, Duration.ofHours(24));
		}
		if (c != null && c > smsSendLimitPerDay) {
			throw new ResourceException("验证码发送过多");
		}

		String vcode = String.valueOf(ThreadLocalRandom.current().nextInt(100_000, 1_000_000));
		Map<String, Object> codeLog = new LinkedHashMap<>();
		codeLog.put("phone", mobilePlain);
		codeLog.put("company", companyId);
		codeLog.put("vcode", vcode);
		log.info("code :{}", jsonOrEmpty(codeLog));

		String smsKey = "member-" + type + ":company" + companyId + ":" + mobilePlain;
		membersRedis.opsForValue().set(smsKey, vcode, Duration.ofSeconds(SMS_VCODE_TTL_SECONDS));

		Map<String, Object> storeLog = new LinkedHashMap<>();
		storeLog.put("key", smsKey);
		storeLog.put("value", vcode);
		storeLog.put("expire", SMS_VCODE_TTL_SECONDS);
		log.info("member redis store :{}", jsonOrEmpty(storeLog));

		boolean sent = companySmsSendPort.sendVerificationCodeForScene(companyId, mobilePlain, vcode, "verification_code");
		if (!sent) {
			throw new ResourceException("短信发送失败");
		}
	}

	private static String jsonOrEmpty(Map<String, Object> m) {
		try {
			return JSON.writeValueAsString(m);
		} catch (Exception e) {
			return "{}";
		}
	}
}
