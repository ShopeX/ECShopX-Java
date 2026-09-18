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

package cn.shopex.ecshopx.wechat.service.openplatform;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaExceptions;
import cn.shopex.ecshopx.wechat.wxa.WechatOpenPlatformAuthorizerTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.open.api.WxOpenComponentService;
import me.chanjar.weixin.open.api.WxOpenService;
import org.springframework.stereotype.Service;

@Service
public class WechatOpenPlatformQueryAuthClient {

	private final WxOpenService wxOpenService;
	private final WechatOpenPlatformAuthorizerTokenService tokenService;
	private final ObjectMapper objectMapper;

	public WechatOpenPlatformQueryAuthClient(
			WxOpenService wxOpenService,
			WechatOpenPlatformAuthorizerTokenService tokenService,
			ObjectMapper objectMapper) {
		this.wxOpenService = wxOpenService;
		this.tokenService = tokenService;
		this.objectMapper = objectMapper;
	}

	public JsonNode queryAuthByAuthorizationCode(String authorizationCode) {
		String trimmed = authorizationCode == null ? null : authorizationCode.trim();
		try {
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("component_appid", tokenService.getOpenPlatformComponentAppId());
			body.put("authorization_code", trimmed);
			String resp =
					wxOpenService
							.getWxOpenComponentService()
							.post(WxOpenComponentService.API_QUERY_AUTH_URL, objectMapper.writeValueAsString(body));
			JsonNode root = parseJsonBody(resp);
			if (root.hasNonNull("errcode") && root.get("errcode").asInt() != 0) {
				throw new ResourceException(root.path("errmsg").asText("query_auth 失败"));
			}
			return root;
		} catch (WxErrorException e) {
			throw WxJavaExceptions.toResource(e);
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("微信开放平台请求失败");
		}
	}

	public JsonNode getAuthorizerInfo(String authorizerAppid) {
		String trimmed = authorizerAppid == null ? null : authorizerAppid.trim();
		try {
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("component_appid", tokenService.getOpenPlatformComponentAppId());
			body.put("authorizer_appid", trimmed);
			String resp =
					wxOpenService
							.getWxOpenComponentService()
							.post(WxOpenComponentService.API_GET_AUTHORIZER_INFO_URL, objectMapper.writeValueAsString(body));
			JsonNode root = parseJsonBody(resp);
			if (root.hasNonNull("errcode") && root.get("errcode").asInt() != 0) {
				throw new ResourceException(root.path("errmsg").asText("get_authorizer_info 失败"));
			}
			return root;
		} catch (WxErrorException e) {
			throw WxJavaExceptions.toResource(e);
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("微信开放平台请求失败");
		}
	}

	private JsonNode parseJsonBody(String raw) {
		if (raw == null || raw.isEmpty()) {
			throw new ResourceException("微信接口返回为空");
		}
		try {
			JsonNode root = objectMapper.readTree(raw);
			if (root.isMissingNode() || !root.isObject()) {
				throw new ResourceException("微信接口返回无法解析");
			}
			return root;
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("微信接口返回无法解析");
		}
	}
}
