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

import cn.shopex.ecshopx.common.exception.ResourceException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class PassphraseParticipateQuotaRedisService {

	private static final DefaultRedisScript<Long> TRY_CONSUME_SCRIPT = new DefaultRedisScript<>(
			"""
			local v = redis.call('GET', KEYS[1])
			if not v then return -1 end
			local n = tonumber(v)
			if n == nil or n <= 0 then return 0 end
			redis.call('DECR', KEYS[1])
			return 1
			""",
			Long.class);

	private final StringRedisTemplate stringRedisTemplate;

	public PassphraseParticipateQuotaRedisService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	public static String buildKey(long companyId, long activityId, long enterpriseId) {
		return "ep:pquota:" + companyId + ":" + activityId + ":" + enterpriseId;
	}

	public void syncRemainingQuota(long companyId, long activityId, long enterpriseId, int participateQuota) {
		String key = buildKey(companyId, activityId, enterpriseId);
		stringRedisTemplate.opsForValue().set(key, String.valueOf(Math.max(participateQuota, 0)));
	}

	public void removeKey(long companyId, long activityId, long enterpriseId) {
		stringRedisTemplate.delete(buildKey(companyId, activityId, enterpriseId));
	}

	public int readRemainingOrThrow(long companyId, long activityId, long enterpriseId) {
		String key = buildKey(companyId, activityId, enterpriseId);
		String v = stringRedisTemplate.opsForValue().get(key);
		if (v == null) {
			throw new ResourceException("口令企业配置不存在");
		}
		try {
			return Integer.parseInt(v.trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("口令企业配置不存在");
		}
	}

	/** @return true 扣减成功；false 名额已满；-1 key 不存在 */
	public TryConsumeResult tryConsumeSlot(long companyId, long activityId, long enterpriseId) {
		String key = buildKey(companyId, activityId, enterpriseId);
		Long result = stringRedisTemplate.execute(TRY_CONSUME_SCRIPT, java.util.List.of(key));
		if (result == null || result == -1L) {
			return TryConsumeResult.NOT_CONFIGURED;
		}
		if (result == 0L) {
			return TryConsumeResult.QUOTA_EMPTY;
		}
		return TryConsumeResult.SUCCESS;
	}

	public void releaseOneSlot(long companyId, long activityId, long enterpriseId) {
		String key = buildKey(companyId, activityId, enterpriseId);
		stringRedisTemplate.opsForValue().increment(key);
	}

	public enum TryConsumeResult {
		SUCCESS,
		QUOTA_EMPTY,
		NOT_CONFIGURED
	}
}
