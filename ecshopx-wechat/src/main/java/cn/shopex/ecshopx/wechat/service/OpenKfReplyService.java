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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.service.openplatform.WechatOfficialAccountKfAccountClient;
import cn.shopex.ecshopx.wechat.wxa.WechatOpenPlatformAuthorizerTokenService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenKfReplyService {

	private static final String REDIS_KEY_PREFIX = "transfer:";
	private static final String SHA1_INPUT_SUFFIX = "set_open_kf_reply";

	private final WechatOpenPlatformAuthorizerTokenService wechatOpenPlatformAuthorizerTokenService;
	private final WechatOfficialAccountKfAccountClient wechatOfficialAccountKfAccountClient;
	private final StringRedisTemplate sharedStringRedisTemplate;

	public OpenKfReplyService(
			WechatOpenPlatformAuthorizerTokenService wechatOpenPlatformAuthorizerTokenService,
			WechatOfficialAccountKfAccountClient wechatOfficialAccountKfAccountClient,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate) {
		this.wechatOpenPlatformAuthorizerTokenService = wechatOpenPlatformAuthorizerTokenService;
		this.wechatOfficialAccountKfAccountClient = wechatOfficialAccountKfAccountClient;
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
	}

	public boolean getOpenKfReply(String authorizerAppId) {
		String redisKey = openKfReplyRedisKey(authorizerAppId);
		String value = sharedStringRedisTemplate.opsForValue().get(redisKey);
		return "true".equals(value);
	}

	public void setOpenKfReply(String authorizerAppId, Object rawIsOpenKfReply) {
		String appId = authorizerAppId == null ? "" : authorizerAppId;
		if (!StringUtils.hasText(appId)) {
			throw new BadRequestException("当前账号未绑定公众号或小程序，请先授权绑定", 400);
		}
		String accessToken = wechatOpenPlatformAuthorizerTokenService.getAuthorizerAccessToken(appId);
		List<Map<String, Object>> kfList = wechatOfficialAccountKfAccountClient.listKfWithOnlineFlags(accessToken);
		if (kfList.isEmpty() && isLooseOpenTrue(rawIsOpenKfReply)) {
			throw new ResourceException("不存在客服人员，请先添加客服后再开启");
		}
		String redisKey = openKfReplyRedisKey(authorizerAppId);
		String value = toRedisStoredString(rawIsOpenKfReply);
		sharedStringRedisTemplate.opsForValue().set(redisKey, value);
	}

	private String openKfReplyRedisKey(String authorizerAppId) {
		String appIdForKey = authorizerAppId == null ? "" : authorizerAppId;
		return REDIS_KEY_PREFIX + sha1HexUtf8(appIdForKey + SHA1_INPUT_SUFFIX);
	}

	private static boolean isLooseOpenTrue(Object raw) {
		if (Boolean.TRUE.equals(raw)) {
			return true;
		}
		return raw instanceof String s && "true".equals(s);
	}

	private static String toRedisStoredString(Object rawIsOpenKfReply) {
		if (rawIsOpenKfReply == null) {
			return "false";
		}
		if (rawIsOpenKfReply instanceof Boolean b) {
			return b ? "true" : "false";
		}
		return String.valueOf(rawIsOpenKfReply);
	}

	private static String sha1HexUtf8(String input) {
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
