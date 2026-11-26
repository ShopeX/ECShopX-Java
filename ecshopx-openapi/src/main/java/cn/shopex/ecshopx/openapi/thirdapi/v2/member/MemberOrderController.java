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
import cn.shopex.ecshopx.common.openapi.OpenapiMemberOrderV2ListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberPointOrderV2ListPort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiMemberQueryParams;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberOrderListBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberOrderListSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberPointOrderListBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberPointOrderListSuccessEnvelopeDoc;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "开放接口-会员订单V2")
@OpenapiResponse
@RestController("openapiV2MemberOrder")
@RequestMapping("/api/openapi/internal/v2")
public class MemberOrderController extends OpenapiBaseController {

	private final OpenapiMemberOrderV2ListPort memberOrderV2ListPort;
	private final OpenapiMemberPointOrderV2ListPort memberPointOrderV2ListPort;

	public MemberOrderController(
			OpenapiMemberOrderV2ListPort memberOrderV2ListPort,
			OpenapiMemberPointOrderV2ListPort memberPointOrderV2ListPort) {
		this.memberOrderV2ListPort = memberOrderV2ListPort;
		this.memberPointOrderV2ListPort = memberPointOrderV2ListPort;
	}

	@Operation(
			summary = "开放接口查询会员订单列表",
			description =
					"按签名鉴权租户 company_id 分页查询会员订单（normal_orders，order_id 降序）："
							+ " 5 键双通道（body 含键优先、值 null 视为未传、不 trim；三过滤键另按存在性判定）。"
							+ " mobile 键存在且非空白时按固定加密等值定位会员——命中按 user_id 过滤，"
							+ " **查无/空白按 user_id=0 过滤（通常空结果，不报错）**。"
							+ " start_date/end_date 键存在且非空非\"0\"时须 yyyy-MM-dd HH:mm:ss（系统时区，"
							+ " create_time 区间），纯数字时间戳或其他格式报 E4002「时间格式有误」；"
							+ " **start_date 缺省按当年 1 月 1 日零点**（仅返回今年以来订单），end_date 缺省不设上界。"
							+ " page 默认 1、page_size 默认 20（PHP 式解析），原始 page_size ≥500 报"
							+ " E5002「每页的大小最大为500！」，钳制后 page≥1、page_size∈[20,499]。"
							+ " data：total_count/is_last_page(0|1)/pager{page,page_size}/list；行 13 键："
							+ " order_id 字符串、create_time 格式化时间、total_fee 元 2 位小数字符串（分 HALF_UP 除 100）、"
							+ " order_class/order_type/type(0 普通 1 跨境)/order_status/ziti_status/cancel_status/"
							+ " pay_status/audit_status（枚举见行 schema）、distributor_id(整型)/distributor_name 回查。"
							+ " Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.member_order.list,"
							+ " version=2.0）签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.member_order.list.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxMemberOrderListDataDoc；"
								+ " 业务失败：status=fail, code=E4002 时间格式有误 / E5002 每页的大小最大为500！，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxMemberOrderListFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxMemberOrderListSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功（枚举为库默认值；时间为格式演示值）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":1,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},"
															+ "\"list\":[{\"order_id\":\"100\",\"create_time\":\"2026-01-01 00:00:00\","
															+ "\"total_fee\":\"0.00\",\"order_class\":\"normal\","
															+ "\"order_type\":\"normal\",\"type\":0,\"order_status\":\"NOTPAY\","
															+ "\"ziti_status\":\"NOTZITI\",\"cancel_status\":\"NO_APPLY_CANCEL\","
															+ "\"pay_status\":\"NOTPAY\",\"audit_status\":\"processing\","
															+ "\"distributor_id\":0,\"distributor_name\":\"\"}]}}"),
									@ExampleObject(
											name = "success-empty",
											summary = "无匹配订单（含 mobile 查无按 user_id=0 过滤场景）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":0,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},\"list\":[]}}"),
									@ExampleObject(
											name = "fail-time-format",
											summary = "start_date/end_date 传时间戳或非法格式",
											value =
													"{\"status\":\"fail\",\"code\":\"E4002\",\"message\":\"时间格式有误\",\"data\":null}"),
									@ExampleObject(
											name = "fail-page-size",
											summary = "page_size 原始解析值 ≥500",
											value =
													"{\"status\":\"fail\",\"code\":\"E5002\","
															+ "\"message\":\"每页的大小最大为500！\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@GetMapping(value = "/ecx.member_order.list", name = "开放接口查询会员订单列表")
	public Map<String, Object> list(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "会员手机号过滤；命中按 user_id 过滤；查无/空白按 user_id=0 过滤（通常空结果，不报错）",
							example = "13800138000")
					@RequestParam(name = "mobile", required = false)
					String mobileParam,
			@Parameter(
							description = "起始时间 yyyy-MM-dd HH:mm:ss；纯数字/非法格式 E4002；缺省按当年 1 月 1 日零点（格式演示值）",
							example = "2026-01-01 00:00:00")
					@RequestParam(name = "start_date", required = false)
					String startDateParam,
			@Parameter(
							description = "截止时间 yyyy-MM-dd HH:mm:ss；解析同 start_date；缺省不设上界（格式演示值）",
							example = "2026-12-31 23:59:59")
					@RequestParam(name = "end_date", required = false)
					String endDateParam,
			@Parameter(
							description = "页码，默认 1；PHP 式解析后 max(…,1)",
							example = "1")
					@RequestParam(name = "page", required = false)
					String pageParam,
			@Parameter(
							description = "每页数量，默认 20；原始解析值 ≥500 报 E5002；钳制后生效 [20,499]",
							example = "20")
					@RequestParam(name = "page_size", required = false)
					String pageSizeParam,
			@RequestBody(
							description = "可选 JSON body；5 键与 query 同名双通道（body 含键优先，值 null 视为未传）；语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxMemberOrderListBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);

		OpenapiThirdApiV2MemberListParams.PageSpec pageSpec =
				OpenapiThirdApiV2MemberListParams.resolve(pageParam, pageSizeParam, body);

		boolean mobilePresent = OpenapiMemberQueryParams.isParamPresent(mobileParam, body, "mobile");
		String mobileRaw = OpenapiRequestParams.originalString(mobileParam, body, "mobile");
		boolean startDatePresent =
				OpenapiMemberQueryParams.isParamPresent(startDateParam, body, "start_date");
		String startDateRaw = OpenapiRequestParams.originalString(startDateParam, body, "start_date");
		boolean endDatePresent =
				OpenapiMemberQueryParams.isParamPresent(endDateParam, body, "end_date");
		String endDateRaw = OpenapiRequestParams.originalString(endDateParam, body, "end_date");

		return memberOrderV2ListPort.listMemberOrders(
				companyId,
				mobilePresent,
				mobileRaw,
				startDatePresent,
				startDateRaw,
				endDatePresent,
				endDateRaw,
				pageSpec.page(),
				pageSpec.pageSize());
	}

	@Operation(
			summary = "开放接口查询会员订单积分列表",
			description =
					"按签名鉴权租户 company_id 分页查询会员订单积分（normal_orders，order_id 降序）："
							+ " 4 键双通道（body 含键优先、值 null 视为未传、不 trim；mobile/order_id 另按存在性判定）。"
							+ " mobile 键存在且非空白时按固定加密等值定位会员——命中按 user_id 过滤，"
							+ " **查无/空白按 user_id=0 过滤（通常空结果，不报错）**。"
							+ " order_id 键存在且非 null 非空串时 trim+parseLong 等值过滤"
							+ "（\"0\" 参与；非数字按 -1 恒空，不报错）。"
							+ " **create_time 下界固定当年 1 月 1 日零点（无时间入参，不可放宽）**。"
							+ " page 默认 1、page_size 默认 20（PHP 式解析），原始 page_size ≥500 报"
							+ " E5002「每页的大小最大为500！」，钳制后 page≥1、page_size∈[20,499]。"
							+ " data：total_count/is_last_page(0|1)/pager{page,page_size}/list；行 10 键："
							+ " order_id 字符串、mobile 订单快照解密、create_time 格式化时间、"
							+ " get_points/bonus_points/extra_points/point_use 整型、point_fee 元 2 位小数字符串"
							+ "（null 为空串）、total_points 三项获取合计、username 回查解密（user_id 不透出）。"
							+ " Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.member_point_order.list,"
							+ " version=2.0）签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.member_point_order.list.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxMemberPointOrderListDataDoc；"
								+ " 业务失败：status=fail, code=E5002 每页的大小最大为500！，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxMemberPointOrderListFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxMemberPointOrderListSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功（积分为库默认 0；时间为格式演示值）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":1,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},"
															+ "\"list\":[{\"order_id\":\"100\",\"mobile\":\"13800138000\","
															+ "\"create_time\":\"2026-01-01 00:00:00\",\"get_points\":0,"
															+ "\"bonus_points\":0,\"extra_points\":0,\"point_use\":0,"
															+ "\"point_fee\":\"0.00\",\"total_points\":0,\"username\":\"张三\"}]}}"),
									@ExampleObject(
											name = "success-empty",
											summary = "无匹配订单（含 mobile 查无/order_id 非数字场景）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":0,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},\"list\":[]}}"),
									@ExampleObject(
											name = "fail-page-size",
											summary = "page_size 原始解析值 ≥500",
											value =
													"{\"status\":\"fail\",\"code\":\"E5002\","
															+ "\"message\":\"每页的大小最大为500！\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@GetMapping(value = "/ecx.member_point_order.list", name = "开放接口查询会员订单积分列表")
	public Map<String, Object> pointList(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "会员手机号过滤；命中按 user_id 过滤；查无/空白按 user_id=0 过滤（通常空结果，不报错）",
							example = "13800138000")
					@RequestParam(name = "mobile", required = false)
					String mobileParam,
			@Parameter(
							description = "订单号过滤；trim+parseLong 等值（\"0\" 参与；非数字按 -1 恒空不报错）；null/空串忽略",
							example = "100")
					@RequestParam(name = "order_id", required = false)
					String orderIdParam,
			@Parameter(
							description = "页码，默认 1；PHP 式解析后 max(…,1)",
							example = "1")
					@RequestParam(name = "page", required = false)
					String pageParam,
			@Parameter(
							description = "每页数量，默认 20；原始解析值 ≥500 报 E5002；钳制后生效 [20,499]",
							example = "20")
					@RequestParam(name = "page_size", required = false)
					String pageSizeParam,
			@RequestBody(
							description = "可选 JSON body；4 键与 query 同名双通道（body 含键优先，值 null 视为未传）；语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxMemberPointOrderListBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);

		OpenapiThirdApiV2MemberListParams.PageSpec pageSpec =
				OpenapiThirdApiV2MemberListParams.resolve(pageParam, pageSizeParam, body);

		boolean mobilePresent = OpenapiMemberQueryParams.isParamPresent(mobileParam, body, "mobile");
		String mobileRaw = OpenapiRequestParams.originalString(mobileParam, body, "mobile");
		boolean orderIdPresent =
				OpenapiMemberQueryParams.isParamPresent(orderIdParam, body, "order_id");
		String orderIdRaw = OpenapiRequestParams.originalString(orderIdParam, body, "order_id");

		return memberPointOrderV2ListPort.listMemberPointOrders(
				companyId,
				mobilePresent,
				mobileRaw,
				orderIdPresent,
				orderIdRaw,
				pageSpec.page(),
				pageSpec.pageSize());
	}
}
