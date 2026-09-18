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

import cn.shopex.ecshopx.wechat.service.openplatform.WechatOfficialAccountKfAccountClient;
import cn.shopex.ecshopx.wechat.wxa.WechatOpenPlatformAuthorizerTokenService;
import org.springframework.stereotype.Service;

@Service
public class WechatOfficialAccountKfDeleteService {

	private final WechatAuthQueryService wechatAuthQueryService;
	private final WechatOpenPlatformAuthorizerTokenService wechatOpenPlatformAuthorizerTokenService;
	private final WechatOfficialAccountKfAccountClient wechatOfficialAccountKfAccountClient;

	public WechatOfficialAccountKfDeleteService(
			WechatAuthQueryService wechatAuthQueryService,
			WechatOpenPlatformAuthorizerTokenService wechatOpenPlatformAuthorizerTokenService,
			WechatOfficialAccountKfAccountClient wechatOfficialAccountKfAccountClient) {
		this.wechatAuthQueryService = wechatAuthQueryService;
		this.wechatOpenPlatformAuthorizerTokenService = wechatOpenPlatformAuthorizerTokenService;
		this.wechatOfficialAccountKfAccountClient = wechatOfficialAccountKfAccountClient;
	}

	public void deleteWechatKf(String authorizerAppid, String account) {
		wechatAuthQueryService.requireWechatAuthWithAliasForKf(authorizerAppid);
		String accessToken = wechatOpenPlatformAuthorizerTokenService.getAuthorizerAccessToken(authorizerAppid.trim());
		wechatOfficialAccountKfAccountClient.deleteKfAccount(accessToken, account);
	}
}
