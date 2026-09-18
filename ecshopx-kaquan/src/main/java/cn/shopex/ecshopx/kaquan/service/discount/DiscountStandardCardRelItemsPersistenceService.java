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

package cn.shopex.ecshopx.kaquan.service.discount;

import cn.shopex.ecshopx.kaquan.mapper.RelItemsMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DiscountStandardCardRelItemsPersistenceService {

	private static final int BATCH = 1000;

	private final RelItemsMapper relItemsMapper;

	public DiscountStandardCardRelItemsPersistenceService(RelItemsMapper relItemsMapper) {
		this.relItemsMapper = relItemsMapper;
	}

	@SuppressWarnings("unchecked")
	public void persist(Map<String, Object> dataInfo, long cardId, long companyId) {
		String useAllItems = DiscountCardParamNormalize.stringVal(dataInfo.get("use_all_items"));
		Object relRaw = dataInfo.get("rel_item_ids");
		if (relRaw instanceof List<?> relList && !relList.isEmpty()) {
			List<KaquanRelItemBatchRow> batch = new ArrayList<>();
			int n = relList.size();
			int k = 0;
			for (Object o : relList) {
				long itemId = DiscountCardParamNormalize.longFromObject(o, 0L);
				KaquanRelItemBatchRow row = new KaquanRelItemBatchRow();
				row.setItemId(itemId);
				row.setCardId(cardId);
				row.setCompanyId(companyId);
				row.setItemType(DiscountCardParamNormalize.stringVal(dataInfo.get("item_type")));
				if (!org.springframework.util.StringUtils.hasText(row.getItemType())) {
					row.setItemType("normal");
				}
				row.setIsShow((k == n - 1) ? 1 : 0);
				row.setUseLimit(0);
				batch.add(row);
				k++;
				if (batch.size() >= BATCH) {
					relItemsMapper.insertBatch(batch);
					batch.clear();
				}
			}
			if (!batch.isEmpty()) {
				relItemsMapper.insertBatch(batch);
			}
		} else if ("true".equals(useAllItems)) {
			KaquanRelItemBatchRow row = new KaquanRelItemBatchRow();
			row.setItemId(0L);
			row.setCardId(cardId);
			row.setCompanyId(companyId);
			row.setItemType(DiscountCardParamNormalize.stringVal(dataInfo.get("item_type")));
			if (!org.springframework.util.StringUtils.hasText(row.getItemType())) {
				row.setItemType("normal");
			}
			row.setIsShow(1);
			row.setUseLimit(0);
			relItemsMapper.insertBatch(List.of(row));
		}
		if ("category".equals(useAllItems)) {
			Object ic = dataInfo.get("item_category");
			if (ic instanceof List<?> cats) {
				saveRelItems(cats, "category", cardId, companyId);
			}
		}
		if ("tag".equals(useAllItems)) {
			List<String> ids = splitIds(DiscountCardParamNormalize.stringVal(dataInfo.get("tag_ids")));
			saveRelItems(ids.stream().map(s -> (Object) s).toList(), "tag", cardId, companyId);
		}
		if ("brand".equals(useAllItems)) {
			List<String> ids = splitIds(DiscountCardParamNormalize.stringVal(dataInfo.get("brand_ids")));
			saveRelItems(ids.stream().map(s -> (Object) s).toList(), "brand", cardId, companyId);
		}
	}

	private void saveRelItems(List<?> items, String itemType, long cardId, long companyId) {
		List<KaquanRelItemBatchRow> batch = new ArrayList<>();
		for (Object o : items) {
			long itemId = DiscountCardParamNormalize.longFromObject(o, 0L);
			KaquanRelItemBatchRow row = new KaquanRelItemBatchRow();
			row.setItemId(itemId);
			row.setCardId(cardId);
			row.setCompanyId(companyId);
			row.setItemType(itemType);
			row.setIsShow(1);
			row.setUseLimit(0);
			batch.add(row);
			if (batch.size() >= BATCH) {
				relItemsMapper.insertBatch(batch);
				batch.clear();
			}
		}
		if (!batch.isEmpty()) {
			relItemsMapper.insertBatch(batch);
		}
	}

	private static List<String> splitIds(String raw) {
		if (!org.springframework.util.StringUtils.hasText(raw)) {
			return List.of();
		}
		String t = raw.trim();
		if (t.startsWith(",")) {
			t = t.substring(1);
		}
		if (t.endsWith(",")) {
			t = t.substring(0, t.length() - 1);
		}
		List<String> out = new ArrayList<>();
		for (String p : t.split(",")) {
			if (org.springframework.util.StringUtils.hasText(p.trim())) {
				out.add(p.trim());
			}
		}
		return out;
	}
}
