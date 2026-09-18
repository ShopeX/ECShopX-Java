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

package cn.shopex.ecshopx.datacube.service;

import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.distribution.mapper.SelfDeliveryStaffMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SelfDeliveryStaffDistributorIdsResolver {

	private static final String OPERATOR_TYPE_SELF_DELIVERY = "self_delivery_staff";

	private final OperatorsMapper operatorsMapper;
	private final SelfDeliveryStaffMapper selfDeliveryStaffMapper;
	private final ObjectMapper objectMapper;

	public SelfDeliveryStaffDistributorIdsResolver(
			OperatorsMapper operatorsMapper,
			SelfDeliveryStaffMapper selfDeliveryStaffMapper,
			ObjectMapper objectMapper) {
		this.operatorsMapper = operatorsMapper;
		this.selfDeliveryStaffMapper = selfDeliveryStaffMapper;
		this.objectMapper = objectMapper;
	}

	public List<Long> resolve(long companyId, List<Long> operatorIds) {
		LambdaQueryWrapper<Operators> q = new LambdaQueryWrapper<>();
		q.eq(Operators::getCompanyId, companyId);
		q.eq(Operators::getOperatorType, OPERATOR_TYPE_SELF_DELIVERY);
		if (operatorIds != null && !operatorIds.isEmpty()) {
			q.in(Operators::getOperatorId, operatorIds);
		}
		List<Operators> operators = operatorsMapper.selectList(q);
		if (operators.isEmpty()) {
			return List.of();
		}
		List<Long> fetchedOperatorIds = new ArrayList<>(operators.size());
		for (Operators op : operators) {
			if (op.getOperatorId() != null) {
				fetchedOperatorIds.add(op.getOperatorId());
			}
		}
		if (!fetchedOperatorIds.isEmpty()) {
			selfDeliveryStaffMapper.selectByOperatorIds(fetchedOperatorIds);
		}
		Set<Long> out = new LinkedHashSet<>();
		for (Operators op : operators) {
			collectFromDistributorIdsJson(op.getDistributorIds(), out);
		}
		return new ArrayList<>(out);
	}

	private void collectFromDistributorIdsJson(String json, Set<Long> out) {
		if (!StringUtils.hasText(json)) {
			return;
		}
		try {
			JsonNode arr = objectMapper.readTree(json);
			if (!arr.isArray()) {
				return;
			}
			for (JsonNode el : arr) {
				if (el == null || !el.isObject()) {
					continue;
				}
				JsonNode idNode = el.get("distributor_id");
				if (idNode == null || idNode.isNull()) {
					continue;
				}
				if (idNode.isNumber()) {
					long v = idNode.longValue();
					if (v > 0) {
						out.add(v);
					}
				} else if (idNode.isTextual()) {
					String t = idNode.asText();
					if (StringUtils.hasText(t)) {
						try {
							long v = Long.parseLong(t.trim());
							if (v > 0) {
								out.add(v);
							}
						} catch (NumberFormatException ignored) {
							// skip malformed entry
						}
					}
				}
			}
		} catch (Exception ignored) {
			// skip invalid JSON for this row
		}
	}
}
