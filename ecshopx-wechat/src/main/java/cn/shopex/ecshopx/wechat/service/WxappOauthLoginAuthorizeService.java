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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.members.port.WxappOauthLoginAuthorizePort;
import cn.shopex.ecshopx.wechat.domain.WechatAuth;
import cn.shopex.ecshopx.wechat.mapper.WechatAuthMapper;
import cn.shopex.ecshopx.wechat.wxa.WxappJscode2SessionLenientClient;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappOauthLoginAuthorizeService {

	private final WechatAuthMapper wechatAuthMapper;
	private final WxappJscode2SessionLenientClient wxappJscode2SessionLenientClient;
	private final WxappOauthLoginAuthorizePort wxappOauthLoginAuthorizePort;

	public WxappOauthLoginAuthorizeService(
			WechatAuthMapper wechatAuthMapper,
			WxappJscode2SessionLenientClient wxappJscode2SessionLenientClient,
			WxappOauthLoginAuthorizePort wxappOauthLoginAuthorizePort) {
		this.wechatAuthMapper = wechatAuthMapper;
		this.wxappJscode2SessionLenientClient = wxappJscode2SessionLenientClient;
		this.wxappOauthLoginAuthorizePort = wxappOauthLoginAuthorizePort;
	}

	public boolean checkOauthLogin(HttpServletRequest request, Map<String, Object> body) {
		Map<String, Object> merged = WxappOpenIdUnionidService.mergeJsonBodyWithRequestParams(body, request);
		Object appidRaw = merged.get("appid");
		if (isAppidEmptyForCheckOauthLogin(appidRaw)) {
			throw new ResourceException("缺少参数，登录失败！", 422);
		}
		String appid = String.valueOf(appidRaw).trim();
		if (!isParamSet(merged, "code")
				|| String.valueOf(merged.get("code")).trim().isEmpty()) {
			throw new ResourceException("当前公众号或小程序未绑定或已解绑，请重新授权", 400, 400001);
		}
		String code = String.valueOf(merged.get("code")).trim();
		WechatAuth row = wechatAuthMapper.selectById(appid);
		String secret = "";
		if (row != null && row.getAuthorizerAppsecret() != null) {
			secret = row.getAuthorizerAppsecret().trim();
		}
		if (row == null || !org.springframework.util.StringUtils.hasText(secret)) {
			throw new ResourceException("当前公众号或小程序未绑定或已解绑，请重新授权", 400, 400001);
		}
		WxappJscode2SessionLenientClient.Jscode2SessionLenientResult sessionResult =
				wxappJscode2SessionLenientClient.jscode2sessionLenient(appid, secret, code);
		if (sessionResult.isWechatBusinessError()) {
			throw new ResourceException("当前公众号或小程序未绑定或已解绑，请重新授权", 400, 400001);
		}
		String openId = sessionResult.getOpenidUnionid().getOrDefault("openid", "").trim();
		if (!org.springframework.util.StringUtils.hasText(openId)) {
			throw new ResourceException("小程序信息错误，请联系供应商！", 400, 400001);
		}
		String unionId = wxappOauthLoginAuthorizePort.findUnionidByWxappOpenId(appid, openId);
		String token = merged.get("token") == null ? "" : String.valueOf(merged.get("token")).trim();
		return wxappOauthLoginAuthorizePort.accessTokenSweep(unionId, token);
	}

	public void authorizeOauthLogin(HttpServletRequest request, Map<String, Object> body) {
		Map<String, Object> merged = WxappOpenIdUnionidService.mergeJsonBodyWithRequestParams(body, request);
		if (!isParamSet(merged, "openid")) {
			if (isParamUnsetAndFalsy(merged, "token")
					|| isParamUnsetAndFalsy(merged, "code")
					|| isParamUnsetAndFalsy(merged, "appid")) {
				throw new ResourceException("授权失败");
			}
			String appid = String.valueOf(merged.get("appid")).trim();
			String code = String.valueOf(merged.get("code")).trim();
			WechatAuth row = wechatAuthMapper.selectById(appid);
			String secret = (row != null && StringUtils.hasText(row.getAuthorizerAppsecret()))
					? row.getAuthorizerAppsecret().trim()
					: "";
			if (!StringUtils.hasText(secret)) {
				throw new ResourceException("授权失败");
			}
			WxappJscode2SessionLenientClient.Jscode2SessionLenientResult sessionResult =
					wxappJscode2SessionLenientClient.jscode2sessionLenient(appid, secret, code);
			if (sessionResult.isWechatBusinessError()) {
				throw new ResourceException("当前公众号或小程序未绑定或已解绑，请重新授权", 400, 400001);
			}
			String openId = sessionResult.getOpenidUnionid().getOrDefault("openid", "");
			if (!StringUtils.hasText(openId)) {
				throw new ResourceException("小程序信息错误，请联系供应商！");
			}
			merged.put("open_id", openId);
		} else {
			merged.put("open_id", firstNonBlank(merged.get("open_id"), merged.get("openid")));
		}
		wxappOauthLoginAuthorizePort.accessTokenAuthorize(merged);
	}

	public Map<String, Object> validOauthLogin(String token) {
		return wxappOauthLoginAuthorizePort.validOauthLogin(token);
	}

	/**
	 * 合并参数 Map 上「键已给出且非 null」判断（JSON / 绑定层显式 null 视为未设置）。
	 */
	private static boolean isAppidEmptyForCheckOauthLogin(Object value) {
		if (value == null) {
			return true;
		}
		if (value instanceof String s) {
			return s.isEmpty() || "0".equals(s);
		}
		String t = String.valueOf(value).trim();
		return t.isEmpty() || "0".equals(t);
	}

	private static boolean isParamSet(Map<String, Object> m, String key) {
		return m.containsKey(key) && m.get(key) != null;
	}

	/**
	 * 换票分支 token/code/appid 校验：参数未设置（键缺失或值为 null）且取值为假时成立；键存在且非 null 时不会因空串单独触发。
	 */
	private static boolean isParamUnsetAndFalsy(Map<String, Object> m, String key) {
		return !isParamSet(m, key) && isFalsyParamValue(m.get(key));
	}

	private static boolean isFalsyParamValue(Object v) {
		if (v == null) {
			return true;
		}
		if (v instanceof Boolean b) {
			return !b;
		}
		if (v instanceof Number n) {
			return n.doubleValue() == 0.0d;
		}
		String s = String.valueOf(v);
		return s.isEmpty() || "0".equals(s);
	}

	private static String firstNonBlank(Object a, Object b) {
		String sa = a == null ? "" : String.valueOf(a).trim();
		if (!sa.isEmpty()) {
			return sa;
		}
		return b == null ? "" : String.valueOf(b).trim();
	}
}
