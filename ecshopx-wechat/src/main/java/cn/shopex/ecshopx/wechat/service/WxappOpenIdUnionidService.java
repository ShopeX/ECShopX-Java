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
import cn.shopex.ecshopx.common.web.FlexibleHttpServletParameterMap;
import cn.shopex.ecshopx.common.web.H5FrontAuthAttributes;
import cn.shopex.ecshopx.wechat.domain.WechatAuth;
import cn.shopex.ecshopx.wechat.mapper.WechatAuthMapper;
import cn.shopex.ecshopx.wechat.wxa.WxappJscode2SessionLenientClient;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappOpenIdUnionidService {

	private final WechatAuthMapper wechatAuthMapper;
	private final WxappJscode2SessionLenientClient jscode2SessionLenientClient;

	public WxappOpenIdUnionidService(
			WechatAuthMapper wechatAuthMapper,
			WxappJscode2SessionLenientClient jscode2SessionLenientClient) {
		this.wechatAuthMapper = wechatAuthMapper;
		this.jscode2SessionLenientClient = jscode2SessionLenientClient;
	}

	public LinkedHashMap<String, Object> getUserOpentIdAndUnionid(HttpServletRequest request, Map<String, Object> body) {
		Map<String, Object> authClaims = readH5AuthClaimsMap(request);
		if (hasNonEmptyAuthClaims(authClaims)) {
			String openId = java.util.Objects.toString(authClaims.get("open_id"), "");
			String unionId = java.util.Objects.toString(authClaims.get("unionid"), "");
			return buildOpenidUnionidDataMap(openId, unionId);
		}
		Map<String, Object> params = mergeJsonBodyWithRequestParams(body, request);
		Object appidRaw = params.get("appid");
		Object codeRaw = params.get("code");
		if (isMissingOrEmptyParam(appidRaw)) {
			throw new BadRequestException("缺少appid");
		}
		if (isMissingOrEmptyParam(codeRaw)) {
			throw new BadRequestException("缺少code");
		}
		String appid = String.valueOf(appidRaw).trim();
		String code = String.valueOf(codeRaw).trim();
		String secret = resolveAuthorizerAppsecretForJscode2session(appid);
		if (!StringUtils.hasText(secret)) {
			throw new ResourceException("当前公众号或小程序未绑定或已解绑，请重新授权", 400, 400001);
		}
		WxappJscode2SessionLenientClient.Jscode2SessionLenientResult sessionResult =
				jscode2SessionLenientClient.jscode2sessionLenient(appid, secret, code);
		if (sessionResult.isWechatBusinessError()) {
			throw new ResourceException("当前公众号或小程序未绑定或已解绑，请重新授权", 400, 400001);
		}
		Map<String, String> sessionMap = sessionResult.getOpenidUnionid();
		String openId = sessionMap.getOrDefault("openid", "");
		String unionId = sessionMap.getOrDefault("unionid", "");
		return buildOpenidUnionidDataMap(openId, unionId);
	}

	/**
	 * Query/Body 合并规则：先 body 再 {@link FlexibleHttpServletParameterMap#toObjectMap(HttpServletRequest)}，
	 * 后者覆盖同名键。
	 */
	public static Map<String, Object> mergeJsonBodyWithRequestParams(Map<String, Object> body, HttpServletRequest request) {
		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		if (body != null && !body.isEmpty()) {
			merged.putAll(body);
		}
		merged.putAll(FlexibleHttpServletParameterMap.toObjectMap(request));
		return merged;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> readH5AuthClaimsMap(HttpServletRequest request) {
		Object raw = request.getAttribute(H5FrontAuthAttributes.H5_AUTH_CLAIMS);
		if (raw instanceof Map<?, ?> m) {
			return (Map<String, Object>) m;
		}
		return Collections.emptyMap();
	}

	private static boolean hasNonEmptyAuthClaims(Map<String, Object> authClaims) {
		return authClaims != null && !authClaims.isEmpty();
	}

	private static boolean isMissingOrEmptyParam(Object value) {
		if (value == null) {
			return true;
		}
		if (value instanceof String s) {
			return s.isEmpty() || "0".equals(s);
		}
		return false;
	}

	private String resolveAuthorizerAppsecretForJscode2session(String appid) {
		WechatAuth row = wechatAuthMapper.selectById(appid);
		if (row == null || !StringUtils.hasText(row.getAuthorizerAppsecret())) {
			return "";
		}
		return row.getAuthorizerAppsecret().trim();
	}

	private static LinkedHashMap<String, Object> buildOpenidUnionidDataMap(String openid, String unionid) {
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("openid", openid == null ? "" : openid);
		data.put("unionid", unionid == null ? "" : unionid);
		return data;
	}
}
