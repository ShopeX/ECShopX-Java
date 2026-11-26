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

package cn.shopex.ecshopx.openapi.thirdapi.v2.items;

import cn.shopex.ecshopx.common.annotation.OpenapiResponse;
import cn.shopex.ecshopx.common.openapi.OpenapiItemPriceGetPort;
import cn.shopex.ecshopx.common.openapi.OpenapiItemPriceSyncPort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemPriceGetBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxItemPriceGetSuccessEnvelopeDoc;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** OpenAPI v2 handler scaffold — methods added during migration. */
@Tag(name = "开放接口-商品价格V2")
@OpenapiResponse
@RestController("openapiV2Price")
@RequestMapping("/api/openapi/internal/v2")
public class PriceController extends OpenapiBaseController {

	private final OpenapiItemPriceSyncPort itemPriceSyncPort;
	private final OpenapiItemPriceGetPort itemPriceGetPort;

	public PriceController(
			OpenapiItemPriceSyncPort itemPriceSyncPort,
			OpenapiItemPriceGetPort itemPriceGetPort) {
		this.itemPriceSyncPort = itemPriceSyncPort;
		this.itemPriceGetPort = itemPriceGetPort;
	}

	@Operation(
			summary = "查询商品价格",
			description =
					"按签名鉴权租户 company_id 查询商品价格：入参 2 键白名单（item_code/distributor_code，"
							+ " body 含键优先否则 query 同名键，空串值忽略）。item_code 按 item_bn 定位"
							+ "（item_id 升序 LIMIT 1；缺省取租户第一条商品——PHP 旧契约），查无 E5101「商品找不到」。"
							+ " data 5 固定键：item_code/item_name 字符串化、price/market_price/cost_price"
							+ " 元两位小数字符串（分÷100 HALF_UP，null 按 0.00）。"
							+ " distributor_code 仅 productModel=standard 生效：查无店铺 E5401「店铺找不到」；"
							+ " 命中追加 distributor_code/distributor_name，且存在 distributor_id IS NULL 的"
							+ " 店铺商品行时用其 price 覆盖；platform 模式忽略。"
							+ " 其他异常 E5000（message 透传，空则「未知错误」）。Advice 包装 message 固定「成功」。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.item.price.get, version=2.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 只读，无写库/异步/第三方副作用。",
			operationId = "ecx.item.price.get.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（Advice 自动包装）。"
								+ " 成功：status=success, code=E0000, message=成功，data 见 EcxItemPriceGetDataDoc；"
								+ " 业务失败：status=fail, code=E5101 商品找不到 / E5401 店铺找不到 / E5000 未知错误，data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxItemPriceGetFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxItemPriceGetSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功（平台价格）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"item_code\":\"SKU200\",\"item_name\":\"Wire test goods\","
															+ "\"price\":\"0.10\",\"market_price\":\"0.00\",\"cost_price\":\"0.00\"}}"),
									@ExampleObject(
											name = "success-distributor",
											summary = "standard 店铺分支（追加店铺键）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"成功\","
															+ "\"data\":{\"item_code\":\"SKU200\",\"item_name\":\"Wire test goods\","
															+ "\"price\":\"0.10\",\"market_price\":\"0.00\",\"cost_price\":\"0.00\","
															+ "\"distributor_code\":\"S001\",\"distributor_name\":\"新门店名\"}}"),
									@ExampleObject(
											name = "fail-goods-not-found",
											summary = "item_code 查无",
											value =
													"{\"status\":\"fail\",\"code\":\"E5101\",\"message\":\"商品找不到\",\"data\":null}"),
									@ExampleObject(
											name = "fail-shop-not-found",
											summary = "standard 分支店铺查无",
											value =
													"{\"status\":\"fail\",\"code\":\"E5401\",\"message\":\"店铺找不到\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@GetMapping("/ecx.item.price.get")
	public Map<String, Object> getPriceDetail(
			@Parameter(hidden = true) HttpServletRequest request,
			@RequestBody(
							description = "可选 JSON body；item_code/distributor_code 与 query 同名双通道（body 含键优先，空串忽略）",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxItemPriceGetBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Map<String, Object> merged = OpenapiThirdApiV2ItemPriceGetParams.buildMergedParams(request, body);
		return itemPriceGetPort.getPriceDetail(companyId, merged);
	}

	@PutMapping("/ecx.item.price.sync")
	public Void sync(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		Map<String, Object> merged = OpenapiThirdApiV2ItemPriceSyncParams.buildMergedParams(request, body);
		itemPriceSyncPort.syncPrice(companyId, merged);
		return null;
	}
}
