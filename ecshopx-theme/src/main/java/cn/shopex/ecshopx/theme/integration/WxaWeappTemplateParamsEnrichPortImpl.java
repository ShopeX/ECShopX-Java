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

package cn.shopex.ecshopx.theme.integration;

import cn.shopex.ecshopx.theme.service.PagesTemplateDecoratorContentService;
import cn.shopex.ecshopx.wechat.port.WxaWeappTemplateParamsEnrichPort;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;

@Service
public class WxaWeappTemplateParamsEnrichPortImpl implements WxaWeappTemplateParamsEnrichPort {

	private final PagesTemplateDecoratorContentService pagesTemplateDecoratorContentService;

	public WxaWeappTemplateParamsEnrichPortImpl(
			PagesTemplateDecoratorContentService pagesTemplateDecoratorContentService) {
		this.pagesTemplateDecoratorContentService = pagesTemplateDecoratorContentService;
	}

	@Override
	public void enrichAdminRow(
			long companyId,
			String widgetName,
			LinkedHashMap<String, Object> params,
			Boolean merchantStatusPerItemOrNull) {
		pagesTemplateDecoratorContentService.enrichAdminDecodedWidgetParams(
				companyId, widgetName, params, merchantStatusPerItemOrNull);
	}
}
