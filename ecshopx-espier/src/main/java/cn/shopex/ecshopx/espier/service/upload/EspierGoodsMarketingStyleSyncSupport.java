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

package cn.shopex.ecshopx.espier.service.upload;

import cn.shopex.ecshopx.common.espier.upload.EspierMarketingStyleItemsLookupPort;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Parses marketing / purchase / discount style syncProcess Excel uploads and enriches rows from Items.
 */
public final class EspierGoodsMarketingStyleSyncSupport {

	private EspierGoodsMarketingStyleSyncSupport() {}

	public enum PriceMode {
		MARKETING,
		PURCHASE,
		DISCOUNT
	}

	public static Map<String, Object> parseSync(
			MultipartFile file,
			UploadHeaderTitle title,
			PriceMode priceMode,
			long companyId,
			long distributorId,
			EspierMarketingStyleItemsLookupPort itemsLookup)
			throws IOException {
		final int maxItemNums = 500;
		byte[] bytes = file.getBytes();
		List<List<Object>> rows = EspierUploadExcelSheetReader.readFirstSheet(bytes);
		if (rows.isEmpty()) {
			return Map.of("succ", List.of(), "invalid", List.of(), "fail", List.of());
		}
		List<String> headerCells = new ArrayList<>();
		for (Object o : rows.get(0)) {
			headerCells.add(o == null ? "" : String.valueOf(o));
		}
		Map<Integer, String> column = EspierUploadHeaderHandle.buildColumnMap(headerCells, title);
		List<List<Object>> dataRows = new ArrayList<>(rows.subList(1, rows.size()));
		if (dataRows.size() > maxItemNums) {
			throw new BadRequestException("每次最多上传" + maxItemNums + "个商品...请减少后再提交");
		}
		LinkedHashMap<String, Map<String, Object>> items = new LinkedHashMap<>();
		for (List<Object> row : dataRows) {
			if (!EspierUploadHeaderHandle.rowHasAnyValue(row)) {
				continue;
			}
			Map<String, Object> item = EspierUploadHeaderHandle.preRowHandle(column, row);
			Object bn = item.get("item_bn");
			if (bn == null || String.valueOf(bn).isBlank()) {
				continue;
			}
			String key = String.valueOf(bn).trim();
			item.put("item_bn", key);
			items.put(key, item);
		}

		if (!items.isEmpty() && itemsLookup != null) {
			Map<String, Map<String, Object>> dbByBn =
					switch (priceMode) {
						case MARKETING -> itemsLookup.lookupForMarketing(companyId, distributorId, items.keySet());
						case PURCHASE, DISCOUNT -> itemsLookup.lookupByItemBn(companyId, items.keySet());
					};
			if (dbByBn != null) {
				for (Map.Entry<String, Map<String, Object>> e : dbByBn.entrySet()) {
					Map<String, Object> row = items.get(e.getKey());
					Map<String, Object> db = e.getValue();
					if (row == null || db == null) {
						continue;
					}
					enrichFromDb(row, db, priceMode);
				}
			}
		}

		List<Map<String, Object>> failItems = new ArrayList<>();
		List<Map<String, Object>> succ = new ArrayList<>();
		for (Map.Entry<String, Map<String, Object>> e : items.entrySet()) {
			Map<String, Object> v = e.getValue();
			Object bn = v.get("item_bn");
			if (bn == null || String.valueOf(bn).trim().isEmpty()) {
				throw new BadRequestException("货号不能为空...请检查数据");
			}
			v.putIfAbsent("sort", 0);
			boolean hasItemId = v.containsKey("item_id");
			if (!hasItemId) {
				failItems.add(Map.of(
						"item_bn",
						String.valueOf(v.get("item_bn")),
						"item_name",
						String.valueOf(v.getOrDefault("item_name", ""))));
			} else {
				succ.add(v);
			}
		}
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("succ", succ);
		out.put("invalid", List.of());
		out.put("fail", failItems);
		return out;
	}

	private static void enrichFromDb(Map<String, Object> row, Map<String, Object> db, PriceMode priceMode) {
		Object itemId = db.get("item_id");
		if (itemId == null) {
			return;
		}
		String itemIdStr = String.valueOf(itemId);
		row.put("item_id", itemIdStr);
		row.put("itemId", itemIdStr);
		Object defaultItemId = db.get("default_item_id");
		row.put("default_item_id", defaultItemId == null ? "0" : String.valueOf(defaultItemId));
		row.put("pics", db.getOrDefault("pics", List.of()));
		Object marketPrice = db.get("market_price");
		row.put("market_price", marketPrice == null ? "0" : String.valueOf(marketPrice));
		Object dbName = db.get("item_name");
		if (dbName != null) {
			row.put("item_name", dbName);
			row.put("itemName", dbName);
		} else {
			row.putIfAbsent("itemName", row.get("item_name"));
		}
		row.put("item_type", db.getOrDefault("item_type", "services"));
		row.put("nospec", true);
		row.put("sort", row.get("sort") != null ? row.get("sort") : 0);
		Object activityStore = row.get("activity_store");
		if (activityStore != null && StringUtils.hasText(String.valueOf(activityStore).trim())) {
			row.put("store", activityStore);
		} else {
			row.put("store", db.get("store"));
		}
		switch (priceMode) {
			case MARKETING, PURCHASE -> row.put("price", activityPriceToCents(row.get("activity_price")));
			case DISCOUNT -> row.put("price", db.get("price"));
			default -> {}
		}
	}

	private static double activityPriceToCents(Object activityPrice) {
		if (activityPrice == null) {
			return 0d;
		}
		if (activityPrice instanceof Number n) {
			return n.doubleValue() * 100d;
		}
		String s = String.valueOf(activityPrice).trim();
		if (!StringUtils.hasText(s)) {
			return 0d;
		}
		try {
			return Double.parseDouble(s) * 100d;
		} catch (NumberFormatException ex) {
			return 0d;
		}
	}
}
