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
import cn.shopex.ecshopx.common.openapi.OpenapiEnvelope;
import cn.shopex.ecshopx.common.openapi.OpenapiProductGoodsListV2Port;
import cn.shopex.ecshopx.common.openapi.OpenapiProductSkuListV2Port;
import cn.shopex.ecshopx.common.openapi.OpenapiProductStockUpdateV2Port;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiGoodsListParams;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiPaginationParams;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxProductGoodsListBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxProductGoodsListSuccessEnvelopeDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxProductSkuListBodyDoc;
import cn.shopex.ecshopx.openapi.thirdapi.v2.items.doc.EcxProductSkuListSuccessEnvelopeDoc;
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

@Tag(name = "开放接口-导购云店商品V2")
@OpenapiResponse
@RestController("openapiV2Item")
@RequestMapping("/api/openapi/internal/v2")
public class ItemController extends OpenapiBaseController {

	private final OpenapiProductSkuListV2Port skuListV2Port;
	private final OpenapiProductStockUpdateV2Port stockUpdateV2Port;
	private final OpenapiProductGoodsListV2Port goodsListV2Port;

	public ItemController(
			OpenapiProductSkuListV2Port skuListV2Port,
			OpenapiProductStockUpdateV2Port stockUpdateV2Port,
			OpenapiProductGoodsListV2Port goodsListV2Port) {
		this.skuListV2Port = skuListV2Port;
		this.stockUpdateV2Port = stockUpdateV2Port;
		this.goodsListV2Port = goodsListV2Port;
	}

	@Operation(
			summary = "开放接口商品SKU列表V2",
			description =
					"鉴权：openapi 签名（公网网关校验后转发 internal；直连 internal 无鉴权上下文报 E4004）。"
							+ " 5 键 query 与 JSON body 同名双通道：page/page_size/country_code body 值非 null 优先，"
							+ " time_begin/time_end body 含键优先（null 视为未传）。"
							+ " page/page_size 必填（缺失/非法 → E0001；page 须 ≥1）；服务内归一 page≥1、"
							+ " page_size≤0→100、>2000→2000（生效值回显）。"
							+ " 按 items.updated 时间窗拉取普通类型（item_type=normal）SKU 行（含店铺商品，is_self 区分），"
							+ " item_id 降序；name 经 country_code 语言分表覆盖（默认 zh-CN）；sku_id 即货品编码 item_bn。"
							+ " 信封显式构造：message 固定空字符串 \"\"。副作用：无（只读）。"
							+ " 失败 HTTP 仍 200：E0001（page参数必填/page_size参数必填）/"
							+ " E5320「开始时间不能大于结束时间」，data=null。",
			operationId = "ecx.product.sku_list.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（显式构造，message 固定空串）。"
								+ " 成功：status=success, code=E0000, message=\"\"，data 见 EcxProductSkuListDataDoc"
								+ "（4 固定键，行 3 键）；"
								+ " 业务失败：status=fail，code=E0001 分页参数缺失/非法 或 E5320 时间窗非法，data=null，"
								+ "见 EcxProductSkuListFailEnvelopeDoc；鉴权失败：status=fail, code=E4004 未授权，data=null。",
				content =
						@Content(
								schema = @Schema(implementation = EcxProductSkuListSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "一条平台 SKU（message 为空串）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"\",\"data\":{"
															+ "\"page\":1,\"page_size\":100,\"total_count\":1,\"sku_list\":["
															+ "{\"name\":\"Wire test goods\",\"sku_id\":\"SKU200\","
															+ "\"is_self\":true}]}}"),
									@ExampleObject(
											name = "success-empty",
											summary = "窗口内无 SKU（sku_list 空数组）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"\","
															+ "\"data\":{\"page\":1,\"page_size\":100,\"total_count\":0,"
															+ "\"sku_list\":[]}}"),
									@ExampleObject(
											name = "fail-missing-page",
											summary = "page 缺失或非法（E0001）",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\",\"message\":\"page参数必填\",\"data\":null}"),
									@ExampleObject(
											name = "fail-missing-page-size",
											summary = "page_size 缺失或非法（E0001）",
											value =
													"{\"status\":\"fail\",\"code\":\"E0001\",\"message\":\"page_size参数必填\",\"data\":null}"),
									@ExampleObject(
											name = "fail-time-window",
											summary = "开始时间大于结束时间（E5320）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5320\","
															+ "\"message\":\"开始时间不能大于结束时间\",\"data\":null}"),
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
			description = "JSON body（与 query 同名键双通道，body 优先；page/page_size 契约必填）",
			content = @Content(schema = @Schema(implementation = EcxProductSkuListBodyDoc.class)))
	@PostMapping(value = "/ecx.product.sku_list", name = "开放接口商品SKU列表V2")
	public OpenapiEnvelope list(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "页码（与 body 同名键双通道，body 值非 null 优先）；必填，≥1（否则 E0001「page参数必填」）",
							required = true,
							example = "1")
					@RequestParam(name = "page", required = false)
					String pageParam,
			@Parameter(
							description =
									"每页数量（双通道同上）；必填整数（否则 E0001「page_size参数必填」）；≤0 归一 100、>2000 截断 2000",
							required = true,
							example = "100")
					@RequestParam(name = "page_size", required = false)
					String pageSizeParam,
			@Parameter(
							description =
									"更新时间窗口起点 items.updated≥（双通道同上）；yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd，"
											+ "非法静默 0（≤0 跳过过滤）；与终点同生效且更大 → E5320（示例为格式演示值）",
							example = "2026-01-01 00:00:00")
					@RequestParam(name = "time_begin", required = false)
					String timeBeginParam,
			@Parameter(
							description = "更新时间窗口终点 items.updated≤（双通道同上）；语义同起点（示例为格式演示值）",
							example = "2026-01-02 00:00:00")
					@RequestParam(name = "time_end", required = false)
					String timeEndParam,
			@Parameter(
							description = "多语言代码（双通道同上）；默认 zh-CN；按语言分表覆盖 name",
							example = "zh-CN")
					@RequestParam(name = "country_code", required = false)
					String countryCodeParam,
			@Parameter(hidden = true) @FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		int page = OpenapiPaginationParams.resolvePage(pageParam, body);
		int pageSize = OpenapiPaginationParams.resolvePageSize(pageSizeParam, body);
		String countryCode = OpenapiPaginationParams.mergeCountryCode(countryCodeParam, body);
		String timeBegin = OpenapiRequestParams.originalString(timeBeginParam, body, "time_begin");
		String timeEnd = OpenapiRequestParams.originalString(timeEndParam, body, "time_end");
		Map<String, Object> data =
				skuListV2Port.listNormalSkus(companyId, page, pageSize, timeBegin, timeEnd, countryCode);
		return new OpenapiEnvelope("success", "E0000", "", data);
	}

	@Operation(
			summary = "开放接口导购云店商品列表V2",
			description =
					"鉴权：openapi 签名（公网网关校验后转发 internal；直连 internal 无鉴权上下文报 E4004）。"
							+ " 4 键 query 与 JSON body 同名双通道：page/page_size body 值非 null 优先（数字直取），"
							+ " time_begin/time_end body 含键优先（null 视为未传）。"
							+ " 按 items.updated 时间窗拉取商品 SKU 行（仅租户过滤——含全部上下架状态与店铺商品，"
							+ " is_self 区分自营；无 ORDER BY，分页顺序不保证稳定）。"
							+ " item_name 经 zh-CN 语言分表覆盖；sku 为规格值空格拼接（关联查询全局 LIMIT 100 无排序，"
							+ " 超出部分商品 sku 为空串）；goods_bn 键实为货品 item_id（PHP 历史契约）。"
							+ " 分页无上下限校验：page 默认 1（≤0 → 负 offset SQL 错误 → HTTP 500）、"
							+ " page_size 默认 100（≤0 → 全量返回）。"
							+ " 信封显式构造：message 固定 \"ok\"。副作用：无（只读）。"
							+ " 失败 HTTP 仍 200：E5320「开始时间不能大于结束时间」（两时间均生效且起点>终点），data=null。",
			operationId = "ecx.product.goods_list.v2")
	@ApiResponses({
		@ApiResponse(
				responseCode = "200",
				description =
						"HTTP 200，OpenapiEnvelope 形态（显式构造，message 固定 ok）。"
								+ " 成功：status=success, code=E0000, message=ok，data 见 EcxProductGoodsListDataDoc"
								+ "（4 固定键，行 8 键）；"
								+ " 业务失败：status=fail, code=E5320 开始时间不能大于结束时间，data=null，"
								+ "见 EcxProductGoodsListFailEnvelopeDoc；鉴权失败：status=fail, code=E4004 未授权，data=null。",
				content =
						@Content(
								schema = @Schema(implementation = EcxProductGoodsListSuccessEnvelopeDoc.class),
								examples = {
									@ExampleObject(
											name = "success",
											summary = "一条平台商品（pic 取 seed 真实 URL 仅作格式演示）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"ok\",\"data\":{"
															+ "\"page\":1,\"page_size\":100,\"total_count\":1,\"goods_list\":["
															+ "{\"item_name\":\"Wire test goods\",\"item_price\":10,"
															+ "\"pic\":\"https://b-img-cdn.yuanyuanke.cn/image/21/2021/05/21/"
															+ "f425f5ae2e6032eb6fded1015fc979e4xPndgZMDIyfeGTWsMJOTcmWvaPQjWPbL\","
															+ "\"sku\":\"\",\"goods_bn\":200,\"item_bn\":\"SKU200\","
															+ "\"approve_status\":\"onsale\",\"is_self\":true}]}}"),
									@ExampleObject(
											name = "success-empty",
											summary = "窗口内无商品（goods_list 空数组）",
											value =
													"{\"status\":\"success\",\"code\":\"E0000\",\"message\":\"ok\","
															+ "\"data\":{\"page\":1,\"page_size\":100,\"total_count\":0,"
															+ "\"goods_list\":[]}}"),
									@ExampleObject(
											name = "fail-time-window",
											summary = "开始时间大于结束时间（E5320，唯一业务失败码）",
											value =
													"{\"status\":\"fail\",\"code\":\"E5320\","
															+ "\"message\":\"开始时间不能大于结束时间\",\"data\":null}"),
									@ExampleObject(
											name = "fail-unauthorized",
											summary = "鉴权失败（直连 internal 无鉴权上下文）",
											value =
													"{\"status\":\"fail\",\"code\":\"E4004\",\"message\":\"未授权\",\"data\":null}")
								})),
		@ApiResponse(
				responseCode = "500",
				description = "未处理异常兜底（含 page≤0 负 offset SQL 错误；GlobalExceptionHandler，ApiResult 非本信封）",
				content =
						@Content(
								examples =
										@ExampleObject(
												name = "internal-error",
												value = "{\"code\":500,\"msg\":\"服务器内部错误\"}")))
	})
	@RequestBody(
			required = false,
			description = "JSON body（与 query 同名键双通道，body 优先）",
			content = @Content(schema = @Schema(implementation = EcxProductGoodsListBodyDoc.class)))
	@PostMapping(value = "/ecx.product.goods_list", name = "开放接口导购云店商品列表V2")
	public OpenapiEnvelope goodsList(
			@Parameter(hidden = true) HttpServletRequest request,
			@Parameter(
							description = "页码（与 body 同名键双通道，body 值非 null 优先）；默认 1，无下限校验（≤0 → HTTP 500）",
							example = "1")
					@RequestParam(name = "page", required = false)
					String pageParam,
			@Parameter(
							description = "每页数量（双通道同上）；默认 100，无上限校验；≤0 全量返回",
							example = "100")
					@RequestParam(name = "page_size", required = false)
					String pageSizeParam,
			@Parameter(
							description =
									"更新时间窗口起点 items.updated≥（双通道同上）；yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd，"
											+ "非法静默 0（≤0 跳过过滤）；与终点同生效且更大 → E5320（示例为格式演示值）",
							example = "2026-01-01 00:00:00")
					@RequestParam(name = "time_begin", required = false)
					String timeBeginParam,
			@Parameter(
							description = "更新时间窗口终点 items.updated≤（双通道同上）；语义同起点（示例为格式演示值）",
							example = "2026-01-02 00:00:00")
					@RequestParam(name = "time_end", required = false)
					String timeEndParam,
			@Parameter(hidden = true) @FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		int page = OpenapiGoodsListParams.resolvePage(pageParam, body);
		int pageSize = OpenapiGoodsListParams.resolvePageSize(pageSizeParam, body);
		String timeBegin = OpenapiRequestParams.originalString(timeBeginParam, body, "time_begin");
		String timeEnd = OpenapiRequestParams.originalString(timeEndParam, body, "time_end");
		Map<String, Object> data =
				goodsListV2Port.listGoods(companyId, page, pageSize, timeBegin, timeEnd);
		return new OpenapiEnvelope("success", "E0000", "ok", data);
	}

	@PostMapping(value = "/ecx.product.stock_update", name = "开放接口库存同步V2")
	public OpenapiEnvelope updateItemStore(
			HttpServletRequest request,
			@RequestParam(name = "sku_list", required = false) String skuListParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String skuListRaw = OpenapiRequestParams.mergeString(skuListParam, body, "sku_list");
		Map<String, Object> data = stockUpdateV2Port.updateItemStore(companyId, skuListRaw);
		return new OpenapiEnvelope("success", "E0000", "成功", data);
	}
}
