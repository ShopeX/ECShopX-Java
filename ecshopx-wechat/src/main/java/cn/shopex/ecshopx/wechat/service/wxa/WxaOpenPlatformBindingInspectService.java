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

import cn.shopex.ecshopx.wechat.config.WechatWxaPublishProperties;
import cn.shopex.ecshopx.wechat.service.openplatform.WechatOpenPlatformAccountClient;
import cn.shopex.ecshopx.wechat.wxa.WechatOpenPlatformAuthorizerTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxaOpenPlatformBindingInspectService {

	private final WechatWxaPublishProperties props;
	private final WechatOpenPlatformAuthorizerTokenService tokenService;
	private final WechatOpenPlatformAccountClient accountClient;

	public WxaOpenPlatformBindingInspectService(
			WechatWxaPublishProperties props,
			WechatOpenPlatformAuthorizerTokenService tokenService,
			WechatOpenPlatformAccountClient accountClient) {
		this.props = props;
		this.tokenService = tokenService;
		this.accountClient = accountClient;
	}

	public boolean checkWxaBind(String officialAuthorizerAppid, String wxaAppId) {
		if (!Boolean.TRUE.equals(props.getWxaNeedOpenPlatform())) {
			return true;
		}
		String off = officialAuthorizerAppid == null ? "" : officialAuthorizerAppid.trim();
		String wxa = wxaAppId == null ? "" : wxaAppId.trim();
		if (!StringUtils.hasText(off) || !StringUtils.hasText(wxa)) {
			return false;
		}
		try {
			String tokenOff = tokenService.getAuthorizerAccessToken(off);
			String tokenWxa = tokenService.getAuthorizerAccessToken(wxa);
			JsonNode r1 = accountClient.openGet(tokenOff, off);
			JsonNode r2 = accountClient.openGet(tokenWxa, wxa);
			String open1 = readOpenAppid(r1);
			String open2 = readOpenAppid(r2);
			return StringUtils.hasText(open1) && open1.equals(open2);
		} catch (Exception e) {
			return false;
		}
	}

	private static String readOpenAppid(JsonNode root) {
		if (root == null || root.isMissingNode()) {
			return "";
		}
		if (root.path("errcode").asInt(-1) != 0) {
			return "";
		}
		String v = root.path("open_appid").asText("");
		return v == null ? "" : v.trim();
	}
}
