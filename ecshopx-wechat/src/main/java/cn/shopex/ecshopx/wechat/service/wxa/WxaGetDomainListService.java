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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.wechat.service.WechatAuthQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxaGetDomainListService {

	private static final Logger log = LoggerFactory.getLogger(WxaGetDomainListService.class);

	private final WechatAuthQueryService wechatAuthQueryService;
	private final WxaOpenPlatformCodeApiClient wxaOpenPlatformCodeApiClient;
	private final WxaTemplateDomainInfoService wxaTemplateDomainInfoService;
	private final ObjectMapper objectMapper;

	public WxaGetDomainListService(
			WechatAuthQueryService wechatAuthQueryService,
			WxaOpenPlatformCodeApiClient wxaOpenPlatformCodeApiClient,
			WxaTemplateDomainInfoService wxaTemplateDomainInfoService,
			ObjectMapper objectMapper) {
		this.wechatAuthQueryService = wechatAuthQueryService;
		this.wxaOpenPlatformCodeApiClient = wxaOpenPlatformCodeApiClient;
		this.wxaTemplateDomainInfoService = wxaTemplateDomainInfoService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getDomainList(long companyId, String wxaAppId, String templateName) {
		if (!StringUtils.hasText(wxaAppId)
				|| !wechatAuthQueryService.isMiniProgramWxaBoundToCompany(companyId, wxaAppId)) {
			throw new BadRequestException("小程序未绑定，请重新绑定", 400001);
		}
		Map<String, Object> wxDomain = wxaOpenPlatformCodeApiClient.queryMergedWxDomainConfig(wxaAppId);
		Map<String, Object> localDomain = wxaTemplateDomainInfoService.buildLocalDomainStructure(templateName);
		logWxDomain(wxDomain);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("wxDomain", wxDomain);
		data.put("localDomain", localDomain);
		return data;
	}

	private void logWxDomain(Map<String, Object> wxDomain) {
		try {
			log.info("获取小程序业务域名和其他的域名的配置参数：{}", objectMapper.writeValueAsString(wxDomain));
		} catch (Exception ignored) {
			log.info("获取小程序业务域名和其他的域名的配置参数：{}", String.valueOf(wxDomain));
		}
	}
}
