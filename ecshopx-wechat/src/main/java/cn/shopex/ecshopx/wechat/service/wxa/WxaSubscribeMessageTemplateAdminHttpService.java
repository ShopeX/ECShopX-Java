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

package cn.shopex.ecshopx.wechat.service.wxa;

import cn.binarywang.wx.miniapp.constant.WxMaApiUrlConstants;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.wechat.WxaSubscribeMessageTemplateAdminPort;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaMaRuntime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service("wxaSubscribeMessageTemplateAdminHttp")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ecshopx.wechat.wxa.subscribe-template", name = "http-enabled", havingValue = "true")
public class WxaSubscribeMessageTemplateAdminHttpService implements WxaSubscribeMessageTemplateAdminPort {

	private final WxJavaMaRuntime wxJavaMaRuntime;

	private final ObjectMapper objectMapper;

	@Override
	public String addTemplate(
			String authorizerAppid, String wxaLibraryTemplateId, List<Integer> keywordIdList, String sceneDescription) {
		Map<String, Object> body = new HashMap<>();
		body.put("tid", wxaLibraryTemplateId);
		body.put("kidList", keywordIdList);
		body.put("sceneDesc", sceneDescription);
		try {
			String raw =
					wxJavaMaRuntime
							.ma(authorizerAppid)
							.post(WxMaApiUrlConstants.Subscribe.TEMPLATE_ADD_URL, body);
			JsonNode root = objectMapper.readTree(raw.getBytes(StandardCharsets.UTF_8));
			if (root.hasNonNull("errcode") && root.get("errcode").asInt() != 0) {
				String msg = root.path("errmsg").asText("微信接口错误");
				throw new ResourceException(msg);
			}
			String pri = root.path("priTmplId").asText(null);
			if (!StringUtils.hasText(pri)) {
				throw new ResourceException("微信接口未返回 priTmplId");
			}
			return pri;
		} catch (WxErrorException e) {
			throw new ResourceException(e.getMessage() != null ? e.getMessage() : "微信接口错误");
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			throw new ResourceException("微信接口返回无法解析");
		}
	}

	@Override
	public boolean deleteTemplate(String authorizerAppid, String priTemplateId) {
		try {
			Map<String, String> body = new HashMap<>();
			body.put("priTmplId", priTemplateId);
			String raw =
					wxJavaMaRuntime
							.ma(authorizerAppid)
							.post(WxMaApiUrlConstants.Subscribe.TEMPLATE_DEL_URL, body);
			JsonNode root = objectMapper.readTree(raw.getBytes(StandardCharsets.UTF_8));
			String errmsg = root.path("errmsg").asText("");
			return "ok".equalsIgnoreCase(errmsg.trim());
		} catch (Exception e) {
			return false;
		}
	}
}
