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
import cn.shopex.ecshopx.common.wechat.WxaSubscribeMessageTemplateAdminPort;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
		prefix = "ecshopx.wechat.wxa.subscribe-template",
		name = "http-enabled",
		havingValue = "false",
		matchIfMissing = true)
public class WxaSubscribeMessageTemplateAdminNoOp implements WxaSubscribeMessageTemplateAdminPort {

	private final MessageSource messageSource;

	public WxaSubscribeMessageTemplateAdminNoOp(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@Override
	public String addTemplate(
			String authorizerAppid, String wxaLibraryTemplateId, List<Integer> keywordIdList, String sceneDescription) {
		Locale locale = LocaleContextHolder.getLocale();
		throw new ResourceException(
				messageSource.getMessage("common.wxa.subscribe_template.http_disabled", null, locale));
	}

	@Override
	public boolean deleteTemplate(String authorizerAppid, String priTemplateId) {
		return false;
	}
}
