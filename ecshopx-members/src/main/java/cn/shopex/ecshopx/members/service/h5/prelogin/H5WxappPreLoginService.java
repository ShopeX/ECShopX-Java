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

package cn.shopex.ecshopx.members.service.h5.prelogin;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.shuyun.ShuyunOpenPlatformWxappMemberSyncPort;
import cn.shopex.ecshopx.members.client.wx.WxOpenPlatformClient;
import cn.shopex.ecshopx.members.config.H5LocalProperties;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class H5WxappPreLoginService {

	private final WxOpenPlatformClient wxOpenPlatformClient;

	private final MemberAccountService memberAccountService;

	private final H5WxappLoginRegisterFacade h5WxappLoginRegisterFacade;

	private final H5LocalProperties h5LocalProperties;

	private final ShuyunOpenPlatformWxappMemberSyncPort openPlatformWxappMemberSyncPort;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public H5WxappPreLoginService(
			WxOpenPlatformClient wxOpenPlatformClient,
			MemberAccountService memberAccountService,
			H5WxappLoginRegisterFacade h5WxappLoginRegisterFacade,
			H5LocalProperties h5LocalProperties,
			ShuyunOpenPlatformWxappMemberSyncPort openPlatformWxappMemberSyncPort) {
		this.wxOpenPlatformClient = wxOpenPlatformClient;
		this.memberAccountService = memberAccountService;
		this.h5WxappLoginRegisterFacade = h5WxappLoginRegisterFacade;
		this.h5LocalProperties = h5LocalProperties;
		this.openPlatformWxappMemberSyncPort = openPlatformWxappMemberSyncPort;
	}

	public Map<String, Object> wxappPreLogin(Map<String, Object> params) {
		String appid = trim(String.valueOf(params.get("appid")));
		if (!StringUtils.hasText(appid)) {
			throw new ResourceException("缺少参数，登录失败！");
		}
		String code = trim(String.valueOf(params.get("code")));
		if (!StringUtils.hasText(code)) {
			throw new ResourceException("缺少参数，登录失败！");
		}
		String iv = trim(String.valueOf(params.get("iv")));
		if (!StringUtils.hasText(iv)) {
			throw new ResourceException("缺少参数，登录失败！");
		}
		String encryptedData = trim(String.valueOf(params.get("encryptedData")));
		if (!StringUtils.hasText(encryptedData)) {
			throw new ResourceException("缺少参数，登录失败！");
		}

		wxOpenPlatformClient.assertWxMiniProgramAppIdRecognized(appid);

		long companyId = toLong(params.get("company_id"));
		Map<String, String> session = wxOpenPlatformClient.miniProgramCode2Session(appid, code);
		String sessionKey = session.get("session_key");
		if (!StringUtils.hasText(sessionKey)) {
			throw new ResourceException("用户登录失败！");
		}
		String openId = session.get("openid");
		if (!StringUtils.hasText(openId)) {
			throw new ResourceException("小程序授权错误，请联系供应商！");
		}
		String unionFromApi = session.get("unionid");
		String unionId = StringUtils.hasText(unionFromApi) ? unionFromApi : openId;

		String phoneJson = wxOpenPlatformClient.decryptWxMiniProgramPhoneNumber(sessionKey, iv, encryptedData);
		String mobile;
		try {
			JsonNode root = objectMapper.readTree(phoneJson);
			JsonNode pure = root.get("purePhoneNumber");
			mobile = pure == null || pure.isNull() ? "" : pure.asText().trim();
		} catch (Exception e) {
			throw new ResourceException("授权手机号失败");
		}
		if (!StringUtils.hasText(mobile)) {
			throw new ResourceException("授权手机号失败");
		}

		if (h5LocalProperties.isTransferMode()) {
			Map<String, Object> wechatUser = memberAccountService.getWechatSimpleUser(Map.of(
					"open_id", openId,
					"authorizer_appid", appid,
					"company_id", companyId));
			if (wechatUser != null && !wechatUser.isEmpty()) {
				String oldUnion = Objects.toString(wechatUser.get("unionid"), "");
				if (StringUtils.hasText(oldUnion) && !oldUnion.equals(unionId)) {
					memberAccountService.updateWechatUnionId(companyId, appid, openId, oldUnion, unionId);
				}
			}
		}

		params.put("open_id", openId);
		params.put("unionid", unionId);
		params.put("mobile", mobile);
		params.put("user_type", "wechat");

		resolveWxappInviterParams(params, companyId, appid, unionId);

		Map<String, Object> wxSession = buildWxSession(sessionKey, openId, unionId, mobile);

		Map<String, Object> member = memberAccountService.getInfoByMobile(companyId, mobile);

		if (member == null || member.isEmpty()) {
			h5WxappLoginRegisterFacade.registerMemberForWxapp(params, wxSession);
			member = memberAccountService.getInfoByMobile(companyId, mobile);
		} else if (h5LocalProperties.isOemShuyun()) {
			long userId = toLong(member.get("user_id"));
			Map<String, Object> userAssociation =
					memberAccountService.getMembersAssociationByUserId(companyId, "wechat", userId);
			if (userAssociation == null || userAssociation.isEmpty()) {
				h5WxappLoginRegisterFacade.registerMemberForWxapp(params, wxSession);
				member = memberAccountService.getInfoByMobile(companyId, mobile);
			} else if (!unionId.equals(Objects.toString(userAssociation.get("unionid"), ""))) {
				throw new ResourceException("该手机号已注册为会员，请更换手机号！");
			} else {
				h5WxappLoginRegisterFacade.registerMemberForWxapp(params, wxSession);
			}
		} else {
			long userId = toLong(member.get("user_id"));
			Map<String, Object> userAssociation =
					memberAccountService.getMembersAssociationByUserId(companyId, "wechat", userId);
			boolean linked = userAssociation != null
					&& !userAssociation.isEmpty()
					&& unionId.equals(Objects.toString(userAssociation.get("unionid"), ""));
			if (!linked) {
				h5WxappLoginRegisterFacade.registerMemberForWxapp(params, wxSession);
				member = memberAccountService.getInfoByMobile(companyId, mobile);
			}
		}

		long finalUserId = toLong(member.get("user_id"));
		h5WxappLoginRegisterFacade.bindSalespersonIfNeeded(params, finalUserId);

		h5WxappLoginRegisterFacade.syncWxappFansFromSession(params, wxSession);

		// 老会员补同步：失败不阻断登录（对齐 PHP maybeSync…AfterPreLogin）
		openPlatformWxappMemberSyncPort.syncWxappOnlineIfEnabled(
				companyId,
				finalUserId,
				mobile,
				unionId,
				openId,
				toLong(params.get("distributor_id")),
				false);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("user_id", member.get("user_id"));
		out.put("open_id", openId);
		out.put("unionid", unionId);
		return out;
	}

	private void resolveWxappInviterParams(Map<String, Object> params, long companyId, String appid, String unionId) {
		long inviterId = numberOrZero(params.get("inviter_id"));
		long uid = toLong(params.get("uid"));
		if (uid > 0L) {
			Map<String, Object> memberInfo = memberAccountService.getMemberInfo(uid, companyId);
			if (memberInfo != null && !memberInfo.isEmpty()) {
				inviterId = uid;
			}
		} else {
			long puid = toLong(params.get("puid"));
			if (puid > 0L) {
				Map<String, Object> memberInfo = memberAccountService.getMemberInfo(puid, companyId);
				if (memberInfo != null && !memberInfo.isEmpty()) {
					inviterId = puid;
				}
			} else if (inviterId == 0L) {
				Map<String, Object> wxUser = memberAccountService.getWechatSimpleUser(Map.of(
						"company_id", companyId,
						"authorizer_appid", appid,
						"unionid", unionId));
				if (wxUser != null && !wxUser.isEmpty()) {
					inviterId = toLong(wxUser.get("inviter_id"));
					String sf = Objects.toString(wxUser.get("source_from"), "").trim();
					if (StringUtils.hasText(sf)) {
						params.put("source_from", sf);
					}
				}
			}
		}
		params.put("inviter_id", inviterId);
	}

	private static Map<String, Object> buildWxSession(String sessionKey, String openId, String unionId, String mobile) {
		Map<String, Object> wxSession = new LinkedHashMap<>();
		wxSession.put("session_key", sessionKey);
		wxSession.put("openid", openId);
		wxSession.put("unionid", unionId);
		wxSession.put("purePhoneNumber", mobile);
		return wxSession;
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String trim(String s) {
		return s == null ? "" : s.trim();
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

	private static String stringOrDefault(Object o, String def) {
		String s = o == null ? "" : String.valueOf(o).trim();
		return StringUtils.hasText(s) ? s : def;
	}
}
