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

import cn.shopex.ecshopx.distribution.service.DistributorEasyListQueryService;
import cn.shopex.ecshopx.distribution.service.DistributorSelfMetaService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DiscountCardConsumeSoftFailDistributorListService {

	private final DistributorEasyListQueryService distributorEasyListQueryService;
	private final DistributorSelfMetaService distributorSelfMetaService;

	public DiscountCardConsumeSoftFailDistributorListService(
			DistributorEasyListQueryService distributorEasyListQueryService,
			DistributorSelfMetaService distributorSelfMetaService) {
		this.distributorEasyListQueryService = distributorEasyListQueryService;
		this.distributorSelfMetaService = distributorSelfMetaService;
	}

	public Map<String, Object> buildDistributorPayload(
			HttpServletRequest request, Map<String, Object> operatorJwt, Map<String, Object> cardInfo) {
		Object did = cardInfo.get("distributor_id");
		String rel = did == null ? "" : did.toString().trim();
		if (rel.startsWith(",")) {
			rel = rel.substring(1);
		}
		if (rel.endsWith(",")) {
			rel = rel.substring(0, rel.length() - 1);
		}
		rel = rel.trim();
		long companyId = longOf(operatorJwt.get("company_id"));
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("page", 1);
		merged.put("pageSize", 0);
		if (!StringUtils.hasText(rel)) {
			merged.put("is_all", true);
			Map<String, Object> data =
					distributorEasyListQueryService.buildEasyList(request, operatorJwt, merged);
			List<Map<String, Object>> list = new ArrayList<>();
			Object rawList = data.get("list");
			if (rawList instanceof List<?>) {
				for (Object o : (List<?>) rawList) {
					if (o instanceof Map<?, ?> mm) {
						Map<String, Object> row = new LinkedHashMap<>();
						for (Map.Entry<?, ?> e : mm.entrySet()) {
							if (e.getKey() != null) {
								row.put(e.getKey().toString(), e.getValue());
							}
						}
						list.add(row);
					}
				}
			}
			Map<String, Object> self = new LinkedHashMap<>(distributorSelfMetaService.getDistributorSelfSimpleInfo(companyId));
			self.put("is_center", true);
			list.add(0, self);
			int total = intOf(data.get("total_count")) + 1;
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("list", list);
			out.put("total_count", total);
			return out;
		}
		List<Long> ids = new ArrayList<>();
		for (String p : rel.split(",")) {
			String t = p.trim();
			if (t.isEmpty()) {
				continue;
			}
			try {
				ids.add(Long.parseLong(t));
			} catch (NumberFormatException ignored) {
				// skip invalid token
			}
		}
		merged.put("distributorIds", ids);
		return distributorEasyListQueryService.buildEasyList(request, operatorJwt, merged);
	}

	private static long longOf(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		if (v == null) {
			return 0L;
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int intOf(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		if (v == null) {
			return 0;
		}
		try {
			return Integer.parseInt(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
