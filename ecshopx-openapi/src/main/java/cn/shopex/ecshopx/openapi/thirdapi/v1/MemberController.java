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
import cn.shopex.ecshopx.common.openapi.OpenapiEnvelope;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberAssignMemberToSalespersonPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberNotifyBecomeFriendPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberBasicInfoPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberBrowseHistoryListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberBrowseHistoryPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberCreatePort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberFrequentItemsPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberInfoListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberCardGradesPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagLibraryPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagLibraryPushPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberTagRelationPushPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberOrderListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberQueryPort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxMemberAssignToSalespersonBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxMemberAssignToSalespersonSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxMemberBasicInfoBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxMemberBasicInfoSuccessEnvelopeDoc;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OpenAPI v1 handler scaffold — methods added during migration. */
@Tag(name = "开放接口-会员V1")
@OpenapiResponse
@RestController("openapiV1Member")
@RequestMapping("/api/openapi/internal/v1")
public class MemberController extends OpenapiBaseController {

	private final OpenapiMemberQueryPort memberQueryPort;
	private final OpenapiMemberCreatePort memberCreatePort;
	private final OpenapiMemberBasicInfoPort memberBasicInfoPort;
	private final OpenapiMemberOrderListPort memberOrderListPort;
	private final OpenapiMemberBrowseHistoryListPort memberBrowseHistoryListPort;
	private final OpenapiMemberListPort memberListPort;
	private final OpenapiMemberInfoListPort memberInfoListPort;
	private final OpenapiMemberFrequentItemsPort memberFrequentItemsPort;
	private final OpenapiMemberBrowseHistoryPort memberBrowseHistoryPort;
	private final OpenapiMemberAssignMemberToSalespersonPort assignMemberToSalespersonPort;
	private final OpenapiMemberNotifyBecomeFriendPort notifyBecomeFriendPort;
	private final OpenapiMemberCardGradesPort memberCardGradesPort;
	private final OpenapiMemberTagLibraryPort memberTagLibraryPort;
	private final OpenapiMemberTagLibraryPushPort memberTagLibraryPushPort;
	private final OpenapiMemberTagRelationPushPort memberTagRelationPushPort;

	public MemberController(
			OpenapiMemberQueryPort memberQueryPort,
			OpenapiMemberCreatePort memberCreatePort,
			OpenapiMemberBasicInfoPort memberBasicInfoPort,
			OpenapiMemberOrderListPort memberOrderListPort,
			OpenapiMemberBrowseHistoryListPort memberBrowseHistoryListPort,
			OpenapiMemberListPort memberListPort,
			OpenapiMemberInfoListPort memberInfoListPort,
			OpenapiMemberFrequentItemsPort memberFrequentItemsPort,
			OpenapiMemberBrowseHistoryPort memberBrowseHistoryPort,
			OpenapiMemberAssignMemberToSalespersonPort assignMemberToSalespersonPort,
			OpenapiMemberNotifyBecomeFriendPort notifyBecomeFriendPort,
			OpenapiMemberCardGradesPort memberCardGradesPort,
			OpenapiMemberTagLibraryPort memberTagLibraryPort,
			OpenapiMemberTagLibraryPushPort memberTagLibraryPushPort,
			OpenapiMemberTagRelationPushPort memberTagRelationPushPort) {
		this.memberQueryPort = memberQueryPort;
		this.memberCreatePort = memberCreatePort;
		this.memberBasicInfoPort = memberBasicInfoPort;
		this.memberOrderListPort = memberOrderListPort;
		this.memberBrowseHistoryListPort = memberBrowseHistoryListPort;
		this.memberListPort = memberListPort;
		this.memberInfoListPort = memberInfoListPort;
		this.memberFrequentItemsPort = memberFrequentItemsPort;
		this.memberBrowseHistoryPort = memberBrowseHistoryPort;
		this.assignMemberToSalespersonPort = assignMemberToSalespersonPort;
		this.notifyBecomeFriendPort = notifyBecomeFriendPort;
		this.memberCardGradesPort = memberCardGradesPort;
		this.memberTagLibraryPort = memberTagLibraryPort;
		this.memberTagLibraryPushPort = memberTagLibraryPushPort;
		this.memberTagRelationPushPort = memberTagRelationPushPort;
	}

	@PostMapping(value = "/ecx.member.create", name = "开放接口会员信息创建")
	public OpenapiEnvelope memberCreate(
			HttpServletRequest request,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String mobileOriginal = OpenapiRequestParams.originalString(mobileParam, body, "mobile");
		Map<String, Object> data = memberCreatePort.memberCreate(companyId, mobileOriginal);
		return new OpenapiEnvelope("success", "E0000", "操作成功", data);
	}

	@PostMapping(value = "/ecx.member.query", name = "开放接口查询会员信息")
	public OpenapiEnvelope memberInfo(
			HttpServletRequest request,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "unionid", required = false) String unionidParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		boolean mobilePresent = OpenapiMemberQueryParams.isParamPresent(mobileParam, body, "mobile");
		boolean unionidPresent = OpenapiMemberQueryParams.isParamPresent(unionidParam, body, "unionid");
		String mobileRaw = OpenapiRequestParams.mergeString(mobileParam, body, "mobile");
		String unionidRaw = OpenapiRequestParams.mergeString(unionidParam, body, "unionid");
		boolean mobileTruthy = OpenapiMemberQueryParams.isPhpTruthy(mobileRaw);
		boolean unionidTruthy = OpenapiMemberQueryParams.isPhpTruthy(unionidRaw);
		Map<String, Object> data =
				memberQueryPort.memberInfo(
						companyId,
						mobileParam,
						unionidParam,
						body,
						mobilePresent,
						mobileRaw,
						mobileTruthy,
						unionidPresent,
						unionidRaw,
						unionidTruthy);
		return new OpenapiEnvelope("success", "E0000", "操作成功", data);
	}

	@Operation(
			summary = "开放接口会员基础信息",
			description =
					"按签名鉴权租户 company_id 查询单个会员基础信息（7 键：user_id/mobile/username/birthday/"
							+ "create_time/gradeInfo/vipgrade）。mobile/username 解密返回；gradeInfo 为会员卡等级"
							+ " 9 键子集（zh-CN 分表覆盖）；vipgrade 为付费会员状态（svip 优先，形态随开通/持有变化）。"
							+ " 定位优先级：mobile 或 external_member_id PHP-truthy 时按其（可联合）查询，"
							+ " 否则按 unionid（wechat 关联）。三选一必填（PHP-empty 视为未提供）。"
							+ " 查无会员返回成功信封且 data=[]（非失败）。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.member.basicInfo, version=1.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 参数双通道（body 含键优先）；body 的 unionid/external_member_id 须为字符串类型。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.member.basicInfo")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态。"
								+ " 成功：status=success, code=E0000, message=操作成功，data 见 EcxMemberBasicInfoDataDoc"
								+ "（查无会员时 data=[]）；"
								+ " 参数校验失败：status=fail, code=E0001（请填写unionid/会员id/请填写正确的手机号/"
								+ "unionid或者手机号或会员id必填），data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxMemberBasicInfoFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxMemberBasicInfoSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功（未开通付费会员）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"操作成功\","
															+ "\"data\":{\"user_id\":5,\"mobile\":\"13800000000\",\"username\":\"u1\","
															+ "\"birthday\":\"2000-01-02\",\"create_time\":1764115200,"
															+ "\"gradeInfo\":{\"grade_id\":1,\"company_id\":\"100\","
															+ "\"grade_name\":\"普通会员\",\"default_grade\":true,"
															+ "\"background_pic_url\":null,\"promotion_condition\":[],"
															+ "\"privileges\":{},\"description\":null,\"grade_background\":null},"
															+ "\"vipgrade\":{\"is_open\":false,\"is_vip\":false,\"is_had_vip\":false}}}"),
									@ExampleObject(
											name = "success-not-found",
											summary = "查无会员（成功信封，data 为空数组）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"操作成功\",\"data\":[]}"),
									@ExampleObject(
											name = "fail-missing-identifier",
											summary = "三个定位参数均未提供",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\","
															+ "\"message\":\"unionid或者手机号或会员id必填\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.member.basicInfo", name = "开放接口会员基础信息")
	public OpenapiEnvelope basicInfo(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "手机号；提供时须匹配 ^1[3-9]\\d{9}$；与 unionid/external_member_id 三选一（body 同名键优先）",
							example = "13800000000")
					@RequestParam(name = "mobile", required = false)
					String mobileParam,
			@Parameter(
							description = "微信 unionid（wechat 关联定位）；body 中须为字符串；查无关联 data=[]（body 同名键优先）",
							example = "union-test")
					@RequestParam(name = "unionid", required = false)
					String unionidParam,
			@Parameter(
							description = "会员 ID（user_id）；body 中须为字符串；与 mobile 同时提供时联合定位（body 同名键优先）",
							example = "5")
					@RequestParam(name = "external_member_id", required = false)
					String externalMemberIdParam,
			@RequestBody(
							description =
									"可选 JSON body；字段与 query 同名双通道，body 含键时优先；"
											+ "unionid/external_member_id 须为字符串类型",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxMemberBasicInfoBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		boolean mobilePresent = OpenapiMemberQueryParams.isParamPresent(mobileParam, body, "mobile");
		boolean unionidPresent = OpenapiMemberQueryParams.isParamPresent(unionidParam, body, "unionid");
		boolean externalMemberIdPresent =
				OpenapiMemberQueryParams.isParamPresent(externalMemberIdParam, body, "external_member_id");
		String mobileRaw = OpenapiRequestParams.mergeString(mobileParam, body, "mobile");
		String unionidRaw = OpenapiRequestParams.mergeString(unionidParam, body, "unionid");
		String externalMemberIdRaw =
				OpenapiRequestParams.mergeString(externalMemberIdParam, body, "external_member_id");
		boolean mobileTruthy = OpenapiMemberQueryParams.isPhpTruthy(mobileRaw);
		boolean externalMemberIdTruthy = OpenapiMemberQueryParams.isPhpTruthy(externalMemberIdRaw);
		Object data =
				memberBasicInfoPort.basicInfo(
						companyId,
						mobileParam,
						unionidParam,
						externalMemberIdParam,
						body,
						mobilePresent,
						mobileRaw,
						mobileTruthy,
						externalMemberIdPresent,
						externalMemberIdRaw,
						externalMemberIdTruthy,
						unionidPresent,
						unionidRaw);
		return new OpenapiEnvelope("success", "E0000", "操作成功", data);
	}

	@PostMapping(value = "/ecx.member.orderList", name = "开放接口会员订单列表")
	public OpenapiEnvelope getMemberOrderLists(
			HttpServletRequest request,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "unionid", required = false) String unionidParam,
			@RequestParam(name = "external_member_id", required = false) String externalMemberIdParam,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@RequestParam(name = "order_class", required = false) String orderClassParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		boolean mobilePresent = OpenapiMemberQueryParams.isParamPresent(mobileParam, body, "mobile");
		boolean unionidPresent = OpenapiMemberQueryParams.isParamPresent(unionidParam, body, "unionid");
		boolean externalMemberIdPresent =
				OpenapiMemberQueryParams.isParamPresent(externalMemberIdParam, body, "external_member_id");
		String mobileRaw = OpenapiRequestParams.mergeString(mobileParam, body, "mobile");
		String unionidRaw = OpenapiRequestParams.mergeString(unionidParam, body, "unionid");
		String externalMemberIdRaw =
				OpenapiRequestParams.mergeString(externalMemberIdParam, body, "external_member_id");
		String orderClassRaw = OpenapiRequestParams.mergeString(orderClassParam, body, "order_class");
		boolean mobileTruthy = OpenapiMemberQueryParams.isPhpTruthy(mobileRaw);
		boolean externalMemberIdTruthy = OpenapiMemberQueryParams.isPhpTruthy(externalMemberIdRaw);
		int page = OpenapiMemberOrderListParams.resolvePage(pageParam, body);
		int pageSize = OpenapiMemberOrderListParams.resolvePageSize(pageSizeParam, body);
		String orderClassForService =
				OpenapiMemberQueryParams.isPhpTruthy(orderClassRaw) ? orderClassRaw : null;
		Map<String, Object> data =
				memberOrderListPort.getMemberOrderLists(
						companyId,
						mobileParam,
						unionidParam,
						externalMemberIdParam,
						orderClassParam,
						body,
						mobilePresent,
						mobileRaw,
						mobileTruthy,
						externalMemberIdPresent,
						externalMemberIdRaw,
						externalMemberIdTruthy,
						unionidPresent,
						unionidRaw,
						orderClassForService,
						page,
						pageSize);
		return new OpenapiEnvelope("success", "E0000", "操作成功", data);
	}

	@PostMapping(value = "/ecx.member.cardGrades", name = "开放接口获取会员卡等级列表")
	public OpenapiEnvelope getMemberCardGrades(
			HttpServletRequest request,
			@RequestParam(name = "country_code", required = false) String countryCodeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String lang = OpenapiPaginationParams.mergeCountryCode(countryCodeParam, body);
		if (lang == null) {
			lang = "zh-CN";
		}
		List<Map<String, Object>> data =
				memberCardGradesPort.getCompanyGradeSimpleList(companyId, lang);
		return new OpenapiEnvelope("success", "E0000", "操作成功", data);
	}

	@PostMapping(value = "/ecx.member.tagLibrary", name = "开放接口获取标签库")
	public OpenapiEnvelope tagLibrary(HttpServletRequest request) {
		long companyId = requireCompanyId(request);
		List<Map<String, Object>> data = memberTagLibraryPort.tagLibrary(companyId);
		return new OpenapiEnvelope("success", "E0000", "操作成功", data);
	}

	@PostMapping(value = "/ecx.member.tagLibrary.push", name = "开放接口推送标签库")
	public OpenapiEnvelope tagLibraryPush(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Object tagLibraryRaw = body != null ? body.get("tag_library") : null;
		if (OpenapiTagLibraryPushParams.isInvalidTagLibrary(tagLibraryRaw)) {
			return OpenapiEnvelope.fail("E1001", "标签库数据不能为空", List.of());
		}
		List<Map<String, Object>> tagLibrary = OpenapiTagLibraryPushParams.resolveTagLibrary(body);
		Map<String, Object> data = memberTagLibraryPushPort.pushTagLibrary(companyId, tagLibrary);
		return new OpenapiEnvelope("success", "E0000", "操作成功", data);
	}

	@PostMapping(value = "/ecx.member.tag.relation.push", name = "开放接口推送会员标签关系")
	public OpenapiEnvelope tagRelationPush(
			HttpServletRequest request,
			@RequestParam(name = "action", required = false) String actionParam,
			@RequestParam(name = "user_id", required = false) String userIdParam,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Object relationsRaw = body != null ? body.get("relations") : null;
		String action = OpenapiTagRelationPushParams.mergeAction(actionParam, body);

		if (OpenapiTagRelationPushParams.isClearMode(action, relationsRaw)) {
			if (OpenapiTagRelationPushParams.isClearModeUserIdMissing(userIdParam, body)) {
				return OpenapiEnvelope.fail("E1001", "清空模式需要提供 user_id", List.of());
			}
			String userId = OpenapiTagRelationPushParams.mergeUserId(userIdParam, body);
			String mobile = OpenapiTagRelationPushParams.mergeMobile(mobileParam, body);
			Map<String, Object> data =
					memberTagRelationPushPort.pushTagRelations(
							companyId, action, userId, mobile, List.of());
			return new OpenapiEnvelope("success", "E0000", "操作成功", data);
		}

		if (OpenapiTagRelationPushParams.isRelationsEmptyOrNotArray(relationsRaw)) {
			return OpenapiEnvelope.fail("E1001", "标签关系数据不能为空", List.of());
		}
		List<Map<String, Object>> relations = OpenapiTagRelationPushParams.resolveRelations(body);
		Map<String, Object> data =
				memberTagRelationPushPort.pushTagRelations(companyId, action, null, null, relations);
		return new OpenapiEnvelope("success", "E0000", "操作成功", data);
	}

	@PostMapping(value = "/ecx.member.frequentItems", name = "开放接口会员常购清单")
	public OpenapiEnvelope frequentItems(
			HttpServletRequest request,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "unionid", required = false) String unionidParam,
			@RequestParam(name = "external_member_id", required = false) String externalMemberIdParam,
			@RequestParam(name = "timeRange", required = false) String timeRangeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		boolean mobilePresent = OpenapiMemberQueryParams.isParamPresent(mobileParam, body, "mobile");
		boolean unionidPresent = OpenapiMemberQueryParams.isParamPresent(unionidParam, body, "unionid");
		boolean externalMemberIdPresent =
				OpenapiMemberQueryParams.isParamPresent(externalMemberIdParam, body, "external_member_id");
		boolean timeRangePresent = OpenapiMemberQueryParams.isParamPresent(timeRangeParam, body, "timeRange");
		String mobileRaw = OpenapiRequestParams.mergeString(mobileParam, body, "mobile");
		String unionidRaw = OpenapiRequestParams.mergeString(unionidParam, body, "unionid");
		String externalMemberIdRaw =
				OpenapiRequestParams.mergeString(externalMemberIdParam, body, "external_member_id");
		String timeRangeRaw = OpenapiRequestParams.mergeString(timeRangeParam, body, "timeRange");
		boolean mobileTruthy = OpenapiMemberQueryParams.isPhpTruthy(mobileRaw);
		boolean externalMemberIdTruthy = OpenapiMemberQueryParams.isPhpTruthy(externalMemberIdRaw);
		Map<String, Object> data =
				memberFrequentItemsPort.frequentItems(
						companyId,
						mobileParam,
						unionidParam,
						externalMemberIdParam,
						timeRangeParam,
						body,
						mobilePresent,
						mobileRaw,
						mobileTruthy,
						externalMemberIdPresent,
						externalMemberIdRaw,
						externalMemberIdTruthy,
						unionidPresent,
						unionidRaw,
						timeRangePresent,
						timeRangeRaw);
		return new OpenapiEnvelope("success", "E0000", "操作成功", data);
	}

	@PostMapping(value = "/ecx.member.browserHistory", name = "开放接口会员浏览足迹")
	public OpenapiEnvelope browseHistory(
			HttpServletRequest request,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "unionid", required = false) String unionidParam,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		boolean mobilePresent = OpenapiMemberQueryParams.isParamPresent(mobileParam, body, "mobile");
		boolean unionidPresent = OpenapiMemberQueryParams.isParamPresent(unionidParam, body, "unionid");
		String mobileRaw = OpenapiRequestParams.mergeString(mobileParam, body, "mobile");
		String unionidRaw = OpenapiRequestParams.mergeString(unionidParam, body, "unionid");
		boolean mobileTruthy = OpenapiMemberQueryParams.isPhpTruthy(mobileRaw);
		OpenapiMemberBrowseHistoryParams.BrowseHistoryPagination pagination =
				OpenapiMemberBrowseHistoryParams.resolveBrowseHistoryPagination(
						pageParam, pageSizeParam, body);
		Map<String, Object> data =
				memberBrowseHistoryPort.browseHistory(
						companyId,
						mobileParam,
						unionidParam,
						body,
						mobilePresent,
						mobileRaw,
						mobileTruthy,
						unionidPresent,
						unionidRaw,
						pagination.page(),
						pagination.pageSize());
		return new OpenapiEnvelope("success", "E0000", "操作成功", data);
	}

	@PostMapping(value = "/ecx.member.browserHistoryList", name = "开放接口会员浏览足迹列表")
	public OpenapiEnvelope geMembertBrowseList(
			HttpServletRequest request,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "unionid", required = false) String unionidParam,
			@RequestParam(name = "external_member_id", required = false) String externalMemberIdParam,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		boolean mobilePresent = OpenapiMemberQueryParams.isParamPresent(mobileParam, body, "mobile");
		boolean unionidPresent = OpenapiMemberQueryParams.isParamPresent(unionidParam, body, "unionid");
		boolean externalMemberIdPresent =
				OpenapiMemberQueryParams.isParamPresent(externalMemberIdParam, body, "external_member_id");
		String mobileRaw = OpenapiRequestParams.mergeString(mobileParam, body, "mobile");
		String unionidRaw = OpenapiRequestParams.mergeString(unionidParam, body, "unionid");
		String externalMemberIdRaw =
				OpenapiRequestParams.mergeString(externalMemberIdParam, body, "external_member_id");
		boolean mobileTruthy = OpenapiMemberQueryParams.isPhpTruthy(mobileRaw);
		boolean externalMemberIdTruthy = OpenapiMemberQueryParams.isPhpTruthy(externalMemberIdRaw);
		int page = OpenapiMemberOrderListParams.resolvePage(pageParam, body);
		int pageSize = OpenapiMemberOrderListParams.resolvePageSize(pageSizeParam, body);
		Map<String, Object> data =
				memberBrowseHistoryListPort.geMembertBrowseList(
						companyId,
						mobileParam,
						unionidParam,
						externalMemberIdParam,
						body,
						mobilePresent,
						mobileRaw,
						mobileTruthy,
						externalMemberIdPresent,
						externalMemberIdRaw,
						externalMemberIdTruthy,
						unionidPresent,
						unionidRaw,
						page,
						pageSize);
		return new OpenapiEnvelope("success", "E0000", "操作成功", data);
	}

	@PostMapping(value = "/ecx.member.list", name = "开放接口会员基础信息列表")
	public OpenapiEnvelope memberInfoList(
			HttpServletRequest request,
			@RequestParam(name = "unionid", required = false) String unionidParam,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		boolean unionidPresent = OpenapiMemberQueryParams.isParamPresent(unionidParam, body, "unionid");
		String unionidRaw = OpenapiRequestParams.mergeString(unionidParam, body, "unionid");
		OpenapiMemberInfoListParams.AssociationsPagination pagination =
				OpenapiMemberInfoListParams.resolveAssociationsPagination(pageParam, pageSizeParam, body);
		Object data =
				memberInfoListPort.memberInfoList(
						companyId,
						unionidParam,
						body,
						unionidPresent,
						unionidRaw,
						pagination.page(),
						pagination.pageSize());
		return new OpenapiEnvelope("success", "E0000", "操作成功", data);
	}

	@Operation(
			summary = "开放接口分配客户回调通知",
			description =
					"企微分配客户回调：按签名鉴权租户 company_id 与 unionid（members_associations，user_type=wechat）"
							+ " 定位会员，将其归属导购更新为 employee_number（members.fp_salesperson，has_fp=true）。"
							+ " 幂等：已分配相同导购且 has_fp=true 时不重复写库，直接返回成功。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}"
							+ "（method=ecx.member.assignMemberToSalesperson, version=1.0）签名校验后转发；"
							+ " 直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 参数双通道：body 含键取原始值（null 不回退 query），否则 query 原样。"
							+ " 失败码为接口自定义语义：E4003 缺参、E4002 会员不存在、E5001 系统错误。"
							+ " 副作用：写库更新 members 归属导购字段；无异步/第三方调用。",
			operationId = "ecx.member.assignMemberToSalesperson")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态。"
								+ " 成功：status=success, code=E0000, message=分配成功，data 回显 employee_number/unionid"
								+ " 并附 assign_time；"
								+ " 业务失败：status=fail（E4003 请填写导购员工编号/请填写会员unionid；E4002 会员不存在；"
								+ "E5001 系统错误：{异常信息}），data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。"
								+ " 见 EcxMemberAssignToSalespersonFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxMemberAssignToSalespersonSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "分配成功",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"分配成功\","
															+ "\"data\":{\"employee_number\":\"E9\",\"unionid\":\"union-test\","
															+ "\"assign_time\":\"2026-11-26 12:00:00\"}}"),
									@ExampleObject(
											name = "fail-member-not-found",
											summary = "会员不存在（E4002，接口自定义语义）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4002\",\"message\":\"会员不存在\",\"data\":null}"),
									@ExampleObject(
											name = "fail-missing-employee",
											summary = "缺导购员工编号（E4003）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4003\","
															+ "\"message\":\"请填写导购员工编号\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.member.assignMemberToSalesperson", name = "开放接口分配客户回调通知")
	public OpenapiEnvelope assignMemberToSalesperson(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "导购员工编号（写入 members.fp_salesperson）；必填非空（body 同名键优先）",
							required = true,
							example = "E9")
					@RequestParam(name = "employee_number", required = false)
					String employeeNumberParam,
			@Parameter(
							description = "会员微信 unionid（租户内 wechat 关联定位）；必填非空（body 同名键优先）",
							required = true,
							example = "union-test")
					@RequestParam(name = "unionid", required = false)
					String unionidParam,
			@RequestBody(
							description =
									"可选 JSON body；字段与 query 同名双通道，body 含键时取原始值"
											+ "（null 视为未提供且不回退 query）",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxMemberAssignToSalespersonBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String employeeNumber =
				OpenapiRequestParams.originalString(employeeNumberParam, body, "employee_number");
		String unionid = OpenapiRequestParams.originalString(unionidParam, body, "unionid");
		Map<String, Object> data =
				assignMemberToSalespersonPort.assignMemberToSalesperson(
						companyId, employeeNumber, unionid);
		return new OpenapiEnvelope("success", "E0000", "分配成功", data);
	}

	@PostMapping(value = "/ecx.member.notifyBecomeFriend", name = "开放接口导购通知加好友")
	public OpenapiEnvelope notifyBecomeFriend(
			HttpServletRequest request,
			@RequestParam(name = "unionid", required = false) String unionidParam,
			@RequestParam(name = "salesperson_code", required = false) String salespersonCodeParam,
			@RequestParam(name = "is_become_friend", required = false) String isBecomeFriendParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String unionid = OpenapiRequestParams.originalString(unionidParam, body, "unionid");
		String salespersonCode =
				OpenapiRequestParams.originalString(salespersonCodeParam, body, "salesperson_code");
		String isBecomeFriendRaw =
				OpenapiRequestParams.originalString(isBecomeFriendParam, body, "is_become_friend");
		notifyBecomeFriendPort.notifyBecomeFriend(
				companyId, unionid, salespersonCode, isBecomeFriendRaw);
		return new OpenapiEnvelope("success", "E0000", "success", List.of());
	}

	@PostMapping(value = "/ecx.member.listFp", name = "开放接口会员列表查询（增强版）")
	public OpenapiEnvelope memberList(
			HttpServletRequest request,
			@RequestParam(name = "scope", required = false) String scopeParam,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@RequestParam(name = "birthday_start", required = false) String birthdayStartParam,
			@RequestParam(name = "birthday_end", required = false) String birthdayEndParam,
			@RequestParam(name = "tag_id", required = false) List<String> tagIdQueryList,
			@RequestParam(name = "type", required = false) String typeParam,
			@RequestParam(name = "salesperson_code", required = false) String salespersonCodeParam,
			@RequestParam(name = "store_bn", required = false) String storeBnParam,
			@RequestParam(name = "point_start", required = false) String pointStartParam,
			@RequestParam(name = "point_end", required = false) String pointEndParam,
			@RequestParam(name = "grade_id", required = false) String gradeIdParam,
			@RequestParam(name = "buy_start", required = false) String buyStartParam,
			@RequestParam(name = "buy_end", required = false) String buyEndParam,
			@RequestParam(name = "keyword", required = false) String keywordParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Map<String, Object> mergedParams =
				OpenapiMemberListParams.buildMergedParams(
						body,
						tagIdQueryList,
						scopeParam,
						pageParam,
						pageSizeParam,
						birthdayStartParam,
						birthdayEndParam,
						typeParam,
						salespersonCodeParam,
						storeBnParam,
						pointStartParam,
						pointEndParam,
						gradeIdParam,
						buyStartParam,
						buyEndParam,
						keywordParam);
		Object data = memberListPort.memberList(companyId, mergedParams);
		return new OpenapiEnvelope("success", "E0000", "操作成功", data);
	}
}
