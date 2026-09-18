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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaMpRuntime;
import com.fasterxml.jackson.core.type.TypeReference;
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
public class OfficialAccountUserInfoBatchService {

	private final WxJavaMpRuntime wxJavaMpRuntime;
	private final ObjectMapper objectMapper;

	public OfficialAccountUserInfoBatchService(WxJavaMpRuntime wxJavaMpRuntime, ObjectMapper objectMapper) {
		this.wxJavaMpRuntime = wxJavaMpRuntime;
		this.objectMapper = objectMapper;
	}

	public List<Map<String, Object>> batchGetUserInfo(String accessToken, List<String> openIds) {
		if (openIds == null || openIds.isEmpty()) {
			return List.of();
		}
		List<Map<String, String>> userList = new ArrayList<>();
		for (String openId : openIds) {
			Map<String, String> one = new LinkedHashMap<>();
			one.put("openid", openId);
			one.put("lang", "zh_CN");
			userList.add(one);
		}
		Map<String, Object> bodyMap = new LinkedHashMap<>();
		bodyMap.put("user_list", userList);

		JsonNode root;
		try {
			String raw =
					wxJavaMpRuntime.mpBearer(accessToken).post(WxMpApiUrl.User.USER_INFO_BATCH_GET_URL, bodyMap);
			root = objectMapper.readTree(raw.getBytes(StandardCharsets.UTF_8));
		} catch (WxErrorException e) {
			throw new ResourceException("批量获取用户信息失败");
		} catch (Exception e) {
			throw new ResourceException("批量获取用户信息失败");
		}
		if (root.hasNonNull("errcode") && root.get("errcode").asInt() != 0) {
			String trimmed = root.path("errmsg").asText("").trim();
			String message = trimmed.isEmpty() ? "批量获取用户信息失败" : trimmed;
			throw new ResourceException(message);
		}

		JsonNode listNode = root.path("user_info_list");
		if (!listNode.isArray()) {
			return List.of();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (JsonNode item : listNode) {
			out.add(objectMapper.convertValue(item, new TypeReference<Map<String, Object>>() {}));
		}
		return out;
	}
}
