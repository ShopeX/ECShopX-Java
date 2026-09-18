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

package cn.shopex.ecshopx.openapi.thirdapi.v1;

import cn.shopex.ecshopx.common.annotation.OpenapiResponse;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.openapi.OpenapiCouponCreatePort;
import cn.shopex.ecshopx.common.openapi.OpenapiCouponListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiCouponUpdatePort;
import cn.shopex.ecshopx.common.openapi.OpenapiCouponVerifyPort;
import cn.shopex.ecshopx.common.openapi.OpenapiEnvelope;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OpenAPI v1 handler scaffold — methods added during migration. */
@OpenapiResponse
@RestController("openapiV1Coupon")
@RequestMapping("/api/openapi/internal/v1")
public class CouponController extends OpenapiBaseController {

	private final OpenapiCouponCreatePort couponCreatePort;
	private final OpenapiCouponVerifyPort couponVerifyPort;
	private final OpenapiCouponUpdatePort couponUpdatePort;
	private final OpenapiCouponListPort couponListPort;

	public CouponController(
			OpenapiCouponCreatePort couponCreatePort,
			OpenapiCouponVerifyPort couponVerifyPort,
			OpenapiCouponUpdatePort couponUpdatePort,
			OpenapiCouponListPort couponListPort) {
		this.couponCreatePort = couponCreatePort;
		this.couponVerifyPort = couponVerifyPort;
		this.couponUpdatePort = couponUpdatePort;
		this.couponListPort = couponListPort;
	}

	@PostMapping(value = "/ecx.coupon.create", name = "开放接口云店优惠券发放")
	public OpenapiEnvelope userGetCard(
			HttpServletRequest request,
			@RequestParam(name = "template", required = false) String templateParam,
			@RequestParam(name = "rule", required = false) String ruleParam,
			@RequestParam(name = "code", required = false) String codeParam,
			@RequestParam(name = "start_time", required = false) String startTimeParam,
			@RequestParam(name = "end_time", required = false) String endTimeParam,
			@RequestParam(name = "outer_crm_userid", required = false) String outerCrmUseridParam,
			@RequestParam(name = "user_id", required = false) String userIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String template =
				OpenapiRequestParams.mergeRequiredString(templateParam, body, "template", "优惠券模板ID必填");
		String rule = OpenapiRequestParams.mergeString(ruleParam, body, "rule");
		if (rule == null) {
			rule = "";
		}
		String code = OpenapiRequestParams.mergeRequiredString(codeParam, body, "code", "优惠券编码必填");
		String startTimeRaw =
				OpenapiRequestParams.mergeRequiredString(startTimeParam, body, "start_time", "优惠券生效时间必填");
		String endTimeRaw =
				OpenapiRequestParams.mergeRequiredString(endTimeParam, body, "end_time", "优惠券失效时间必填");
		String outerCrmUserid = OpenapiRequestParams.mergeString(outerCrmUseridParam, body, "outer_crm_userid");
		String mobileUserId = OpenapiRequestParams.mergeString(userIdParam, body, "user_id");

		long startTimeEpoch = parseRequiredEpoch(startTimeRaw, "优惠券生效时间必填");
		long endTimeEpoch = parseRequiredEpoch(endTimeRaw, "优惠券失效时间必填");

		Map<String, Object> data =
				couponCreatePort.userGetCard(
						companyId,
						template,
						rule,
						code,
						startTimeEpoch,
						endTimeEpoch,
						outerCrmUserid,
						mobileUserId);
		return new OpenapiEnvelope("success", "E0000", "发放成功", data);
	}

	@PostMapping(value = "/ecx.coupon.update", name = "开放接口优惠券有效期更新")
	public OpenapiEnvelope updateUserCard(
			HttpServletRequest request,
			@RequestParam(name = "template", required = false) String templateParam,
			@RequestParam(name = "rule", required = false) String ruleParam,
			@RequestParam(name = "code", required = false) String codeParam,
			@RequestParam(name = "start_time", required = false) String startTimeParam,
			@RequestParam(name = "end_time", required = false) String endTimeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String template =
				OpenapiRequestParams.mergeRequiredString(templateParam, body, "template", "优惠券模板ID必填");
		String rule = OpenapiRequestParams.mergeString(ruleParam, body, "rule");
		if (rule == null) {
			rule = "";
		}
		String code = OpenapiRequestParams.mergeRequiredString(codeParam, body, "code", "优惠券编码必填");
		String startTimeRaw =
				OpenapiRequestParams.mergeRequiredString(startTimeParam, body, "start_time", "优惠券生效时间必填");
		String endTimeRaw =
				OpenapiRequestParams.mergeRequiredString(endTimeParam, body, "end_time", "优惠券失效时间必填");

		Map<String, Object> data =
				couponUpdatePort.updateUserCard(companyId, template, rule, code, startTimeRaw, endTimeRaw);
		return new OpenapiEnvelope("success", "E0000", "修改成功", data);
	}

	@PostMapping(value = "/ecx.coupon.list", name = "开放接口优惠券列表")
	public OpenapiEnvelope getCouponList(
			HttpServletRequest request,
			@RequestParam(name = "distributor_id", required = false) String distributorIdParam,
			@RequestParam(name = "page_no", required = false) String pageNoParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String distributorId = OpenapiRequestParams.mergeString(distributorIdParam, body, "distributor_id");
		boolean applyDistributorFilter = OpenapiCouponListParams.isPhpTruthyDistributorId(distributorId);
		boolean pageNoPresent = OpenapiCouponListParams.isParamPresent(pageNoParam, body, "page_no");
		boolean pageSizePresent = OpenapiCouponListParams.isParamPresent(pageSizeParam, body, "page_size");
		String pageNoRaw = OpenapiRequestParams.mergeString(pageNoParam, body, "page_no");
		String pageSizeRaw = OpenapiRequestParams.mergeString(pageSizeParam, body, "page_size");
		Map<String, Object> data =
				couponListPort.getCouponList(
						companyId,
						applyDistributorFilter,
						distributorId,
						pageNoPresent,
						pageNoRaw,
						pageSizePresent,
						pageSizeRaw);
		return new OpenapiEnvelope("success", "E0000", "操作成功", data);
	}

	@PostMapping(value = "/ecx.coupon.verify", name = "开放接口优惠券状态更新")
	public OpenapiEnvelope userConsumeCard(
			HttpServletRequest request,
			@RequestParam(name = "coupon_code", required = false) String couponCodeParam,
			@RequestParam(name = "coupon_status", required = false) String couponStatusParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String couponCode =
				OpenapiRequestParams.mergeRequiredString(couponCodeParam, body, "coupon_code", "优惠券编码必填");
		String couponStatus =
				OpenapiRequestParams.mergeRequiredString(couponStatusParam, body, "coupon_status", "状态必填");
		if (!"1".equals(couponStatus) && !"2".equals(couponStatus)) {
			throw new ResourceException("不支持的核销状态");
		}
		Map<String, Object> data = couponVerifyPort.userConsumeCard(companyId, couponCode, couponStatus);
		return new OpenapiEnvelope("success", "E0000", "修改成功", data);
	}

	private static long parseRequiredEpoch(String raw, String requiredMessage) {
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			throw new ResourceException(requiredMessage);
		}
	}
}
