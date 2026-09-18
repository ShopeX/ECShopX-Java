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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.distribution.service.DistributorSalemanShopListService;
import cn.shopex.ecshopx.salesperson.service.support.ShopSalespersonApiFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SalemanShopListService {

	private static final Logger log = LoggerFactory.getLogger(SalemanShopListService.class);

	private final SalespersonListService salespersonListService;
	private final DistributorSalemanShopListService distributorSalemanShopListService;

	public SalemanShopListService(SalespersonListService salespersonListService,
			DistributorSalemanShopListService distributorSalemanShopListService) {
		this.salespersonListService = salespersonListService;
		this.distributorSalemanShopListService = distributorSalemanShopListService;
	}

	public Object salemanShopList(long companyId, long memberUserId, int requestPageIgnored, int pageSize,
			String mobileOpt, String nameOpt) {
		log.info(
				"salemanShopList start companyId={} memberUserId={} requestPageIgnored={} pageSize={} hasName={} hasMobile={}",
				companyId, memberUserId, requestPageIgnored, pageSize, nameOpt != null, mobileOpt != null);
		Map<String, Object> listdata =
				salespersonListService.listForSalemanShopList(companyId, (int) memberUserId, pageSize);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> spList = (List<Map<String, Object>>) listdata.get("list");
		if (spList == null) {
			spList = List.of();
		}
		log.info("salemanShopList salesperson size={} total_count={}", spList.size(), listdata.get("total_count"));

		for (Map<String, Object> row : spList) {
			Object isValid = row.get("is_valid");
			String s = String.valueOf(isValid).trim();
			if (Objects.equals(s, "true")) {
				row.put("is_valid", Boolean.TRUE);
			} else if (Objects.equals(s, "false")) {
				row.put("is_valid", Boolean.FALSE);
			}
		}

		List<Long> dIds = new ArrayList<>();
		for (Map<String, Object> row : spList) {
			Object v = row.get("shop_id");
			String shopIdStr = (v == null) ? null : (v instanceof String ? (String) v : String.valueOf(v));
			Long id = ShopSalespersonApiFields.shopIdAsLongOrNull(shopIdStr);
			if (id != null) {
				dIds.add(id);
			}
		}
		if (dIds.isEmpty()) {
			return Collections.emptyList();
		}

		LinkedHashMap<Long, Map<String, Object>> salespersonByShopId = new LinkedHashMap<>();
		for (Map<String, Object> row : spList) {
			Object v = row.get("shop_id");
			String shopIdStr = (v == null) ? null : (v instanceof String ? (String) v : String.valueOf(v));
			Long key = ShopSalespersonApiFields.shopIdAsLongOrNull(shopIdStr);
			if (key != null) {
				salespersonByShopId.put(key, row);
			}
		}

		Map<String, Object> listShop = distributorSalemanShopListService.lists(companyId, dIds, nameOpt, mobileOpt);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> shops = (List<Map<String, Object>>) listShop.get("list");
		if (shops == null) {
			return listShop;
		}
		for (Map<String, Object> vShop : shops) {
			vShop.put("user_id", memberUserId);
			Long distributorKey = distributorIdAsLong(vShop.get("distributor_id"));
			Object sp = distributorKey != null ? salespersonByShopId.get(distributorKey) : null;
			vShop.put("salesperson", sp != null ? sp : Collections.emptyList());
		}
		return listShop;
	}

	private static Long distributorIdAsLong(Object raw) {
		if (raw == null) {
			return null;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
