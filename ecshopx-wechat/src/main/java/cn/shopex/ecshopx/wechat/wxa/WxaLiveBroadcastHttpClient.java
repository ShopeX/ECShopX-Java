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

import static cn.binarywang.wx.miniapp.constant.WxMaApiUrlConstants.Broadcast.Room.GET_LIVE_INFO;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaExceptions;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaMaRuntime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxaLiveBroadcastHttpClient {

	private final WxJavaMaRuntime wxJavaMaRuntime;
	private final ObjectMapper objectMapper;

	public WxaLiveBroadcastHttpClient(WxJavaMaRuntime wxJavaMaRuntime, ObjectMapper objectMapper) {
		this.wxJavaMaRuntime = wxJavaMaRuntime;
		this.objectMapper = objectMapper;
	}

	public JsonNode fetchLiveInfoRoot(
			String authorizerAppid, int start, int limit, String action, String roomid) {
		Map<String, Object> body = new HashMap<>();
		body.put("start", start);
		body.put("limit", limit);
		if (action != null && "get_replay".equals(action.trim())) {
			body.put("action", "get_replay");
			String rid = roomid == null ? "" : roomid.trim();
			if (StringUtils.hasText(rid)) {
				body.put("room_id", Long.parseLong(rid));
			}
		}
		String json;
		try {
			json = objectMapper.writeValueAsString(body);
		} catch (Exception e) {
			throw new ResourceException("请求体序列化失败");
		}
		try {
			String resp = wxJavaMaRuntime.ma(authorizerAppid.trim()).post(GET_LIVE_INFO, json);
			JsonNode root = objectMapper.readTree(resp);
			if (root.path("errcode").asInt(0) != 0) {
				throw new ResourceException(root.path("errmsg").asText("微信直播接口错误"));
			}
			return root;
		} catch (WxErrorException e) {
			throw WxJavaExceptions.toResource(e);
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("微信接口返回无法解析");
		}
	}
}
