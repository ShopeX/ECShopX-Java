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

package cn.shopex.ecshopx.wechat.service;

import cn.shopex.ecshopx.wechat.wxa.WxaUnlimitedQrcodeClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PcLoginQrcodeService {

	private static final String PAGE_AUTH = "others/pages/auth/index";

	private final WxaAuthorizerAppIdByTemplateService wxaAuthorizerAppIdByTemplateService;
	private final WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient;
	private final StringRedisTemplate membersRedis;
	private final ObjectMapper objectMapper;
	private final String pcWxcodeLoginSecondsRaw;

	public PcLoginQrcodeService(
			WxaAuthorizerAppIdByTemplateService wxaAuthorizerAppIdByTemplateService,
			WxaUnlimitedQrcodeClient wxaUnlimitedQrcodeClient,
			@Qualifier("membersStringRedisTemplate") StringRedisTemplate membersRedis,
			ObjectMapper objectMapper,
			@Value("${ecshopx.wechat.pc-wxcode-login-seconds:}") String pcWxcodeLoginSecondsRaw) {
		this.wxaAuthorizerAppIdByTemplateService = wxaAuthorizerAppIdByTemplateService;
		this.wxaUnlimitedQrcodeClient = wxaUnlimitedQrcodeClient;
		this.membersRedis = membersRedis;
		this.objectMapper = objectMapper;
		this.pcWxcodeLoginSecondsRaw = pcWxcodeLoginSecondsRaw == null ? "" : pcWxcodeLoginSecondsRaw;
	}

	public Map<String, Object> getPcLoginQrcode(long companyId) {
		String authorizerAppid = wxaAuthorizerAppIdByTemplateService.requireAuthorizerAppidForPcLogin(companyId);

		long nowSec = Instant.now().getEpochSecond();
		String timesKey = "member:oauth:login:times:" + nowSec;
		Long num = membersRedis.opsForValue().increment(timesKey);
		membersRedis.expire(timesKey, Duration.ofSeconds(60));
		String token = new Hashids(Long.toString(nowSec), 12).encode(Objects.requireNonNull(num).longValue());

		long expEpochSec;
		long ttlSeconds;
		Integer cfg = parseOptionalPositiveSeconds(pcWxcodeLoginSecondsRaw);
		if (cfg != null && cfg > 0) {
			expEpochSec = nowSec + cfg;
			ttlSeconds = cfg;
		} else {
			expEpochSec = nowSec + 120;
			ttlSeconds = 3720;
		}

		String redisKey = "member:oauth:login:" + token;
		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("exp", expEpochSec);
		payload.put("time", nowSec);
		payload.put("status", 0);
		String json;
		try {
			json = objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("登录令牌数据序列化失败", e);
		}
		membersRedis.opsForValue().set(redisKey, json, Duration.ofSeconds(ttlSeconds));

		String scene =
				"cid="
						+ companyId
						+ "&t="
						+ URLEncoder.encode(token, StandardCharsets.UTF_8);

		byte[] raw = wxaUnlimitedQrcodeClient.getUnlimitedCodeBytes(authorizerAppid, scene, PAGE_AUTH);

		String base64 = "data:image/jpg;base64," + Base64.getEncoder().encodeToString(raw);
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("base64Image", base64);
		out.put("access_token", token);
		return out;
	}

	private static Integer parseOptionalPositiveSeconds(String raw) {
		if (!StringUtils.hasText(raw)) {
			return null;
		}
		try {
			int v = Integer.parseInt(raw.trim());
			return v > 0 ? v : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
