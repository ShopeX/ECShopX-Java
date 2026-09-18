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

package cn.shopex.ecshopx.promotions.service;

import cn.shopex.ecshopx.common.util.LeadingNumberParser;
import cn.shopex.ecshopx.promotions.domain.PackageItemPromotions;
import cn.shopex.ecshopx.promotions.domain.PackageMainItemPromotions;
import cn.shopex.ecshopx.promotions.domain.PackagePromotions;
import cn.shopex.ecshopx.promotions.mapper.PackageItemPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.PackageMainItemPromotionsMapper;
import cn.shopex.ecshopx.promotions.mapper.PackagePromotionsMapper;
import cn.shopex.ecshopx.promotions.port.LimitPromotionAdminGoodsSupportPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PackagePromotionInfoService {

	private final PackagePromotionsMapper packagePromotionsMapper;
	private final PackageItemPromotionsMapper packageItemPromotionsMapper;
	private final PackageMainItemPromotionsMapper packageMainItemPromotionsMapper;
	private final LimitPromotionAdminGoodsSupportPort limitPromotionAdminGoodsSupportPort;

	public PackagePromotionInfoService(
			PackagePromotionsMapper packagePromotionsMapper,
			PackageItemPromotionsMapper packageItemPromotionsMapper,
			PackageMainItemPromotionsMapper packageMainItemPromotionsMapper,
			LimitPromotionAdminGoodsSupportPort limitPromotionAdminGoodsSupportPort) {
		this.packagePromotionsMapper = packagePromotionsMapper;
		this.packageItemPromotionsMapper = packageItemPromotionsMapper;
		this.packageMainItemPromotionsMapper = packageMainItemPromotionsMapper;
		this.limitPromotionAdminGoodsSupportPort = limitPromotionAdminGoodsSupportPort;
	}

	public Object info(long companyId, String packageIdRaw) {
		String raw = packageIdRaw == null ? "" : packageIdRaw.trim();
		if (!StringUtils.hasText(raw)) {
			return List.of();
		}
		long pid = LeadingNumberParser.parseAsLong(raw);
		if (pid <= 0L) {
			return List.of();
		}

		LambdaQueryWrapper<PackagePromotions> pw = new LambdaQueryWrapper<>();
		pw.eq(PackagePromotions::getPackageId, pid).eq(PackagePromotions::getCompanyId, companyId);
		PackagePromotions main = packagePromotionsMapper.selectOne(pw);
		if (main == null) {
			return List.of();
		}

		LambdaQueryWrapper<PackageItemPromotions> iw = new LambdaQueryWrapper<>();
		iw.eq(PackageItemPromotions::getPackageId, pid)
				.eq(PackageItemPromotions::getCompanyId, companyId)
				.orderByDesc(PackageItemPromotions::getCreated)
				.last("LIMIT 1000");
		List<PackageItemPromotions> relRows = packageItemPromotionsMapper.selectList(iw);

		List<Long> packageItems = new ArrayList<>();
		LinkedHashMap<String, Object> newPrice = new LinkedHashMap<>();
		for (PackageItemPromotions rel : relRows) {
			Long itemId = rel.getItemId();
			if (itemId != null) {
				packageItems.add(itemId);
				int priceVal = rel.getPackagePrice() == null ? 0 : rel.getPackagePrice().intValue();
				newPrice.put(String.valueOf(itemId), priceVal);
			}
		}

		List<Long> firstLoadIds = new ArrayList<>();
		for (PackageItemPromotions rel : relRows) {
			Long itemId = rel.getItemId();
			if (itemId != null) {
				firstLoadIds.add(itemId);
			}
		}
		Long mainItemIdForLoad = main.getMainItemId();
		if (mainItemIdForLoad != null && !firstLoadIds.contains(mainItemIdForLoad)) {
			firstLoadIds.add(mainItemIdForLoad);
		}

		List<Map<String, Object>> itemsList;
		if (firstLoadIds.isEmpty()) {
			itemsList = List.of();
		} else {
			Map<String, Object> skuPack =
					limitPromotionAdminGoodsSupportPort.loadSkuItemsListForPromotionDetail(
							companyId, firstLoadIds);
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> rawList = (List<Map<String, Object>>) skuPack.get("list");
			itemsList = rawList == null ? List.of() : rawList;
		}

		for (Map<String, Object> row : itemsList) {
			Long iid = toLong(row.get("item_id"));
			if (iid != null && newPrice.containsKey(String.valueOf(iid))) {
				row.put("new_price", newPrice.get(String.valueOf(iid)));
			}
		}

		Map<Long, Map<String, Object>> skuById = new LinkedHashMap<>();
		for (Map<String, Object> row : itemsList) {
			Long k = toLong(row.get("item_id"));
			if (k != null && !skuById.containsKey(k)) {
				skuById.put(k, row);
			}
		}

		List<Map<String, Object>> relItems = new ArrayList<>();
		for (PackageItemPromotions rel : relRows) {
			Long itemId = rel.getItemId();
			if (itemId != null && skuById.containsKey(itemId)) {
				relItems.add(new LinkedHashMap<>(skuById.get(itemId)));
			}
		}

		Long mainItemId = main.getMainItemId();
		List<Map<String, Object>> mainItemOut;
		if (mainItemId == null || !skuById.containsKey(mainItemId)) {
			mainItemOut = List.of();
		} else {
			Map<String, Object> mainRow = new LinkedHashMap<>(skuById.get(mainItemId));
			mainRow.put(
					"price",
					main.getMainItemPrice() == null ? 0 : main.getMainItemPrice().intValue());
			mainItemOut = List.of(mainRow);
		}

		List<Map<String, Object>> itemTreeLists;
		if (relItems.isEmpty()) {
			itemTreeLists = List.of();
		} else {
			itemTreeLists = limitPromotionAdminGoodsSupportPort.formatItemsList(relItems);
		}

		LambdaQueryWrapper<PackageMainItemPromotions> mw = new LambdaQueryWrapper<>();
		mw.eq(PackageMainItemPromotions::getPackageId, pid)
				.eq(PackageMainItemPromotions::getCompanyId, companyId)
				.orderByAsc(PackageMainItemPromotions::getMainItemId)
				.last("LIMIT 1000");
		List<PackageMainItemPromotions> mainRelRows = packageMainItemPromotionsMapper.selectList(mw);

		Map<Long, PackageMainItemPromotions> mainItemDataById = new LinkedHashMap<>();
		for (PackageMainItemPromotions r : mainRelRows) {
			Long mid = r.getMainItemId();
			if (mid != null) {
				mainItemDataById.put(mid, r);
			}
		}

		List<Long> mainSkuIds =
				mainRelRows.stream()
						.map(PackageMainItemPromotions::getMainItemId)
						.filter(Objects::nonNull)
						.distinct()
						.toList();

		List<Map<String, Object>> mainItemsList;
		if (mainSkuIds.isEmpty()) {
			mainItemsList = List.of();
		} else {
			Map<String, Object> mainSkuPack =
					limitPromotionAdminGoodsSupportPort.loadSkuItemsListForPromotionDetail(
							companyId, mainSkuIds);
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> rawMain =
					(List<Map<String, Object>>) mainSkuPack.get("list");
			mainItemsList = rawMain == null ? List.of() : rawMain;
		}

		List<Map<String, Object>> mainItemsOut = new ArrayList<>();
		for (Map<String, Object> row : mainItemsList) {
			Long itemId = toLong(row.get("item_id"));
			if (itemId == null) {
				continue;
			}
			PackageMainItemPromotions mid = mainItemDataById.get(itemId);
			if (mid == null) {
				continue;
			}
			LinkedHashMap<String, Object> line = new LinkedHashMap<>();
			line.put("item_id", itemId);
			line.put("item_title", row.get("item_name"));
			line.put(
					"item_price",
					mid.getMainItemPrice() == null ? 0 : mid.getMainItemPrice().intValue());
			Object t = row.get("item_type");
			line.put(
					"item_type",
					(t != null && !t.toString().isEmpty()) ? t.toString() : "services");
			line.put("item_spec_desc", Objects.toString(row.get("item_spec_desc"), ""));
			mainItemsOut.add(line);
		}

		LinkedHashMap<String, Object> detail = new LinkedHashMap<>();
		detail.put("package_id", main.getPackageId());
		detail.put("company_id", main.getCompanyId());
		detail.put("goods_id", main.getGoodsId());
		detail.put("main_item_id", main.getMainItemId());
		detail.put(
				"main_item_price",
				main.getMainItemPrice() == null ? 0 : main.getMainItemPrice().intValue());
		detail.put("package_name", main.getPackageName());
		detail.put("used_platform", main.getUsedPlatform());
		detail.put("free_postage", main.getFreePostage());
		detail.put("package_total_price", main.getPackageTotalPrice());
		detail.put("start_time", main.getStartTime());
		detail.put("end_time", main.getEndTime());
		detail.put("package_status", main.getPackageStatus());
		detail.put("reason", main.getReason());
		detail.put("created", main.getCreated());
		detail.put("updated", main.getUpdated());
		detail.put("source_type", main.getSourceType());
		detail.put("source_id", main.getSourceId() != null ? main.getSourceId() : 0L);
		detail.put("valid_grade", parseValidGradeList(main.getValidGrade()));
		detail.put("package_items", packageItems);
		detail.put("new_price", newPrice);
		detail.put("items", itemsList);
		detail.put("mainItem", mainItemOut);
		detail.put("itemTreeLists", itemTreeLists);
		detail.put("main_items", mainItemsOut);

		return detail;
	}

	private List<String> parseValidGradeList(String validGrade) {
		String vg = validGrade;
		if (vg == null || !StringUtils.hasText(vg.trim())) {
			return List.of();
		}
		return Arrays.stream(vg.split(","))
				.map(String::trim)
				.filter(StringUtils::hasText)
				.collect(Collectors.toList());
	}

	private static Long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		if (o == null) {
			return null;
		}
		try {
			return Long.parseLong(o.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
