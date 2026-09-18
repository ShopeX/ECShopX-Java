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

import cn.shopex.ecshopx.orders.repository.InvoiceSellerRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class InvoiceSellerAdminListService {

	private static final Logger log = LoggerFactory.getLogger(InvoiceSellerAdminListService.class);

	private final InvoiceSellerRepository invoiceSellerRepository;

	public InvoiceSellerAdminListService(InvoiceSellerRepository invoiceSellerRepository) {
		this.invoiceSellerRepository = invoiceSellerRepository;
	}

	public Map<String, Object> getSellerList(
			long companyId,
			Optional<String> sellerCompanyNameLike,
			Optional<String> sellerTaxNoEq,
			int page,
			int pageSize) {
		log.info("invoice_seller list filter companyId={} sellerCompanyNameLike={} sellerTaxNoEq={} page={} pageSize={}",
				companyId, sellerCompanyNameLike.orElse(null), sellerTaxNoEq.orElse(null), page, pageSize);

		List<Map<String, Object>> list = invoiceSellerRepository.selectSellerListPage(companyId,
				sellerCompanyNameLike, sellerTaxNoEq, page, pageSize);
		int totalCount = list.size();
		log.info("getSellerList:result:listSize={}", list.size());

		LinkedHashMap<String, Object> result = new LinkedHashMap<>();
		result.put("list", list);
		result.put("total_count", totalCount);
		return result;
	}
}
