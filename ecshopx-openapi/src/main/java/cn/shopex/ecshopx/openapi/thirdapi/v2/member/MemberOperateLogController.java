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

package cn.shopex.ecshopx.openapi.thirdapi.v2.member;

import cn.shopex.ecshopx.common.annotation.OpenapiResponse;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberOperateLogV2ListPort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiMemberQueryParams;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@OpenapiResponse
@RestController("openapiV2MemberOperateLog")
@RequestMapping("/api/openapi/internal/v2")
public class MemberOperateLogController extends OpenapiBaseController {

	private final OpenapiMemberOperateLogV2ListPort memberOperateLogV2ListPort;

	public MemberOperateLogController(OpenapiMemberOperateLogV2ListPort memberOperateLogV2ListPort) {
		this.memberOperateLogV2ListPort = memberOperateLogV2ListPort;
	}

	@GetMapping(value = "/ecx.member_operate_log.list", name = "开放接口查询会员信息操作日志")
	public Map<String, Object> list(
			HttpServletRequest request,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "start_date", required = false) String startDateParam,
			@RequestParam(name = "end_date", required = false) String endDateParam,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);

		OpenapiThirdApiV2MemberListParams.PageSpec pageSpec =
				OpenapiThirdApiV2MemberListParams.resolve(pageParam, pageSizeParam, body);

		boolean mobilePresent = OpenapiMemberQueryParams.isParamPresent(mobileParam, body, "mobile");
		String mobileRaw = OpenapiRequestParams.originalString(mobileParam, body, "mobile");
		boolean startDatePresent =
				OpenapiMemberQueryParams.isParamPresent(startDateParam, body, "start_date");
		String startDateRaw = OpenapiRequestParams.originalString(startDateParam, body, "start_date");
		boolean endDatePresent =
				OpenapiMemberQueryParams.isParamPresent(endDateParam, body, "end_date");
		String endDateRaw = OpenapiRequestParams.originalString(endDateParam, body, "end_date");

		return memberOperateLogV2ListPort.listMemberOperateLogs(
				companyId,
				mobilePresent,
				mobileRaw,
				startDatePresent,
				startDateRaw,
				endDatePresent,
				endDateRaw,
				pageSpec.page(),
				pageSpec.pageSize());
	}
}
