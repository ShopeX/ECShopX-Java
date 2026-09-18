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
import cn.shopex.ecshopx.common.openapi.OpenapiMemberCardVipGradeCreatePort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberCardVipGradeDeletePort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberCardVipGradeDetailPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberCardVipGradeListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberCardVipGradeOrderListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberCardVipGradeUpdatePort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.OpenapiThirdApiV2ShippingTemplatesListParams;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OpenAPI v2 handler scaffold — methods added during migration. */
@OpenapiResponse
@RestController("openapiV2MemberCardVipGrade")
@RequestMapping("/api/openapi/internal/v2")
public class MemberCardVipGradeController extends OpenapiBaseController {

	private final OpenapiMemberCardVipGradeListPort memberCardVipGradeListPort;
	private final OpenapiMemberCardVipGradeOrderListPort memberCardVipGradeOrderListPort;
	private final OpenapiMemberCardVipGradeDetailPort memberCardVipGradeDetailPort;
	private final OpenapiMemberCardVipGradeCreatePort memberCardVipGradeCreatePort;
	private final OpenapiMemberCardVipGradeUpdatePort memberCardVipGradeUpdatePort;
	private final OpenapiMemberCardVipGradeDeletePort memberCardVipGradeDeletePort;

	public MemberCardVipGradeController(
			OpenapiMemberCardVipGradeListPort memberCardVipGradeListPort,
			OpenapiMemberCardVipGradeOrderListPort memberCardVipGradeOrderListPort,
			OpenapiMemberCardVipGradeDetailPort memberCardVipGradeDetailPort,
			OpenapiMemberCardVipGradeCreatePort memberCardVipGradeCreatePort,
			OpenapiMemberCardVipGradeUpdatePort memberCardVipGradeUpdatePort,
			OpenapiMemberCardVipGradeDeletePort memberCardVipGradeDeletePort) {
		this.memberCardVipGradeListPort = memberCardVipGradeListPort;
		this.memberCardVipGradeOrderListPort = memberCardVipGradeOrderListPort;
		this.memberCardVipGradeDetailPort = memberCardVipGradeDetailPort;
		this.memberCardVipGradeCreatePort = memberCardVipGradeCreatePort;
		this.memberCardVipGradeUpdatePort = memberCardVipGradeUpdatePort;
		this.memberCardVipGradeDeletePort = memberCardVipGradeDeletePort;
	}

	@GetMapping(value = "/ecx.member_card_vip_grade.list", name = "开放接口查询付费会员卡等级列表")
	public Map<String, Object> list(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@RequestParam(name = "vip_grade_id", required = false) String vipGradeIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2ShippingTemplatesListParams.PageSpec pageSpec =
				OpenapiThirdApiV2ShippingTemplatesListParams.resolve(pageParam, pageSizeParam, body);
		String vipGradeIdRaw = OpenapiRequestParams.originalString(vipGradeIdParam, body, "vip_grade_id");
		return memberCardVipGradeListPort.list(
				companyId, pageSpec.page(), pageSpec.pageSize(), vipGradeIdRaw);
	}

	@PostMapping(value = "/ecx.member_card_vip_grade.create", name = "开放接口创建付费会员卡等级")
	public Map<String, Object> create(
			HttpServletRequest request,
			@RequestParam(name = "grade_name", required = false) String gradeNameParam,
			@RequestParam(name = "monthly_fee", required = false) String monthlyFeeParam,
			@RequestParam(name = "quarter_fee", required = false) String quarterFeeParam,
			@RequestParam(name = "year_fee", required = false) String yearFeeParam,
			@RequestParam(name = "discount", required = false) String discountParam,
			@RequestParam(name = "guide_title", required = false) String guideTitleParam,
			@RequestParam(name = "description", required = false) String descriptionParam,
			@RequestParam(name = "is_default", required = false) String isDefaultParam,
			@RequestParam(name = "is_disabled", required = false) String isDisabledParam,
			@RequestParam(name = "external_id", required = false) String externalIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		return memberCardVipGradeCreatePort.create(
				companyId,
				OpenapiRequestParams.originalString(gradeNameParam, body, "grade_name"),
				OpenapiRequestParams.originalString(monthlyFeeParam, body, "monthly_fee"),
				OpenapiRequestParams.originalString(quarterFeeParam, body, "quarter_fee"),
				OpenapiRequestParams.originalString(yearFeeParam, body, "year_fee"),
				OpenapiRequestParams.originalString(discountParam, body, "discount"),
				OpenapiRequestParams.mergeString(guideTitleParam, body, "guide_title"),
				OpenapiRequestParams.mergeString(descriptionParam, body, "description"),
				OpenapiRequestParams.mergeString(isDefaultParam, body, "is_default"),
				OpenapiRequestParams.mergeString(isDisabledParam, body, "is_disabled"),
				OpenapiRequestParams.mergeString(externalIdParam, body, "external_id"));
	}

	@PatchMapping(value = "/ecx.member_card_vip_grade.update", name = "开放接口更新付费会员卡等级")
	public Map<String, Object> update(
			HttpServletRequest request,
			@RequestParam(name = "vip_grade_id", required = false) String vipGradeIdParam,
			@RequestParam(name = "grade_name", required = false) String gradeNameParam,
			@RequestParam(name = "monthly_fee", required = false) String monthlyFeeParam,
			@RequestParam(name = "quarter_fee", required = false) String quarterFeeParam,
			@RequestParam(name = "year_fee", required = false) String yearFeeParam,
			@RequestParam(name = "discount", required = false) String discountParam,
			@RequestParam(name = "guide_title", required = false) String guideTitleParam,
			@RequestParam(name = "description", required = false) String descriptionParam,
			@RequestParam(name = "is_default", required = false) String isDefaultParam,
			@RequestParam(name = "is_disabled", required = false) String isDisabledParam,
			@RequestParam(name = "external_id", required = false) String externalIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		return memberCardVipGradeUpdatePort.update(
				companyId,
				OpenapiRequestParams.originalString(vipGradeIdParam, body, "vip_grade_id"),
				OpenapiRequestParams.presentOptionalString(gradeNameParam, body, "grade_name"),
				OpenapiRequestParams.presentOptionalString(monthlyFeeParam, body, "monthly_fee"),
				OpenapiRequestParams.presentOptionalString(quarterFeeParam, body, "quarter_fee"),
				OpenapiRequestParams.presentOptionalString(yearFeeParam, body, "year_fee"),
				OpenapiRequestParams.presentOptionalString(discountParam, body, "discount"),
				OpenapiRequestParams.presentOptionalString(guideTitleParam, body, "guide_title"),
				OpenapiRequestParams.presentOptionalString(descriptionParam, body, "description"),
				OpenapiRequestParams.presentOptionalString(isDefaultParam, body, "is_default"),
				OpenapiRequestParams.presentOptionalString(isDisabledParam, body, "is_disabled"),
				OpenapiRequestParams.presentOptionalString(externalIdParam, body, "external_id"));
	}

	@DeleteMapping(value = "/ecx.member_card_vip_grade.delete", name = "开放接口删除付费会员卡等级")
	public Void delete(
			HttpServletRequest request,
			@RequestParam(name = "vip_grade_id", required = false) String vipGradeIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		memberCardVipGradeDeletePort.delete(
				companyId,
				OpenapiRequestParams.originalString(vipGradeIdParam, body, "vip_grade_id"));
		return null;
	}

	@GetMapping(value = "/ecx.member_card_vip_grade_order.list", name = "开放接口查询付费会员卡等级购买记录列表")
	public Map<String, Object> orderList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2ShippingTemplatesListParams.PageSpec pageSpec =
				OpenapiThirdApiV2ShippingTemplatesListParams.resolve(pageParam, pageSizeParam, body);
		return memberCardVipGradeOrderListPort.list(
				companyId, pageSpec.page(), pageSpec.pageSize());
	}

	@GetMapping(value = "/ecx.member_card_vip_grade.detail", name = "开放接口查询会员关联付费等级信息")
	public Map<String, Object> detail(
			HttpServletRequest request,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String mobileRaw = OpenapiRequestParams.originalString(mobileParam, body, "mobile");
		return memberCardVipGradeDetailPort.getVipGradeDetailByMobile(companyId, mobileRaw);
	}
}
