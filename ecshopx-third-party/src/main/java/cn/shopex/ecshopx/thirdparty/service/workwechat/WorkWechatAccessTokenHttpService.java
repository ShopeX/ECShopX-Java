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

package cn.shopex.ecshopx.thirdparty.service.workwechat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.impl.WxCpServiceImpl;
import me.chanjar.weixin.cp.config.impl.WxCpDefaultConfigImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service("workWechatAccessTokenHttp")
@ConditionalOnProperty(
		prefix = "ecshopx.thirdparty.work-wechat.corp-app",
		name = "http-enabled",
		havingValue = "true")
@RequiredArgsConstructor
public class WorkWechatAccessTokenHttpService implements WorkWechatAccessTokenProvider {

	private static final Logger log = LoggerFactory.getLogger(WorkWechatAccessTokenHttpService.class);

	private static final String CONFIG_KEY_PREFIX = "workwechat:config:";

	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	@Override
	public Optional<String> getAccessToken(long companyId) {
		if (companyId <= 0L) {
			return Optional.empty();
		}
		String raw = stringRedisTemplate.opsForValue().get(CONFIG_KEY_PREFIX + sha1Hex(String.valueOf(companyId)));
		if (!StringUtils.hasText(raw)) {
			return Optional.empty();
		}
		String corpid;
		String secret;
		try {
			JsonNode root = objectMapper.readTree(raw);
			corpid = root.path("corpid").asText("").trim();
			JsonNode app = root.path("agents").path("app");
			secret = app.path("secret").asText("").trim();
		} catch (Exception e) {
			log.debug("work wechat config parse failed companyId={}", companyId, e);
			return Optional.empty();
		}
		if (!StringUtils.hasText(corpid) || !StringUtils.hasText(secret)) {
			return Optional.empty();
		}
		try {
			WxCpDefaultConfigImpl cfg = new WxCpDefaultConfigImpl();
			cfg.setCorpId(corpid);
			cfg.setCorpSecret(secret);
			WxCpServiceImpl wxCp = new WxCpServiceImpl();
			wxCp.setWxCpConfigStorage(cfg);
			String token = wxCp.getAccessToken(false);
			if (!StringUtils.hasText(token)) {
				return Optional.empty();
			}
			return Optional.of(token.trim());
		} catch (WxErrorException e) {
			log.debug("work wechat gettoken wx error companyId={}", companyId, e);
			return Optional.empty();
		} catch (Exception e) {
			log.debug("work wechat gettoken request failed companyId={}", companyId, e);
			return Optional.empty();
		}
	}

	private static String sha1Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(digest.length * 2);
			for (byte b : digest) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-1 not available", e);
		}
	}
}
