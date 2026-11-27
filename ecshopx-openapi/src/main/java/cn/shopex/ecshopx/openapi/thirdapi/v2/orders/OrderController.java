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
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc.EcxOrderCancelReasonsGetSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc.EcxOrderCancelSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc.EcxOrderGetBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc.EcxOrderGetSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc.EcxOrderListBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc.EcxOrderListSuccessEnvelopeDoc;
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

	@Operation(
			summary = "开放接口会员订单列表",
			description =
					"鉴权：openapi 签名（公网网关校验后转发 internal；直连 internal 无鉴权上下文报 E4004）。"
							+ " 4 键 query 与 JSON body 同名双通道：body 含键优先（null/空白串视为未传），mergeString trim。"
							+ " mobile 与 unionid 至少一个非空（PHP empty 语义）；mobile 优先按手机号查会员，"
							+ " 否则按 unionid（user_type=wechat）查会员；查无会员 → E0001「会员信息获取失败」。"
							+ " 统计（count/total_fee/order_avg）覆盖该会员全部订单（含未支付/已取消，不随分页变化）；"
							+ " list 为分页内订单按商品行展平（create_time 降序，无商品行的订单跳过），"
							+ " 行级 total_fee 为商品行金额（覆盖订单级同名键）。金额键为元字符串（2 位小数）。"
							+ " 分页：page 未传固定 LIMIT 10；page 已传且 page_size 缺失/PHP empty/解析失败 → 返回全部；"
							+ " page_size>0 才 LIMIT/OFFSET，无上下限校验（page≤0 → 负 OFFSET SQL 错误 → HTTP 500）。"
							+ " 信封显式构造：message 按 count 判定（≤0「成功」，>0「操作成功」）。副作用：无（只读）。"
							+ " 失败 HTTP 仍 200：E0001（请填写unionid/请填写正确的手机号/unionid或者手机号必填/会员信息获取失败），data=null。",
			operationId = "ecx.order.list.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（显式构造）。"
								+ " 成功：status=success, code=E0000，message=count>0「操作成功」/否则「成功」，"
								+ " data 见 EcxOrderListDataDoc（4 固定键，行 12 键）；"
								+ " 业务失败：status=fail, code=E0001，data=null，见 EcxOrderListFailEnvelopeDoc；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。",
				content =
						@Content(
								schema = @Schema(implementation = EcxOrderListSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "有订单（金额为元字符串；时间为格式演示值）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"操作成功\",\"data\":{"
															+ "\"count\":1,\"total_fee\":\"80.00\",\"order_avg\":\"80.00\","
															+ "\"list\":[{\"order_id\":\"100\",\"trade_no\":\"-\","
															+ "\"order_status\":\"PAYED\",\"total_fee\":\"80.00\","
															+ "\"create_time\":1767225600,\"item_name\":\"Item-A\",\"item_id\":1,"
															+ "\"item_bn\":\"\",\"price\":\"80.00\",\"num\":1,"
															+ "\"item_spec_desc\":\"\",\"pic\":\"\"}]}}"),
									@ExampleObject(
											name = "success-empty",
											summary = "会员无订单（数值键为整型 0，message「成功」）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"count\":0,\"total_fee\":0,\"order_avg\":0,\"list\":[]}}"),
									@ExampleObject(
											name = "fail-missing-identity",
											summary = "mobile 与 unionid 均空（E0001）",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\",\"message\":\"unionid或者手机号必填\",\"data\":null}"),
									@ExampleObject(
											name = "fail-bad-mobile",
											summary = "mobile 键出现但格式非法（E0001）",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\",\"message\":\"请填写正确的手机号\",\"data\":null}"),
									@ExampleObject(
											name = "fail-member-not-found",
											summary = "手机号/unionid 查无会员（E0001）",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\",\"message\":\"会员信息获取失败\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								})),
		@ApiResponse(
				responseCode = "500",
				description = "未处理异常兜底（含 page≤0 负 OFFSET SQL 错误；GlobalExceptionHandler，ApiResult 非本信封）",
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
			content = @Content(schema = @Schema(implementation = EcxOrderListBodyDoc.class)))
	@PostMapping(value = "/ecx.order.list", name = "开放接口会员订单列表")
	public OpenapiEnvelope list(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description =
									"微信 unionid（与 body 同名键双通道，body 含键优先）；与 mobile 至少一个非空；"
											+ "body 值须为字符串或 null（非字符串 → E0001「请填写unionid」）",
							example = "oX1vU0abcDEF123456")
					@RequestParam(name = "unionid", required = false)
					String unionidParam,
			@Parameter(
							description =
									"会员手机号（双通道同上）；键一旦出现即须匹配 ^1[345789][0-9]{9}$"
											+ "（否则 E0001「请填写正确的手机号」）；非空时优先于 unionid",
							example = "13800138000")
					@RequestParam(name = "mobile", required = false)
					String mobileParam,
			@Parameter(
							description = "页码（双通道同上）；默认 1；PHP truthy 才启用自定义分页；无下限校验",
							example = "1")
					@RequestParam(name = "page", required = false)
					String pageParam,
			@Parameter(
							description =
									"每页数量（双通道同上）；page 未传时忽略（固定 10）；page 已传且本键空/非法 → 返回全部；>0 才分页",
							example = "10")
					@RequestParam(name = "page_size", required = false)
					String pageSizeParam,
			@Parameter(hidden = true) @FlexibleBody(required = false) Map<String, Object> body) {
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

	@Operation(
			summary = "开放接口订单取消原因列表",
			description =
					"鉴权：openapi 签名（公网网关校验后转发 internal；直连 internal 无鉴权上下文报 E4004）。"
							+ " 无入参（HttpServletRequest 仅容器注入，方法内未读取；无 query/body 参数）。"
							+ " 返回内置取消原因映射（OrderCancelReasonConfig.REASONS 静态配置，固定 12 键 \"1\"～\"12\"，"
							+ " 插入序 1→12），键即 ecx.order.cancel 的 cancel_reason_id 取值；选 12 须另传 cancel_reason。"
							+ " 副作用：无（纯静态配置读取，不访问 DB）。本接口无业务失败码。",
			operationId = "ecx.order.cancel.reasons.get.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 为固定 12 键原因映射；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxOrderCancelReasonsGetFailEnvelopeDoc。",
				content =
						@Content(
								schema =
										@Schema(
												implementation =
														EcxOrderCancelReasonsGetSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "全部 12 条内置取消原因（线上真实键值）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"1\":\"客户现在不想购买\",\"2\":\"客户商品价格较贵\","
															+ "\"3\":\"客户价格波动\",\"4\":\"客户商品缺货\",\"5\":\"客户重复下单\","
															+ "\"6\":\"客户订单商品选择有误\",\"7\":\"客户支付方式选择有误\","
															+ "\"8\":\"客户收货信息填写有误\",\"9\":\"客户发票信息填写有误\","
															+ "\"10\":\"客户无法支付订单\",\"11\":\"客户长时间未付款\",\"12\":\"客户其他原因\"}}"),
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
	@GetMapping(value = "/ecx.order.cancel.reasons.get", name = "开放接口订单取消原因列表")
	public Map<String, String> getCancelReasons(@Parameter(hidden = true) HttpServletRequest request) {
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

	@Operation(
			summary = "开放接口订单详情",
			description =
					"鉴权：openapi 签名（公网网关校验后转发 internal；直连 internal 无鉴权上下文报 E4004）。"
							+ " 2 键 query 与 JSON body 同名双通道：body 含键优先（值 null 视为未传），取值不 trim。"
							+ " order_id 与 ziti_code 二选一（PHP falsy：null/空串/\"0\" 视为未传），order_id 优先；"
							+ " ziti_code 经 Redis orderziticode:{code} 映射订单号。"
							+ " 数据源 buildOrderBundle（checkaftersales=false）：订单 42 固定键 + 商品行 14 键 +"
							+ " 优惠明细 5 键；无旧式快递单号时回填首张发货单的快递公司/单号；"
							+ " trade_no 取成功交易单号（无则 \"-\"）、pay_time 取成功交易 time_expire；"
							+ " username/unionid/openid 经会员表+微信用户表匹配（缺失空串）；goods_id 按 item_id 反查（查不到 0）。"
							+ " 副作用：无（只读，含 Redis 读）。"
							+ " 失败：HTTP 200 信封 E0001（请填写订单编号/订单相应的明细不存在）、"
							+ " E0000+status=fail（订单号为{id}的订单不存在，PHP 零码契约）；"
							+ " order_id 非整数 → HTTP 400 ApiResult{code:400,msg:参数错误}（非信封）。",
			operationId = "ecx.order.get.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxOrderGetDataDoc（42 固定键）；"
								+ " 业务失败：status=fail，code=E0001 或 E0000（PHP 零码，订单不存在），data=null，"
								+ "见 EcxOrderGetFailEnvelopeDoc；鉴权失败：status=fail, code=E4004 未授权，data=null。",
				content =
						@Content(
								schema = @Schema(implementation = EcxOrderGetSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "已支付已发货订单（时间为格式演示值；空串键为 null 归一结果）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\",\"data\":{"
															+ "\"order_id\":\"100\",\"trade_no\":\"-\",\"title\":\"\","
															+ "\"shop_code\":null,\"total_fee\":8000,\"mobile\":\"\","
															+ "\"freight_fee\":0,\"item_fee\":8000,\"receipt_type\":\"logistics\","
															+ "\"order_status\":\"WAIT_BUYER_CONFIRM\",\"pay_status\":\"PAYED\","
															+ "\"pay_time\":\"2026-01-01 00:00:00\",\"delivery_corp\":\"SF\","
															+ "\"delivery_code\":\"SFTRACK001\","
															+ "\"delivery_time\":\"2026-01-01 00:00:00\",\"end_time\":\"\","
															+ "\"delivery_status\":\"DONE\",\"cancel_status\":\"NO_APPLY_CANCEL\","
															+ "\"receiver_name\":\"\",\"receiver_mobile\":\"\",\"receiver_zip\":\"\","
															+ "\"receiver_state\":\"\",\"receiver_city\":\"\",\"receiver_district\":\"\","
															+ "\"receiver_address\":\"\",\"member_discount\":0,\"coupon_discount\":0,"
															+ "\"discount_fee\":0,\"discount_info\":[],"
															+ "\"create_time\":\"2026-01-01 00:00:00\","
															+ "\"update_time\":\"2026-01-01 00:00:00\",\"pay_type\":\"\","
															+ "\"remark\":\"\",\"distributor_remark\":\"\",\"point_use\":0,"
															+ "\"point_fee\":0,\"items\":[{\"goods_id\":1,\"item_bn\":\"\","
															+ "\"item_name\":\"Item-A\",\"num\":1,\"price\":8000,\"total_fee\":8000,"
															+ "\"item_fee\":8000,\"discount_info\":[],\"point_use\":0,\"point_fee\":0,"
															+ "\"item_spec_desc\":\"\",\"volume\":0,\"weight\":0.0,"
															+ "\"item_type\":\"normal\"}],\"user_id\":50,\"username\":\"\","
															+ "\"unionid\":\"\",\"openid\":\"\",\"order_class\":\"normal\"}}"),
									@ExampleObject(
											name = "fail-missing-order-id",
											summary = "order_id 与 ziti_code 均未传（E0001）",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\",\"message\":\"请填写订单编号\",\"data\":null}"),
									@ExampleObject(
											name = "fail-ziti-expired",
											summary = "核销码无映射/过期（E0001）",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\",\"message\":\"订单相应的明细不存在\",\"data\":null}"),
									@ExampleObject(
											name = "fail-order-not-found-zero-code",
											summary = "订单不存在（E0000 + status=fail，PHP 零码契约）",
											value =
													"{\"status\":\"fail\",\"code\":\"E0000\","
															+ "\"message\":\"订单号为100的订单不存在\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								})),
		@ApiResponse(
				responseCode = "400",
				description = "order_id 非整数（BadRequest「参数错误」，GlobalExceptionHandler，ApiResult 非本信封）",
				content =
						@Content(
								examples =
										@ExampleObject(
												name = "bad-order-id",
												value = "{\"code\":400,\"msg\":\"参数错误\"}"))),
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
			description = "GET 可选 JSON body（与 query 同名键双通道，body 含键优先）",
			content = @Content(schema = @Schema(implementation = EcxOrderGetBodyDoc.class)))
	@GetMapping(value = "/ecx.order.get", name = "开放接口订单详情")
	public Map<String, Object> getDetail(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description =
									"订单 ID（与 body 同名键双通道，body 含键优先）；与 ziti_code 二选一（本键优先，"
											+ "PHP falsy：null/空串/\"0\" 视为未传）；非整数 → HTTP 400「参数错误」；"
											+ "无此单 → E0000+fail「订单号为{id}的订单不存在」",
							example = "100")
					@RequestParam(name = "order_id", required = false)
					String orderIdParam,
			@Parameter(
							description =
									"自提核销码（双通道同上）；order_id 未传时经 Redis 映射订单号，无映射/过期 →"
											+ " E0001「订单相应的明细不存在」",
							example = "760773986531")
					@RequestParam(name = "ziti_code", required = false)
					String zitiCodeParam,
			@Parameter(hidden = true) @FlexibleBody(required = false) Map<String, Object> body) {
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
