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
import cn.shopex.ecshopx.wechat.wxjava.WxJavaMpRuntime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.enums.WxMpApiUrl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WechatOfficialAccountMaterialStatsService {

	private final WxJavaMpRuntime wxJavaMpRuntime;
	private final ObjectMapper objectMapper;

	public WechatOfficialAccountMaterialStatsService(
			WxJavaMpRuntime wxJavaMpRuntime, ObjectMapper objectMapper) {
		this.wxJavaMpRuntime = wxJavaMpRuntime;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getMaterialStats(String authorizerAppId) {
		String appId = authorizerAppId == null ? "" : authorizerAppId.trim();
		String body;
		try {
			body = wxJavaMpRuntime.mp(appId).get(WxMpApiUrl.Material.MATERIAL_GET_COUNT_URL, "");
		} catch (WxErrorException e) {
			throw new ResourceException(resolveWechatFailureMessage(e));
		}
		if (!StringUtils.hasText(body)) {
			throw new ResourceException("微信接口调用失败");
		}
		final JsonNode root;
		try {
			root = objectMapper.readTree(body);
		} catch (IOException e) {
			throw new ResourceException("微信接口调用失败");
		}
		if (root.hasNonNull("errcode") && root.get("errcode").asInt() != 0) {
			String msg = root.path("errmsg").asText("");
			if (!StringUtils.hasText(msg)) {
				msg = "微信接口错误";
			}
			throw new ResourceException(msg);
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		for (Iterator<Map.Entry<String, JsonNode>> it = root.fields(); it.hasNext(); ) {
			Map.Entry<String, JsonNode> e = it.next();
			String key = e.getKey();
			if ("errcode".equals(key) || "errmsg".equals(key)) {
				continue;
			}
			out.put(key, objectMapper.convertValue(e.getValue(), Object.class));
		}
		out.put(
				"image_limit",
				out.containsKey("image_count")
						? Integer.valueOf(5000 - parseCountForLimit(out.get("image_count")))
						: Integer.valueOf(5000));
		out.put(
				"news_limit",
				out.containsKey("news_count")
						? Integer.valueOf(5000 - parseCountForLimit(out.get("news_count")))
						: Integer.valueOf(5000));
		out.put(
				"video_limit",
				out.containsKey("video_count")
						? Integer.valueOf(1000 - parseCountForLimit(out.get("video_count")))
						: Integer.valueOf(1000));
		return out;
	}

	private static int parseCountForLimit(Object value) {
		long v;
		if (value instanceof Number n) {
			v = n.longValue();
		} else if (value instanceof String s) {
			try {
				v = Long.parseLong(s.trim());
			} catch (NumberFormatException e) {
				throw new ResourceException("微信接口调用失败");
			}
		} else {
			throw new ResourceException("微信接口调用失败");
		}
		if (v < Integer.MIN_VALUE || v > Integer.MAX_VALUE) {
			throw new ResourceException("微信接口调用失败");
		}
		return (int) v;
	}

	private static String resolveWechatFailureMessage(Throwable e) {
		if (e == null) {
			return "微信接口调用失败";
		}
		String m = e.getMessage();
		return StringUtils.hasText(m) ? m : "微信接口调用失败";
	}
}
