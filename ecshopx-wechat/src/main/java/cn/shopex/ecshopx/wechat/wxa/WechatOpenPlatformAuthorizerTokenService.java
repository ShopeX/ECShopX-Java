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

package cn.shopex.ecshopx.wechat.wxa;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.domain.WechatAuth;
import cn.shopex.ecshopx.wechat.mapper.WechatAuthMapper;
import cn.shopex.ecshopx.wechat.wxjava.WxJavaExceptions;
import java.util.concurrent.ConcurrentHashMap;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;
import me.chanjar.weixin.open.api.WxOpenConfigStorage;
import me.chanjar.weixin.open.api.WxOpenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WechatOpenPlatformAuthorizerTokenService {

	private final WechatAuthMapper wechatAuthMapper;
	private final WxOpenService wxOpenService;
	private final WxOpenConfigStorage wxOpenConfigStorage;
	private final ConcurrentHashMap<String, WxMpServiceImpl> directMpByAppId = new ConcurrentHashMap<>();

	@Value("${ecshopx.wechat.open.component-appid:}")
	private String componentAppId;

	@Value("${ecshopx.wechat.open.component-appsecret:}")
	private String componentAppSecret;

	@Value("${ecshopx.wechat.open.component-verify-ticket:}")
	private String componentVerifyTicket;

	public WechatOpenPlatformAuthorizerTokenService(
			WechatAuthMapper wechatAuthMapper,
			WxOpenService wxOpenService,
			WxOpenConfigStorage wxOpenConfigStorage) {
		this.wechatAuthMapper = wechatAuthMapper;
		this.wxOpenService = wxOpenService;
		this.wxOpenConfigStorage = wxOpenConfigStorage;
	}

	public String getAuthorizerAccessToken(String authorizerAppid) {
		if (!StringUtils.hasText(authorizerAppid)) {
			throw new ResourceException("小程序 AppId 无效");
		}
		WechatAuth auth = wechatAuthMapper.selectById(authorizerAppid);
		if (auth == null) {
			throw new ResourceException("当前公众号或小程序未绑定或已解绑，请重新授权", 400, 400001);
		}
		boolean direct = auth.getIsDirect() != null && auth.getIsDirect() == 1;
		if (direct) {
			return getDirectAccessToken(authorizerAppid, auth.getAuthorizerAppsecret());
		}
		if (!"bind".equals(auth.getBindStatus())) {
			throw new ResourceException("当前公众号或小程序未绑定或已解绑，请重新授权", 400, 400001);
		}
		String refresh = auth.getAuthorizerRefreshToken();
		if (!StringUtils.hasText(refresh)) {
			throw new ResourceException("当前公众号或小程序未绑定或已解绑，请重新授权", 400, 400001);
		}
		assertOpenPlatformComponentReady();
		return getThirdPartyAuthorizerAccessToken(authorizerAppid, refresh);
	}

	private void assertOpenPlatformComponentReady() {
		if (!StringUtils.hasText(componentAppId) || !StringUtils.hasText(componentAppSecret)) {
			throw new ResourceException("微信开放平台组件配置不完整");
		}
		if (!StringUtils.hasText(componentVerifyTicket)) {
			throw new ResourceException("微信开放平台 component_verify_ticket 未配置或未同步");
		}
	}

	public String getDirectAccessTokenForCredentials(String appId, String appSecret) {
		if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)) {
			throw new ResourceException("小程序直连密钥未配置");
		}
		return getDirectAccessToken(appId.trim(), appSecret.trim());
	}

	public String getOpenPlatformComponentAccessToken() {
		assertOpenPlatformComponentReady();
		try {
			return wxOpenService.getWxOpenComponentService().getComponentAccessToken(false);
		} catch (WxErrorException e) {
			throw WxJavaExceptions.toResource(e);
		}
	}

	public String getOpenPlatformComponentAppId() {
		assertOpenPlatformComponentReady();
		return componentAppId.trim();
	}

	public String getOpenPlatformComponentAppIdForOAuthAuthorizeUrl() {
		if (!StringUtils.hasText(componentAppId)) {
			return "";
		}
		return componentAppId.trim();
	}

	private String getDirectAccessToken(String appid, String secret) {
		if (!StringUtils.hasText(secret)) {
			throw new ResourceException("小程序直连密钥未配置");
		}
		WxMpServiceImpl mp =
				directMpByAppId.computeIfAbsent(
						appid,
						id -> {
							WxMpDefaultConfigImpl cfg = new WxMpDefaultConfigImpl();
							cfg.setAppId(id);
							cfg.setSecret(secret);
							WxMpServiceImpl s = new WxMpServiceImpl();
							s.setWxMpConfigStorage(cfg);
							return s;
						});
		WxMpDefaultConfigImpl cfg = (WxMpDefaultConfigImpl) mp.getWxMpConfigStorage();
		cfg.setSecret(secret);
		try {
			return mp.getAccessToken(false);
		} catch (WxErrorException e) {
			throw WxJavaExceptions.toResource(e);
		}
	}

	private String getThirdPartyAuthorizerAccessToken(String authorizerAppid, String refreshToken) {
		wxOpenConfigStorage.updateAuthorizerRefreshToken(authorizerAppid, refreshToken.trim());
		try {
			return wxOpenService.getWxOpenComponentService().getAuthorizerAccessToken(authorizerAppid, false);
		} catch (WxErrorException e) {
			throw WxJavaExceptions.toResource(e);
		}
	}
}
