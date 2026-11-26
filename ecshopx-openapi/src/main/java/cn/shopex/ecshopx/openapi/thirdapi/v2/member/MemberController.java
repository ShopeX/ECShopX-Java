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
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberCardCodeGradeUpdateBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberCardCodeGradeUpdateSuccessEnvelopeDoc;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "开放接口-会员V2")
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

	@Operation(
			summary = "开放接口修改会员卡号等级",
			description =
					"按签名鉴权租户 company_id 以手机号定位会员，局部更新卡号/等级（3 键双通道，body 含键优先）："
							+ " mobile 必填，trim 后须 ^1[3456789][0-9]{9}$（E5001「手机号必填」/「手机号填写错误」）；"
							+ " user_card_code/grade_id 按键存在性 PATCH——两键均缺席时无操作成功（mobile 仍先校验）。"
							+ " user_card_code：body 值须字符串或 null（否则 E5001「会员卡号参数错误」）；trim 非空直接用，"
							+ " null/空白自动生成 12 位大写随机码（Redis zset usercardcode 去重，3 次冲突→E5000）；"
							+ " 被其他手机号会员占用 E5522「会员卡号已存在」（占用者即本手机号放行）。"
							+ " grade_id：须数值/可 parseDouble 有限数字字符串（否则 E5001「等级ID参数错误」），小数截断；"
							+ " membercard_grade 查无 E5531「会员等级找不到」。"
							+ " 会员按固定加密 mobile 等值定位，查无或 UPDATE affected=0 → E5501「会员找不到」。"
							+ " 注意：query 参数 card_code 被接收但忽略（未并入 mergeAll），改卡号须用 user_card_code。"
							+ " 其他异常统一 E5000「系统错误」。成功恒 data=null，Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}"
							+ "（method=ecx.member_card_code_grade.update, version=2.0）签名校验后转发；"
							+ " 直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：@Transactional UPDATE members.user_card_code/grade_id/updated；"
							+ " 空卡号时 Redis zset usercardcode 写入随机码；无第三方调用。",
			operationId = "ecx.member_card_code_grade.update.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功, data 恒 null（含两 PATCH 键均缺席的无操作场景）；"
								+ " 业务失败：status=fail, code=E5001 手机号必填·手机号填写错误·会员卡号参数错误·等级ID参数错误 /"
								+ " E5501 会员找不到 / E5522 会员卡号已存在 / E5531 会员等级找不到 / E5000 系统错误，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxMemberCardCodeGradeUpdateFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxMemberCardCodeGradeUpdateSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "更新成功（无返回体）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\",\"data\":null}"),
									@ExampleObject(
											name = "fail-mobile-format",
											summary = "mobile 格式错误",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\","
															+ "\"message\":\"手机号填写错误\",\"data\":null}"),
									@ExampleObject(
											name = "fail-card-exist",
											summary = "卡号被其他会员占用",
											value =
													"{\"status\":\"fail\",\"code\":\"E5522\","
															+ "\"message\":\"会员卡号已存在\",\"data\":null}"),
									@ExampleObject(
											name = "fail-grade-not-found",
											summary = "grade_id 查无",
											value =
													"{\"status\":\"fail\",\"code\":\"E5531\","
															+ "\"message\":\"会员等级找不到\",\"data\":null}"),
									@ExampleObject(
											name = "fail-member-not-found",
											summary = "mobile 查无会员",
											value =
													"{\"status\":\"fail\",\"code\":\"E5501\",\"message\":\"会员找不到\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PatchMapping(value = "/ecx.member_card_code_grade.update", name = "开放接口修改会员卡号等级")
	public Void updateCardCodeAndGrade(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "会员手机号（明文）；必填，trim 后须 ^1[3456789][0-9]{9}$；查无会员 E5501",
							required = true,
							example = "13800138000")
					@RequestParam(name = "mobile", required = false)
					String mobileParam,
			@Parameter(
							description = "新会员卡号；键存在即更新；空白/null 自动生成 12 位随机码；他人占用 E5522（详见 body schema）",
							example = "card")
					@RequestParam(name = "user_card_code", required = false)
					String userCardCodeParam,
			@Parameter(
							description = "遗留参数：被接收但忽略（未并入 mergeAll），修改卡号请使用 user_card_code",
							deprecated = true)
					@RequestParam(name = "card_code", required = false)
					String cardCodeParam,
			@Parameter(
							description = "新会员等级 ID；键存在即更新；数字（小数截断），membercard_grade 查无 E5531",
							example = "1")
					@RequestParam(name = "grade_id", required = false)
					String gradeIdParam,
			@RequestBody(
							description =
									"可选 JSON body；3 键与 query 同名双通道（body 含键优先，PATCH 键原值保留）；"
											+ "card_code 键不被读取；语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxMemberCardCodeGradeUpdateBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
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
