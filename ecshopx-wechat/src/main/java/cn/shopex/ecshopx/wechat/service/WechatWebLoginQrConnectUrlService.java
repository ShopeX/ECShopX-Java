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

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class WechatWebLoginQrConnectUrlService {

	private static final String QR_CONNECT_BASE = "https://open.weixin.qq.com/connect/qrconnect";

	public String buildQrConnectAuthorizeUrl(String appId, String redirectUri) {
		String trimmedAppId = appId == null ? "" : appId.trim();
		String trimmedRedirect = redirectUri == null ? "" : redirectUri.trim();
		String state = UUID.randomUUID().toString();
		try {
			return UriComponentsBuilder.fromHttpUrl(QR_CONNECT_BASE)
					.queryParam("appid", trimmedAppId)
					.queryParam("redirect_uri", trimmedRedirect)
					.queryParam("response_type", "code")
					.queryParam("scope", "snsapi_login")
					.queryParam("state", state)
					.fragment("wechat_redirect")
					.build(false)
					.encode(StandardCharsets.UTF_8)
					.toUriString();
		} catch (Exception e) {
			return "";
		}
	}
}
