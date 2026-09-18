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
import cn.shopex.ecshopx.wechat.wxjava.WxJavaMpRuntime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class OfficialAccountDistributorJsSdkService {

	private static final String MODE_AUTHORIZED = "authorized";
	private static final String MODE_DIRECT = "direct";

	private static final String ALPHANUM =
			"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	private static final int JSAPI_TICKET_TTL_SECONDS = 7000;
	private static final String CACHE_KEY_PREFIX = "wechat:mp:jssdk:jsapi_ticket:";

	private static final List<String> WXAPP_ORDER_JS_PAY_JS_API_LIST = List.of(
			"chooseImage",
			"previewImage",
			"checkJsApi",
			"scanQRCode",
			"hideOptionMenu",
			"showOptionMenu",
			"hideMenuItems",
			"showMenuItems",
			"hideAllNonBaseMenuItem",
			"showAllNonBaseMenuItem");

	private final OpenPlatformWoaFacade openPlatformWoaFacade;
	private final WxJavaMpRuntime wxJavaMpRuntime;
	private final StringRedisTemplate sharedStringRedisTemplate;
	private final SecureRandom secureRandom = new SecureRandom();

	public OfficialAccountDistributorJsSdkService(
			OpenPlatformWoaFacade openPlatformWoaFacade,
			WxJavaMpRuntime wxJavaMpRuntime,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate) {
		this.openPlatformWoaFacade = openPlatformWoaFacade;
		this.wxJavaMpRuntime = wxJavaMpRuntime;
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
	}

	public Map<String, Object> buildConfig(long companyId, String pageUrl) {
		Map<String, Object> woa = openPlatformWoaFacade.getWoaApp(companyId, "weixin", "touch");
		Object modeObj = woa.get("mode");
		String mode = modeObj != null ? String.valueOf(modeObj) : "";

		WxMpService mp;
		String appIdForSdk;
		if (MODE_AUTHORIZED.equals(mode)) {
			Object aid = woa.get("authorizerAppid");
			String authorizerAppid = aid != null ? String.valueOf(aid).trim() : "";
			if (!StringUtils.hasText(authorizerAppid)) {
				throw new ResourceException("公众号信息有误！");
			}
			appIdForSdk = authorizerAppid;
			mp = wxJavaMpRuntime.mp(authorizerAppid);
		} else if (MODE_DIRECT.equals(mode)) {
			String appId = stringField(woa, "app_id");
			String secret = stringField(woa, "secret");
			if (!StringUtils.hasText(appId) || !StringUtils.hasText(secret)) {
				throw new ResourceException("公众号信息有误！");
			}
			appIdForSdk = appId.trim();
			mp = wxJavaMpRuntime.mpDirect(appId.trim(), secret.trim());
		} else {
			throw new ResourceException("公众号信息有误！");
		}

		String ticket = getOrFetchMpJsapiTicket(appIdForSdk, mp);

		String nonceStr = randomAlphanumeric(16);
		long timestamp = System.currentTimeMillis() / 1000;
		String plain = "jsapi_ticket="
				+ ticket
				+ "&noncestr="
				+ nonceStr
				+ "&timestamp="
				+ timestamp
				+ "&url="
				+ pageUrl;
		String signature = sha1HexUtf8(plain);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("beta", Boolean.FALSE);
		out.put("debug", Boolean.FALSE);
		List<String> jsApis = new ArrayList<>(List.of("scanQRCode"));
		out.put("jsApiList", jsApis);
		out.put("appId", appIdForSdk);
		out.put("nonceStr", nonceStr);
		out.put("timestamp", timestamp);
		out.put("url", pageUrl);
		out.put("signature", signature);
		return out;
	}

	public Map<String, Object> buildWxappOrderJsPayConfig(long companyId, String pageUrl) {
		Map<String, Object> woa = openPlatformWoaFacade.getWoaAppForWxappOrderJsPay(companyId);
		Object aid = woa.get("authorizerAppid");
		String authorizerAppid = aid != null ? String.valueOf(aid).trim() : "";
		if (!StringUtils.hasText(authorizerAppid)) {
			throw new BadRequestException("当前账号未绑定公众号或小程序，请先授权绑定");
		}
		WxMpService mp = wxJavaMpRuntime.mp(authorizerAppid);
		String ticket = getOrFetchMpJsapiTicket(authorizerAppid, mp);

		String nonceStr = randomAlphanumeric(16);
		long timestamp = System.currentTimeMillis() / 1000;
		String plain = "jsapi_ticket="
				+ ticket
				+ "&noncestr="
				+ nonceStr
				+ "&timestamp="
				+ timestamp
				+ "&url="
				+ pageUrl;
		String signature = sha1HexUtf8(plain);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("beta", Boolean.FALSE);
		out.put("debug", Boolean.FALSE);
		out.put("jsApiList", new ArrayList<>(WXAPP_ORDER_JS_PAY_JS_API_LIST));
		out.put("appId", authorizerAppid);
		out.put("nonceStr", nonceStr);
		out.put("timestamp", timestamp);
		out.put("url", pageUrl);
		out.put("signature", signature);
		return out;
	}

	private static String stringField(Map<String, Object> woa, String key) {
		Object v = woa.get(key);
		return v != null ? String.valueOf(v) : "";
	}

	private String getOrFetchMpJsapiTicket(String appIdForCache, WxMpService mp) {
		String cacheKey = CACHE_KEY_PREFIX + sha1HexUtf8(appIdForCache);
		String cached = sharedStringRedisTemplate.opsForValue().get(cacheKey);
		if (StringUtils.hasText(cached)) {
			return cached;
		}
		String ticket = fetchMpJsapiTicket(mp);
		sharedStringRedisTemplate
				.opsForValue()
				.set(cacheKey, ticket, Duration.ofSeconds(JSAPI_TICKET_TTL_SECONDS));
		return ticket;
	}

	private String fetchMpJsapiTicket(WxMpService mp) {
		try {
			String ticket = mp.getJsapiTicket();
			if (!StringUtils.hasText(ticket)) {
				throw new ResourceException("微信接口调用失败");
			}
			return ticket.trim();
		} catch (WxErrorException e) {
			log.warn("wechat mp getticket errcode={} msg={}", e.getError() != null ? e.getError().getErrorCode() : null, e.getMessage());
			throw new ResourceException("微信接口调用失败");
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.warn("wechat mp getticket request failed", e);
			throw new ResourceException("微信接口调用失败");
		}
	}

	private String randomAlphanumeric(int len) {
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			sb.append(ALPHANUM.charAt(secureRandom.nextInt(ALPHANUM.length())));
		}
		return sb.toString();
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
