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

package cn.shopex.ecshopx.openapi.thirdapi.v2.distributor;

import cn.shopex.ecshopx.common.annotation.OpenapiResponse;
import cn.shopex.ecshopx.common.openapi.OpenapiDistributorItemDownloadCommand;
import cn.shopex.ecshopx.common.openapi.OpenapiDistributorItemDownloadPort;
import cn.shopex.ecshopx.common.openapi.OpenapiDistributorItemListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiDistributorItemUpdateCommand;
import cn.shopex.ecshopx.common.openapi.OpenapiDistributorItemUpdatePort;
import cn.shopex.ecshopx.common.openapi.OpenapiDistributorV2FailException;
import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.OpenapiThirdApiV2ShippingTemplatesListParams;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OpenAPI v2 handler scaffold — methods added during migration. */
@OpenapiResponse
@RestController("openapiV2DistributorItem")
@RequestMapping("/api/openapi/internal/v2")
public class DistributorItemController extends OpenapiBaseController {

	private final OpenapiDistributorItemListPort distributorItemListPort;
	private final OpenapiDistributorItemUpdatePort distributorItemUpdatePort;
	private final OpenapiDistributorItemDownloadPort distributorItemDownloadPort;

	public DistributorItemController(
			OpenapiDistributorItemListPort distributorItemListPort,
			OpenapiDistributorItemUpdatePort distributorItemUpdatePort,
			OpenapiDistributorItemDownloadPort distributorItemDownloadPort) {
		this.distributorItemListPort = distributorItemListPort;
		this.distributorItemUpdatePort = distributorItemUpdatePort;
		this.distributorItemDownloadPort = distributorItemDownloadPort;
	}

	@GetMapping(value = "/ecx.distributor_item.list", name = "开放接口查询店铺商品列表")
	public Map<String, Object> list(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@RequestParam(name = "shop_code", required = false) String shopCodeParam,
			@RequestParam(name = "item_code", required = false) String itemCodeParam,
			@RequestParam(name = "item_name", required = false) String itemNameParam,
			@RequestParam(name = "goods_can_sale", required = false) String goodsCanSaleParam,
			@RequestParam(name = "is_total_store", required = false) String isTotalStoreParam,
			@RequestParam(name = "status", required = false) String statusParam,
			@RequestParam(name = "distributor_id", required = false) String distributorIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);

		if (OpenapiRequestParams.presentOptionalString(shopCodeParam, body, "shop_code").isEmpty()) {
			throw new OpenapiDistributorV2FailException(
					OpenapiErrorCode.SERVICE_MISSING_PARAMS, "店铺号参数错误");
		}

		OpenapiThirdApiV2ShippingTemplatesListParams.PageSpec pageSpec =
				OpenapiThirdApiV2ShippingTemplatesListParams.resolve(pageParam, pageSizeParam, body);

		return distributorItemListPort.list(
				companyId,
				pageSpec.page(),
				pageSpec.pageSize(),
				OpenapiRequestParams.originalString(shopCodeParam, body, "shop_code"),
				OpenapiRequestParams.originalString(itemCodeParam, body, "item_code"),
				OpenapiRequestParams.originalString(itemNameParam, body, "item_name"),
				OpenapiRequestParams.originalString(goodsCanSaleParam, body, "goods_can_sale"),
				OpenapiRequestParams.originalString(isTotalStoreParam, body, "is_total_store"),
				OpenapiRequestParams.originalString(statusParam, body, "status"),
				OpenapiRequestParams.originalString(distributorIdParam, body, "distributor_id"));
	}

	@PatchMapping(value = "/ecx.distributor_item.update", name = "开放接口更新店铺商品")
	public Map<String, Object> update(
			HttpServletRequest request,
			@RequestParam(name = "shop_code", required = false) String shopCodeParam,
			@RequestParam(name = "item_code", required = false) String itemCodeParam,
			@RequestParam(name = "is_can_sale", required = false) String isCanSaleParam,
			@RequestParam(name = "is_total_store", required = false) String isTotalStoreParam,
			@RequestParam(name = "store", required = false) String storeParam,
			@RequestParam(name = "price", required = false) String priceParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);

		OpenapiDistributorItemUpdateCommand command =
				OpenapiThirdApiV2DistributorItemUpdateParams.resolve(
						shopCodeParam, itemCodeParam,
						isCanSaleParam, isTotalStoreParam,
						storeParam, priceParam, body);

		distributorItemUpdatePort.update(companyId, command);
		return null;
	}

	@GetMapping(value = "/ecx.distributor_item.download", name = "开放接口生成店铺商品码")
	public Map<String, Object> download(
			HttpServletRequest request,
			@RequestParam(name = "shop_code", required = false) String shopCodeParam,
			@RequestParam(name = "item_code", required = false) String itemCodeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiDistributorItemDownloadCommand command =
				OpenapiThirdApiV2DistributorItemDownloadParams.resolve(
						shopCodeParam, itemCodeParam, body);
		return distributorItemDownloadPort.download(companyId, command);
	}
}
