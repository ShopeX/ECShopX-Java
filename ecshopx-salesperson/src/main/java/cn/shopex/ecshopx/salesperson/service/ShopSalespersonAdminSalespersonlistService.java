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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopSalespersonAdminSalespersonlistService {

	private final SalespersonListService salespersonListService;

	public Map<String, Object> salespersonlist(long companyId, Map<String, Object> mergedInput) {
		int pageSize = resolvePageSize(mergedInput.get("pageSize"));
		mergedInput.get("page");

		String mobile = null;
		Object mobileRaw = mergedInput.get("mobile");
		if (mobileRaw != null) {
			String t = String.valueOf(mobileRaw).trim();
			if (StringUtils.hasText(t)) {
				mobile = t;
			}
		}

		String username = null;
		Object usernameRaw = mergedInput.get("username");
		if (usernameRaw != null) {
			String t = String.valueOf(usernameRaw).trim();
			if (StringUtils.hasText(t)) {
				username = t;
			}
		}

		boolean distributorIdKeyPresent = mergedInput.containsKey("distributor_id");
		Object distributorIdRaw = mergedInput.get("distributor_id");

		Map<String, Object> listdata = salespersonListService.listForH5SalespersonAdmin(companyId,
				distributorIdKeyPresent, distributorIdRaw, mobile, username, pageSize);

		Object listObjForLog = listdata.get("list");
		int listSizeForLog = listObjForLog instanceof List<?> l ? l.size() : 0;
		log.info(
				":salesperson:salespersonlist companyId={} distributor_id={} mobileFilter={} usernameFilter={} pageSize={} listSize={}",
				companyId, distributorIdRaw, mobile != null, username != null, pageSize, listSizeForLog);

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) listdata.get("list");
		if (list == null) {
			list = List.of();
		}

		for (Map<String, Object> row : list) {
			Object iv = row.get("is_valid");
			if (iv == null) {
				continue;
			}
			String s = String.valueOf(iv);
			if ("true".equals(s)) {
				row.put("is_valid", Boolean.TRUE);
			} else if ("false".equals(s)) {
				row.put("is_valid", Boolean.FALSE);
			}
		}

		List<String> dIds = new ArrayList<>(list.size());
		for (Map<String, Object> row : list) {
			Object raw = row.get("shop_id");
			if (raw == null) {
				dIds.add(null);
			} else if (raw instanceof String str) {
				dIds.add(str.trim());
			} else if (raw instanceof Number n) {
				dIds.add(String.valueOf(n.longValue()));
			} else {
				dIds.add(String.valueOf(raw).trim());
			}
		}

		LinkedHashMap<String, Object> inputData = new LinkedHashMap<>(mergedInput);
		LinkedHashMap<String, Object> legacy = new LinkedHashMap<>();
		legacy.put("status", 1);
		legacy.put("code", 0);
		legacy.put("data", listdata);
		legacy.put("inputData", inputData);
		return legacy;
	}

	private static int resolvePageSize(Object raw) {
		if (raw == null) {
			return 100;
		}
		try {
			return Integer.parseInt(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 100;
		}
	}
}
