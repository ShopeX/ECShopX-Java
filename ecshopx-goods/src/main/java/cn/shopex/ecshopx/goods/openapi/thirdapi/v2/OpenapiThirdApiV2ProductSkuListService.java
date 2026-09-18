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
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import cn.shopex.ecshopx.goods.service.ItemsListMultiLangApplier;
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
public class OpenapiThirdApiV2ProductSkuListService {

	private static final DateTimeFormatter LOCAL_DATE_TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private final ItemsListQueryRepository itemsListQueryRepository;
	private final ItemsListMultiLangApplier itemsListMultiLangApplier;

	public OpenapiThirdApiV2ProductSkuListService(
			ItemsListQueryRepository itemsListQueryRepository,
			ItemsListMultiLangApplier itemsListMultiLangApplier) {
		this.itemsListQueryRepository = itemsListQueryRepository;
		this.itemsListMultiLangApplier = itemsListMultiLangApplier;
	}

	public Map<String, Object> executeOpenapiProductSkuList(
			long companyId,
			int page,
			int pageSize,
			String timeBeginRaw,
			String timeEndRaw,
			String countryCode) {
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

		int normalizedPage = Math.max(page, 1);
		int normalizedPageSize = pageSize > 2000 ? 2000 : pageSize;
		if (pageSize <= 0) {
			normalizedPageSize = 100;
		}

		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put(ItemsListQueryRepository.KEY_COMPANY_ID, companyId);
		filter.put("item_type", "normal");
		if (timeBeginTruthy) {
			filter.put(ItemsListQueryRepository.KEY_UPDATED_GTE, toEpochSecondsInt(phpStrtotime(timeBeginRaw)));
		}
		if (timeEndTruthy) {
			filter.put(ItemsListQueryRepository.KEY_UPDATED_LTE, toEpochSecondsInt(phpStrtotime(timeEndRaw)));
		}

		long totalCount = itemsListQueryRepository.countByParams(filter);
		List<Map<String, Object>> rowMaps = List.of();
		if (totalCount > 0) {
			int offset = (normalizedPage - 1) * normalizedPageSize;
			List<Items> rows =
					itemsListQueryRepository.selectPageByParamsItemIdDesc(filter, offset, normalizedPageSize);
			if (!rows.isEmpty()) {
				rowMaps = new ArrayList<>(rows.size());
				for (Items row : rows) {
					Map<String, Object> m = new LinkedHashMap<>();
					m.put("item_id", row.getItemId());
					m.put("item_name", row.getItemName());
					m.put("item_bn", row.getItemBn());
					m.put("distributor_id", row.getDistributorId());
					rowMaps.add(m);
				}
				String lang = StringUtils.hasText(countryCode) ? countryCode.trim() : "zh-CN";
				itemsListMultiLangApplier.applyToRows(companyId, lang, rowMaps);
			}
		}

		List<Map<String, Object>> skuList = new ArrayList<>();
		if (!rowMaps.isEmpty()) {
			for (Map<String, Object> row : rowMaps) {
				Object distributorId = row.get("distributor_id");
				boolean isSelf = distributorId == null
						|| (distributorId instanceof Number n && n.intValue() == 0);
				Map<String, Object> sku = new LinkedHashMap<>();
				sku.put("name", row.get("item_name"));
				sku.put("sku_id", row.get("item_bn"));
				sku.put("is_self", isSelf);
				skuList.add(sku);
			}
		}

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("page", normalizedPage);
		data.put("page_size", normalizedPageSize);
		data.put("total_count", totalCount);
		data.put("sku_list", skuList);
		return data;
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
