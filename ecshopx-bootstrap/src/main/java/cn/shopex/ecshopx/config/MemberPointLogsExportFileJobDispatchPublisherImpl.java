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
import cn.shopex.ecshopx.common.dispatch.MemberPointLogsExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import cn.shopex.ecshopx.point.dispatch.MemberPointLogsExportFileJobTypes;
import cn.shopex.ecshopx.point.service.export.PointMemberLogExportContext;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Component;

@Component
public class MemberPointLogsExportFileJobDispatchPublisherImpl implements MemberPointLogsExportFileJobDispatchPublisher {

	private final DispatchFacade dispatchFacade;

	public MemberPointLogsExportFileJobDispatchPublisherImpl(DispatchFacade dispatchFacade) {
		this.dispatchFacade = dispatchFacade;
	}

	@Override
	public void enqueueMemberPointLogsExport(PointMemberLogExportContext ctx) {
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", ctx.companyId());
		filter.put("datapass_block", ctx.datapassBlock() ? 1 : 0);
		filter.put("user_id", ctx.userIdParam());
		filter.put("mobile", ctx.mobile());
		filter.put("username", ctx.username());
		filter.put("name", ctx.name());
		if (ctx.dateBegin() != null) {
			filter.put("date_begin", ctx.dateBegin());
		}
		if (ctx.dateEnd() != null) {
			filter.put("date_end", ctx.dateEnd());
		}

		LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", MemberPointLogsExportFileJobTypes.TYPE_MEMBER_POINT_LOGS);
		payload.put("company_id", ctx.companyId());
		payload.put("operator_id", ctx.operatorId());
		payload.put("supplier_id", ctx.supplierId());
		payload.put("filter", filter);

		dispatchFacade.dispatchJob(
				EspierDispatchJobNames.EXPORT_FILE_JOB_MEMBER_POINT_LOGS,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						"slow",
						null,
						RetryPolicy.platformDefault()));
	}
}
