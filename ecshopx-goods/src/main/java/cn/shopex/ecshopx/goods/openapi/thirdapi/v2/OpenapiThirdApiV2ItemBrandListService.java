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

import cn.shopex.ecshopx.common.openapi.OpenapiItemsV2FailException;
import cn.shopex.ecshopx.goods.domain.ItemsAttributes;
import cn.shopex.ecshopx.goods.repository.ItemsAttributesRepository;
import cn.shopex.ecshopx.goods.service.ItemsAttributesMultiLangApplier;
import cn.shopex.ecshopx.goods.service.ItemsAttributesRowMaps;
import cn.shopex.ecshopx.members.openapi.thirdapi.v2.OpenapiThirdApiV2MemberTagListService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2ItemBrandListService {

	private static final DateTimeFormatter DATETIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final ItemsAttributesRepository itemsAttributesRepository;
	private final ItemsAttributesMultiLangApplier itemsAttributesMultiLangApplier;

	public OpenapiThirdApiV2ItemBrandListService(
			ItemsAttributesRepository itemsAttributesRepository,
			ItemsAttributesMultiLangApplier itemsAttributesMultiLangApplier) {
		this.itemsAttributesRepository = itemsAttributesRepository;
		this.itemsAttributesMultiLangApplier = itemsAttributesMultiLangApplier;
	}

	public Map<String, Object> executeOpenapiGetItemBrandList(long companyId, int page, int pageSize) {
		IPage<ItemsAttributes> pageResult;
		try {
			pageResult = itemsAttributesRepository.selectPageByFilter(
					companyId,
					"brand",
					null,
					null,
					null,
					page,
					pageSize);
		} catch (DataAccessException ex) {
			throw new OpenapiItemsV2FailException("E5000", ex.getMostSpecificCause().getMessage());
		}

		long totalCount = pageResult.getTotal();
		List<ItemsAttributes> records = pageResult.getRecords();

		if (records.isEmpty()) {
			return OpenapiThirdApiV2MemberTagListService.formatListStruct(
					totalCount, List.of(), page, pageSize);
		}

		List<Map<String, Object>> rowMaps = new ArrayList<>();
		for (ItemsAttributes rec : records) {
			rowMaps.add(ItemsAttributesRowMaps.toAttributeRowMap(rec));
		}
		itemsAttributesMultiLangApplier.applyListLangForAttributes(companyId, "zh-CN", rowMaps);

		List<Map<String, Object>> outRows = new ArrayList<>();
		for (Map<String, Object> row : rowMaps) {
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("brand_id", row.get("attribute_id"));
			out.put("brand_name", row.get("attribute_name"));
			out.put("image_url", row.get("image_url"));
			out.put("created", formatEpochSeconds(row.get("created")));
			out.put("updated", formatEpochSeconds(row.get("updated")));
			outRows.add(out);
		}

		return OpenapiThirdApiV2MemberTagListService.formatListStruct(
				totalCount, outRows, page, pageSize);
	}

	private static String formatEpochSeconds(Object epochObj) {
		if (epochObj == null) {
			return null;
		}
		long epoch;
		if (epochObj instanceof Number n) {
			epoch = n.longValue();
		} else {
			try {
				epoch = Long.parseLong(epochObj.toString());
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return Instant.ofEpochSecond(epoch).atZone(ZoneId.systemDefault()).format(DATETIME_FMT);
	}
}
