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

import cn.shopex.ecshopx.common.dispatch.SalespersonTaskJobDispatchPublisher;
import cn.shopex.ecshopx.distribution.service.DistributorRepositoryGetInfoSimpleService;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SalespersonSubtaskPostService {

	private static final Logger log = LoggerFactory.getLogger(SalespersonSubtaskPostService.class);

	private final DistributorRepositoryGetInfoSimpleService distributorRepositoryGetInfoSimpleService;
	private final SalespersonTaskJobDispatchPublisher salespersonTaskJobDispatchPublisher;

	public Map<String, Object> postSubtask(long companyId, Map<String, Object> input, String unionId) {
		log.info("guide-api-callbacksubtask start {}", input);

		long subtaskId = parseLongDefault(input, "subtask_id");
		long distributorId = parseLongDefault(input, "distributor_id");
		String shopCode = stringOrEmpty(input, "shop_code");
		String employeeNumber = stringOrEmpty(input, "employee_number");
		long itemId = parseLongDefault(input, "item_id");

		if (subtaskId <= 0L
				|| !StringUtils.hasText(employeeNumber)
				|| (distributorId <= 0L && !StringUtils.hasText(shopCode))) {
			log.info("guide-api-callbacksubtask error 参数不正确");
			LinkedHashMap<String, Object> out = new LinkedHashMap<>();
			out.put("status", Boolean.TRUE);
			out.put("input", new LinkedHashMap<>(input));
			out.put("error", "参数不正确");
			return out;
		}

		if (!StringUtils.hasText(shopCode) && distributorId > 0L) {
			shopCode = distributorRepositoryGetInfoSimpleService
					.findShopCodeForValidDistributor(companyId, distributorId)
					.orElse("");
		}

		if (!StringUtils.hasText(shopCode)) {
			log.info("guide-api-callbacksubtask error 门店信息获取失败");
			LinkedHashMap<String, Object> out = new LinkedHashMap<>();
			out.put("status", Boolean.TRUE);
			out.put("input", new LinkedHashMap<>(input));
			out.put("error", "门店信息获取失败");
			return out;
		}

		salespersonTaskJobDispatchPublisher.publish(
				companyId, subtaskId, shopCode, employeeNumber, itemId, unionId);
		return Map.of("status", Boolean.TRUE);
	}

	private static long parseLongDefault(Map<String, Object> input, String key) {
		Object v = input.get(key);
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s) || "undefined".equals(s) || "null".equals(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String stringOrEmpty(Map<String, Object> input, String key) {
		Object v = input.get(key);
		if (v == null) {
			return "";
		}
		return v.toString().trim();
	}
}
