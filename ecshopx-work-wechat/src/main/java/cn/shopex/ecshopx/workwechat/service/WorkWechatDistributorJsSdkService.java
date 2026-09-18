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

package cn.shopex.ecshopx.workwechat.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.workwechat.wxjava.WorkWechatWxCpRuntime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class WorkWechatDistributorJsSdkService {

	private static final String ALPHANUM =
			"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	private static final int JSAPI_TICKET_TTL_SECONDS = 7000;

	private final WorkWechatConfigService workWechatConfigService;
	private final WorkWechatWxCpRuntime workWechatWxCpRuntime;
	private final StringRedisTemplate sharedStringRedisTemplate;
	private final SecureRandom secureRandom = new SecureRandom();

	public WorkWechatDistributorJsSdkService(
			WorkWechatConfigService workWechatConfigService,
			WorkWechatWxCpRuntime workWechatWxCpRuntime,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate) {
		this.workWechatConfigService = workWechatConfigService;
		this.workWechatWxCpRuntime = workWechatWxCpRuntime;
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
	}

	public Map<String, Object> buildDistributorJsConfig(long companyId, String pageUrl) {
		Map<String, Object> cfg = workWechatConfigService.loadParsedWorkWechatConfig(companyId);
		workWechatConfigService.validateDianwuForJsSdk(cfg);

		String corpid = trim(cfg.get("corpid"));
		@SuppressWarnings("unchecked")
		Map<String, Object> agents = (Map<String, Object>) cfg.get("agents");
		@SuppressWarnings("unchecked")
		Map<String, Object> dianwu = (Map<String, Object>) agents.get("dianwu");
		String corpsecret = trim(dianwu.get("secret"));
		String agentId = trim(dianwu.get("agent_id"));

		String accessToken = fetchAccessToken(corpid, corpsecret);
		String ticket = getOrFetchJsapiTicket(accessToken, corpid, agentId);

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
		out.put("jsApiList", new ArrayList<>());
		out.put("openTagList", new ArrayList<>());
		out.put("appId", corpid);
		out.put("nonceStr", nonceStr);
		out.put("timestamp", timestamp);
		out.put("url", pageUrl);
		out.put("signature", signature);
		return out;
	}

	private static String trim(Object o) {
		if (o == null) {
			return "";
		}
		return String.valueOf(o).trim();
	}

	private String fetchAccessToken(String corpid, String corpsecret) {
		try {
			return workWechatWxCpRuntime.cpDirect(corpid, corpsecret).getAccessToken(false);
		} catch (WxErrorException e) {
			log.warn(
					"work wechat gettoken wx err errcode={} errmsg={}",
					e.getError() != null ? e.getError().getErrorCode() : null,
					e.getError() != null ? e.getError().getErrorMsg() : e.getMessage());
			throw new ResourceException("企业微信接口调用失败");
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.warn("work wechat gettoken request failed", e);
			throw new ResourceException("企业微信接口调用失败");
		}
	}

	private String getOrFetchJsapiTicket(String accessToken, String corpid, String agentId) {
		String cacheKey = "workwechat:jssdk:jsapi_ticket:" + sha1HexUtf8(corpid + "|" + agentId);
		String cached = sharedStringRedisTemplate.opsForValue().get(cacheKey);
		if (StringUtils.hasText(cached)) {
			return cached;
		}
		String ticket = fetchJsapiTicket(accessToken);
		sharedStringRedisTemplate
				.opsForValue()
				.set(cacheKey, ticket, Duration.ofSeconds(JSAPI_TICKET_TTL_SECONDS));
		return ticket;
	}

	private String fetchJsapiTicket(String accessToken) {
		WxCpService cp = workWechatWxCpRuntime.cpBearer(accessToken);
		try {
			return cp.getJsapiTicket(false);
		} catch (WxErrorException e) {
			log.warn(
					"work wechat get_jsapi_ticket wx err errcode={} errmsg={}",
					e.getError() != null ? e.getError().getErrorCode() : null,
					e.getError() != null ? e.getError().getErrorMsg() : e.getMessage());
			throw new ResourceException("企业微信接口调用失败");
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			log.warn("work wechat get_jsapi_ticket request failed", e);
			throw new ResourceException("企业微信接口调用失败");
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
