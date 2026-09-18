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
import cn.shopex.ecshopx.common.openapi.OpenapiMemberCardGradeBatchSavePort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberCardGradeCreatePort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberCardGradeDeletePort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberCardGradeDetailPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberCardGradeListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberCardGradeUpdatePort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.OpenapiThirdApiV2ShippingTemplatesListParams;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
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
@RestController("openapiV2MemberCardGrade")
@RequestMapping("/api/openapi/internal/v2")
public class MemberCardGradeController extends OpenapiBaseController {

	private final OpenapiMemberCardGradeListPort memberCardGradeListPort;
	private final OpenapiMemberCardGradeDetailPort memberCardGradeDetailPort;
	private final OpenapiMemberCardGradeCreatePort memberCardGradeCreatePort;
	private final OpenapiMemberCardGradeUpdatePort memberCardGradeUpdatePort;
	private final OpenapiMemberCardGradeDeletePort memberCardGradeDeletePort;
	private final OpenapiMemberCardGradeBatchSavePort memberCardGradeBatchSavePort;

	public MemberCardGradeController(
			OpenapiMemberCardGradeListPort memberCardGradeListPort,
			OpenapiMemberCardGradeDetailPort memberCardGradeDetailPort,
			OpenapiMemberCardGradeCreatePort memberCardGradeCreatePort,
			OpenapiMemberCardGradeUpdatePort memberCardGradeUpdatePort,
			OpenapiMemberCardGradeDeletePort memberCardGradeDeletePort,
			OpenapiMemberCardGradeBatchSavePort memberCardGradeBatchSavePort) {
		this.memberCardGradeListPort = memberCardGradeListPort;
		this.memberCardGradeDetailPort = memberCardGradeDetailPort;
		this.memberCardGradeCreatePort = memberCardGradeCreatePort;
		this.memberCardGradeUpdatePort = memberCardGradeUpdatePort;
		this.memberCardGradeDeletePort = memberCardGradeDeletePort;
		this.memberCardGradeBatchSavePort = memberCardGradeBatchSavePort;
	}

	@GetMapping(value = "/ecx.member_card_grade.list", name = "开放接口查询会员卡等级列表")
	public Map<String, Object> list(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@RequestParam(name = "grade_id", required = false) String gradeIdParam,
			@RequestParam(name = "country_code", required = false) String countryCodeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2ShippingTemplatesListParams.PageSpec pageSpec =
				OpenapiThirdApiV2ShippingTemplatesListParams.resolve(pageParam, pageSizeParam, body);
		String gradeIdRaw = OpenapiRequestParams.originalString(gradeIdParam, body, "grade_id");
		String langRaw = OpenapiRequestParams.mergeString(countryCodeParam, body, "country_code");
		return memberCardGradeListPort.list(
				companyId, pageSpec.page(), pageSpec.pageSize(), gradeIdRaw, langRaw);
	}

	@GetMapping(value = "/ecx.member_card_grade.detail", name = "开放接口查询会员关联等级信息")
	public Map<String, Object> detail(
			HttpServletRequest request,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String mobileRaw = OpenapiRequestParams.originalString(mobileParam, body, "mobile");
		return memberCardGradeDetailPort.getGradeDetailByMobile(companyId, mobileRaw);
	}

	@PostMapping(value = "/ecx.member_card_grade.create", name = "开放接口创建会员卡等级")
	public Map<String, Object> create(
			HttpServletRequest request,
			@RequestParam(name = "grade_name", required = false) String gradeNameParam,
			@RequestParam(name = "discount", required = false) String discountParam,
			@RequestParam(name = "total_consumption", required = false) String totalConsumptionParam,
			@RequestParam(name = "background_pic_url", required = false) String backgroundPicUrlParam,
			@RequestParam(name = "external_id", required = false) String externalIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		return memberCardGradeCreatePort.create(
				companyId,
				OpenapiRequestParams.originalString(gradeNameParam, body, "grade_name"),
				OpenapiRequestParams.originalString(discountParam, body, "discount"),
				OpenapiRequestParams.originalString(totalConsumptionParam, body, "total_consumption"),
				OpenapiRequestParams.mergeString(backgroundPicUrlParam, body, "background_pic_url"),
				OpenapiRequestParams.mergeString(externalIdParam, body, "external_id"));
	}

	@PatchMapping(value = "/ecx.member_card_grade.update", name = "开放接口修改会员卡等级")
	public Map<String, Object> update(
			HttpServletRequest request,
			@RequestParam(name = "grade_id", required = false) String gradeIdParam,
			@RequestParam(name = "grade_name", required = false) String gradeNameParam,
			@RequestParam(name = "discount", required = false) String discountParam,
			@RequestParam(name = "total_consumption", required = false) String totalConsumptionParam,
			@RequestParam(name = "background_pic_url", required = false) String backgroundPicUrlParam,
			@RequestParam(name = "external_id", required = false) String externalIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		return memberCardGradeUpdatePort.update(
				companyId,
				OpenapiRequestParams.originalString(gradeIdParam, body, "grade_id"),
				OpenapiRequestParams.presentOptionalString(gradeNameParam, body, "grade_name"),
				OpenapiRequestParams.presentOptionalString(discountParam, body, "discount"),
				OpenapiRequestParams.presentOptionalString(totalConsumptionParam, body, "total_consumption"),
				OpenapiRequestParams.presentOptionalString(backgroundPicUrlParam, body, "background_pic_url"),
				OpenapiRequestParams.presentOptionalString(externalIdParam, body, "external_id"));
	}

	@PostMapping(value = "/ecx.card_grade.batch_save", name = "开放接口批量设置会员卡等级（数云）")
	public Object batchSave(
			HttpServletRequest request,
			@RequestParam(name = "grade_info", required = false) String gradeInfoParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String gradeInfoRaw = OpenapiRequestParams.originalString(gradeInfoParam, body, "grade_info");
		List<Map<String, Object>> items =
				OpenapiMemberCardGradeV2BatchSaveParams.parseGradeInfoArray(gradeInfoRaw);
		List<Map<String, Object>> result = memberCardGradeBatchSavePort.batchSave(companyId, items);
		return (result == null || result.isEmpty()) ? null : result;
	}

	@DeleteMapping(value = "/ecx.member_card_grade.delete", name = "开放接口删除会员卡等级")
	public Void delete(
			HttpServletRequest request,
			@RequestParam(name = "grade_id", required = false) String gradeIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		memberCardGradeDeletePort.delete(
				companyId,
				OpenapiRequestParams.originalString(gradeIdParam, body, "grade_id"));
		return null;
	}
}
