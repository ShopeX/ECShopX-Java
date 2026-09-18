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
import cn.shopex.ecshopx.wechat.config.WechatWxaPublishProperties;
import cn.shopex.ecshopx.wechat.domain.Weapp;
import cn.shopex.ecshopx.wechat.service.WechatOpenUserPlatformService;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class WxaSubmitAuditCheckService {

	private final WeappRowQueryService weappRowQueryService;
	private final WechatWxaPublishProperties props;
	private final WxaOpenPlatformBindingInspectService bindingInspect;
	private final WechatOpenUserPlatformService wechatOpenUserPlatformService;

	public WxaSubmitAuditCheckService(
			WeappRowQueryService weappRowQueryService,
			WechatWxaPublishProperties props,
			WxaOpenPlatformBindingInspectService bindingInspect,
			WechatOpenUserPlatformService wechatOpenUserPlatformService) {
		this.weappRowQueryService = weappRowQueryService;
		this.props = props;
		this.bindingInspect = bindingInspect;
		this.wechatOpenUserPlatformService = wechatOpenUserPlatformService;
	}

	public void submitAuditCheck(long companyId, String authorizerAppid, String wxaAppId, String templateName, String wxaName) {
		String tpl = templateName == null ? "" : templateName.trim();
		Optional<Weapp> row = weappRowQueryService.findByCompanyAndWxa(companyId, wxaAppId);
		if (row.isPresent()) {
			String existingTpl = row.get().getTemplateName();
			String existingNorm = existingTpl == null ? "" : existingTpl.trim();
			if (!existingNorm.equals(tpl)) {
				throw new BadRequestException("授权小程序已绑定其他模版，请换一个重试", 400);
			}
		}
		if (Boolean.TRUE.equals(props.getOpenThird())) {
			String auth = authorizerAppid == null ? "" : authorizerAppid.trim();
			if (!bindingInspect.checkWxaBind(auth, wxaAppId)) {
				try {
					wechatOpenUserPlatformService.openCreate(companyId, auth);
				} catch (Exception e) {
					throw new BadRequestException("当前小程序未绑定开放平台，请到微信->开放平台开通", 400);
				}
			}
		}
	}
}
