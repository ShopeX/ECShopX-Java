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
import cn.shopex.ecshopx.common.openapi.OpenapiAftersalesDetailPort;
import cn.shopex.ecshopx.common.openapi.OpenapiAftersalesIncrListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiAftersalesListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiRefundDetailPort;
import cn.shopex.ecshopx.common.openapi.OpenapiRefundIncrListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiRefundListPort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiMemberQueryParams;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc.EcxAftersalesDetailGetBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc.EcxAftersalesDetailGetSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc.EcxAftersalesIncrListBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc.EcxAftersalesIncrListSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc.EcxAftersalesListBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc.EcxAftersalesListSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc.EcxRefundDetailGetBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc.EcxRefundDetailGetSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc.EcxRefundIncrListBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc.EcxRefundIncrListSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc.EcxRefundListBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.doc.EcxRefundListSuccessEnvelopeDoc;
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
@Tag(name = "开放接口-售后退款V2")
@OpenapiResponse
@RestController("openapiV2Aftersales")
@RequestMapping("/api/openapi/internal/v2")
public class AftersalesController extends OpenapiBaseController {

	private final OpenapiAftersalesListPort aftersalesListPort;
	private final OpenapiAftersalesIncrListPort aftersalesIncrListPort;
	private final OpenapiAftersalesDetailPort aftersalesDetailPort;
	private final OpenapiRefundListPort refundListPort;
	private final OpenapiRefundIncrListPort refundIncrListPort;
	private final OpenapiRefundDetailPort refundDetailPort;

	public AftersalesController(
			OpenapiAftersalesListPort aftersalesListPort,
			OpenapiAftersalesIncrListPort aftersalesIncrListPort,
			OpenapiAftersalesDetailPort aftersalesDetailPort,
			OpenapiRefundListPort refundListPort,
			OpenapiRefundIncrListPort refundIncrListPort,
			OpenapiRefundDetailPort refundDetailPort) {
		this.aftersalesListPort = aftersalesListPort;
		this.aftersalesIncrListPort = aftersalesIncrListPort;
		this.aftersalesDetailPort = aftersalesDetailPort;
		this.refundListPort = refundListPort;
		this.refundIncrListPort = refundIncrListPort;
		this.refundDetailPort = refundDetailPort;
	}

	@Operation(
			summary = "开放接口售后单列表",
			description =
					"按签名鉴权租户 company_id 分页返回售后单（create_time 倒序）："
							+ " data 含 total_count、is_last_page（1/0）、pager（page/page_size 归一化回显）与 list（18 键/行）。"
							+ " 分页：page 缺省 1（<1 归一 1）；page_size 缺省 20、<20 归一 20；解析后 ≥500 报 E0001"
							+ "「每页的大小最大为500！」（500 本身也报错）。"
							+ " mobile PHP-truthy 才生效：须匹配 ^1[345789][0-9]{9}$（E5320），查无会员 E5331，命中按 user_id 过滤；"
							+ " time_begin/time_end PHP-truthy + yyyy-MM-dd[ HH:mm:ss] 转 Unix 秒做 create_time 闭区间（≥/≤），"
							+ " 解析失败按 0；双端生效且下界>上界报 E5320「开始时间不能大于结束时间」。"
							+ " 行内 aftersales_address：主单 JSON 原样解析；主单空/\"0\" 且门店有默认售后地址（is_default=1）"
							+ " 则回退默认地址 5 键（含 is_default）；均无为 {}。detail 明细按 company+aftersales_bn+user_id 查，"
							+ " 行 update_time 实取 create_time（PHP 旧契约）。sendconfirm_data 为库内 JSON 透传，"
							+ " 当前 Java 无写入路径（换货重发未迁入），内部键集不虚构。无 refund_bn（详情接口才有）。"
							+ " 五参数双通道（body 含键取原始值，否则用 query）。返回 Map 由 Advice 包装（message 固定「成功」）。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.aftersales.get, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.aftersales.get.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxAftersalesListDataDoc"
								+ "（无数据时 total_count=0、is_last_page=1、list=[]）；"
								+ " 业务失败：status=fail, code=E0001 每页的大小最大为500！ / E5320 请填写正确的手机号·"
								+ "开始时间不能大于结束时间 / E5331 会员找不到，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxAftersalesListFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxAftersalesListSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功（主单无地址，回退门店默认地址）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":1,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},"
															+ "\"list\":[{\"aftersales_bn\":\"202605101200001\",\"order_id\":\"88001\","
															+ "\"aftersales_type\":\"REFUND_GOODS\",\"aftersales_status\":1,\"progress\":2,"
															+ "\"refund_fee\":100,\"refund_point\":0,\"reason\":\"测试售后\","
															+ "\"description\":null,\"evidence_pic\":[],\"refuse_reason\":null,\"memo\":null,"
															+ "\"sendback_data\":{\"corp_code\":\"SF\",\"logi_no\":\"1234567890\","
															+ "\"receiver_address\":\"\",\"receiver_mobile\":\"\"},\"sendconfirm_data\":null,"
															+ "\"create_time\":\"2023-11-15 08:00:01\",\"update_time\":\"2023-11-15 08:00:01\","
															+ "\"aftersales_address\":{\"aftersales_address_id\":1,"
															+ "\"aftersales_address\":\"中关村大街1号\",\"aftersales_contact\":\"王五\","
															+ "\"aftersales_mobile\":\"13912345678\",\"is_default\":1},"
															+ "\"detail\":[{\"goods_id\":1001,\"item_bn\":\"A1\",\"item_name\":\"示例商品\","
															+ "\"item_pic\":\"https://example.test/key.jpg\",\"num\":1,\"refund_fee\":100,"
															+ "\"refund_point\":0,\"aftersales_type\":\"REFUND_GOODS\",\"progress\":2,"
															+ "\"aftersales_status\":1,\"create_time\":\"2023-11-15 08:00:01\","
															+ "\"update_time\":\"2023-11-15 08:00:01\",\"auto_refuse_time\":\"\"}]}]}}"),
									@ExampleObject(
											name = "success-empty",
											summary = "无售后单",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":0,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},\"list\":[]}}"),
									@ExampleObject(
											name = "fail-page-size",
											summary = "page_size 解析后 ≥500",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\",\"message\":\"每页的大小最大为500！\",\"data\":null}"),
									@ExampleObject(
											name = "fail-member-not-found",
											summary = "mobile 查无会员",
											value =
													"{\"status\":\"fail\",\"code\":\"E5331\",\"message\":\"会员找不到\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@Activated(routeAlias = "aftersales.list")
	@GetMapping(value = "/ecx.aftersales.get", name = "开放接口售后单列表")
	public Map<String, Object> getAftersalesList(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(description = "页码；缺省 1，<1 归一 1（PHP 整型解析，body 同名键优先）", example = "1")
					@RequestParam(name = "page", required = false)
					String pageParam,
			@Parameter(
							description = "每页条数；缺省 20，<20 归一 20；≥500 报「每页的大小最大为500！」（body 同名键优先）",
							example = "20")
					@RequestParam(name = "page_size", required = false)
					String pageSizeParam,
			@Parameter(
							description = "会员手机号；PHP-truthy 才生效，须匹配 ^1[345789][0-9]{9}$，按会员过滤（body 同名键优先）",
							example = "13800000000")
					@RequestParam(name = "mobile", required = false)
					String mobileParam,
			@Parameter(
							description = "create_time 下界（≥）；yyyy-MM-dd[ HH:mm:ss]，PHP-truthy 才生效（body 同名键优先）",
							example = "2023-11-15")
					@RequestParam(name = "time_begin", required = false)
					String timeBeginParam,
			@Parameter(
							description = "create_time 上界（≤）；语义同 time_begin（body 同名键优先）",
							example = "2023-11-16")
					@RequestParam(name = "time_end", required = false)
					String timeEndParam,
			@RequestBody(
							description = "可选 JSON body；字段与 query 同名双通道（取原始值）；分页/过滤语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxAftersalesListBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2AftersalesListParams.PageSpec pageSpec =
				OpenapiThirdApiV2AftersalesListParams.resolve(pageParam, pageSizeParam, body);

		String mobileRaw = OpenapiRequestParams.originalString(mobileParam, body, "mobile");
		String timeBeginRaw = OpenapiRequestParams.originalString(timeBeginParam, body, "time_begin");
		String timeEndRaw = OpenapiRequestParams.originalString(timeEndParam, body, "time_end");

		return aftersalesListPort.list(
				companyId,
				pageSpec.page(),
				pageSpec.pageSize(),
				OpenapiMemberQueryParams.isPhpTruthy(mobileRaw),
				mobileRaw,
				OpenapiMemberQueryParams.isPhpTruthy(timeBeginRaw),
				timeBeginRaw,
				OpenapiMemberQueryParams.isPhpTruthy(timeEndRaw),
				timeEndRaw);
	}

	@Operation(
			summary = "开放接口增量售后单列表",
			description =
					"按签名鉴权租户 company_id 分页返回增量售后单（update_time 倒序）："
							+ " data 结构与全量列表一致（total_count/is_last_page/pager/list，行 18 键，"
							+ " 地址默认回退、sendconfirm_data 透传等语义见 EcxAftersalesListRowDoc）。"
							+ " 与全量差异：start_modified/end_modified 作用于 update_time 闭区间（≥/≤，strtotime，"
							+ " 解析失败按 0，双端下界>上界报 E5320）；另支持 aftersales_type（原样等值，无枚举校验）、"
							+ " aftersales_status/progress（整数解析非法按 0；PHP-truthy 门槛使 \"0\" 无法过滤进度 0）。"
							+ " 分页与 mobile 校验同全量：page_size 解析后 ≥500 报 E0001（含 500）、<20 归一 20；"
							+ " mobile 须匹配 ^1[345789][0-9]{9}$（E5320），查无会员 E5331。"
							+ " 八参数双通道（body 含键取原始值，否则用 query）。返回 Map 由 Advice 包装（message 固定「成功」）。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.aftersales.incr.get, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.aftersales.incr.get.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxAftersalesListDataDoc"
								+ "（无数据时 total_count=0、is_last_page=1、list=[]）；"
								+ " 业务失败：status=fail, code=E0001 每页的大小最大为500！ / E5320 请填写正确的手机号·"
								+ "开始时间不能大于结束时间 / E5331 会员找不到，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxAftersalesIncrListFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxAftersalesIncrListSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功（update_time 倒序）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":1,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},"
															+ "\"list\":[{\"aftersales_bn\":\"202605101200001\",\"order_id\":\"88001\","
															+ "\"aftersales_type\":\"REFUND_GOODS\",\"aftersales_status\":1,\"progress\":2,"
															+ "\"refund_fee\":100,\"refund_point\":0,\"reason\":\"测试售后\","
															+ "\"description\":null,\"evidence_pic\":[],\"refuse_reason\":null,\"memo\":null,"
															+ "\"sendback_data\":{\"corp_code\":\"SF\",\"logi_no\":\"1234567890\","
															+ "\"receiver_address\":\"\",\"receiver_mobile\":\"\"},\"sendconfirm_data\":null,"
															+ "\"create_time\":\"2023-11-15 08:00:01\",\"update_time\":\"2023-11-15 08:00:01\","
															+ "\"aftersales_address\":{\"aftersales_address_id\":1,"
															+ "\"aftersales_address\":\"中关村大街1号\",\"aftersales_contact\":\"王五\","
															+ "\"aftersales_mobile\":\"13912345678\",\"is_default\":1},"
															+ "\"detail\":[{\"goods_id\":1001,\"item_bn\":\"A1\",\"item_name\":\"示例商品\","
															+ "\"item_pic\":\"https://example.test/key.jpg\",\"num\":1,\"refund_fee\":100,"
															+ "\"refund_point\":0,\"aftersales_type\":\"REFUND_GOODS\",\"progress\":2,"
															+ "\"aftersales_status\":1,\"create_time\":\"2023-11-15 08:00:01\","
															+ "\"update_time\":\"2023-11-15 08:00:01\",\"auto_refuse_time\":\"\"}]}]}}"),
									@ExampleObject(
											name = "success-empty",
											summary = "无增量售后单",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":0,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},\"list\":[]}}"),
									@ExampleObject(
											name = "fail-time-range",
											summary = "start_modified > end_modified（双端生效）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5320\","
															+ "\"message\":\"开始时间不能大于结束时间\",\"data\":null}"),
									@ExampleObject(
											name = "fail-page-size",
											summary = "page_size 解析后 ≥500",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\",\"message\":\"每页的大小最大为500！\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@GetMapping(value = "/ecx.aftersales.incr.get", name = "开放接口增量售后单列表")
	public Map<String, Object> getIncrAftersalesList(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(description = "页码；缺省 1，<1 归一 1（PHP 整型解析，body 同名键优先）", example = "1")
					@RequestParam(name = "page", required = false)
					String pageParam,
			@Parameter(
							description = "每页条数；缺省 20，<20 归一 20；≥500 报「每页的大小最大为500！」（body 同名键优先）",
							example = "20")
					@RequestParam(name = "page_size", required = false)
					String pageSizeParam,
			@Parameter(
							description = "会员手机号；PHP-truthy 才生效，须匹配 ^1[345789][0-9]{9}$，按会员过滤（body 同名键优先）",
							example = "13800000000")
					@RequestParam(name = "mobile", required = false)
					String mobileParam,
			@Parameter(
							description = "update_time 下界（≥）；yyyy-MM-dd[ HH:mm:ss]，PHP-truthy 才生效（body 同名键优先）",
							example = "2023-11-15")
					@RequestParam(name = "start_modified", required = false)
					String startModifiedParam,
			@Parameter(
							description = "update_time 上界（≤）；语义同 start_modified（body 同名键优先）",
							example = "2023-11-16")
					@RequestParam(name = "end_modified", required = false)
					String endModifiedParam,
			@Parameter(
							description = "售后类型精确过滤；原样等值无枚举校验（body 同名键优先）",
							schema = @Schema(allowableValues = {"ONLY_REFUND", "REFUND_GOODS", "EXCHANGING_GOODS"}),
							example = "REFUND_GOODS")
					@RequestParam(name = "aftersales_type", required = false)
					String aftersalesTypeParam,
			@Parameter(
							description = "售后状态精确过滤；整数解析非法按 0（body 同名键优先）",
							example = "1")
					@RequestParam(name = "aftersales_status", required = false)
					String aftersalesStatusParam,
			@Parameter(
							description = "处理进度精确过滤；整数解析非法按 0；\"0\" 视为未提供（body 同名键优先）",
							example = "2")
					@RequestParam(name = "progress", required = false)
					String progressParam,
			@RequestBody(
							description = "可选 JSON body；字段与 query 同名双通道（取原始值）；分页/过滤语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxAftersalesIncrListBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2AftersalesListParams.PageSpec pageSpec =
				OpenapiThirdApiV2AftersalesListParams.resolve(pageParam, pageSizeParam, body);

		String mobileRaw = OpenapiRequestParams.originalString(mobileParam, body, "mobile");
		String startModifiedRaw =
				OpenapiRequestParams.originalString(startModifiedParam, body, "start_modified");
		String endModifiedRaw =
				OpenapiRequestParams.originalString(endModifiedParam, body, "end_modified");
		String aftersalesTypeRaw =
				OpenapiRequestParams.originalString(aftersalesTypeParam, body, "aftersales_type");
		String aftersalesStatusRaw =
				OpenapiRequestParams.originalString(aftersalesStatusParam, body, "aftersales_status");
		String progressRaw = OpenapiRequestParams.originalString(progressParam, body, "progress");

		return aftersalesIncrListPort.incrList(
				companyId,
				pageSpec.page(),
				pageSpec.pageSize(),
				OpenapiMemberQueryParams.isPhpTruthy(mobileRaw),
				mobileRaw,
				OpenapiMemberQueryParams.isPhpTruthy(startModifiedRaw),
				startModifiedRaw,
				OpenapiMemberQueryParams.isPhpTruthy(endModifiedRaw),
				endModifiedRaw,
				OpenapiMemberQueryParams.isPhpTruthy(aftersalesTypeRaw),
				aftersalesTypeRaw,
				OpenapiMemberQueryParams.isPhpTruthy(aftersalesStatusRaw),
				aftersalesStatusRaw,
				OpenapiMemberQueryParams.isPhpTruthy(progressRaw),
				progressRaw);
	}

	@Operation(
			summary = "开放接口退款单列表",
			description =
					"按签名鉴权租户 company_id 分页返回退款单（create_time 倒序）："
							+ " data 含 total_count、is_last_page（1/0）、pager（page/page_size 归一化回显）与 list（15 键/行）。"
							+ " 分页：page 缺省 1（<1 归一 1）；page_size 缺省 20、<20 归一 20；解析后 ≥500 报 E0001"
							+ "「每页的大小最大为500！」（500 本身也报错）。"
							+ " mobile PHP-truthy 才生效：须匹配 ^1[345789][0-9]{9}$（E5340），查无会员 E5331，命中按 user_id 过滤；"
							+ " time_begin/time_end PHP-truthy + yyyy-MM-dd[ HH:mm:ss] 转 Unix 秒做 create_time 闭区间（≥/≤），"
							+ " 解析失败按 0；双端生效且下界>上界报 E5340「开始时间不能大于结束时间」。"
							+ " 行 15 键：refund_bn/order_id 字符串形态，aftersales_bn 售前取消退款为 null，金额单位分，"
							+ " 时间 yyyy-MM-dd HH:mm:ss（null/≤0 按 epoch 0）；refund_success_time 空值按 epoch 0"
							+ "（区别于详情接口的当前时间）；无 return_freight、无 cancel_reason（详情接口才有）。"
							+ " 五参数双通道（body 含键取原始值，否则用 query）。返回 Map 由 Advice 包装（message 固定「成功」）。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.refund.get, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.refund.get.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxRefundListDataDoc"
								+ "（无数据时 total_count=0、is_last_page=1、list=[]）；"
								+ " 业务失败：status=fail, code=E0001 每页的大小最大为500！ / E5340 请填写正确的手机号·"
								+ "开始时间不能大于结束时间 / E5331 会员找不到，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxRefundListFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxRefundListSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功（售后退款单，create_time 倒序）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":1,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},"
															+ "\"list\":[{\"refund_bn\":\"2202608258337278667\","
															+ "\"aftersales_bn\":\"202605101200001\",\"order_id\":\"88001\","
															+ "\"trade_id\":\"\",\"refund_channel\":\"original\","
															+ "\"refund_status\":\"SUCCESS\",\"refund_fee\":100,\"refunded_fee\":100,"
															+ "\"pay_type\":\"\",\"refund_id\":null,\"refund_point\":0,"
															+ "\"refunded_point\":0,\"refund_success_time\":\"2023-11-15 08:00:01\","
															+ "\"create_time\":\"2023-11-15 08:00:01\","
															+ "\"update_time\":\"2023-11-15 08:00:01\"}]}}"),
									@ExampleObject(
											name = "success-presale-cancel",
											summary = "售前取消退款（aftersales_bn=null；未退款成功，refund_success_time 按 epoch 0）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":1,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},"
															+ "\"list\":[{\"refund_bn\":\"2202608258337278667\","
															+ "\"aftersales_bn\":null,\"order_id\":\"88001\","
															+ "\"trade_id\":\"\",\"refund_channel\":\"original\","
															+ "\"refund_status\":\"READY\",\"refund_fee\":100,\"refunded_fee\":0,"
															+ "\"pay_type\":\"\",\"refund_id\":null,\"refund_point\":0,"
															+ "\"refunded_point\":0,\"refund_success_time\":\"1970-01-01 08:00:00\","
															+ "\"create_time\":\"2023-11-15 08:00:01\","
															+ "\"update_time\":\"2023-11-15 08:00:01\"}]}}"),
									@ExampleObject(
											name = "success-empty",
											summary = "无退款单",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":0,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},\"list\":[]}}"),
									@ExampleObject(
											name = "fail-page-size",
											summary = "page_size 解析后 ≥500",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\",\"message\":\"每页的大小最大为500！\",\"data\":null}"),
									@ExampleObject(
											name = "fail-mobile",
											summary = "mobile 格式非法（E5340）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5340\",\"message\":\"请填写正确的手机号\",\"data\":null}"),
									@ExampleObject(
											name = "fail-time-range",
											summary = "time_begin > time_end（双端生效，E5340）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5340\","
															+ "\"message\":\"开始时间不能大于结束时间\",\"data\":null}"),
									@ExampleObject(
											name = "fail-member-not-found",
											summary = "mobile 查无会员（E5331）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5331\",\"message\":\"会员找不到\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@GetMapping(value = "/ecx.refund.get", name = "开放接口退款单列表")
	public Map<String, Object> getRefundList(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(description = "页码；缺省 1，<1 归一 1（PHP 整型解析，body 同名键优先）", example = "1")
					@RequestParam(name = "page", required = false)
					String pageParam,
			@Parameter(
							description = "每页条数；缺省 20，<20 归一 20；≥500 报「每页的大小最大为500！」（body 同名键优先）",
							example = "20")
					@RequestParam(name = "page_size", required = false)
					String pageSizeParam,
			@Parameter(
							description = "会员手机号；PHP-truthy 才生效，须匹配 ^1[345789][0-9]{9}$，按会员过滤（body 同名键优先）",
							example = "13800000000")
					@RequestParam(name = "mobile", required = false)
					String mobileParam,
			@Parameter(
							description = "create_time 下界（≥）；yyyy-MM-dd[ HH:mm:ss]，PHP-truthy 才生效（body 同名键优先）",
							example = "2023-11-15")
					@RequestParam(name = "time_begin", required = false)
					String timeBeginParam,
			@Parameter(
							description = "create_time 上界（≤）；语义同 time_begin（body 同名键优先）",
							example = "2023-11-16")
					@RequestParam(name = "time_end", required = false)
					String timeEndParam,
			@RequestBody(
							description = "可选 JSON body；字段与 query 同名双通道（取原始值）；分页/过滤语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxRefundListBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2AftersalesListParams.PageSpec pageSpec =
				OpenapiThirdApiV2AftersalesListParams.resolve(pageParam, pageSizeParam, body);

		String mobileRaw = OpenapiRequestParams.originalString(mobileParam, body, "mobile");
		String timeBeginRaw = OpenapiRequestParams.originalString(timeBeginParam, body, "time_begin");
		String timeEndRaw = OpenapiRequestParams.originalString(timeEndParam, body, "time_end");

		return refundListPort.list(
				companyId,
				pageSpec.page(),
				pageSpec.pageSize(),
				OpenapiMemberQueryParams.isPhpTruthy(mobileRaw),
				mobileRaw,
				OpenapiMemberQueryParams.isPhpTruthy(timeBeginRaw),
				timeBeginRaw,
				OpenapiMemberQueryParams.isPhpTruthy(timeEndRaw),
				timeEndRaw);
	}

	@Operation(
			summary = "开放接口增量退款单列表",
			description =
					"按签名鉴权租户 company_id 分页返回增量退款单（update_time 倒序）："
							+ " data 结构与全量退款列表一致（total_count/is_last_page/pager/list，行 15 键，"
							+ " 键集与语义见 EcxRefundListRowDoc：无 return_freight、无 cancel_reason，"
							+ " refund_success_time 空值按 epoch 0）。"
							+ " 与全量差异：start_modified/end_modified 作用于 update_time 闭区间（≥/≤，"
							+ " yyyy-MM-dd[ HH:mm:ss] 转 Unix 秒，解析失败按 0，双端生效且下界>上界报 E5340"
							+ "「开始时间不能大于结束时间」）。"
							+ " 分页与 mobile 校验同全量：page 缺省 1（<1 归一 1）；page_size 缺省 20、<20 归一 20，"
							+ " 解析后 ≥500 报 E0001「每页的大小最大为500！」（含 500）；"
							+ " mobile PHP-truthy 才生效，须匹配 ^1[345789][0-9]{9}$（E5340），查无会员 E5331，"
							+ " 命中按 user_id 过滤。"
							+ " 五参数双通道（body 含键取原始值，否则用 query）。返回 Map 由 Advice 包装（message 固定「成功」）。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.refund.incr.get, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.refund.incr.get.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxRefundListDataDoc"
								+ "（无数据时 total_count=0、is_last_page=1、list=[]）；"
								+ " 业务失败：status=fail, code=E0001 每页的大小最大为500！ / E5340 请填写正确的手机号·"
								+ "开始时间不能大于结束时间 / E5331 会员找不到，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxRefundIncrListFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxRefundIncrListSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功（update_time 倒序）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":1,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},"
															+ "\"list\":[{\"refund_bn\":\"2202608258337278667\","
															+ "\"aftersales_bn\":\"202605101200001\",\"order_id\":\"88001\","
															+ "\"trade_id\":\"\",\"refund_channel\":\"original\","
															+ "\"refund_status\":\"SUCCESS\",\"refund_fee\":100,\"refunded_fee\":100,"
															+ "\"pay_type\":\"\",\"refund_id\":null,\"refund_point\":0,"
															+ "\"refunded_point\":0,\"refund_success_time\":\"2023-11-15 08:00:01\","
															+ "\"create_time\":\"2023-11-15 08:00:01\","
															+ "\"update_time\":\"2023-11-15 08:00:01\"}]}}"),
									@ExampleObject(
											name = "success-empty",
											summary = "无增量退款单",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"total_count\":0,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},\"list\":[]}}"),
									@ExampleObject(
											name = "fail-time-range",
											summary = "start_modified > end_modified（双端生效，E5340）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5340\","
															+ "\"message\":\"开始时间不能大于结束时间\",\"data\":null}"),
									@ExampleObject(
											name = "fail-page-size",
											summary = "page_size 解析后 ≥500",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\",\"message\":\"每页的大小最大为500！\",\"data\":null}"),
									@ExampleObject(
											name = "fail-member-not-found",
											summary = "mobile 查无会员（E5331）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5331\",\"message\":\"会员找不到\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@GetMapping(value = "/ecx.refund.incr.get", name = "开放接口增量退款单列表")
	public Map<String, Object> getIncrRefundList(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(description = "页码；缺省 1，<1 归一 1（PHP 整型解析，body 同名键优先）", example = "1")
					@RequestParam(name = "page", required = false)
					String pageParam,
			@Parameter(
							description = "每页条数；缺省 20，<20 归一 20；≥500 报「每页的大小最大为500！」（body 同名键优先）",
							example = "20")
					@RequestParam(name = "page_size", required = false)
					String pageSizeParam,
			@Parameter(
							description = "会员手机号；PHP-truthy 才生效，须匹配 ^1[345789][0-9]{9}$，按会员过滤（body 同名键优先）",
							example = "13800000000")
					@RequestParam(name = "mobile", required = false)
					String mobileParam,
			@Parameter(
							description = "update_time 下界（≥）；yyyy-MM-dd[ HH:mm:ss]，PHP-truthy 才生效（body 同名键优先）",
							example = "2023-11-15")
					@RequestParam(name = "start_modified", required = false)
					String startModifiedParam,
			@Parameter(
							description = "update_time 上界（≤）；语义同 start_modified（body 同名键优先）",
							example = "2023-11-16")
					@RequestParam(name = "end_modified", required = false)
					String endModifiedParam,
			@RequestBody(
							description = "可选 JSON body；字段与 query 同名双通道（取原始值）；分页/过滤语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxRefundIncrListBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2AftersalesListParams.PageSpec pageSpec =
				OpenapiThirdApiV2AftersalesListParams.resolve(pageParam, pageSizeParam, body);

		String mobileRaw = OpenapiRequestParams.originalString(mobileParam, body, "mobile");
		String startModifiedRaw =
				OpenapiRequestParams.originalString(startModifiedParam, body, "start_modified");
		String endModifiedRaw =
				OpenapiRequestParams.originalString(endModifiedParam, body, "end_modified");

		return refundIncrListPort.incrList(
				companyId,
				pageSpec.page(),
				pageSpec.pageSize(),
				OpenapiMemberQueryParams.isPhpTruthy(mobileRaw),
				mobileRaw,
				OpenapiMemberQueryParams.isPhpTruthy(startModifiedRaw),
				startModifiedRaw,
				OpenapiMemberQueryParams.isPhpTruthy(endModifiedRaw),
				endModifiedRaw);
	}

	@Operation(
			summary = "开放接口售后详情",
			description =
					"按签名鉴权租户 company_id 查询单个售后单详情：aftersales_bn 必填非空白（trim 后精确匹配），"
							+ " 缺失报 E5001「请填写售后单号」，查无/同租户单号重复报 E5320「没有售后信息」。"
							+ " data 18 键：主单字段（aftersales_bn/order_id 字符串形态、金额分、时间 yyyy-MM-dd HH:mm:ss）+"
							+ " evidence_pic（JSON 数组解析，非法恒 []）+ sendback_data（回寄物流 4 键对象，未提交为 null）+"
							+ " aftersales_address（主单 JSON 原样解析，不查默认地址，无地址为 {}；线上审核 4 键、"
							+ " 店务按数量申请另含 aftersales_name/aftersales_hours）+ detail（明细 13 键/行，create_time 倒序，"
							+ " 行 update_time 实取 create_time——PHP 旧契约）+ refund_bn（关联退款单号字符串，无/重复为 \"\"）。"
							+ " 与列表行差异：无 sendconfirm_data。返回 Map 由 Advice 包装（message 固定「成功」）。"
							+ " aftersales_bn 双通道（body 含键取原始值不 trim，否则用 query）。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.aftersales.detail.get, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.aftersales.detail.get.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxAftersalesDetailGetDataDoc；"
								+ " 业务失败：status=fail, code=E5001 请填写售后单号 / E5320 没有售后信息，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxAftersalesDetailGetFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxAftersalesDetailGetSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功（已回寄，含地址与物流）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"aftersales_bn\":\"202605101200001\",\"order_id\":\"88001\","
															+ "\"aftersales_type\":\"REFUND_GOODS\",\"aftersales_status\":1,\"progress\":2,"
															+ "\"refund_fee\":100,\"refund_point\":0,\"reason\":\"测试售后\","
															+ "\"description\":null,\"evidence_pic\":[],\"refuse_reason\":null,\"memo\":null,"
															+ "\"sendback_data\":{\"corp_code\":\"SF\",\"logi_no\":\"1234567890\","
															+ "\"receiver_address\":\"\",\"receiver_mobile\":\"\"},"
															+ "\"create_time\":\"2023-11-15 08:00:01\",\"update_time\":\"2023-11-15 08:00:01\","
															+ "\"aftersales_address\":{\"aftersales_address_id\":1,\"aftersales_contact\":\"王五\","
															+ "\"aftersales_mobile\":\"13912345678\","
															+ "\"aftersales_address\":\"北京市北京市海淀区中关村大街1号\"},"
															+ "\"detail\":[{\"goods_id\":1001,\"item_bn\":\"A1\",\"item_name\":\"示例商品\","
															+ "\"item_pic\":\"https://example.test/key.jpg\",\"num\":1,\"refund_fee\":100,"
															+ "\"refund_point\":0,\"aftersales_type\":\"REFUND_GOODS\",\"progress\":2,"
															+ "\"aftersales_status\":1,\"create_time\":\"2023-11-15 08:00:01\","
															+ "\"update_time\":\"2023-11-15 08:00:01\",\"auto_refuse_time\":\"\"}],"
															+ "\"refund_bn\":\"2202608258337278667\"}}"),
									@ExampleObject(
											name = "success-minimal",
											summary = "刚申请（未回寄、无地址、无退款单）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"aftersales_bn\":\"202605101200001\",\"order_id\":\"88001\","
															+ "\"aftersales_type\":\"ONLY_REFUND\",\"aftersales_status\":0,\"progress\":0,"
															+ "\"refund_fee\":100,\"refund_point\":0,\"reason\":\"测试售后\","
															+ "\"description\":null,\"evidence_pic\":[],\"refuse_reason\":null,\"memo\":null,"
															+ "\"sendback_data\":null,\"create_time\":\"2023-11-15 08:00:01\","
															+ "\"update_time\":\"2023-11-15 08:00:01\",\"aftersales_address\":{},"
															+ "\"detail\":[],\"refund_bn\":\"\"}}"),
									@ExampleObject(
											name = "fail-missing-bn",
											summary = "aftersales_bn 缺失/空白",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\",\"message\":\"请填写售后单号\",\"data\":null}"),
									@ExampleObject(
											name = "fail-not-found",
											summary = "查无售后单（或同租户单号重复）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5320\",\"message\":\"没有售后信息\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.aftersales.detail.get", name = "开放接口售后详情")
	public Map<String, Object> getAftersalesDetail(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "售后单号；必填非空白（查询时 trim），精确匹配（body 同名键优先，原始值不 trim）",
							required = true,
							example = "202605101200001")
					@RequestParam(name = "aftersales_bn", required = false)
					String aftersalesBnParam,
			@RequestBody(
							description = "可选 JSON body；aftersales_bn 与 query 同名双通道（取原始值不 trim）",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxAftersalesDetailGetBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		return aftersalesDetailPort.getAftersalesDetail(
				companyId,
				OpenapiRequestParams.originalString(aftersalesBnParam, body, "aftersales_bn"));
	}

	@Operation(
			summary = "开放接口退款单详情",
			description =
					"鉴权：openapi 签名（公网网关校验后转发 internal；直连 internal 无鉴权上下文报 E4004）。"
							+ " 1 键 query 与 JSON body 同名双通道：body 含键优先（值 null 视为未传），取值不 trim，服务内 trim。"
							+ " 按租户 + refund_bn 等值查 aftersales_refund（无 LIMIT 1——多行命中 TooManyResults → E5340）。"
							+ " data 16 固定键 + 条件键 cancel_reason（仅售前取消退款 aftersales_bn null/0 时携带，"
							+ " 取 cancel_orders.cancel_reason）；refund_success_time 空值返回当前时间"
							+ "（区别于列表行的 epoch 0）；create/update_time 空值按 epoch 0（1970-01-01 08:00:00）。"
							+ " 副作用：无（只读）。"
							+ " 失败 HTTP 仍 200：E5001「请填写退款单号」/ E5340（查无 → PHP 文案"
							+ " 「Undefined array key \\\"refund_channel\\\"」；其他异常 message 透传），data=null。",
			operationId = "ecx.refund.detail.get.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxRefundDetailGetDataDoc"
								+ "（16 固定键 + 条件键 cancel_reason）；"
								+ " 业务失败：status=fail，code=E5001/E5340，data=null，见 EcxRefundDetailGetFailEnvelopeDoc；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。",
				content =
						@Content(
								schema = @Schema(implementation = EcxRefundDetailGetSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success-aftersales-refund",
											summary = "售后退款单（无 cancel_reason 键；时间为格式演示值）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\",\"data\":{"
															+ "\"refund_bn\":\"2202608258337278667\","
															+ "\"aftersales_bn\":\"202605101200001\",\"order_id\":\"88001\","
															+ "\"trade_id\":\"\",\"refund_channel\":\"original\","
															+ "\"refund_status\":\"SUCCESS\",\"return_freight\":0,"
															+ "\"refund_fee\":100,\"refunded_fee\":100,\"pay_type\":\"\","
															+ "\"refund_id\":null,\"refund_point\":0,\"refunded_point\":0,"
															+ "\"refund_success_time\":\"2023-11-15 08:00:01\","
															+ "\"create_time\":\"2023-11-15 08:00:01\","
															+ "\"update_time\":\"2023-11-15 08:00:01\"}}"),
									@ExampleObject(
											name = "success-presale-cancel",
											summary = "售前取消退款（aftersales_bn=null，携带 cancel_reason）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\",\"data\":{"
															+ "\"refund_bn\":\"2202608258337278667\",\"aftersales_bn\":null,"
															+ "\"order_id\":\"88001\",\"trade_id\":\"\","
															+ "\"refund_channel\":\"original\",\"refund_status\":\"READY\","
															+ "\"return_freight\":0,\"refund_fee\":100,\"refunded_fee\":0,"
															+ "\"pay_type\":\"\",\"refund_id\":null,\"refund_point\":0,"
															+ "\"refunded_point\":0,\"refund_success_time\":\"2023-11-15 08:00:01\","
															+ "\"create_time\":\"2023-11-15 08:00:01\","
															+ "\"update_time\":\"2023-11-15 08:00:01\","
															+ "\"cancel_reason\":\"客户现在不想购买\"}}"),
									@ExampleObject(
											name = "fail-missing-refund-bn",
											summary = "缺少退款单号（E5001）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5001\",\"message\":\"请填写退款单号\",\"data\":null}"),
									@ExampleObject(
											name = "fail-not-found",
											summary = "查无此退款单（E5340，PHP 错误文案对齐）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5340\","
															+ "\"message\":\"Undefined array key \\\"refund_channel\\\"\",\"data\":null}"),
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
			content = @Content(schema = @Schema(implementation = EcxRefundDetailGetBodyDoc.class)))
	@PostMapping(value = "/ecx.refund.detail.get", name = "开放接口退款单详情")
	public Map<String, Object> getRefundDetail(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description =
									"退款单号（与 body 同名键双通道，body 含键优先）；必填；查无 → E5340"
											+ "「Undefined array key \\\"refund_channel\\\"」",
							required = true,
							example = "2202608258337278667")
					@RequestParam(name = "refund_bn", required = false)
					String refundBnParam,
			@Parameter(hidden = true) @FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		return refundDetailPort.getRefundDetail(
				companyId,
				OpenapiRequestParams.originalString(refundBnParam, body, "refund_bn"));
	}
}
