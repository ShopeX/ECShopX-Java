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
import cn.shopex.ecshopx.companys.service.companycreate.CompanyInitDemoDataService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CompanyCreateInitDemoDataDispatchListener implements DispatchListener {

	private final CompanyInitDemoDataService companyInitDemoDataService;

	@Value("${ecshopx.request-field.oem-shuyun:false}")
	private boolean oemShuyun;

	public CompanyCreateInitDemoDataDispatchListener(CompanyInitDemoDataService companyInitDemoDataService) {
		this.companyInitDemoDataService = companyInitDemoDataService;
	}

	@Override
	public void onEvent(Map<String, Object> payload) {
		if (oemShuyun) {
			return;
		}
		Object raw = payload.get("company_id");
		if (raw == null) {
			return;
		}
		long companyId = ((Number) raw).longValue();
		companyInitDemoDataService.initialize(companyId);
	}
}
