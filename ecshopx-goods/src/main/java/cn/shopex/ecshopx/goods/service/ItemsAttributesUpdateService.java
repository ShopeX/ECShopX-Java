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
import cn.shopex.ecshopx.goods.repository.ItemRelAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsAttributeValuesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsAttributesRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class ItemsAttributesUpdateService {

	private final ItemsAttributesRepository itemsAttributesRepository;
	private final ItemsAttributeValuesRepository itemsAttributeValuesRepository;
	private final ItemRelAttributesRepository itemRelAttributesRepository;
	private final ItemsAttributesMultiLangApplier itemsAttributesMultiLangApplier;
	private final TransactionTemplate transactionTemplate;

	public ItemsAttributesUpdateService(ItemsAttributesRepository itemsAttributesRepository,
			ItemsAttributeValuesRepository itemsAttributeValuesRepository,
			ItemRelAttributesRepository itemRelAttributesRepository,
			ItemsAttributesMultiLangApplier itemsAttributesMultiLangApplier,
			PlatformTransactionManager transactionManager) {
		this.itemsAttributesRepository = itemsAttributesRepository;
		this.itemsAttributeValuesRepository = itemsAttributeValuesRepository;
		this.itemRelAttributesRepository = itemRelAttributesRepository;
		this.itemsAttributesMultiLangApplier = itemsAttributesMultiLangApplier;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	public void updateAttr(long companyId, long attributeId, Map<String, Object> input, String countryCode) {
		boolean fromOms = Boolean.TRUE.equals(input.get("from_oms"));
		ItemsAttributes info = itemsAttributesRepository.selectByCompanyAndAttributeId(companyId, attributeId);
		if (info == null) {
			throw new ResourceException("更新的数据不存在");
		}

		Object rawName = input.get("attribute_name");
		String attributeName = rawName == null ? "" : rawName.toString().trim();
		if ("brand".equals(info.getAttributeType())
				&& itemsAttributesRepository.existsBrandByCompanyAndNameExcludingAttributeId(companyId, attributeName, attributeId)) {
			throw new ResourceException("品牌名称不能重复");
		}

		int now = (int) (System.currentTimeMillis() / 1000L);
		applyMainTablePatch(info, input, now);
		itemsAttributesRepository.updateById(info);

		String memo = info.getAttributeMemo() == null ? "" : info.getAttributeMemo();
		itemsAttributesMultiLangApplier.afterAttributeUpdate(companyId, attributeId, info.getAttributeName(), memo, countryCode);

		if ("brand".equals(info.getAttributeType())) {
			return;
		}

		List<ItemsAttributeValues> oldList = itemsAttributeValuesRepository.listByAttributeId(companyId, attributeId);
		if (!input.containsKey("attribute_values") && !oldList.isEmpty()) {
			throw new BadRequestException("attribute_values 不能为空");
		}

		List<Map<String, Object>> avList = null;
		if (input.containsKey("attribute_values")) {
			Object rawAv = input.get("attribute_values");
			if (rawAv instanceof List<?> list) {
				avList = new ArrayList<>();
				for (Object o : list) {
					if (o instanceof Map<?, ?> m) {
						LinkedHashMap<String, Object> row = new LinkedHashMap<>();
						for (Map.Entry<?, ?> e : m.entrySet()) {
							row.put(String.valueOf(e.getKey()), e.getValue());
						}
						avList.add(row);
					}
				}
			}
		}
		if (avList != null && avList.size() > 60) {
			throw new ResourceException("参数或规格值不能超过60个");
		}
		if ((avList == null || avList.isEmpty()) && Objects.equals(input.get("is_show"), "true")) {
			throw new ResourceException("请添加参数或者规格值");
		}

		final List<ItemsAttributeValues> oldListFinal = oldList;
		transactionTemplate.executeWithoutResult(status -> {
			try {
				if (fromOms) {
					prefillAttributeValueIdsFromOms(companyId, attributeId, input);
				} else {
					preUpdateAttrValues(companyId, attributeId, input, oldListFinal);
				}
				upsertAttributeValues(companyId, attributeId, info, input, countryCode, now);
			} catch (ResourceException | BadRequestException e) {
				throw e;
			} catch (Exception e) {
				throw new ResourceException(e.getMessage());
			}
		});
	}

	private void prefillAttributeValueIdsFromOms(long companyId, long attributeId, Map<String, Object> input) {
		if (!input.containsKey("attribute_values")) {
			return;
		}
		Object raw = input.get("attribute_values");
		if (!(raw instanceof List<?> list) || list.isEmpty()) {
			return;
		}
		List<Long> omsIds = new ArrayList<>();
		for (Object o : list) {
			if (o instanceof Map<?, ?> m) {
				Long id = parseLongOrNull(m.get("oms_value_id"));
				if (id != null && id != 0L) {
					omsIds.add(id);
				}
			}
		}
		if (omsIds.isEmpty()) {
			return;
		}
		List<ItemsAttributeValues> existing = itemsAttributeValuesRepository.listByCompanyAndAttributeAndOmsValueIdsIn(companyId, attributeId, omsIds);
		Map<Long, Long> omsToVid = new LinkedHashMap<>();
		for (ItemsAttributeValues row : existing) {
			if (row.getOmsValueId() != null && row.getAttributeValueId() != null) {
				omsToVid.putIfAbsent(row.getOmsValueId(), row.getAttributeValueId());
			}
		}
		for (Object o : list) {
			if (o instanceof Map<?, ?> m) {
				@SuppressWarnings("unchecked")
				Map<String, Object> row = (Map<String, Object>) m;
				Long omsKey = parseLongOrNull(row.get("oms_value_id"));
				if (omsKey != null && omsKey != 0L) {
					Long vid = omsToVid.get(omsKey);
					if (vid != null) {
						row.put("attribute_value_id", vid);
					}
				}
			}
		}
	}

	private void applyMainTablePatch(ItemsAttributes info, Map<String, Object> input, int now) {
		if (input.containsKey("attribute_type")) {
			info.setAttributeType(stringOrNull(input.get("attribute_type")));
		}
		if (input.containsKey("attribute_name")) {
			Object v = input.get("attribute_name");
			info.setAttributeName(v == null ? null : v.toString().trim());
		}
		if (input.containsKey("attribute_memo")) {
			Object v = input.get("attribute_memo");
			info.setAttributeMemo(v == null ? null : v.toString());
		}
		if (input.containsKey("attribute_sort")) {
			Object v = input.get("attribute_sort");
			info.setAttributeSort(v == null ? null : v.toString());
		}
		if (input.containsKey("is_show")) {
			Object v = input.get("is_show");
			info.setIsShow(v == null ? null : v.toString());
		}
		if (input.containsKey("image_url")) {
			Object v = input.get("image_url");
			info.setImageUrl(v == null ? null : v.toString());
		}
		if (input.containsKey("is_image")) {
			Object v = input.get("is_image");
			info.setIsImage(v == null ? null : v.toString());
		}
		info.setUpdated(now);
	}

	private void preUpdateAttrValues(long companyId, long attributeId, Map<String, Object> input, List<ItemsAttributeValues> oldList) {
		List<Long> oldIds = new ArrayList<>();
		for (ItemsAttributeValues row : oldList) {
			if (row.getAttributeValueId() != null) {
				oldIds.add(row.getAttributeValueId());
			}
		}
		Set<Long> newIds = new HashSet<>();
		if (input.containsKey("attribute_values")) {
			Object raw = input.get("attribute_values");
			if (raw instanceof List<?> list) {
				for (Object o : list) {
					if (o instanceof Map<?, ?> m) {
						Long id = parseLongOrNull(m.get("attribute_value_id"));
						if (id != null) {
							newIds.add(id);
						}
					}
				}
			}
		}
		List<Long> deleteIds = new ArrayList<>();
		for (Long id : oldIds) {
			if (!newIds.contains(id)) {
				deleteIds.add(id);
			}
		}
		if (deleteIds.isEmpty()) {
			return;
		}
		if (itemRelAttributesRepository.existsByCompanyAndAttributeAndValueIds(companyId, attributeId, deleteIds)) {
			throw new ResourceException("数值有关联商品，请先处理关联的商品");
		}
		itemsAttributeValuesRepository.deleteByAttributeValueIds(companyId, attributeId, deleteIds);
	}

	private void upsertAttributeValues(long companyId, long attributeId, ItemsAttributes info, Map<String, Object> input, String countryCode,
			int now) {
		if (!input.containsKey("attribute_values")) {
			return;
		}
		Object raw = input.get("attribute_values");
		if (!(raw instanceof List<?> list) || list.isEmpty()) {
			return;
		}
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> rows = (List<Map<String, Object>>) raw;
		Map<String, Boolean> seenTrimmed = new HashMap<>();
		for (int key = 0; key < rows.size(); key++) {
			Map<String, Object> value = rows.get(key);
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

			ItemsAttributeValues existing = itemsAttributeValuesRepository.selectByAttributeCompanyAndValue(attributeId, companyId, trimmed);
			Long rowId = parseLongOrNull(value.get("attribute_value_id"));
			if (existing != null && (rowId == null || !Objects.equals(rowId, existing.getAttributeValueId()))) {
				continue;
			}

			long shopId = info.getShopId() == null ? 0L : info.getShopId();
			String rowImageUrl = "";
			if (value.get("image_url") != null) {
				rowImageUrl = value.get("image_url").toString();
			}
			Object oms = value.get("oms_value_id");
			long omsVal = oms == null ? 0L : parseLongOrZero(oms);

			if (rowId != null) {
				ItemsAttributeValues ve = new ItemsAttributeValues();
				ve.setAttributeValueId(rowId);
				ve.setAttributeId(attributeId);
				ve.setCompanyId(companyId);
				ve.setShopId(shopId);
				ve.setAttributeValue(trimmed);
				ve.setSort(String.valueOf(key));
				ve.setImageUrl(rowImageUrl);
				ve.setUpdated(now);
				if (omsVal != 0L) {
					ve.setOmsValueId(omsVal);
				}
				itemsAttributeValuesRepository.updateById(ve);
				itemsAttributesMultiLangApplier.afterAttributeValueUpdate(companyId, rowId, trimmed, countryCode);
			} else {
				ItemsAttributeValues ve = new ItemsAttributeValues();
				ve.setAttributeId(attributeId);
				ve.setCompanyId(companyId);
				ve.setShopId(shopId);
				ve.setAttributeValue(trimmed);
				ve.setSort(String.valueOf(key));
				ve.setImageUrl(rowImageUrl);
				ve.setCreated(now);
				ve.setUpdated(now);
				if (omsVal != 0L) {
					ve.setOmsValueId(omsVal);
				}
				itemsAttributeValuesRepository.insert(ve);
				Long newVid = ve.getAttributeValueId();
				if (newVid == null) {
					throw new ResourceException("创建属性值失败");
				}
				itemsAttributesMultiLangApplier.afterAttributeValueInsert(companyId, newVid, trimmed, countryCode);
			}
		}
	}

	private static String stringOrNull(Object o) {
		return o == null ? null : o.toString();
	}

	private static Long parseLongOrNull(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
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
