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

package cn.shopex.ecshopx.workwechat.service;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class DistributorWorkWechatFacade {

	private final StringRedisTemplate sharedStringRedisTemplate;
	private final WorkWechatConfigService workWechatConfigService;

	public DistributorWorkWechatFacade(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			WorkWechatConfigService workWechatConfigService) {
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.workWechatConfigService = workWechatConfigService;
	}

	public Map<String, Object> getWorkConfig(Long companyId, String tag) {
		if (companyId == null) {
			throw new BadRequestException("缺少参数，登录失败");
		}
		Map<String, Object> config = workWechatConfigService.loadParsedWorkWechatConfig(companyId);
		if ("dianwu".equals(tag)) {
			workWechatConfigService.validateDianwuForJsSdk(config);
		}
		return config;
	}

	private static String bindKey(long companyId, String workUserid) {
		return "bind:workwechat:" + companyId + ":" + workUserid;
	}

	public void setReBindMobileEncrypt(long companyId, String workUserid, String encrypt) {
		String key = bindKey(companyId, workUserid);
		sharedStringRedisTemplate.opsForValue().set(key, encrypt, Duration.ofSeconds(300));
	}

	public boolean checkReBindMobile(long companyId, String workUserid, String encrypt) {
		String key = bindKey(companyId, workUserid);
		String redisValue = sharedStringRedisTemplate.opsForValue().get(key);
		return Objects.equals(redisValue, encrypt);
	}

	public void delReBindKey(long companyId, String workUserid) {
		sharedStringRedisTemplate.delete(bindKey(companyId, workUserid));
	}
}
