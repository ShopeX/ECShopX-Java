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

package cn.shopex.ecshopx.members.service.wxapp;

import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.members.mapper.WechatAuthorizerLookupMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappMemberAuthService {

	private static final String SESSION_KEY_PREFIX = "session3rd:";

	private final StringRedisTemplate wechatRedis;

	private final ObjectMapper objectMapper;

	private final MemberAccountService memberAccountService;

	private final WechatAuthorizerLookupMapper wechatAuthorizerLookupMapper;

	public WxappMemberAuthService(
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate wechatRedis,
			ObjectMapper objectMapper,
			MemberAccountService memberAccountService,
			WechatAuthorizerLookupMapper wechatAuthorizerLookupMapper) {
		this.wechatRedis = wechatRedis;
		this.objectMapper = objectMapper;
		this.memberAccountService = memberAccountService;
		this.wechatAuthorizerLookupMapper = wechatAuthorizerLookupMapper;
	}

	/**
	 * Resolves wxapp session from Redis and loads member context (aligned with legacy wxapp auth provider).
	 */
	public Map<String, Object> resolveSession(String sessionKey) {
		String raw = wechatRedis.opsForValue().get(SESSION_KEY_PREFIX + sessionKey);
		if (!StringUtils.hasText(raw)) {
			throw new UnauthorizedException("Unable to authenticate wxapp user.");
		}
		Map<String, Object> sessionVal;
		try {
			sessionVal = objectMapper.readValue(raw, new TypeReference<>() {});
		} catch (Exception e) {
			throw new UnauthorizedException("Unable to authenticate wxapp user.");
		}
		if (sessionVal == null || sessionVal.isEmpty()) {
			throw new UnauthorizedException("Unable to authenticate wxapp user.");
		}
		String openId = stringVal(sessionVal.get("open_id"));
		if (!StringUtils.hasText(openId)) {
			throw new UnauthorizedException("Unable to authenticate wxapp user.");
		}
		String unionId = stringVal(firstNonNull(sessionVal.get("union_id"), sessionVal.get("unionid")));

		Map<String, Object> userFilter = new LinkedHashMap<>();
		userFilter.put("open_id", openId);
		if (StringUtils.hasText(unionId)) {
			userFilter.put("unionid", unionId);
		}
		Map<String, Object> user = memberAccountService.getWechatUserInfo(userFilter);
		if (user == null || user.isEmpty()) {
			throw new UnauthorizedException("Unable to authenticate wxapp user.");
		}
		String authorizerAppid = stringVal(user.get("authorizer_appid"));
		if (!StringUtils.hasText(authorizerAppid)) {
			throw new UnauthorizedException("Unable to authenticate wxapp user.");
		}
		Long companyId = wechatAuthorizerLookupMapper.selectCompanyIdByAuthorizerAppid(authorizerAppid);
		if (companyId == null || companyId <= 0) {
			throw new UnauthorizedException("Unable to authenticate wxapp user.");
		}

		Object uidRaw = user.get("user_id");
		long userId = 0L;
		if (uidRaw != null && StringUtils.hasText(uidRaw.toString().trim())) {
			try {
				userId = Long.parseLong(uidRaw.toString().trim());
			} catch (NumberFormatException ignored) {
				userId = 0L;
			}
		}

		Map<String, Object> memberInfo =
				userId > 0 ? memberAccountService.getMemberInfo(userId, companyId) : Map.of();
		if (!memberInfo.isEmpty() && isDisabled(memberInfo.get("disabled"))) {
			throw new UnauthorizedException("该账号已被禁用.");
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("user_id", userId > 0 ? userId : "");
		out.put("company_id", companyId);
		out.put("wxapp_appid", authorizerAppid);
		out.put("open_id", stringVal(user.get("open_id")));
		out.put("unionid", stringVal(user.get("unionid")));
		out.put("nickname", stringVal(user.get("nickname")));
		out.put(
				"mobile",
				memberInfo.containsKey("mobile") ? stringVal(memberInfo.get("mobile")) : "");
		out.put(
				"username",
				memberInfo.containsKey("username") ? stringVal(memberInfo.get("username")) : "");
		int sex = 0;
		if (memberInfo.containsKey("sex") && memberInfo.get("sex") != null) {
			try {
				sex = Integer.parseInt(memberInfo.get("sex").toString().trim());
			} catch (Exception ignored) {
				sex = 0;
			}
		} else if (user.get("sex") != null) {
			try {
				sex = Integer.parseInt(user.get("sex").toString().trim());
			} catch (Exception ignored) {
				sex = 0;
			}
		}
		out.put("sex", sex);
		return out;
	}

	private static Object firstNonNull(Object a, Object b) {
		return a != null ? a : b;
	}

	private static String stringVal(Object o) {
		return o == null ? "" : Objects.toString(o, "");
	}

	private static boolean isDisabled(Object v) {
		if (v == null) {
			return false;
		}
		if (Boolean.TRUE.equals(v)) {
			return true;
		}
		String s = v.toString().trim();
		return "1".equals(s) || "true".equalsIgnoreCase(s);
	}
}
