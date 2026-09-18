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

package cn.shopex.ecshopx.companys.openapi.thirdapi.v1;

import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV1JurisdictionGetuserService {

	private final OperatorsQueryService operatorsQueryService;

	private final ObjectMapper objectMapper;

	public OpenapiThirdApiV1JurisdictionGetuserService(
			OperatorsQueryService operatorsQueryService, ObjectMapper objectMapper) {
		this.operatorsQueryService = operatorsQueryService;
		this.objectMapper = objectMapper;
	}

	public Optional<Map<String, Object>> findStaffOperator(String mobile, String type) {
		if (!"staff".equals(type)) {
			return Optional.empty();
		}
		Map<String, Object> filter = new LinkedHashMap<>();
		filter.put("mobile", mobile);
		filter.put("operator_type", "staff");

		Map<String, Object> row = operatorsQueryService.getInfo(filter);
		if (row == null || row.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(toPhpOperatorData(row));
	}

	private Map<String, Object> toPhpOperatorData(Map<String, Object> row) {
		LinkedHashMap<String, Object> result = new LinkedHashMap<>(row);
		coalesceSnakeCaseFromCamelCaseAliases(result);
		decodeJsonColumnsIntoOperatorMap(result);
		ensureOperatorInfoNullPlaceholders(result);
		return result;
	}

	private static void coalesceSnakeCaseFromCamelCaseAliases(LinkedHashMap<String, Object> m) {
		coalesceKey(m, "operator_id", "operatorId");
		coalesceKey(m, "company_id", "companyId");
		coalesceKey(m, "login_name", "loginName");
		coalesceKey(m, "operator_type", "operatorType");
		coalesceKey(m, "merchant_id", "merchantId");
		coalesceKey(m, "regionauth_id", "regionauthId");
		coalesceKey(m, "username", "userName");
		coalesceKey(m, "head_portrait", "headPortrait");
		coalesceKey(m, "passport_uid", "passportUid");
		coalesceKey(m, "split_ledger_info", "splitLedgerInfo");
		coalesceKey(m, "adapay_open_account_time", "adapayOpenAccountTime");
		coalesceKey(m, "dealer_parent_id", "dealerParentId");
		coalesceKey(m, "distributor_ids", "distributorIds");
		coalesceKey(m, "shop_ids", "shopIds");
		coalesceKey(m, "is_disable", "isDisable");
		coalesceKey(m, "is_dealer_main", "isDealerMain");
		coalesceKey(m, "is_merchant_main", "isMerchantMain");
		coalesceKey(m, "is_distributor_main", "isDistributorMain");
	}

	private static void coalesceKey(LinkedHashMap<String, Object> m, String snake, String camel) {
		if (m.containsKey(snake)) {
			m.remove(camel);
			return;
		}
		if (m.containsKey(camel)) {
			m.put(snake, m.remove(camel));
		}
	}

	private void decodeJsonColumnsIntoOperatorMap(LinkedHashMap<String, Object> opMap) {
		Object shopRaw = opMap.get("shop_ids");
		if (shopRaw instanceof String s && StringUtils.hasText(s)) {
			opMap.put("shop_ids", decodeShopIdsValue(s));
		}
		Object distRaw = opMap.get("distributor_ids");
		if (distRaw instanceof String s && StringUtils.hasText(s)) {
			opMap.put("distributor_ids", decodeDistributorIdsJson(s));
		}
	}

	private Object decodeShopIdsValue(String raw) {
		try {
			Object parsed = objectMapper.readValue(raw, Object.class);
			if (parsed instanceof List<?> || parsed instanceof Map<?, ?>) {
				return parsed;
			}
			return new ArrayList<>();
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	private List<Map<String, Object>> decodeDistributorIdsJson(String raw) {
		try {
			List<Object> parsed = objectMapper.readValue(raw, new TypeReference<List<Object>>() {});
			if (parsed == null) {
				return new ArrayList<>();
			}
			List<Map<String, Object>> out = new ArrayList<>();
			for (Object o : parsed) {
				if (o instanceof Map<?, ?> m) {
					LinkedHashMap<String, Object> row = new LinkedHashMap<>();
					for (Map.Entry<?, ?> e : m.entrySet()) {
						if (e.getKey() instanceof String k) {
							row.put(k, e.getValue());
						}
					}
					out.add(row);
				}
			}
			return out;
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	private static void ensureOperatorInfoNullPlaceholders(LinkedHashMap<String, Object> m) {
		putNullIfAbsent(m, "login_name");
		putNullIfAbsent(m, "password");
		putNullIfAbsent(m, "eid");
		putNullIfAbsent(m, "passport_uid");
		putNullIfAbsent(m, "username");
		putNullIfAbsent(m, "head_portrait");
		putNullIfAbsent(m, "split_ledger_info");
		putNullIfAbsent(m, "contact");
		putNullIfAbsent(m, "adapay_open_account_time");
		putNullIfAbsent(m, "dealer_parent_id");
		putNullIfAbsent(m, "is_dealer_main");
		putNullIfAbsent(m, "updated");
		putNullIfAbsent(m, "merchant_id");
		putNullIfAbsent(m, "is_merchant_main");
		putNullIfAbsent(m, "is_distributor_main");
		if (!m.containsKey("shop_ids")) {
			m.put("shop_ids", null);
		}
		if (!m.containsKey("distributor_ids")) {
			m.put("distributor_ids", null);
		}
	}

	private static void putNullIfAbsent(LinkedHashMap<String, Object> m, String key) {
		if (!m.containsKey(key)) {
			m.put(key, null);
		}
	}
}
