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

package cn.shopex.ecshopx.orders.service.categorytaxrate;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.goods.port.ItemsCategoryInvoiceTaxRateAdminPort;
import cn.shopex.ecshopx.orders.repository.CategoryTaxRateRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryTaxRateAdminDeleteService {

	private final CategoryTaxRateRepository categoryTaxRateRepository;
	private final ItemsCategoryInvoiceTaxRateAdminPort itemsCategoryInvoiceTaxRateAdminPort;
	private final ObjectMapper objectMapper;

	public CategoryTaxRateAdminDeleteService(CategoryTaxRateRepository categoryTaxRateRepository,
			ItemsCategoryInvoiceTaxRateAdminPort itemsCategoryInvoiceTaxRateAdminPort,
			ObjectMapper objectMapper) {
		this.categoryTaxRateRepository = categoryTaxRateRepository;
		this.itemsCategoryInvoiceTaxRateAdminPort = itemsCategoryInvoiceTaxRateAdminPort;
		this.objectMapper = objectMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> deleteTaxRate(long companyId, long rateId) {
		Optional<Map<String, Object>> opt = categoryTaxRateRepository.getInfoById(rateId);
		if (opt.isEmpty()) {
			throw new ResourceException("未查询到更新数据");
		}
		Map<String, Object> info = opt.get();
		Object t = info.get("tax_rate_type");
		boolean isAll = "ALL".equals(t == null ? null : String.valueOf(t));
		if (!isAll) {
			Object rawIds = info.get("category_ids");
			if (rawIds == null) {
				throw new ResourceException("未查询到更新数据");
			}
			if (!(rawIds instanceof String)) {
				throw new ResourceException("未查询到更新数据");
			}
			String json = ((String) rawIds).trim();
			if (json.isEmpty()) {
				throw new ResourceException("未查询到更新数据");
			}
			List<Long> list = parseCategoryIdsJson(json);
			if (list.isEmpty()) {
				throw new ResourceException("未查询到更新数据");
			}
			itemsCategoryInvoiceTaxRateAdminPort.clearInvoiceTaxFieldsForCategoryIds(companyId, list);
		}
		categoryTaxRateRepository.deleteById(rateId);
		LinkedHashMap<String, Object> body = new LinkedHashMap<>();
		body.put("success", Boolean.TRUE);
		return body;
	}

	private List<Long> parseCategoryIdsJson(String json) {
		List<?> rawList;
		try {
			rawList = objectMapper.readValue(json, new TypeReference<List<?>>() {});
		} catch (JsonProcessingException e) {
			throw new ResourceException("未查询到更新数据");
		}
		if (rawList == null) {
			throw new ResourceException("未查询到更新数据");
		}
		List<Long> out = new ArrayList<>(rawList.size());
		for (Object el : rawList) {
			if (el == null) {
				throw new ResourceException("未查询到更新数据");
			}
			if (el instanceof Number n) {
				out.add(n.longValue());
			} else {
				try {
					out.add(Long.parseLong(String.valueOf(el).trim()));
				} catch (NumberFormatException e) {
					throw new ResourceException("未查询到更新数据");
				}
			}
		}
		return out;
	}
}
