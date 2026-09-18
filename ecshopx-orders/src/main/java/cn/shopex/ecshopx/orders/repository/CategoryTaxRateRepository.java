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

package cn.shopex.ecshopx.orders.repository;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.CategoryTaxRate;
import cn.shopex.ecshopx.orders.mapper.CategoryTaxRateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class CategoryTaxRateRepository {

	private final CategoryTaxRateMapper mapper;

	public CategoryTaxRateRepository(CategoryTaxRateMapper mapper) {
		this.mapper = mapper;
	}

	public Optional<CategoryTaxRate> findDuplicateForCreate(long companyId, String salesPartyId, String taxRateType,
			String categoryIdsJsonUtf8) {
		LambdaQueryWrapper<CategoryTaxRate> w = new LambdaQueryWrapper<>();
		w.eq(CategoryTaxRate::getCompanyId, companyId)
				.eq(CategoryTaxRate::getSalesPartyId, salesPartyId)
				.eq(CategoryTaxRate::getTaxRateType, taxRateType);
		if ("SPECIFIED".equals(taxRateType)) {
			w.eq(CategoryTaxRate::getCategoryIds, categoryIdsJsonUtf8);
		}
		return Optional.ofNullable(mapper.selectOne(w.last("LIMIT 1")));
	}

	public Map<String, Object> insertEntity(CategoryTaxRate entity) {
		mapper.insert(entity);
		return toRowMap(entity);
	}

	public Optional<CategoryTaxRate> findFirstByTaxRateTypeAll() {
		LambdaQueryWrapper<CategoryTaxRate> w = new LambdaQueryWrapper<>();
		w.eq(CategoryTaxRate::getTaxRateType, "ALL");
		return Optional.ofNullable(mapper.selectOne(w.last("LIMIT 1")));
	}

	public Optional<Map<String, Object>> getInfoById(long id) {
		CategoryTaxRate entity = mapper.selectById(id);
		if (entity == null) {
			return Optional.empty();
		}
		return Optional.of(toRowMap(entity));
	}

	public void deleteById(long id) {
		mapper.deleteById(id);
	}

	public Map<String, Object> updateByIdPartialReturningRow(long id, Map<String, Object> data) {
		CategoryTaxRate existing = mapper.selectById(id);
		if (existing == null) {
			throw new ResourceException("未查询到更新数据");
		}
		if (data.containsKey("company_id")) {
			Object v = data.get("company_id");
			if (v == null) {
				existing.setCompanyId(null);
			} else if (v instanceof Number) {
				existing.setCompanyId(((Number) v).longValue());
			} else {
				try {
					existing.setCompanyId(Long.parseLong(String.valueOf(v).trim()));
				} catch (NumberFormatException e) {
					throw new ResourceException("更新失败");
				}
			}
		}
		if (data.containsKey("sales_party_id")) {
			Object v = data.get("sales_party_id");
			existing.setSalesPartyId(v == null ? null : String.valueOf(v));
		}
		if (data.containsKey("tax_rate_type")) {
			Object v = data.get("tax_rate_type");
			existing.setTaxRateType(v == null ? null : String.valueOf(v));
		}
		if (data.containsKey("category_ids")) {
			Object v = data.get("category_ids");
			existing.setCategoryIds(v == null ? null : String.valueOf(v));
		}
		if (data.containsKey("invoice_tax_rate")) {
			Object v = data.get("invoice_tax_rate");
			existing.setInvoiceTaxRate(v == null ? null : String.valueOf(v));
		}
		existing.setUpdatedAt((int) (System.currentTimeMillis() / 1000L));
		mapper.updateById(existing);
		CategoryTaxRate latest = mapper.selectById(id);
		if (latest == null) {
			throw new ResourceException("未查询到更新数据");
		}
		return toRowMap(latest);
	}

	public List<Map<String, Object>> selectTaxRateListPage(
			long companyId,
			Optional<String> salesPartyIdContains,
			Optional<String> taxRateTypeEquals,
			int page,
			int pageSize) {
		LambdaQueryWrapper<CategoryTaxRate> w = new LambdaQueryWrapper<>();
		w.eq(CategoryTaxRate::getCompanyId, companyId);
		salesPartyIdContains.ifPresent(v -> w.like(CategoryTaxRate::getSalesPartyId, "%" + v + "%"));
		taxRateTypeEquals.ifPresent(v -> w.eq(CategoryTaxRate::getTaxRateType, v));
		w.orderByDesc(CategoryTaxRate::getId);
		long offset = (long) (page - 1) * (long) pageSize;
		w.last("LIMIT " + pageSize + " OFFSET " + offset);
		List<CategoryTaxRate> rows = mapper.selectList(w);
		List<Map<String, Object>> out = new ArrayList<>(rows.size());
		for (CategoryTaxRate e : rows) {
			out.add(rowMapFrom(e));
		}
		return out;
	}

	private Map<String, Object> rowMapFrom(CategoryTaxRate entity) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", entity.getId());
		m.put("company_id", entity.getCompanyId());
		m.put("sales_party_id", entity.getSalesPartyId());
		m.put("tax_rate_type", entity.getTaxRateType());
		m.put("category_ids", entity.getCategoryIds());
		m.put("invoice_tax_rate", entity.getInvoiceTaxRate());
		m.put("created_at", entity.getCreatedAt());
		m.put("updated_at", entity.getUpdatedAt());
		return m;
	}

	private Map<String, Object> toRowMap(CategoryTaxRate entity) {
		return rowMapFrom(entity);
	}
}
