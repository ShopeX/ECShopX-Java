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
import cn.shopex.ecshopx.common.openapi.OpenapiOrderListPort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxOrderListBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxOrderListSuccessEnvelopeDoc;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "开放接口-订单V1")
@OpenapiResponse
@RestController("openapiV1Order")
@RequestMapping("/api/openapi/internal/v1")
public class OrderController extends OpenapiBaseController {

	private final OpenapiOrderListPort orderListPort;

	public OrderController(OpenapiOrderListPort orderListPort) {
		this.orderListPort = orderListPort;
	}

	@Operation(
			summary = "开放接口会员订单列表",
			description =
					"按签名鉴权租户 company_id 查询会员全部订单统计与订单商品行展平列表："
							+ " count 为订单总数（非本页）、total_fee/order_avg 为元两位小数字符串；"
							+ " list 每商品行重复订单字段（12 键，行内 total_fee 为商品行金额），create_time 倒序，"
							+ " 无商品行的订单跳过；trade_no 取成功支付单号否则 \"-\"；"
							+ " NOTPAY 且过 auto_cancel_time 展示为 CANCEL。"
							+ " 分页：page PHP-falsy 固定前 10 条；page truthy 且 page_size 有效才 LIMIT，"
							+ " page_size 缺失/PHP-empty/非整数返回全部订单。"
							+ " 定位：mobile PHP-truthy 优先，否则 unionid（wechat 关联）；二选一必填；"
							+ " 查无会员报「会员信息获取失败」。"
							+ " message 随 count：>0「操作成功」，≤0「成功」（空结果 total_fee/order_avg 为数值 0）。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.order.list, version=1.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 参数双通道（body 含键优先）；body unionid 须为字符串。只读，无写库/异步/第三方副作用。",
			operationId = "ecx.order.list.v1")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态。"
								+ " 成功：status=success, code=E0000，message 为「操作成功」（count>0）或「成功」（count≤0），"
								+ " data 见 EcxOrderListDataDoc（空结果 count/total_fee/order_avg=0、list=[]）；"
								+ " 业务失败：status=fail, code=E0001（请填写unionid/请填写正确的手机号/"
								+ "unionid或者手机号必填/会员信息获取失败），data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxOrderListFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation = EcxOrderListSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功（订单商品行展平）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"操作成功\","
															+ "\"data\":{\"count\":1,\"total_fee\":\"99.00\",\"order_avg\":\"99.00\","
															+ "\"list\":[{\"order_id\":\"10001\",\"trade_no\":\"-\","
															+ "\"order_status\":\"WAIT_BUYER_CONFIRM\",\"total_fee\":\"99.00\","
															+ "\"create_time\":1764115200,\"item_name\":\"示例商品\","
															+ "\"item_id\":1001,\"item_bn\":\"A1\",\"price\":\"99.00\","
															+ "\"num\":1,\"item_spec_desc\":\"\",\"pic\":\"\"}]}}"),
									@ExampleObject(
											name = "success-empty",
											summary = "无订单（message 为「成功」，金额为数值 0）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"count\":0,\"total_fee\":0,\"order_avg\":0,\"list\":[]}}"),
									@ExampleObject(
											name = "fail-member-not-found",
											summary = "查无会员",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\","
															+ "\"message\":\"会员信息获取失败\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.order.list", name = "开放接口会员订单列表")
	public OpenapiEnvelope list(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "微信 unionid（wechat 关联）；body 中须为字符串；与 mobile 二选一（body 同名键优先）",
							example = "union-test")
					@RequestParam(name = "unionid", required = false)
					String unionidParam,
			@Parameter(
							description = "手机号；提供时须匹配 ^1[3-9]\\d{9}$；PHP-truthy 优先于 unionid（body 同名键优先）",
							example = "13800000000")
					@RequestParam(name = "mobile", required = false)
					String mobileParam,
			@Parameter(
							description = "页码；空/\"0\" 固定返回前 10 条；truthy 时按其分页（body 同名键优先）",
							example = "1")
					@RequestParam(name = "page", required = false)
					String pageParam,
			@Parameter(
							description = "每页条数；仅 page truthy 时生效；缺失/空/\"0\"/非整数返回全部订单（body 同名键优先）",
							example = "10")
					@RequestParam(name = "page_size", required = false)
					String pageSizeParam,
			@RequestBody(
							description =
									"可选 JSON body；字段与 query 同名双通道，body 含键时优先；unionid 须为字符串类型",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema = @Schema(implementation = EcxOrderListBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		boolean mobilePresent = OpenapiMemberQueryParams.isParamPresent(mobileParam, body, "mobile");
		String mobileRaw = OpenapiRequestParams.mergeString(mobileParam, body, "mobile");
		String unionidRaw = OpenapiRequestParams.mergeString(unionidParam, body, "unionid");
		boolean mobileTruthy = OpenapiMemberQueryParams.isPhpTruthy(mobileRaw);
		OpenapiOrderListParams.PageSpec pageSpec =
				OpenapiOrderListParams.resolve(pageParam, pageSizeParam, body);
		Map<String, Object> data =
				orderListPort.list(
						companyId,
						mobileParam,
						unionidParam,
						body,
						mobilePresent,
						mobileRaw,
						mobileTruthy,
						unionidRaw,
						pageSpec.page(),
						pageSpec.pageSize(),
						pageSpec.pageOverridden());
		String message = resolveListMessage(data);
		return new OpenapiEnvelope("success", "E0000", message, data);
	}

	private static String resolveListMessage(Map<String, Object> data) {
		Object count = data != null ? data.get("count") : null;
		long c = count instanceof Number n ? n.longValue() : 0L;
		return c <= 0L ? "成功" : "操作成功";
	}
}
