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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.enums.WxMpApiUrl;

@Service
public class WechatOfficialAccountMaterialListService {

	private final WxJavaMpRuntime wxJavaMpRuntime;
	private final WechatOfficialAccountPermanentMaterialClient wechatOfficialAccountPermanentMaterialClient;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public WechatOfficialAccountMaterialListService(
			WxJavaMpRuntime wxJavaMpRuntime,
			WechatOfficialAccountPermanentMaterialClient wechatOfficialAccountPermanentMaterialClient) {
		this.wxJavaMpRuntime = wxJavaMpRuntime;
		this.wechatOfficialAccountPermanentMaterialClient = wechatOfficialAccountPermanentMaterialClient;
	}

	public Object getNewsMaterial(String authorizerAppId, String materialId) {
		return wechatOfficialAccountPermanentMaterialClient.fetchPermanentMaterialForReply(authorizerAppId, materialId);
	}

	public Object getMaterialLists(String authorizerAppId, String type, int page, int pageSize) {
		if (pageSize > 20) {
			return Collections.emptyList();
		}
		try {
			LinkedHashMap<String, Object> req = new LinkedHashMap<>();
			req.put("type", type == null ? "" : type);
			req.put("offset", (page - 1) * pageSize);
			req.put("count", pageSize);
			String rawStr =
					wxJavaMpRuntime
							.mp(authorizerAppId.trim())
							.post(WxMpApiUrl.Material.MATERIAL_BATCHGET_URL, req);
			byte[] raw = rawStr.getBytes(StandardCharsets.UTF_8);
			if (raw == null || raw.length == 0) {
				return Collections.emptyList();
			}
			JsonNode root = objectMapper.readTree(new String(raw, StandardCharsets.UTF_8));
			if (root.hasNonNull("errcode") && root.get("errcode").asInt() != 0) {
				return Collections.emptyList();
			}
			JsonNode itemNode = root.path("item");
			List<Map<String, Object>> items = new ArrayList<>();
			if (itemNode.isArray()) {
				for (JsonNode el : itemNode) {
					if (el.isObject()) {
						items.add(objectMapper.convertValue(el, new TypeReference<LinkedHashMap<String, Object>>() {}));
					}
				}
			}
			LinkedHashMap<String, Object> out = new LinkedHashMap<>();
			out.put("item", items);
			if (root.has("total_count")) {
				out.put("total_count", objectMapper.convertValue(root.get("total_count"), Number.class));
			}
			int itemCount = root.has("item_count") ? root.get("item_count").asInt() : items.size();
			out.put("item_count", itemCount);
			if ("video".equals(type)) {
				for (Map<String, Object> itemMap : items) {
					Object rawMid = itemMap.get("media_id");
					String mediaId = rawMid == null ? "" : String.valueOf(rawMid).trim();
					String urlVal;
					String desc;
					if (mediaId.isEmpty()) {
						urlVal = "";
						desc = "";
					} else {
						Object detail = wechatOfficialAccountPermanentMaterialClient.fetchPermanentMaterialForReply(
								authorizerAppId, mediaId);
						if (detail instanceof Map<?, ?> d) {
							urlVal = d.get("down_url") != null ? String.valueOf(d.get("down_url")) : "";
							desc = d.get("description") != null ? String.valueOf(d.get("description")) : "";
						} else {
							urlVal = "";
							desc = "";
						}
					}
					itemMap.put("url", urlVal);
					itemMap.put("desc", desc);
				}
			}
			return out;
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}
}
