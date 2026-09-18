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

package cn.shopex.ecshopx.wsugc.service.content;

import cn.shopex.ecshopx.wsugc.domain.Setting;
import cn.shopex.ecshopx.wsugc.mapper.SettingMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service
public class UgcWxContentTextCheckService {

	private static final Logger log = LoggerFactory.getLogger(UgcWxContentTextCheckService.class);

	private final SettingMapper settingMapper;
	private final StringRedisTemplate redis;
	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate = new RestTemplate();

	public UgcWxContentTextCheckService(
			SettingMapper settingMapper,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate redis,
			ObjectMapper objectMapper) {
		this.settingMapper = settingMapper;
		this.redis = redis;
		this.objectMapper = objectMapper;
	}

	/**
	 * Text security check outcome for downstream logic: {@code 0} pending or needs review,
	 * {@code 1} pass, {@code 4} risky or violation.
	 */
	public int checkTextStatus(long companyId, String text, String openId) {
		String enableRaw = resolveSetting(companyId, "contentCheck_enable");
		if (!isFeatureEnabled(enableRaw)) {
			return 0;
		}
		try {
			String accessToken = getAccessToken(companyId);
			if (!StringUtils.hasText(accessToken)) {
				log.debug("msgCheck-文本审查结果-access_token获取失败");
				return 0;
			}
			String baseUrl = resolveSetting(companyId, "contentCheck_url").trim();
			if (!StringUtils.hasText(baseUrl)) {
				log.debug("msgCheck-文本审查结果-contentCheck_url 为空");
				return 0;
			}
			String url = baseUrl + "/wxa/msg_sec_check?access_token=" + accessToken;

			Map<String, Object> body = new HashMap<>();
			body.put("openid", openId == null ? "" : openId);
			body.put("scene", 2);
			body.put("version", 2);
			body.put("content", text == null ? "" : text);
			String json = objectMapper.writeValueAsString(body);
			log.debug("msgCheck-文本审查: open_id:{} 提交字节长度:{}", openId, json.getBytes(StandardCharsets.UTF_8).length);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			String res = restTemplate.postForObject(url, new HttpEntity<>(json, headers), String.class);
			JsonNode root = objectMapper.readTree(res != null ? res : "{}");
			String suggest;
			if (root.path("errcode").asInt(-1) == 0 && root.has("result")) {
				suggest = root.path("result").path("suggest").asText("review");
			} else {
				log.debug("msgCheck-文本审查结果-接口返回失败: {}", res);
				suggest = "review";
			}
			if ("pass".equals(suggest)) {
				return 1;
			}
			if ("risky".equals(suggest)) {
				return 4;
			}
			return 0;
		} catch (Exception e) {
			log.debug("msgCheck-文本审查结果-报错了{}", e.getMessage());
			return 0;
		}
	}

	private String getAccessToken(long companyId) {
		String cachedToken = resolveSetting(companyId, "wx.access_token");
		String expRaw = resolveSetting(companyId, "wx.expires_time");
		long now = System.currentTimeMillis() / 1000L;
		long exp = 0L;
		if (StringUtils.hasText(expRaw)) {
			try {
				exp = Long.parseLong(expRaw.trim());
			} catch (NumberFormatException ignored) {
				exp = 0L;
			}
		}
		if (StringUtils.hasText(cachedToken) && exp > now) {
			return cachedToken.trim();
		}

		String appid = resolveSetting(companyId, "contentCheck_appid").trim();
		String secret = resolveSetting(companyId, "contentCheck_appsecret").trim();
		String baseUrl = resolveSetting(companyId, "contentCheck_url").trim();
		if (!StringUtils.hasText(appid) || !StringUtils.hasText(secret) || !StringUtils.hasText(baseUrl)) {
			return "";
		}
		String tokenUrl = baseUrl + "/cgi-bin/token?grant_type=client_credential&appid=" + appid + "&secret=" + secret;
		log.debug("msgCheck-文本审查结果请求token: {}", tokenUrl);
		try {
			String res = restTemplate.getForObject(tokenUrl, String.class);
			JsonNode root = objectMapper.readTree(res != null ? res : "{}");
			if (!root.has("access_token")) {
				return "";
			}
			String token = root.get("access_token").asText("");
			int expiresIn = root.path("expires_in").asInt(0);
			long expiresAt = expiresIn > 0 ? now + expiresIn - 60 : now;
			redis.opsForHash().put("ugc_setting:" + companyId, "wx.access_token", token);
			redis.opsForHash().put("ugc_setting:" + companyId, "wx.expires_time", String.valueOf(expiresAt));
			return token;
		} catch (Exception e) {
			log.debug("msgCheck-token 请求异常: {}", e.getMessage());
			return "";
		}
	}

	private String resolveSetting(long companyId, String keyname) {
		String redisKey = "ugc_setting:" + companyId;
		Object h = redis.opsForHash().get(redisKey, keyname);
		if (h != null) {
			String s = String.valueOf(h);
			if (isFeatureEnabled(s)) {
				return s;
			}
		}
		Setting row = settingMapper.selectOne(new LambdaQueryWrapper<Setting>()
				.eq(Setting::getCompanyId, companyId)
				.eq(Setting::getKeyname, keyname)
				.last("LIMIT 1"));
		return row != null && row.getValue() != null ? row.getValue() : "";
	}

	private static boolean isFeatureEnabled(String raw) {
		if (raw == null) {
			return false;
		}
		String t = raw.trim();
		if (t.isEmpty()) {
			return false;
		}
		if ("0".equals(t)) {
			return false;
		}
		return !"false".equalsIgnoreCase(t);
	}

}
