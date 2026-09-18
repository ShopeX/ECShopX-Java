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
import cn.shopex.ecshopx.common.openapi.OpenapiItemBrandCreatePort;
import cn.shopex.ecshopx.common.openapi.OpenapiItemBrandDeletePort;
import cn.shopex.ecshopx.common.openapi.OpenapiItemBrandListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiItemBrandUpdatePort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OpenAPI v2 handler scaffold — methods added during migration. */
@OpenapiResponse
@RestController("openapiV2ItemsAttributes")
@RequestMapping("/api/openapi/internal/v2")
public class ItemsAttributesController extends OpenapiBaseController {

	private final OpenapiItemBrandCreatePort itemBrandCreatePort;
	private final OpenapiItemBrandDeletePort itemBrandDeletePort;
	private final OpenapiItemBrandUpdatePort itemBrandUpdatePort;
	private final OpenapiItemBrandListPort itemBrandListPort;

	public ItemsAttributesController(
			OpenapiItemBrandCreatePort itemBrandCreatePort,
			OpenapiItemBrandDeletePort itemBrandDeletePort,
			OpenapiItemBrandUpdatePort itemBrandUpdatePort,
			OpenapiItemBrandListPort itemBrandListPort) {
		this.itemBrandCreatePort = itemBrandCreatePort;
		this.itemBrandDeletePort = itemBrandDeletePort;
		this.itemBrandUpdatePort = itemBrandUpdatePort;
		this.itemBrandListPort = itemBrandListPort;
	}

	@GetMapping(value = "/ecx.item.brand.get", name = "开放接口查询商品品牌列表")
	public Map<String, Object> getItemBrandList(
			HttpServletRequest request,
			@RequestParam(name = "page", required = false) String pageParam,
			@RequestParam(name = "page_size", required = false) String pageSizeParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		OpenapiThirdApiV2ItemBrandListParams.PageSpec pageSpec =
				OpenapiThirdApiV2ItemBrandListParams.resolve(pageParam, pageSizeParam, body);
		return itemBrandListPort.getItemBrandList(
				companyId, pageSpec.page(), pageSpec.pageSize());
	}

	@PostMapping(value = "/ecx.item.brand.add", name = "开放接口新增商品品牌")
	public Map<String, Object> createItemBrand(
			HttpServletRequest request,
			@RequestParam(name = "brand_name", required = false) String brandNameParam,
			@RequestParam(name = "image_url", required = false) String imageUrlParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String brandNameRaw = OpenapiRequestParams.originalString(brandNameParam, body, "brand_name");
		String imageUrlRaw = OpenapiRequestParams.originalString(imageUrlParam, body, "image_url");
		return itemBrandCreatePort.createItemBrand(companyId, brandNameRaw, imageUrlRaw);
	}

	@DeleteMapping(value = "/ecx.item.brand.delete", name = "开放接口删除商品品牌")
	public Map<String, Object> deleteItemBrand(
			HttpServletRequest request,
			@RequestParam(name = "brand_id", required = false) String brandIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String brandIdRaw = OpenapiRequestParams.originalString(brandIdParam, body, "brand_id");
		return itemBrandDeletePort.deleteItemBrand(companyId, brandIdRaw);
	}

	@PostMapping(value = "/ecx.item.brand.update", name = "开放接口更新商品品牌")
	public Map<String, Object> updateItemBrand(
			HttpServletRequest request,
			@RequestParam(name = "brand_id", required = false) String brandIdParam,
			@RequestParam(name = "brand_name", required = false) String brandNameParam,
			@RequestParam(name = "image_url", required = false) String imageUrlParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String brandIdRaw = OpenapiRequestParams.originalString(brandIdParam, body, "brand_id");
		String brandNameRaw = OpenapiRequestParams.originalString(brandNameParam, body, "brand_name");
		String imageUrlRaw = OpenapiRequestParams.originalString(imageUrlParam, body, "image_url");
		return itemBrandUpdatePort.updateItemBrand(companyId, brandIdRaw, brandNameRaw, imageUrlRaw);
	}
}
