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

package cn.shopex.ecshopx.wechat.openapi;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.util.PhpUniqid;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class OpenapiWxappShareIdRedisService {

	private static final int TTL_SECONDS = 2_592_000;

	private final StringRedisTemplate redis;
	private final ObjectMapper objectMapper;

	public OpenapiWxappShareIdRedisService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redis,
			ObjectMapper objectMapper) {
		this.redis = redis;
		this.objectMapper = objectMapper;
	}

	public String getShareId(long companyId, Map<String, Object> sceneParams) {
		String shareId = PhpUniqid.next();
		String redisKey = companyId + "_" + shareId;
		try {
			String json = objectMapper.writeValueAsString(sceneParams);
			redis.opsForValue().set(redisKey, json);
			redis.expireAt(redisKey, Instant.now().plusSeconds(TTL_SECONDS));
		} catch (JsonProcessingException e) {
			throw new ResourceException("参数错误");
		}
		return shareId;
	}
}
