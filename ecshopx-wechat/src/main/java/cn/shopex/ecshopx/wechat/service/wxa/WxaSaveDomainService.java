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

package cn.shopex.ecshopx.wechat.service.wxa;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.wechat.service.WechatAuthQueryService;
import cn.shopex.ecshopx.wechat.wxa.WechatOpenPlatformAuthorizerTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxaSaveDomainService {

	private static final Logger log = LoggerFactory.getLogger(WxaSaveDomainService.class);

	private final WechatAuthQueryService wechatAuthQueryService;
	private final WxaTemplateDomainInfoService wxaTemplateDomainInfoService;
	private final WxaOpenPlatformCodeApiClient wxaOpenPlatformCodeApiClient;
	private final ObjectMapper objectMapper;
	private final WechatOpenPlatformAuthorizerTokenService wechatOpenPlatformAuthorizerTokenService;

	public WxaSaveDomainService(
			WechatAuthQueryService wechatAuthQueryService,
			WxaTemplateDomainInfoService wxaTemplateDomainInfoService,
			WxaOpenPlatformCodeApiClient wxaOpenPlatformCodeApiClient,
			ObjectMapper objectMapper,
			WechatOpenPlatformAuthorizerTokenService wechatOpenPlatformAuthorizerTokenService) {
		this.wechatAuthQueryService = wechatAuthQueryService;
		this.wxaTemplateDomainInfoService = wxaTemplateDomainInfoService;
		this.wxaOpenPlatformCodeApiClient = wxaOpenPlatformCodeApiClient;
		this.objectMapper = objectMapper;
		this.wechatOpenPlatformAuthorizerTokenService = wechatOpenPlatformAuthorizerTokenService;
	}

	public void saveDomain(long companyId, String wxaAppId, String templateName) {
		String appId = wxaAppId == null ? "" : wxaAppId.trim();
		if (!StringUtils.hasText(appId)
				|| !wechatAuthQueryService.isMiniProgramWxaBoundToCompany(companyId, appId)) {
			throw new ResourceException("小程序未绑定，请重新绑定", 400, 400001);
		}
		wechatOpenPlatformAuthorizerTokenService.getAuthorizerAccessToken(appId);
		Map<String, Object> domain = wxaTemplateDomainInfoService.buildLocalDomainStructure(templateName);
		if (domain == null) {
			domain = new LinkedHashMap<>();
		}
		if (domain.isEmpty()) {
			return;
		}
		try {
			log.info("保存小程序服务器域名参数：{}", objectMapper.writeValueAsString(domain));
		} catch (Exception ignored) {
			log.info("保存小程序服务器域名参数：{}", String.valueOf(domain));
		}
		wxaOpenPlatformCodeApiClient.modifyDomain(appId, domain, false);
		Object rawWebview = domain.get("webviewdomain");
		if (shouldInvokeSetWebviewDomain(rawWebview)) {
			List<String> webviewPayload = webviewDomainPayloadForWechat(rawWebview);
			if (!webviewPayload.isEmpty()) {
				wxaOpenPlatformCodeApiClient.setWebviewDomainSet(appId, webviewPayload, false);
			}
		}
	}

	private static boolean shouldInvokeSetWebviewDomain(Object raw) {
		return raw instanceof List<?> list && !list.isEmpty();
	}

	private static List<String> webviewDomainPayloadForWechat(Object raw) {
		if (!(raw instanceof List<?> list)) {
			return new ArrayList<>();
		}
		List<String> out = new ArrayList<>();
		for (Object o : list) {
			if (o != null) {
				out.add(String.valueOf(o));
			}
		}
		return out;
	}
}
