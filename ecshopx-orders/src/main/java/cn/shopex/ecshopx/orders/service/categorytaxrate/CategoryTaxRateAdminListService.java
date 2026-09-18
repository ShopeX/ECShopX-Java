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

package cn.shopex.ecshopx.orders.service.categorytaxrate;

import cn.shopex.ecshopx.orders.repository.CategoryTaxRateRepository;
import cn.shopex.ecshopx.orders.repository.InvoiceSellerRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CategoryTaxRateAdminListService {

	private static final Logger log = LoggerFactory.getLogger(CategoryTaxRateAdminListService.class);

	private final CategoryTaxRateRepository categoryTaxRateRepository;
	private final InvoiceSellerRepository invoiceSellerRepository;

	public CategoryTaxRateAdminListService(CategoryTaxRateRepository categoryTaxRateRepository,
			InvoiceSellerRepository invoiceSellerRepository) {
		this.categoryTaxRateRepository = categoryTaxRateRepository;
		this.invoiceSellerRepository = invoiceSellerRepository;
	}

	public Map<String, Object> getTaxRateList(
			long companyId,
			Optional<String> salesPartyIdLike,
			Optional<String> taxRateTypeEq,
			int page,
			int pageSize) {
		log.info("category_tax_rate list filter companyId={} salesPartyIdLike={} taxRateTypeEq={} page={} pageSize={}",
				companyId, salesPartyIdLike.orElse(null), taxRateTypeEq.orElse(null), page, pageSize);

		List<Map<String, Object>> list = categoryTaxRateRepository.selectTaxRateListPage(companyId,
				salesPartyIdLike, taxRateTypeEq, page, pageSize);
		int totalCount = list.size();
		log.info("getTaxRateList:result:listSize={}", list.size());

		if (list.isEmpty()) {
			Map<String, Object> result = new LinkedHashMap<>();
			result.put("list", list);
			result.put("total_count", totalCount);
			return result;
		}

		Set<Long> idSet = new LinkedHashSet<>();
		for (Map<String, Object> row : list) {
			Object spObj = row.get("sales_party_id");
			if (spObj == null) {
				continue;
			}
			String s = String.valueOf(spObj).trim();
			if (s.isEmpty()) {
				continue;
			}
			try {
				idSet.add(Long.parseLong(s));
			} catch (NumberFormatException ignored) {
			}
		}
		List<Long> ids = new ArrayList<>(idSet);
		log.info("getTaxRateList:salesPartyIds:{}", ids);

		Map<String, Map<String, Object>> byIdKey = new LinkedHashMap<>();
		if (!ids.isEmpty()) {
			List<Map<String, Object>> sellerRows = invoiceSellerRepository
					.selectSellerRowsByIdsOrderByIdDescLimit20(ids);
			for (Map<String, Object> sellerRow : sellerRows) {
				Object idObj = sellerRow.get("id");
				if (idObj == null) {
					continue;
				}
				byIdKey.put(String.valueOf(idObj), sellerRow);
			}
		}
		for (Map<String, Object> row : list) {
			Object spObj = row.get("sales_party_id");
			String lookupKey = spObj == null ? "" : String.valueOf(spObj).trim();
			Map<String, Object> party = byIdKey.get(lookupKey);
			row.put("sales_party_info", party != null ? party : Collections.emptyList());
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("list", list);
		result.put("total_count", totalCount);
		return result;
	}
}
