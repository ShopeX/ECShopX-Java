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
import cn.shopex.ecshopx.common.annotation.Activated;
import cn.shopex.ecshopx.common.openapi.OpenapiEnvelope;
import cn.shopex.ecshopx.common.openapi.OpenapiEnabledLogisticsListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderCancelPort;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderCancelReasonsPort;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderConfirmCancelPort;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderDetailPort;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderIncrListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderListV2Port;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderSoldListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiOrderWriteoffPort;
import cn.shopex.ecshopx.common.openapi.OpenapiShippingTemplatesListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiTradeListPort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiMemberQueryParams;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiOrderListParams;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc.EcxLogisticsEnabledGetBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc.EcxLogisticsEnabledGetErrorBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc.EcxLogisticsEnabledGetSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc.EcxOrderCancelBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc.EcxOrderCancelConfirmBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc.EcxOrderCancelConfirmSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc.EcxOrderCancelSuccessEnvelopeDoc;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "开放接口-订单V2")
@OpenapiResponse
@RestController("openapiV2Order")
@RequestMapping("/api/openapi/internal/v2")
public class OrderController extends OpenapiBaseController {

	private final OpenapiOrderWriteoffPort orderWriteoffPort;
	private final OpenapiOrderIncrListPort orderIncrListPort;
	private final OpenapiOrderSoldListPort orderSoldListPort;
	private final OpenapiOrderListV2Port orderListV2Port;
	private final OpenapiOrderDetailPort orderDetailPort;
	private final OpenapiOrderCancelReasonsPort cancelReasonsPort;
	private final OpenapiOrderCancelPort orderCancelPort;
	private final OpenapiEnabledLogisticsListPort enabledLogisticsListPort;
	private final OpenapiOrderConfirmCancelPort confirmCancelPort;
	private final OpenapiShippingTemplatesListPort shippingTemplatesListPort;
	private final OpenapiTradeListPort tradeListPort;

	public OrderController(
			OpenapiOrderWriteoffPort orderWriteoffPort,
			OpenapiOrderIncrListPort orderIncrListPort,
			OpenapiOrderSoldListPort orderSoldListPort,
			OpenapiOrderListV2Port orderListV2Port,
			OpenapiOrderDetailPort orderDetailPort,
			OpenapiOrderCancelReasonsPort cancelReasonsPort,
			OpenapiOrderCancelPort orderCancelPort,
			OpenapiEnabledLogisticsListPort enabledLogisticsListPort,
			OpenapiOrderConfirmCancelPort confirmCancelPort,
			OpenapiShippingTemplatesListPort shippingTemplatesListPort,
			OpenapiTradeListPort tradeListPort) {
		this.orderWriteoffPort = orderWriteoffPort;
		this.orderIncrListPort = orderIncrListPort;
		this.orderSoldListPort = orderSoldListPort;
		this.orderListV2Port = orderListV2Port;
		this.orderDetailPort = orderDetailPort;
		this.cancelReasonsPort = cancelReasonsPort;
		this.orderCancelPort = orderCancelPort;
		this.enabledLogisticsListPort = enabledLogisticsListPort;
		this.confirmCancelPort = confirmCancelPort;
		this.shippingTemplatesListPort = shippingTemplatesListPort;
		this.tradeListPort = tradeListPort;
	}

	@PostMapping(value = "/ecx.order.list", name = "开放接口会员订单列表")
	public OpenapiEnvelope list(
			HttpServletRequest request,
			@RequestParam(name = "unionid", required = false) String unionidParam,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		boolean mobilePresent = OpenapiMemberQueryParams.isParamPresent(mobileParam, body, "mobile");
		String mobileRaw = OpenapiRequestParams.mergeString(mobileParam, body, "mobile");
		String unionidRaw = OpenapiRequestParams.mergeString(unionidParam, body, "unionid");
		boolean mobileTruthy = OpenapiMemberQueryParams.isPhpTruthy(mobileRaw);
		OpenapiOrderListParams.PageSpec pageSpec =
				OpenapiOrderListParams.resolve(pageParam, pageSizeParam, body);
		Map<String, Object> data =
				orderListV2Port.list(
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

	@Activated(routeAlias = "order.writeoff.set")
	@PostMapping(value = "/ecx.order.writeoff", name = "开放接口自提订单核销")
	public Map<String, Object> orderWriteoff(
			HttpServletRequest request,
			@RequestParam(name = "order_id", required = false) String orderIdParam,
			@RequestParam(name = "pickupcode", required = false) String pickupcodeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		orderWriteoffPort.executeWriteoff(
				companyId,
				OpenapiRequestParams.originalString(orderIdParam, body, "order_id"),
				OpenapiRequestParams.originalString(pickupcodeParam, body, "pickupcode"));
		return Map.of("status", true);
	}

	@GetMapping(value = "/ecx.orders.incr.get", name = "开放接口增量订单搜索")
	public Object getIncrOrderList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "start_modified", required = false) String startModifiedParam,
			@RequestParam(name = "end_modified", required = false) String endModifiedParam,
			@RequestParam(name = "shop_code", required = false) String shopCodeParam,
			@RequestParam(name = "is_self", required = false) String isSelfParam,
			@RequestParam(name = "order_status", required = false) String orderStatusParam,
			@RequestParam(name = "pay_status", required = false) String payStatusParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2OrderIncrListParams.PageSpec pageSpec =
				OpenapiThirdApiV2OrderIncrListParams.resolve(pageParam, pageSizeParam, body);

		String mobileRaw = OpenapiRequestParams.originalString(mobileParam, body, "mobile");
		String shopCodeRaw = OpenapiRequestParams.originalString(shopCodeParam, body, "shop_code");
		String startModifiedRaw =
				OpenapiRequestParams.originalString(startModifiedParam, body, "start_modified");
		String endModifiedRaw = OpenapiRequestParams.originalString(endModifiedParam, body, "end_modified");
		String isSelfRaw = OpenapiRequestParams.originalString(isSelfParam, body, "is_self");
		String orderStatusRaw = OpenapiRequestParams.originalString(orderStatusParam, body, "order_status");
		String payStatusRaw = OpenapiRequestParams.originalString(payStatusParam, body, "pay_status");

		return orderIncrListPort.executeIncrOrderList(
				companyId,
				pageSpec.page(),
				pageSpec.pageSize(),
				OpenapiMemberQueryParams.isPhpTruthy(mobileRaw),
				mobileRaw,
				OpenapiMemberQueryParams.isPhpTruthy(shopCodeRaw),
				shopCodeRaw,
				OpenapiMemberQueryParams.isPhpTruthy(startModifiedRaw),
				startModifiedRaw,
				OpenapiMemberQueryParams.isPhpTruthy(endModifiedRaw),
				endModifiedRaw,
				OpenapiMemberQueryParams.isParamPresent(isSelfParam, body, "is_self"),
				isSelfRaw,
				OpenapiMemberQueryParams.isParamPresent(orderStatusParam, body, "order_status"),
				orderStatusRaw,
				OpenapiMemberQueryParams.isParamPresent(payStatusParam, body, "pay_status"),
				payStatusRaw);
	}

	@GetMapping(value = "/ecx.order.cancel.reasons.get", name = "开放接口订单取消原因列表")
	public Map<String, String> getCancelReasons(HttpServletRequest request) {
		return cancelReasonsPort.listAllAsStringKeyMap();
	}

	@GetMapping(value = "/ecx.trades.get", name = "开放接口交易单列表")
	public Map<String, Object> getTradeList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2TradeListParams.PageSpec pageSpec =
				OpenapiThirdApiV2TradeListParams.resolve(pageParam, pageSizeParam, body);
		return tradeListPort.list(companyId, pageSpec.page(), pageSpec.pageSize());
	}

	@GetMapping(value = "/ecx.shipping.templates.get", name = "开放接口运费模板列表")
	public Map<String, Object> getShippingtemplates(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2ShippingTemplatesListParams.PageSpec pageSpec =
				OpenapiThirdApiV2ShippingTemplatesListParams.resolve(pageParam, pageSizeParam, body);
		return shippingTemplatesListPort.list(companyId, pageSpec.page(), pageSpec.pageSize());
	}

	@Operation(
			summary = "开放接口开启物流公司列表",
			description =
					"按签名鉴权租户 company_id 查询已开启物流公司列表：distributor_id/supplier_id 与 query"
							+ " 同名双通道（body 含键优先），宽松整型解析：缺键/空白/null/非数字一律按 0，不报参数错误。"
							+ " 已开启判定：company_rel_logistics 按 (company_id, distributor_id, supplier_id) 等值"
							+ " 匹配（默认 0/0 总部维度），仅保留有开启记录的物流字典行（status=1 过滤）。"
							+ " data 恒为数组，首项固定 {corp_code:OTHER, corp_name:其他}；后续项仅 2 键"
							+ " corp_code/corp_name（字典 null 值转空串），按物流字典行序。"
							+ " 无已开启公司时仅返回 OTHER 一项。Advice 包装 message 固定「成功」。"
							+ " 错误面：直连 internal 无鉴权上下文 HTTP 200 E4004 未授权；"
							+ " company_id 超 Integer.MAX → HTTP 400 ApiResult{code:400,msg:参数错误}（非信封，"
							+ " GlobalExceptionHandler 兜底）；其他未处理异常（如 DB 故障）→ HTTP 500"
							+ " ApiResult{code:500,msg:服务器内部错误}。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.logistics.enabled.get,"
							+ " version=2.0）签名校验后转发。只读，无写库/异步/第三方副作用。",
			operationId = "ecx.logistics.enabled.get.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 为物流公司数组"
								+ "（首项恒 OTHER/其他，行 2 键见 EcxLogisticsEnabledRowDoc）；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxLogisticsEnabledGetFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxLogisticsEnabledGetSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "已开启顺丰/圆通（示例值取自物流字典种子 kuaidi.json）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":[{\"corp_code\":\"OTHER\",\"corp_name\":\"其他\"},"
															+ "{\"corp_code\":\"SF\",\"corp_name\":\"顺丰快递\"},"
															+ "{\"corp_code\":\"YTO\",\"corp_name\":\"圆通速递\"}]}"),
									@ExampleObject(
											name = "success-only-other",
											summary = "该维度无已开启公司（仅 OTHER 一项）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":[{\"corp_code\":\"OTHER\",\"corp_name\":\"其他\"}]}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								})),
		@ApiResponse(
				responseCode = "400",
				description =
						"HTTP 400，ApiResult 形态（非 OpenapiEnvelope）：company_id 超 Integer.MAX 触发"
								+ " BadRequestException「参数错误」，由 GlobalExceptionHandler 兜底。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxLogisticsEnabledGetErrorBodyDoc.class),
								examples =
										@ExampleObject(
												name = "bad-request",
												value = "{\"code\":400,\"msg\":\"参数错误\"}"))),
		@ApiResponse(
				responseCode = "500",
				description = "HTTP 500，ApiResult 形态（非 OpenapiEnvelope）：其他未处理异常（如 DB 故障）兜底。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxLogisticsEnabledGetErrorBodyDoc.class),
								examples =
										@ExampleObject(
												name = "internal-error",
												value = "{\"code\":500,\"msg\":\"服务器内部错误\"}")))
	})
	@GetMapping(value = "/ecx.logistics.enabled.get", name = "开放接口开启物流公司列表")
	public List<Map<String, Object>> getEnabledLogisticsList(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "店铺（分销商）ID；整数，默认 0（总部维度）；空白/null/非数字均按 0，不报错",
							example = "0")
					@RequestParam(name = "distributor_id", required = false)
					String distributorIdParam,
			@Parameter(
							description = "供应商 ID；整数，默认 0；解析规则同 distributor_id",
							example = "0")
					@RequestParam(name = "supplier_id", required = false)
					String supplierIdParam,
			@RequestBody(
							description = "可选 JSON body；2 键与 query 同名双通道（body 含键优先）；宽松解析语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxLogisticsEnabledGetBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		int distributorId = parseOptionalIntDefaultZero(distributorIdParam, body, "distributor_id");
		int supplierId = parseOptionalIntDefaultZero(supplierIdParam, body, "supplier_id");
		return enabledLogisticsListPort.listEnabled(companyId, distributorId, supplierId);
	}

	@Operation(
			summary = "开放接口确认订单取消审核",
			description =
					"鉴权：openapi 签名（公网网关校验后转发 internal；直连 internal 无鉴权上下文报 E4004）。"
							+ " 3 键 query 与 JSON body 同名双通道：body 含键优先（值 null 视为未传），取值不 trim，服务内 trim 后校验。"
							+ " 审核对象：该订单最新一条待审核（READY）售前退款单（refund_type=1 且未挂售后单，create_time 降序取 1）；"
							+ " order_type 固定 normal、operator_type=openapi、operator_id=0（不接受 order_type/refund_bn 入参）。"
							+ " cancel_handle=1 同意：退款单置 AUDIT_SUCCESS、取消单 progress=2，主单取消状态置 CANCEL/SUCCESS，"
							+ " 回补库存与优惠资源、返还下单积分、关闭分销佣金、订单分润置 0、发票置 cancel、写流程日志，"
							+ " PAYED 单同步分发聚水潭/旺店通 ERP 取消（action=pass_refund），事务提交后异步 SaasErp 订单同步与第三方交易更新"
							+ "（实际退款到账由退款分发异步完成，本接口返回不代表已到账）。"
							+ " cancel_handle=0 拒绝：退款单置 REFUSE、取消单 progress=4/refund_status=SHOP_CHECK_FAILS、"
							+ " 落 shop_reject_reason，主单取消状态置 FAILS、写流程日志；达达单（dada_status=5）重新下发达达并置 1；"
							+ " 事务提交后异步分发 SaasErp 退款取消与旺店通取消事件。"
							+ " 失败 HTTP 仍 200：E5001 参数缺失 / E5310 处理失败（下游文案经映射表转换，见 EcxOrderCancelConfirmFailEnvelopeDoc），data=null。",
			operationId = "ecx.order.cancel.confirm.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data.status 恒 true；"
								+ " 业务失败：status=fail，code=E5001 参数缺失 / E5310 处理失败"
								+ "（各分支文案见 EcxOrderCancelConfirmFailEnvelopeDoc），data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。",
				content =
						@Content(
								schema =
										@Schema(
												implementation =
														EcxOrderCancelConfirmSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "审核受理成功（同意或拒绝均此形状）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"status\":true}}"),
									@ExampleObject(
											name = "missing-order-id",
											summary = "缺少订单编号（E5001）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\",\"message\":\"订单编号必填\",\"data\":null}"),
									@ExampleObject(
											name = "missing-reject-reason",
											summary = "拒绝但未填原因（E5001）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\",\"message\":\"拒绝退款时原因必填\",\"data\":null}"),
									@ExampleObject(
											name = "refund-not-found",
											summary = "无待审退款单或订单号格式错误（E5310，映射文案）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5310\","
															+ "\"message\":\"没有查到退款单，无法同意取消订单\",\"data\":null}"),
									@ExampleObject(
											name = "refund-not-ready",
											summary = "拒绝时退款单非待审核（E5310，映射文案）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5310\","
															+ "\"message\":\"退款单状态不是待审核状态，无法拒绝\",\"data\":null}"),
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
			description = "JSON body（与 query 同名键双通道，body 含键优先）",
			content = @Content(schema = @Schema(implementation = EcxOrderCancelConfirmBodyDoc.class)))
	@PostMapping(value = "/ecx.order.cancel.confirm", name = "开放接口确认订单取消审核")
	public Map<String, Object> confirmCancel(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description =
									"订单 ID（与 body 同名键双通道，body 含键优先）；必填，trim 后 Long 解析，"
											+ "格式错误或无待审退款单 → E5310「没有查到退款单，无法同意取消订单」",
							example = "100")
					@RequestParam(name = "order_id", required = false)
					String orderIdParam,
			@Parameter(
							description =
									"是否同意取消（双通道同上）；必填，\"1\" 同意 / \"0\" 拒绝（trim 后须匹配 ^[01]$，"
											+ "否则 E5001「是否同意必填」）",
							example = "1")
					@RequestParam(name = "cancel_handle", required = false)
					String cancelHandleParam,
			@Parameter(
							description =
									"拒绝原因（双通道同上）；cancel_handle=0 时必填（trim 后非空，否则"
											+ " E5001「拒绝退款时原因必填」），落 shop_reject_reason；=1 时忽略",
							example = "档期已满")
					@RequestParam(name = "reject_reason", required = false)
					String rejectReasonParam,
			@Parameter(hidden = true) @FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		confirmCancelPort.executeConfirmCancel(
				companyId,
				OpenapiRequestParams.originalString(orderIdParam, body, "order_id"),
				OpenapiRequestParams.originalString(cancelHandleParam, body, "cancel_handle"),
				OpenapiRequestParams.originalString(rejectReasonParam, body, "reject_reason"));
		return Map.of("status", true);
	}

	@Operation(
			summary = "开放接口取消订单",
			description =
					"鉴权：openapi 签名（公网网关校验后转发 internal；直连 internal 无鉴权上下文报 E4004）。"
							+ " 3 键 query 与 JSON body 同名双通道：body 含键优先（值 null 视为未传），取值不 trim，服务内 trim 后解析。"
							+ " 仅 order_associations 中 order_type=normal（实体类）订单可取消，否则 E5310「实体类订单才能取消订单」。"
							+ " cancel_reason_id 限 1..12（OrderCancelReasonConfig 内置文案）；=12 时 cancel_reason 必填；"
							+ " cancel_reason 非空覆盖配置文案。"
							+ " 副作用（AdminNormalOrderFullCancelService，事务）：未支付（NOTPAY）直接写取消单并关单、"
							+ " 返还下单积分/购物金；已支付（PAYED/REVIEW_PASS 且发货 PENDING）写取消单并创建退款、"
							+ " 订单状态置 WAIT_PROCESS（取消审核流程，非立即退款完成），达达单校验骑手状态并联动取消。"
							+ " 发票状态置 cancel、写订单流程日志（operator_type=openapi、cancel_from=shop）；"
							+ " PAYED 时同步发布 SaasErp 退款事件与聚水潭/旺店通 ERP 取消分发，事务提交后异步分发退款与订单取消事件。"
							+ " 失败 HTTP 仍 200：E5001 参数缺失 / E5301 订单不存在 / E5310 处理失败（下游异常 message 透传），data=null。",
			operationId = "ecx.order.cancel.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data.status 恒 true；"
								+ " 业务失败：status=fail，code=E5001 参数缺失 / E5301 订单相应的明细不存在 /"
								+ " E5310 处理失败（各分支文案见 EcxOrderCancelFailEnvelopeDoc），data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。",
				content =
						@Content(
								schema = @Schema(implementation = EcxOrderCancelSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "取消受理成功",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"status\":true}}"),
									@ExampleObject(
											name = "missing-order-id",
											summary = "缺少订单编号（E5001）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\",\"message\":\"请填写订单编号\",\"data\":null}"),
									@ExampleObject(
											name = "order-not-found",
											summary = "订单不存在或 order_id 非整数（E5301）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5301\",\"message\":\"订单相应的明细不存在\",\"data\":null}"),
									@ExampleObject(
											name = "bad-reason-id",
											summary = "取消原因 ID 非法（E5310）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5310\",\"message\":\"请填写正确的取消原因ID\",\"data\":null}"),
									@ExampleObject(
											name = "status-not-cancelable",
											summary = "订单状态不可取消（E5310，下游文案透传）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5310\",\"message\":\"订单状态已不能申请取消\",\"data\":null}"),
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
			description = "JSON body（与 query 同名键双通道，body 含键优先）",
			content = @Content(schema = @Schema(implementation = EcxOrderCancelBodyDoc.class)))
	@PostMapping(value = "/ecx.order.cancel", name = "开放接口取消订单")
	public Map<String, Object> orderCancel(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description =
									"订单 ID（与 body 同名键双通道，body 含键优先）；必填，trim 后 Long 解析，"
											+ "失败或无此单 → E5301「订单相应的明细不存在」",
							example = "100")
					@RequestParam(name = "order_id", required = false)
					String orderIdParam,
			@Parameter(
							description =
									"取消原因 ID（双通道同上）；必填，1..12（内置文案），解析失败或超范围 →"
											+ " E5310「请填写正确的取消原因ID」；=12 时须传 cancel_reason",
							example = "1")
					@RequestParam(name = "cancel_reason_id", required = false)
					String cancelReasonIdParam,
			@Parameter(
							description =
									"自定义取消原因描述（双通道同上）；cancel_reason_id=12 时必填；trim 后 >255 字符报"
											+ " E5001「请填写取消原因描述」；非空覆盖内置文案",
							example = "客户电话要求取消")
					@RequestParam(name = "cancel_reason", required = false)
					String cancelReasonParam,
			@Parameter(hidden = true) @FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		orderCancelPort.executeCancel(
				companyId,
				OpenapiRequestParams.originalString(orderIdParam, body, "order_id"),
				OpenapiRequestParams.originalString(cancelReasonIdParam, body, "cancel_reason_id"),
				OpenapiRequestParams.originalString(cancelReasonParam, body, "cancel_reason"));
		return Map.of("status", true);
	}

	@GetMapping(value = "/ecx.order.get", name = "开放接口订单详情")
	public Map<String, Object> getDetail(
			HttpServletRequest request,
			@RequestParam(name = "order_id", required = false) String orderIdParam,
			@RequestParam(name = "ziti_code", required = false) String zitiCodeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		return orderDetailPort.getOrderDetail(
				companyId,
				OpenapiRequestParams.originalString(orderIdParam, body, "order_id"),
				OpenapiRequestParams.originalString(zitiCodeParam, body, "ziti_code"));
	}

	@GetMapping(value = "/ecx.orders.sold.get", name = "开放接口订单搜索")
	public Object getList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "time_begin", required = false) String timeBeginParam,
			@RequestParam(name = "time_end", required = false) String timeEndParam,
			@RequestParam(name = "shop_code", required = false) String shopCodeParam,
			@RequestParam(name = "is_self", required = false) String isSelfParam,
			@RequestParam(name = "order_status", required = false) String orderStatusParam,
			@RequestParam(name = "pay_status", required = false) String payStatusParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2OrderSoldListParams.PageSpec pageSpec =
				OpenapiThirdApiV2OrderSoldListParams.resolve(pageParam, pageSizeParam, body);

		String mobileRaw = OpenapiRequestParams.originalString(mobileParam, body, "mobile");
		String timeBeginRaw = OpenapiRequestParams.originalString(timeBeginParam, body, "time_begin");
		String timeEndRaw = OpenapiRequestParams.originalString(timeEndParam, body, "time_end");
		String shopCodeRaw = OpenapiRequestParams.originalString(shopCodeParam, body, "shop_code");
		String isSelfRaw = OpenapiRequestParams.originalString(isSelfParam, body, "is_self");
		String orderStatusRaw = OpenapiRequestParams.originalString(orderStatusParam, body, "order_status");
		String payStatusRaw = OpenapiRequestParams.originalString(payStatusParam, body, "pay_status");

		return orderSoldListPort.executeSoldOrderList(
				companyId,
				pageSpec.page(),
				pageSpec.pageSize(),
				OpenapiMemberQueryParams.isPhpTruthy(mobileRaw),
				mobileRaw,
				OpenapiMemberQueryParams.isPhpTruthy(timeBeginRaw),
				timeBeginRaw,
				OpenapiMemberQueryParams.isPhpTruthy(timeEndRaw),
				timeEndRaw,
				OpenapiMemberQueryParams.isPhpTruthy(shopCodeRaw),
				shopCodeRaw,
				OpenapiMemberQueryParams.isParamPresent(isSelfParam, body, "is_self"),
				isSelfRaw,
				OpenapiMemberQueryParams.isParamPresent(orderStatusParam, body, "order_status"),
				orderStatusRaw,
				OpenapiMemberQueryParams.isParamPresent(payStatusParam, body, "pay_status"),
				payStatusRaw);
	}

	private static String resolveListMessage(Map<String, Object> data) {
		Object count = data != null ? data.get("count") : null;
		long c = count instanceof Number n ? n.longValue() : 0L;
		return c <= 0L ? "成功" : "操作成功";
	}

	/**
	 * 可选整型参数：Query/Body 合并（Body 含键优先，{@link OpenapiRequestParams#originalString}）。
	 * 键缺失 → 0；存在但 blank/null → 0；存在且可解析为 int → 解析值；非法非空字符串 → 0。
	 */
	private static int parseOptionalIntDefaultZero(
			String queryParam, Map<String, Object> body, String key) {
		boolean presentInBody = body != null && body.containsKey(key);
		boolean presentInQuery = queryParam != null;
		if (!presentInBody && !presentInQuery) {
			return 0;
		}
		String raw = OpenapiRequestParams.originalString(queryParam, body, key);
		if (raw == null || raw.isBlank()) {
			return 0;
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
