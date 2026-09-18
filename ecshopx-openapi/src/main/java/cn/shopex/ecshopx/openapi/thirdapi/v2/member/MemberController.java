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
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberBatchCreatePort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberCreateDetailPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2DetailPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2ListByGradeOrVipGradePort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2ListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2UpdateCardCodeGradePort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2UpdateDetailPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2UpdateGradePort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberV2UpdateMobilePort;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiMemberQueryParams;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@OpenapiResponse
@RestController("openapiV2Member")
@RequestMapping("/api/openapi/internal/v2")
public class MemberController extends OpenapiBaseController {

	private final OpenapiMemberCreateDetailPort memberCreateDetailPort;
	private final OpenapiMemberBatchCreatePort memberBatchCreatePort;
	private final OpenapiMemberV2ListPort memberV2ListPort;
	private final OpenapiMemberV2ListByGradeOrVipGradePort memberV2ListByGradeOrVipGradePort;
	private final OpenapiMemberV2DetailPort memberV2DetailPort;
	private final OpenapiMemberV2UpdateDetailPort memberV2UpdateDetailPort;
	private final OpenapiMemberV2UpdateMobilePort memberV2UpdateMobilePort;
	private final OpenapiMemberV2UpdateCardCodeGradePort memberV2UpdateCardCodeGradePort;
	private final OpenapiMemberV2UpdateGradePort memberV2UpdateGradePort;

	public MemberController(
			OpenapiMemberCreateDetailPort memberCreateDetailPort,
			OpenapiMemberBatchCreatePort memberBatchCreatePort,
			OpenapiMemberV2ListPort memberV2ListPort,
			OpenapiMemberV2ListByGradeOrVipGradePort memberV2ListByGradeOrVipGradePort,
			OpenapiMemberV2DetailPort memberV2DetailPort,
			OpenapiMemberV2UpdateDetailPort memberV2UpdateDetailPort,
			OpenapiMemberV2UpdateMobilePort memberV2UpdateMobilePort,
			OpenapiMemberV2UpdateCardCodeGradePort memberV2UpdateCardCodeGradePort,
			OpenapiMemberV2UpdateGradePort memberV2UpdateGradePort) {
		this.memberCreateDetailPort = memberCreateDetailPort;
		this.memberBatchCreatePort = memberBatchCreatePort;
		this.memberV2ListPort = memberV2ListPort;
		this.memberV2ListByGradeOrVipGradePort = memberV2ListByGradeOrVipGradePort;
		this.memberV2DetailPort = memberV2DetailPort;
		this.memberV2UpdateDetailPort = memberV2UpdateDetailPort;
		this.memberV2UpdateMobilePort = memberV2UpdateMobilePort;
		this.memberV2UpdateCardCodeGradePort = memberV2UpdateCardCodeGradePort;
		this.memberV2UpdateGradePort = memberV2UpdateGradePort;
	}

	@Activated(routeAlias = "adapay.member.create")
	@PostMapping(value = "/ecx.member.create", name = "开放接口创建会员单个")
	public Map<String, Object> create(
			HttpServletRequest request,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "inviter_mobile", required = false) String inviterMobileParam,
			@RequestParam(name = "salesperson_mobile", required = false) String salespersonMobileParam,
			@RequestParam(name = "union_id", required = false) String unionIdParam,
			@RequestParam(name = "status", required = false) String statusParam,
			@RequestParam(name = "tag_names", required = false) String tagNamesParam,
			@RequestParam(name = "tag_ids", required = false) String tagIdsParam,
			@RequestParam(name = "card_code", required = false) String cardCodeParam,
			@RequestParam(name = "grade_id", required = false) String gradeIdParam,
			@RequestParam(name = "username", required = false) String usernameParam,
			@RequestParam(name = "avatar", required = false) String avatarParam,
			@RequestParam(name = "sex", required = false) String sexParam,
			@RequestParam(name = "birthday", required = false) String birthdayParam,
			@RequestParam(name = "edu_background", required = false) String eduBackgroundParam,
			@RequestParam(name = "income", required = false) String incomeParam,
			@RequestParam(name = "industry", required = false) String industryParam,
			@RequestParam(name = "email", required = false) String emailParam,
			@RequestParam(name = "address", required = false) String addressParam,
			@RequestParam(name = "remarks", required = false) String remarksParam,
			@RequestParam(name = "source_from", required = false) String sourceFromParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Map<String, Object> raw =
				OpenapiMemberV2CreateHandlerParams.mergeAll(
						body,
						mobileParam,
						inviterMobileParam,
						salespersonMobileParam,
						unionIdParam,
						statusParam,
						tagNamesParam,
						tagIdsParam,
						cardCodeParam,
						gradeIdParam,
						usernameParam,
						avatarParam,
						sexParam,
						birthdayParam,
						eduBackgroundParam,
						incomeParam,
						industryParam,
						emailParam,
						addressParam,
						remarksParam,
						sourceFromParam);
		if (body != null && body.containsKey("habbit")) {
			raw.put("habbit", body.get("habbit"));
		}
		return memberCreateDetailPort.createDetail(companyId, raw);
	}

	@PostMapping(value = "/ecx.member.batch_create", name = "开放接口批量创建会员异步")
	public Map<String, Object> batchCreate(
			HttpServletRequest request,
			@RequestParam(name = "data", required = false) String dataParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		boolean hasData =
				OpenapiMemberV2BatchCreateParams.hasDataParameter(request, body, dataParam);
		if (!hasData) {
			return memberBatchCreatePort.batchCreate(companyId, false, List.of());
		}
		String dataRaw = OpenapiRequestParams.originalString(dataParam, body, "data");
		List<Map<String, Object>> items =
				OpenapiMemberV2BatchCreateParams.parseDataArray(dataRaw);
		return memberBatchCreatePort.batchCreate(companyId, true, items);
	}

	@PostMapping(value = "/ecx.member.card_grade.update", name = "开放接口修改会员等级")
	public Void updateGrade(
			HttpServletRequest request,
			@RequestParam(name = "plat_account", required = false) String platAccountParam,
			@RequestParam(name = "grade_id", required = false) String gradeIdParam,
			@RequestParam(name = "grade_level", required = false) String gradeLevelParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Map<String, Object> merged =
				OpenapiMemberV2UpdateGradeHandlerParams.mergeAll(
						body, platAccountParam, gradeIdParam, gradeLevelParam);
		memberV2UpdateGradePort.updateGrade(companyId, merged);
		return null;
	}

	@PatchMapping(value = "/ecx.member_card_code_grade.update", name = "开放接口修改会员卡号等级")
	public Void updateCardCodeAndGrade(
			HttpServletRequest request,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "user_card_code", required = false) String userCardCodeParam,
			@RequestParam(name = "card_code", required = false) String cardCodeParam,
			@RequestParam(name = "grade_id", required = false) String gradeIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Map<String, Object> merged =
				OpenapiMemberV2UpdateCardCodeGradeHandlerParams.mergeAll(
						body, mobileParam, userCardCodeParam, gradeIdParam);
		memberV2UpdateCardCodeGradePort.updateCardCodeAndGrade(companyId, merged);
		return null;
	}

	@PatchMapping(value = "/ecx.member_mobile.update", name = "开放接口修改会员手机号")
	public Void updateMobile(
			HttpServletRequest request,
			@RequestParam(name = "user_id", required = false) String userIdParam,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "new_mobile", required = false) String newMobileParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Map<String, Object> merged =
				OpenapiMemberV2UpdateMobileHandlerParams.mergeAll(
						body, userIdParam, mobileParam, newMobileParam);
		memberV2UpdateMobilePort.updateMemberMobile(companyId, merged);
		return null;
	}

	@PatchMapping(value = "/ecx.member_info.update", name = "开放接口修改会员信息")
	public Void updateDetail(
			HttpServletRequest request,
			@RequestParam(name = "user_id", required = false) String userIdParam,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "inviter_mobile", required = false) String inviterMobileParam,
			@RequestParam(name = "status", required = false) String statusParam,
			@RequestParam(name = "remarks", required = false) String remarksParam,
			@RequestParam(name = "username", required = false) String usernameParam,
			@RequestParam(name = "avatar", required = false) String avatarParam,
			@RequestParam(name = "sex", required = false) String sexParam,
			@RequestParam(name = "birthday", required = false) String birthdayParam,
			@RequestParam(name = "edu_background", required = false) String eduBackgroundParam,
			@RequestParam(name = "income", required = false) String incomeParam,
			@RequestParam(name = "industry", required = false) String industryParam,
			@RequestParam(name = "email", required = false) String emailParam,
			@RequestParam(name = "address", required = false) String addressParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Map<String, Object> merged =
				OpenapiMemberV2UpdateDetailHandlerParams.mergeAll(
						body,
						userIdParam,
						mobileParam,
						inviterMobileParam,
						statusParam,
						remarksParam,
						usernameParam,
						avatarParam,
						sexParam,
						birthdayParam,
						eduBackgroundParam,
						incomeParam,
						industryParam,
						emailParam,
						addressParam);
		if (body != null && body.containsKey("habbit")) {
			merged.put("habbit", body.get("habbit"));
		}
		Map<String, Object> requestData = OpenapiMemberV2UpdateDetailHandlerParams.buildRequestData(merged);
		memberV2UpdateDetailPort.updateMemberInfo(companyId, merged, requestData);
		return null;
	}

	@GetMapping(value = "/ecx.member.detail", name = "开放接口查询会员详情")
	public Map<String, Object> detail(
			HttpServletRequest request,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String mobile = OpenapiRequestParams.originalString(mobileParam, body, "mobile");
		return memberV2DetailPort.getMemberDetail(companyId, mobile);
	}

	@GetMapping(value = "/ecx.member.list", name = "开放接口查询会员批量")
	public Map<String, Object> list(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "source_from", required = false) String sourceFromParam,
			@RequestParam(name = "inviter_mobile", required = false) String inviterMobileParam,
			@RequestParam(name = "salesperson_mobile", required = false) String salespersonMobileParam,
			@RequestParam(name = "status", required = false) String statusParam,
			@RequestParam(name = "tag_id", required = false) String tagIdParam,
			@RequestParam(name = "tag_name", required = false) String tagNameParam,
			@RequestParam(name = "have_consume", required = false) String haveConsumeParam,
			@RequestParam(name = "card_code", required = false) String cardCodeParam,
			@RequestParam(name = "user_card_code", required = false) String userCardCodeParam,
			@RequestParam(name = "grade_id", required = false) String gradeIdParam,
			@RequestParam(name = "grade_name", required = false) String gradeNameParam,
			@RequestParam(name = "vip_grade_id", required = false) String vipGradeIdParam,
			@RequestParam(name = "vip_grade_name", required = false) String vipGradeNameParam,
			@RequestParam(name = "start_date", required = false) String startDateParam,
			@RequestParam(name = "end_date", required = false) String endDateParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2MemberListParams.PageSpec pageSpec =
				OpenapiThirdApiV2MemberListParams.resolve(pageParam, pageSizeParam, body);
		Map<String, Object> raw =
				OpenapiMemberV2ListHandlerParams.mergePresentKeys(
						body,
						mobileParam,
						sourceFromParam,
						inviterMobileParam,
						salespersonMobileParam,
						statusParam,
						tagIdParam,
						tagNameParam,
						haveConsumeParam,
						cardCodeParam,
						userCardCodeParam,
						gradeIdParam,
						gradeNameParam,
						vipGradeIdParam,
						vipGradeNameParam,
						startDateParam,
						endDateParam);
		return memberV2ListPort.listMembers(
				companyId, pageSpec.page(), pageSpec.pageSize(), raw);
	}

	@GetMapping(value = "/ecx.member_card_grade_rel.list", name = "开放接口会员等级关联会员列表")
	public Map<String, Object> listByGradeOrVipGrade(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@RequestParam(name = "grade_id", required = false) String gradeIdParam,
			@RequestParam(name = "vip_grade_id", required = false) String vipGradeIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);

		OpenapiThirdApiV2MemberListParams.PageSpec pageSpec =
				OpenapiThirdApiV2MemberListParams.resolve(pageParam, pageSizeParam, body);

		boolean gradeIdPresent =
				OpenapiMemberQueryParams.isParamPresent(gradeIdParam, body, "grade_id");
		String gradeIdRaw = OpenapiRequestParams.originalString(gradeIdParam, body, "grade_id");
		boolean vipGradeIdPresent =
				OpenapiMemberQueryParams.isParamPresent(vipGradeIdParam, body, "vip_grade_id");
		String vipGradeIdRaw =
				OpenapiRequestParams.originalString(vipGradeIdParam, body, "vip_grade_id");

		return memberV2ListByGradeOrVipGradePort.listByGradeOrVipGrade(
				companyId,
				gradeIdPresent,
				gradeIdRaw,
				vipGradeIdPresent,
				vipGradeIdRaw,
				pageSpec.page(),
				pageSpec.pageSize());
	}
}
