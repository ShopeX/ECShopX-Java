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
import cn.shopex.ecshopx.common.openapi.OpenapiEnvelope;
import cn.shopex.ecshopx.common.openapi.OpenapiProductGoodsListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiProductSkuListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiProductStockUpdatePort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxProductGoodsListBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxProductGoodsListSuccessEnvelopeDoc;
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

@Tag(name = "开放接口-商品V1")
@OpenapiResponse
@RestController("openapiV1Item")
@RequestMapping("/api/openapi/internal/v1")
public class ItemController extends OpenapiBaseController {

	private final OpenapiProductSkuListPort skuListPort;
	private final OpenapiProductStockUpdatePort stockUpdatePort;
	private final OpenapiProductGoodsListPort goodsListPort;

	public ItemController(
			OpenapiProductSkuListPort skuListPort,
			OpenapiProductStockUpdatePort stockUpdatePort,
			OpenapiProductGoodsListPort goodsListPort) {
		this.skuListPort = skuListPort;
		this.stockUpdatePort = stockUpdatePort;
		this.goodsListPort = goodsListPort;
	}

	@PostMapping(value = "/ecx.product.sku_list", name = "开放接口商品SKU列表")
	public OpenapiEnvelope list(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@RequestParam(name = "country_code", required = false) String countryCodeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		int page = OpenapiPaginationParams.resolvePage(pageParam, body);
		int pageSize = OpenapiPaginationParams.resolvePageSize(pageSizeParam, body);
		String countryCode = OpenapiPaginationParams.mergeCountryCode(countryCodeParam, body);
		Map<String, Object> data = skuListPort.listNormalSkus(companyId, page, pageSize, countryCode);
		return new OpenapiEnvelope("success", "E0000", "", data);
	}

	@Operation(
			summary = "开放接口导购云店商品列表",
			description =
					"按签名鉴权租户 company_id 分页返回全部商品（无类型/状态过滤、无排序），"
							+ " data 含 page/page_size 回显、total_count 与 goods_list（7 键/条）："
							+ " item_name（zh-CN 分表覆盖）、item_price（分）、pic（pics 首图）、"
							+ " sku（关联属性值空格拼接，关联行全局 LIMIT 100）、goods_bn（实为 item_id 整型，PHP 旧契约）、"
							+ " item_bn、approve_status。"
							+ " 分页：page 缺省 1、page_size 缺省 100，非整数回退默认；page_size≤0 返回全部。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.product.goods_list, version=1.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 参数双通道（body 含键取原始值，null 回退 query）。"
							+ " 只读，无写库/异步/第三方副作用；无业务校验失败分支。",
			operationId = "ecx.product.goods_list.v1")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态。"
								+ " 成功：status=success, code=E0000, message=ok，data 见 EcxProductGoodsListDataDoc"
								+ "（无商品时 total_count=0、goods_list=[]）；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxProductGoodsListFailEnvelopeDoc。"
								+ " 本接口无业务校验失败分支。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxProductGoodsListSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"ok\","
															+ "\"data\":{\"page\":1,\"page_size\":100,\"total_count\":1,"
															+ "\"goods_list\":[{\"item_name\":\"示例商品\",\"item_price\":9900,"
															+ "\"pic\":\"https://example.test/key.jpg\",\"sku\":\"红 L\","
															+ "\"goods_bn\":1001,\"item_bn\":\"A1\","
															+ "\"approve_status\":\"onsale\"}]}}"),
									@ExampleObject(
											name = "success-empty",
											summary = "无商品",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"ok\","
															+ "\"data\":{\"page\":1,\"page_size\":100,\"total_count\":0,"
															+ "\"goods_list\":[]}}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.product.goods_list", name = "开放接口导购云店商品列表")
	public OpenapiEnvelope goodsList(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(description = "页码，从 1 起；非整数/缺省按 1（body 同名键优先）", example = "1")
					@RequestParam(name = "page", required = false)
					String pageParam,
			@Parameter(
							description = "每页条数；非整数/缺省按 100；≤0 返回全部商品（body 同名键优先）",
							example = "100")
					@RequestParam(name = "page_size", required = false)
					String pageSizeParam,
			@RequestBody(
							description =
									"可选 JSON body；字段与 query 同名双通道，body 含键时取原始值（null 回退 query）",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxProductGoodsListBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		int page = OpenapiGoodsListParams.resolvePage(pageParam, body);
		int pageSize = OpenapiGoodsListParams.resolvePageSize(pageSizeParam, body);
		Map<String, Object> data = goodsListPort.listGoods(companyId, page, pageSize);
		return new OpenapiEnvelope("success", "E0000", "ok", data);
	}

	@PostMapping(value = "/ecx.product.stock_update", name = "开放接口库存同步")
	public OpenapiEnvelope updateItemStore(
			HttpServletRequest request,
			@RequestParam(name = "sku_list", required = false) String skuListParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String skuListRaw = OpenapiRequestParams.mergeRequiredString(
				skuListParam, body, "sku_list", "sku_list必填");
		Map<String, Object> data = stockUpdatePort.updateItemStore(companyId, skuListRaw);
		return new OpenapiEnvelope("success", "E0000", "更新成功", data);
	}
}
