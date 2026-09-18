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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.domain.DistributorSalesmanRole;
import cn.shopex.ecshopx.distribution.mapper.DistributorSalesmanRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalLong;
import org.springframework.stereotype.Service;

@Service
public class DistributorSalesmanRoleWriteService {

	private final DistributorSalesmanRoleMapper distributorSalesmanRoleMapper;
	private final ObjectMapper objectMapper;

	public DistributorSalesmanRoleWriteService(
			DistributorSalesmanRoleMapper distributorSalesmanRoleMapper, ObjectMapper objectMapper) {
		this.distributorSalesmanRoleMapper = distributorSalesmanRoleMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> createRole(long companyId, String roleName, JsonNode ruleIds) {
		DistributorSalesmanRole entity = new DistributorSalesmanRole();
		entity.setCompanyId(Math.toIntExact(companyId));
		entity.setRoleName(roleName);

		ArrayNode effective;
		if (ruleIds == null || !ruleIds.isArray()) {
			effective = objectMapper.createArrayNode();
		} else {
			effective = (ArrayNode) ruleIds;
		}

		try {
			entity.setRuleIds(objectMapper.writeValueAsString(effective));
		} catch (JsonProcessingException e) {
			throw new ResourceException("rule_ids 数据异常");
		}
		distributorSalesmanRoleMapper.insert(entity);

		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("salesman_role_id", entity.getSalesmanRoleId());
		data.put("company_id", entity.getCompanyId());
		data.put("role_name", entity.getRoleName());
		String stored = entity.getRuleIds();
		if (stored == null) {
			data.put("rule_ids", objectMapper.createArrayNode());
		} else {
			try {
				data.put("rule_ids", objectMapper.readTree(stored));
			} catch (JsonProcessingException e) {
				throw new ResourceException("rule_ids 数据异常");
			}
		}
		return data;
	}

	public Map<String, Object> updateRole(
			long companyId, long salesmanRoleId, String roleName, JsonNode ruleIds, String notFoundMessage) {
		DistributorSalesmanRole entity =
				distributorSalesmanRoleMapper.selectOne(
						new LambdaQueryWrapper<DistributorSalesmanRole>()
								.eq(DistributorSalesmanRole::getSalesmanRoleId, salesmanRoleId)
								.eq(DistributorSalesmanRole::getCompanyId, Math.toIntExact(companyId)));
		if (entity == null) {
			throw new ResourceException(notFoundMessage);
		}

		ArrayNode effective;
		if (ruleIds == null || !ruleIds.isArray()) {
			effective = objectMapper.createArrayNode();
		} else {
			effective = (ArrayNode) ruleIds;
		}

		entity.setCompanyId(Math.toIntExact(companyId));
		entity.setRoleName(roleName);
		try {
			entity.setRuleIds(objectMapper.writeValueAsString(effective));
		} catch (JsonProcessingException e) {
			throw new ResourceException("rule_ids 数据异常");
		}
		distributorSalesmanRoleMapper.updateById(entity);

		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("salesman_role_id", entity.getSalesmanRoleId());
		data.put("company_id", entity.getCompanyId());
		data.put("role_name", entity.getRoleName());
		String stored = entity.getRuleIds();
		if (stored == null) {
			data.put("rule_ids", objectMapper.createArrayNode());
		} else {
			try {
				data.put("rule_ids", objectMapper.readTree(stored));
			} catch (JsonProcessingException e) {
				throw new ResourceException("rule_ids 数据异常");
			}
		}
		return data;
	}

	public void delRole(long companyId, OptionalLong salesmanRoleId) {
		if (salesmanRoleId == null || salesmanRoleId.isEmpty()) {
			return;
		}
		distributorSalesmanRoleMapper.delete(
				new LambdaQueryWrapper<DistributorSalesmanRole>()
						.eq(DistributorSalesmanRole::getCompanyId, Math.toIntExact(companyId))
						.eq(DistributorSalesmanRole::getSalesmanRoleId, salesmanRoleId.getAsLong()));
	}
}
