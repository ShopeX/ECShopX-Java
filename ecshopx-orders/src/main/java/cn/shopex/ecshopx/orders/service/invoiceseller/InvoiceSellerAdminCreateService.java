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

package cn.shopex.ecshopx.orders.service.invoiceseller;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.InvoiceSeller;
import cn.shopex.ecshopx.orders.repository.InvoiceSellerRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceSellerAdminCreateService {

	private final InvoiceSellerRepository invoiceSellerRepository;

	public InvoiceSellerAdminCreateService(InvoiceSellerRepository invoiceSellerRepository) {
		this.invoiceSellerRepository = invoiceSellerRepository;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createSeller(long companyId, Map<String, Object> mergedIn) {
		Map<String, Object> merged = new LinkedHashMap<>(mergedIn);
		merged.put("company_id", companyId);

		String sellerCompanyName = readRequiredNonEmptyScalar(merged, "seller_company_name", "销售方名称不能为空");
		String sellerTaxNo = readRequiredNonEmptyScalar(merged, "seller_tax_no", "销售方税号不能为空");
		String sellerBankName = readRequiredNonEmptyScalar(merged, "seller_bank_name", "销售方开户行不能为空");
		String sellerBankAccount = readRequiredNonEmptyScalar(merged, "seller_bank_account", "销售方银行账号不能为空");
		String sellerPhone = readRequiredNonEmptyScalar(merged, "seller_phone", "销售方电话不能为空");
		String sellerAddress = readRequiredNonEmptyScalar(merged, "seller_address", "销售方地址不能为空");

		if (invoiceSellerRepository.findOneBySellerTaxNo(sellerTaxNo).isPresent()) {
			throw new ResourceException("销售方税号已存在");
		}

		String sellerName = resolveOptionalSpaceField(merged, "seller_name");
		String payee = resolveOptionalSpaceField(merged, "payee");
		String reviewer = resolveOptionalSpaceField(merged, "reviewer");

		InvoiceSeller entity = new InvoiceSeller();
		entity.setCompanyId(companyId);
		entity.setSellerCompanyName(sellerCompanyName);
		entity.setSellerTaxNo(sellerTaxNo);
		entity.setSellerBankName(sellerBankName);
		entity.setSellerBankAccount(sellerBankAccount);
		entity.setSellerPhone(sellerPhone);
		entity.setSellerAddress(sellerAddress);
		entity.setSellerName(sellerName);
		entity.setPayee(payee);
		entity.setReviewer(reviewer);
		int now = (int) (System.currentTimeMillis() / 1000L);
		entity.setCreatedAt(now);
		entity.setUpdatedAt(now);

		Map<String, Object> data = invoiceSellerRepository.insertEntity(entity);
		Long id = entity.getId();
		if (id == null || id <= 0) {
			throw new ResourceException("创建失败");
		}
		return data;
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

	private static String resolveOptionalSpaceField(Map<String, Object> merged, String key) {
		if (!merged.containsKey(key) || merged.get(key) == null) {
			return "";
		}
		Object v = merged.get(key);
		if (v instanceof String s) {
			if (s.isEmpty()) {
				return "";
			}
			return s;
		}
		return String.valueOf(v);
	}
}
