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

package cn.shopex.ecshopx.wechat.wxjava;

import cn.shopex.ecshopx.wechat.wxa.WechatOpenPlatformAuthorizerTokenService;
import java.util.concurrent.ConcurrentHashMap;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;
import org.springframework.stereotype.Service;

@Service
public class WxJavaMpRuntime {

	private static final int TOKEN_EXPIRES_SECONDS = 7200;

	private final WechatOpenPlatformAuthorizerTokenService tokenService;
	private final ConcurrentHashMap<String, WxMpServiceImpl> cache = new ConcurrentHashMap<>();

	public WxJavaMpRuntime(WechatOpenPlatformAuthorizerTokenService tokenService) {
		this.tokenService = tokenService;
	}

	public WxMpService mp(String authorizerAppId) {
		WxMpServiceImpl svc =
				cache.computeIfAbsent(
						authorizerAppId,
						id -> {
							WxMpDefaultConfigImpl cfg = new WxMpDefaultConfigImpl();
							cfg.setAppId(id);
							WxMpServiceImpl impl = new WxMpServiceImpl();
							impl.setWxMpConfigStorage(cfg);
							return impl;
						});
		String tok = tokenService.getAuthorizerAccessToken(authorizerAppId);
		WxMpDefaultConfigImpl c = (WxMpDefaultConfigImpl) svc.getWxMpConfigStorage();
		c.updateAccessToken(tok, TOKEN_EXPIRES_SECONDS);
		return svc;
	}

	/**
	 * Uses only a caller-supplied MP / authorizer {@code access_token} (no refresh).
	 */
	public WxMpService mpBearer(String accessToken) {
		String tok = accessToken == null ? "" : accessToken.trim();
		WxMpDefaultConfigImpl cfg = new WxMpDefaultConfigImpl();
		cfg.setAppId("bearer");
		cfg.updateAccessToken(tok, TOKEN_EXPIRES_SECONDS);
		WxMpServiceImpl impl = new WxMpServiceImpl();
		impl.setWxMpConfigStorage(cfg);
		return impl;
	}

	public WxMpService mpDirect(String appId, String secret) {
		String key = "direct:" + appId;
		WxMpServiceImpl svc =
				cache.computeIfAbsent(
						key,
						k -> {
							WxMpDefaultConfigImpl cfg = new WxMpDefaultConfigImpl();
							cfg.setAppId(appId);
							cfg.setSecret(secret);
							WxMpServiceImpl impl = new WxMpServiceImpl();
							impl.setWxMpConfigStorage(cfg);
							return impl;
						});
		WxMpDefaultConfigImpl c = (WxMpDefaultConfigImpl) svc.getWxMpConfigStorage();
		c.setSecret(secret);
		try {
			svc.getAccessToken(false);
		} catch (me.chanjar.weixin.common.error.WxErrorException e) {
			throw WxJavaExceptions.toResource(e);
		}
		return svc;
	}
}
