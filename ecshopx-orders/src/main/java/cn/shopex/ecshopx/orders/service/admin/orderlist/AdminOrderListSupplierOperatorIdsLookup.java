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

package cn.shopex.ecshopx.orders.service.admin.orderlist;

import cn.shopex.ecshopx.supplier.domain.Supplier;
import cn.shopex.ecshopx.supplier.mapper.SupplierMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AdminOrderListSupplierOperatorIdsLookup {

	private final SupplierMapper supplierMapper;

	public AdminOrderListSupplierOperatorIdsLookup(SupplierMapper supplierMapper) {
		this.supplierMapper = supplierMapper;
	}

	public List<Long> listOperatorIdsBySupplierNameContains(long companyId, String supplierNameTrimmed) {
		List<Supplier> rows =
				supplierMapper.selectList(
						new LambdaQueryWrapper<Supplier>()
								.eq(Supplier::getCompanyId, companyId)
								.like(Supplier::getSupplierName, "%" + supplierNameTrimmed + "%"));
		List<Long> out = new ArrayList<>();
		for (Supplier s : rows) {
			if (s.getOperatorId() != null) {
				out.add(s.getOperatorId().longValue());
			}
		}
		return out;
	}
}
