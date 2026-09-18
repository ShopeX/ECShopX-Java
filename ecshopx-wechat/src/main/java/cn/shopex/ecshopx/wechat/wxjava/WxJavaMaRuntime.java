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

import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.domain.WechatAuth;
import cn.shopex.ecshopx.wechat.mapper.WechatAuthMapper;
import cn.shopex.ecshopx.wechat.wxa.WechatOpenPlatformAuthorizerTokenService;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxJavaMaRuntime {

	private static final int TOKEN_EXPIRES_SECONDS = 7200;

	private final WechatAuthMapper wechatAuthMapper;
	private final WechatOpenPlatformAuthorizerTokenService tokenService;
	private final ConcurrentHashMap<String, WxMaServiceImpl> cache = new ConcurrentHashMap<>();

	public WxJavaMaRuntime(
			WechatAuthMapper wechatAuthMapper, WechatOpenPlatformAuthorizerTokenService tokenService) {
		this.wechatAuthMapper = wechatAuthMapper;
		this.tokenService = tokenService;
	}

	public WxMaService ma(String authorizerAppId) {
		if (!StringUtils.hasText(authorizerAppId)) {
			throw new ResourceException("小程序 AppId 无效");
		}
		String appid = authorizerAppId.trim();
		WechatAuth auth = wechatAuthMapper.selectById(appid);
		if (auth != null && auth.getIsDirect() != null && auth.getIsDirect() == 1) {
			String secret = auth.getAuthorizerAppsecret();
			if (!StringUtils.hasText(secret)) {
				throw new ResourceException("当前公众号或小程序未配置secret", 400, 400001);
			}
			return maDirect(appid, secret.trim());
		}
		WxMaServiceImpl svc =
				cache.computeIfAbsent(
						appid,
						id -> {
							WxMaDefaultConfigImpl cfg = new WxMaDefaultConfigImpl();
							cfg.setAppid(id);
							WxMaServiceImpl impl = new WxMaServiceImpl();
							impl.setWxMaConfig(cfg);
							return impl;
						});
		String tok = tokenService.getAuthorizerAccessToken(appid);
		WxMaDefaultConfigImpl c = (WxMaDefaultConfigImpl) svc.getWxMaConfig();
		c.updateAccessToken(tok, TOKEN_EXPIRES_SECONDS);
		return svc;
	}

	public WxMaService maDirect(String appId, String secret) {
		String key = "direct:" + appId;
		WxMaServiceImpl svc =
				cache.computeIfAbsent(
						key,
						k -> {
							WxMaDefaultConfigImpl cfg = new WxMaDefaultConfigImpl();
							cfg.setAppid(appId);
							cfg.setSecret(secret);
							WxMaServiceImpl impl = new WxMaServiceImpl();
							impl.setWxMaConfig(cfg);
							return impl;
						});
		WxMaDefaultConfigImpl c = (WxMaDefaultConfigImpl) svc.getWxMaConfig();
		c.setSecret(secret);
		try {
			svc.getAccessToken(false);
		} catch (me.chanjar.weixin.common.error.WxErrorException e) {
			throw WxJavaExceptions.toResource(e);
		}
		return svc;
	}
}
