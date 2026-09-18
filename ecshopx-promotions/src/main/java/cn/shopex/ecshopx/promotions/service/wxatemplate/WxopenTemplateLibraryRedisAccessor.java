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

package cn.shopex.ecshopx.promotions.service.wxatemplate;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxopenTemplateLibraryRedisAccessor {

	private final StringRedisTemplate stringRedisTemplate;

	public WxopenTemplateLibraryRedisAccessor(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	public void hset(String wxappAppid, String wxaLibraryTemplateId, String scenesName, String priTemplateId) {
		String key = "wxopen_template_library:" + wxappAppid + ":" + wxaLibraryTemplateId;
		stringRedisTemplate.opsForHash().put(key, scenesName, priTemplateId);
	}

	public void hdelIfPresent(String wxappAppid, String wxaLibraryTemplateId, String scenesName) {
		String key = "wxopen_template_library:" + wxappAppid + ":" + wxaLibraryTemplateId;
		stringRedisTemplate.opsForHash().delete(key, scenesName);
	}

	@Nullable
	public String hget(String wxappAppid, String wxaLibraryTemplateId, String scenesName) {
		if (!StringUtils.hasText(wxappAppid)) {
			return null;
		}
		String key = "wxopen_template_library:" + wxappAppid + ":" + wxaLibraryTemplateId;
		Object raw = stringRedisTemplate.opsForHash().get(key, scenesName);
		return raw != null ? raw.toString() : null;
	}
}
