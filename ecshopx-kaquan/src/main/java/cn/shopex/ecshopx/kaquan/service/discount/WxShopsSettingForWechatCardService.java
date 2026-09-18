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

package cn.shopex.ecshopx.kaquan.service.discount;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 读取门店微信展示配置（logo、商户名等），用于组装卡券 base_info。
 */
@Service
public class WxShopsSettingForWechatCardService {

	private static final ObjectMapper OM = new ObjectMapper();

	private final StringRedisTemplate companysRedisTemplate;

	public WxShopsSettingForWechatCardService(@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate) {
		this.companysRedisTemplate = companysRedisTemplate;
	}

	/**
	 * Redis 未命中时返回空 Map（与线上可观察行为一致）。
	 */
	public Map<String, Object> load(long companyId) {
		String key = "wxShopsSetting:" + sha1Hex(String.valueOf(companyId));
		String raw = companysRedisTemplate.opsForValue().get(key);
		if (!StringUtils.hasText(raw)) {
			return Collections.emptyMap();
		}
		try {
			return OM.readValue(raw, new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			return Collections.emptyMap();
		}
	}

	private static String sha1Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] d = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(d.length * 2);
			for (byte b : d) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (Exception e) {
			throw new IllegalStateException("SHA-1 not available", e);
		}
	}
}
