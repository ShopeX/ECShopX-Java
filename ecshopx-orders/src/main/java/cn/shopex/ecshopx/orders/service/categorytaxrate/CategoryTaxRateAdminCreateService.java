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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CategoryTaxRateAdminCreateService {

	private final CategoryTaxRateRepository categoryTaxRateRepository;
	private final ItemsCategoryInvoiceTaxRateAdminPort itemsCategoryInvoiceTaxRateAdminPort;
	private final CategoryTaxRateAdminCreateTxService categoryTaxRateAdminCreateTxService;
	private final ObjectMapper objectMapper;

	public CategoryTaxRateAdminCreateService(CategoryTaxRateRepository categoryTaxRateRepository,
			ItemsCategoryInvoiceTaxRateAdminPort itemsCategoryInvoiceTaxRateAdminPort,
			CategoryTaxRateAdminCreateTxService categoryTaxRateAdminCreateTxService, ObjectMapper objectMapper) {
		this.categoryTaxRateRepository = categoryTaxRateRepository;
		this.itemsCategoryInvoiceTaxRateAdminPort = itemsCategoryInvoiceTaxRateAdminPort;
		this.categoryTaxRateAdminCreateTxService = categoryTaxRateAdminCreateTxService;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> createTaxRate(long companyId, Map<String, Object> mergedIn) {
		Map<String, Object> merged = new LinkedHashMap<>(mergedIn);
		merged.put("company_id", companyId);

		String salesPartyId = readRequiredNonEmptyScalar(merged, "sales_party_id", "销售方不能为空");
		String taxRateType = readRequiredNonEmptyScalar(merged, "tax_rate_type", "税率分类不能为空");
		String invoiceTaxRateRaw = readRequiredNonEmptyScalar(merged, "invoice_tax_rate", "发票税率不能为空");

		if (!"SPECIFIED".equals(taxRateType) && !merged.containsKey("category_ids")) {
			throw new BadRequestException("请求体缺少 category_ids");
		}
		if ("SPECIFIED".equals(taxRateType) && !merged.containsKey("category_ids")) {
			throw new BadRequestException("指定分类税率时，分类不能为空");
		}

		List<Long> categoryIdsParsed = parseCategoryIdsFromMerged(merged);
		String categoryIdsJsonForDb = resolveCategoryIdsJsonForDb(merged, categoryIdsParsed);

		validateTaxRateData(companyId, salesPartyId, taxRateType, invoiceTaxRateRaw, categoryIdsParsed,
				categoryIdsJsonForDb);

		itemsCategoryInvoiceTaxRateAdminPort.assertNoOccupiedCategories(companyId,
				categoryIdsJsonForDb == null ? "" : categoryIdsJsonForDb);

		boolean enterItemsUpdateBranch = merged.containsKey("category_ids") && categoryIdsJsonForDb != null
				&& !categoryIdsJsonForDb.isEmpty();

		List<Long> categoryIdsSnapshot = categoryIdsParsed != null ? categoryIdsParsed : List.of();

		return categoryTaxRateAdminCreateTxService.createInTransaction(companyId, salesPartyId, taxRateType,
				categoryIdsSnapshot, invoiceTaxRateRaw, categoryIdsJsonForDb, enterItemsUpdateBranch);
	}

	private static String readRequiredNonEmptyScalar(Map<String, Object> merged, String key, String emptyMessage) {
		if (!merged.containsKey(key)) {
			throw new BadRequestException(emptyMessage);
		}
		Object v = merged.get(key);
		if (v == null) {
			throw new BadRequestException(emptyMessage);
		}
		if (v instanceof String s) {
			if (s.isEmpty()) {
				throw new BadRequestException(emptyMessage);
			}
			return s;
		}
		String as = String.valueOf(v);
		if (as.isEmpty()) {
			throw new BadRequestException(emptyMessage);
		}
		return as;
	}

	private List<Long> parseCategoryIdsFromMerged(Map<String, Object> merged) {
		if (!merged.containsKey("category_ids")) {
			return null;
		}
		Object raw = merged.get("category_ids");
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

	private String resolveCategoryIdsJsonForDb(Map<String, Object> merged, List<Long> categoryIdsParsed) {
		if (!merged.containsKey("category_ids")) {
			return null;
		}
		Object raw = merged.get("category_ids");
		if (raw == null) {
			return null;
		}
		if (!isCategoryIdsArrayShape(raw)) {
			return null;
		}
		if (categoryIdsParsed == null || categoryIdsParsed.isEmpty()) {
			return "[]";
		}
		try {
			return objectMapper.copy()
					.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, false)
					.writeValueAsString(categoryIdsParsed);
		} catch (JsonProcessingException e) {
			throw new ResourceException("创建失败");
		}
	}

	private static boolean isCategoryIdsArrayShape(Object raw) {
		return raw instanceof Collection<?>
				|| raw instanceof long[]
				|| raw instanceof int[]
				|| raw instanceof Object[];
	}

	private void validateTaxRateData(long companyId, String salesPartyId, String taxRateType, String invoiceTaxRateRaw,
			List<Long> categoryIdsParsed, String categoryIdsJsonForDb) {
		readRequiredNonEmptyScalarInValidate(salesPartyId, "销售方不能为空");
		readRequiredNonEmptyScalarInValidate(taxRateType, "税率分类不能为空");
		readRequiredNonEmptyScalarInValidate(invoiceTaxRateRaw, "发票税率不能为空");

		if (taxRateType.equals("SPECIFIED") && (categoryIdsParsed == null || categoryIdsParsed.isEmpty())) {
			throw new BadRequestException("指定分类税率时，分类不能为空");
		}

		String s = invoiceTaxRateRaw.replace("%", "");
		if (!s.matches("^\\d{1,2}(\\.\\d{1,2})?$")) {
			throw new BadRequestException("税率格式错误");
		}
		BigDecimal bd;
		try {
			bd = new BigDecimal(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException("税率格式错误");
		}
		if (bd.compareTo(BigDecimal.ZERO) < 0 || bd.compareTo(new BigDecimal("100")) > 0) {
			throw new BadRequestException("税率超出范围");
		}

		String categoryIdsArgForDup = taxRateType.equals("SPECIFIED") ? categoryIdsJsonForDb : null;
		if (categoryTaxRateRepository.findDuplicateForCreate(companyId, salesPartyId, taxRateType, categoryIdsArgForDup)
				.isPresent()) {
			throw new ResourceException("分类税率已存在");
		}
	}

	private static void readRequiredNonEmptyScalarInValidate(String value, String emptyMessage) {
		if (value == null || value.isEmpty()) {
			throw new BadRequestException(emptyMessage);
		}
	}
}
