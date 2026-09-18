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

package cn.shopex.ecshopx.goods.service.espier;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.goods.domain.ItemsCategory;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SupplierSaleCategoryPathIdsResolver {

	private final ItemsCategoryRepository itemsCategoryRepository;

	public SupplierSaleCategoryPathIdsResolver(ItemsCategoryRepository itemsCategoryRepository) {
		this.itemsCategoryRepository = itemsCategoryRepository;
	}

	public List<Long> resolve(long companyId, long distributorId, String itemCategoryCell, boolean supplierMode) {
		if (!StringUtils.hasText(itemCategoryCell)) {
			if (supplierMode) {
				return List.of();
			}
			throw new BadRequestException("请上传销售分类");
		}
		List<String> pipeSegments = new ArrayList<>();
		for (String seg : itemCategoryCell.split("\\|")) {
			String t = seg.trim();
			if (StringUtils.hasText(t)) {
				pipeSegments.add(t);
			}
		}
		Set<String> allNames = new LinkedHashSet<>();
		for (String seg : pipeSegments) {
			for (String p : seg.split("->")) {
				if (StringUtils.hasText(p.trim())) {
					allNames.add(p.trim());
				}
			}
		}
		if (allNames.isEmpty()) {
			throw new BadRequestException("上传商品分类参数有误");
		}
		List<ItemsCategory> list = itemsCategoryRepository.listNonMainByCompanyDistributorAndNames(companyId, distributorId, allNames);
		if (list.isEmpty()) {
			throw new BadRequestException("上传商品分类参数有误");
		}
		Map<Long, String> categoryNameById = new LinkedHashMap<>();
		for (ItemsCategory c : list) {
			if (c.getCategoryId() != null && StringUtils.hasText(c.getCategoryName())) {
				categoryNameById.put(c.getCategoryId(), c.getCategoryName());
			}
		}
		Map<Long, String> catNamePath = new LinkedHashMap<>();
		for (ItemsCategory catRow : list) {
			Long cid = catRow.getCategoryId();
			if (cid == null || !StringUtils.hasText(catRow.getPath())) {
				continue;
			}
			String[] path = catRow.getPath().split(",");
			StringBuilder sb = new StringBuilder();
			for (String idRaw : path) {
				if (!StringUtils.hasText(idRaw)) {
					continue;
				}
				long id;
				try {
					id = Long.parseLong(idRaw.trim());
				} catch (NumberFormatException e) {
					continue;
				}
				String nm = categoryNameById.get(id);
				if (!StringUtils.hasText(nm)) {
					continue;
				}
				if (sb.length() > 0) {
					sb.append("->");
				}
				sb.append(nm);
			}
			if (sb.length() > 0) {
				catNamePath.put(cid, sb.toString());
			}
		}
		List<Long> catIds = new ArrayList<>();
		for (Map.Entry<Long, String> e : catNamePath.entrySet()) {
			if (pipeSegments.contains(e.getValue())) {
				catIds.add(e.getKey());
			}
		}
		if (catIds.isEmpty()) {
			throw new BadRequestException("上传商品分类参数有误");
		}
		return catIds;
	}
}
