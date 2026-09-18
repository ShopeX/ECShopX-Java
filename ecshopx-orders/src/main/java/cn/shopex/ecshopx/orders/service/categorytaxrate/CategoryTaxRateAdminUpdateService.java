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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.goods.port.ItemsCategoryInvoiceTaxRateAdminPort;
import cn.shopex.ecshopx.orders.repository.CategoryTaxRateRepository;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CategoryTaxRateAdminUpdateService {

	private final CategoryTaxRateRepository categoryTaxRateRepository;
	private final ItemsCategoryInvoiceTaxRateAdminPort itemsCategoryInvoiceTaxRateAdminPort;
	private final ObjectMapper objectMapper;

	public CategoryTaxRateAdminUpdateService(CategoryTaxRateRepository categoryTaxRateRepository,
			ItemsCategoryInvoiceTaxRateAdminPort itemsCategoryInvoiceTaxRateAdminPort, ObjectMapper objectMapper) {
		this.categoryTaxRateRepository = categoryTaxRateRepository;
		this.itemsCategoryInvoiceTaxRateAdminPort = itemsCategoryInvoiceTaxRateAdminPort;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> updateTaxRate(long companyId, long rateId, Map<String, Object> mergedIn) {
		Map<String, Object> data = new LinkedHashMap<>(mergedIn);
		data.put("company_id", companyId);

		if (data.containsKey("category_ids") && isCategoryIdsArrayShape(data.get("category_ids"))) {
			List<Long> ids = parseCategoryIdsFromData(data);
			itemsCategoryInvoiceTaxRateAdminPort.assertCategoriesNotBoundToOtherTaxRate(companyId, ids, rateId);
		}

		Object t = data.get("tax_rate_type");
		boolean isAll = "ALL".equals(t instanceof String ? (String) t : (t == null ? null : String.valueOf(t)));
		if (isAll) {
			categoryTaxRateRepository.findFirstByTaxRateTypeAll().ifPresent(existing -> {
				if (existing.getId() != null && existing.getId().longValue() != rateId) {
					throw new ResourceException("全部分类税率配置已存在");
				}
			});
		}

		String invoiceTaxRateForItems = null;
		if (data.containsKey("invoice_tax_rate")) {
			Object v = data.get("invoice_tax_rate");
			invoiceTaxRateForItems = v == null ? null : String.valueOf(v);
		}

		if (data.containsKey("category_ids") && isCategoryIdsArrayShape(data.get("category_ids"))) {
			List<Long> ids = parseCategoryIdsFromData(data);
			itemsCategoryInvoiceTaxRateAdminPort.clearInvoiceTaxFieldsForInvoiceTaxRateId(companyId, rateId);
			itemsCategoryInvoiceTaxRateAdminPort.applyInvoiceTaxRateToCategories(companyId, ids, rateId,
					invoiceTaxRateForItems);
			try {
				data.put("category_ids", objectMapper.copy()
						.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, false)
						.writeValueAsString(ids));
			} catch (JsonProcessingException e) {
				throw new ResourceException("未查询到更新数据");
			}
		}

		return categoryTaxRateRepository.updateByIdPartialReturningRow(rateId, data);
	}

	private List<Long> parseCategoryIdsFromData(Map<String, Object> data) {
		if (!data.containsKey("category_ids")) {
			return null;
		}
		Object raw = data.get("category_ids");
		if (raw == null) {
			return null;
		}
		List<Long> out = new ArrayList<>();
		if (raw instanceof Collection<?> coll) {
			for (Object el : coll) {
				out.add(parseCategoryIdElement(el));
			}
			return out;
		}
		if (raw instanceof long[] arr) {
			for (long v : arr) {
				out.add(v);
			}
			return out;
		}
		if (raw instanceof int[] arr) {
			for (int v : arr) {
				out.add((long) v);
			}
			return out;
		}
		if (raw instanceof Object[] arr) {
			for (Object el : arr) {
				out.add(parseCategoryIdElement(el));
			}
			return out;
		}
		throw new BadRequestException("category_ids 格式错误");
	}

	private static long parseCategoryIdElement(Object el) {
		if (el == null) {
			throw new BadRequestException("category_ids 格式错误");
		}
		if (el instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(el));
		} catch (NumberFormatException e) {
			throw new BadRequestException("category_ids 格式错误");
		}
	}

	private static boolean isCategoryIdsArrayShape(Object raw) {
		return raw instanceof Collection<?>
				|| raw instanceof long[]
				|| raw instanceof int[]
				|| raw instanceof Object[];
	}
}
