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
import cn.shopex.ecshopx.common.openapi.OpenapiOrderDeliverPort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxOrderDeliverBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxOrderDeliverSuccessEnvelopeDoc;
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

/** OpenAPI v1 handler scaffold — methods added during migration. */
@Tag(name = "开放接口-订单发货V1")
@OpenapiResponse
@RestController("openapiV1Delivery")
@RequestMapping("/api/openapi/internal/v1")
public class DeliveryController extends OpenapiBaseController {

	private final OpenapiOrderDeliverPort orderDeliverPort;

	public DeliveryController(OpenapiOrderDeliverPort orderDeliverPort) {
		this.orderDeliverPort = orderDeliverPort;
	}

	@Operation(
			summary = "开放接口订单发货",
			description =
					"按签名鉴权租户 company_id 对订单执行拆单发货（delivery_type=sep，事务内）："
							+ " item_info JSON 数组按 sku_id 匹配订单行 item_bn、qty 为发货数量，"
							+ " 仅处理 delivery_status=PENDING 的行；创建发货单（orders_delivery + items），"
							+ " 更新订单行/主单/关联表发货状态（部分发货 PARTAIL，全部发货 DONE 且"
							+ " order_status=WAIT_BUYER_CONFIRM、写自动收货时间），返回发货后订单关联快照（35 键）。"
							+ " 已发货订单（DONE）报「订单已发货，请勿重复发货」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.order.deliver, version=1.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 四参数双通道（body 含键优先）且业务必填。"
							+ " 副作用：写库发货单/订单行/主单/关联表；发布订单流程日志与微信发货信息异步事件；"
							+ " 无第三方同步调用。",
			operationId = "ecx.order.deliver.v1")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态。"
								+ " 成功：status=success, code=E0000, message=操作成功，data 为订单关联快照"
								+ "（35 键，见 EcxOrderDeliverDataDoc）；"
								+ " 业务失败：status=fail, code=E0001（订单号必填/发货商品信息必填/快递公司编码必填/"
								+ "快递单编码必填/此订单不存在/订单已发货，请勿重复发货/获取订单信息失败/操作失败），data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxOrderDeliverFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxOrderDeliverSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "发货成功（字段节选，完整 35 键见 schema）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"操作成功\","
															+ "\"data\":{\"order_id\":10001,\"company_id\":100,"
															+ "\"order_class\":\"normal\",\"order_status\":\"WAIT_BUYER_CONFIRM\","
															+ "\"delivery_corp\":\"SF\",\"delivery_code\":\"SFTRACK001\","
															+ "\"delivery_status\":\"DONE\",\"delivery_time\":1764115200,"
															+ "\"cancel_status\":\"NO_APPLY_CANCEL\",\"total_fee\":9900,"
															+ "\"member_discount\":0,\"coupon_discount\":0,"
															+ "\"coupon_discount_desc\":[],\"member_discount_desc\":[],"
															+ "\"is_distribution\":false,\"total_rebate\":0,"
															+ "\"fee_type\":\"CNY\",\"fee_rate\":1.0,\"fee_symbol\":\"￥\","
															+ "\"salesman_id\":0}}"),
									@ExampleObject(
											name = "fail-already-delivered",
											summary = "重复发货",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\","
															+ "\"message\":\"订单已发货，请勿重复发货\",\"data\":null}"),
									@ExampleObject(
											name = "fail-order-not-found",
											summary = "订单不存在",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\",\"message\":\"此订单不存在\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.order.deliver", name = "开放接口订单发货")
	public OpenapiEnvelope createDelivery(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "订单号；必填；非整数/查无报「此订单不存在」（body 同名键优先）",
							required = true,
							example = "10001")
					@RequestParam(name = "order_id", required = false)
					String orderIdParam,
			@Parameter(
							description =
									"发货商品信息 JSON 数组字符串 [{sku_id, qty}]；sku_id 匹配订单行 item_bn；"
											+ "必填；仅发 PENDING 行（body 同名键优先）",
							required = true,
							example = "[{\"sku_id\":\"A1\",\"qty\":1}]")
					@RequestParam(name = "item_info", required = false)
					String itemInfoParam,
			@Parameter(description = "快递公司编码（如 SF/YTO）；必填（body 同名键优先）", required = true, example = "SF")
					@RequestParam(name = "lc_code", required = false)
					String lcCodeParam,
			@Parameter(description = "快递单号；必填（body 同名键优先）", required = true, example = "SFTRACK001")
					@RequestParam(name = "l_code", required = false)
					String lCodeParam,
			@RequestBody(
							description =
									"可选 JSON body；字段与 query 同名双通道，body 含键时优先"
											+ "（null/空白视为未提供且不回退 query）",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema = @Schema(implementation = EcxOrderDeliverBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String orderId =
				OpenapiRequestParams.mergeRequiredString(orderIdParam, body, "order_id", "订单号必填");
		String itemInfoRaw =
				OpenapiRequestParams.mergeRequiredString(
						itemInfoParam, body, "item_info", "发货商品信息必填");
		String lcCode =
				OpenapiRequestParams.mergeRequiredString(lcCodeParam, body, "lc_code", "快递公司编码必填");
		String lCode =
				OpenapiRequestParams.mergeRequiredString(lCodeParam, body, "l_code", "快递单编码必填");
		Map<String, Object> data =
				orderDeliverPort.createDelivery(companyId, orderId, itemInfoRaw, lcCode, lCode);
		return new OpenapiEnvelope("success", "E0000", "操作成功", data);
	}
}
