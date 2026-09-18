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
import java.util.List;
import java.util.Map;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.enums.WxMpApiUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WechatOfficialAccountMenuCreateClient {

	private static final Logger log = LoggerFactory.getLogger(WechatOfficialAccountMenuCreateClient.class);

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final WxJavaMpRuntime wxJavaMpRuntime;

	public WechatOfficialAccountMenuCreateClient(WxJavaMpRuntime wxJavaMpRuntime) {
		this.wxJavaMpRuntime = wxJavaMpRuntime;
	}

	public boolean createMenu(String accessToken, List<Map<String, Object>> button) {
		try {
			String raw =
					wxJavaMpRuntime
							.mpBearer(accessToken)
							.post(WxMpApiUrl.Menu.MENU_CREATE, Map.of("button", button));
			if (raw == null || raw.isEmpty()) {
				return false;
			}
			JsonNode root = objectMapper.readTree(raw.getBytes(StandardCharsets.UTF_8));
			if (root.has("errcode") && root.get("errcode").asInt() == 0) {
				return true;
			}
			return false;
		} catch (WxErrorException e) {
			log.warn("WeChat official account menu create wx failed", e);
			return false;
		} catch (Exception e) {
			log.warn("WeChat official account menu create request failed", e);
			return false;
		}
	}
}
