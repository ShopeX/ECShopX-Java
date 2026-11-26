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
import cn.shopex.ecshopx.common.openapi.OpenapiDiscountCardDetailPort;
import cn.shopex.ecshopx.common.openapi.OpenapiDiscountCardListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiEnvelope;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxDiscountcardInfoBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxDiscountcardInfoSuccessEnvelopeDoc;
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
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OpenAPI v1 handler scaffold — methods added during migration. */
@Tag(name = "开放接口-卡券模板")
@OpenapiResponse
@RestController("openapiV1DiscountCard")
@RequestMapping("/api/openapi/internal/v1")
public class DiscountCardController extends OpenapiBaseController {

	private final OpenapiDiscountCardDetailPort discountCardDetailPort;
	private final OpenapiDiscountCardListPort discountCardListPort;

	public DiscountCardController(OpenapiDiscountCardDetailPort discountCardDetailPort,
			OpenapiDiscountCardListPort discountCardListPort) {
		this.discountCardDetailPort = discountCardDetailPort;
		this.discountCardListPort = discountCardListPort;
	}

	@Operation(
			summary = "开放接口获取卡券详情",
			description =
					"按签名鉴权得到的租户 company_id 与 card_id 查询卡券模板详情，含关联店铺/商品 SKU 树/标签/品牌聚合。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.discountcard.info, version=1.0）"
							+ " 签名校验后转发到本 internal 路径；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " card_id 双通道：body 含键时以 body 为准（null/空白不回退 query），否则读 query；须为整数字符串。"
							+ " 展示层转换：discount>0 换算为 (100-值)/10 折数；least_cost/reduce_cost 有效值除以 100 转元；"
							+ " begin_time/end_time 整型化、days 取 fixed_term（null 默认 30）；time_limit 转为"
							+ " time_limit_type/time_limit_date；user_tag_ids 不返回。"
							+ " 业务失败均为 status=fail 且 code=E0000（PHP 旧契约）。只读，无写库/异步/第三方副作用。",
			operationId = "ecx.discountcard.info")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态。"
								+ " 成功：status=success, code=E0000, message=success，data 见 EcxDiscountcardInfoDataDoc；"
								+ " 业务失败：status=fail 且 code=E0000（获取卡券的详细信息出错./该优惠券已失效/获取失败.），data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxDiscountcardInfoFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxDiscountcardInfoSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功（字段节选，完整字段见 schema）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"success\","
															+ "\"data\":{\"card_id\":3,\"company_id\":100,\"card_type\":\"cash\","
															+ "\"title\":\"满减券\",\"description\":\"\",\"date_type\":\"DATE_TYPE_FIX_TERM\","
															+ "\"begin_date\":1764115200,\"end_date\":1766707200,"
															+ "\"begin_time\":1764115200,\"end_time\":1766707200,\"days\":30,"
															+ "\"discount\":0,\"least_cost\":0,\"reduce_cost\":0,"
															+ "\"quantity\":100,\"use_scenes\":\"QUICK\",\"use_bound\":0,"
															+ "\"rel_shops_ids\":[],\"rel_distributor_ids\":null,"
															+ "\"distributor_info\":[],\"use_all_distributor\":true,"
															+ "\"rel_item_ids\":[],\"use_all_items\":\"true\","
															+ "\"tag_ids\":[],\"rel_tag_ids\":[],\"tag_list\":[],"
															+ "\"brand_ids\":[],\"rel_brand_ids\":[],\"brand_list\":[],"
															+ "\"grade_ids\":[],\"vip_grade_ids\":[],\"is_active\":false,"
															+ "\"coupon_type\":\"mall\",\"kq_status\":0}}"),
									@ExampleObject(
											name = "fail-card-invalid",
											summary = "券不存在（PHP 旧契约：code=E0000 但 status=fail）",
											value =
													"{\"status\":\"fail\",\"code\":\"E0000\",\"message\":\"该优惠券已失效\",\"data\":null}"),
									@ExampleObject(
											name = "fail-bad-card-id",
											summary = "card_id 缺失或非整数",
											value =
													"{\"status\":\"fail\",\"code\":\"E0000\","
															+ "\"message\":\"获取卡券的详细信息出错.\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.discountcard.info", name = "开放接口获取卡券详情")
	public OpenapiEnvelope getDiscountCardDetail(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "卡券模板 ID（kaquan_discount_cards.card_id）；必填，整数字符串（body 同名键优先）",
							required = true,
							example = "3")
					@RequestParam(name = "card_id", required = false)
					String cardIdParam,
			@RequestBody(
							description =
									"可选 JSON body；card_id 与 query 同名双通道，body 含键时优先"
											+ "（null/空白视为未提供且不回退 query）",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema = @Schema(implementation = EcxDiscountcardInfoBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String cardId = OpenapiRequestParams.mergeString(cardIdParam, body, "card_id");
		Map<String, Object> data = discountCardDetailPort.getDiscountCardDetail(companyId, cardId);
		return new OpenapiEnvelope("success", "E0000", "success", data);
	}

	@PostMapping(value = "/ecx.discountcard.list", name = "开放接口获取卡券列表")
	public OpenapiEnvelope getDiscountCardList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "pageSize", required = false) String pageSizeParam,
			@RequestParam(name = "title", required = false) String titleParam,
			@RequestParam(name = "card_type", required = false) String cardTypeParam,
			@RequestParam(name = "card_ids", required = false) String cardIdsParam,
			@RequestParam(name = "is_valid", required = false) String isValidParam,
			@RequestParam(name = "status", required = false) String statusParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("page", OpenapiRequestParams.mergeString(pageParam, body, "page"));
		merged.put("pageSize", OpenapiRequestParams.mergeString(pageSizeParam, body, "pageSize"));
		merged.put("title", OpenapiRequestParams.mergeString(titleParam, body, "title"));
		merged.put("card_type", OpenapiRequestParams.mergeString(cardTypeParam, body, "card_type"));
		merged.put("card_ids", OpenapiRequestParams.mergeString(cardIdsParam, body, "card_ids"));
		merged.put("is_valid", OpenapiRequestParams.mergeString(isValidParam, body, "is_valid"));
		merged.put("status", OpenapiRequestParams.mergeString(statusParam, body, "status"));
		Map<String, Object> data = discountCardListPort.getDiscountCardList(companyId, merged);
		return new OpenapiEnvelope("success", "E0000", "success", data);
	}
}
