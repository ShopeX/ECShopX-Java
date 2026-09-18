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

package cn.shopex.ecshopx.orders.service.financial.export;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class FinancialSalesreportItemIdConstraintResolver {

	private static final String SQL_BRAND_ITEM_IDS = """
			SELECT DISTINCT ra.item_id
			FROM items_rel_attributes ra
			INNER JOIN items_attributes a
				ON ra.attribute_id = a.attribute_id AND ra.company_id = a.company_id
			WHERE ra.company_id = :companyId
				AND ra.attribute_type = 'brand'
				AND a.attribute_type = 'brand'
				AND a.attribute_name = :brandName
			""";

	private static final String SQL_MAIN_CATEGORY_ITEM_IDS = """
			SELECT DISTINCT i.item_id
			FROM items i
			INNER JOIN items_category c
				ON c.company_id = i.company_id
				AND c.category_id = CAST(NULLIF(TRIM(i.item_category), '') AS UNSIGNED)
				AND c.is_main_category = 1
				AND c.category_name = :categoryName
			WHERE i.company_id = :companyId
			""";

	private final NamedParameterJdbcTemplate jdbc;

	public FinancialSalesreportItemIdConstraintResolver(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public FinancialSalesreportItemIdResolution resolve(long companyId, String brand, String mainCategory) {
		boolean hasBrand = StringUtils.hasText(brand);
		boolean hasCat = StringUtils.hasText(mainCategory);
		if (!hasBrand && !hasCat) {
			return new FinancialSalesreportItemIdResolution(FinancialSalesreportItemIdResolution.Kind.UNRESTRICTED, List.of());
		}

		List<Long> brandIds = null;
		if (hasBrand) {
			brandIds = queryItemIds(SQL_BRAND_ITEM_IDS, companyId, Map.of("brandName", brand.trim()));
			if (brandIds.isEmpty()) {
				return new FinancialSalesreportItemIdResolution(FinancialSalesreportItemIdResolution.Kind.NO_MATCH, List.of());
			}
		}

		List<Long> catIds = null;
		if (hasCat) {
			catIds = queryItemIds(SQL_MAIN_CATEGORY_ITEM_IDS, companyId, Map.of("categoryName", mainCategory.trim()));
			if (catIds.isEmpty()) {
				return new FinancialSalesreportItemIdResolution(FinancialSalesreportItemIdResolution.Kind.NO_MATCH, List.of());
			}
		}

		if (hasBrand && hasCat) {
			Set<Long> brandSet = new HashSet<>(brandIds);
			List<Long> inter = catIds.stream().filter(brandSet::contains).toList();
			if (inter.isEmpty()) {
				return new FinancialSalesreportItemIdResolution(FinancialSalesreportItemIdResolution.Kind.NO_MATCH, List.of());
			}
			return new FinancialSalesreportItemIdResolution(FinancialSalesreportItemIdResolution.Kind.IDS, inter);
		}
		if (hasBrand) {
			return new FinancialSalesreportItemIdResolution(FinancialSalesreportItemIdResolution.Kind.IDS, brandIds);
		}
		return new FinancialSalesreportItemIdResolution(FinancialSalesreportItemIdResolution.Kind.IDS, catIds);
	}

	private List<Long> queryItemIds(String sql, long companyId, Map<String, String> extra) {
		MapSqlParameterSource params = new MapSqlParameterSource("companyId", companyId);
		for (Map.Entry<String, String> en : extra.entrySet()) {
			params.addValue(en.getKey(), en.getValue());
		}
		return jdbc.query(
				sql,
				params,
				(rs, rowNum) -> rs.getLong("item_id"));
	}
}
