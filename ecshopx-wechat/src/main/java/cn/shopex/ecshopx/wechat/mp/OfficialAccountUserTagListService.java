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

package cn.shopex.ecshopx.wechat.mp;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaMpRuntime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.enums.WxMpApiUrl;
import org.springframework.stereotype.Service;

@Service
public class OfficialAccountUserTagListService {

	private final WxJavaMpRuntime wxJavaMpRuntime;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public OfficialAccountUserTagListService(WxJavaMpRuntime wxJavaMpRuntime) {
		this.wxJavaMpRuntime = wxJavaMpRuntime;
	}

	/**
	 * 调用微信公众平台 tags/get，返回标签列表。
	 *
	 * @return 每条含键 {@code id}（Long）、{@code name}（String）
	 */
	public List<Map<String, Object>> listTags(String authorizerAppid) {
		if (authorizerAppid == null || authorizerAppid.isEmpty()) {
			throw new BadRequestException("当前账号未绑定公众号或小程序，请先授权绑定", 400);
		}
		JsonNode root;
		try {
			String raw =
					wxJavaMpRuntime.mp(authorizerAppid).get(WxMpApiUrl.UserTag.TAGS_GET, "");
			root = objectMapper.readTree(raw.getBytes(StandardCharsets.UTF_8));
		} catch (WxErrorException e) {
			throw new BadRequestException("当前账号未绑定公众号或小程序，请先授权绑定", 400);
		} catch (Exception e) {
			throw new ResourceException("微信接口返回无法解析");
		}
		if (root.hasNonNull("errcode") && root.get("errcode").asInt() != 0) {
			String errmsg = root.path("errmsg").asText("");
			throw new ResourceException(
					(errmsg != null && !errmsg.isEmpty()) ? errmsg : "微信获取标签失败");
		}
		JsonNode tagsNode = root.get("tags");
		if (tagsNode == null || tagsNode.isNull() || !tagsNode.isArray()) {
			throw new ResourceException("微信获取标签失败");
		}

		List<Map<String, Object>> out = new ArrayList<>();
		for (JsonNode el : tagsNode) {
			if (el == null || el.isNull() || !el.hasNonNull("id")) {
				continue;
			}
			JsonNode idNode = el.get("id");
			Long tagId;
			if (idNode.isNumber()) {
				tagId = idNode.asLong();
			} else {
				try {
					tagId = Long.parseLong(idNode.asText());
				} catch (NumberFormatException e) {
					continue;
				}
			}
			String name = el.path("name").asText("");
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("id", tagId);
			row.put("name", name);
			out.add(row);
		}
		return out;
	}
}
