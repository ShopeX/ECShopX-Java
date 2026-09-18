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

package cn.shopex.ecshopx.goods.openapi.thirdapi.v2;

import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiItemsV2FailException;
import cn.shopex.ecshopx.goods.domain.ItemsCategory;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import cn.shopex.ecshopx.goods.service.ItemsCategoryUpdateService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2ItemCategoryUpdateService {

	private final ItemsCategoryUpdateService itemsCategoryUpdateService;
	private final ItemsCategoryRepository itemsCategoryRepository;

	public OpenapiThirdApiV2ItemCategoryUpdateService(
			ItemsCategoryUpdateService itemsCategoryUpdateService,
			ItemsCategoryRepository itemsCategoryRepository) {
		this.itemsCategoryUpdateService = itemsCategoryUpdateService;
		this.itemsCategoryRepository = itemsCategoryRepository;
	}

	public Map<String, Object> executeOpenapiUpdateItemCategory(
			long companyId,
			String categoryIdRaw,
			String categoryNameRaw,
			String sortRaw,
			String imageUrlRaw) {
		try {
			validateCategoryId(categoryIdRaw);
			validateCategoryName(categoryNameRaw);
			long categoryId = parseCategoryIdForUpdate(categoryIdRaw);
			ItemsCategory existing = itemsCategoryRepository
					.findEntityByCompanyAndCategoryId(companyId, categoryId)
					.orElseThrow(OpenapiThirdApiV2ItemCategoryUpdateService::categoryNotFound);
			assertNoDuplicateName(companyId, categoryId, existing, categoryNameRaw);
			Map<String, Object> body = buildUpdateBody(categoryNameRaw, sortRaw, imageUrlRaw);
			itemsCategoryUpdateService.updateCategory(companyId, categoryId, body, "zh-CN");
		} catch (OpenapiItemsV2FailException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new OpenapiItemsV2FailException(
					OpenapiErrorCode.GOODS_CATEGORY_ERROR, ex.getMessage());
		}
		return Map.of("status", Boolean.TRUE);
	}

	private void validateCategoryId(String categoryIdRaw) {
		if (categoryIdRaw == null || categoryIdRaw.isEmpty()) {
			throw missingParams("分类ID必填");
		}
	}

	private void validateCategoryName(String categoryNameRaw) {
		if (categoryNameRaw == null || categoryNameRaw.isEmpty()) {
			throw missingParams("分类名称必填");
		}
	}

	private static OpenapiItemsV2FailException missingParams(String message) {
		return new OpenapiItemsV2FailException(OpenapiErrorCode.SERVICE_MISSING_PARAMS, message);
	}

	private long parseCategoryIdForUpdate(String categoryIdRaw) {
		String trimmed = categoryIdRaw.trim();
		if (trimmed.isEmpty()) {
			throw categoryNotFound();
		}
		try {
			return Long.parseLong(trimmed);
		} catch (NumberFormatException e) {
			throw categoryNotFound();
		}
	}

	private static OpenapiItemsV2FailException categoryNotFound() {
		return new OpenapiItemsV2FailException(
				OpenapiErrorCode.GOODS_CATEGORY_ERROR, "更新的分类不存在");
	}

	private void assertNoDuplicateName(
			long companyId,
			long categoryId,
			ItemsCategory existing,
			String categoryNameRaw) {
		String newName = categoryNameRaw == null ? "" : categoryNameRaw.trim();
		String oldName = existing.getCategoryName() == null ? "" : existing.getCategoryName().trim();
		if (Objects.equals(oldName, newName)) {
			return;
		}
		long parentId = existing.getParentId() == null ? 0L : existing.getParentId();
		long distributorId = existing.getDistributorId() == null ? 0L : existing.getDistributorId();
		Optional<ItemsCategory> duplicate = itemsCategoryRepository.findDuplicateForClassification(
				companyId,
				categoryNameRaw,
				parentId,
				Boolean.TRUE.equals(existing.getIsMainCategory()),
				distributorId);
		if (duplicate.isPresent() && duplicate.get().getCategoryId() != categoryId) {
			throw new OpenapiItemsV2FailException(
					OpenapiErrorCode.GOODS_CATEGORY_ERROR, "分类名称不能重复");
		}
	}

	private Map<String, Object> buildUpdateBody(
			String categoryNameRaw,
			String sortRaw,
			String imageUrlRaw) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("category_name", categoryNameRaw);
		if (sortRaw != null) {
			body.put("sort", sortRaw);
		}
		if (imageUrlRaw != null) {
			body.put("image_url", imageUrlRaw);
		}
		body.put("customize_page_id", 0L);
		return body;
	}
}
