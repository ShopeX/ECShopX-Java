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

import cn.shopex.ecshopx.aliyunsms.dispatch.SyncSmsSignsJobHandler;
import cn.shopex.ecshopx.common.dispatch.AliyunsmsDispatchJobNames;
import cn.shopex.ecshopx.dispatch.DispatchRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AliyunsmsSyncSmsSignsJobRegistrationConfig {

	private final DispatchRegistry dispatchRegistry;
	private final SyncSmsSignsJobHandler syncSmsSignsJobHandler;

	public AliyunsmsSyncSmsSignsJobRegistrationConfig(
			DispatchRegistry dispatchRegistry, SyncSmsSignsJobHandler syncSmsSignsJobHandler) {
		this.dispatchRegistry = dispatchRegistry;
		this.syncSmsSignsJobHandler = syncSmsSignsJobHandler;
	}

	@PostConstruct
	public void registerSyncSmsSignsJob() {
		dispatchRegistry.registerJob(AliyunsmsDispatchJobNames.SYNC_SMS_SIGNS_JOB, syncSmsSignsJobHandler);
	}
}
