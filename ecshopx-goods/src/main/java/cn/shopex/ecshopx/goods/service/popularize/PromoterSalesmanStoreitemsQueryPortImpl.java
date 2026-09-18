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

package cn.shopex.ecshopx.goods.service.popularize;

import cn.shopex.ecshopx.common.popularize.PromoterSalesmanStoreitemsQueryPort;
import cn.shopex.ecshopx.distribution.service.DistributorH5ListShopByIdsService;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsListQueryRepository;
import cn.shopex.ecshopx.goods.service.ItemsListMultiLangApplier;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsListRowMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PromoterSalesmanStoreitemsQueryPortImpl implements PromoterSalesmanStoreitemsQueryPort {

	private static final String SHOP_IDS_SQL =
			"SELECT shop_id FROM shop_salesperson WHERE company_id = :companyId AND user_id = :userId LIMIT :pageSize";

	private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private final ItemsListQueryRepository itemsListQueryRepository;
	private final ItemsListMultiLangApplier itemsListMultiLangApplier;
	private final DistributorH5ListShopByIdsService distributorH5ListShopByIdsService;

	public PromoterSalesmanStoreitemsQueryPortImpl(
			NamedParameterJdbcTemplate namedParameterJdbcTemplate,
			ItemsListQueryRepository itemsListQueryRepository,
			ItemsListMultiLangApplier itemsListMultiLangApplier,
			DistributorH5ListShopByIdsService distributorH5ListShopByIdsService) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
		this.itemsListQueryRepository = itemsListQueryRepository;
		this.itemsListMultiLangApplier = itemsListMultiLangApplier;
		this.distributorH5ListShopByIdsService = distributorH5ListShopByIdsService;
	}

	@Override
	public Object getSalesmanStoreitems(
			long companyId,
			long userId,
			int page,
			int pageSize,
			Object isDefaultRaw,
			String distributorIdRaw,
			String requestLangOrCountryCode) {
		int effectivePageSize = pageSize <= 0 ? 1 : pageSize;
		List<Long> dIds = loadShopIdsPageOneBugCompat(companyId, userId, effectivePageSize);
		if (dIds.isEmpty()) {
			return Collections.emptyList();
		}

		int parsedIsDefault = parseParsedIsDefaultDefaultOne(isDefaultRaw);
		boolean isDefaultEffective = parsedIsDefault != 0;

		LinkedHashMap<String, Object> p = new LinkedHashMap<>();
		p.put(ItemsListQueryRepository.KEY_COMPANY_ID, companyId);
		p.put("audit_status", "approved");
		p.put("rebate", 1);

		if (isDefaultEffective) {
			p.put(ItemsListQueryRepository.KEY_EXISTS_INNER_ITEMS_APPROVE_STATUS_IN, List.of("onsale", "only_show"));
			p.put(ItemsListQueryRepository.KEY_IS_DEFAULT_EQ, parsedIsDefault);
		} else {
			p.put("approve_status", List.of("onsale", "only_show"));
			p.put(ItemsListQueryRepository.KEY_IS_DEFAULT_EQ, parsedIsDefault);
		}

		applyDistributorScope(p, dIds, distributorIdRaw);

		int offset = (page - 1) * pageSize;
		int limit = pageSize;
		long total = itemsListQueryRepository.countByParams(p);
		List<Items> itemRows = itemsListQueryRepository.selectPageByParamsItemIdDesc(p, offset, limit);
		List<Map<String, Object>> list = new ArrayList<>();
		for (Items it : itemRows) {
			list.add(GoodsItemsListRowMapper.toRow(it));
		}
		itemsListMultiLangApplier.applyToRows(companyId, requestLangOrCountryCode, list);

		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", total);
		result.put("list", list);
		Map<String, Object> listShopMap =
				distributorH5ListShopByIdsService.listShopByDistributorIds(companyId, dIds, requestLangOrCountryCode);
		result.put("listShop", listShopMap);
		return result;
	}

	private void applyDistributorScope(LinkedHashMap<String, Object> p, List<Long> dIds, String distributorIdRaw) {
		if (StringUtils.hasText(distributorIdRaw)) {
			String t = distributorIdRaw.trim();
			try {
				long v = Long.parseLong(t);
				if (v > 0L && v <= Integer.MAX_VALUE && dIds.contains(v)) {
					p.put(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_EQ, (int) v);
					return;
				}
			} catch (NumberFormatException ignored) {
				// fall through to IN list
			}
		}
		List<Integer> in = new ArrayList<>();
		for (Long id : dIds) {
			if (id != null && id > 0L && id <= Integer.MAX_VALUE) {
				in.add(id.intValue());
			}
		}
		p.put(ItemsListQueryRepository.KEY_DISTRIBUTOR_ID_IN, in);
	}

	private static int parseParsedIsDefaultDefaultOne(Object raw) {
		if (raw == null) {
			return 1;
		}
		String s = String.valueOf(raw).trim();
		if (!StringUtils.hasText(s)) {
			return 1;
		}
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 1;
		}
	}

	private List<Long> loadShopIdsPageOneBugCompat(long companyId, long userId, int pageSize) {
		int lim = pageSize <= 0 ? 1 : pageSize;
		MapSqlParameterSource src =
				new MapSqlParameterSource()
						.addValue("companyId", companyId)
						.addValue("userId", userId)
						.addValue("pageSize", lim);
		List<Long> out = new ArrayList<>();
		List<Map<String, Object>> rows = namedParameterJdbcTemplate.queryForList(SHOP_IDS_SQL, src);
		for (Map<String, Object> row : rows) {
			Object sid = row.get("shop_id");
			if (sid instanceof Number n) {
				out.add(n.longValue());
			} else if (sid != null && StringUtils.hasText(sid.toString())) {
				try {
					out.add(Long.parseLong(sid.toString().trim()));
				} catch (NumberFormatException ignored) {
					// skip malformed row
				}
			}
		}
		return out;
	}
}
