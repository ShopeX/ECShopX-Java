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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.wechat.domain.WechatAuth;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxappOAuthRedirectUrlService {

	private final WechatAuthQueryService wechatAuthQueryService;
	private final OfficialAccountOAuthFacade officialAccountOAuthFacade;

	public WxappOAuthRedirectUrlService(
			WechatAuthQueryService wechatAuthQueryService,
			OfficialAccountOAuthFacade officialAccountOAuthFacade) {
		this.wechatAuthQueryService = wechatAuthQueryService;
		this.officialAccountOAuthFacade = officialAccountOAuthFacade;
	}

	public String oauthRedirectUrl(long companyId, String url) {
		String companyIdRaw = String.valueOf(companyId);
		WechatAuth woa =
				wechatAuthQueryService.findWoaWechatAuthForCompanyQuery(companyIdRaw).orElse(null);
		if (woa == null || !StringUtils.hasText(woa.getAuthorizerAppid())) {
			throw new BadRequestException("当前账号未绑定公众号或小程序，请先授权绑定");
		}
		String appid = woa.getAuthorizerAppid().trim();
		Map<String, Object> woaApp = new LinkedHashMap<>();
		boolean isDirect = woa.getIsDirect() != null && woa.getIsDirect() != 0;
		if (isDirect) {
			woaApp.put("mode", "direct");
			woaApp.put("app_id", appid);
			String secret = woa.getAuthorizerAppsecret();
			woaApp.put("secret", secret == null ? "" : secret.trim());
		} else {
			woaApp.put("mode", "authorized");
			woaApp.put("authorizerAppid", appid);
		}
		return officialAccountOAuthFacade.buildSnsapiBaseAuthorizeUrl(woaApp, url);
	}
}
