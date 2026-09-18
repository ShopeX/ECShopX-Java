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

package cn.shopex.ecshopx.companys.dispatch;

import cn.shopex.ecshopx.common.dispatch.DispatchListener;
import cn.shopex.ecshopx.thirdparty.service.prism.PrismIshopexFacade;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CompanyCreateOnlineOpenCallbackDispatchListener implements DispatchListener {

	private final PrismIshopexFacade prismIshopexFacade;

	@Value("${ecshopx.request-field.oem-shuyun:false}")
	private boolean oemShuyun;

	@Value("${common.system-is-saas:false}")
	private boolean systemIsSaas;

	@Value("${common.shop-admin-url:}")
	private String shopAdminUrl;

	public CompanyCreateOnlineOpenCallbackDispatchListener(PrismIshopexFacade prismIshopexFacade) {
		this.prismIshopexFacade = prismIshopexFacade;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		if (oemShuyun || !systemIsSaas) {
			return;
		}
		String issueId = stringOrNull(payload.get("issue_id"));
		if (!StringUtils.hasText(issueId)) {
			return;
		}
		if (!StringUtils.hasText(shopAdminUrl)) {
			return;
		}
		prismIshopexFacade.onlineOpenCallback(issueId.trim(), shopAdminUrl.trim());
	}

	private static String stringOrNull(Object o) {
		if (o == null) {
			return null;
		}
		String s = o.toString().trim();
		return s.isEmpty() ? null : s;
	}
}
