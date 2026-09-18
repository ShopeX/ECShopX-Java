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

package cn.shopex.ecshopx.supplier.repository;

import cn.shopex.ecshopx.supplier.domain.Supplier;
import cn.shopex.ecshopx.supplier.mapper.SupplierMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class SupplierOperatorQueryRepository {

	private final SupplierMapper supplierMapper;

	public SupplierOperatorQueryRepository(SupplierMapper supplierMapper) {
		this.supplierMapper = supplierMapper;
	}

	/**
	 * supplier_name LIKE %keyword% → operator_id 列表（去重）。
	 */
	public List<Long> listOperatorIdsBySupplierNameLike(long companyId, String keyword) {
		if (!StringUtils.hasText(keyword)) {
			return List.of();
		}
		String k = keyword.trim();
		LambdaQueryWrapper<Supplier> w = new LambdaQueryWrapper<>();
		w.eq(Supplier::getCompanyId, companyId).like(Supplier::getSupplierName, k);
		return supplierMapper.selectList(w).stream().map(Supplier::getOperatorId).filter(id -> id != null && id > 0).distinct()
				.collect(Collectors.toList());
	}

	public Optional<Long> findSupplierIdByCompanyAndOperatorId(long companyId, long operatorId) {
		LambdaQueryWrapper<Supplier> w = new LambdaQueryWrapper<>();
		w.eq(Supplier::getCompanyId, companyId).eq(Supplier::getOperatorId, operatorId).last("LIMIT 1");
		Supplier s = supplierMapper.selectOne(w);
		if (s == null || s.getId() == null) {
			return Optional.empty();
		}
		return Optional.of(s.getId());
	}

	public Map<Long, String> mapSupplierNameByOperatorIds(long companyId, Collection<Long> operatorIds) {
		Map<Long, String> out = new LinkedHashMap<>();
		if (operatorIds == null || operatorIds.isEmpty()) {
			return out;
		}
		LambdaQueryWrapper<Supplier> w = new LambdaQueryWrapper<>();
		w.eq(Supplier::getCompanyId, companyId).in(Supplier::getOperatorId, operatorIds);
		for (Supplier s : supplierMapper.selectList(w)) {
			if (s.getOperatorId() != null) {
				out.put(s.getOperatorId(), s.getSupplierName() != null ? s.getSupplierName() : "");
			}
		}
		return out;
	}
}
