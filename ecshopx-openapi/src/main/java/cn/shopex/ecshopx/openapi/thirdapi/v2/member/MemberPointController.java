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
import cn.shopex.ecshopx.common.openapi.OpenapiMemberPointLogV2ListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberPointV2DetailPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberPointV2UpdatePort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiMemberQueryParams;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@OpenapiResponse
@RestController("openapiV2MemberPoint")
@RequestMapping("/api/openapi/internal/v2")
public class MemberPointController extends OpenapiBaseController {

	private final OpenapiMemberPointLogV2ListPort memberPointLogV2ListPort;
	private final OpenapiMemberPointV2UpdatePort memberPointV2UpdatePort;
	private final OpenapiMemberPointV2DetailPort memberPointV2DetailPort;

	public MemberPointController(
			OpenapiMemberPointLogV2ListPort memberPointLogV2ListPort,
			OpenapiMemberPointV2UpdatePort memberPointV2UpdatePort,
			OpenapiMemberPointV2DetailPort memberPointV2DetailPort) {
		this.memberPointLogV2ListPort = memberPointLogV2ListPort;
		this.memberPointV2UpdatePort = memberPointV2UpdatePort;
		this.memberPointV2DetailPort = memberPointV2DetailPort;
	}

	@GetMapping(value = "/ecx.member_point_log.list", name = "开放接口查询会员积分历史记录")
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

		return memberPointLogV2ListPort.listMemberPointLogs(
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

	@GetMapping(value = "/ecx.member_point.detail", name = "开放接口查询会员可用积分")
	public Map<String, Object> detail(
			HttpServletRequest request,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String mobileRaw = OpenapiRequestParams.originalString(mobileParam, body, "mobile");
		return memberPointV2DetailPort.getMemberPointDetail(companyId, mobileRaw);
	}

	@PatchMapping(value = "/ecx.member_point.update", name = "开放接口修改会员积分")
	public Void update(
			HttpServletRequest request,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "increase_point", required = false) String increasePointParam,
			@RequestParam(name = "decrease_point", required = false) String decreasePointParam,
			@RequestParam(name = "record", required = false) String recordParam,
			@RequestParam(name = "external_id", required = false) String externalIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Map<String, Object> merged =
				OpenapiMemberV2UpdatePointHandlerParams.mergeAll(
						body,
						mobileParam,
						increasePointParam,
						decreasePointParam,
						recordParam,
						externalIdParam);
		memberPointV2UpdatePort.updateMemberPoint(companyId, merged);
		return null;
	}
}
