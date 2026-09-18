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

package cn.shopex.ecshopx.goods.service.pointsmall;

import cn.shopex.ecshopx.wechat.wxjava.WxJavaMpRuntime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import me.chanjar.weixin.mp.enums.WxMpApiUrl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 公众号永久素材视频：根据 media_id 拉取微信返回的 {@code down_url}。
 */
@Service
public class PointsmallFrontWechatVideoUrlService {

	private final WxJavaMpRuntime wxJavaMpRuntime;
	private final ObjectMapper objectMapper;

	public PointsmallFrontWechatVideoUrlService(WxJavaMpRuntime wxJavaMpRuntime, ObjectMapper objectMapper) {
		this.wxJavaMpRuntime = wxJavaMpRuntime;
		this.objectMapper = objectMapper;
	}

	/**
	 * @param authorizerAppId 授权方公众号 appid（与前台 JWT {@code woa_appid} 一致）
	 * @param mediaId         素材 media_id
	 * @return {@code down_url}，失败或不可用时返回空串
	 */
	public String resolveVideoDownUrl(String authorizerAppId, String mediaId) {
		if (!StringUtils.hasText(authorizerAppId) || !StringUtils.hasText(mediaId)) {
			return "";
		}
		Map<String, String> body = new LinkedHashMap<>();
		body.put("media_id", mediaId.trim());
		try {
			String raw =
					wxJavaMpRuntime
							.mp(authorizerAppId.trim())
							.post(WxMpApiUrl.Material.MATERIAL_GET_URL, body);
			if (!StringUtils.hasText(raw)) {
				return "";
			}
			JsonNode root = objectMapper.readTree(raw);
			if (root.hasNonNull("errcode") && root.path("errcode").asInt(0) != 0) {
				return "";
			}
			JsonNode down = root.get("down_url");
			if (down != null && down.isTextual()) {
				String u = down.asText();
				return StringUtils.hasText(u) ? u : "";
			}
			return "";
		} catch (Exception e) {
			return "";
		}
	}
}
