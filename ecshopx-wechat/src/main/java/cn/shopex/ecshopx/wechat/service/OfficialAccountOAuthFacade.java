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
import cn.shopex.ecshopx.wechat.wxa.WechatOpenPlatformAuthorizerTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import me.chanjar.weixin.mp.enums.WxMpApiUrl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service
public class OfficialAccountOAuthFacade {

	private static final String MODE_AUTHORIZED = "authorized";
	private static final String MODE_DIRECT = "direct";

	private static final String OAUTH2_AUTHORIZE_PAGE = "https://open.weixin.qq.com/connect/oauth2/authorize";

	private final WechatOpenPlatformAuthorizerTokenService tokenService;
	private final ObjectMapper objectMapper;
	private final RestTemplate restTemplate = new RestTemplate();

	public OfficialAccountOAuthFacade(
			WechatOpenPlatformAuthorizerTokenService tokenService, ObjectMapper objectMapper) {
		this.tokenService = tokenService;
		this.objectMapper = objectMapper;
	}

	/**
	 * 仅拼接公众号网页授权跳转 URL（{@code snsapi_userinfo}），不请求微信接口。
	 *
	 * @param woaApp {@link OpenPlatformWoaFacade#getWoaApp} 返回值（{@code mode} 为 authorized 或 direct）
	 * @param redirectUri 授权完成后的回调地址
	 * @return 完整授权 URL；入参不足以拼链时返回空串
	 */
	public String buildSnsapiUserinfoAuthorizeUrl(Map<String, Object> woaApp, String redirectUri) {
		return buildOAuth2AuthorizePageUrl(woaApp, redirectUri, "snsapi_userinfo");
	}

	/**
	 * 仅拼接公众号网页授权跳转 URL（{@code snsapi_base}），不请求微信接口。
	 *
	 * @param woaApp {@code mode} 为 {@code authorized} 或 {@code direct} 的上下文（与 {@link #buildSnsapiUserinfoAuthorizeUrl} 相同结构）
	 * @param redirectUri 授权完成后的回调地址
	 * @return 完整授权 URL；入参不足以拼链时返回空串
	 */
	public String buildSnsapiBaseAuthorizeUrl(Map<String, Object> woaApp, String redirectUri) {
		return buildOAuth2AuthorizePageUrl(woaApp, redirectUri, "snsapi_base");
	}

	private String buildOAuth2AuthorizePageUrl(Map<String, Object> woaApp, String redirectUri, String scope) {
		if (!StringUtils.hasText(redirectUri)) {
			return "";
		}
		if (woaApp == null || woaApp.isEmpty()) {
			return "";
		}
		String mode = woaApp.get("mode") == null ? "" : String.valueOf(woaApp.get("mode"));
		String state = UUID.randomUUID().toString();
		String trimmedRedirect = redirectUri.trim();
		String encRedirect = formUrlEncode(trimmedRedirect);
		String encScope = formUrlEncode(scope);
		String encState = formUrlEncode(state);
		if (MODE_AUTHORIZED.equals(mode)) {
			String appid = woaApp.get("authorizerAppid") == null ? "" : String.valueOf(woaApp.get("authorizerAppid")).trim();
			String componentAppid = tokenService.getOpenPlatformComponentAppIdForOAuthAuthorizeUrl();
			if (!StringUtils.hasText(appid) || !StringUtils.hasText(componentAppid)) {
				return "";
			}
			String encAppid = formUrlEncode(appid);
			String encComponent = formUrlEncode(componentAppid.trim());
			return OAUTH2_AUTHORIZE_PAGE
					+ "?appid="
					+ encAppid
					+ "&redirect_uri="
					+ encRedirect
					+ "&response_type=code&scope="
					+ encScope
					+ "&component_appid="
					+ encComponent
					+ "&state="
					+ encState
					+ "&connect_redirect=1#wechat_redirect";
		}
		if (MODE_DIRECT.equals(mode)) {
			String appid = stringField(woaApp, "app_id");
			if (!StringUtils.hasText(appid)) {
				return "";
			}
			String encAppid = formUrlEncode(appid);
			return OAUTH2_AUTHORIZE_PAGE
					+ "?appid="
					+ encAppid
					+ "&redirect_uri="
					+ encRedirect
					+ "&response_type=code&scope="
					+ encScope
					+ "&state="
					+ encState
					+ "&connect_redirect=1#wechat_redirect";
		}
		return "";
	}

	/** application/x-www-form-urlencoded；空格为 {@code %20}，{@code redirect_uri} 须整段编码。 */
	private static String formUrlEncode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
	}

	/**
	 * 使用网页授权 {@code code} 换取 {@code openid}（仅 sns/oauth2 换票，不请求 userinfo）。
	 *
	 * @param woaApp 与 {@link #getUserInfoByCode} 相同的 WOA 上下文
	 * @param code 前置条件：调用方已保证非 {@code null}、非空串、且不为字面量 {@code "0"}
	 * @return 非空 openid（已 trim）
	 */
	public String getOpenIdByOAuthCode(Object woaApp, String code) {
		if (!(woaApp instanceof Map<?, ?> raw)) {
			throw new ResourceException("公众号信息有误！");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> woa = (Map<String, Object>) raw;
		JsonNode tokenNode = exchangeSnsOAuthTokenForCode(woa, code);
		String openid = textOrEmpty(tokenNode, "openid");
		if (!StringUtils.hasText(openid)) {
			throwOAuthTokenFailure(tokenNode);
		}
		return openid;
	}

	public Map<String, Object> getUserInfoByCode(Object woaApp, String code) {
		if (!StringUtils.hasText(code)) {
			throw new ResourceException("该账号不在店务应用可见范围内");
		}
		if (!(woaApp instanceof Map<?, ?> raw)) {
			throw new ResourceException("公众号信息有误！");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> woa = (Map<String, Object>) raw;
		JsonNode tokenNode = exchangeSnsOAuthTokenForCode(woa, code);
		String oauthAccessToken = textOrEmpty(tokenNode, "access_token");
		String openidFromToken = textOrEmpty(tokenNode, "openid");
		if (!StringUtils.hasText(oauthAccessToken) || !StringUtils.hasText(openidFromToken)) {
			throwOAuthTokenFailure(tokenNode);
		}
		Object modeObj = woa.get("mode");
		String mode = modeObj != null ? String.valueOf(modeObj) : "";

		WxMpApiUrl.OAuth2 userinfo = WxMpApiUrl.OAuth2.OAUTH2_USERINFO_URL;
		String userinfoUrl =
				userinfo.getPrefix()
						+ String.format(userinfo.getPath(), oauthAccessToken, openidFromToken, "zh_CN");
		JsonNode user = fetchJson(userinfoUrl);
		String openid = textOrEmpty(user, "openid");
		if (!StringUtils.hasText(openid)) {
			throwOAuthTokenFailure(user);
		}
		String unionid = textOrEmpty(user, "unionid");
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("openid", openid);
		out.put("unionid", unionid);
		if (MODE_AUTHORIZED.equals(mode)) {
			out.put("appid", stringField(woa, "authorizerAppid"));
		} else {
			out.put("appid", stringField(woa, "app_id"));
		}
		return out;
	}

	private JsonNode exchangeSnsOAuthTokenForCode(Map<String, Object> woa, String code) {
		Object modeObj = woa.get("mode");
		String mode = modeObj != null ? String.valueOf(modeObj) : "";
		if (MODE_AUTHORIZED.equals(mode)) {
			Object aid = woa.get("authorizerAppid");
			String authorizerAppid = aid != null ? String.valueOf(aid).trim() : "";
			if (!StringUtils.hasText(authorizerAppid)) {
				throw new ResourceException("公众号信息有误！");
			}
			String componentToken = tokenService.getOpenPlatformComponentAccessToken();
			String componentAppId = tokenService.getOpenPlatformComponentAppId();
			String accessTokenUrl =
					String.format(
									me.chanjar.weixin.open.api.WxOpenComponentService.OAUTH2_ACCESS_TOKEN_URL,
									authorizerAppid,
									code.trim(),
									componentAppId)
							+ "&component_access_token="
							+ URLEncoder.encode(componentToken, StandardCharsets.UTF_8);
			return fetchJson(accessTokenUrl);
		}
		if (MODE_DIRECT.equals(mode)) {
			String appId = stringField(woa, "app_id");
			String secret = stringField(woa, "secret");
			if (!StringUtils.hasText(appId) || !StringUtils.hasText(secret)) {
				throw new ResourceException("公众号信息有误！");
			}
			WxMpApiUrl.OAuth2 u = WxMpApiUrl.OAuth2.OAUTH2_ACCESS_TOKEN_URL;
			String accessTokenUrl =
					u.getPrefix() + String.format(u.getPath(), appId.trim(), secret.trim(), code.trim());
			return fetchJson(accessTokenUrl);
		}
		throw new ResourceException("公众号信息有误！");
	}

	private static void throwOAuthTokenFailure(JsonNode node) {
		int err = node.path("errcode").asInt(0);
		String msg = textOrEmpty(node, "errmsg");
		if (err != 0 && StringUtils.hasText(msg)) {
			throw new ResourceException(msg);
		}
		throw new ResourceException("该账号不在店务应用可见范围内");
	}

	private JsonNode fetchJson(String url) {
		try {
			ResponseEntity<byte[]> resp = restTemplate.getForEntity(url, byte[].class);
			if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
				throw new ResourceException("微信接口调用失败");
			}
			return objectMapper.readTree(new String(resp.getBody(), StandardCharsets.UTF_8));
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("微信接口调用失败");
		}
	}

	private static String textOrEmpty(JsonNode node, String field) {
		JsonNode v = node.get(field);
		return v == null || v.isNull() ? "" : v.asText("").trim();
	}

	private static String stringField(Map<String, Object> woa, String key) {
		Object v = woa.get(key);
		return v != null ? String.valueOf(v).trim() : "";
	}
}
