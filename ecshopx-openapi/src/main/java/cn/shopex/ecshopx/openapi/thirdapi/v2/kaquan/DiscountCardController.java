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

package cn.shopex.ecshopx.openapi.thirdapi.v2.kaquan;

import cn.shopex.ecshopx.common.annotation.OpenapiResponse;
import cn.shopex.ecshopx.common.openapi.OpenapiDiscountCardV2ListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiDiscountCardV2SendPort;
import cn.shopex.ecshopx.common.openapi.OpenapiDiscountCardV2UserDiscountListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiEnvelope;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v2.kaquan.doc.EcxDiscountcardV2ListBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.kaquan.doc.EcxDiscountcardV2ListSuccessEnvelopeDoc;
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

@Tag(name = "开放接口-优惠券V2")
@OpenapiResponse
@RestController("openapiV2DiscountCard")
@RequestMapping("/api/openapi/internal/v2")
public class DiscountCardController extends OpenapiBaseController {

	private final OpenapiDiscountCardV2ListPort discountCardV2ListPort;
	private final OpenapiDiscountCardV2SendPort discountCardV2SendPort;
	private final OpenapiDiscountCardV2UserDiscountListPort userDiscountListPort;

	public DiscountCardController(
			OpenapiDiscountCardV2ListPort discountCardV2ListPort,
			OpenapiDiscountCardV2SendPort discountCardV2SendPort,
			OpenapiDiscountCardV2UserDiscountListPort userDiscountListPort) {
		this.discountCardV2ListPort = discountCardV2ListPort;
		this.discountCardV2SendPort = discountCardV2SendPort;
		this.userDiscountListPort = userDiscountListPort;
	}

	@Operation(
			summary = "开放接口获取优惠券列表",
			description =
					"按签名鉴权租户 company_id 分页返回全部卡券（kaquan_discount_cards，无类型/状态过滤，created 倒序）："
							+ " data 含 total_count、is_last_page（1/0）、pager 与 list（15 键/行，恒精简形态）。"
							+ " 行内金额 reduce_cost/least_cost/most_cost 为元（分÷100 HALF_UP 取整字符串）；"
							+ " begin_time/end_time 为 begin_date/end_date Unix 秒字符串化（不格式化日期，与 v1 不同）；"
							+ " use_all_items 由 use_bound 映射（0→true/1→false/2→category/3→tag/4→brand），"
							+ " rel_data 对应商品名列表/分类串（逗号段排序后 / 连接单元素）/标签品牌逗号拆分；"
							+ " left_quantity=max(quantity-已领取,0)；description null 回退 \"0\"（PHP 旧契约）。"
							+ " 分页：page 缺省 1（<1 归一 1）；page_size 缺省 20、<20 归一 20；解析后 ≥500 报"
							+ " E5002「每页的大小最大为500！」（500 本身也报错）。两参数双通道（body 含键取原始值）。"
							+ " 成功 message 固定「操作成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.discountcard.list, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.discountcard.list.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态。"
								+ " 成功：status=success, code=E0000, message=操作成功，data 见 EcxDiscountcardV2ListDataDoc"
								+ "（无卡券时 total_count=0、is_last_page=1、list=[]）；"
								+ " 分页失败：status=fail, code=E5002 每页的大小最大为500！，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxDiscountcardV2ListFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxDiscountcardV2ListSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功（全场通用代金券）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"操作成功\","
															+ "\"data\":{\"total_count\":1,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},"
															+ "\"list\":[{\"card_id\":3,\"card_type\":\"cash\",\"title\":\"满减券\","
															+ "\"description\":\"0\",\"discount\":\"0\",\"reduce_cost\":\"0\","
															+ "\"date_type\":\"DATE_TYPE_FIX_TERM\",\"begin_time\":\"\",\"end_time\":\"\","
															+ "\"fixed_term\":\"30\",\"least_cost\":\"0\",\"most_cost\":\"999999\","
															+ "\"use_all_items\":\"true\",\"rel_data\":[],\"left_quantity\":100}]}}"),
									@ExampleObject(
											name = "success-empty",
											summary = "无卡券",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"操作成功\","
															+ "\"data\":{\"total_count\":0,\"is_last_page\":1,"
															+ "\"pager\":{\"page\":1,\"page_size\":20},\"list\":[]}}"),
									@ExampleObject(
											name = "fail-page-size",
											summary = "page_size 解析后 ≥500",
											value =
													"{\"status\":\"fail\",\"code\":\"E5002\",\"message\":\"每页的大小最大为500！\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.discountcard.list", name = "开放接口获取优惠券列表")
	public OpenapiEnvelope getDiscountCardList(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(description = "页码；缺省 1，<1 归一 1（PHP 整型解析，body 同名键优先）", example = "1")
					@RequestParam(name = "page", required = false)
					String pageParam,
			@Parameter(
							description = "每页条数；缺省 20，<20 归一 20；≥500 报「每页的大小最大为500！」（body 同名键优先）",
							example = "20")
					@RequestParam(name = "page_size", required = false)
					String pageSizeParam,
			@RequestBody(
							description = "可选 JSON body；字段与 query 同名双通道（取原始值）；分页语义见 schema",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxDiscountcardV2ListBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2DiscountCardListParams.PageSpec pageSpec =
				OpenapiThirdApiV2DiscountCardListParams.resolve(pageParam, pageSizeParam, body);
		Map<String, Object> data = discountCardV2ListPort.getDiscountCardList(
				companyId, pageSpec.page(), pageSpec.pageSize());
		return new OpenapiEnvelope("success", "E0000", "操作成功", data);
	}

	@PostMapping(value = "/ecx.discountcard.send", name = "开放接口单张优惠券发放")
	public Map<String, Object> userGetCard(
			HttpServletRequest request,
			@RequestParam(name = "plat_account", required = false) String platAccountParam,
			@RequestParam(name = "card_id", required = false) String cardIdParam,
			@RequestParam(name = "source_type", required = false) String sourceTypeParam,
			@RequestParam(name = "activity_name", required = false) String activityNameParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2DiscountCardSendParams.SendParams params =
				OpenapiThirdApiV2DiscountCardSendParams.validate(
						platAccountParam, cardIdParam, sourceTypeParam, activityNameParam, body);
		return discountCardV2SendPort.userSendDiscountCard(
				companyId,
				params.platAccount(),
				params.cardId(),
				params.sourceType(),
				params.activityName());
	}

	@PostMapping(value = "/ecx.userdiscount.list", name = "开放接口会员优惠券列表")
	public Map<String, Object> getUserDiscountList(
			HttpServletRequest request,
			@RequestParam(name = "plat_account", required = false) String platAccountParam,
			@RequestParam(name = "code", required = false) String codeParam,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2DiscountCardUserDiscountListParams.QueryParams params =
				OpenapiThirdApiV2DiscountCardUserDiscountListParams.validate(
						platAccountParam, codeParam, pageParam, pageSizeParam, body);
		return userDiscountListPort.getUserDiscountList(
				companyId, params.platAccount(), params.code(), params.page(), params.pageSize());
	}
}
