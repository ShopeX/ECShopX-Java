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

package cn.shopex.ecshopx.theme.service;

import cn.shopex.ecshopx.kaquan.domain.CardPackage;
import cn.shopex.ecshopx.kaquan.service.cardpackage.CardPackageListExistsByIdsService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PagesTemplateDecoratorFilterService {

	private final CardPackageListExistsByIdsService cardPackageListExistsByIdsService;

	public PagesTemplateDecoratorFilterService(CardPackageListExistsByIdsService cardPackageListExistsByIdsService) {
		this.cardPackageListExistsByIdsService = cardPackageListExistsByIdsService;
	}

	public Map<String, Object> applyTemplateFilter(long companyId, Map<String, Object> row) {
		if (row == null) {
			return new LinkedHashMap<>();
		}
		String name = String.valueOf(row.get("name"));
		if ("coupon".equals(name)) {
			List<Object> emptyList = List.of();
			row.put("data", row.get("data") != null ? row.get("data") : emptyList);
			Object vpRaw = row.get("voucher_package");
			List<Map<String, Object>> voucherPackage = new ArrayList<>();
			if (vpRaw instanceof List<?> l) {
				for (Object o : l) {
					if (o instanceof Map<?, ?> m) {
						@SuppressWarnings("unchecked")
						Map<String, Object> vm = (Map<String, Object>) (Map<?, ?>) m;
						voucherPackage.add(vm);
					}
				}
			}
			row.put("voucher_package", voucherPackage);
			if (voucherPackage.isEmpty()) {
				return row;
			}
			boolean usePackageIdKey = false;
			Map<String, Object> first = voucherPackage.get(0);
			if (first != null && first.containsKey("package_id")) {
				usePackageIdKey = true;
			}
			List<Long> idList = new ArrayList<>();
			for (Map<String, Object> el : voucherPackage) {
				Object idObj = usePackageIdKey ? el.get("package_id") : el.get("id");
				long id = longOrZero(idObj);
				if (id > 0L) {
					idList.add(id);
				}
			}
			List<CardPackage> pkgs = cardPackageListExistsByIdsService.getListByIdList(companyId, idList);
			Set<Long> existing =
					pkgs.stream()
							.map(CardPackage::getPackageId)
							.filter(Objects::nonNull)
							.collect(Collectors.toSet());
			List<Map<String, Object>> filtered = new ArrayList<>();
			for (Map<String, Object> el : voucherPackage) {
				long pid = longOrZero(usePackageIdKey ? el.get("package_id") : el.get("id"));
				if (pid > 0L && existing.contains(pid)) {
					filtered.add(el);
				}
			}
			row.put("voucher_package", new ArrayList<>(filtered));
		}
		return row;
	}

	private static long longOrZero(Object v) {
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
}
