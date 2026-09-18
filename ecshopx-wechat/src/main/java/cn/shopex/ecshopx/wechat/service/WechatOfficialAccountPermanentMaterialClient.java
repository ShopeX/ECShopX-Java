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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import me.chanjar.weixin.mp.enums.WxMpApiUrl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Fetches permanent material URLs for公众号图文/视频组件（如商品详情 intro 中的 film 块）。
 */
@Service
public class WechatOfficialAccountPermanentMaterialClient {

	private final WxJavaMpRuntime wxJavaMpRuntime;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public WechatOfficialAccountPermanentMaterialClient(WxJavaMpRuntime wxJavaMpRuntime) {
		this.wxJavaMpRuntime = wxJavaMpRuntime;
	}

	/**
	 * 与现网行为对齐：URL 形素材直接返回；无授权 appid 或拉取失败时返回 empty（不抛异常打断详情）。
	 */
	private static boolean isUrlLikeExternalMaterialId(String raw) {
		if (raw == null) {
			return false;
		}
		String mid = raw.trim();
		if (mid.regionMatches(true, 0, "http://", 0, 7)
				|| mid.regionMatches(true, 0, "https://", 0, 8)
				|| mid.regionMatches(true, 0, "ftp://", 0, 6)) {
			return true;
		}
		return mid.regionMatches(true, 0, "www.", 0, 4);
	}

	public Optional<String> resolveMaterialDownloadUrl(String authorizerAppId, String mediaId) {
		if (!StringUtils.hasText(mediaId)) {
			return Optional.empty();
		}
		String mid = mediaId.trim();
		if (isUrlLikeExternalMaterialId(mid)) {
			return Optional.of(mid);
		}
		if (!StringUtils.hasText(authorizerAppId)) {
			return Optional.empty();
		}
		try {
			Map<String, String> body = new LinkedHashMap<>();
			body.put("media_id", mid);
			String raw = wxJavaMpRuntime
					.mp(authorizerAppId.trim())
					.post(WxMpApiUrl.Material.MATERIAL_GET_URL, body);
			byte[] bytes = raw.getBytes(StandardCharsets.UTF_8);
			if (bytes.length == 0) {
				return Optional.empty();
			}
			JsonNode root = objectMapper.readTree(new String(bytes, StandardCharsets.UTF_8));
			if (root.hasNonNull("errcode") && root.get("errcode").asInt() != 0) {
				return Optional.empty();
			}
			if (root.has("down_url") && root.get("down_url").isTextual()) {
				String u = root.get("down_url").asText();
				return StringUtils.hasText(u) ? Optional.of(u) : Optional.empty();
			}
			if (root.has("url") && root.get("url").isTextual()) {
				String u = root.get("url").asText();
				return StringUtils.hasText(u) ? Optional.of(u) : Optional.empty();
			}
			return Optional.empty();
		} catch (Exception ignored) {
			return Optional.empty();
		}
	}

	public Object fetchPermanentMaterialForReply(String authorizerAppId, String materialIdOrUrl) {
		String mid = materialIdOrUrl == null ? "" : materialIdOrUrl.trim();
		if (isUrlLikeExternalMaterialId(materialIdOrUrl)) {
			LinkedHashMap<String, Object> m = new LinkedHashMap<>();
			m.put("down_url", mid);
			return m;
		}
		if (!StringUtils.hasText(authorizerAppId)) {
			return Collections.emptyList();
		}
		try {
			Map<String, String> body = new LinkedHashMap<>();
			body.put("media_id", mid);
			String raw = wxJavaMpRuntime
					.mp(authorizerAppId.trim())
					.post(WxMpApiUrl.Material.MATERIAL_GET_URL, body);
			byte[] respBody = raw.getBytes(StandardCharsets.UTF_8);
			if (respBody.length == 0) {
				return Collections.emptyList();
			}
			JsonNode root = objectMapper.readTree(new String(respBody, StandardCharsets.UTF_8));
			if (!root.isObject()) {
				return Collections.emptyList();
			}
			if (root.hasNonNull("errcode") && root.get("errcode").asInt() != 0) {
				return Collections.emptyList();
			}
			return objectMapper.convertValue(root, new TypeReference<LinkedHashMap<String, Object>>() {});
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}
}
