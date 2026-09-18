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

package cn.shopex.ecshopx.orders.service.orderexport;

import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.ServiceOrdersMapper;
import cn.shopex.ecshopx.orders.service.orderexport.support.OrderExportNormalOrderQuerySupport;
import cn.shopex.ecshopx.orders.service.orderexport.support.OrderExportServiceOrderQuerySupport;
import cn.shopex.ecshopx.orders.service.orderexport.support.OrderExportSupplierOrderQuerySupport;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Component;

@Component
public class OrderExportCountCoordinator {

	private final NormalOrdersMapper normalOrdersMapper;
	private final ServiceOrdersMapper serviceOrdersMapper;
	private final SupplierOrderMapper supplierOrderMapper;

	public OrderExportCountCoordinator(
			NormalOrdersMapper normalOrdersMapper,
			ServiceOrdersMapper serviceOrdersMapper,
			SupplierOrderMapper supplierOrderMapper) {
		this.normalOrdersMapper = normalOrdersMapper;
		this.serviceOrdersMapper = serviceOrdersMapper;
		this.supplierOrderMapper = supplierOrderMapper;
	}

	public long count(String orderTypeKey, String exportTypeForJob, LinkedHashMap<String, Object> filter) {
		long companyId = longVal(filter.get("company_id"));
		if ("normal".equals(orderTypeKey)) {
			if ("normal_master_order".equals(exportTypeForJob)) {
				Long ct = normalOrdersMapper.selectCount(OrderExportNormalOrderQuerySupport.toCountWrapper(companyId, filter));
				return ct == null ? 0L : ct;
			}
			if ("normal_order".equals(exportTypeForJob)) {
				return normalOrdersMapper.countExportNormalOrderItems(filter);
			}
			Long ct = normalOrdersMapper.selectCount(OrderExportNormalOrderQuerySupport.toCountWrapper(companyId, filter));
			return ct == null ? 0L : ct;
		}
		if ("service".equals(orderTypeKey)) {
			Long ct = serviceOrdersMapper.selectCount(OrderExportServiceOrderQuerySupport.toCountWrapper(companyId, filter));
			return ct == null ? 0L : ct;
		}
		if ("supplier_order".equals(orderTypeKey)) {
			LinkedHashMap<String, Object> copy = new LinkedHashMap<>(filter);
			copy.remove("order_type");
			Long ct = supplierOrderMapper.selectCount(OrderExportSupplierOrderQuerySupport.toCountWrapper(companyId, copy));
			return ct == null ? 0L : ct;
		}
		return 0L;
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
