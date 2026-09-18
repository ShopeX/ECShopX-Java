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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.repository.InvoiceSellerRepository;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class InvoiceSellerAdminDetailService {

	private final InvoiceSellerRepository invoiceSellerRepository;

	public InvoiceSellerAdminDetailService(InvoiceSellerRepository invoiceSellerRepository) {
		this.invoiceSellerRepository = invoiceSellerRepository;
	}

	public Map<String, Object> getSellerDetail(String id) {
		String s = (id == null) ? "" : id.trim();
		long sellerId;
		try {
			sellerId = Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new ResourceException("未找到销售方信息");
		}
		return invoiceSellerRepository.getInfoById(sellerId)
				.orElseThrow(() -> new ResourceException("未找到销售方信息"));
	}
}
