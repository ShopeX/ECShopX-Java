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
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

	@PostMapping(value = "/ecx.product.sku_list", name = "开放接口商品SKU列表V2")
	public OpenapiEnvelope list(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@RequestParam(name = "time_begin", required = false) String timeBeginParam,
			@RequestParam(name = "time_end", required = false) String timeEndParam,
			@RequestParam(name = "country_code", required = false) String countryCodeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
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

	@PostMapping(value = "/ecx.product.goods_list", name = "开放接口导购云店商品列表V2")
	public OpenapiEnvelope goodsList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@RequestParam(name = "time_begin", required = false) String timeBeginParam,
			@RequestParam(name = "time_end", required = false) String timeEndParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
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
