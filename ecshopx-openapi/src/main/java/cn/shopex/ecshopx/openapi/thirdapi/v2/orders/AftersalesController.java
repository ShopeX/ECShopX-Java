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

	@Activated(routeAlias = "aftersales.list")
	@GetMapping(value = "/ecx.aftersales.get", name = "开放接口售后单列表")
	public Map<String, Object> getAftersalesList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "time_begin", required = false) String timeBeginParam,
			@RequestParam(name = "time_end", required = false) String timeEndParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
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

	@GetMapping(value = "/ecx.aftersales.incr.get", name = "开放接口增量售后单列表")
	public Map<String, Object> getIncrAftersalesList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "start_modified", required = false) String startModifiedParam,
			@RequestParam(name = "end_modified", required = false) String endModifiedParam,
			@RequestParam(name = "aftersales_type", required = false) String aftersalesTypeParam,
			@RequestParam(name = "aftersales_status", required = false) String aftersalesStatusParam,
			@RequestParam(name = "progress", required = false) String progressParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
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

	@GetMapping(value = "/ecx.refund.get", name = "开放接口退款单列表")
	public Map<String, Object> getRefundList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "time_begin", required = false) String timeBeginParam,
			@RequestParam(name = "time_end", required = false) String timeEndParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
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

	@GetMapping(value = "/ecx.refund.incr.get", name = "开放接口增量退款单列表")
	public Map<String, Object> getIncrRefundList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@RequestParam(name = "mobile", required = false) String mobileParam,
			@RequestParam(name = "start_modified", required = false) String startModifiedParam,
			@RequestParam(name = "end_modified", required = false) String endModifiedParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
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

	@PostMapping(value = "/ecx.refund.detail.get", name = "开放接口退款单详情")
	public Map<String, Object> getRefundDetail(
			HttpServletRequest request,
			@RequestParam(name = "refund_bn", required = false) String refundBnParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		return refundDetailPort.getRefundDetail(
				companyId,
				OpenapiRequestParams.originalString(refundBnParam, body, "refund_bn"));
	}
}
