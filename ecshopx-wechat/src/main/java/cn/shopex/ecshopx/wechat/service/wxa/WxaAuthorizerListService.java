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

import cn.shopex.ecshopx.wechat.domain.Weapp;
import cn.shopex.ecshopx.wechat.domain.WechatAuth;
import cn.shopex.ecshopx.wechat.mapper.WeappMapper;
import cn.shopex.ecshopx.wechat.service.WechatAuthQueryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class WxaAuthorizerListService {

	private static final Logger log = LoggerFactory.getLogger(WxaAuthorizerListService.class);

	private final WechatAuthQueryService wechatAuthQueryService;
	private final WeappMapper weappMapper;
	private final WeappRowQueryService weappRowQueryService;
	private final WxaTemplateDomainInfoService wxaTemplateDomainInfoService;
	private final SuperadminWxappTemplateMetadataService superadminWxappTemplateMetadataService;

	public WxaAuthorizerListService(
			WechatAuthQueryService wechatAuthQueryService,
			WeappMapper weappMapper,
			WeappRowQueryService weappRowQueryService,
			WxaTemplateDomainInfoService wxaTemplateDomainInfoService,
			SuperadminWxappTemplateMetadataService superadminWxappTemplateMetadataService) {
		this.wechatAuthQueryService = wechatAuthQueryService;
		this.weappMapper = weappMapper;
		this.weappRowQueryService = weappRowQueryService;
		this.wxaTemplateDomainInfoService = wxaTemplateDomainInfoService;
		this.superadminWxappTemplateMetadataService = superadminWxappTemplateMetadataService;
	}

	public List<Map<String, Object>> getWxaList(long companyId) {
		List<WechatAuth> authRows = wechatAuthQueryService.listBoundMiniProgramsForCompany(companyId);
		if (authRows.isEmpty()) {
			return List.of();
		}
		Map<String, LinkedHashMap<String, Object>> weappByAppId = new LinkedHashMap<>();
		try {
			LambdaQueryWrapper<Weapp> w = new LambdaQueryWrapper<>();
			w.eq(Weapp::getCompanyId, companyId).isNull(Weapp::getDeletedAt);
			List<Weapp> weapps = weappMapper.selectList(w);
			if (weapps != null) {
				for (Weapp weapp : weapps) {
					String aid = weapp.getAuthorizerAppid();
					if (aid == null || aid.isBlank()) {
						continue;
					}
					weappByAppId.put(aid.trim(), weappRowQueryService.toAuthorizerListStyleMap(weapp));
				}
			}
		} catch (DataAccessException e) {
			log.warn("wechat_weapp 列表查询失败: companyId={}", companyId, e);
		}

		Map<String, LinkedHashMap<String, Object>> templateByKeyName =
				wxaTemplateDomainInfoService.listWxappTemplatesMergedAsDataList();

		List<Map<String, Object>> out = new ArrayList<>(authRows.size());
		for (WechatAuth row : authRows) {
			String appid = row.getAuthorizerAppid();
			Map<String, Object> weappRow = appid == null ? null : weappByAppId.get(appid);
			Object weappPayload = weappRow != null ? weappRow : "";

			String templateName = "";
			if (weappRow != null) {
				Object tn = weappRow.get("template_name");
				templateName = tn == null ? "" : String.valueOf(tn).trim();
			}

			Map<String, Object> templateData = templateByKeyName.get(templateName);
			if (templateData == null) {
				templateData =
						superadminWxappTemplateMetadataService
								.tryResolveTemplateRowForAuthorizerList(templateName)
								.orElse(null);
			}
			Object templatePayload;
			if (templateData != null && !templateData.isEmpty()) {
				templatePayload = templateData;
			} else {
				templatePayload = "";
			}

			LinkedHashMap<String, Object> item = new LinkedHashMap<>();
			item.put("authorizer_appid", row.getAuthorizerAppid());
			item.put("authorizer_appsecret", row.getAuthorizerAppsecret());
			item.put("auto_publish", row.getAutoPublish());
			item.put("is_direct", row.getIsDirect());
			item.put("nick_name", row.getNickName());
			item.put("head_img", row.getHeadImg());
			item.put("verify_type_info", row.getVerifyTypeInfo());
			item.put("qrcode_url", row.getQrcodeUrl());
			item.put("principal_name", row.getPrincipalName());
			item.put("signature", row.getSignature());
			item.put("weapp", weappPayload);
			item.put("weappTemplate", templatePayload);
			out.add(item);
		}
		return out;
	}
}
