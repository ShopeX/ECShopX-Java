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

package cn.shopex.ecshopx.companys.service.auth.workwechatoauth;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.companys.domain.Companys;
import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.workwechat.config.WorkWechatModuleProperties;
import cn.shopex.ecshopx.workwechat.service.WorkWechatConfigService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OperatorWorkWechatOauthAuthorizeUrlService {

	private static final String OAUTH_AUTHORIZE_BASE = "https://open.weixin.qq.com/connect/oauth2/authorize";

	private final CompanysMapper companysMapper;
	private final WorkWechatConfigService workWechatConfigService;
	private final WorkWechatModuleProperties workWechatModuleProperties;

	public OperatorWorkWechatOauthAuthorizeUrlService(
			CompanysMapper companysMapper,
			WorkWechatConfigService workWechatConfigService,
			WorkWechatModuleProperties workWechatModuleProperties) {
		this.companysMapper = companysMapper;
		this.workWechatConfigService = workWechatConfigService;
		this.workWechatModuleProperties = workWechatModuleProperties;
	}

	public String getWorkwechatOuthorizeurl(String companyIdRaw) {
		String trimmed = companyIdRaw == null ? "" : companyIdRaw.trim();
		long companyIdLong;
		try {
			companyIdLong = Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw new ResourceException("company_id");
		}

		Companys row = companysMapper.selectById(companyIdLong);
		if (row == null) {
			throw new ResourceException("company_id");
		}

		Map<String, Object> viewConfig = workWechatConfigService.getViewConfig(companyIdLong);
		if (viewConfig == null) {
			throw new ResourceException("company_id未配置");
		}

		workWechatConfigService.validateDianwuAgentIdForOperatorWorkWechatOauth(viewConfig);

		String corpid = viewConfig.get("corpid") == null ? "" : String.valueOf(viewConfig.get("corpid")).trim();

		String authBase = workWechatModuleProperties.getDisWorkwechatH5AuthUri();
		if (authBase == null) {
			authBase = "";
		}
		String callback =
				authBase + (authBase.indexOf('?') >= 0 ? "&" : "?") + "company_id=" + urlEncodeUtf8(companyIdRaw);

		String queryString = "appid="
				+ urlEncodeUtf8(corpid)
				+ "&redirect_uri="
				+ urlEncodeUtf8(callback)
				+ "&response_type="
				+ urlEncodeUtf8("code")
				+ "&scope="
				+ urlEncodeUtf8("snsapi_base");
		String stateEncoded = urlEncodeUtf8(companyIdRaw == null ? "" : companyIdRaw);
		return OAUTH_AUTHORIZE_BASE + "?" + queryString + "&state=" + stateEncoded + "#wechat_redirect";
	}

	private static String urlEncodeUtf8(String s) {
		return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
	}
}
