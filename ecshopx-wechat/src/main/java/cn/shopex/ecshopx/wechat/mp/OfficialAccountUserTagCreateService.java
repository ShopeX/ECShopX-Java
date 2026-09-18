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
public class OfficialAccountUserTagCreateService {

	private final WxJavaMpRuntime wxJavaMpRuntime;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public OfficialAccountUserTagCreateService(WxJavaMpRuntime wxJavaMpRuntime) {
		this.wxJavaMpRuntime = wxJavaMpRuntime;
	}

	/**
	 * 调用微信公众平台 tags/create。
	 *
	 * @return 含微信返回的 id、name；键名为小写 {@code id}、{@code name}
	 */
	public Map<String, Object> createTag(String authorizerAppid, String tagName) {
		if (authorizerAppid == null || authorizerAppid.isEmpty()) {
			throw new BadRequestException("当前账号未绑定公众号或小程序，请先授权绑定", 400);
		}
		Map<String, String> bodyMap = new LinkedHashMap<>();
		bodyMap.put("name", tagName);
		JsonNode root;
		try {
			String raw =
					wxJavaMpRuntime.mp(authorizerAppid).post(WxMpApiUrl.UserTag.TAGS_CREATE, bodyMap);
			root = objectMapper.readTree(raw.getBytes(StandardCharsets.UTF_8));
		} catch (WxErrorException e) {
			throw new BadRequestException("当前账号未绑定公众号或小程序，请先授权绑定", 400);
		} catch (Exception e) {
			throw new ResourceException("微信接口返回无法解析");
		}
		if (root.hasNonNull("errcode") && root.get("errcode").asInt() != 0) {
			String errmsg = root.path("errmsg").asText("");
			throw new ResourceException(
					(errmsg != null && !errmsg.isEmpty()) ? errmsg : "微信创建标签失败");
		}
		JsonNode tag = root.get("tag");
		if (tag == null || tag.isNull() || !tag.hasNonNull("id")) {
			throw new ResourceException("微信创建标签失败");
		}
		long idLong = tag.get("id").asLong();
		String nameString = tag.path("name").asText("");
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("id", idLong);
		out.put("name", nameString);
		return out;
	}
}
