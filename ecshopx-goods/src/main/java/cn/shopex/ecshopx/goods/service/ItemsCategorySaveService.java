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

package cn.shopex.ecshopx.goods.service;

import cn.shopex.ecshopx.common.dispatch.GoodsItemCategoryAddDispatchPublisher;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.shuyun.ShuyunOpenPlatformCategorySyncPort;
import cn.shopex.ecshopx.goods.domain.ItemsCategory;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import cn.shopex.ecshopx.goods.service.dto.CategoryTreeNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
public class ItemsCategorySaveService {

	private final GoodsItemCategoryAddDispatchPublisher goodsItemCategoryAddDispatchPublisher;
	private final ItemsCategoryRepository itemsCategoryRepository;
	private final ItemsCategoryDistributorIdResolver distributorIdResolver;
	private final ObjectMapper objectMapper;
	private final ShuyunOpenPlatformCategorySyncPort openPlatformCategorySyncPort;

	public ItemsCategorySaveService(
			GoodsItemCategoryAddDispatchPublisher goodsItemCategoryAddDispatchPublisher,
			ItemsCategoryRepository itemsCategoryRepository,
			ItemsCategoryDistributorIdResolver distributorIdResolver,
			ObjectMapper objectMapper,
			ShuyunOpenPlatformCategorySyncPort openPlatformCategorySyncPort) {
		this.goodsItemCategoryAddDispatchPublisher = goodsItemCategoryAddDispatchPublisher;
		this.itemsCategoryRepository = itemsCategoryRepository;
		this.distributorIdResolver = distributorIdResolver;
		this.objectMapper = objectMapper;
		this.openPlatformCategorySyncPort = openPlatformCategorySyncPort;
	}

	@Transactional(rollbackFor = Exception.class)
	public void saveItemsCategory(long companyId, long distributorId, List<CategoryTreeNode> rootNodes) {
		try {
			validateParamsTree(rootNodes);
			for (CategoryTreeNode n : rootNodes) {
				applyCustomizePageDefaults(n);
			}
			checkCategoryData(rootNodes);
			Set<Long> syncCategoryIds = new LinkedHashSet<>();
			saveCategoryLevel(rootNodes, companyId, distributorId, 1, 0L, "", syncCategoryIds);
			registerItemCategoryAddDispatchAfterCommit(companyId, syncCategoryIds);
		} catch (BadRequestException e) {
			throw e;
		} catch (ResourceException e) {
			throw e;
		} catch (Exception e) {
			String msg = e.getMessage();
			throw new ResourceException(msg != null && !msg.isEmpty() ? msg : "保存失败");
		}
	}

	private void registerItemCategoryAddDispatchAfterCommit(long companyId, Set<Long> syncCategoryIds) {
		if (!TransactionSynchronizationManager.isActualTransactionActive()
				|| !TransactionSynchronizationManager.isSynchronizationActive()) {
			return;
		}
		List<Long> ids = new ArrayList<>(syncCategoryIds);
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				goodsItemCategoryAddDispatchPublisher.publish(companyId);
				for (Long cid : ids) {
					if (cid != null && cid > 0) {
						openPlatformCategorySyncPort.dispatchIfAuthAllows(companyId, cid);
					}
				}
			}
		});
	}

	private void validateParamsTree(List<CategoryTreeNode> nodes) {
		for (CategoryTreeNode n : nodes) {
			validateOneParams(n);
		}
	}

	private void validateOneParams(CategoryTreeNode n) {
		if (n.getCategoryName() == null || !StringUtils.hasText(n.getCategoryName().trim())) {
			throw new BadRequestException("分类名称必填");
		}
		if (n.isCategoryIdKeyPresent()) {
			if (n.getCategoryId() == null || n.getCategoryId() <= 0) {
				throw new BadRequestException("category_id 无效");
			}
		}
		for (CategoryTreeNode c : n.getChildrenOrEmpty()) {
			validateOneParams(c);
		}
	}

	private void applyCustomizePageDefaults(CategoryTreeNode n) {
		if (n.isCustomizePageIdKeyPresent() && isEmptyCustomizePage(n.getCustomizePageId())) {
			n.setCustomizePageId(0L);
		}
		for (CategoryTreeNode c : n.getChildrenOrEmpty()) {
			applyCustomizePageDefaults(c);
		}
	}

	private static boolean isEmptyCustomizePage(Long v) {
		return v == null || v == 0L;
	}

	private void checkCategoryData(List<CategoryTreeNode> nodes) {
		for (CategoryTreeNode n : nodes) {
			checkOneCategoryData(n);
		}
	}

	private void checkOneCategoryData(CategoryTreeNode n) {
		String name = n.getCategoryName();
		if (name == null || !visibleText(name)) {
			throw new BadRequestException("分类名称必填");
		}
		if (name.getBytes(StandardCharsets.UTF_8).length > 50) {
			throw new BadRequestException("分类名称长度最多16个汉字或50个字符");
		}
		for (CategoryTreeNode c : n.getChildrenOrEmpty()) {
			checkOneCategoryData(c);
		}
	}

	private static boolean visibleText(String s) {
		return StringUtils.hasText(s.trim());
	}

	private void saveCategoryLevel(
			List<CategoryTreeNode> rows,
			long companyId,
			long jwtDistributorId,
			int level,
			long parentId,
			String parentPath,
			Set<Long> syncCategoryIds) {
		for (CategoryTreeNode row : rows) {
			if (row.getCategoryCode() != null && !row.getCategoryCode().isEmpty()) {
				Optional<ItemsCategory> exist =
						itemsCategoryRepository.getByCategoryCode(row.getCategoryCode(), companyId);
				exist.ifPresent(c -> row.setCategoryId(c.getCategoryId()));
			}
			boolean isUpdate = row.getCategoryId() != null && row.getCategoryId() > 0;
			ItemsCategory entity = buildEntity(row, companyId, level, parentId, parentPath, isUpdate);
			entity.setDistributorId(jwtDistributorId);
			row.setPendingDistributorIdBeforeResolve(jwtDistributorId);
			row.setMergedParentIdKeyPresent(true);
			row.setMergedCategoryIdKeyPresent(isUpdate);
			entity.setDistributorId(distributorIdResolver.resolveForCategoryNode(row, companyId, jwtDistributorId));

			if (isUpdate) {
				itemsCategoryRepository.updateOneBy(entity, row.getCategoryId(), companyId);
			} else {
				itemsCategoryRepository.insert(entity);
			}
			Long catId = entity.getCategoryId();
			if (catId == null) {
				throw new ResourceException("保存失败");
			}
			if (level == 2 || level == 3) {
				syncCategoryIds.add(catId);
			}
			String newPath =
					parentPath != null && !parentPath.isEmpty() ? parentPath + "," + catId : String.valueOf(catId);
			itemsCategoryRepository.updatePath(catId, companyId, newPath);

			if (!row.getChildrenOrEmpty().isEmpty()) {
				saveCategoryLevel(
						row.getChildren(), companyId, jwtDistributorId, level + 1, catId, newPath, syncCategoryIds);
			}
		}
	}

	private ItemsCategory buildEntity(
			CategoryTreeNode row, long companyId, int level, long parentId, String parentPath, boolean isUpdate) {
		ItemsCategory e = new ItemsCategory();
		if (isUpdate) {
			e.setCategoryId(row.getCategoryId());
		}
		e.setCompanyId(companyId);
		e.setCategoryName(row.getCategoryName());
		e.setParentId(parentId);
		e.setCategoryLevel(level);
		e.setPath(parentPath != null ? parentPath : "");
		e.setCategoryCode(row.getCategoryCode() != null ? row.getCategoryCode() : "");
		e.setSort(row.getSort() != null ? row.getSort() : 0L);
		e.setIsMainCategory(row.getIsMainCategory() != null ? row.getIsMainCategory() : false);
		if (row.getIsShowFront() != null) {
			e.setIsShowFront(row.getIsShowFront());
		}
		e.setGoodsParams(jsonNodeToString(row.getGoodsParams()));
		e.setGoodsSpec(jsonNodeToString(row.getGoodsSpec()));
		e.setImageUrl(row.getImageUrl());
		e.setCategoryIdTaobao(row.getCategoryIdTaobao() != null ? row.getCategoryIdTaobao() : 0L);
		e.setParentIdTaobao(row.getParentIdTaobao() != null ? row.getParentIdTaobao() : 0L);
		e.setTaobaoCategoryInfo(jsonNodeToString(row.getTaobaoCategoryInfo()));
		if (row.getCustomizePageId() != null) {
			e.setCustomizePageId(row.getCustomizePageId());
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		e.setCreated(now);
		e.setUpdated(now);
		return e;
	}

	private String jsonNodeToString(JsonNode n) {
		if (n == null || n.isNull()) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(n);
		} catch (JsonProcessingException ex) {
			throw new ResourceException("goods 参数序列化失败");
		}
	}
}
