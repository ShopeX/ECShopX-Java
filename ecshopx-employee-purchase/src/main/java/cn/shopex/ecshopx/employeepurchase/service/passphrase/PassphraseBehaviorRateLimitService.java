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

package cn.shopex.ecshopx.employeepurchase.service.passphrase;

import cn.shopex.ecshopx.common.exception.TooManyRequestsException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * ECX-10917：未认证行为上报限流（防刷库）。键维度
 * company+activity+enterprise+主体（userId &gt; visitorKey &gt; IP 兜底），窗口内超限抛 429。
 */
@Service
public class PassphraseBehaviorRateLimitService {

	/** 窗口内最大上报次数；调参入口。 */
	static final long LIMIT_MAX = 10L;

	/** 窗口长度（秒）；调参入口。 */
	static final long WINDOW_SECONDS = 60L;

	private final StringRedisTemplate stringRedisTemplate;

	public PassphraseBehaviorRateLimitService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	public void assertAllowed(
			long companyId,
			long activityId,
			long enterpriseId,
			Long userId,
			String visitorKey,
			String clientIp) {
		String subject;
		if (userId != null && userId > 0L) {
			subject = "u" + userId;
		} else if (StringUtils.hasText(visitorKey)) {
			subject = "v" + visitorKey.trim();
		} else {
			subject = "ip" + (StringUtils.hasText(clientIp) ? clientIp.trim() : "unknown");
		}
		String key = "ep_behavior_rl:" + companyId + ":" + activityId + ":" + enterpriseId + ":" + subject;
		Long n = stringRedisTemplate.opsForValue().increment(key);
		if (n != null && n == 1L) {
			stringRedisTemplate.expire(key, Duration.ofSeconds(WINDOW_SECONDS));
		}
		if (n != null && n > LIMIT_MAX) {
			throw new TooManyRequestsException("操作过于频繁，请稍后再试");
		}
	}
}
