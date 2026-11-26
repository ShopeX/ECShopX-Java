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
