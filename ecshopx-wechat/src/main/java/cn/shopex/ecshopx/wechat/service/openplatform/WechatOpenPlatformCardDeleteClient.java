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

package cn.shopex.ecshopx.wechat.service.openplatform;

import cn.shopex.ecshopx.wechat.wxjava.WxJavaMpRuntime;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.enums.WxMpApiUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WechatOpenPlatformCardDeleteClient {

	private static final Logger log = LoggerFactory.getLogger(WechatOpenPlatformCardDeleteClient.class);

	private final WxJavaMpRuntime wxJavaMpRuntime;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public WechatOpenPlatformCardDeleteClient(WxJavaMpRuntime wxJavaMpRuntime) {
		this.wxJavaMpRuntime = wxJavaMpRuntime;
	}

	/**
	 * @return {@code true} 当 errcode 为 0；入参不全、接口失败或网络异常时返回 {@code false}（仅打 warn）。
	 */
	public boolean deleteCard(String authorizerAppId, String wechatCardId) {
		if (!StringUtils.hasText(authorizerAppId) || !StringUtils.hasText(wechatCardId)) {
			return false;
		}
		try {
			String raw = wxJavaMpRuntime
					.mp(authorizerAppId)
					.post(WxMpApiUrl.Card.CARD_DELETE, Map.of("card_id", wechatCardId));
			JsonNode node = objectMapper.readTree(raw.getBytes(StandardCharsets.UTF_8));
			int errcode = node.path("errcode").asInt(-1);
			if (errcode == 0) {
				return true;
			}
			log.warn("wechat card delete failed errcode={} errmsg={}", errcode, node.path("errmsg").asText(""));
			return false;
		} catch (WxErrorException e) {
			log.warn("wechat card delete wx error: {}", e.getMessage());
			return false;
		} catch (Exception e) {
			log.warn("wechat card delete request error: {}", e.getMessage());
			return false;
		}
	}
}
