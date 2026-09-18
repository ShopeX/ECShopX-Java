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

package cn.shopex.ecshopx.members.service.h5.protocol;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class H5RegisterProtocolQueryService {

	private static final String[] TYPES = {"member_register", "privacy"};

	private final StringRedisTemplate stringRedisTemplate;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public H5RegisterProtocolQueryService(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	public List<Map<String, Object>> listRegisterAndPrivacyProtocols(long companyId) {
		String hashKey = "hash:companyProtocol:" + sha1Hex(String.valueOf(companyId));
		List<Map<String, Object>> out = new ArrayList<>();
		for (String type : TYPES) {
			String json = (String) stringRedisTemplate.opsForHash().get(hashKey, type);
			if (json == null || json.isEmpty()) {
				continue;
			}
			try {
				out.add(objectMapper.readValue(json, new TypeReference<>() {}));
			} catch (Exception ignored) {
			}
		}
		return out;
	}

	private static String sha1Hex(String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
