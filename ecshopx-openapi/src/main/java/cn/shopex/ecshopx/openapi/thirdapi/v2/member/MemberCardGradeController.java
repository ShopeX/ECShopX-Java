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
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxCardGradeBatchSaveBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxCardGradeBatchSaveSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberCardGradeCreateBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberCardGradeCreateSuccessEnvelopeDoc;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OpenAPI v2 handler scaffold — methods added during migration. */
@Tag(name = "开放接口-会员卡等级V2")
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

	@Operation(
			summary = "开放接口创建会员卡等级",
			description =
					"按签名鉴权租户 company_id 创建会员卡等级（5 键双通道，body 含键优先、值 null 视为未传）："
							+ " grade_name 必填非空（原样不 trim，E5001「未填写等级名称」）；"
							+ " discount 必填（E5001「未填写会员折扣」），须可 parseDouble 且 1≤值≤10（否则 E5001「请求参数错误」），"
							+ " 原样存 privileges.discount_desc、另存 privileges.discount=100-(int)(值*10)（小数截断）；"
							+ " total_consumption 必填（E5001「未填写升级条件」），须可 parseDouble 且 ≥0"
							+ "（否则 E5001「请求参数错误」），原样存 promotion_condition.total_consumption；"
							+ " background_pic_url/external_id 可选（mergeString trim，null 按空串）。"
							+ " 租户等级计数 ≥6 拒绝（E5530「最多只能创建5个会员等级」，实际含默认等级共 6 上限）。"
							+ " 新建恒非默认（default_grade=false）。JSON 序列化异常 E0001「等级数据序列化失败」。"
							+ " 成功 data 为新建等级 9 键行（grade_id/is_default(0)/grade_name/discount/total_consumption/"
							+ " background_pic_url/external_id/created/updated 格式化时间，回显入参原样字符串）。"
							+ " Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.member_card_grade.create,"
							+ " version=2.0）签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：事务内 INSERT membercard_grade + zh-CN 多语言分表 upsert"
							+ "（grade_name/description/background_pic_url/grade_background）；无异步、无第三方调用。",
			operationId = "ecx.member_card_grade.create.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxMemberCardGradeCreateDataDoc；"
								+ " 业务失败：status=fail, code=E5001 未填写等级名称·未填写会员折扣·未填写升级条件·请求参数错误 /"
								+ " E5530 最多只能创建5个会员等级 / E0001 等级数据序列化失败，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxMemberCardGradeCreateFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxMemberCardGradeCreateSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "创建成功（discount=10 无折扣；时间/URL 为格式演示值）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"grade_id\":1,\"is_default\":0,\"grade_name\":\"普通会员\","
															+ "\"discount\":\"10\",\"total_consumption\":\"0\","
															+ "\"background_pic_url\":\"https://example.test/key.jpg\","
															+ "\"external_id\":\"1\",\"created\":\"2026-01-01 00:00:00\","
															+ "\"updated\":\"2026-01-01 00:00:00\"}}"),
									@ExampleObject(
											name = "fail-missing-name",
											summary = "grade_name 缺失/空串",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\","
															+ "\"message\":\"未填写等级名称\",\"data\":null}"),
									@ExampleObject(
											name = "fail-discount-range",
											summary = "discount 越界/非数字",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\",\"message\":\"请求参数错误\",\"data\":null}"),
									@ExampleObject(
											name = "fail-upper-limit",
											summary = "租户等级数量达上限",
											value =
													"{\"status\":\"fail\",\"code\":\"E5530\","
															+ "\"message\":\"最多只能创建5个会员等级\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.member_card_grade.create", name = "开放接口创建会员卡等级")
	public Map<String, Object> create(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "等级名称；必填非空（不 trim），否则 E5001「未填写等级名称」",
							required = true,
							example = "普通会员")
					@RequestParam(name = "grade_name", required = false)
					String gradeNameParam,
			@Parameter(
							description = "会员折扣（折）；必填，1≤值≤10（可小数）；10=无折扣；越界/非数字 E5001「请求参数错误」",
							required = true,
							example = "10")
					@RequestParam(name = "discount", required = false)
					String discountParam,
			@Parameter(
							description = "升级条件（累计消费门槛）；必填，数值 ≥0；非法 E5001「请求参数错误」",
							required = true,
							example = "0")
					@RequestParam(name = "total_consumption", required = false)
					String totalConsumptionParam,
			@Parameter(
							description = "会员卡背景图 URL；可选，trim 后落库，null 按空串（示例为格式演示值）",
							example = "https://example.test/key.jpg")
					@RequestParam(name = "background_pic_url", required = false)
					String backgroundPicUrlParam,
			@Parameter(
							description = "外部等级唯一标识；可选，trim 后落库，null 按空串，不校验唯一性",
							example = "1")
					@RequestParam(name = "external_id", required = false)
					String externalIdParam,
			@RequestBody(
							description =
									"可选 JSON body；5 键与 query 同名双通道（body 含键优先，值 null 视为未传）；"
											+ "前三键不 trim、后两键 trim；语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxMemberCardGradeCreateBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
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

	@Operation(
			summary = "开放接口批量设置会员卡等级（数云）",
			description =
					"按签名鉴权租户 company_id 全量覆盖会员卡等级（数云对接）："
							+ " grade_info 必填非空 JSON 数组（元素含 grade_id→external_id、grade_name、grade_level），"
							+ " 解析失败 E5001「请求参数错误」。全部行按 grade_level 升序排序（非法按 0），"
							+ " 最小行置默认等级（复用既有默认等级行），其余按 external_id 匹配既有非默认等级更新、无匹配新建；"
							+ " 每行 privileges 固定折扣 10（无折扣）、promotion_condition.total_consumption=grade_level。"
							+ " 本次未保留的既有等级将被删除；其中仍有会员在用的等级阻止整个请求"
							+ "（E5530「{external_id}-{等级名}[;…]，已有会员在使用，不能变更。」）。"
							+ " 成功 data 为保存后回读租户全量等级（zh-CN 分表覆盖）的数云 5 字段数组"
							+ "（grade_id 实为 external_id、grade_level 字符串、created/updated 格式化时间）；回读为空 data=null。"
							+ " grade_info 双通道（body 含键取原始值，可直接传数组）。返回由 Advice 包装（message 固定「成功」）。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.card_grade.batch_save, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：事务内 DELETE/INSERT/UPDATE membercard_grade + zh-CN 多语言分表写 +"
							+ " 删除旧等级卡券包触发绑定（card_package trigger）；无第三方调用、无异步。",
			operationId = "ecx.card_grade.batch_save.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 为全量等级数组（见 EcxCardGradeBatchSaveRowDoc）；"
								+ " 业务失败：status=fail, code=E5001 请求参数错误 / E5530 已有会员在使用不能变更 /"
								+ " E0001 要删除的等级不存在或已变更·要更新的等级不存在·等级保存失败，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxCardGradeBatchSaveFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxCardGradeBatchSaveSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "覆盖保存成功（返回租户全量等级）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":[{\"grade_id\":\"1\",\"grade_name\":\"普通会员\","
															+ "\"grade_level\":\"0\",\"created\":\"2020-09-13 20:26:40\","
															+ "\"updated\":\"2023-11-15 06:13:21\"}]}"),
									@ExampleObject(
											name = "fail-param",
											summary = "grade_info 缺失/非数组/空",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\",\"message\":\"请求参数错误\",\"data\":null}"),
									@ExampleObject(
											name = "fail-grade-in-use",
											summary = "待删除等级仍有会员在用",
											value =
													"{\"status\":\"fail\",\"code\":\"E5530\","
															+ "\"message\":\"1-普通会员，已有会员在使用，不能变更。\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.card_grade.batch_save", name = "开放接口批量设置会员卡等级（数云）")
	public Object batchSave(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description =
									"等级数组 JSON 字符串 [{grade_id, grade_name, grade_level}]；必填非空数组"
											+ "（body 同名键优先，可直接传数组）",
							required = true,
							example = "[{\"grade_id\":\"1\",\"grade_name\":\"普通会员\",\"grade_level\":0}]")
					@RequestParam(name = "grade_info", required = false)
					String gradeInfoParam,
			@RequestBody(
							description = "可选 JSON body；grade_info 与 query 同名双通道（取原始值）；元素键与覆盖语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxCardGradeBatchSaveBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
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
