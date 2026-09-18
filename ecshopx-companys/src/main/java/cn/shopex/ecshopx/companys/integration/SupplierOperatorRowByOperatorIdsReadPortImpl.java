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

package cn.shopex.ecshopx.companys.integration;

import cn.shopex.ecshopx.common.port.supplier.SupplierOperatorRowByOperatorIdsReadPort;
import cn.shopex.ecshopx.companys.dto.SupplierOperatorNameRow;
import cn.shopex.ecshopx.companys.mapper.SupplierOperatorReadMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SupplierOperatorRowByOperatorIdsReadPortImpl implements SupplierOperatorRowByOperatorIdsReadPort {

	private final SupplierOperatorReadMapper supplierOperatorReadMapper;

	public SupplierOperatorRowByOperatorIdsReadPortImpl(SupplierOperatorReadMapper supplierOperatorReadMapper) {
		this.supplierOperatorReadMapper = supplierOperatorReadMapper;
	}

	@Override
	public Map<Long, Map<String, Object>> mapRowByOperatorIds(long companyId, Collection<Long> operatorIds) {
		Map<Long, Map<String, Object>> out = new LinkedHashMap<>();
		if (operatorIds == null || operatorIds.isEmpty()) {
			return out;
		}
		List<Long> ids = new ArrayList<>();
		for (Long id : operatorIds) {
			if (id != null && id > 0L) {
				ids.add(id);
			}
		}
		if (ids.isEmpty()) {
			return out;
		}
		for (SupplierOperatorNameRow row :
				supplierOperatorReadMapper.selectSupplierNameRowsByCompanyIdAndOperatorIds(companyId, ids)) {
			if (row.getOperatorId() == null) {
				continue;
			}
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("operator_id", row.getOperatorId());
			m.put("supplier_name", row.getSupplierName() != null ? row.getSupplierName() : "");
			out.put(row.getOperatorId(), m);
		}
		return out;
	}
}
