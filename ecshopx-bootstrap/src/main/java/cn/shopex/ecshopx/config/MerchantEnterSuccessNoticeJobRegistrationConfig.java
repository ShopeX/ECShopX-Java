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

import cn.shopex.ecshopx.common.dispatch.MerchantDispatchJobNames;
import cn.shopex.ecshopx.dispatch.DispatchRegistry;
import cn.shopex.ecshopx.promotions.dispatch.MerchantEnterSuccessNoticeJobHandler;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MerchantEnterSuccessNoticeJobRegistrationConfig {

	private final DispatchRegistry dispatchRegistry;
	private final MerchantEnterSuccessNoticeJobHandler merchantEnterSuccessNoticeJobHandler;

	public MerchantEnterSuccessNoticeJobRegistrationConfig(
			DispatchRegistry dispatchRegistry,
			MerchantEnterSuccessNoticeJobHandler merchantEnterSuccessNoticeJobHandler) {
		this.dispatchRegistry = dispatchRegistry;
		this.merchantEnterSuccessNoticeJobHandler = merchantEnterSuccessNoticeJobHandler;
	}

	@PostConstruct
	public void registerMerchantEnterSuccessNoticeJob() {
		dispatchRegistry.registerJob(
				MerchantDispatchJobNames.MERCHANT_ENTER_SUCCESS_NOTICE, merchantEnterSuccessNoticeJobHandler);
	}
}
