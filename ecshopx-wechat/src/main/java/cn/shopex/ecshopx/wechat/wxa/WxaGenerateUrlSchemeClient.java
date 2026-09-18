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

package cn.shopex.ecshopx.wechat.wxa;

import cn.binarywang.wx.miniapp.bean.scheme.WxMaGenerateSchemeRequest;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaExceptions;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaMaRuntime;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WxaGenerateUrlSchemeClient {

	private static final String BINDING_MSG = "当前公众号或小程序未绑定或已解绑，请重新授权";

	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

	private final WxJavaMaRuntime wxJavaMaRuntime;
	private final ObjectMapper objectMapper;

	public WxaGenerateUrlSchemeClient(WxJavaMaRuntime wxJavaMaRuntime, ObjectMapper objectMapper) {
		this.wxJavaMaRuntime = wxJavaMaRuntime;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> generate(
			String authorizerAppId,
			String path,
			String queryForWechat,
			String envVersion,
			long expireTimeEpochSeconds) {
		WxMaGenerateSchemeRequest.JumpWxa jump =
				WxMaGenerateSchemeRequest.JumpWxa.newBuilder()
						.path(path)
						.query(queryForWechat == null ? "" : queryForWechat)
						.envVersion(envVersion)
						.build();
		WxMaGenerateSchemeRequest req =
				WxMaGenerateSchemeRequest.newBuilder()
						.jumpWxa(jump)
						.expireType(0)
						.expireTime(expireTimeEpochSeconds)
						.build();
		String responseBody;
		try {
			responseBody = wxJavaMaRuntime.ma(authorizerAppId).getWxMaSchemeService().generate(req);
		} catch (WxErrorException e) {
			throw new ResourceException(BINDING_MSG, 400, 400001);
		}

		if (!StringUtils.hasText(responseBody)) {
			throw new ResourceException(BINDING_MSG, 400, 400001);
		}

		JsonNode root;
		try {
			root = objectMapper.readTree(responseBody);
		} catch (Exception e) {
			throw new ResourceException(BINDING_MSG, 400, 400001);
		}

		if (root.hasNonNull("errcode") && root.get("errcode").asInt(0) != 0) {
			String msg = root.path("errmsg").asText("");
			if (!StringUtils.hasText(msg)) {
				msg = "生成小程序 URL Scheme 失败";
			}
			throw new ResourceException(msg, 400, 400001);
		}

		try {
			return objectMapper.readValue(responseBody, MAP_TYPE);
		} catch (Exception e) {
			throw new ResourceException(BINDING_MSG, 400, 400001);
		}
	}
}
