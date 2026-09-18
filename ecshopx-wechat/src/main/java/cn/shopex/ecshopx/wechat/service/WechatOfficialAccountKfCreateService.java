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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.domain.WechatAuth;
import cn.shopex.ecshopx.wechat.service.openplatform.WechatOfficialAccountKfAccountClient;
import cn.shopex.ecshopx.wechat.wxa.WechatOpenPlatformAuthorizerTokenService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WechatOfficialAccountKfCreateService {

	private final WechatAuthQueryService wechatAuthQueryService;
	private final WechatOpenPlatformAuthorizerTokenService wechatOpenPlatformAuthorizerTokenService;
	private final WechatOfficialAccountKfAccountClient wechatOfficialAccountKfAccountClient;

	public WechatOfficialAccountKfCreateService(
			WechatAuthQueryService wechatAuthQueryService,
			WechatOpenPlatformAuthorizerTokenService wechatOpenPlatformAuthorizerTokenService,
			WechatOfficialAccountKfAccountClient wechatOfficialAccountKfAccountClient) {
		this.wechatAuthQueryService = wechatAuthQueryService;
		this.wechatOpenPlatformAuthorizerTokenService = wechatOpenPlatformAuthorizerTokenService;
		this.wechatOfficialAccountKfAccountClient = wechatOfficialAccountKfAccountClient;
	}

	public void createWechatKf(String authorizerAppid, String wxName, String nick, Path avatarLocalPathOrNull) {
		WechatAuth auth = wechatAuthQueryService.requireWechatAuthWithAliasForKf(authorizerAppid);
		String alias = auth.getAlias();
		if (!StringUtils.hasText(alias)) {
			throw new ResourceException("当前公众号未设置微信号，请先设置微信号");
		}
		String accessToken = wechatOpenPlatformAuthorizerTokenService.getAuthorizerAccessToken(authorizerAppid.trim());
		String account = (wxName == null ? "" : wxName.trim()) + "@" + alias.trim();
		boolean created = wechatOfficialAccountKfAccountClient.addKfAccount(
				accessToken, account, nick == null ? "" : nick.trim());
		if (created) {
			wechatOfficialAccountKfAccountClient.inviteWorker(
					accessToken, account, wxName == null ? "" : wxName.trim());
			if (avatarLocalPathOrNull != null && Files.exists(avatarLocalPathOrNull)) {
				wechatOfficialAccountKfAccountClient.uploadHeadImg(accessToken, account, avatarLocalPathOrNull);
			}
		}
	}
}
