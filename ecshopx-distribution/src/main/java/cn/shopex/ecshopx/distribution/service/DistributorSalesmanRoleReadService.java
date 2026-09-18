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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DistributorSalesmanRoleReadService {

	private final DistributorSalesmanRoleMapper distributorSalesmanRoleMapper;
	private final ObjectMapper objectMapper;

	public DistributorSalesmanRoleReadService(
			DistributorSalesmanRoleMapper distributorSalesmanRoleMapper, ObjectMapper objectMapper) {
		this.distributorSalesmanRoleMapper = distributorSalesmanRoleMapper;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> getRoleList(long companyId, int page, int pageSize) {
		int cid = Math.toIntExact(companyId);
		LambdaQueryWrapper<DistributorSalesmanRole> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(DistributorSalesmanRole::getCompanyId, cid);
		wrapper.orderByDesc(DistributorSalesmanRole::getSalesmanRoleId);

		Page<DistributorSalesmanRole> p = new Page<>(page, pageSize);
		distributorSalesmanRoleMapper.selectPage(p, wrapper);

		List<Map<String, Object>> list = new ArrayList<>();
		for (DistributorSalesmanRole entity : p.getRecords()) {
			LinkedHashMap<String, Object> data = new LinkedHashMap<>();
			data.put("salesman_role_id", entity.getSalesmanRoleId());
			data.put("company_id", entity.getCompanyId());
			data.put("role_name", entity.getRoleName());
			data.put("rule_ids", formatRuleIdsForResponse(entity.getRuleIds()));
			list.add(data);
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", (int) p.getTotal());
		out.put("list", list);
		return out;
	}

	public Optional<Map<String, Object>> getRoleInfo(long companyId, long salesmanRoleId) {
		int cid = Math.toIntExact(companyId);
		LambdaQueryWrapper<DistributorSalesmanRole> w = new LambdaQueryWrapper<>();
		w.eq(DistributorSalesmanRole::getCompanyId, cid);
		w.eq(DistributorSalesmanRole::getSalesmanRoleId, salesmanRoleId);
		DistributorSalesmanRole entity = distributorSalesmanRoleMapper.selectOne(w);
		if (entity == null) {
			return Optional.empty();
		}
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("salesman_role_id", entity.getSalesmanRoleId());
		data.put("company_id", entity.getCompanyId());
		data.put("role_name", entity.getRoleName());
		data.put("rule_ids", formatRuleIdsForDetailResponse(entity.getRuleIds()));
		return Optional.of(data);
	}

	private String formatRuleIdsForResponse(String stored) {
		if (stored == null || !StringUtils.hasText(stored)) {
			return "[]";
		}
		try {
			JsonNode node = objectMapper.readTree(stored);
			return objectMapper.writeValueAsString(node);
		} catch (JsonProcessingException e) {
			throw new ResourceException("rule_ids 数据异常");
		}
	}

	/**
	 * Detail response mirrors JSON column storage: any JSON array shape is returned as-is (including
	 * non-numeric tokens); list API still uses {@link #formatRuleIdsForResponse} string form.
	 */
	private JsonNode formatRuleIdsForDetailResponse(String stored) {
		if (stored == null || !StringUtils.hasText(stored)) {
			return objectMapper.createArrayNode();
		}
		try {
			JsonNode node = objectMapper.readTree(stored);
			if (!node.isArray()) {
				throw new ResourceException("rule_ids 数据异常");
			}
			return node;
		} catch (JsonProcessingException e) {
			throw new ResourceException("rule_ids 数据异常");
		}
	}
}
