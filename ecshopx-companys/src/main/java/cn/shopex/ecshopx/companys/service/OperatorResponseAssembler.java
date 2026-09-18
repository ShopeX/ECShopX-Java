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

package cn.shopex.ecshopx.companys.service;

import cn.shopex.ecshopx.companys.domain.Operators;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OperatorResponseAssembler {

	private final ObjectMapper objectMapper;

	public OperatorResponseAssembler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> toStatusMap(Operators op) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("operator_id", op.getOperatorId());
		m.put("company_id", op.getCompanyId());
		m.put("mobile", op.getMobile());
		m.put("login_name", op.getLoginName());
		m.put("username", op.getUsername());
		m.put("head_portrait", op.getHeadPortrait());
		m.put("password", op.getPassword());
		m.put("operator_type", op.getOperatorType());
		m.put("eid", op.getEid());
		m.put("passport_uid", op.getPassportUid());
		m.put("shopex_bind_account", op.getShopexBindAccount());
		m.put("distributor_ids", toApiIdList(op.getDistributorIds()));
		m.put("shop_ids", toApiIdList(op.getShopIds()));
		m.put("regionauth_id", op.getRegionauthId());
		m.put("contact", op.getContact());
		m.put("split_ledger_info", op.getSplitLedgerInfo());
		m.put("adapay_open_account_time", op.getAdapayOpenAccountTime());
		m.put("merchant_id", op.getMerchantId());
		m.put("dealer_parent_id", op.getDealerParentId());
		m.put("is_dealer_main", boolToInt(op.getIsDealerMain()));
		m.put("is_distributor_main", boolToInt(op.getIsDistributorMain()));
		m.put("is_merchant_main", boolToInt(op.getIsMerchantMain()));
		m.put("is_disable", op.getIsDisable() != null && op.getIsDisable() ? 1 : 0);
		m.put("created", op.getCreated());
		m.put("updated", op.getUpdated());
		return m;
	}

	private Object toApiIdList(Object value) {
		if (value == null) {
			return Collections.emptyList();
		}
		if (value instanceof List<?> list) {
			return list;
		}
		if (value instanceof String s) {
			if (s.isBlank()) {
				return Collections.emptyList();
			}
			try {
				JsonNode node = objectMapper.readTree(s);
				if (!node.isArray()) {
					return Collections.emptyList();
				}
				return objectMapper.convertValue(node, new TypeReference<List<Object>>() {});
			} catch (JsonProcessingException e) {
				return Collections.emptyList();
			}
		}
		return Collections.emptyList();
	}

	private static int boolToInt(Boolean b) {
		return Boolean.TRUE.equals(b) ? 1 : 0;
	}
}
