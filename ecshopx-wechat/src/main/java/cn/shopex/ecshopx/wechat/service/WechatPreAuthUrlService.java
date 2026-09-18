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

import cn.shopex.ecshopx.wechat.service.openplatform.WechatOpenPlatformPreAuthClient;
import cn.shopex.ecshopx.wechat.wxa.WechatOpenPlatformAuthorizerTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class WechatPreAuthUrlService {

	private static final Logger log = LoggerFactory.getLogger(WechatPreAuthUrlService.class);

	private static final String COMPONENT_LOGIN_PAGE = "https://mp.weixin.qq.com/cgi-bin/componentloginpage";

	private final WechatOpenPlatformPreAuthClient preAuthClient;
	private final WechatOpenPlatformAuthorizerTokenService tokenService;

	public WechatPreAuthUrlService(
			WechatOpenPlatformPreAuthClient preAuthClient,
			WechatOpenPlatformAuthorizerTokenService tokenService) {
		this.preAuthClient = preAuthClient;
		this.tokenService = tokenService;
	}

	public String getPreAuthUrl(String callbackUrl) {
		try {
			if (callbackUrl == null) {
				return "";
			}
			String preAuthCode = preAuthClient.createPreAuthCode();
			String componentAppId = tokenService.getOpenPlatformComponentAppId();
			return UriComponentsBuilder.fromHttpUrl(COMPONENT_LOGIN_PAGE)
					.queryParam("component_appid", componentAppId)
					.queryParam("pre_auth_code", preAuthCode)
					.queryParam("redirect_uri", callbackUrl)
					.build(true)
					.toUriString();
		} catch (Exception e) {
			log.warn("getPreAuthUrl failed, callbackUrl={}", callbackUrl, e);
			return "";
		}
	}
}
