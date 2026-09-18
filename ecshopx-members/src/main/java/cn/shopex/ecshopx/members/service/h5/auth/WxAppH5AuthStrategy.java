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
import cn.shopex.ecshopx.members.config.H5LocalProperties;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.members.service.h5.H5GenericUser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class WxAppH5AuthStrategy implements H5AuthStrategy {

	private final H5LocalProperties h5LocalProperties;

	private final WxOpenPlatformClient wxOpenPlatformClient;

	private final MemberAccountService memberAccountService;

	private final ShuyunLoginBridgeService shuyunLoginBridgeService;

	public WxAppH5AuthStrategy(
			H5LocalProperties h5LocalProperties,
			WxOpenPlatformClient wxOpenPlatformClient,
			MemberAccountService memberAccountService,
			ShuyunLoginBridgeService shuyunLoginBridgeService) {
		this.h5LocalProperties = h5LocalProperties;
		this.wxOpenPlatformClient = wxOpenPlatformClient;
		this.memberAccountService = memberAccountService;
		this.shuyunLoginBridgeService = shuyunLoginBridgeService;
	}

	@Override
	public H5AuthType type() {
		return H5AuthType.WXAPP;
	}

	@Override
	public Optional<H5GenericUser> resolve(Map<String, Object> credentials) {
		if (h5LocalProperties.isOemShuyun()) {
			return resolveShuyun(credentials);
		}
		return resolveStandard(credentials);
	}

	private Optional<H5GenericUser> resolveStandard(Map<String, Object> credentials) {
		String appid = stringVal(credentials.get("appid"));
		if (!StringUtils.hasText(appid)) {
			throw new ResourceException("缺少小程序ID");
		}
		Map<String, String> res = sessionOrProvided(credentials, appid);
		String openid = res.get("openid");
		if (!StringUtils.hasText(openid)) {
			throw new ResourceException("小程序登录获取信息失败，请重试！");
		}
		String unionid = res.get("unionid");
		if (!StringUtils.hasText(unionid)) {
			unionid = openid;
		}
		long companyId = parseCompanyId(credentials, appid);
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
		if (h5LocalProperties.isTransferMode() && Boolean.TRUE.equals(wechatUser.get("need_transfer"))) {
			throw new ResourceException("请您重新授权用户信息！");
		}
		return Optional.of(buildUserAttrs(companyId, openid, user));
	}

	private Optional<H5GenericUser> resolveShuyun(Map<String, Object> credentials) {
		String appid = stringVal(credentials.get("appid"));
		if (!StringUtils.hasText(appid)) {
			throw new ResourceException("缺少小程序ID");
		}
		Map<String, String> res = sessionOrProvided(credentials, appid);
		String openid = res.get("openid");
		if (!StringUtils.hasText(openid)) {
			throw new ResourceException("小程序登陆获取信息失败，请重试！");
		}
		String unionid = res.get("unionid");
		if (!StringUtils.hasText(unionid)) {
			unionid = openid;
		}
		long companyId = parseCompanyId(credentials, appid);
		Map<String, Object> wechatUser = memberAccountService.getWechatSimpleUser(Map.of(
				"open_id", openid,
				"authorizer_appid", appid,
				"company_id", companyId
		));
		if (wechatUser == null || wechatUser.isEmpty()) {
			Map<String, Object> p = baseShuyunParams(companyId, openid, unionid);
			if (shuyunLoginBridgeService.shuyunMemberSilent(credentials, p).isEmpty()) {
				throw new ResourceException("请您检查是否已经授权！");
			}
			wechatUser = memberAccountService.getWechatSimpleUser(Map.of(
					"open_id", openid,
					"authorizer_appid", appid,
					"company_id", companyId
			));
		}
		Map<String, Object> user = memberAccountService.getWechatUserInfo(Map.of(
				"open_id", openid,
				"unionid", unionid,
				"company_id", companyId
		));
		if ((user == null || user.isEmpty()) && wechatUser != null && !wechatUser.isEmpty()) {
			memberAccountService.updateWechatUnionId(
					companyId,
					appid,
					openid,
					stringVal(wechatUser.get("unionid")),
					unionid);
			user = memberAccountService.getWechatUserInfo(Map.of(
					"open_id", openid,
					"unionid", unionid,
					"company_id", companyId
			));
		}
		if (user == null || user.isEmpty()) {
			if (shuyunLoginBridgeService.shuyunMemberSilent(credentials, baseShuyunParams(companyId, openid, unionid)).isEmpty()) {
				throw new ResourceException("登录出错，请联系服务商！");
			}
			user = memberAccountService.getWechatUserInfo(Map.of(
					"open_id", openid,
					"unionid", unionid,
					"company_id", companyId
			));
		}
		Object uid = user != null ? user.get("user_id") : null;
		if (user == null || user.isEmpty() || uid == null || uid.toString().isEmpty() || "0".equals(String.valueOf(uid))) {
			if (shuyunLoginBridgeService.shuyunMemberSilent(credentials, baseShuyunParams(companyId, openid, unionid)).isEmpty()) {
				throw new ResourceException("请注册！");
			}
			user = memberAccountService.getWechatUserInfo(Map.of(
					"open_id", openid,
					"unionid", unionid,
					"company_id", companyId
			));
			uid = user != null ? user.get("user_id") : null;
		}
		if (user == null || user.isEmpty() || uid == null || uid.toString().isEmpty() || "0".equals(String.valueOf(uid))) {
			throw new ResourceException("请注册！");
		}
		Map<String, Object> wu = memberAccountService.getWechatSimpleUser(Map.of(
				"open_id", openid,
				"authorizer_appid", appid,
				"company_id", companyId
		));
		if (h5LocalProperties.isTransferMode() && wu != null && Boolean.TRUE.equals(wu.get("need_transfer"))) {
			throw new ResourceException("请您重新授权用户信息！");
		}
		return Optional.of(buildUserAttrs(companyId, openid, user));
	}

	private Map<String, Object> baseShuyunParams(long companyId, String openid, String unionid) {
		Map<String, Object> p = new HashMap<>();
		p.put("unionid", unionid);
		p.put("open_id", openid);
		p.put("company_id", companyId);
		return p;
	}

	private H5GenericUser buildUserAttrs(long companyId, String openid, Map<String, Object> user) {
		long userId = toLong(user.get("user_id"));
		Map<String, Object> attrs = new HashMap<>();
		attrs.put("id", userId + "_espier_" + String.valueOf(user.get("open_id")) + "_espier_" + String.valueOf(user.get("unionid")));
		attrs.put("user_id", userId);
		attrs.put("company_id", companyId);
		attrs.put("unionid", user.get("unionid"));
		attrs.put("openid", openid);
		attrs.put("operator_type", "user");
		return new H5GenericUser(attrs);
	}

	private Map<String, String> sessionOrProvided(Map<String, Object> credentials, String appid) {
		if (StringUtils.hasText(stringVal(credentials.get("openid")))) {
			Map<String, String> m = new HashMap<>();
			m.put("openid", stringVal(credentials.get("openid")));
			m.put("unionid", stringVal(credentials.get("unionid")));
			return m;
		}
		return wxOpenPlatformClient.miniProgramCode2Session(appid, stringVal(credentials.get("code")));
	}

	private long parseCompanyId(Map<String, Object> credentials, String appid) {
		Object cid = credentials.get("company_id");
		if (cid != null && StringUtils.hasText(String.valueOf(cid))) {
			return toLong(cid);
		}
		Long fromDb = wxOpenPlatformClient.getCompanyIdByAuthorizerAppid(appid);
		if (fromDb == null) {
			throw new ResourceException("缺少企业信息");
		}
		return fromDb;
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
