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

package cn.shopex.ecshopx.adapay.api.admin.v1;

import cn.shopex.ecshopx.adapay.api.admin.v1.doc.AdapayCallbackBodyDoc;
import cn.shopex.ecshopx.adapay.api.admin.v1.doc.AdapayCallbackFailEnvelopeDoc;
import cn.shopex.ecshopx.adapay.api.admin.v1.doc.AdapayCallbackSuccessEnvelopeDoc;
import cn.shopex.ecshopx.adapay.service.callback.AdapayCallbackOrchestratorService;
import cn.shopex.ecshopx.common.annotation.DingoResponse;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Adapay 异步回调", description = "汇付 Adapay 第三方服务器异步通知入口（验签后按事件类型分发）")
@DingoResponse(
		resource = DingoResponse.ResourceStyle.PLAIN,
		badRequest = DingoResponse.BadRequestStyle.NONE,
		unauthorized = false,
		notFound = false)
@RestController("adapayCallBackAdminV1")
@RequestMapping("/api/v1/adapay")
public class CallBackController {

	private final AdapayCallbackOrchestratorService adapayCallbackOrchestratorService;

	public CallBackController(AdapayCallbackOrchestratorService adapayCallbackOrchestratorService) {
		this.adapayCallbackOrchestratorService = adapayCallbackOrchestratorService;
	}

	@Operation(
			summary = "AdaPay 异步回调",
			description =
					"AdaPay 服务器异步通知入口（无 mapping.name，路由无别名）。"
							+ " 鉴权面：公开——OperatorJwtAuthenticationFilter 对本 POST 路径显式放行，"
							+ "类无 @AdminAuth，不使用 Operator JWT；以 body.sign 对 body.data 原文做"
							+ " SHA1withRSA 验签（公钥 adapay.callback.rsa-public-key），验签失败/公钥未配置报 400。"
							+ " 请求体 @FlexibleBody：application/json 与 form（含 query 参数并入 form Map）双通道；"
							+ "空体报 400「请求体不能为空」。"
							+ " 顶层读取 type/data/sign 三键；data 为 JSON 字符串，验签后解析，键集合随 type"
							+ "（见 AdapayCallbackPaymentPayload / AdapayCallbackCorpMemberPayload /"
							+ " AdapayCallbackCorpMemberUpdatePayload / AdapayCallbackPaymentReversePayload）。"
							+ " type 枚举 10 值：payment.succeeded/failed → 订单支付更新（description=membercard 另置"
							+ " DIVED；description=depositRecharge 走储值分支且恒报 422 储值单不存在）；"
							+ "corp_member.succeeded/failed → 开户审核落库（E 时回写结算账户与经销商开户时间）；"
							+ "corp_member_update.succeeded/failed → 修改审核落库（入站 S→D 触发结算账户删旧建新出站调用，"
							+ "F→B）；payment_reverse.succeeded/failed → 撤销单状态落库；"
							+ "payment.close.succeeded/failed → 忽略（data 返回 {}）；其余 type 报 400「unknown type」。"
							+ " 副作用：写 trade/adapay_member/adapay_corp_member/adapay_settle_account/"
							+ "adapay_member_update_log/adapay_payment_reverse/operators 等表，Redis 计数与储值累计，"
							+ "事务提交后发布充值短信通知任务，开户/修改审核按 is_sms 发送短信，"
							+ "修改审核 D 分支出站调用 AdaPay SettleAccount.delete/create"
							+ "（adapay.callback.settle-outbound-base-url 为空时本地 no-op 成功）。"
							+ " 成功响应为 Dingo 数组信封 {message:成功,status_code:200,data:[\"success\"]}；"
							+ "失败为标准 ApiResult（PLAIN/NONE，无 Dingo 包装）。",
			operationId = "adapay.callback")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，Dingo 数组信封：message=成功、status_code=200；data 各事件为 [\"success\"]，"
								+ "payment.close.* 为 {}。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(implementation = AdapayCallbackSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "事件处理成功（payment/corp_member/corp_member_update/payment_reverse）",
											value = "{\"message\":\"成功\",\"status_code\":200,\"data\":[\"success\"]}"),
									@ExampleObject(
											name = "success-close",
											summary = "payment.close.succeeded / payment.close.failed（忽略）",
											value = "{\"message\":\"成功\",\"status_code\":200,\"data\":{}}")
								})),
		@ApiResponse(
				responseCode = "400",
				description =
						"参数/验签失败（BadRequestException，NONE 风格标准 ApiResult）：签名验证失败 /"
								+ " 回调 data 不是合法 JSON / unknown type / member_id 无效 / 缺少 order_no /"
								+ " 请求体不能为空 / 更新已处理，不需要更新。",
				content =
						@Content(
								mediaType = "application/json",
								schema = @Schema(implementation = AdapayCallbackFailEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "fail-sign",
											summary = "验签失败/公钥未配置",
											value = "{\"code\":400,\"msg\":\"签名验证失败\"}"),
									@ExampleObject(
											name = "fail-bad-data",
											summary = "data 空或非法 JSON",
											value = "{\"code\":400,\"msg\":\"回调 data 不是合法 JSON\"}"),
									@ExampleObject(
											name = "fail-unknown-type",
											summary = "type 不在枚举内",
											value = "{\"code\":400,\"msg\":\"unknown type\"}"),
									@ExampleObject(
											name = "fail-already-processed",
											summary = "trade 非 NOTPAY 重复回调",
											value = "{\"code\":400,\"msg\":\"更新已处理，不需要更新\"}")
								})),
		@ApiResponse(
				responseCode = "422",
				description =
						"业务失败（ResourceException，PLAIN 风格标准 ApiResult）：开户会员信息不存在 /"
								+ " 企业用户修改记录不存在 / 未查询到更新数据 / 更新订单不存在 / 交易单状态更新失败 /"
								+ " 储值单不存在或主键无效，无法完成回调。",
				content =
						@Content(
								mediaType = "application/json",
								schema = @Schema(implementation = AdapayCallbackFailEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "fail-member-not-found",
											value = "{\"code\":422,\"msg\":\"开户会员信息不存在\"}"),
									@ExampleObject(
											name = "fail-trade-not-found",
											value = "{\"code\":422,\"msg\":\"更新订单不存在\"}"),
									@ExampleObject(
											name = "fail-deposit",
											summary = "description=depositRecharge 分支（null 储值单号恒失败）",
											value = "{\"code\":422,\"msg\":\"储值单不存在或主键无效，无法完成回调\"}")
								})),
		@ApiResponse(
				responseCode = "500",
				description = "未处理异常（GlobalExceptionHandler.handleException），固定文案。",
				content =
						@Content(
								mediaType = "application/json",
								schema = @Schema(implementation = AdapayCallbackFailEnvelopeDoc.class),
								examples =
										@ExampleObject(
												name = "server-error",
												value = "{\"code\":500,\"msg\":\"服务器内部错误\"}")))
	})
	@SecurityRequirements
	@PostMapping(value = "/callback")
	public ResponseEntity<Map<String, Object>> handle(
			@RequestBody(
							description =
									"回调报文（JSON 或 form 双通道）；顶层 type/data/sign 三键，data 内键集合随 type，"
											+ "语义与真实样例见 schema",
							required = true,
							content =
									@Content(
											mediaType = "application/json",
											schema = @Schema(implementation = AdapayCallbackBodyDoc.class),
											examples = {
												@ExampleObject(
														name = "payment-succeeded",
														summary = "支付成功（membercard；sign 为占位 Base64）",
														value =
																"{\"type\":\"payment.succeeded\",\"data\":\"{\\\"order_no\\\":"
																		+ "\\\"3651023000015\\\",\\\"status\\\":\\\"succeeded\\\","
																		+ "\\\"pay_channel\\\":\\\"wx_lite\\\",\\\"description\\\":"
																		+ "\\\"membercard\\\",\\\"expend\\\":{\\\"bank_type\\\":"
																		+ "\\\"ALIPAY\\\"},\\\"id\\\":\\\"tx1\\\"}\","
																		+ "\"sign\":\"K0x1eS5wq8Zm3nQ1rT7vYb2cD4eF6gH8iJ0kL2mN4oP6qR8sT0uV2wX4yZ6aB8cD0eF==\"}"),
												@ExampleObject(
														name = "corp-member-succeeded",
														summary = "企业开户审核通过（audit_state=E 回写结算账户）",
														value =
																"{\"type\":\"corp_member.succeeded\",\"data\":\"{\\\"member_id\\\":"
																		+ "\\\"1\\\",\\\"app_id\\\":\\\"app-x\\\",\\\"audit_state\\\":"
																		+ "\\\"E\\\",\\\"audit_desc\\\":\\\"审核通过\\\","
																		+ "\\\"settle_account_id\\\":\\\"local_settle_account\\\"}\","
																		+ "\"sign\":\"K0x1eS5wq8Zm3nQ1rT7vYb2cD4eF6gH8iJ0kL2mN4oP6qR8sT0uV2wX4yZ6aB8cD0eF==\"}"),
												@ExampleObject(
														name = "payment-reverse-succeeded",
														summary = "支付撤销成功",
														value =
																"{\"type\":\"payment_reverse.succeeded\",\"data\":\"{\\\"order_no\\\":"
																		+ "\\\"20231127001_rev_543211700000000000\\\",\\\"status\\\":"
																		+ "\\\"succeeded\\\"}\","
																		+ "\"sign\":\"K0x1eS5wq8Zm3nQ1rT7vYb2cD4eF6gH8iJ0kL2mN4oP6qR8sT0uV2wX4yZ6aB8cD0eF==\"}")
											}))
					@FlexibleBody Map<String, Object> body) {
		return ResponseEntity.ok(adapayCallbackOrchestratorService.handle(body));
	}
}
