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

package cn.shopex.ecshopx.openapi.thirdapi.v2.orders;

import cn.shopex.ecshopx.common.annotation.OpenapiResponse;
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.openapi.OpenapiAftersalesDetailPort;
import cn.shopex.ecshopx.common.openapi.OpenapiAftersalesIncrListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiAftersalesListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiRefundDetailPort;
import cn.shopex.ecshopx.common.openapi.OpenapiRefundIncrListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiRefundListPort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiMemberQueryParams;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OpenAPI v2 handler scaffold — methods added during migration. */
@OpenapiResponse
@RestController("openapiV2Aftersales")
@RequestMapping("/api/openapi/internal/v2")
public class AftersalesController extends OpenapiBaseController {

	private final OpenapiAftersalesListPort aftersalesListPort;
	private final OpenapiAftersalesIncrListPort aftersalesIncrListPort;
	private final OpenapiAftersalesDetailPort aftersalesDetailPort;
	private final OpenapiRefundListPort refundListPort;
	private final OpenapiRefundIncrListPort refundIncrListPort;
	private final OpenapiRefundDetailPort refundDetailPort;

	public AftersalesController(
			OpenapiAftersalesListPort aftersalesListPort,
			OpenapiAftersalesIncrListPort aftersalesIncrListPort,
			OpenapiAftersalesDetailPort aftersalesDetailPort,
			OpenapiRefundListPort refundListPort,
			OpenapiRefundIncrListPort refundIncrListPort,
			OpenapiRefundDetailPort refundDetailPort) {
		this.aftersalesListPort = aftersalesListPort;
		this.aftersalesIncrListPort = aftersalesIncrListPort;
		this.aftersalesDetailPort = aftersalesDetailPort;
		this.refundListPort = refundListPort;
		this.refundIncrListPort = refundIncrListPort;
		this.refundDetailPort = refundDetailPort;
	}

	@Activated(routeAlias = "aftersales.list")
	@GetMapping(value = "/ecx.aftersales.get", name = "开放接口售后单列表")
	public Map<String, Object> getAftersalesList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "time_begin", required = false) String timeBeginParam,
			@RequestParam(name = "time_end", required = false) String timeEndParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2AftersalesListParams.PageSpec pageSpec =
				OpenapiThirdApiV2AftersalesListParams.resolve(pageParam, pageSizeParam, body);

		String mobileRaw = OpenapiRequestParams.originalString(mobileParam, body, "mobile");
		String timeBeginRaw = OpenapiRequestParams.originalString(timeBeginParam, body, "time_begin");
		String timeEndRaw = OpenapiRequestParams.originalString(timeEndParam, body, "time_end");

		return aftersalesListPort.list(
				companyId,
				pageSpec.page(),
				pageSpec.pageSize(),
				OpenapiMemberQueryParams.isPhpTruthy(mobileRaw),
				mobileRaw,
				OpenapiMemberQueryParams.isPhpTruthy(timeBeginRaw),
				timeBeginRaw,
				OpenapiMemberQueryParams.isPhpTruthy(timeEndRaw),
				timeEndRaw);
	}

	@GetMapping(value = "/ecx.aftersales.incr.get", name = "开放接口增量售后单列表")
	public Map<String, Object> getIncrAftersalesList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "start_modified", required = false) String startModifiedParam,
			@RequestParam(name = "end_modified", required = false) String endModifiedParam,
			@RequestParam(name = "aftersales_type", required = false) String aftersalesTypeParam,
			@RequestParam(name = "aftersales_status", required = false) String aftersalesStatusParam,
			@RequestParam(name = "progress", required = false) String progressParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2AftersalesListParams.PageSpec pageSpec =
				OpenapiThirdApiV2AftersalesListParams.resolve(pageParam, pageSizeParam, body);

		String mobileRaw = OpenapiRequestParams.originalString(mobileParam, body, "mobile");
		String startModifiedRaw =
				OpenapiRequestParams.originalString(startModifiedParam, body, "start_modified");
		String endModifiedRaw =
				OpenapiRequestParams.originalString(endModifiedParam, body, "end_modified");
		String aftersalesTypeRaw =
				OpenapiRequestParams.originalString(aftersalesTypeParam, body, "aftersales_type");
		String aftersalesStatusRaw =
				OpenapiRequestParams.originalString(aftersalesStatusParam, body, "aftersales_status");
		String progressRaw = OpenapiRequestParams.originalString(progressParam, body, "progress");

		return aftersalesIncrListPort.incrList(
				companyId,
				pageSpec.page(),
				pageSpec.pageSize(),
				OpenapiMemberQueryParams.isPhpTruthy(mobileRaw),
				mobileRaw,
				OpenapiMemberQueryParams.isPhpTruthy(startModifiedRaw),
				startModifiedRaw,
				OpenapiMemberQueryParams.isPhpTruthy(endModifiedRaw),
				endModifiedRaw,
				OpenapiMemberQueryParams.isPhpTruthy(aftersalesTypeRaw),
				aftersalesTypeRaw,
				OpenapiMemberQueryParams.isPhpTruthy(aftersalesStatusRaw),
				aftersalesStatusRaw,
				OpenapiMemberQueryParams.isPhpTruthy(progressRaw),
				progressRaw);
	}

	@GetMapping(value = "/ecx.refund.get", name = "开放接口退款单列表")
	public Map<String, Object> getRefundList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "time_begin", required = false) String timeBeginParam,
			@RequestParam(name = "time_end", required = false) String timeEndParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2AftersalesListParams.PageSpec pageSpec =
				OpenapiThirdApiV2AftersalesListParams.resolve(pageParam, pageSizeParam, body);

		String mobileRaw = OpenapiRequestParams.originalString(mobileParam, body, "mobile");
		String timeBeginRaw = OpenapiRequestParams.originalString(timeBeginParam, body, "time_begin");
		String timeEndRaw = OpenapiRequestParams.originalString(timeEndParam, body, "time_end");

		return refundListPort.list(
				companyId,
				pageSpec.page(),
				pageSpec.pageSize(),
				OpenapiMemberQueryParams.isPhpTruthy(mobileRaw),
				mobileRaw,
				OpenapiMemberQueryParams.isPhpTruthy(timeBeginRaw),
				timeBeginRaw,
				OpenapiMemberQueryParams.isPhpTruthy(timeEndRaw),
				timeEndRaw);
	}

	@GetMapping(value = "/ecx.refund.incr.get", name = "开放接口增量退款单列表")
	public Map<String, Object> getIncrRefundList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "start_modified", required = false) String startModifiedParam,
			@RequestParam(name = "end_modified", required = false) String endModifiedParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2AftersalesListParams.PageSpec pageSpec =
				OpenapiThirdApiV2AftersalesListParams.resolve(pageParam, pageSizeParam, body);

		String mobileRaw = OpenapiRequestParams.originalString(mobileParam, body, "mobile");
		String startModifiedRaw =
				OpenapiRequestParams.originalString(startModifiedParam, body, "start_modified");
		String endModifiedRaw =
				OpenapiRequestParams.originalString(endModifiedParam, body, "end_modified");

		return refundIncrListPort.incrList(
				companyId,
				pageSpec.page(),
				pageSpec.pageSize(),
				OpenapiMemberQueryParams.isPhpTruthy(mobileRaw),
				mobileRaw,
				OpenapiMemberQueryParams.isPhpTruthy(startModifiedRaw),
				startModifiedRaw,
				OpenapiMemberQueryParams.isPhpTruthy(endModifiedRaw),
				endModifiedRaw);
	}

	@PostMapping(value = "/ecx.aftersales.detail.get", name = "开放接口售后详情")
	public Map<String, Object> getAftersalesDetail(
			HttpServletRequest request,
			@RequestParam(name = "aftersales_bn", required = false) String aftersalesBnParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		return aftersalesDetailPort.getAftersalesDetail(
				companyId,
				OpenapiRequestParams.originalString(aftersalesBnParam, body, "aftersales_bn"));
	}

	@PostMapping(value = "/ecx.refund.detail.get", name = "开放接口退款单详情")
	public Map<String, Object> getRefundDetail(
			HttpServletRequest request,
			@RequestParam(name = "refund_bn", required = false) String refundBnParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		return refundDetailPort.getRefundDetail(
				companyId,
				OpenapiRequestParams.originalString(refundBnParam, body, "refund_bn"));
	}
}
