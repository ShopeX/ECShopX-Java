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

package cn.shopex.ecshopx.goods.service.wechat;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.wechat.WechatCustomizePageCategoryBindFacade;
import cn.shopex.ecshopx.goods.domain.ItemsCategory;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import cn.shopex.ecshopx.goods.service.ItemsCategoryDistributorIdResolver;
import cn.shopex.ecshopx.goods.service.ItemsCategoryFrontDisplayResolver;
import cn.shopex.ecshopx.wechat.domain.WeappCustomizePage;
import cn.shopex.ecshopx.wechat.repository.WeappCustomizePageRepository;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class WechatCustomizePageCategoryBindFacadeImpl implements WechatCustomizePageCategoryBindFacade {

	private final WeappCustomizePageRepository weappCustomizePageRepository;
	private final ItemsCategoryRepository itemsCategoryRepository;
	private final ItemsCategoryDistributorIdResolver distributorIdResolver;
	private final ItemsCategoryFrontDisplayResolver frontDisplayResolver;

	public WechatCustomizePageCategoryBindFacadeImpl(WeappCustomizePageRepository weappCustomizePageRepository,
			ItemsCategoryRepository itemsCategoryRepository, ItemsCategoryDistributorIdResolver distributorIdResolver,
			ItemsCategoryFrontDisplayResolver frontDisplayResolver) {
		this.weappCustomizePageRepository = weappCustomizePageRepository;
		this.itemsCategoryRepository = itemsCategoryRepository;
		this.distributorIdResolver = distributorIdResolver;
		this.frontDisplayResolver = frontDisplayResolver;
	}

	@Override
	public Map<String, Object> bindCategoryId(long companyId, long jwtDistributorId, long customizePageId, long regionauthId,
			long categoryId) {
		WeappCustomizePage page = weappCustomizePageRepository
				.findOneByIdCompanyIdAndRegionauthId(customizePageId, companyId, regionauthId)
				.orElseThrow(() -> new ResourceException("页面不存在"));
		String pageType = page.getPageType();
		if (!"category".equals(pageType)) {
			throw new ResourceException("只能绑定分类页");
		}
		ItemsCategory row = itemsCategoryRepository
				.findOneByCompanyIdRegionauthIdAndCategoryId(companyId, regionauthId, categoryId)
				.orElseThrow(() -> new ResourceException("分类不存在"));
		String productModel = distributorIdResolver.resolveProductModel(companyId);
		int level = row.getCategoryLevel() == null ? 0 : row.getCategoryLevel();
		boolean isMain = Boolean.TRUE.equals(row.getIsMainCategory());
		String bindError = frontDisplayResolver.validateCustomizePageBindCategory(level, isMain, productModel);
		if (bindError != null) {
			throw new ResourceException(bindError);
		}
		Optional<ItemsCategory> occupant = itemsCategoryRepository.findOneForCustomizePageOccupant(companyId, regionauthId,
				customizePageId, jwtDistributorId);
		if (occupant.isPresent()) {
			itemsCategoryRepository.clearCustomizePageIdForOccupant(companyId, regionauthId, customizePageId, jwtDistributorId);
		}
		itemsCategoryRepository.updateCustomizePageIdByCompanyRegionauthAndCategoryId(companyId, regionauthId, categoryId,
				customizePageId);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("status", Boolean.TRUE);
		return data;
	}

	@Override
	public List<Map<String, Object>> listCategoriesForCustomizePageList(long companyId, long jwtDistributorId,
			Collection<Long> customizePageIds) {
		if (customizePageIds == null || customizePageIds.isEmpty()) {
			return List.of();
		}
		String productModel = distributorIdResolver.resolveProductModel(companyId);
		boolean bindMain = frontDisplayResolver.isCustomizePageBindMainCategory(productModel);
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("customize_page_id_in", customizePageIds);
		filter.put("parent_id", 0);
		filter.put("category_level", 1);
		filter.put("is_main_category", bindMain);
		return itemsCategoryRepository.lists(filter, companyId, jwtDistributorId, 1, 0);
	}
}
