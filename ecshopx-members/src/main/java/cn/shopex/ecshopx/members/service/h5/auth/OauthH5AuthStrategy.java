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

package cn.shopex.ecshopx.members.service.h5.auth;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.client.wx.WxOpenPlatformClient;
import cn.shopex.ecshopx.members.mapper.WechatAuthorizerLookupMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.members.service.h5.H5GenericUser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class OauthH5AuthStrategy implements H5AuthStrategy {

	private final WxOpenPlatformClient wxOpenPlatformClient;

	private final MemberAccountService memberAccountService;

	private final StringRedisTemplate stringRedisTemplate;

	private final WechatAuthorizerLookupMapper wechatAuthorizerLookupMapper;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public OauthH5AuthStrategy(
			WxOpenPlatformClient wxOpenPlatformClient,
			MemberAccountService memberAccountService,
			StringRedisTemplate stringRedisTemplate,
			WechatAuthorizerLookupMapper wechatAuthorizerLookupMapper) {
		this.wxOpenPlatformClient = wxOpenPlatformClient;
		this.memberAccountService = memberAccountService;
		this.stringRedisTemplate = stringRedisTemplate;
		this.wechatAuthorizerLookupMapper = wechatAuthorizerLookupMapper;
	}

	@Override
	public H5AuthType type() {
		return H5AuthType.OAUTH;
	}

	@Override
	public Optional<H5GenericUser> resolve(Map<String, Object> credentials) {
		String appid = stringVal(credentials.get("appid"));
		if (!StringUtils.hasText(appid)) {
			throw new ResourceException("缺少参数，获取用户信息失败！");
		}
		if (!StringUtils.hasText(stringVal(credentials.get("openid")))) {
			throw new ResourceException("小程序信息错误，请联系服务商！");
		}
		String openid = stringVal(credentials.get("openid"));
		Map<String, Object> wechatUser = memberAccountService.getWechatSimpleUser(Map.of(
				"open_id", openid,
				"authorizer_appid", appid
		));
		if (wechatUser == null || wechatUser.isEmpty()) {
			throw new ResourceException("请您检查是否已经授权！");
		}
		String unionid = stringVal(wechatUser.get("unionid"));
		if (!StringUtils.hasText(unionid)) {
			throw new ResourceException("请授权！");
		}
		long companyId = toLong(wechatUser.get("company_id"));
		if (companyId <= 0) {
			Long fromApp = wxOpenPlatformClient.getCompanyIdByAuthorizerAppid(appid);
			if (fromApp == null) {
				throw new ResourceException("缺少企业信息");
			}
			companyId = fromApp;
		}
		String woaAppid = stringVal(wechatAuthorizerLookupMapper.selectServiceAccountAppidByCompanyId(companyId));

		String token = stringVal(credentials.get("token"));
		String redisKey = "member:oauth:login:" + token;
		String json = stringRedisTemplate.opsForValue().get(redisKey);
		if (!StringUtils.hasText(json)) {
			throw new ResourceException("授权失败");
		}
		try {
			Map<String, Object> info = objectMapper.readValue(json, new TypeReference<>() {});
			if (!unionid.equals(stringVal(info.get("union_id")))) {
				throw new ResourceException("用户错误");
			}
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("授权失败");
		}

		Map<String, Object> user = memberAccountService.getWechatUserInfo(Map.of(
				"open_id", openid,
				"unionid", unionid,
				"company_id", companyId
		));
		if (user == null || user.isEmpty()) {
			throw new ResourceException("登录出错，请联系服务商！");
		}
		Object uid = user.get("user_id");
		if (uid == null || uid.toString().isEmpty() || "0".equals(String.valueOf(uid))) {
			throw new ResourceException("请注册！");
		}
		Map<String, Object> memberInfo = memberAccountService.getMemberInfo(toLong(uid), companyId);
		Map<String, Object> attrs = new HashMap<>();
		attrs.put("id", String.valueOf(uid) + "_espier_" + String.valueOf(user.get("open_id")) + "_espier_" + String.valueOf(user.get("unionid")));
		attrs.put("user_id", toLong(uid));
		attrs.put("disabled", memberInfo.getOrDefault("disabled", 0));
		attrs.put("company_id", companyId);
		attrs.put("wxapp_appid", user.get("authorizer_appid"));
		attrs.put("woa_appid", woaAppid);
		attrs.put("unionid", user.get("unionid"));
		attrs.put("openid", openid);
		attrs.put("nickname", user.get("nickname"));
		attrs.put("mobile", memberInfo.get("mobile"));
		attrs.put("username", memberInfo.get("username"));
		attrs.put("sex", memberInfo.get("sex") != null ? memberInfo.get("sex") : user.get("sex"));
		attrs.put("user_card_code", memberInfo.get("user_card_code"));
		attrs.put("offline_card_code", memberInfo.get("offline_card_code"));
		attrs.put("operator_type", "user");
		return Optional.of(new H5GenericUser(attrs));
	}

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(o));
	}
}
