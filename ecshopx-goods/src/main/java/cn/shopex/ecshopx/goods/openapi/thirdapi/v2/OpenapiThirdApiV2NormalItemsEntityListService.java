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
import cn.shopex.ecshopx.goods.domain.ItemsCategory;
import cn.shopex.ecshopx.goods.domain.ItemsRelCats;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRelCatsRepository;
import cn.shopex.ecshopx.goods.service.ItemsListMultiLangApplier;
import cn.shopex.ecshopx.members.openapi.thirdapi.v2.OpenapiThirdApiV2MemberTagListService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2NormalItemsEntityListService {

	private static final DateTimeFormatter LOCAL_DATE_TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private final ItemsListQueryRepository itemsListQueryRepository;
	private final ItemsRelCatsRepository itemsRelCatsRepository;
	private final ItemsCategoryRepository itemsCategoryRepository;
	private final ItemsListMultiLangApplier itemsListMultiLangApplier;

	public OpenapiThirdApiV2NormalItemsEntityListService(
			ItemsListQueryRepository itemsListQueryRepository,
			ItemsRelCatsRepository itemsRelCatsRepository,
			ItemsCategoryRepository itemsCategoryRepository,
			ItemsListMultiLangApplier itemsListMultiLangApplier) {
		this.itemsListQueryRepository = itemsListQueryRepository;
		this.itemsRelCatsRepository = itemsRelCatsRepository;
		this.itemsCategoryRepository = itemsCategoryRepository;
		this.itemsListMultiLangApplier = itemsListMultiLangApplier;
	}

	public Map<String, Object> executeOpenapiGetEntityList(
			long companyId,
			int page,
			int pageSize,
			String approveStatusRaw,
			String brandIdRaw,
			String categoryIdRaw,
			String timeBeginRaw,
			String timeEndRaw,
			boolean isSelfPresent,
			String isSelfRaw) {
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
		filter.put("item_type", "normal");
		filter.put(ItemsListQueryRepository.KEY_IS_DEFAULT_EQ, 1);

		if (phpTruthy(approveStatusRaw)) {
			filter.put("approve_status", approveStatusRaw.trim());
			filter.put(
					ItemsListQueryRepository.KEY_EXISTS_INNER_ITEMS_APPROVE_STATUS_EQ,
					approveStatusRaw.trim());
		}
		if (phpTruthy(brandIdRaw)) {
			filter.put("brand_id", brandIdRaw.trim());
		}
		if (phpTruthy(categoryIdRaw)) {
			filter.put(
					ItemsListQueryRepository.KEY_LEGACY_OPENAPI_CATEGORY_ID_EQ,
					categoryIdRaw.trim());
		}
		if (timeBeginTruthy) {
			filter.put(ItemsListQueryRepository.KEY_UPDATED_GTE, toEpochSecondsInt(phpStrtotime(timeBeginRaw)));
		}
		if (timeEndTruthy) {
			filter.put(ItemsListQueryRepository.KEY_UPDATED_LTE, toEpochSecondsInt(phpStrtotime(timeEndRaw)));
		}
		if (isSelfPresent) {
			if ("true".equals(isSelfRaw)) {
				filter.put(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ, 0);
			} else {
				filter.put(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_GT0_ONLY, true);
			}
		}

		long totalCount;
		List<Items> rows;
		try {
			totalCount = itemsListQueryRepository.countByParams(filter);
			int offset = (page - 1) * pageSize;
			rows = totalCount > 0
					? itemsListQueryRepository.selectPageByParamsItemIdDesc(filter, offset, pageSize)
					: List.of();
		} catch (DataAccessException ex) {
			throw new OpenapiItemsV2FailException("E5000", ex.getMostSpecificCause().getMessage());
		}

		if (rows.isEmpty()) {
			return OpenapiThirdApiV2MemberTagListService.formatListStruct(totalCount, List.of(), page, pageSize);
		}

		List<Long> itemIds = rows.stream().map(Items::getItemId).toList();
		Map<Long, List<Long>> catIdsByItemId = buildCatIdsByItemId(companyId, itemIds);

		Set<Long> allCatIds = new LinkedHashSet<>();
		for (List<Long> catIds : catIdsByItemId.values()) {
			allCatIds.addAll(catIds);
		}
		Map<Long, String> nameByCatId = buildNameByCatId(companyId, allCatIds);

		List<Map<String, Object>> rowMaps = new ArrayList<>();
		for (Items item : rows) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("item_id", item.getItemId());
			row.put("item_bn", item.getItemBn());
			row.put("item_name", item.getItemName());
			row.put("price", item.getPrice());
			row.put("store", item.getStore());
			row.put("approve_status", item.getApproveStatus());
			row.put("nospec", toNospecBool(item.getNospec()));
			row.put("distributor_id", item.getDistributorId());
			row.put("updated", item.getUpdated());
			rowMaps.add(row);
		}

		itemsListMultiLangApplier.applyToRows(companyId, "zh-CN", rowMaps);

		List<Map<String, Object>> outRows = new ArrayList<>();
		for (int i = 0; i < rows.size(); i++) {
			Items item = rows.get(i);
			Map<String, Object> row = rowMaps.get(i);
			Integer distributorId = item.getDistributorId();

			Map<String, Object> out = new LinkedHashMap<>();
			out.put("item_id", row.get("item_id"));
			out.put("item_bn", row.get("item_bn"));
			out.put("item_name", row.get("item_name"));
			out.put("price", row.get("price"));
			out.put("store", row.get("store"));
			out.put("approve_status", row.get("approve_status"));
			out.put("nospec", row.get("nospec"));
			out.put("category_name", formatCategoryNames(catIdsByItemId.get(item.getItemId()), nameByCatId));
			out.put("is_self", distributorId == null || distributorId == 0);
			out.put("update_time", row.get("updated"));
			outRows.add(out);
		}

		return OpenapiThirdApiV2MemberTagListService.formatListStruct(totalCount, outRows, page, pageSize);
	}

	private Map<Long, List<Long>> buildCatIdsByItemId(long companyId, List<Long> itemIds) {
		Map<Long, List<Long>> catIdsByItemId = new LinkedHashMap<>();
		for (ItemsRelCats rel : itemsRelCatsRepository.listByCompanyIdAndItemIdIn(companyId, itemIds)) {
			catIdsByItemId.computeIfAbsent(rel.getItemId(), k -> new ArrayList<>()).add(rel.getCategoryId());
		}
		return catIdsByItemId;
	}

	private Map<Long, String> buildNameByCatId(long companyId, Set<Long> allCatIds) {
		Map<Long, String> nameByCatId = new LinkedHashMap<>();
		if (allCatIds.isEmpty()) {
			return nameByCatId;
		}
		for (ItemsCategory c : itemsCategoryRepository.listByCompanyAndCategoryIdIn(companyId, allCatIds)) {
			nameByCatId.put(c.getCategoryId(), c.getCategoryName());
		}
		return nameByCatId;
	}

	private static List<String> formatCategoryNames(List<Long> catIds, Map<Long, String> nameByCatId) {
		if (catIds == null || catIds.isEmpty()) {
			return List.of();
		}
		List<String> result = new ArrayList<>();
		for (Long catId : catIds) {
			String name = nameByCatId.get(catId);
			result.add("[" + (name != null ? name : "") + "]");
		}
		return result;
	}

	private static boolean toNospecBool(String nospec) {
		if (nospec == null) {
			return false;
		}
		String s = nospec.trim();
		return "true".equalsIgnoreCase(s) || "1".equals(s);
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
