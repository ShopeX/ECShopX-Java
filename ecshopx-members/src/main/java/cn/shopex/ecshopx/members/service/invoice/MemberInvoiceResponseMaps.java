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

package cn.shopex.ecshopx.members.service.invoice;

import cn.shopex.ecshopx.members.domain.MembersInvoices;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MemberInvoiceResponseMaps {

	private MemberInvoiceResponseMaps() {}

	public static Map<String, Object> toRow(MembersInvoices e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("invoices_id", e.getInvoicesId());
		m.put("company_id", e.getCompanyId());
		m.put("user_id", e.getUserId());
		m.put("invoices_type", e.getInvoicesType());
		m.put("name", e.getName());
		m.put("telephone", e.getTelephone());
		m.put("tax_number", e.getTaxNumber());
		m.put("business_address", e.getBusinessAddress());
		m.put("bank", e.getBank());
		m.put("bank_account", e.getBankAccount());
		boolean flag = Boolean.TRUE.equals(e.getIsDef());
		m.put("is_def", flag ? 1 : 0);
		m.put("created", e.getCreated());
		m.put("updated", e.getUpdated());
		return m;
	}
}
