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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class DistributorWechatRebindRedisService {

	private static final String BIND_WECHAT_REDIS_KEY_PREFIX = "bind:wechat:";

	private final StringRedisTemplate sharedStringRedisTemplate;

	public DistributorWechatRebindRedisService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate) {
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
	}

	private static String buildKey(long companyId, String appId, String openid, String unionid) {
		return BIND_WECHAT_REDIS_KEY_PREFIX + companyId + ":" + appId + ":" + openid + ":" + unionid;
	}

	public boolean checkReBindMobile(long companyId, String appId, String openid, String unionid, String checkToken) {
		String key = buildKey(companyId, appId, openid, unionid);
		String redisValue = sharedStringRedisTemplate.opsForValue().get(key);
		return Objects.equals(redisValue, checkToken);
	}

	public void delReBindKey(long companyId, String appId, String openid, String unionid) {
		String key = buildKey(companyId, appId, openid, unionid);
		sharedStringRedisTemplate.delete(key);
	}

	public void setCheckToken(long companyId, String appId, String openid, String unionid, String checkToken) {
		String key = buildKey(companyId, appId, openid, unionid);
		sharedStringRedisTemplate.opsForValue().set(key, checkToken, Duration.ofSeconds(300));
	}
}
