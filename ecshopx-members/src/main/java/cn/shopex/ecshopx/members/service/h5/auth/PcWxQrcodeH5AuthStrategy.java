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
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.members.service.h5.H5GenericUser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class PcWxQrcodeH5AuthStrategy implements H5AuthStrategy {

	private final WxOpenPlatformClient wxOpenPlatformClient;

	private final MemberAccountService memberAccountService;

	public PcWxQrcodeH5AuthStrategy(WxOpenPlatformClient wxOpenPlatformClient, MemberAccountService memberAccountService) {
		this.wxOpenPlatformClient = wxOpenPlatformClient;
		this.memberAccountService = memberAccountService;
	}

	@Override
	public H5AuthType type() {
		return H5AuthType.PC_WXQRCODE;
	}

	@Override
	public Optional<H5GenericUser> resolve(Map<String, Object> credentials) {
		if (!StringUtils.hasText(stringVal(credentials.get("company_id")))) {
			throw new ResourceException("缺少company_id参数，获取用户信息失败！");
		}
		if (!StringUtils.hasText(stringVal(credentials.get("appid")))) {
			throw new ResourceException("缺少appid参数，获取用户信息失败！");
		}
		long companyId = toLong(credentials.get("company_id"));
		String appid = stringVal(credentials.get("appid"));
		String secret = stringVal(credentials.get("secret"));
		String code = stringVal(credentials.get("code"));
		if (!StringUtils.hasText(secret) || !StringUtils.hasText(code)) {
			throw new ResourceException("公众号授权信息错误，请联系服务商！");
		}
		Map<String, String> tok = wxOpenPlatformClient.oauth2SnsAccessToken(appid, secret, code);
		String openid = tok.get("openid");
		if (!StringUtils.hasText(openid)) {
			throw new ResourceException("公众号授权信息错误，请联系服务商！");
		}
		String accessToken = tok.get("access_token");
		Map<String, String> wxUser = wxOpenPlatformClient.snsUserinfo(accessToken, openid);
		String unionid = wxUser.get("unionid");
		if (!StringUtils.hasText(unionid)) {
			unionid = openid;
		}
		Map<String, Object> fanParams = new HashMap<>();
		fanParams.put("openid", openid);
		fanParams.put("unionid", unionid);
		fanParams.put("nickname", stringVal(wxUser.get("nickname")));
		fanParams.put("headimgurl", stringVal(wxUser.get("headimgurl")));
		if (wxUser.get("sex") != null) {
			fanParams.put("sex", Integer.parseInt(wxUser.get("sex")));
		} else {
			fanParams.put("sex", 0);
		}
		fanParams.put("country", "");
		fanParams.put("province", "");
		fanParams.put("city", "");
		fanParams.put("language", "");
		fanParams.put("inviter_id", numberOrZero(credentials.get("inviter_id")));
		fanParams.put("source_id", numberOrZero(credentials.get("source_id")));
		fanParams.put("monitor_id", numberOrZero(credentials.get("monitor_id")));
		fanParams.put("source_from", stringOrDefault(credentials.get("source_from"), "default"));
		Map<String, Object> created = memberAccountService.createOffiaccountFans(companyId, appid, fanParams);
		if (created == null || created.isEmpty()) {
			throw new ResourceException("Invalid store wxapp user.");
		}
		Map<String, Object> wechatUser = memberAccountService.getWechatSimpleUser(Map.of(
				"open_id", openid,
				"authorizer_appid", appid,
				"company_id", companyId
		));
		if (wechatUser == null || wechatUser.isEmpty()) {
			throw new ResourceException("请您检查是否已经授权！");
		}
		Map<String, Object> user = memberAccountService.getWechatUserInfo(Map.of(
				"open_id", openid,
				"unionid", stringVal(wechatUser.get("unionid")),
				"company_id", companyId
		));
		if (user == null || user.isEmpty()) {
			throw new ResourceException("登录出错，请联系服务商！");
		}
		Map<String, Object> memberInfo = new HashMap<>();
		Object uid = user.get("user_id");
		if (uid != null && toLong(uid) > 0) {
			memberInfo = memberAccountService.getMemberInfo(toLong(uid), companyId);
		} else {
			user.put("user_id", 0L);
		}
		Map<String, Object> attrs = new HashMap<>();
		attrs.put("id", String.valueOf(user.get("user_id")) + "_espier_" + String.valueOf(user.get("open_id")) + "_espier_" + String.valueOf(user.get("unionid")));
		attrs.put("user_id", user.get("user_id") != null ? toLong(user.get("user_id")) : 0L);
		attrs.put("disabled", memberInfo.getOrDefault("disabled", 0));
		attrs.put("company_id", companyId);
		attrs.put("wxapp_appid", user.get("authorizer_appid"));
		attrs.put("woa_appid", appid);
		attrs.put("unionid", user.get("unionid"));
		attrs.put("openid", openid);
		attrs.put("nickname", stringVal(user.get("nickname")));
		attrs.put("mobile", stringVal(memberInfo.get("mobile")));
		attrs.put("username", stringVal(memberInfo.get("username")));
		attrs.put("sex", memberInfo.get("sex") != null ? memberInfo.get("sex") : user.get("sex"));
		attrs.put("user_card_code", stringVal(memberInfo.get("user_card_code")));
		attrs.put("offline_card_code", stringVal(memberInfo.get("offline_card_code")));
		attrs.put("operator_type", "user");
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
