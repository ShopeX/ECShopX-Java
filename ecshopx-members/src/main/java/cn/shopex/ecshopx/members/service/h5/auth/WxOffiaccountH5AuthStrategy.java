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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class WxOffiaccountH5AuthStrategy implements H5AuthStrategy {

	private final WxOpenPlatformClient wxOpenPlatformClient;

	private final WechatAuthorizerLookupMapper wechatAuthorizerLookupMapper;

	private final MemberAccountService memberAccountService;

	public WxOffiaccountH5AuthStrategy(
			WxOpenPlatformClient wxOpenPlatformClient,
			WechatAuthorizerLookupMapper wechatAuthorizerLookupMapper,
			MemberAccountService memberAccountService) {
		this.wxOpenPlatformClient = wxOpenPlatformClient;
		this.wechatAuthorizerLookupMapper = wechatAuthorizerLookupMapper;
		this.memberAccountService = memberAccountService;
	}

	@Override
	public H5AuthType type() {
		return H5AuthType.WX_OFFIACCOUNT;
	}

	@Override
	public Optional<H5GenericUser> resolve(Map<String, Object> credentials) {
		if (!StringUtils.hasText(stringVal(credentials.get("company_id")))) {
			throw new ResourceException("缺少参数！");
		}
		if (!StringUtils.hasText(stringVal(credentials.get("code")))) {
			throw new ResourceException("code error");
		}
		long companyId = toLong(credentials.get("company_id"));
		String woaAppid = wechatAuthorizerLookupMapper.selectServiceAccountAppidByCompanyId(companyId);
		if (!StringUtils.hasText(woaAppid)) {
			throw new ResourceException("公众号信息有误！");
		}
		String secret = wechatAuthorizerLookupMapper.selectAuthorizerSecretByAppid(woaAppid);
		if (!StringUtils.hasText(secret)) {
			throw new ResourceException("公众号信息有误！");
		}
		Map<String, String> tok =
				wxOpenPlatformClient.oauth2SnsAccessToken(woaAppid, secret, stringVal(credentials.get("code")));
		String openid = tok.get("openid");
		String accessToken = tok.get("access_token");
		if (!StringUtils.hasText(openid) || !StringUtils.hasText(accessToken)) {
			throw new ResourceException("公众号信息有误！");
		}
		Map<String, String> res = wxOpenPlatformClient.snsUserinfo(accessToken, openid);
		String unionid = res.get("unionid");
		if (!StringUtils.hasText(unionid)) {
			unionid = openid;
		}
		String nickname = stringVal(res.get("nickname"));
		int sex = 0;
		if (res.get("sex") != null) {
			try {
				sex = Integer.parseInt(res.get("sex"));
			} catch (NumberFormatException ignored) {
			}
		}
		String headimg = stringVal(res.get("headimgurl"));
		Map<String, Object> fanParams = new HashMap<>();
		fanParams.put("openid", openid);
		fanParams.put("unionid", unionid);
		fanParams.put("nickname", nickname);
		fanParams.put("sex", sex);
		fanParams.put("headimgurl", headimg);
		fanParams.put("country", stringVal(res.get("country")));
		fanParams.put("province", stringVal(res.get("province")));
		fanParams.put("city", stringVal(res.get("city")));
		fanParams.put("language", stringVal(res.get("language")));
		fanParams.put("inviter_id", numberOrZero(credentials.get("inviter_id")));
		fanParams.put("source_id", numberOrZero(credentials.get("source_id")));
		fanParams.put("monitor_id", numberOrZero(credentials.get("monitor_id")));
		fanParams.put("source_from", stringOrDefault(credentials.get("source_from"), "default"));
		Map<String, Object> created = memberAccountService.createOffiaccountFans(companyId, woaAppid, fanParams);
		if (created == null || created.isEmpty()) {
			throw new ResourceException("Invalid store wxapp user.");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> wechatUser = (Map<String, Object>) created.get("wechatuser");
		if (wechatUser == null) {
			wechatUser = Map.of();
		}
		int isNew = 0;
		Object isNewObj = created.get("is_new");
		if (isNewObj instanceof Number n) {
			isNew = n.intValue();
		}
		Map<String, Object> attrs = new HashMap<>();
		if (isNew != 0) {
			attrs.put("id", "0_espier_" + stringVal(wechatUser.get("open_id")) + "_espier_" + stringVal(wechatUser.get("unionid")));
			attrs.put("user_id", 0L);
			attrs.put("disabled", 0);
			attrs.put("company_id", companyId);
			attrs.put("wxapp_appid", wechatUser.get("authorizer_appid"));
			attrs.put("woa_appid", woaAppid);
			attrs.put("unionid", wechatUser.get("unionid"));
			attrs.put("openid", wechatUser.get("open_id"));
			attrs.put("nickname", nickname);
			attrs.put("mobile", "");
			attrs.put("username", stringVal(res.get("nickname")));
			attrs.put("sex", sex);
			attrs.put("user_card_code", "");
			attrs.put("offline_card_code", "");
			attrs.put("operator_type", "user");
			attrs.put("is_new", 1);
		} else {
			@SuppressWarnings("unchecked")
			Map<String, Object> member = (Map<String, Object>) created.get("memberInfo");
			if (member == null) {
				member = Map.of();
			}
			long userId = toLong(member.get("user_id"));
			attrs.put("id", userId + "_espier_" + stringVal(wechatUser.get("open_id")) + "_espier_" + stringVal(wechatUser.get("unionid")));
			attrs.put("user_id", userId);
			attrs.put("disabled", member.getOrDefault("disabled", 0));
			attrs.put("mobile", stringVal(member.get("mobile")));
			attrs.put("user_card_code", stringVal(member.get("user_card_code")));
			attrs.put("offline_card_code", stringVal(member.get("offline_card_code")));
			attrs.put("company_id", companyId);
			attrs.put("wxapp_appid", wechatUser.get("authorizer_appid"));
			attrs.put("woa_appid", woaAppid);
			attrs.put("unionid", wechatUser.get("unionid"));
			attrs.put("openid", wechatUser.get("open_id"));
			attrs.put("nickname", nickname);
			attrs.put("username", stringVal(member.get("username")));
			attrs.put("sex", member.get("sex"));
			attrs.put("operator_type", "user");
			attrs.put("is_new", 0);
		}
		return Optional.of(new H5GenericUser(attrs));
	}

	private static String stringVal(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private static String stringOrDefault(Object o, String def) {
		String s = stringVal(o);
		return StringUtils.hasText(s) ? s : def;
	}

	private static int numberOrZero(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(o));
	}
}
