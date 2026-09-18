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
import cn.shopex.ecshopx.wechat.domain.WechatAuth;
import cn.shopex.ecshopx.wechat.service.WechatAuthQueryService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class WxaGetTemplateWeappDetailService {

	private final WxaTemplateDomainInfoService wxaTemplateDomainInfoService;
	private final WeappRowQueryService weappRowQueryService;
	private final WechatAuthQueryService wechatAuthQueryService;

	public WxaGetTemplateWeappDetailService(
			WxaTemplateDomainInfoService wxaTemplateDomainInfoService,
			WeappRowQueryService weappRowQueryService,
			WechatAuthQueryService wechatAuthQueryService) {
		this.wxaTemplateDomainInfoService = wxaTemplateDomainInfoService;
		this.weappRowQueryService = weappRowQueryService;
		this.wechatAuthQueryService = wechatAuthQueryService;
	}

	public LinkedHashMap<String, Object> getTemplateWeappDetail(long companyId, Integer templateId) {
		LinkedHashMap<String, Object> detail =
				wxaTemplateDomainInfoService.requireEnabledTemplateDetailByTemplateId(templateId);
		Object kn = detail.get("key_name");
		String keyNameForWeapp = kn == null ? null : String.valueOf(kn);
		Optional<Map<String, Object>> weappOpt =
				weappRowQueryService.findWeappPayloadByCompanyAndTemplateName(companyId, keyNameForWeapp);
		if (weappOpt.isEmpty()) {
			detail.put("authorizer", new LinkedHashMap<String, Object>());
			return detail;
		}
		Map<String, Object> wxaAppData = weappOpt.get();
		Object rawAppid = wxaAppData.get("authorizer_appid");
		if (rawAppid == null || String.valueOf(rawAppid).trim().isEmpty()) {
			throw new ResourceException("授权数据不一致");
		}
		Optional<WechatAuth> authOpt = wechatAuthQueryService.findBoundAuthByCompanyAndAuthorizerAppid(
				companyId, String.valueOf(rawAppid).trim());
		if (authOpt.isEmpty()) {
			throw new ResourceException("授权数据不一致");
		}
		WechatAuth auth = authOpt.get();
		LinkedHashMap<String, Object> authorizer = new LinkedHashMap<>();
		authorizer.put("authorizer_appid", auth.getAuthorizerAppid());
		authorizer.put("authorizer_appsecret", auth.getAuthorizerAppsecret());
		authorizer.put("auto_publish", auth.getAutoPublish());
		authorizer.put("nick_name", auth.getNickName());
		authorizer.put("head_img", auth.getHeadImg());
		authorizer.put("service_type_info", auth.getServiceTypeInfo());
		authorizer.put("verify_type_info", auth.getVerifyTypeInfo());
		authorizer.put("signature", auth.getSignature());
		authorizer.put("principal_name", auth.getPrincipalName());
		authorizer.put("business_info", wechatAuthQueryService.jsonColumnToJavaObject(auth.getBusinessInfo()));
		authorizer.put("qrcode_url", auth.getQrcodeUrl());
		authorizer.put("operator_id", auth.getOperatorId());
		authorizer.put("bind_status", auth.getBindStatus());
		authorizer.put("company_id", auth.getCompanyId());
		authorizer.put("is_direct", auth.getIsDirect());
		authorizer.put("weapp", wxaAppData);
		detail.put("authorizer", authorizer);
		return detail;
	}
}
