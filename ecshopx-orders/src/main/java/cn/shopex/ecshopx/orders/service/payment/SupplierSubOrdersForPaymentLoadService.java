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

package cn.shopex.ecshopx.orders.service.payment;

import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SupplierSubOrdersForPaymentLoadService {

	private final SupplierOrderMapper supplierOrderMapper;

	public SupplierSubOrdersForPaymentLoadService(SupplierOrderMapper supplierOrderMapper) {
		this.supplierOrderMapper = supplierOrderMapper;
	}

	public List<Map<String, Object>> listMapsByCompanyAndParentOrderId(long companyId, long parentOrderId) {
		List<SupplierOrder> rows =
				supplierOrderMapper.selectList(
						new LambdaQueryWrapper<SupplierOrder>()
								.eq(SupplierOrder::getCompanyId, companyId)
								.eq(SupplierOrder::getOrderId, parentOrderId));
		List<Map<String, Object>> out = new ArrayList<>();
		for (SupplierOrder s : rows) {
			out.add(supplierRowToPaymentMap(s, parentOrderId));
		}
		return out;
	}

	private static Map<String, Object> supplierRowToPaymentMap(SupplierOrder s, long parentOrderId) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("supplier_id", s.getSupplierId() == null ? 0 : s.getSupplierId());
		m.put("order_id", Long.toString(parentOrderId));
		m.put("total_fee", s.getTotalFee() == null ? "0" : s.getTotalFee());
		m.put("item_fee", s.getItemFee() == null ? "0" : s.getItemFee());
		m.put("commission_fee", s.getCommissionFee() == null ? 0 : s.getCommissionFee());
		m.put("title", s.getTitle() == null ? "" : s.getTitle());
		m.put("user_id", s.getUserId() == null ? 0L : s.getUserId());
		m.put("distributor_id", s.getDistributorId() == null ? 0L : s.getDistributorId());
		m.put("shop_id", s.getShopId() == null ? 0L : s.getShopId());
		m.put("order_status", s.getOrderStatus() == null ? "" : s.getOrderStatus());
		m.put("pay_type", s.getPayType() == null ? "" : s.getPayType());
		return m;
	}
}
