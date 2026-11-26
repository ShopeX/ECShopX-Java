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
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxProductSkuListBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v1.doc.EcxProductSkuListSuccessEnvelopeDoc;
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

	@Operation(
			summary = "开放接口商品SKU列表",
			description =
					"按签名鉴权租户 company_id 分页返回普通商品 SKU（item_type=normal，item_id 倒序），"
							+ " data 含归一化 page/page_size 回显、total_count 与 sku_list（每条仅 name/sku_id 两键，"
							+ " name 按 country_code 语言分表覆盖，sku_id 为货品编码 item_bn）。"
							+ " 分页：page 必填正整数、page_size 必填整数（否则 E0001）；"
							+ " 服务层归一化 page≥1、page_size≤0→100、>2000 封顶 2000。"
							+ " country_code 双通道（body 含键优先，null/空白不回退 query），缺省 zh-CN。"
							+ " 鉴权面 openapi sign：公网经 /api/openapi/{method}（method=ecx.product.sku_list, version=1.0）"
							+ " 签名校验后转发；直连 internal 无鉴权上下文返回 E4004 未授权。"
							+ " 成功 message 恒为空串。只读，无写库/异步/第三方副作用。",
			operationId = "ecx.product.sku_list.v1")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态。"
								+ " 成功：status=success, code=E0000, message 为空串，data 见 EcxProductSkuListDataDoc"
								+ "（无商品时 total_count=0、sku_list=[]）；"
								+ " 校验失败：status=fail, code=E0001（page参数必填/page_size参数必填），data=null；"
								+ " 鉴权失败：status=fail, code=E4004 未授权，data=null。见 EcxProductSkuListFailEnvelopeDoc。",
				content =
						@Content(
								mediaType = "application/json",
								schema =
										@Schema(
												implementation =
														EcxProductSkuListSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "查询成功（message 为空串）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"\","
															+ "\"data\":{\"page\":1,\"page_size\":100,\"total_count\":1,"
															+ "\"sku_list\":[{\"name\":\"示例商品\",\"sku_id\":\"A1\"}]}}"),
									@ExampleObject(
											name = "success-empty",
											summary = "无普通商品",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"\","
															+ "\"data\":{\"page\":1,\"page_size\":100,\"total_count\":0,"
															+ "\"sku_list\":[]}}"),
									@ExampleObject(
											name = "fail-missing-page",
											summary = "page 缺失/非法",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\",\"message\":\"page参数必填\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								}))
	})
	@PostMapping(value = "/ecx.product.sku_list", name = "开放接口商品SKU列表")
	public OpenapiEnvelope list(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "页码；必填正整数（缺失/非整数/小于 1 报「page参数必填」）（body 同名键优先）",
							required = true,
							example = "1")
					@RequestParam(name = "page", required = false)
					String pageParam,
			@Parameter(
							description = "每页条数；必填整数；≤0 按 100，>2000 封顶 2000（body 同名键优先）",
							required = true,
							example = "100")
					@RequestParam(name = "page_size", required = false)
					String pageSizeParam,
			@Parameter(
							description = "语言/国家码，默认 zh-CN；body 含 country_code 键时以 body 为准（body 优先）",
							example = "zh-CN")
					@RequestParam(name = "country_code", required = false)
					String countryCodeParam,
			@RequestBody(
							description =
									"可选 JSON body；字段与 query 同名双通道，body 含键时优先",
							required = false,
							content =
									@Content(
											mediaType = "application/json",
											schema =
													@Schema(
															implementation =
																	EcxProductSkuListBodyDoc.class)))
					@FlexibleBody(required = false)
					Map<String, Object> body) {
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
