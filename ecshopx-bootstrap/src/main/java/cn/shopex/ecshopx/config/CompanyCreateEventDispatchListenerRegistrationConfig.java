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

package cn.shopex.ecshopx.config;

import cn.shopex.ecshopx.common.dispatch.CompanysDispatchEventNames;
import cn.shopex.ecshopx.companys.dispatch.CompanyCreateDeveloperShuyunDispatchListener;
import cn.shopex.ecshopx.companys.dispatch.CompanyCreateInitDemoDataDispatchListener;
import cn.shopex.ecshopx.companys.dispatch.CompanyCreateOnlineOpenCallbackDispatchListener;
import cn.shopex.ecshopx.companys.dispatch.CompanyCreateOnlineOpenEmailDispatchListener;
import cn.shopex.ecshopx.companys.dispatch.CompanyCreateOnlineOpenSmsDispatchListener;
import cn.shopex.ecshopx.dispatch.DispatchRegistry;
import cn.shopex.ecshopx.dispatch.ListenerDispatchOptions;
import cn.shopex.ecshopx.kaquan.dispatch.CompanyCreateDefaultMemberGradeDispatchListener;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@Order(10)
public class CompanyCreateEventDispatchListenerRegistrationConfig {

	private final DispatchRegistry dispatchRegistry;
	private final CompanyCreateDeveloperShuyunDispatchListener companyCreateDeveloperShuyunDispatchListener;
	private final CompanyCreateDefaultMemberGradeDispatchListener companyCreateDefaultMemberGradeDispatchListener;
	private final CompanyCreateOnlineOpenCallbackDispatchListener companyCreateOnlineOpenCallbackDispatchListener;
	private final CompanyCreateOnlineOpenSmsDispatchListener companyCreateOnlineOpenSmsDispatchListener;
	private final CompanyCreateOnlineOpenEmailDispatchListener companyCreateOnlineOpenEmailDispatchListener;
	private final CompanyCreateInitDemoDataDispatchListener companyCreateInitDemoDataDispatchListener;

	public CompanyCreateEventDispatchListenerRegistrationConfig(
			DispatchRegistry dispatchRegistry,
			CompanyCreateDeveloperShuyunDispatchListener companyCreateDeveloperShuyunDispatchListener,
			CompanyCreateDefaultMemberGradeDispatchListener companyCreateDefaultMemberGradeDispatchListener,
			CompanyCreateOnlineOpenCallbackDispatchListener companyCreateOnlineOpenCallbackDispatchListener,
			CompanyCreateOnlineOpenSmsDispatchListener companyCreateOnlineOpenSmsDispatchListener,
			CompanyCreateOnlineOpenEmailDispatchListener companyCreateOnlineOpenEmailDispatchListener,
			CompanyCreateInitDemoDataDispatchListener companyCreateInitDemoDataDispatchListener) {
		this.dispatchRegistry = dispatchRegistry;
		this.companyCreateDeveloperShuyunDispatchListener = companyCreateDeveloperShuyunDispatchListener;
		this.companyCreateDefaultMemberGradeDispatchListener = companyCreateDefaultMemberGradeDispatchListener;
		this.companyCreateOnlineOpenCallbackDispatchListener = companyCreateOnlineOpenCallbackDispatchListener;
		this.companyCreateOnlineOpenSmsDispatchListener = companyCreateOnlineOpenSmsDispatchListener;
		this.companyCreateOnlineOpenEmailDispatchListener = companyCreateOnlineOpenEmailDispatchListener;
		this.companyCreateInitDemoDataDispatchListener = companyCreateInitDemoDataDispatchListener;
	}

	@PostConstruct
	public void registerCompanyCreateEventListeners() {
		dispatchRegistry.registerEventListener(
				CompanysDispatchEventNames.EVENT_COMPANY_CREATE,
				"listener:companys.company_create_developer_shuyun",
				ListenerDispatchOptions.asyncDefaults(),
				companyCreateDeveloperShuyunDispatchListener);
		dispatchRegistry.registerEventListener(
				CompanysDispatchEventNames.EVENT_COMPANY_CREATE,
				"listener:kaquan.company_create_default_member_grade",
				ListenerDispatchOptions.asyncDefaults(),
				companyCreateDefaultMemberGradeDispatchListener);
		dispatchRegistry.registerEventListener(
				CompanysDispatchEventNames.EVENT_COMPANY_CREATE,
				"listener:companys.company_create_online_open_callback",
				ListenerDispatchOptions.asyncDefaults(),
				companyCreateOnlineOpenCallbackDispatchListener);
		dispatchRegistry.registerEventListener(
				CompanysDispatchEventNames.EVENT_COMPANY_CREATE,
				"listener:companys.company_create_online_open_sms",
				ListenerDispatchOptions.asyncDefaults(),
				companyCreateOnlineOpenSmsDispatchListener);
		dispatchRegistry.registerEventListener(
				CompanysDispatchEventNames.EVENT_COMPANY_CREATE,
				"listener:companys.company_create_online_open_email",
				ListenerDispatchOptions.asyncDefaults(),
				companyCreateOnlineOpenEmailDispatchListener);
		dispatchRegistry.registerEventListener(
				CompanysDispatchEventNames.EVENT_COMPANY_CREATE,
				"listener:companys.company_create_init_demo_data",
				ListenerDispatchOptions.asyncDefaults(),
				companyCreateInitDemoDataDispatchListener);
	}
}
