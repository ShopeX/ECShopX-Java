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

package cn.shopex.ecshopx.goods.openapi.thirdapi.v2;

import cn.shopex.ecshopx.common.openapi.OpenapiErrorCode;
import cn.shopex.ecshopx.common.openapi.OpenapiItemsV2FailException;
import cn.shopex.ecshopx.goods.domain.ItemRelAttributes;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemRelAttributesRepository;
import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import cn.shopex.ecshopx.goods.service.ItemsListMultiLangApplier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2ProductGoodsListService {

	private static final DateTimeFormatter LOCAL_DATE_TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private final ItemsListQueryRepository itemsListQueryRepository;
	private final ItemsListMultiLangApplier itemsListMultiLangApplier;
	private final ItemRelAttributesRepository itemRelAttributesRepository;
	private final ObjectMapper objectMapper;

	public OpenapiThirdApiV2ProductGoodsListService(
			ItemsListQueryRepository itemsListQueryRepository,
			ItemsListMultiLangApplier itemsListMultiLangApplier,
			ItemRelAttributesRepository itemRelAttributesRepository,
			ObjectMapper objectMapper) {
		this.itemsListQueryRepository = itemsListQueryRepository;
		this.itemsListMultiLangApplier = itemsListMultiLangApplier;
		this.itemRelAttributesRepository = itemRelAttributesRepository;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> executeOpenapiProductGoodsList(
			long companyId,
			int page,
			int pageSize,
			String timeBeginRaw,
			String timeEndRaw) {
		boolean timeBeginTruthy = phpTruthy(timeBeginRaw);
		boolean timeEndTruthy = phpTruthy(timeEndRaw);
		if (timeBeginTruthy && timeEndTruthy) {
			long beginEpoch = phpStrtotime(timeBeginRaw);
			long endEpoch = phpStrtotime(timeEndRaw);
			if (beginEpoch > endEpoch) {
				throw new OpenapiItemsV2FailException(
						OpenapiErrorCode.ORDER_AFTERSALES_HANDLE_ERROR, "开始时间不能大于结束时间");
			}
		}

		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put(ItemsListQueryRepository.KEY_COMPANY_ID, companyId);
		if (timeBeginTruthy) {
			filter.put(ItemsListQueryRepository.KEY_UPDATED_GTE, toEpochSecondsInt(phpStrtotime(timeBeginRaw)));
		}
		if (timeEndTruthy) {
			filter.put(ItemsListQueryRepository.KEY_UPDATED_LTE, toEpochSecondsInt(phpStrtotime(timeEndRaw)));
		}

		long totalCount = itemsListQueryRepository.countByParams(filter);
		List<Map<String, Object>> rowMaps = List.of();
		if (totalCount > 0) {
			List<Items> rows;
			if (pageSize > 0) {
				int offset = (page - 1) * pageSize;
				rows = itemsListQueryRepository.selectPageByParamsWithoutOrder(filter, offset, pageSize);
			} else {
				rows = itemsListQueryRepository.selectAllByParamsWithoutOrder(filter);
			}
			if (!rows.isEmpty()) {
				rowMaps = new ArrayList<>(rows.size());
				for (Items row : rows) {
					Map<String, Object> m = new LinkedHashMap<>();
					m.put("item_id", row.getItemId());
					m.put("item_bn", row.getItemBn());
					m.put("item_name", row.getItemName());
					m.put("price", row.getPrice());
					m.put("approve_status", row.getApproveStatus());
					m.put("pics", row.getPics());
					m.put("distributor_id", row.getDistributorId());
					rowMaps.add(m);
				}
				itemsListMultiLangApplier.applyToRows(companyId, "zh-CN", rowMaps);
			}
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("page", page);
		data.put("page_size", pageSize);
		data.put("total_count", totalCount);
		data.put("goods_list", new ArrayList<>());
		if (rowMaps.isEmpty()) {
			return data;
		}

		List<Long> itemIds = new ArrayList<>(rowMaps.size());
		for (Map<String, Object> row : rowMaps) {
			Object id = row.get("item_id");
			if (id instanceof Number n) {
				itemIds.add(n.longValue());
			}
		}

		List<ItemRelAttributes> relRows =
				itemRelAttributesRepository.listByCompanyAndItemIdsLimit100WithoutOrder(companyId, itemIds);
		Map<Long, List<String>> skuArr = new LinkedHashMap<>();
		for (ItemRelAttributes rel : relRows) {
			Long itemId = rel.getItemId();
			if (itemId == null) {
				continue;
			}
			String val = rel.getCustomAttributeValue();
			skuArr.computeIfAbsent(itemId, k -> new ArrayList<>()).add(val != null ? val : "");
		}

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> goodsList = (List<Map<String, Object>>) data.get("goods_list");
		for (Map<String, Object> row : rowMaps) {
			Long itemId = row.get("item_id") instanceof Number n ? n.longValue() : null;
			Object distributorId = row.get("distributor_id");
			boolean isSelf = distributorId == null
					|| (distributorId instanceof Number n && n.intValue() == 0);
			Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("item_name", str(row.get("item_name")));
			entry.put("item_price", intOrZero(row.get("price")));
			entry.put("pic", firstPicFromPicsJson((String) row.get("pics")));
			List<String> skuParts = itemId != null ? skuArr.get(itemId) : null;
			entry.put("sku", (skuParts == null || skuParts.isEmpty()) ? "" : String.join(" ", skuParts));
			entry.put("goods_bn", itemId != null ? itemId.intValue() : 0);
			entry.put("item_bn", str(row.get("item_bn")));
			entry.put("approve_status", str(row.get("approve_status")));
			entry.put("is_self", isSelf);
			goodsList.add(entry);
		}

		return data;
	}

	private String firstPicFromPicsJson(String picsJson) {
		if (!StringUtils.hasText(picsJson)) {
			return "";
		}
		try {
			JsonNode root = objectMapper.readTree(picsJson.trim());
			if (root.isArray() && root.size() > 0) {
				JsonNode first = root.get(0);
				if (first.isTextual()) {
					return first.asText("");
				}
			}
		} catch (Exception ignored) {
		}
		return "";
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private static int intOrZero(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		return 0;
	}

	private static boolean phpTruthy(String raw) {
		if (raw == null || raw.isEmpty()) {
			return false;
		}
		return !"0".equals(raw);
	}

	private static long phpStrtotime(String raw) {
		if (raw == null || raw.isBlank()) {
			return 0L;
		}
		String t = raw.trim();
		try {
			LocalDateTime dt = LocalDateTime.parse(t, LOCAL_DATE_TIME_FMT);
			return dt.atZone(ZoneId.systemDefault()).toEpochSecond();
		} catch (DateTimeParseException ignored) {
			// fall through
		}
		try {
			LocalDate d = LocalDate.parse(t, DATE_FMT);
			return d.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
		} catch (DateTimeParseException ignored) {
			return 0L;
		}
	}

	private static int toEpochSecondsInt(long epochSeconds) {
		return (int) epochSeconds;
	}
}
