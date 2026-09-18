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

package cn.shopex.ecshopx.salesperson.repository;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.salesperson.domain.SalesPromotions;
import cn.shopex.ecshopx.salesperson.mapper.SalesPromotionsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class SalesPromotionsRepository {

	private final SalesPromotionsMapper mapper;
	private final ObjectMapper objectMapper;

	public SalesPromotionsRepository(SalesPromotionsMapper mapper, ObjectMapper objectMapper) {
		this.mapper = mapper;
		this.objectMapper = objectMapper;
	}

	public Optional<SalesPromotions> getInfo(long companyId, long distributorId, long salespersonId, String uniqueKey) {
		LambdaQueryWrapper<SalesPromotions> w = new LambdaQueryWrapper<>();
		w.eq(SalesPromotions::getCompanyId, companyId)
				.eq(SalesPromotions::getDistributorId, distributorId)
				.eq(SalesPromotions::getSalespersonId, salespersonId)
				.eq(SalesPromotions::getUniqueKey, uniqueKey)
				.last("LIMIT 1");
		SalesPromotions row = mapper.selectOne(w);
		return Optional.ofNullable(row);
	}

	public Map<String, Object> create(
			long companyId, long distributorId, long salespersonId, String uniqueKey, String promotionItemsJson) {
		SalesPromotions e = new SalesPromotions();
		e.setCompanyId(companyId);
		e.setDistributorId(distributorId);
		e.setSalespersonId(salespersonId);
		e.setUniqueKey(uniqueKey);
		e.setPromotionItems(promotionItemsJson);
		mapper.insert(e);
		SalesPromotions loaded = mapper.selectById(e.getSalesPromotionId());
		if (loaded == null) {
			throw new ResourceException("未查询到更新数据");
		}
		return toColumnMap(loaded);
	}

	public Map<String, Object> updateOneBy(
			long companyId, long distributorId, long salespersonId, String uniqueKey, String promotionItemsJson) {
		LambdaQueryWrapper<SalesPromotions> w = new LambdaQueryWrapper<>();
		w.eq(SalesPromotions::getCompanyId, companyId)
				.eq(SalesPromotions::getDistributorId, distributorId)
				.eq(SalesPromotions::getSalespersonId, salespersonId)
				.eq(SalesPromotions::getUniqueKey, uniqueKey)
				.last("LIMIT 1");
		SalesPromotions e = mapper.selectOne(w);
		if (e == null) {
			throw new ResourceException("未查询到更新数据");
		}
		e.setPromotionItems(promotionItemsJson);
		mapper.updateById(e);
		SalesPromotions loaded = mapper.selectById(e.getSalesPromotionId());
		if (loaded == null) {
			throw new ResourceException("未查询到更新数据");
		}
		return toColumnMap(loaded);
	}

	private Map<String, Object> toColumnMap(SalesPromotions e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("sales_promotion_id", e.getSalesPromotionId());
		m.put("salesperson_id", e.getSalespersonId());
		m.put("unique_key", e.getUniqueKey());
		m.put("promotion_items", parsePromotionItemsJson(e.getPromotionItems()));
		m.put("company_id", e.getCompanyId());
		m.put("distributor_id", e.getDistributorId());
		return m;
	}

	private List<Map<String, Object>> parsePromotionItemsJson(String json) {
		if (json == null || json.isEmpty()) {
			return List.of();
		}
		try {
			return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
		} catch (JsonProcessingException ex) {
			throw new ResourceException("促销单数据解析失败");
		}
	}
}
