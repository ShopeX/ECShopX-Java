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
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

	@PostMapping(value = "/ecx.product.goods_list", name = "开放接口导购云店商品列表")
	public OpenapiEnvelope goodsList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
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
