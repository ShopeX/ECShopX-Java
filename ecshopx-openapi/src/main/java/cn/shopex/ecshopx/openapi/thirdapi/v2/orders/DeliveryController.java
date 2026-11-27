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

package cn.shopex.ecshopx.openapi.thirdapi.v2.orders;

import cn.shopex.ecshopx.common.annotation.OpenapiResponse;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderDeliverPort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc.EcxOrderDeliverBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc.EcxOrderDeliverSuccessEnvelopeDoc;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OpenAPI v2 handler scaffold — methods added during migration. */
@Tag(name = "开放接口-订单发货V2")
@OpenapiResponse
@RestController("openapiV2Delivery")
@RequestMapping("/api/openapi/internal/v2")
public class DeliveryController extends OpenapiBaseController {

	private final OpenapiOrderDeliverPort orderDeliverPort;

	public DeliveryController(OpenapiOrderDeliverPort orderDeliverPort) {
		this.orderDeliverPort = orderDeliverPort;
	}

	@GetMapping(value = "/ecx.order.deliver.get", name = "开放接口订单发货单列表")
	public Map<String, Object> getOrderDeliveryList(
			HttpServletRequest request,
			@RequestParam(name = "order_id", required = false) String orderIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String orderIdRaw = OpenapiRequestParams.mergeString(orderIdParam, body, "order_id");
		return orderDeliverPort.getOrderDeliveryListV2(companyId, orderIdRaw);
	}

	@Operation(
			summary = "开放接口订单发货",
			description =
					"鉴权：openapi 签名（公网网关校验后转发 internal；直连 internal 无鉴权上下文报 E4004）。"
							+ " 4 键 query 与 JSON body 同名双通道：body 含键优先（null/空白串视为未传），mergeString trim；"
							+ " item_info 在 body 内可直接传 JSON 数组（自动序列化为字符串）。"
							+ " 按 sku_id 匹配订单待发行（delivery_status=PENDING）的 item_bn，qty 为本次发货数量（正整数）；"
							+ " delivery_type 固定 sep 拆单、operator_type=system、operator_id=0、supplier_id=0（供应商商品行不可发）。"
							+ " 副作用（AdminNormalOrderDeliveryCoreService，事务）：写发货单 orders_delivery 与明细、"
							+ " 累加订单行已发数量（发完置行 DONE）、累加可售后数量；全部发完订单与关联表置 DONE/WAIT_BUYER_CONFIRM"
							+ " 并写自动完成时间（平台 order_finish_time 设置，默认 7 天），否则 PARTAIL（部分发货）；"
							+ " normal_orders 回写 delivery_corp/delivery_code（关联表不回写——响应中这两键为既有值）；"
							+ " 写订单流程日志、微信小程序发货信息上报、发货通知；事务提交后异步分发订单发货事件、"
							+ " SaasErp 订单同步与第三方交易更新。"
							+ " 失败 HTTP 仍 200：E5002 item_info 格式错误 / E5001 参数缺失 / E5301 订单不存在 /"
							+ " E5312 重复发货 / E5310 处理失败（文案见 EcxOrderDeliverFailEnvelopeDoc），data=null。",
			operationId = "ecx.order.deliver.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 为订单关联数据 35 固定键"
								+ "（见 EcxOrderDeliverDataDoc；delivery_corp/delivery_code 为关联表既有值非入参回显）；"
								+ " 业务失败：status=fail，code=E5002/E5001/E5301/E5312/E5310，data=null，"
								+ "各分支文案见 EcxOrderDeliverFailEnvelopeDoc；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。",
				content =
						@Content(
								schema = @Schema(implementation = EcxOrderDeliverSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "全部发完（时间为格式演示值；SKU/名称示例为格式演示值）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\",\"data\":{"
															+ "\"order_id\":100,\"authorizer_appid\":null,\"wxa_appid\":null,"
															+ "\"title\":null,\"total_fee\":8000,\"company_id\":1,\"shop_id\":null,"
															+ "\"store_name\":null,\"user_id\":50,\"salesman_id\":0,"
															+ "\"promoter_user_id\":null,\"promoter_shop_id\":null,\"source_id\":null,"
															+ "\"monitor_id\":null,\"mobile\":null,\"order_class\":\"normal\","
															+ "\"order_type\":\"normal\",\"order_status\":\"WAIT_BUYER_CONFIRM\","
															+ "\"create_time\":1767225600,\"update_time\":1767225600,"
															+ "\"is_distribution\":false,\"total_rebate\":0,"
															+ "\"delivery_corp\":null,\"delivery_code\":null,"
															+ "\"member_discount\":0,\"coupon_discount\":0,"
															+ "\"coupon_discount_desc\":[],\"member_discount_desc\":[],"
															+ "\"delivery_status\":\"DONE\",\"delivery_time\":1767225600,"
															+ "\"cancel_status\":\"NO_APPLY_CANCEL\",\"end_time\":null,"
															+ "\"fee_type\":\"CNY\",\"fee_rate\":1.0,\"fee_symbol\":\"￥\"}}"),
									@ExampleObject(
											name = "fail-item-info-format",
											summary = "item_info 非法 JSON 或空数组（E5002）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5002\",\"message\":\"发货商品信息格式错误\",\"data\":null}"),
									@ExampleObject(
											name = "fail-missing-lc-code",
											summary = "缺少快递公司编码（E5001）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\",\"message\":\"快递公司编码必填\",\"data\":null}"),
									@ExampleObject(
											name = "fail-order-not-found",
											summary = "订单不存在或 order_id 非整数（E5301）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5301\",\"message\":\"订单相应的明细不存在\",\"data\":null}"),
									@ExampleObject(
											name = "fail-already-delivered",
											summary = "订单已全部发货（E5312）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5312\",\"message\":\"订单已发货，请勿重复发货\",\"data\":null}"),
									@ExampleObject(
											name = "fail-not-pay",
											summary = "订单未支付（E5310，下游文案透传）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5310\",\"message\":\"订单未支付，不能发货\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								})),
		@ApiResponse(
				responseCode = "500",
				description = "未处理异常兜底（GlobalExceptionHandler，ApiResult 非本信封）",
				content =
						@Content(
								examples =
										@ExampleObject(
												name = "internal-error",
												value = "{\"code\":500,\"msg\":\"服务器内部错误\"}")))
	})
	@RequestBody(
			required = false,
			description = "JSON body（与 query 同名键双通道，body 含键优先；item_info 可直接传 JSON 数组）",
			content = @Content(schema = @Schema(implementation = EcxOrderDeliverBodyDoc.class)))
	@PostMapping(value = "/ecx.order.deliver", name = "开放接口订单发货")
	public Map<String, Object> createDelivery(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description =
									"订单 ID（与 body 同名键双通道，body 含键优先）；必填，Long 解析失败或无此单 →"
											+ " E5301「订单相应的明细不存在」；已发货 → E5312",
							example = "100")
					@RequestParam(name = "order_id", required = false)
					String orderIdParam,
			@Parameter(
							description =
									"发货商品信息（双通道同上）；必填，JSON 数组字符串，元素 3 键 name/sku_id/qty 均必填；"
											+ "sku_id 匹配订单行 item_bn，qty 正整数；格式错误 → E5002",
							example = "[{\"name\":\"Item-A\",\"sku_id\":\"SKU-A\",\"qty\":1}]")
					@RequestParam(name = "item_info", required = false)
					String itemInfoParam,
			@Parameter(
							description =
									"快递公司编码（双通道同上）；必填；kuaidi100 启用时按 kuaidi_code 忽略大小写匹配，"
											+ "否则按 corp_code 匹配，未匹配显示「其他」",
							example = "SF")
					@RequestParam(name = "lc_code", required = false)
					String lcCodeParam,
			@Parameter(description = "快递单号（双通道同上）；必填", example = "SFTRACK001")
					@RequestParam(name = "l_code", required = false)
					String lCodeParam,
			@Parameter(hidden = true) @FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String orderIdRaw = OpenapiRequestParams.mergeString(orderIdParam, body, "order_id");
		String itemInfoRaw = OpenapiRequestParams.mergeString(itemInfoParam, body, "item_info");
		String lcCode = OpenapiRequestParams.mergeString(lcCodeParam, body, "lc_code");
		String lCode = OpenapiRequestParams.mergeString(lCodeParam, body, "l_code");
		return orderDeliverPort.createDeliveryV2(companyId, orderIdRaw, itemInfoRaw, lcCode, lCode);
	}
}
