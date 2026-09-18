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

package cn.shopex.ecshopx.orders.service.invoice;

import cn.shopex.ecshopx.orders.domain.OrderInvoiceItem;
import java.util.LinkedHashMap;
import java.util.Map;

public final class OrderInvoiceItemApiRowSupport {

	private OrderInvoiceItemApiRowSupport() {
	}

	public static Map<String, Object> toRow(OrderInvoiceItem row) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", row.getId());
		m.put("user_id", row.getUserId());
		m.put("company_id", row.getCompanyId());
		m.put("invoice_id", row.getInvoiceId());
		m.put("invoice_apply_bn", row.getInvoiceApplyBn());
		m.put("order_id", row.getOrderId());
		m.put("oid", row.getOid());
		m.put("item_name", row.getItemName());
		m.put("item_bn", row.getItemBn());
		m.put("main_img", row.getMainImg());
		m.put("spec_info", row.getSpecInfo());
		m.put("item_spec_desc", row.getItemSpecDesc());
		m.put("num", row.getNum());
		m.put("amount", row.getAmount());
		m.put("invoice_tax_rate", row.getInvoiceTaxRate());
		m.put("original_num", row.getOriginalNum());
		m.put("original_amount", row.getOriginalAmount());
		m.put("create_time", row.getCreateTime());
		m.put("update_time", row.getUpdateTime());
		return m;
	}
}
