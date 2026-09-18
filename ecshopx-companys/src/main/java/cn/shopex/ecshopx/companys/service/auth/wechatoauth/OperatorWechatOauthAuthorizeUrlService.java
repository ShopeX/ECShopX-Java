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

package cn.shopex.ecshopx.companys.service.auth.wechatoauth;

import cn.shopex.ecshopx.wechat.service.OfficialAccountOAuthFacade;
import cn.shopex.ecshopx.wechat.service.OpenPlatformWoaFacade;
import cn.shopex.ecshopx.workwechat.config.WorkWechatModuleProperties;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OperatorWechatOauthAuthorizeUrlService {

	private static final String TRUSTLOGIN_TAG_WEIXIN = "weixin";
	private static final String VERSION_TAG_TOUCH = "touch";

	private final OpenPlatformWoaFacade openPlatformWoaFacade;
	private final OfficialAccountOAuthFacade officialAccountOAuthFacade;
	private final WorkWechatModuleProperties workWechatModuleProperties;

	public OperatorWechatOauthAuthorizeUrlService(
			OpenPlatformWoaFacade openPlatformWoaFacade,
			OfficialAccountOAuthFacade officialAccountOAuthFacade,
			WorkWechatModuleProperties workWechatModuleProperties) {
		this.openPlatformWoaFacade = openPlatformWoaFacade;
		this.officialAccountOAuthFacade = officialAccountOAuthFacade;
		this.workWechatModuleProperties = workWechatModuleProperties;
	}

	public String getWechatOuthorizeurl(String companyIdRaw) {
		String base = workWechatModuleProperties.getDisWorkwechatH5AuthUri();
		if (base == null) {
			base = "";
		}
		Map<String, Object> woa =
				openPlatformWoaFacade.getWoaApp(companyIdRaw, TRUSTLOGIN_TAG_WEIXIN, VERSION_TAG_TOUCH);
		String oauthUrl = officialAccountOAuthFacade.buildSnsapiUserinfoAuthorizeUrl(woa, base);
		return StringUtils.hasText(oauthUrl) ? oauthUrl : "";
	}
}
