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

import cn.shopex.ecshopx.wechat.wxjava.WxJavaMpRuntime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.enums.WxMpApiUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Sends WeChat official-account (MP) template messages via open-platform authorizer tokens.
 */
@Service
public class WoaMpTemplateMessageClient {

	private static final Logger log = LoggerFactory.getLogger(WoaMpTemplateMessageClient.class);

	private final WxJavaMpRuntime wxJavaMpRuntime;
	private final ObjectMapper objectMapper;

	public WoaMpTemplateMessageClient(WxJavaMpRuntime wxJavaMpRuntime, ObjectMapper objectMapper) {
		this.wxJavaMpRuntime = wxJavaMpRuntime;
		this.objectMapper = objectMapper;
	}

	/**
	 * @param keywordData flat map (e.g. {@code amount6} → display string); wrapped per WeChat {@code data} schema.
	 */
	public void sendTemplateMessage(
			String authorizerAppId, String templateId, String touser, Map<String, Object> keywordData) {
		String appId = authorizerAppId == null ? "" : authorizerAppId.trim();
		if (appId.isEmpty()) {
			return;
		}
		Map<String, Object> data = new LinkedHashMap<>();
		if (keywordData != null) {
			for (Map.Entry<String, Object> e : keywordData.entrySet()) {
				if (e.getKey() == null) {
					continue;
				}
				Map<String, String> item = new LinkedHashMap<>();
				item.put("value", e.getValue() == null ? "" : String.valueOf(e.getValue()));
				data.put(e.getKey(), item);
			}
		}
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("touser", touser);
		body.put("template_id", templateId);
		body.put("data", data);
		JsonNode root = postJsonReturnRoot(appId, body);
		int ec = root.path("errcode").asInt(0);
		if (ec != 0) {
			log.warn(
					"woa template send errcode={} errmsg={} touser={} templateId={}",
					ec,
					root.path("errmsg").asText(""),
					touser,
					templateId);
		}
	}

	private JsonNode postJsonReturnRoot(String authorizerAppId, Map<String, Object> body) {
		try {
			String raw =
					wxJavaMpRuntime.mp(authorizerAppId).post(WxMpApiUrl.TemplateMsg.MESSAGE_TEMPLATE_SEND, body);
			return parseRoot(raw);
		} catch (WxErrorException e) {
			log.warn("woa template send wx failed: {}", e.getMessage());
			return objectMapper.createObjectNode();
		} catch (Exception e) {
			log.warn("woa template send http failed: {}", e.toString());
			return objectMapper.createObjectNode();
		}
	}

	private JsonNode parseRoot(String raw) {
		if (raw == null || raw.isEmpty()) {
			return objectMapper.createObjectNode();
		}
		try {
			return objectMapper.readTree(raw);
		} catch (Exception e) {
			return objectMapper.createObjectNode();
		}
	}
}
