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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaExceptions;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaMaRuntime;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import me.chanjar.weixin.common.error.WxErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxaUnlimitedQrcodeClient {

	private static final Logger log = LoggerFactory.getLogger(WxaUnlimitedQrcodeClient.class);

	private static final int DEFAULT_WIDTH_PX = 430;

	private final WxJavaMaRuntime wxJavaMaRuntime;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public WxaUnlimitedQrcodeClient(WxJavaMaRuntime wxJavaMaRuntime) {
		this.wxJavaMaRuntime = wxJavaMaRuntime;
	}

	public byte[] getUnlimitedCodeBytes(String authorizerAppid, String scene, String page) {
		return getUnlimitedCodeBytes(authorizerAppid, scene, page, null);
	}

	public byte[] getUnlimitedCodeBytes(String authorizerAppid, String scene, String page, Integer width) {
		if (!StringUtils.hasText(authorizerAppid)) {
			throw new ResourceException("小程序 AppId 无效");
		}
		if (!StringUtils.hasText(page)) {
			throw new ResourceException("小程序码 page 无效");
		}
		if (scene == null) {
			throw new ResourceException("小程序码 scene 无效");
		}
		int widthPx = (width != null && width > 0) ? width : DEFAULT_WIDTH_PX;
		Map<String, Object> bodyForLog = new LinkedHashMap<>();
		bodyForLog.put("page", page);
		bodyForLog.put("scene", scene);
		bodyForLog.put("width", widthPx);
		bodyForLog.put("auto_color", Boolean.TRUE);
		logWeChatRequest(authorizerAppid, bodyForLog);
		byte[] raw;
		try {
			raw =
					wxJavaMaRuntime
							.ma(authorizerAppid)
							.getQrcodeService()
							.createWxaCodeUnlimitBytes(
									scene,
									page,
									false,
									"release",
									widthPx,
									true,
									null,
									false);
		} catch (WxErrorException e) {
			throw mapWxError(e);
		}
		logWeChatResponse(raw);
		if (raw == null || raw.length == 0) {
			throw new ResourceException("获取小程序码失败：响应为空");
		}
		throwIfJsonErrorPayload(raw);
		return raw;
	}

	private ResourceException mapWxError(WxErrorException e) {
		if (e.getError() != null && e.getError().getErrorCode() == 41030) {
			return new ResourceException("小程序页面尚未发布或未通过审核");
		}
		return WxJavaExceptions.toResource(e);
	}

	private void throwIfJsonErrorPayload(byte[] raw) {
		if (!looksLikeJsonError(raw)) {
			return;
		}
		JsonNode root;
		try {
			root = objectMapper.readTree(raw);
		} catch (Exception e) {
			throw new ResourceException("获取小程序码失败");
		}
		int err = root.path("errcode").asInt(-1);
		if (err == 0) {
			return;
		}
		if (err == 41030) {
			throw new ResourceException("小程序页面尚未发布或未通过审核");
		}
		String msg = root.path("errmsg").asText("获取小程序码失败");
		throw new ResourceException(msg);
	}

	private static boolean looksLikeJsonError(byte[] raw) {
		for (int i = 0; i < raw.length; i++) {
			byte b = raw[i];
			if (b == ' ' || b == '\n' || b == '\r' || b == '\t') {
				continue;
			}
			return b == '{';
		}
		return false;
	}

	private void logWeChatRequest(String authorizerAppid, Map<String, Object> body) {
		String bodyJson;
		try {
			bodyJson = objectMapper.writeValueAsString(body);
		} catch (JsonProcessingException e) {
			bodyJson = String.valueOf(body);
		}
		log.info(
				"WeChat getwxacodeunlimit request: authorizerAppid={}, accessToken={}, bodyJson={}",
				authorizerAppid,
				"(via WxJava)",
				bodyJson);
	}

	private void logWeChatResponse(byte[] raw) {
		log.info(
				"WeChat getwxacodeunlimit response: {}",
				describeResponseBody(raw));
	}

	private String describeResponseBody(byte[] raw) {
		if (raw == null) {
			return "body=(null)";
		}
		if (raw.length == 0) {
			return "bodyBytes=0, body=(empty)";
		}
		if (looksLikeJsonError(raw)) {
			String s = new String(raw, StandardCharsets.UTF_8);
			String truncated = s.length() > 8000 ? s.substring(0, 8000) + "...(truncated)" : s;
			return "bodyBytes=" + raw.length + ", bodyJson=" + truncated;
		}
		int headLen = Math.min(32, raw.length);
		return "bodyBytes="
				+ raw.length
				+ ", binaryHeadHex="
				+ hexPrefix(raw, headLen)
				+ " (success image payload)";
	}

	private static String hexPrefix(byte[] raw, int maxBytes) {
		int n = Math.min(maxBytes, raw.length);
		StringBuilder sb = new StringBuilder(n * 2);
		for (int i = 0; i < n; i++) {
			sb.append(String.format("%02x", raw[i]));
		}
		return sb.toString();
	}
}
