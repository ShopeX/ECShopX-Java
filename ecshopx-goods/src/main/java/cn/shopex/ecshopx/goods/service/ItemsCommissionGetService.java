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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.ItemsCommission;
import cn.shopex.ecshopx.goods.repository.ItemsCommissionQueryRepository;
import cn.shopex.ecshopx.goods.service.items.ItemDetailScalarHelper;
import cn.shopex.ecshopx.goods.service.items.ItemsDetailSpecDisplayService;
import cn.shopex.ecshopx.goods.service.items.PlatformItemsDetailCoreService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ItemsCommissionGetService {

	private final PlatformItemsDetailCoreService platformItemsDetailCoreService;
	private final ItemsDetailSpecDisplayService itemsDetailSpecDisplayService;
	private final ItemsListMultiLangApplier itemsListMultiLangApplier;
	private final ItemDetailScalarHelper itemDetailScalarHelper;
	private final ItemsCommissionQueryRepository itemsCommissionQueryRepository;
	private final ObjectMapper objectMapper;

	public ItemsCommissionGetService(PlatformItemsDetailCoreService platformItemsDetailCoreService,
			ItemsDetailSpecDisplayService itemsDetailSpecDisplayService,
			ItemsListMultiLangApplier itemsListMultiLangApplier,
			ItemDetailScalarHelper itemDetailScalarHelper,
			ItemsCommissionQueryRepository itemsCommissionQueryRepository,
			ObjectMapper objectMapper) {
		this.platformItemsDetailCoreService = platformItemsDetailCoreService;
		this.itemsDetailSpecDisplayService = itemsDetailSpecDisplayService;
		this.itemsListMultiLangApplier = itemsListMultiLangApplier;
		this.itemDetailScalarHelper = itemDetailScalarHelper;
		this.itemsCommissionQueryRepository = itemsCommissionQueryRepository;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getItemsCommission(long companyId, long itemId, String authorizerAppId,
			String countryCode) {
		Map<String, Object> detail = platformItemsDetailCoreService.build(companyId, itemId, authorizerAppId);
		if (detail == null || detail.isEmpty() || detail.get("item_id") == null) {
			throw new ResourceException("商品获取失败");
		}

		itemsDetailSpecDisplayService.apply(detail);
		itemsListMultiLangApplier.applyToRows(companyId, countryCode, List.of(detail));

		long goodsId = parseRequiredGoodsId(detail);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("item_id", String.valueOf(itemId));
		result.put("goods_id", String.valueOf(goodsId));
		result.put("commission_type", "1");
		result.put("commission", "");
		List<Map<String, Object>> skuCommission = new ArrayList<>();
		result.put("sku_commission", skuCommission);

		boolean multiSpec = itemDetailScalarHelper.isMultiSpec(detail.get("nospec"));
		List<Long> itemIds = new ArrayList<>();

		if (!multiSpec) {
			Map<String, Object> skuRow = new LinkedHashMap<>();
			skuRow.put("item_id", itemId);
			skuRow.put("item_spec_desc", "单规格");
			skuRow.put("price", detail.get("price"));
			skuRow.put("cost_price", detail.get("cost_price"));
			skuRow.put("commission", "");
			skuCommission.add(skuRow);
			parseItemIdToList(itemId, itemIds);
		} else {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> specItems = coerceSpecItemsList(detail.get("spec_items"));
			for (Map<String, Object> specRow : specItems) {
				Map<String, Object> skuRow = new LinkedHashMap<>();
				Object specItemId = specRow.get("item_id");
				skuRow.put("item_id", specItemId);
				Object csn = specRow.get("custom_spec_name");
				skuRow.put("item_spec_desc", csn != null ? csn.toString() : "");
				skuRow.put("price", specRow.get("price"));
				skuRow.put("cost_price", specRow.get("cost_price"));
				skuRow.put("commission", "");
				skuCommission.add(skuRow);
				parseItemIdToList(specItemId, itemIds);
			}
		}

		Optional<ItemsCommission> spuOpt =
				itemsCommissionQueryRepository.findByCompanyRelIdAndType(companyId, goodsId, "goods");
		if (spuOpt.isEmpty()) {
			return result;
		}

		ItemsCommission spu = spuOpt.get();
		result.put("commission_type", spu.getCommissionType() != null ? spu.getCommissionType() : "1");
		String spuCommissionRaw = commissionDisplayStringFromConf(spu.getCommissionConf());
		if (Objects.equals("2", String.valueOf(result.get("commission_type")))) {
			result.put("commission", scaleAmountType2(spuCommissionRaw));
		} else {
			result.put("commission", spuCommissionRaw);
		}

		if (itemIds.isEmpty()) {
			return result;
		}

		List<ItemsCommission> skuRows =
				itemsCommissionQueryRepository.listByCompanyRelIdsAndType(companyId, itemIds, "item");
		if (skuRows.isEmpty()) {
			return result;
		}

		Map<Long, ItemsCommission> byRelId = skuRows.stream()
				.filter(r -> r.getRelId() != null)
				.collect(Collectors.toMap(ItemsCommission::getRelId, r -> r, (a, b) -> a));

		applySkuCommissionsToLines(result, byRelId);
		return result;
	}

	private void applySkuCommissionsToLines(Map<String, Object> result, Map<Long, ItemsCommission> byRelId) {
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> skuCommission = (List<Map<String, Object>>) result.get("sku_commission");
		String commissionType = String.valueOf(result.get("commission_type"));
		boolean type2 = Objects.equals("2", commissionType);

		for (Map<String, Object> line : skuCommission) {
			long rid = parseItemIdKey(line.get("item_id"));
			if (rid < 1) {
				continue;
			}
			ItemsCommission row = byRelId.get(rid);
			if (row == null) {
				continue;
			}
			String display = commissionDisplayStringFromConf(row.getCommissionConf());
			if (type2) {
				line.put("commission", scaleAmountType2(display));
			} else {
				line.put("commission", display);
			}
		}
	}

	private static void parseItemIdToList(Object itemIdObj, List<Long> itemIds) {
		long id = parseItemIdKey(itemIdObj);
		if (id > 0) {
			itemIds.add(id);
		}
	}

	private static long parseItemIdKey(Object itemIdObj) {
		if (itemIdObj == null) {
			return -1L;
		}
		if (itemIdObj instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(itemIdObj.toString().trim());
		} catch (NumberFormatException e) {
			return -1L;
		}
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> coerceSpecItemsList(Object specItemsObj) {
		if (!(specItemsObj instanceof List<?> rawList)) {
			return List.of();
		}
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object o : rawList) {
			if (o instanceof Map<?, ?> m) {
				out.add((Map<String, Object>) m);
			}
		}
		return out;
	}

	private static long parseRequiredGoodsId(Map<String, Object> detail) {
		Object g = detail.get("goods_id");
		if (g == null) {
			throw new ResourceException("商品获取失败");
		}
		try {
			if (g instanceof Number n) {
				return n.longValue();
			}
			return Long.parseLong(g.toString().trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("商品获取失败");
		}
	}

	private String commissionDisplayStringFromConf(String commissionConfJson) {
		if (!StringUtils.hasText(commissionConfJson)) {
			return "";
		}
		try {
			return objectMapper.readTree(commissionConfJson).path("commission").asText("");
		} catch (Exception e) {
			return "";
		}
	}

	private static String scaleAmountType2(String raw) {
		if (raw == null || raw.isEmpty()) {
			return "";
		}
		try {
			BigDecimal bd = new BigDecimal(raw.trim());
			return bd.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).toPlainString();
		} catch (NumberFormatException e) {
			return raw;
		}
	}
}
