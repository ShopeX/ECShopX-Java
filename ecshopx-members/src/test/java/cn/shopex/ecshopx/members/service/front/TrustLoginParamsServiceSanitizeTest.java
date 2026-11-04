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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.members.service.admin.AdminTrustLoginListService;
import cn.shopex.ecshopx.members.service.trustlogin.SocialTrustLoginService;
import cn.shopex.ecshopx.wechat.service.OfficialAccountOAuthFacade;
import cn.shopex.ecshopx.wechat.service.OpenPlatformWoaFacade;
import cn.shopex.ecshopx.wechat.service.TrustLoginWeixinTouchConfigService;
import cn.shopex.ecshopx.wechat.service.WechatWebLoginQrConnectUrlService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrustLoginParamsServiceSanitizeTest {

	@Mock
	private TrustLoginWeixinTouchConfigService trustLoginWeixinTouchConfigService;

	@Mock
	private WechatWebLoginQrConnectUrlService wechatWebLoginQrConnectUrlService;

	@Mock
	private OpenPlatformWoaFacade openPlatformWoaFacade;

	@Mock
	private OfficialAccountOAuthFacade officialAccountOAuthFacade;

	@Mock
	private AdminTrustLoginListService adminTrustLoginListService;

	@Mock
	private SocialTrustLoginService socialTrustLoginService;

	private TrustLoginParamsService service;

	@BeforeEach
	void setUp() {
		service =
				new TrustLoginParamsService(
						trustLoginWeixinTouchConfigService,
						wechatWebLoginQrConnectUrlService,
						openPlatformWoaFacade,
						officialAccountOAuthFacade,
						adminTrustLoginListService,
						socialTrustLoginService);
	}

	@Test
	void getTrustLoginParams_standardWeixin_stripsSecretFromConfigInfo() {
		Map<String, Object> raw = new LinkedHashMap<>();
		raw.put("type", "weixin");
		raw.put("app_id", "wx-app");
		raw.put("secret", "super-secret");
		raw.put("name", "微信");
		raw.put("status", "true");
		raw.put("extra_config", "");
		when(trustLoginWeixinTouchConfigService.getConfigRow(eq("weixin"), eq("standard"), anyLong()))
				.thenReturn(raw);
		when(wechatWebLoginQrConnectUrlService.buildQrConnectAuthorizeUrl(anyString(), anyString()))
				.thenReturn("https://open.weixin.qq.com/connect/qrconnect?appid=wx-app");

		Object body = service.getTrustLoginParams(10L, "weixin", "standard", "", "");

		assertInstanceOf(Map.class, body);
		@SuppressWarnings("unchecked")
		Map<String, Object> out = (Map<String, Object>) body;
		assertTrue(out.get("config_info") instanceof Map);
		@SuppressWarnings("unchecked")
		Map<String, Object> configInfo = (Map<String, Object>) out.get("config_info");
		assertEquals("wx-app", configInfo.get("app_id"));
		assertFalse(configInfo.containsKey("secret"));
		assertTrue(String.valueOf(out.get("redirect_url")).contains("qrconnect"));
	}
}
