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

package cn.shopex.ecshopx.orders.service.shippingtemplate;

import cn.shopex.ecshopx.companys.service.CommonLangModReadService;
import cn.shopex.ecshopx.espier.api.admin.v1.EspierAdminJwtControllerSupport;
import cn.shopex.ecshopx.orders.domain.ShippingTemplates;
import cn.shopex.ecshopx.orders.repository.ShippingTemplatesQueryRepository;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ShippingTemplateAdminInfoService {

	private final ShippingTemplatesQueryRepository shippingTemplatesQueryRepository;
	private final ShippingTemplateAdminCreateService shippingTemplateAdminCreateService;
	private final CommonLangModReadService commonLangModReadService;

	public ShippingTemplateAdminInfoService(
			ShippingTemplatesQueryRepository shippingTemplatesQueryRepository,
			ShippingTemplateAdminCreateService shippingTemplateAdminCreateService,
			CommonLangModReadService commonLangModReadService) {
		this.shippingTemplatesQueryRepository = shippingTemplatesQueryRepository;
		this.shippingTemplateAdminCreateService = shippingTemplateAdminCreateService;
		this.commonLangModReadService = commonLangModReadService;
	}

	public Object getShippingTemplatesInfo(String templateIdStr, long companyId, String acceptLanguageHeader) {
		Long templateId = EspierAdminJwtControllerSupport.parseLongOrNull(templateIdStr);
		if (templateId == null || templateId <= 0L) {
			return Collections.emptyList();
		}
		Optional<ShippingTemplates> rowOpt =
				shippingTemplatesQueryRepository.findByTemplateIdAndCompanyId(companyId, templateId);
		if (rowOpt.isEmpty()) {
			return Collections.emptyList();
		}
		ShippingTemplates entity = rowOpt.get();
		Map<String, Object> data = shippingTemplateAdminCreateService.toTemplateDetailRow(entity);
		commonLangModReadService.applyShippingTemplateDetailNameLangOverlay(companyId, data, acceptLanguageHeader);
		return data;
	}
}
