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

import cn.shopex.ecshopx.common.dispatch.MembersBundleDispatchJobNames;
import cn.shopex.ecshopx.dispatch.DispatchRegistry;
import cn.shopex.ecshopx.members.dispatch.BindSalsepersonJobHandler;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BindSalsepersonJobRegistrationConfig {

	private final DispatchRegistry dispatchRegistry;
	private final BindSalsepersonJobHandler bindSalsepersonJobHandler;

	public BindSalsepersonJobRegistrationConfig(
			DispatchRegistry dispatchRegistry, BindSalsepersonJobHandler bindSalsepersonJobHandler) {
		this.dispatchRegistry = dispatchRegistry;
		this.bindSalsepersonJobHandler = bindSalsepersonJobHandler;
	}

	@PostConstruct
	public void registerBindSalsepersonJob() {
		dispatchRegistry.registerJob(MembersBundleDispatchJobNames.BIND_SALSEPERSON_JOB, bindSalsepersonJobHandler);
		dispatchRegistry.registerJob(
				MembersBundleDispatchJobNames.BIND_SALSEPERSON_JOB_DM_MEMBER_REGISTER, bindSalsepersonJobHandler);
		dispatchRegistry.registerJob(
				MembersBundleDispatchJobNames.BIND_SALSEPERSON_JOB_WXAPP_BIND_SALESPERSON, bindSalsepersonJobHandler);
	}
}
