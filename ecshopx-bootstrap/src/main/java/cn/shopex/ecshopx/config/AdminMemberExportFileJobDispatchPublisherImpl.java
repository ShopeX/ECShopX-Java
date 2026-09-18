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

import cn.shopex.ecshopx.common.dispatch.EspierDispatchJobNames;
import cn.shopex.ecshopx.members.dispatch.AdminMemberExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.members.dispatch.AdminMemberExportFileJobPayloadSupport;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import cn.shopex.ecshopx.members.service.export.AdminMemberExportJobContext;
import org.springframework.stereotype.Component;

@Component
public class AdminMemberExportFileJobDispatchPublisherImpl implements AdminMemberExportFileJobDispatchPublisher {

	/**
	 * Queue name passed to {@link DispatchOptions}: default {@code slow} until the upstream API
	 * repository is available to verify the canonical queue name; align options and tests when verified.
	 */
	public static final String ADMIN_MEMBER_EXPORT_JOB_QUEUE = "slow";

	private final DispatchFacade dispatchFacade;

	public AdminMemberExportFileJobDispatchPublisherImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public void enqueueAdminMemberExport(AdminMemberExportJobContext ctx) {
		var payload = AdminMemberExportFileJobPayloadSupport.toPayload(ctx);
		dispatchFacade.dispatchJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_ADMIN_MEMBER_EXPORT,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						ADMIN_MEMBER_EXPORT_JOB_QUEUE,
						null,
						RetryPolicy.platformDefault()));
	}
}
