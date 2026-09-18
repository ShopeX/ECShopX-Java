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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.goods.domain.ItemsAttributeValues;
import cn.shopex.ecshopx.goods.domain.ItemsAttributes;
import cn.shopex.ecshopx.goods.repository.ItemsAttributeValuesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsAttributesRepository;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ItemsAttributesQueryService {

	private final ItemsAttributesRepository itemsAttributesRepository;
	private final ItemsAttributeValuesRepository itemsAttributeValuesRepository;
	private final ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver;
	private final ItemsAttributesMultiLangApplier itemsAttributesMultiLangApplier;

	public ItemsAttributesQueryService(ItemsAttributesRepository itemsAttributesRepository,
			ItemsAttributeValuesRepository itemsAttributeValuesRepository,
			ItemsCategoryDistributorIdResolver itemsCategoryDistributorIdResolver,
			ItemsAttributesMultiLangApplier itemsAttributesMultiLangApplier) {
		this.itemsAttributesRepository = itemsAttributesRepository;
		this.itemsAttributeValuesRepository = itemsAttributeValuesRepository;
		this.itemsCategoryDistributorIdResolver = itemsCategoryDistributorIdResolver;
		this.itemsAttributesMultiLangApplier = itemsAttributesMultiLangApplier;
	}

	public Map<String, Object> getAttrList(long companyId, long jwtDistributorId, String attributeType, String attributeName,
			List<Long> attributeIdFilter, String distributorIdQuery, int page, int pageSize, String countryCode) {
		validatePaging(page, pageSize);

		String attributeNameContains = StringUtils.hasText(attributeName) ? attributeName.trim() : null;
		List<Long> attributeIdsOrNull = (attributeIdFilter != null && !attributeIdFilter.isEmpty()) ? attributeIdFilter : null;

		Long distributorIdEqOrNull = null;
		if ("brand".equals(attributeType)) {
			String productModel = itemsCategoryDistributorIdResolver.resolveProductModel(companyId);
			if ("platform".equals(productModel) && !"all".equals(distributorIdQuery)) {
				distributorIdEqOrNull = jwtDistributorId;
			}
		}

		IPage<ItemsAttributes> pageResult = itemsAttributesRepository.selectPageByFilter(companyId, attributeType, attributeNameContains,
				attributeIdsOrNull, distributorIdEqOrNull, page, pageSize);

		long totalCount = pageResult.getTotal();
		List<ItemsAttributes> records = pageResult.getRecords();

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);

		if (totalCount == 0) {
			out.put("list", List.of());
			return out;
		}

		List<Map<String, Object>> rowMaps = new ArrayList<>();
		for (ItemsAttributes rec : records) {
			rowMaps.add(ItemsAttributesRowMaps.toAttributeRowMap(rec));
		}
		itemsAttributesMultiLangApplier.applyListLangForAttributes(companyId, countryCode, rowMaps);

		for (Map<String, Object> row : rowMaps) {
			Object typeObj = row.get("attribute_type");
			String type = typeObj != null ? typeObj.toString() : null;
			if ("item_spec".equals(type) || "item_params".equals(type)) {
				Object aidObj = row.get("attribute_id");
				long attributeId = aidObj instanceof Number n ? n.longValue() : Long.parseLong(aidObj.toString());
				List<ItemsAttributeValues> vals = itemsAttributeValuesRepository.listByAttributeId(companyId, attributeId);
				List<Map<String, Object>> valMaps = new ArrayList<>();
				for (ItemsAttributeValues v : vals) {
					valMaps.add(ItemsAttributesRowMaps.toAttributeValueRowMap(v));
				}
				itemsAttributesMultiLangApplier.applyListLangForAttributeValues(companyId, countryCode, valMaps);
				row.put("attribute_values", ItemsAttributesRowMaps.buildAttributeValuesNested(valMaps));
			}
		}

		out.put("list", rowMaps);
		return out;
	}

	private static void validatePaging(int page, int pageSize) {
		if (page < 1) {
			throw new BadRequestException("分页参数错误");
		}
		if (pageSize < 1 || pageSize > 1000) {
			throw new BadRequestException("每页最多查询1000条数据");
		}
	}
}
