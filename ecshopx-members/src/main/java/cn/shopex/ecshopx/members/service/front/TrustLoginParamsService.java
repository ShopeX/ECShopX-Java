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

package cn.shopex.ecshopx.members.service.front;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.service.admin.AdminTrustLoginListService;
import cn.shopex.ecshopx.members.service.trustlogin.SocialTrustLoginService;
import cn.shopex.ecshopx.members.service.trustlogin.TrustLoginConfigSupport;
import cn.shopex.ecshopx.wechat.service.OfficialAccountOAuthFacade;
import cn.shopex.ecshopx.wechat.service.OpenPlatformWoaFacade;
import cn.shopex.ecshopx.wechat.service.TrustLoginWeixinTouchConfigService;
import cn.shopex.ecshopx.wechat.service.WechatWebLoginQrConnectUrlService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TrustLoginParamsService {

	private final TrustLoginWeixinTouchConfigService trustLoginWeixinTouchConfigService;
	private final WechatWebLoginQrConnectUrlService wechatWebLoginQrConnectUrlService;
	private final OpenPlatformWoaFacade openPlatformWoaFacade;
	private final OfficialAccountOAuthFacade officialAccountOAuthFacade;
	private final AdminTrustLoginListService adminTrustLoginListService;
	private final SocialTrustLoginService socialTrustLoginService;

	@Value("${common.h5-base-url:}")
	private String commonH5BaseUrl;

	public TrustLoginParamsService(
			TrustLoginWeixinTouchConfigService trustLoginWeixinTouchConfigService,
			WechatWebLoginQrConnectUrlService wechatWebLoginQrConnectUrlService,
			OpenPlatformWoaFacade openPlatformWoaFacade,
			OfficialAccountOAuthFacade officialAccountOAuthFacade,
			AdminTrustLoginListService adminTrustLoginListService,
			SocialTrustLoginService socialTrustLoginService) {
		this.trustLoginWeixinTouchConfigService = trustLoginWeixinTouchConfigService;
		this.wechatWebLoginQrConnectUrlService = wechatWebLoginQrConnectUrlService;
		this.openPlatformWoaFacade = openPlatformWoaFacade;
		this.officialAccountOAuthFacade = officialAccountOAuthFacade;
		this.adminTrustLoginListService = adminTrustLoginListService;
		this.socialTrustLoginService = socialTrustLoginService;
	}

	public Object getTrustLoginParams(
			long companyId,
			String trustloginTag,
			String versionTag,
			String h5HostFromOrigin,
			String redirectUrl) {
		String tt = normalizeTrustloginTag(trustloginTag);
		String vt = normalizeVersionTag(versionTag);

		if ("standard".equals(vt)) {
			if ("weixin".equals(tt)) {
				Map<String, Object> configRow =
						trustLoginWeixinTouchConfigService.getConfigRow("weixin", "standard", companyId);
				String appId = stringOrEmpty(configRow.get("app_id"));
				String redirectUrlForSocialite = redirectUrl == null ? "" : redirectUrl;
				String qrUrl =
						wechatWebLoginQrConnectUrlService.buildQrConnectAuthorizeUrl(appId, redirectUrlForSocialite);
				LinkedHashMap<String, Object> out = new LinkedHashMap<>();
				if (configRow.isEmpty()) {
					out.put("config_info", Collections.emptyList());
				} else {
					out.put("config_info", new LinkedHashMap<>(configRow));
				}
				out.put("redirect_url", qrUrl);
				return out;
			}
			return Collections.emptyList();
		}

		if ("touch".equals(vt)) {
			if ("weixin".equals(tt)) {
				String h5Host = resolveH5Host(h5HostFromOrigin);
				String pathSuffix = StringUtils.hasText(redirectUrl) ? ("?redi_url=" + redirectUrl) : "";
				String innerPageUrl = h5Host + "/subpages/auth/auth-loading" + pathSuffix;
				Map<String, Object> woaApp = openPlatformWoaFacade.getWoaApp(companyId, "weixin", "touch");
				String oauthUrl = officialAccountOAuthFacade.buildSnsapiUserinfoAuthorizeUrl(woaApp, innerPageUrl);
				LinkedHashMap<String, Object> out = new LinkedHashMap<>();
				out.put("oauth_url", oauthUrl == null ? "" : oauthUrl);
				return out;
			}
			if (socialTrustLoginService.isSocialProvider(tt)) {
				Map<String, Object> configRow = adminTrustLoginListService.getConfigRow(tt, vt, companyId);
				if (configRow.isEmpty() || !TrustLoginConfigSupport.isEnabled(configRow.get("status"))) {
					throw new ResourceException("该登录方式未开启");
				}
				String h5Host = socialTrustLoginService.resolveH5Host(h5HostFromOrigin);
				if (!StringUtils.hasText(h5Host)) {
					throw new ResourceException("缺少 H5 域名配置");
				}
				String redirectUri = socialTrustLoginService.buildRedirectUri(h5Host, tt);
				String oauthUrl =
						socialTrustLoginService.getAuthorizeUrl(tt, configRow, redirectUri, h5Host);
				return Map.of("oauth_url", oauthUrl);
			}
			return Collections.emptyList();
		}

		return new LinkedHashMap<>();
	}

	private String resolveH5Host(String h5HostFromOrigin) {
		return socialTrustLoginService.resolveH5Host(
				h5HostFromOrigin == null ? "" : h5HostFromOrigin.trim());
	}

	private static String normalizeTrustloginTag(String trustloginTag) {
		if (trustloginTag == null) {
			return "weixin";
		}
		String t = trustloginTag.trim();
		return t.isEmpty() ? "weixin" : t;
	}

	private static String normalizeVersionTag(String versionTag) {
		if (versionTag == null) {
			return "standard";
		}
		String t = versionTag.trim();
		return t.isEmpty() ? "standard" : t;
	}

	private static String stringOrEmpty(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}
}
