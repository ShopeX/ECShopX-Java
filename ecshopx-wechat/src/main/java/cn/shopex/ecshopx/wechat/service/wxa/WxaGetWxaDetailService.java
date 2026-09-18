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
import cn.shopex.ecshopx.wechat.domain.Weapp;
import cn.shopex.ecshopx.wechat.domain.WechatAuth;
import cn.shopex.ecshopx.wechat.service.WechatAuthQueryService;
import cn.shopex.ecshopx.wechat.wxa.WechatOpenPlatformAuthorizerTokenService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WxaGetWxaDetailService {

	private final WechatAuthQueryService wechatAuthQueryService;
	private final WechatOpenPlatformAuthorizerTokenService wechatOpenPlatformAuthorizerTokenService;
	private final WeappRowQueryService weappRowQueryService;
	private final WxaTemplateDomainInfoService wxaTemplateDomainInfoService;
	private final SuperadminWxappTemplateMetadataService superadminWxappTemplateMetadataService;

	public WxaGetWxaDetailService(
			WechatAuthQueryService wechatAuthQueryService,
			WechatOpenPlatformAuthorizerTokenService wechatOpenPlatformAuthorizerTokenService,
			WeappRowQueryService weappRowQueryService,
			WxaTemplateDomainInfoService wxaTemplateDomainInfoService,
			SuperadminWxappTemplateMetadataService superadminWxappTemplateMetadataService) {
		this.wechatAuthQueryService = wechatAuthQueryService;
		this.wechatOpenPlatformAuthorizerTokenService = wechatOpenPlatformAuthorizerTokenService;
		this.weappRowQueryService = weappRowQueryService;
		this.wxaTemplateDomainInfoService = wxaTemplateDomainInfoService;
		this.superadminWxappTemplateMetadataService = superadminWxappTemplateMetadataService;
	}

	public Map<String, Object> getWxaDetail(long companyId, String wxaAppIdRaw) {
		String appid = wxaAppIdRaw == null ? "" : wxaAppIdRaw.trim();

		Optional<WechatAuth> detailOpt =
				wechatAuthQueryService.findBoundAuthByCompanyAndAuthorizerAppid(companyId, appid);
		if (detailOpt.isEmpty()) {
			throw new ResourceException("小程序未绑定，请重新绑定", 400, 400001);
		}

		WechatAuth detail = detailOpt.get();
		wechatOpenPlatformAuthorizerTokenService.getAuthorizerAccessToken(appid);

		Optional<Weapp> weappOpt = weappRowQueryService.findByCompanyAndWxa(companyId, appid);
		Object weappPayload = weappOpt.isEmpty() ? "" : weappRowQueryService.toAuthorizerListStyleMap(weappOpt.get());

		String templateName = "";
		if (weappPayload instanceof Map<?, ?> weappRow) {
			Object tn = weappRow.get("template_name");
			templateName = tn == null ? "" : String.valueOf(tn).trim();
		}

		Map<String, LinkedHashMap<String, Object>> templateByKey =
				wxaTemplateDomainInfoService.listWxappTemplatesMergedAsDataList();
		Map<String, Object> templateData = templateName.isEmpty() ? null : templateByKey.get(templateName);
		if (StringUtils.hasText(templateName) && (templateData == null || templateData.isEmpty())) {
			templateData =
					superadminWxappTemplateMetadataService
							.tryResolveTemplateRowForAuthorizerList(templateName)
							.orElse(null);
		}

		Object weappTemplatePayload;
		if (templateData != null && !templateData.isEmpty()) {
			weappTemplatePayload = templateData;
		} else {
			weappTemplatePayload = "";
		}

		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("authorizer_appid", detail.getAuthorizerAppid());
		result.put("authorizer_appsecret", detail.getAuthorizerAppsecret());
		result.put("auto_publish", detail.getAutoPublish());
		result.put("nick_name", detail.getNickName());
		result.put("head_img", detail.getHeadImg());
		result.put("service_type_info", detail.getServiceTypeInfo());
		result.put("verify_type_info", detail.getVerifyTypeInfo());
		result.put("signature", detail.getSignature());
		result.put("principal_name", detail.getPrincipalName());
		result.put("business_info", wechatAuthQueryService.jsonColumnToJavaObject(detail.getBusinessInfo()));
		result.put("qrcode_url", detail.getQrcodeUrl());
		result.put("operator_id", detail.getOperatorId());
		result.put("bind_status", detail.getBindStatus());
		result.put("company_id", detail.getCompanyId());
		result.put("weapp", weappPayload);
		result.put("weappTemplate", weappTemplatePayload);
		return result;
	}
}
