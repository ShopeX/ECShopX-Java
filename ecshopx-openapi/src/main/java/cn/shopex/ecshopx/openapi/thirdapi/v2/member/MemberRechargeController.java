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
import cn.shopex.ecshopx.common.openapi.OpenapiMemberRechargeRuleCreatePort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberRechargeRuleDeletePort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberRechargeRuleListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberRechargeRuleUpdatePort;
import cn.shopex.ecshopx.common.openapi.OpenapiMemberRechargeTradeListPort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberRechargeTradeListBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxMemberRechargeTradeListSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxRechargeRuleAddBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxRechargeRuleAddSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxRechargeRuleDeleteBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxRechargeRuleDeleteSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxRechargeRuleGetSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxRechargeRuleUpdateBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.member.doc.EcxRechargeRuleUpdateSuccessEnvelopeDoc;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "开放接口-会员储值V2")
@OpenapiResponse
@RestController("openapiV2MemberRecharge")
@RequestMapping("/api/openapi/internal/v2")
public class MemberRechargeController extends OpenapiBaseController {

	private final OpenapiMemberRechargeRuleCreatePort rechargeRuleCreatePort;
	private final OpenapiMemberRechargeRuleDeletePort rechargeRuleDeletePort;
	private final OpenapiMemberRechargeRuleUpdatePort rechargeRuleUpdatePort;
	private final OpenapiMemberRechargeRuleListPort rechargeRuleListPort;
	private final OpenapiMemberRechargeTradeListPort rechargeTradeListPort;

	public MemberRechargeController(
			OpenapiMemberRechargeRuleCreatePort rechargeRuleCreatePort,
			OpenapiMemberRechargeRuleDeletePort rechargeRuleDeletePort,
			OpenapiMemberRechargeRuleUpdatePort rechargeRuleUpdatePort,
			OpenapiMemberRechargeRuleListPort rechargeRuleListPort,
			OpenapiMemberRechargeTradeListPort rechargeTradeListPort) {
		this.rechargeRuleCreatePort = rechargeRuleCreatePort;
		this.rechargeRuleDeletePort = rechargeRuleDeletePort;
		this.rechargeRuleUpdatePort = rechargeRuleUpdatePort;
		this.rechargeRuleListPort = rechargeRuleListPort;
		this.rechargeTradeListPort = rechargeTradeListPort;
	}

	@Operation(
			summary = "开放接口查询储值面额规则列表",
			description =
					"按签名鉴权租户 company_id 查询储值面额规则列表，无业务入参。"
							+ " 固定 page=1/page_size=20 查 deposit_recharge_rule（create_time 降序；"
							+ " 面额上限 14 故实际全量），无分页/过滤参数。"
							+ " data 单键 {list}，行 5 键同创建接口（rechargerule_id/money 元 2 位小数/"
							+ " rule_type/rule_data 整型/create_time 格式化时间）。"
							+ " **无面额规则时 data=null**（仍 success，非空 list）。Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.member.rechargerule.get,"
							+ " version=2.0）签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.member.rechargerule.get.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data={list:[行 5 键]}"
								+ "（无规则 data=null，仍 success）；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxRechargeRuleGetFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxRechargeRuleGetSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功（时间为格式演示值）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"list\":[{\"rechargerule_id\":1,\"money\":\"80.00\","
															+ "\"rule_type\":\"money\",\"rule_data\":0,"
															+ "\"create_time\":\"2026-01-01 00:00:00\"}]}}"),
									@ExampleObject(
											name = "success-empty",
											summary = "无面额规则（data=null，仍 success）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@GetMapping(value = "/ecx.member.rechargerule.get", name = "开放接口查询储值面额规则列表")
	public Map<String, Object> getRechargeRuleList(@Parameter(hidden = true) HttpServletRequest request) {
		long companyId = requireCompanyId(request);
		return rechargeRuleListPort.getRechargeRuleList(companyId);
	}

	@Operation(
			summary = "开放接口查询储值交易记录",
			description =
					"按签名鉴权租户 company_id 分页查询储值交易（deposit_trade，固定 trade_status=SUCCESS，"
							+ " time_start 降序）：7 键双通道（page/page_size originalString 不 trim；"
							+ " 其余 5 键 mergeString trim；body 含键优先、值 null 视为未传）。"
							+ " 过滤键 PHP truthy（空串/\"0\" 忽略）：mobile trim 加密等值、trade_id/shop_id trim 等值；"
							+ " date_begin/date_end 按 yyyy-MM-dd HH:mm:ss（系统时区）解析为 time_start 区间，"
							+ " **解析失败静默按 0 不报错**（begin=0 等于不过滤；end=0 通常空结果）。"
							+ " page 默认 1、page_size 默认 20（PHP 式解析），原始 page_size ≥500 报"
							+ " E5002「每页的大小最大为500！」，钳制后 page≥1、page_size∈[20,499]。"
							+ " data：total_count/is_last_page(0|1)/pager{page,page_size}/list；行 15 键："
							+ " trade_id/trade_type(recharge|consume)/money 元 2 位小数字符串/mobile 解密（null 透传）/"
							+ " member_card_code/shop_id/shop_name/open_id/transaction_id/recharge_rule_id/bank_type"
							+ "（后三键空白归 null）/authorizer_appid/detail/time_start/time_expire 格式化时间"
							+ "（Unix 字符串非法按 epoch 0）。Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.member.recharge.trade.get,"
							+ " version=2.0）签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.member.recharge.trade.get.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxMemberRechargeTradeListDataDoc；"
								+ " 业务失败：status=fail, code=E5002 每页的大小最大为500！，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxMemberRechargeTradeListFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxMemberRechargeTradeListSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功（trade_id/money 取自 AdapayDepositTradeRechargeCallback 测试夹具；时间为格式演示值）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":1,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},"
															+ "\"list\":[{\"trade_id\":\"d1\",\"trade_type\":\"recharge\","
															+ "\"money\":\"80.00\",\"mobile\":\"13800138000\","
															+ "\"member_card_code\":\"card\",\"shop_id\":\"0\",\"shop_name\":\"\","
															+ "\"open_id\":null,\"transaction_id\":null,\"recharge_rule_id\":null,"
															+ "\"bank_type\":null,\"authorizer_appid\":\"\",\"detail\":\"\","
															+ "\"time_start\":\"2026-01-01 00:00:00\","
															+ "\"time_expire\":\"2026-01-01 00:00:00\"}]}}"),
									@ExampleObject(
											name = "success-empty",
											summary = "无成功交易（list 空数组）",
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
	@GetMapping(value = "/ecx.member.recharge.trade.get", name = "开放接口查询储值交易记录")
	public Map<String, Object> getRechargeTradeList(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(description = "页码，默认 1；PHP 式解析后 max(…,1)", example = "1")
					@RequestParam(name = "page", required = false)
					String pageParam,
			@Parameter(
							description = "每页数量，默认 20；原始解析值 ≥500 报 E5002；钳制后生效 [20,499]",
							example = "20")
					@RequestParam(name = "page_size", required = false)
					String pageSizeParam,
			@Parameter(
							description = "会员手机号；trim 加密等值过滤；空串/\"0\" 忽略",
							example = "13800138000")
					@RequestParam(name = "mobile", required = false)
					String mobileParam,
			@Parameter(
							description = "储值交易单号；trim 等值过滤；空串/\"0\" 忽略",
							example = "d1")
					@RequestParam(name = "trade_id", required = false)
					String tradeIdParam,
			@Parameter(
							description = "门店 ID（字符串等值）；空串/\"0\" 忽略（示例为格式演示值）",
							example = "1")
					@RequestParam(name = "shop_id", required = false)
					String shopIdParam,
			@Parameter(
							description = "起始时间 yyyy-MM-dd HH:mm:ss；解析失败静默按 0（不过滤）；空串/\"0\" 忽略（格式演示值）",
							example = "2026-01-01 00:00:00")
					@RequestParam(name = "date_begin", required = false)
					String dateBeginParam,
			@Parameter(
							description = "截止时间；解析失败按 0（time_start≤0 通常空结果）（格式演示值）",
							example = "2026-12-31 23:59:59")
					@RequestParam(name = "date_end", required = false)
					String dateEndParam,
			@RequestBody(
							description = "可选 JSON body；7 键与 query 同名双通道（body 含键优先，值 null 视为未传）；语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxMemberRechargeTradeListBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2MemberRechargeTradeListParams.PageSpec pageSpec =
				OpenapiThirdApiV2MemberRechargeTradeListParams.resolve(pageParam, pageSizeParam, body);
		String mobile = OpenapiRequestParams.mergeString(mobileParam, body, "mobile");
		String tradeId = OpenapiRequestParams.mergeString(tradeIdParam, body, "trade_id");
		String shopId = OpenapiRequestParams.mergeString(shopIdParam, body, "shop_id");
		String dateBegin = OpenapiRequestParams.mergeString(dateBeginParam, body, "date_begin");
		String dateEnd = OpenapiRequestParams.mergeString(dateEndParam, body, "date_end");
		return rechargeTradeListPort.getRechargeTradeList(
				companyId,
				pageSpec.page(),
				pageSpec.pageSize(),
				mobile,
				tradeId,
				shopId,
				dateBegin,
				dateEnd);
	}

	@Operation(
			summary = "开放接口新增储值面额规则",
			description =
					"按签名鉴权租户 company_id 新增储值面额规则（3 键均必填，mergeString 双通道 trim，"
							+ " body 含键优先、值 null 视为未传）："
							+ " fixed_money 元（小数位 ≤2，换算分后 1≤分值≤10,000,000 即 0.01~100000 元，"
							+ " 否则 E5580「请填写正确的充值金额」；缺失 E5001「需付金额必填」）；"
							+ " rule_type 仅 money/point（否则 E5001「请填写正确的充值类型」）；"
							+ " rule_data 非负整数字符串（小数点/科学计数拒绝，E5001「请填写正确的赠送金额」；"
							+ " money 为赠送金额、point 为赠送积分，整数原样落库）。"
							+ " 同租户面额（分）唯一：重复 E5580「当前面额数已存在，不能重复添加」；"
							+ " 面额总数 ≥14 拒绝 E5580「最多添加14个面额」。"
							+ " 成功 data 5 键：rechargerule_id/money（元 2 位小数）/rule_type/rule_data（整型）/"
							+ " create_time 格式化时间。Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.member.rechargerule.add,"
							+ " version=2.0）签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：INSERT deposit_recharge_rule 单行（无显式事务）；无异步、无第三方调用。",
			operationId = "ecx.member.rechargerule.add.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxRechargeRuleAddDataDoc；"
								+ " 业务失败：status=fail, code=E5001 需付金额必填·请填写正确的充值类型·请填写正确的赠送金额 /"
								+ " E5580 请填写正确的充值金额·当前面额数已存在不能重复添加·最多添加14个面额，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxRechargeRuleAddFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxRechargeRuleAddSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "新增成功（80 元面额；时间为格式演示值）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"rechargerule_id\":1,\"money\":\"80.00\","
															+ "\"rule_type\":\"money\",\"rule_data\":0,"
															+ "\"create_time\":\"2026-01-01 00:00:00\"}}"),
									@ExampleObject(
											name = "fail-duplicate",
											summary = "同面额已存在",
											value =
													"{\"status\":\"fail\",\"code\":\"E5580\","
															+ "\"message\":\"当前面额数已存在，不能重复添加\",\"data\":null}"),
									@ExampleObject(
											name = "fail-upper-limit",
											summary = "面额数量达上限",
											value =
													"{\"status\":\"fail\",\"code\":\"E5580\","
															+ "\"message\":\"最多添加14个面额\",\"data\":null}"),
									@ExampleObject(
											name = "fail-rule-type",
											summary = "rule_type 缺失/非法",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\","
															+ "\"message\":\"请填写正确的充值类型\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.member.rechargerule.add", name = "开放接口新增储值面额规则")
	public Map<String, Object> createRechargeRule(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "需付金额（元）；必填，小数位 ≤2 且 0.01~100000；重复面额/超 14 个 E5580",
							required = true,
							example = "80")
					@RequestParam(name = "fixed_money", required = false)
					String fixedMoneyParam,
			@Parameter(
							description = "充值赠送类型；必填，仅 money/point（否则 E5001）",
							required = true,
							example = "money")
					@RequestParam(name = "rule_type", required = false)
					String ruleTypeParam,
			@Parameter(
							description = "赠送数值；必填非负整数字符串（小数/科学计数拒绝）；money 赠金额/point 赠积分",
							required = true,
							example = "0")
					@RequestParam(name = "rule_data", required = false)
					String ruleDataParam,
			@RequestBody(
							description = "可选 JSON body；3 键与 query 同名双通道（mergeString trim，body 含键优先）；语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxRechargeRuleAddBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String fixedMoneyRaw = OpenapiRequestParams.mergeString(fixedMoneyParam, body, "fixed_money");
		String ruleTypeRaw = OpenapiRequestParams.mergeString(ruleTypeParam, body, "rule_type");
		String ruleDataRaw = OpenapiRequestParams.mergeString(ruleDataParam, body, "rule_data");
		return rechargeRuleCreatePort.createRechargeRule(
				companyId, fixedMoneyRaw, ruleTypeRaw, ruleDataRaw);
	}

	@Operation(
			summary = "开放接口修改储值面额规则",
			description =
					"按签名鉴权租户 company_id 修改储值面额规则（4 键均必填，mergeString 双通道 trim，"
							+ " body 含键优先、值 null 视为未传）："
							+ " rechargerule_id 整数 ≥1（否则 E5001「请填写正确的储值规则ID」）；"
							+ " fixed_money 元（小数位 ≤2，分值 1~10,000,000，否则 E5580「请填写正确的充值金额」；"
							+ " 缺失 E5001「需付金额必填」）；rule_type 仅 money/point（E5001）；"
							+ " rule_data 非负整数字符串（E5001「请填写正确的赠送金额」）。"
							+ " **全量覆盖** money/rule_type/rule_data 三字段（create_time 不变，非 PATCH）。"
							+ " 定位查无/UPDATE affected=0/更新后回读为空 → E5580「修改的充值规则不存在」；"
							+ " 面额（分）同租户唯一排除自身：冲突 E5580「当前面额数已存在，不能重复添加」"
							+ "（同值改自身放行）。无面额数量上限校验（区别于新增）。"
							+ " 成功 data 为更新后回读 5 键行（同创建接口格式）。Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}"
							+ "（method=ecx.member.rechargerule.update, version=2.0）签名校验后转发；"
							+ " 直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：@Transactional UPDATE deposit_recharge_rule 三字段；无异步、无第三方调用。",
			operationId = "ecx.member.rechargerule.update.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 为更新后 5 键行；"
								+ " 业务失败：status=fail, code=E5001 储值规则ID·需付金额必填·充值类型·赠送金额 /"
								+ " E5580 请填写正确的充值金额·修改的充值规则不存在·当前面额数已存在不能重复添加，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxRechargeRuleUpdateFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxRechargeRuleUpdateSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "修改成功（返回更新后行；时间为格式演示值）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"rechargerule_id\":1,\"money\":\"80.00\","
															+ "\"rule_type\":\"money\",\"rule_data\":0,"
															+ "\"create_time\":\"2026-01-01 00:00:00\"}}"),
									@ExampleObject(
											name = "fail-not-found",
											summary = "规则查无",
											value =
													"{\"status\":\"fail\",\"code\":\"E5580\","
															+ "\"message\":\"修改的充值规则不存在\",\"data\":null}"),
									@ExampleObject(
											name = "fail-duplicate",
											summary = "面额与其他规则冲突",
											value =
													"{\"status\":\"fail\",\"code\":\"E5580\","
															+ "\"message\":\"当前面额数已存在，不能重复添加\",\"data\":null}"),
									@ExampleObject(
											name = "fail-param",
											summary = "rechargerule_id 缺失/非法",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\","
															+ "\"message\":\"请填写正确的储值规则ID\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.member.rechargerule.update", name = "开放接口修改储值面额规则")
	public Map<String, Object> updateRechargeRule(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "储值规则 ID；必填整数 ≥1（否则 E5001）；查无 E5580「修改的充值规则不存在」",
							required = true,
							example = "1")
					@RequestParam(name = "rechargerule_id", required = false)
					String rechargeruleIdParam,
			@Parameter(
							description = "需付金额（元）；必填，小数位 ≤2 且 0.01~100000；与其他规则面额冲突 E5580",
							required = true,
							example = "80")
					@RequestParam(name = "fixed_money", required = false)
					String fixedMoneyParam,
			@Parameter(
							description = "充值赠送类型；必填，仅 money/point（否则 E5001）",
							required = true,
							example = "money")
					@RequestParam(name = "rule_type", required = false)
					String ruleTypeParam,
			@Parameter(
							description = "赠送数值；必填非负整数字符串；money 赠金额/point 赠积分",
							required = true,
							example = "0")
					@RequestParam(name = "rule_data", required = false)
					String ruleDataParam,
			@RequestBody(
							description = "可选 JSON body；4 键与 query 同名双通道（mergeString trim，body 含键优先）；全量覆盖语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxRechargeRuleUpdateBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String rechargeruleIdRaw =
				OpenapiRequestParams.mergeString(rechargeruleIdParam, body, "rechargerule_id");
		String fixedMoneyRaw = OpenapiRequestParams.mergeString(fixedMoneyParam, body, "fixed_money");
		String ruleTypeRaw = OpenapiRequestParams.mergeString(ruleTypeParam, body, "rule_type");
		String ruleDataRaw = OpenapiRequestParams.mergeString(ruleDataParam, body, "rule_data");
		return rechargeRuleUpdatePort.updateRechargeRule(
				companyId, rechargeruleIdRaw, fixedMoneyRaw, ruleTypeRaw, ruleDataRaw);
	}

	@Operation(
			summary = "开放接口删除储值面额规则",
			description =
					"按签名鉴权租户 company_id 删除储值面额规则：rechargerule_id 必填"
							+ "（mergeString 双通道 trim，body 含键优先、值 null 视为未传；"
							+ " trim 后须可 parseLong 且 ≥1，否则 E5001「请填写正确的储值规则ID」）。"
							+ " 按 company_id+id 定位 deposit_recharge_rule（LIMIT 1），"
							+ " 查无 E5581「未查询到该储值规则」；命中后按 company_id+id 删除，"
							+ " 不校验规则是否被交易引用。"
							+ " 成功 data={status:true}（布尔）。Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}"
							+ "（method=ecx.member.rechargerule.delete, version=2.0）签名校验后转发；"
							+ " 直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 副作用：@Transactional DELETE deposit_recharge_rule 单行；无异步、无第三方调用。",
			operationId = "ecx.member.rechargerule.delete.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功, data={status:true}；"
								+ " 业务失败：status=fail, code=E5001 请填写正确的储值规则ID /"
								+ " E5581 未查询到该储值规则，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxRechargeRuleDeleteFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxRechargeRuleDeleteSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "删除成功",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"status\":true}}"),
									@ExampleObject(
											name = "fail-not-found",
											summary = "规则查无",
											value =
													"{\"status\":\"fail\",\"code\":\"E5581\","
															+ "\"message\":\"未查询到该储值规则\",\"data\":null}"),
									@ExampleObject(
											name = "fail-param",
											summary = "rechargerule_id 缺失/非整数/小于 1",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\","
															+ "\"message\":\"请填写正确的储值规则ID\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@DeleteMapping(value = "/ecx.member.rechargerule.delete", name = "开放接口删除储值面额规则")
	public Map<String, Object> deleteRechargeRule(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "储值规则 ID；必填整数 ≥1（否则 E5001）；查无 E5581「未查询到该储值规则」",
							required = true,
							example = "1")
					@RequestParam(name = "rechargerule_id", required = false)
					String rechargeruleIdParam,
			@RequestBody(
							description = "可选 JSON body；rechargerule_id 与 query 同名双通道（mergeString trim，body 含键优先）",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxRechargeRuleDeleteBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String rechargeruleIdRaw =
				OpenapiRequestParams.mergeString(rechargeruleIdParam, body, "rechargerule_id");
		return rechargeRuleDeletePort.deleteRechargeRule(companyId, rechargeruleIdRaw);
	}
}
