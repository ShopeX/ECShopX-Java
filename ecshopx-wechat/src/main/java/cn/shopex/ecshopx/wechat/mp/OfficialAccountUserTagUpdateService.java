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
import java.util.LinkedHashMap;
import java.util.Map;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.enums.WxMpApiUrl;
import org.springframework.stereotype.Service;

@Service
public class OfficialAccountUserTagUpdateService {

	private final WxJavaMpRuntime wxJavaMpRuntime;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public OfficialAccountUserTagUpdateService(WxJavaMpRuntime wxJavaMpRuntime) {
		this.wxJavaMpRuntime = wxJavaMpRuntime;
	}

	public void updateTag(String authorizerAppid, long tagId, String tagName) {
		if (authorizerAppid == null || authorizerAppid.isEmpty()) {
			throw new BadRequestException("当前账号未绑定公众号或小程序，请先授权绑定", 400);
		}
		Map<String, Object> tag = new LinkedHashMap<>();
		tag.put("id", tagId);
		tag.put("name", tagName);
		Map<String, Object> bodyMap = new LinkedHashMap<>();
		bodyMap.put("tag", tag);
		JsonNode root;
		try {
			String raw =
					wxJavaMpRuntime.mp(authorizerAppid).post(WxMpApiUrl.UserTag.TAGS_UPDATE, bodyMap);
			root = objectMapper.readTree(raw.getBytes(StandardCharsets.UTF_8));
		} catch (WxErrorException e) {
			throw new BadRequestException("当前账号未绑定公众号或小程序，请先授权绑定", 400);
		} catch (Exception e) {
			throw new ResourceException("微信接口返回无法解析");
		}
		if (root.hasNonNull("errcode") && root.get("errcode").asInt() != 0) {
			String errmsg = root.path("errmsg").asText("");
			throw new ResourceException(
					(errmsg != null && !errmsg.isEmpty()) ? errmsg : "微信更新标签失败");
		}
	}
}
