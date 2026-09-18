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
import cn.shopex.ecshopx.common.openapi.OpenapiItemCategoryCreatePort;
import cn.shopex.ecshopx.common.openapi.OpenapiItemCategoryDeletePort;
import cn.shopex.ecshopx.common.openapi.OpenapiItemCategoryListPort;
import cn.shopex.ecshopx.common.openapi.OpenapiItemCategoryUpdatePort;
import cn.shopex.ecshopx.common.openapi.OpenapiItemMainCategoryDetailPort;
import cn.shopex.ecshopx.common.web.FlexibleBody;
import cn.shopex.ecshopx.openapi.thirdapi.OpenapiBaseController;
import cn.shopex.ecshopx.openapi.thirdapi.v1.OpenapiRequestParams;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** OpenAPI v2 handler scaffold — methods added during migration. */
@OpenapiResponse
@RestController("openapiV2ItemsCategory")
@RequestMapping("/api/openapi/internal/v2")
public class ItemsCategoryController extends OpenapiBaseController {

	private final OpenapiItemCategoryCreatePort itemCategoryCreatePort;
	private final OpenapiItemCategoryDeletePort itemCategoryDeletePort;
	private final OpenapiItemCategoryUpdatePort itemCategoryUpdatePort;
	private final OpenapiItemCategoryListPort itemCategoryListPort;
	private final OpenapiItemMainCategoryDetailPort mainCategoryDetailPort;

	public ItemsCategoryController(
			OpenapiItemCategoryCreatePort itemCategoryCreatePort,
			OpenapiItemCategoryDeletePort itemCategoryDeletePort,
			OpenapiItemCategoryUpdatePort itemCategoryUpdatePort,
			OpenapiItemCategoryListPort itemCategoryListPort,
			OpenapiItemMainCategoryDetailPort mainCategoryDetailPort) {
		this.itemCategoryCreatePort = itemCategoryCreatePort;
		this.itemCategoryDeletePort = itemCategoryDeletePort;
		this.itemCategoryUpdatePort = itemCategoryUpdatePort;
		this.itemCategoryListPort = itemCategoryListPort;
		this.mainCategoryDetailPort = mainCategoryDetailPort;
	}

	@GetMapping(value = "/ecx.item.category.get", name = "开放接口查询商品分类列表")
	public List<Map<String, Object>> getItemCategoryList(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		return itemCategoryListPort.getItemCategoryList(companyId);
	}

	@GetMapping(value = "/ecx.item.maincategory.get", name = "开放接口查询商品主类目列表")
	public List<Map<String, Object>> getItemMainCategoryList(
			HttpServletRequest request,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		return itemCategoryListPort.getItemMainCategoryList(companyId);
	}

	@GetMapping(value = "/ecx.item.maincategory.detail.get", name = "开放接口获取商品主类目详情")
	public Object getItemMainCategoryDetail(
			HttpServletRequest request,
			@RequestParam(name = "category", required = false) String categoryParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String categoryRaw = OpenapiRequestParams.originalString(categoryParam, body, "category");
		return mainCategoryDetailPort.getItemMainCategoryDetail(companyId, categoryRaw);
	}

	@PostMapping(value = "/ecx.item.category.add", name = "开放接口新增商品分类")
	public Map<String, Object> createItemCategory(
			HttpServletRequest request,
			@RequestParam(name = "category_name", required = false) String categoryNameParam,
			@RequestParam(name = "sort", required = false) String sortParam,
			@RequestParam(name = "image_url", required = false) String imageUrlParam,
			@RequestParam(name = "parent_id", required = false) String parentIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String categoryNameRaw = OpenapiRequestParams.originalString(categoryNameParam, body, "category_name");
		String sortRaw = OpenapiRequestParams.originalString(sortParam, body, "sort");
		String imageUrlRaw = OpenapiRequestParams.originalString(imageUrlParam, body, "image_url");
		String parentIdRaw = OpenapiRequestParams.originalString(parentIdParam, body, "parent_id");
		return itemCategoryCreatePort.createItemCategory(
				companyId, categoryNameRaw, sortRaw, imageUrlRaw, parentIdRaw);
	}

	@PostMapping(value = "/ecx.item.category.update", name = "开放接口更新商品分类")
	public Map<String, Object> updateItemCategory(
			HttpServletRequest request,
			@RequestParam(name = "category_id", required = false) String categoryIdParam,
			@RequestParam(name = "category_name", required = false) String categoryNameParam,
			@RequestParam(name = "sort", required = false) String sortParam,
			@RequestParam(name = "image_url", required = false) String imageUrlParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String categoryIdRaw = OpenapiRequestParams.originalString(categoryIdParam, body, "category_id");
		String categoryNameRaw = OpenapiRequestParams.originalString(categoryNameParam, body, "category_name");
		String sortRaw = OpenapiRequestParams.originalString(sortParam, body, "sort");
		String imageUrlRaw = OpenapiRequestParams.originalString(imageUrlParam, body, "image_url");
		return itemCategoryUpdatePort.updateItemCategory(
				companyId, categoryIdRaw, categoryNameRaw, sortRaw, imageUrlRaw);
	}

	@DeleteMapping(value = "/ecx.item.category.delete", name = "开放接口删除商品分类")
	public Map<String, Object> deleteItemCategory(
			HttpServletRequest request,
			@RequestParam(name = "category_id", required = false) String categoryIdParam,
			@FlexibleBody(required = false) Map<String, Object> body) {
		long companyId = requireCompanyId(request);
		String categoryIdRaw = OpenapiRequestParams.originalString(categoryIdParam, body, "category_id");
		return itemCategoryDeletePort.deleteItemCategory(companyId, categoryIdRaw);
	}
}
