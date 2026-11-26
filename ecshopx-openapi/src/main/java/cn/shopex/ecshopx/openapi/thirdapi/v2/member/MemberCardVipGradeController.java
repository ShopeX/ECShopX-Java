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
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxVipGradeCreateBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxVipGradeCreateSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxVipGradeDeleteBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxVipGradeDeleteSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxVipGradeDetailBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxVipGradeDetailSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxVipGradeListBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxVipGradeListSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxVipGradeOrderListBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxVipGradeOrderListSuccessEnvelopeDoc;
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
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OpenAPI v2 handler scaffold — methods added during migration. */
@Tag(name = "开放接口-付费会员卡等级V2")
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

	@Operation(
			summary = "开放接口查询付费会员卡等级列表",
			description =
					"按签名鉴权租户 company_id 分页查询付费会员卡等级：3 键双通道"
							+ "（body 含键优先、值 null 视为未传、不 trim）。"
							+ " page 默认 1、page_size 默认 20（PHP 式解析），page_size 原始解析 ≥500 报"
							+ " E0001「每页的大小最大为500！」（复用运费模板分页解析器，ResourceException 通道；"
							+ " 500 本身也报错），钳制后 page≥1、page_size∈[20,499]（另两条分页文案分支不可达）。"
							+ " vip_grade_id 过滤按 PHP isset 非空语义（null/空串忽略，\"0\" 参与）：可 parseLong → 等值；"
							+ " 非数字 → 按 vip_grade_id=-1 过滤（恒空结果，不报错）。"
							+ " vip_grade_id 降序；无多语言覆盖（区别于 membercard 等级列表）。"
							+ " data 键序 total_count/is_last_page(0|1)/pager{page,page_size}/list；"
							+ " 行 15 键（同 create：vip_grade_id/type/grade_name/三档 fee 原样字符串或 null/discount/"
							+ " guide_title/description/background_pic_url/is_default(0|1)/is_disabled(0|1)/external_id/"
							+ " created/updated）。Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.member_card_vip_grade.list,"
							+ " version=2.0）签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.member_card_vip_grade.list.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxVipGradeListDataDoc；"
								+ " 业务失败：status=fail, code=E0001 每页的大小最大为500！，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxVipGradeListFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxVipGradeListSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "列表查询成功（时间为格式演示值）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":1,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},"
															+ "\"list\":[{\"vip_grade_id\":2,\"type\":\"vip\","
															+ "\"grade_name\":\"Gold\",\"monthly_fee\":\"0\","
															+ "\"quarter_fee\":null,\"year_fee\":null,\"discount\":\"10\","
															+ "\"guide_title\":\"\",\"description\":\"\","
															+ "\"background_pic_url\":\"\",\"is_default\":0,\"is_disabled\":0,"
															+ "\"external_id\":\"\",\"created\":\"2026-01-01 00:00:00\","
															+ "\"updated\":\"2026-01-01 00:00:00\"}]}}"),
									@ExampleObject(
											name = "success-empty",
											summary = "vip_grade_id 过滤无匹配（含非数字恒空场景）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":0,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},\"list\":[]}}"),
									@ExampleObject(
											name = "fail-page-size",
											summary = "page_size 原始解析值 ≥500（E0001 通道）",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\","
															+ "\"message\":\"每页的大小最大为500！\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@GetMapping(value = "/ecx.member_card_vip_grade.list", name = "开放接口查询付费会员卡等级列表")
	public Map<String, Object> list(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "页码，默认 1；PHP 式解析后 max(…,1)，负数/0/非法按 1",
							example = "1")
					@RequestParam(name = "page", required = false)
					String pageParam,
			@Parameter(
							description = "每页数量，默认 20；原始解析值 ≥500 报 E0001「每页的大小最大为500！」；钳制后生效 [20,499]",
							example = "20")
					@RequestParam(name = "page_size", required = false)
					String pageSizeParam,
			@Parameter(
							description = "付费等级 ID 过滤；null/空串忽略（\"0\" 参与过滤）；非数字按 -1 过滤恒空结果，不报错",
							example = "2")
					@RequestParam(name = "vip_grade_id", required = false)
					String vipGradeIdParam,
			@RequestBody(
							description = "可选 JSON body；3 键与 query 同名双通道（body 含键优先，值 null 视为未传）；语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxVipGradeListBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2ShippingTemplatesListParams.PageSpec pageSpec =
				OpenapiThirdApiV2ShippingTemplatesListParams.resolve(pageParam, pageSizeParam, body);
		String vipGradeIdRaw = OpenapiRequestParams.originalString(vipGradeIdParam, body, "vip_grade_id");
		return memberCardVipGradeListPort.list(
				companyId, pageSpec.page(), pageSpec.pageSize(), vipGradeIdRaw);
	}

	@Operation(
			summary = "开放接口创建付费会员卡等级",
			description =
					"按签名鉴权租户 company_id 创建付费会员卡等级（10 键双通道，body 含键优先、值 null 视为未传）："
							+ " grade_name 必填非空（原样不 trim，E5001「未填写付费等级名称」）；"
							+ " discount 必填（E5001「未填写会员折扣」），须 1≤值≤10（否则 E5001「请求参数错误」），"
							+ " 原样存 privileges.discount_desc、另存 discount=100-(int)(值*10)；"
							+ " monthly_fee/quarter_fee/year_fee 至少一项非 null（全缺 E5001「购买金额必填一项」），"
							+ " 键存在须非空且数值 ≥0（否则 E5001「请求参数错误」），原样字符串存 price_list"
							+ "（monthly=30 天/quarter=90 天/year=365 天，未传档 price=null）；"
							+ " guide_title/description/is_default/is_disabled/external_id 走 mergeString trim。"
							+ " is_default/is_disabled 弱布尔：null/空串/\"0\"/\"false\"→false，其余 true。"
							+ " lv_type 自动分配：租户无等级→vip；已有 vip→svip；同类型已存在 E5542"
							+ "「该类型下会员付费等级已存在」；vip+svip 均存在 E5540「最多只能创建2个会员付费等级」。"
							+ " 注意：external_id 被接收但忽略（恒写空串）；background_pic_url 恒空串。"
							+ " 序列化异常 E0001「付费等级数据序列化失败」。"
							+ " 成功 data 15 键行（vip_grade_id/type/grade_name/三档 fee 回显原样字符串或 null/"
							+ " discount/guide_title/description/background_pic_url/is_default(0|1)/is_disabled(0|1)/"
							+ " external_id/created/updated）。Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}"
							+ "（method=ecx.member_card_vip_grade.create, version=2.0）签名校验后转发；"
							+ " 直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：INSERT kaquan_vip_grade 单行（无显式事务、无多语言分表写）；无异步、无第三方调用。",
			operationId = "ecx.member_card_vip_grade.create.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxVipGradeCreateDataDoc；"
								+ " 业务失败：status=fail, code=E5001 未填写付费等级名称·未填写会员折扣·购买金额必填一项·请求参数错误 /"
								+ " E5542 该类型下会员付费等级已存在 / E5540 最多只能创建2个会员付费等级 /"
								+ " E0001 付费等级数据序列化失败，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxVipGradeCreateFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxVipGradeCreateSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "创建成功（首个等级 type=vip；仅传月卡 0 元；时间为格式演示值）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"vip_grade_id\":2,\"type\":\"vip\",\"grade_name\":\"Gold\","
															+ "\"monthly_fee\":\"0\",\"quarter_fee\":null,\"year_fee\":null,"
															+ "\"discount\":\"10\",\"guide_title\":\"\",\"description\":\"\","
															+ "\"background_pic_url\":\"\",\"is_default\":0,\"is_disabled\":0,"
															+ "\"external_id\":\"\",\"created\":\"2026-01-01 00:00:00\","
															+ "\"updated\":\"2026-01-01 00:00:00\"}}"),
									@ExampleObject(
											name = "fail-missing-fee",
											summary = "三档金额全缺",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\","
															+ "\"message\":\"购买金额必填一项\",\"data\":null}"),
									@ExampleObject(
											name = "fail-type-exist",
											summary = "待分配类型已有等级",
											value =
													"{\"status\":\"fail\",\"code\":\"E5542\","
															+ "\"message\":\"该类型下会员付费等级已存在\",\"data\":null}"),
									@ExampleObject(
											name = "fail-upper-limit",
											summary = "vip 与 svip 均已存在",
											value =
													"{\"status\":\"fail\",\"code\":\"E5540\","
															+ "\"message\":\"最多只能创建2个会员付费等级\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.member_card_vip_grade.create", name = "开放接口创建付费会员卡等级")
	public Map<String, Object> create(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "付费等级名称；必填非空（不 trim），否则 E5001「未填写付费等级名称」",
							required = true,
							example = "Gold")
					@RequestParam(name = "grade_name", required = false)
					String gradeNameParam,
			@Parameter(
							description = "月卡金额（元）；三档至少一项；键存在须非空且 ≥0（否则 E5001）；原样字符串落库",
							example = "0")
					@RequestParam(name = "monthly_fee", required = false)
					String monthlyFeeParam,
			@Parameter(
							description = "季卡金额（元，90 天档）；校验同 monthly_fee",
							example = "0")
					@RequestParam(name = "quarter_fee", required = false)
					String quarterFeeParam,
			@Parameter(
							description = "年卡金额（元，365 天档）；校验同 monthly_fee",
							example = "0")
					@RequestParam(name = "year_fee", required = false)
					String yearFeeParam,
			@Parameter(
							description = "会员折扣（折）；必填，1≤值≤10（可小数）；10=无折扣；越界/非数字 E5001「请求参数错误」",
							required = true,
							example = "10")
					@RequestParam(name = "discount", required = false)
					String discountParam,
			@Parameter(description = "导购标题；可选，trim；null/空串落库 null")
					@RequestParam(name = "guide_title", required = false)
					String guideTitleParam,
			@Parameter(description = "等级描述；可选，trim；null/空串落库 null")
					@RequestParam(name = "description", required = false)
					String descriptionParam,
			@Parameter(
							description = "是否默认等级；弱布尔：空/\"0\"/\"false\"→false，其余 true；默认 false",
							example = "0")
					@RequestParam(name = "is_default", required = false)
					String isDefaultParam,
			@Parameter(
							description = "是否停用；弱布尔语义同 is_default；默认 false",
							example = "0")
					@RequestParam(name = "is_disabled", required = false)
					String isDisabledParam,
			@Parameter(
							description = "遗留参数：被接收但忽略（服务恒写 external_id 空串）",
							deprecated = true)
					@RequestParam(name = "external_id", required = false)
					String externalIdParam,
			@RequestBody(
							description =
									"可选 JSON body；10 键与 query 同名双通道（body 含键优先，值 null 视为未传）；"
											+ "前五键不 trim、后五键 trim；语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxVipGradeCreateBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
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

	@Operation(
			summary = "开放接口删除付费会员卡等级",
			description =
					"按签名鉴权租户 company_id 删除付费会员卡等级：vip_grade_id 必填非空"
							+ "（originalString 双通道，body 含键优先、值 null 视为未传、不 trim；"
							+ " 缺失 E5001「请求参数错误」）。trim 后 parseLong，失败再按整值 double 截断"
							+ "（如 \"2.0\"→2），仍失败按 -1 查询；kaquan_vip_grade 查无 E5541「会员付费等级找不到」。"
							+ " 默认等级禁删（is_default=true → E5543「会员付费等级删除错误」）；"
							+ " 关联会员检查 vip_grade_rel_user(company_id, vip_grade_id) 计数（trim+parseLong，"
							+ " 非法按 0；不过滤 end_date，过期关联也计入），>0 报 E5543"
							+ "「会员付费等级无法删除，该会员付费等级下扔存在关联的会员」（源码原文含错字「扔」）。"
							+ " 成功恒 data=null，Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}"
							+ "（method=ecx.member_card_vip_grade.delete, version=2.0）签名校验后转发；"
							+ " 直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：事务内 DELETE kaquan_vip_grade 单行；不清理 vip_grade_rel_user 关联；"
							+ " 无异步、无第三方调用。",
			operationId = "ecx.member_card_vip_grade.delete.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功, data 恒 null；"
								+ " 业务失败：status=fail, code=E5001 请求参数错误 / E5541 会员付费等级找不到 /"
								+ " E5543 会员付费等级删除错误（默认等级）·存在关联会员禁删，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxVipGradeDeleteFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxVipGradeDeleteSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "删除成功（无返回体）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\",\"data\":null}"),
									@ExampleObject(
											name = "fail-grade-not-found",
											summary = "vip_grade_id 查无（含非法字符串按 -1 查询）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5541\","
															+ "\"message\":\"会员付费等级找不到\",\"data\":null}"),
									@ExampleObject(
											name = "fail-default-grade",
											summary = "默认等级禁删",
											value =
													"{\"status\":\"fail\",\"code\":\"E5543\","
															+ "\"message\":\"会员付费等级删除错误\",\"data\":null}"),
									@ExampleObject(
											name = "fail-grade-in-use",
											summary = "等级下仍有关联会员（文案原文含错字「扔」）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5543\","
															+ "\"message\":\"会员付费等级无法删除，该会员付费等级下扔存在关联的会员\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@DeleteMapping(value = "/ecx.member_card_vip_grade.delete", name = "开放接口删除付费会员卡等级")
	public Void delete(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description =
									"付费等级 ID；必填非空（E5001）；trim+parseLong（整值 double 兜底，非法按 -1），"
											+ "查无 E5541；默认等级/有关联会员 E5543",
							required = true,
							example = "2")
					@RequestParam(name = "vip_grade_id", required = false)
					String vipGradeIdParam,
			@RequestBody(
							description = "可选 JSON body；vip_grade_id 与 query 同名双通道（body 含键优先，值 null 视为未传）",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxVipGradeDeleteBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		memberCardVipGradeDeletePort.delete(
				companyId,
				OpenapiRequestParams.originalString(vipGradeIdParam, body, "vip_grade_id"));
		return null;
	}

	@Operation(
			summary = "开放接口查询付费会员卡等级购买记录列表",
			description =
					"按签名鉴权租户 company_id 分页查询付费会员卡购买记录：仅 page/page_size 2 键双通道"
							+ "（body 含键优先、值 null 视为未传、不 trim）。"
							+ " 固定过滤 kaquan_vip_grade_order.order_status=DONE（已完成订单，含购买/领取/续期/赠送来源），"
							+ " order_id 降序。page 默认 1、page_size 默认 20（PHP 式解析），"
							+ " page_size 原始解析 ≥500 报 E0001「每页的大小最大为500！」（复用运费模板分页解析器，"
							+ " ResourceException 通道；500 本身也报错），钳制后 page≥1、page_size∈[20,499]。"
							+ " data 键序 total_count/is_last_page(0|1)/pager{page,page_size}/list；行 11 键："
							+ " order_id 字符串、price 元 2 位小数字符串（分 HALF_UP 除 100）、user_id/vip_grade_id 整型、"
							+ " mobile 解密明文（失败回退原存储值）、lv_type(vip/svip)、title、"
							+ " card_type 为天数整型（card_type JSON.day：monthly=30/quarter=90/year=365）、"
							+ " discount 折 1 位小数字符串（(100-额度)/10，0 额度→\"10.0\"）、created/updated 格式化时间。"
							+ " Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}"
							+ "（method=ecx.member_card_vip_grade_order.list, version=2.0）签名校验后转发；"
							+ " 直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.member_card_vip_grade_order.list.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxVipGradeOrderListDataDoc；"
								+ " 业务失败：status=fail, code=E0001 每页的大小最大为500！，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxVipGradeOrderListFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxVipGradeOrderListSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功（示例值取自 VipGradeMembercardSalePaidFlowTest；时间/手机号为格式演示值）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":1,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},"
															+ "\"list\":[{\"order_id\":\"100\",\"price\":\"0.00\","
															+ "\"user_id\":50,\"mobile\":\"13800138000\",\"vip_grade_id\":2,"
															+ "\"lv_type\":\"vip\",\"title\":\"Gold\",\"card_type\":30,"
															+ "\"discount\":\"10.0\",\"created\":\"2026-01-01 00:00:00\","
															+ "\"updated\":\"2026-01-01 00:00:00\"}]}}"),
									@ExampleObject(
											name = "success-empty",
											summary = "无已完成购买记录（list 空数组）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":0,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},\"list\":[]}}"),
									@ExampleObject(
											name = "fail-page-size",
											summary = "page_size 原始解析值 ≥500（E0001 通道）",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\","
															+ "\"message\":\"每页的大小最大为500！\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@GetMapping(value = "/ecx.member_card_vip_grade_order.list", name = "开放接口查询付费会员卡等级购买记录列表")
	public Map<String, Object> orderList(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "页码，默认 1；PHP 式解析后 max(…,1)，负数/0/非法按 1",
							example = "1")
					@RequestParam(name = "page", required = false)
					String pageParam,
			@Parameter(
							description = "每页数量，默认 20；原始解析值 ≥500 报 E0001「每页的大小最大为500！」；钳制后生效 [20,499]",
							example = "20")
					@RequestParam(name = "page_size", required = false)
					String pageSizeParam,
			@RequestBody(
							description = "可选 JSON body；page/page_size 与 query 同名双通道（body 含键优先，值 null 视为未传）",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxVipGradeOrderListBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2ShippingTemplatesListParams.PageSpec pageSpec =
				OpenapiThirdApiV2ShippingTemplatesListParams.resolve(pageParam, pageSizeParam, body);
		return memberCardVipGradeOrderListPort.list(
				companyId, pageSpec.page(), pageSpec.pageSize());
	}

	@Operation(
			summary = "开放接口查询会员关联付费等级信息",
			description =
					"按签名鉴权租户 company_id 以手机号查询会员当前关联的付费会员卡等级："
							+ " mobile 必填（originalString 双通道，body 含键优先、值 null 视为未传；服务内 trim 后须"
							+ " ^1[3456789][0-9]{9}$，缺失/空白/格式错统一 E5001「缺少参数」）。"
							+ " 会员按固定加密 mobile 等值定位（LIMIT 1），查无或 user_id 为空 E5501「会员找不到」。"
							+ " 关联查 vip_grade_rel_user(company_id, user_id)（不过滤 end_date，过期关联仍返回；"
							+ " rel 的 vip_type/end_date 被读取但不透出）。"
							+ " 无关联/关联无 vip_grade_id/等级行查无均不报错，返回占位行"
							+ "（vip_grade_id=0、三档 fee=null、discount=\"0\"、is_disabled=0、其余字符串键空串）。"
							+ " data 14 固定键：vip_grade_id/type/grade_name/monthly_fee/quarter_fee/year_fee/"
							+ " discount/guide_title/description/background_pic_url/is_disabled(0|1)/external_id/"
							+ " created/updated；无 is_default 键（区别于 create/update 行格式）。"
							+ " Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}"
							+ "（method=ecx.member_card_vip_grade.detail, version=2.0）签名校验后转发；"
							+ " 直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.member_card_vip_grade.detail.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxVipGradeDetailDataDoc"
								+ "（无关联/等级查无为占位行，仍 success）；"
								+ " 业务失败：status=fail, code=E5001 缺少参数 / E5501 会员找不到，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxVipGradeDetailFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxVipGradeDetailSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功（时间为格式演示值）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"vip_grade_id\":2,\"type\":\"vip\",\"grade_name\":\"Gold\","
															+ "\"monthly_fee\":\"0\",\"quarter_fee\":null,\"year_fee\":null,"
															+ "\"discount\":\"10\",\"guide_title\":\"\",\"description\":\"\","
															+ "\"background_pic_url\":\"\",\"is_disabled\":0,\"external_id\":\"\","
															+ "\"created\":\"2026-01-01 00:00:00\",\"updated\":\"2026-01-01 00:00:00\"}}"),
									@ExampleObject(
											name = "success-no-vip",
											summary = "会员无付费关联（占位行，仍 success）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"vip_grade_id\":0,\"type\":\"\",\"grade_name\":\"\","
															+ "\"monthly_fee\":null,\"quarter_fee\":null,\"year_fee\":null,"
															+ "\"discount\":\"0\",\"guide_title\":\"\",\"description\":\"\","
															+ "\"background_pic_url\":\"\",\"is_disabled\":0,\"external_id\":\"\","
															+ "\"created\":\"\",\"updated\":\"\"}}"),
									@ExampleObject(
											name = "fail-mobile",
											summary = "mobile 缺失/空白/格式错",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\",\"message\":\"缺少参数\",\"data\":null}"),
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
	@GetMapping(value = "/ecx.member_card_vip_grade.detail", name = "开放接口查询会员关联付费等级信息")
	public Map<String, Object> detail(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "会员手机号（明文）；必填，trim 后须 ^1[3456789][0-9]{9}$；缺失/格式错 E5001「缺少参数」；查无会员 E5501",
							required = true,
							example = "13800138000")
					@RequestParam(name = "mobile", required = false)
					String mobileParam,
			@RequestBody(
							description = "可选 JSON body；mobile 与 query 同名双通道（body 含键优先，值 null 视为未传）",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxVipGradeDetailBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String mobileRaw = OpenapiRequestParams.originalString(mobileParam, body, "mobile");
		return memberCardVipGradeDetailPort.getVipGradeDetailByMobile(companyId, mobileRaw);
	}
}
