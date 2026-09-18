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

package cn.shopex.ecshopx.companys.service.merchant;

import cn.shopex.ecshopx.companys.mapper.MerchantOperatorListMapper;
import cn.shopex.ecshopx.merchant.port.MerchantOperatorListQueryPort;
import cn.shopex.ecshopx.merchant.service.MerchantOperatorListFilter;
import cn.shopex.ecshopx.merchant.service.MerchantOperatorListPage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CompanysMerchantOperatorListQueryService implements MerchantOperatorListQueryPort {

	private final MerchantOperatorListMapper merchantOperatorListMapper;

	public CompanysMerchantOperatorListQueryService(MerchantOperatorListMapper merchantOperatorListMapper) {
		this.merchantOperatorListMapper = merchantOperatorListMapper;
	}

	@Override
	public MerchantOperatorListPage queryPage(MerchantOperatorListFilter filter, int page, int pageSize) {
		String merchantNameLike = null;
		if (StringUtils.hasText(filter.merchantNameContainsOrNull())) {
			merchantNameLike = escapeSqlLike(filter.merchantNameContainsOrNull().trim());
		}
		long total = merchantOperatorListMapper.countByFilter(
				filter.companyId(),
				filter.merchantIdOrNull(),
				filter.onlyMerchantMain(),
				filter.operatorsMobileEncryptedOrNull(),
				merchantNameLike);
		if (total == 0) {
			return new MerchantOperatorListPage(0L, List.of());
		}
		int offset = (page - 1) * pageSize;
		List<Map<String, Object>> raw =
				merchantOperatorListMapper.selectPageByFilter(
						filter.companyId(),
						filter.merchantIdOrNull(),
						filter.onlyMerchantMain(),
						filter.operatorsMobileEncryptedOrNull(),
						merchantNameLike,
						pageSize,
						offset);
		List<Map<String, Object>> rows = new ArrayList<>();
		for (Map<String, Object> r : raw) {
			rows.add(toSnakeRow(r));
		}
		return new MerchantOperatorListPage(total, rows);
	}

	private static LinkedHashMap<String, Object> toSnakeRow(Map<String, Object> raw) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("operator_id", first(raw, "operator_id", "operatorId"));
		row.put("mobile", first(raw, "mobile"));
		row.put("password", first(raw, "password"));
		row.put("merchant_name", first(raw, "merchant_name", "merchantName"));
		row.put("settled_type", first(raw, "settled_type", "settledType"));
		row.put("is_merchant_main", normalizeMerchantMainFlag(first(raw, "is_merchant_main", "isMerchantMain")));
		row.put("merchant_table_id", first(raw, "merchant_table_id", "merchantTableId"));
		return row;
	}

	private static Object first(Map<String, Object> raw, String... keys) {
		for (String k : keys) {
			if (raw != null && raw.containsKey(k)) {
				return raw.get(k);
			}
		}
		return null;
	}

	private static Integer normalizeMerchantMainFlag(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Boolean b) {
			return b ? 1 : 0;
		}
		if (raw instanceof Number n) {
			return n.intValue() != 0 ? 1 : 0;
		}
		try {
			int v = Integer.parseInt(raw.toString().trim());
			return v != 0 ? 1 : 0;
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String escapeSqlLike(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "";
		}
		return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
}
