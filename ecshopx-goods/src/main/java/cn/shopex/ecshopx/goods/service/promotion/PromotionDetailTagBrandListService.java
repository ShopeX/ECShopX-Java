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

package cn.shopex.ecshopx.goods.service.promotion;

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.goods.domain.ItemsAttributes;
import cn.shopex.ecshopx.goods.domain.ItemsTags;
import cn.shopex.ecshopx.goods.repository.ItemsAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsTagsListFilter;
import cn.shopex.ecshopx.goods.repository.ItemsTagsRepository;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromotionDetailTagBrandListService {

	private final ItemsTagsRepository itemsTagsRepository;
	private final DistributorListQueryService distributorListQueryService;
	private final ItemsAttributesRepository itemsAttributesRepository;

	public PromotionDetailTagBrandListService(
			ItemsTagsRepository itemsTagsRepository,
			DistributorListQueryService distributorListQueryService,
			ItemsAttributesRepository itemsAttributesRepository) {
		this.itemsTagsRepository = itemsTagsRepository;
		this.distributorListQueryService = distributorListQueryService;
		this.itemsAttributesRepository = itemsAttributesRepository;
	}

	public List<Map<String, Object>> listTagsForDetail(long companyId, List<Long> tagIds) {
		if (tagIds == null || tagIds.isEmpty()) {
			return List.of();
		}
		ItemsTagsListFilter filter = new ItemsTagsListFilter();
		filter.setCompanyId(companyId);
		filter.setTagIdsIn(tagIds);
		Page<ItemsTags> page = new Page<>(1, 500);
		IPage<ItemsTags> p = itemsTagsRepository.selectPageByFilter(page, filter);
		List<ItemsTags> rows = p.getRecords();
		if (rows.isEmpty()) {
			return List.of();
		}
		List<Long> distIds =
				rows.stream().map(ItemsTags::getDistributorId).filter(Objects::nonNull).filter(id -> id > 0).distinct().toList();
		Map<Long, Distributor> distById = Map.of();
		if (!distIds.isEmpty()) {
			distById = distributorListQueryService.listByIdsAndCompany(companyId, distIds).stream()
					.collect(Collectors.toMap(Distributor::getDistributorId, d -> d, (a, b) -> a));
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (ItemsTags t : rows) {
			out.add(itemsTagToRow(t, distById));
		}
		return out;
	}

	public List<Map<String, Object>> listBrandsForDetail(long companyId, List<Long> brandIds) {
		if (brandIds == null || brandIds.isEmpty()) {
			return List.of();
		}
		List<ItemsAttributes> attrs = itemsAttributesRepository.listByCompanyAndAttributeIdsIn(companyId, brandIds);
		List<Map<String, Object>> out = new ArrayList<>();
		for (ItemsAttributes a : attrs) {
			if (a.getAttributeType() != null && "brand".equalsIgnoreCase(a.getAttributeType().trim())) {
				out.add(itemsAttributeToRow(a));
			}
		}
		return out;
	}

	private static Map<String, Object> itemsTagToRow(ItemsTags t, Map<Long, Distributor> distById) {
		Map<String, Object> m = new LinkedHashMap<>();
		putLong(m, "tag_id", t.getTagId());
		putLong(m, "company_id", t.getCompanyId());
		m.put("tag_name", t.getTagName());
		m.put("tag_color", t.getTagColor());
		putLong(m, "distributor_id", t.getDistributorId());
		m.put("font_color", t.getFontColor());
		m.put("description", t.getDescription());
		m.put("tag_icon", t.getTagIcon());
		m.put("front_show", t.getFrontShow());
		m.put("created", t.getCreated());
		m.put("updated", t.getUpdated());
		long did = t.getDistributorId() != null ? t.getDistributorId() : 0L;
		if (did > 0) {
			Distributor d = distById.get(did);
			m.put("distributor_name", d != null && StringUtils.hasText(d.getName()) ? d.getName() : "");
			m.put("is_platform", false);
		} else {
			m.put("distributor_name", "平台");
			m.put("is_platform", true);
		}
		return m;
	}

	private static Map<String, Object> itemsAttributeToRow(ItemsAttributes a) {
		Map<String, Object> m = new LinkedHashMap<>();
		putLong(m, "attribute_id", a.getAttributeId());
		putLong(m, "company_id", a.getCompanyId());
		m.put("attribute_type", a.getAttributeType());
		m.put("attribute_name", a.getAttributeName());
		m.put("attribute_memo", a.getAttributeMemo());
		m.put("attribute_sort", a.getAttributeSort());
		putLong(m, "shop_id", a.getShopId());
		return m;
	}

	private static void putLong(Map<String, Object> m, String key, Long v) {
		if (v != null) {
			m.put(key, v);
		}
	}
}
