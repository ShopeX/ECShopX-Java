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
import cn.shopex.ecshopx.orders.domain.CategoryTaxRate;
import cn.shopex.ecshopx.orders.repository.CategoryTaxRateRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryTaxRateAdminCreateTxService {

	private final CategoryTaxRateRepository categoryTaxRateRepository;
	private final ItemsCategoryInvoiceTaxRateAdminPort itemsCategoryInvoiceTaxRateAdminPort;

	public CategoryTaxRateAdminCreateTxService(CategoryTaxRateRepository categoryTaxRateRepository,
			ItemsCategoryInvoiceTaxRateAdminPort itemsCategoryInvoiceTaxRateAdminPort) {
		this.categoryTaxRateRepository = categoryTaxRateRepository;
		this.itemsCategoryInvoiceTaxRateAdminPort = itemsCategoryInvoiceTaxRateAdminPort;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createInTransaction(long companyId, String salesPartyId, String taxRateType,
			List<Long> categoryIdsSnapshot, String invoiceTaxRateForDisplay, String categoryIdsJsonForDb,
			boolean enterItemsUpdateBranch) {
		CategoryTaxRate entity = new CategoryTaxRate();
		entity.setCompanyId(companyId);
		entity.setSalesPartyId(salesPartyId);
		entity.setTaxRateType(taxRateType);
		entity.setCategoryIds(categoryIdsJsonForDb);
		entity.setInvoiceTaxRate(invoiceTaxRateForDisplay);
		int now = (int) (System.currentTimeMillis() / 1000L);
		entity.setCreatedAt(now);
		entity.setUpdatedAt(now);

		Map<String, Object> res = categoryTaxRateRepository.insertEntity(entity);
		Object idObj = res.get("id");
		long idLong = 0L;
		if (idObj instanceof Number n) {
			idLong = n.longValue();
		}
		if (idObj == null || idLong <= 0) {
			throw new ResourceException("创建失败");
		}

		if (enterItemsUpdateBranch) {
			itemsCategoryInvoiceTaxRateAdminPort.applyInvoiceTaxRateToCategories(companyId, categoryIdsSnapshot, idLong,
					entity.getInvoiceTaxRate());
		}
		return res;
	}
}
