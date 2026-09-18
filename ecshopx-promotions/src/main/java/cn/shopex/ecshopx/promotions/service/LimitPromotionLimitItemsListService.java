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

import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.service.DistributorListQueryService;
import cn.shopex.ecshopx.promotions.domain.LimitItemPromotions;
import cn.shopex.ecshopx.promotions.mapper.LimitItemPromotionsMapper;
import cn.shopex.ecshopx.promotions.port.LimitPromotionAdminGoodsSupportPort;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LimitPromotionLimitItemsListService {

	private static final int ITEM_BN_FILTER_MAX_IDS = 100;

	private final LimitItemPromotionsMapper limitItemPromotionsMapper;
	private final DistributorListQueryService distributorListQueryService;
	private final LimitPromotionAdminGoodsSupportPort limitPromotionAdminGoodsSupportPort;
	private final MessageSource messageSource;

	public LimitPromotionLimitItemsListService(
			LimitItemPromotionsMapper limitItemPromotionsMapper,
			DistributorListQueryService distributorListQueryService,
			LimitPromotionAdminGoodsSupportPort limitPromotionAdminGoodsSupportPort,
			MessageSource messageSource) {
		this.limitItemPromotionsMapper = limitItemPromotionsMapper;
		this.distributorListQueryService = distributorListQueryService;
		this.limitPromotionAdminGoodsSupportPort = limitPromotionAdminGoodsSupportPort;
		this.messageSource = messageSource;
	}

	public Map<String, Object> getLimitItems(
			long companyId, long limitId, String itemBnRaw, int page, int pageSize, Locale locale) {
		String itemType = "normal";
		page = Math.max(1, page);
		pageSize = pageSize <= 0 ? 20 : pageSize;

		String bn = itemBnRaw == null ? "" : itemBnRaw.trim();
		List<Long> bnItemIds = null;
		if (StringUtils.hasText(bn)) {
			bnItemIds =
					limitPromotionAdminGoodsSupportPort.listItemIdsByCompanyAndItemBnContainsCapped(
							companyId, bn, ITEM_BN_FILTER_MAX_IDS);
			if (bnItemIds.isEmpty()) {
				Map<String, Object> empty = new LinkedHashMap<>();
				empty.put("total_count", 0L);
				empty.put("list", Collections.emptyList());
				return empty;
			}
		}

		LambdaQueryWrapper<LimitItemPromotions> base = new LambdaQueryWrapper<>();
		base.eq(LimitItemPromotions::getCompanyId, companyId)
				.eq(LimitItemPromotions::getLimitId, limitId)
				.eq(LimitItemPromotions::getItemType, itemType)
				.gt(LimitItemPromotions::getDistributorId, 0L);
		if (bnItemIds != null) {
			base.in(LimitItemPromotions::getItemId, bnItemIds);
		}
		base.select(
						LimitItemPromotions::getLimitId,
						LimitItemPromotions::getItemId,
						LimitItemPromotions::getLimitNum,
						LimitItemPromotions::getDistributorId)
				.orderByAsc(LimitItemPromotions::getItemId);

		Page<LimitItemPromotions> p = new Page<>(page, pageSize);
		limitItemPromotionsMapper.selectPage(p, base);
		long totalCount = p.getTotal();
		List<LimitItemPromotions> rows = p.getRecords();

		if (rows.isEmpty()) {
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("total_count", totalCount);
			out.put("list", Collections.emptyList());
			return out;
		}

		List<Long> distIds =
				rows.stream()
						.map(LimitItemPromotions::getDistributorId)
						.filter(Objects::nonNull)
						.distinct()
						.toList();
		List<Distributor> distRows = distributorListQueryService.listByIdsAndCompany(companyId, distIds);
		Map<Long, Distributor> distById = new LinkedHashMap<>();
		for (Distributor d : distRows) {
			if (d.getDistributorId() != null) {
				distById.putIfAbsent(d.getDistributorId(), d);
			}
		}

		List<Long> itemIds =
				rows.stream()
						.map(LimitItemPromotions::getItemId)
						.filter(Objects::nonNull)
						.distinct()
						.toList();
		List<Map<String, Object>> itemRows =
				limitPromotionAdminGoodsSupportPort.loadItemNameBnRowsByItemIds(companyId, itemIds);
		Map<Long, Map<String, Object>> itemById = new LinkedHashMap<>();
		for (Map<String, Object> row : itemRows) {
			Object idObj = row.get("item_id");
			if (idObj instanceof Number n) {
				itemById.putIfAbsent(n.longValue(), row);
			}
		}

		String unknownProductLabel =
				messageSource.getMessage("promotions.limit.unknown_product", null, locale);

		long noStart = totalCount - (long) (page - 1) * pageSize;
		List<Map<String, Object>> list = new ArrayList<>();
		for (int i = 0; i < rows.size(); i++) {
			LimitItemPromotions e = rows.get(i);
			long no = noStart - i;
			Map<String, Object> line = new LinkedHashMap<>();
			line.put("limit_id", e.getLimitId() == null ? 0L : e.getLimitId());
			line.put("item_id", e.getItemId() == null ? 0L : e.getItemId());
			line.put("limit_num", e.getLimitNum() == null ? 0L : e.getLimitNum());
			Long distributorId = e.getDistributorId();
			line.put("distributor_id", distributorId == null ? 0L : distributorId);
			line.put("no", no);

			Map<String, Object> itemRow = e.getItemId() == null ? null : itemById.get(e.getItemId());
			String itemName = "";
			if (itemRow != null) {
				Object nm = itemRow.get("item_name");
				if (nm != null && StringUtils.hasText(nm.toString())) {
					itemName = nm.toString();
				}
			}
			if (!StringUtils.hasText(itemName)) {
				itemName = unknownProductLabel;
			}
			line.put("item_name", itemName);

			String itemBnOut = "";
			if (itemRow != null) {
				Object ib = itemRow.get("item_bn");
				if (ib != null) {
					itemBnOut = ib.toString();
				}
			}
			line.put("item_bn", itemBnOut);

			String shopCode = "";
			String shopName = "";
			if (distributorId != null) {
				Distributor dist = distById.get(distributorId);
				if (dist != null) {
					if (dist.getShopCode() != null) {
						shopCode = dist.getShopCode();
					}
					if (dist.getName() != null) {
						shopName = dist.getName();
					}
				}
			}
			line.put("shop_code", shopCode);
			line.put("shop_name", shopName);
			list.add(line);
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", totalCount);
		result.put("list", list);
		return result;
	}
}
