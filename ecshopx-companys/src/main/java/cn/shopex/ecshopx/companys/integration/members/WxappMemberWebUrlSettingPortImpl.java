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

package cn.shopex.ecshopx.companys.integration.members;

import cn.shopex.ecshopx.common.members.port.WxappMemberWebUrlSettingPort;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service("wxappMemberWebUrlSettingPortImpl")
public class WxappMemberWebUrlSettingPortImpl implements WxappMemberWebUrlSettingPort {

	private static final String KEY_PREFIX = "webUrlSetting:";

	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;

	public WxappMemberWebUrlSettingPortImpl(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
	}

	@Override
	public void applyClasshourArranged(long companyId, Map<String, Object> result) {
		String raw = companysRedisTemplate.opsForValue().get(KEY_PREFIX + companyId);
		if (!StringUtils.hasText(raw)) {
			return;
		}
		try {
			Map<String, Object> inputData = objectMapper.readValue(raw.trim(), new TypeReference<Map<String, Object>>() {});
			if (inputData == null || inputData.isEmpty()) {
				return;
			}
			LinkedHashMap<String, Object> weburl = new LinkedHashMap<>();
			weburl.put("classhour", String.valueOf(inputData.getOrDefault("classhour", "")));
			weburl.put("arranged", String.valueOf(inputData.getOrDefault("arranged", "")));
			result.put("weburl", weburl);
		} catch (Exception ignored) {
			// 保持与其它 Redis JSON 读取一致：解析失败则跳过
		}
	}
}
