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
import cn.shopex.ecshopx.wechat.wxjava.WxJavaMpRuntime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.open.api.WxOpenComponentService;
import org.springframework.stereotype.Service;

@Service
public class WechatOpenPlatformAccountClient {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final WxJavaMpRuntime wxJavaMpRuntime;

	public WechatOpenPlatformAccountClient(WxJavaMpRuntime wxJavaMpRuntime) {
		this.wxJavaMpRuntime = wxJavaMpRuntime;
	}

	public JsonNode openGet(String accessToken, String appid) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("appid", appid == null ? "" : appid);
		return postWithToken(WxOpenComponentService.GET_OPEN_URL, accessToken, body);
	}

	public JsonNode openCreate(String accessToken, String appid) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("appid", appid == null ? "" : appid);
		return postWithToken(WxOpenComponentService.CREATE_OPEN_URL, accessToken, body);
	}

	public JsonNode openBind(String accessToken, String appid, String openAppid) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("appid", appid == null ? "" : appid);
		body.put("open_appid", openAppid == null ? "" : openAppid);
		return postWithToken(WxOpenComponentService.BIND_OPEN_URL, accessToken, body);
	}

	public JsonNode openUnbind(String accessToken, String appid, String openAppid) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("appid", appid == null ? "" : appid);
		body.put("open_appid", openAppid == null ? "" : openAppid);
		return postWithToken(WxOpenComponentService.UNBIND_OPEN_URL, accessToken, body);
	}

	private JsonNode postWithToken(String apiUrl, String accessToken, Map<String, Object> body) {
		try {
			String raw = wxJavaMpRuntime.mpBearer(accessToken).post(apiUrl, body);
			return parseJsonBody(raw.getBytes(StandardCharsets.UTF_8));
		} catch (WxErrorException e) {
			throw WxJavaExceptions.toResource(e);
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("微信开放平台请求失败");
		}
	}

	private JsonNode parseJsonBody(byte[] raw) {
		if (raw == null || raw.length == 0) {
			throw new ResourceException("微信开放平台请求失败");
		}
		try {
			JsonNode root = objectMapper.readTree(new String(raw, StandardCharsets.UTF_8));
			if (root.isMissingNode() || !root.isObject()) {
				throw new ResourceException("微信开放平台请求失败");
			}
			return root;
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("微信开放平台请求失败");
		}
	}
}
