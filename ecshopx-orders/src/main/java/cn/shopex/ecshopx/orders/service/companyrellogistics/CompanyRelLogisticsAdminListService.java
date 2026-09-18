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

package cn.shopex.ecshopx.orders.service.companyrellogistics;

import cn.shopex.ecshopx.companys.service.companylogistics.CompanyLogisticsListService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CompanyRelLogisticsAdminListService {

	private static final String OTHER_LOGO_URL =
			"https://b-img-cdn.yuanyuanke.cn/image/21/2021/05/21/f425f5ae2e6032eb6fded1015fc979e4FBZzXLlyYgHllKWlMHg0NyKqVBZDNkvM";

	private final CompanyLogisticsListService companyLogisticsListService;

	public CompanyRelLogisticsAdminListService(CompanyLogisticsListService companyLogisticsListService) {
		this.companyLogisticsListService = companyLogisticsListService;
	}

	public Map<String, Object> getCompanyLogisticsList(
			long companyId,
			String operatorType,
			Long operatorIdOrNull,
			int distributorId,
			String corpNameQueryOrNull,
			String statusQueryOrNull) {
		long supplierIdLong =
				Objects.equals(operatorType, "supplier") && operatorIdOrNull != null
						? operatorIdOrNull.longValue()
						: 0L;
		int supplierId = (int) supplierIdLong;

		String corpNameForService = null;
		if (corpNameQueryOrNull != null && !corpNameQueryOrNull.trim().isEmpty()) {
			corpNameForService = corpNameQueryOrNull.trim();
		}

		Map<String, Object> data =
				companyLogisticsListService.getCompanyLogisticsList(
						companyId, distributorId, supplierId, corpNameForService, statusQueryOrNull);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) data.get("list");
		long totalCount = ((Number) data.get("total_count")).longValue();

		List<Map<String, Object>> enabled = new ArrayList<>();
		List<Map<String, Object>> disabled = new ArrayList<>();
		for (Map<String, Object> row : list) {
			if (looseStringCompanyIdEquals(row.get("company_id"), companyId)) {
				row.put("is_enable", Boolean.TRUE);
				enabled.add(row);
			} else {
				disabled.add(row);
			}
		}

		Comparator<Map<String, Object>> byCorpNameKey =
				Comparator.comparing(
						r ->
								CorpNameFirstCharSortKey.sortKeyForMultisort(
										Objects.toString(r.get("corp_name"), "")),
						Comparator.nullsFirst(String::compareTo));
		enabled.sort(byCorpNameKey);
		disabled.sort(byCorpNameKey);

		List<Map<String, Object>> merged = new ArrayList<>(enabled.size() + disabled.size());
		merged.addAll(enabled);
		merged.addAll(disabled);

		Map<String, Object> other = new LinkedHashMap<>();
		other.put("company_id", Integer.valueOf((int) companyId));
		other.put("corp_code", "OTHER");
		other.put("corp_id", "0");
		other.put("corp_name", "其他");
		other.put("id", "0");
		other.put("is_enable", Boolean.TRUE);
		other.put("kuaidi_code", "");
		other.put("phone", "");
		other.put("logo", OTHER_LOGO_URL);

		if (corpNameForService == null
				&& !companyLogisticsListService.normalizedStatusCodeIfFilterActive(statusQueryOrNull).isPresent()) {
			merged = prependOther(merged, other);
			totalCount++;
		}
		Optional<Integer> st = companyLogisticsListService.normalizedStatusCodeIfFilterActive(statusQueryOrNull);
		if (st.isPresent() && st.get().equals(Integer.valueOf(1))) {
			merged = prependOther(merged, other);
			totalCount++;
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		out.put("list", merged);
		return out;
	}

	private static List<Map<String, Object>> prependOther(
			List<Map<String, Object>> merged, Map<String, Object> other) {
		List<Map<String, Object>> next = new ArrayList<>(merged.size() + 1);
		next.add(other);
		next.addAll(merged);
		return next;
	}

	private static boolean looseStringCompanyIdEquals(Object rowCompanyId, long companyId) {
		if (rowCompanyId == null) {
			return false;
		}
		return String.valueOf(rowCompanyId)
				.trim()
				.equals(String.valueOf(companyId).trim());
	}
}
