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
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.ItemsAttributeValues;
import cn.shopex.ecshopx.goods.domain.ItemsAttributes;
import cn.shopex.ecshopx.goods.repository.ItemsAttributeValuesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsAttributesRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ItemsAttributesCreateService {

	private final ItemsAttributesRepository itemsAttributesRepository;
	private final ItemsAttributeValuesRepository itemsAttributeValuesRepository;
	private final ItemsAttributesMultiLangApplier itemsAttributesMultiLangApplier;

	public ItemsAttributesCreateService(ItemsAttributesRepository itemsAttributesRepository,
			ItemsAttributeValuesRepository itemsAttributeValuesRepository,
			ItemsAttributesMultiLangApplier itemsAttributesMultiLangApplier) {
		this.itemsAttributesRepository = itemsAttributesRepository;
		this.itemsAttributeValuesRepository = itemsAttributeValuesRepository;
		this.itemsAttributesMultiLangApplier = itemsAttributesMultiLangApplier;
	}

	@Transactional(rollbackFor = Exception.class)
	public void createAttr(long companyId, Map<String, Object> input, String countryCode) {
		String attributeType = stringOrNull(input.get("attribute_type"));
		Object rawName = input.get("attribute_name");
		String attributeName = rawName == null ? "" : rawName.toString().trim();
		String attributeMemo = "";
		if (input.get("attribute_memo") != null) {
			attributeMemo = input.get("attribute_memo").toString();
		}
		long shopId = 0L;
		if (input.get("shop_id") != null) {
			shopId = parseLongOrZero(input.get("shop_id"));
		}
		String attributeSort = "1";
		if (input.get("attribute_sort") != null) {
			attributeSort = input.get("attribute_sort").toString();
		}
		long distributorId = 0L;
		if (input.get("distributor_id") != null) {
			distributorId = parseLongOrZero(input.get("distributor_id"));
		}
		String isShow = "true";
		if (input.get("is_show") != null) {
			isShow = input.get("is_show").toString();
		}
		String isImage = "true";
		if (input.get("is_image") != null) {
			isImage = input.get("is_image").toString();
		}
		String imageUrl = "";
		if (input.get("image_url") != null) {
			imageUrl = input.get("image_url").toString();
		}

		List<Map<String, Object>> attributeValuesList = null;
		Object avRaw = input.get("attribute_values");
		if (avRaw instanceof List<?> list) {
			attributeValuesList = new ArrayList<>();
			for (Object o : list) {
				if (o instanceof Map<?, ?> m) {
					LinkedHashMap<String, Object> row = new LinkedHashMap<>();
					for (Map.Entry<?, ?> e : m.entrySet()) {
						row.put(String.valueOf(e.getKey()), e.getValue());
					}
					attributeValuesList.add(row);
				}
			}
		}

		if (attributeValuesList != null && attributeValuesList.size() > 60) {
			throw new ResourceException("参数或规格值不能超过60个");
		}
		if (!Boolean.TRUE.equals(input.get("from_oms"))
				&& attributeValuesList != null && attributeValuesList.size() <= 0 && Objects.equals(input.get("is_show"), "true")) {
			throw new ResourceException("请添加参数或者规格值");
		}
		if ("brand".equals(attributeType)
				&& itemsAttributesRepository.existsBrandByCompanyAndName(companyId, attributeName)) {
			throw new ResourceException("品牌名称不能重复");
		}

		try {
			int now = (int) (System.currentTimeMillis() / 1000L);
			ItemsAttributes attr = new ItemsAttributes();
			attr.setCompanyId(companyId);
			attr.setShopId(shopId);
			attr.setAttributeType(attributeType);
			attr.setAttributeName(attributeName);
			attr.setAttributeMemo(attributeMemo);
			attr.setAttributeSort(attributeSort);
			attr.setDistributorId(distributorId);
			attr.setIsShow(isShow);
			attr.setIsImage(isImage);
			attr.setImageUrl(imageUrl);
			attr.setCreated(now);
			attr.setUpdated(now);
			Object codeObj = input.get("attribute_code");
			if (codeObj != null && StringUtils.hasText(codeObj.toString())) {
				attr.setAttributeCode(codeObj.toString());
			}

			itemsAttributesRepository.insert(attr);
			Long attributeId = attr.getAttributeId();
			if (attributeId == null) {
				throw new ResourceException("创建属性失败");
			}

			itemsAttributesMultiLangApplier.afterAttributeInsert(companyId, attributeId, attributeName, attributeMemo, countryCode);

			if (attributeValuesList != null && !attributeValuesList.isEmpty()) {
				Map<String, Boolean> seenTrimmed = new HashMap<>();
				for (int key = 0; key < attributeValuesList.size(); key++) {
					Map<String, Object> value = attributeValuesList.get(key);
					Object avObj = value.get("attribute_value");
					String attrValText = avObj == null ? "" : avObj.toString();
					if (!StringUtils.hasText(attrValText)) {
						throw new ResourceException("参数值不能为空");
					}
					String trimmed = attrValText.trim();
					if (seenTrimmed.containsKey(trimmed)) {
						continue;
					}
					seenTrimmed.put(trimmed, Boolean.TRUE);

					ItemsAttributeValues ve = new ItemsAttributeValues();
					ve.setAttributeId(attributeId);
					ve.setCompanyId(companyId);
					ve.setShopId(shopId);
					ve.setAttributeValue(trimmed);
					ve.setSort(String.valueOf(key));
					String rowImageUrl = "";
					if (value.get("image_url") != null) {
						rowImageUrl = value.get("image_url").toString();
					}
					ve.setImageUrl(rowImageUrl);
					ve.setCreated(now);
					ve.setUpdated(now);
					Object oms = value.get("oms_value_id");
					long omsVal = oms == null ? 0L : parseLongOrZero(oms);
					if (omsVal != 0L) {
						ve.setOmsValueId(omsVal);
					}

					itemsAttributeValuesRepository.insert(ve);
					Long attributeValueId = ve.getAttributeValueId();
					if (attributeValueId == null) {
						throw new ResourceException("创建属性值失败");
					}
					itemsAttributesMultiLangApplier.afterAttributeValueInsert(companyId, attributeValueId, trimmed, countryCode);
				}
			}
		} catch (Exception e) {
			throw new ResourceException(e.getMessage());
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public boolean createItemsParamsAttributeValue(Map<String, Object> params, String countryCode) {
		long companyId = parseRequiredLongParam(params.get("company_id"), "company_id 缺失或无效");
		long attributeId = parseRequiredAttributeId(params.get("attribute_id"));

		ItemsAttributes row = itemsAttributesRepository.selectByCompanyAndAttributeId(companyId, attributeId);
		if (row == null) {
			throw new ResourceException("属性不存在attribute_id:" + attributeId);
		}

		Object avRaw = params.get("attribute_value");
		if (avRaw == null) {
			throw new BadRequestException("必须填写attribute_value");
		}
		String trimmed = avRaw.toString().trim();
		if (!StringUtils.hasText(trimmed)) {
			throw new BadRequestException("必须填写attribute_value");
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		ItemsAttributeValues ve = new ItemsAttributeValues();
		ve.setAttributeId(attributeId);
		ve.setCompanyId(companyId);
		ve.setShopId(explicitParam(params, "shop_id") ? parseLongOrZero(params.get("shop_id")) : 0L);
		ve.setAttributeValue(trimmed);
		ve.setSort(explicitParam(params, "sort") ? String.valueOf(params.get("sort")) : "0");
		ve.setImageUrl(explicitParam(params, "image_url") ? params.get("image_url").toString() : "");
		ve.setCreated(now);
		ve.setUpdated(now);

		itemsAttributeValuesRepository.insert(ve);
		Long id = ve.getAttributeValueId();
		if (id == null || id < 1) {
			return false;
		}
		itemsAttributesMultiLangApplier.afterAttributeValueInsert(companyId, id, trimmed, countryCode);
		return id != null && id > 0;
	}

	private static boolean explicitParam(Map<String, Object> params, String key) {
		return params.containsKey(key) && params.get(key) != null;
	}

	private static long parseRequiredLongParam(Object v, String message) {
		if (v == null) {
			throw new BadRequestException(message);
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(message);
		}
	}

	private static long parseRequiredAttributeId(Object v) {
		if (v == null) {
			throw new BadRequestException("attribute_id 缺失或无效");
		}
		long id;
		if (v instanceof Number n) {
			id = n.longValue();
		} else {
			try {
				id = Long.parseLong(v.toString().trim());
			} catch (NumberFormatException e) {
				throw new BadRequestException("attribute_id 缺失或无效");
			}
		}
		if (id < 1) {
			throw new BadRequestException("attribute_id 缺失或无效");
		}
		return id;
	}

	private static String stringOrNull(Object o) {
		return o == null ? null : o.toString();
	}

	private static long parseLongOrZero(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
