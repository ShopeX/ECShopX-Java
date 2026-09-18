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

package cn.shopex.ecshopx.orders.repository;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.InvoiceSeller;
import cn.shopex.ecshopx.orders.mapper.InvoiceSellerMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class InvoiceSellerRepository {

	private final InvoiceSellerMapper mapper;

	public InvoiceSellerRepository(InvoiceSellerMapper mapper) {
		this.mapper = mapper;
	}

	public Optional<InvoiceSeller> findOneBySellerTaxNo(String sellerTaxNo) {
		LambdaQueryWrapper<InvoiceSeller> w = new LambdaQueryWrapper<>();
		w.eq(InvoiceSeller::getSellerTaxNo, sellerTaxNo);
		return Optional.ofNullable(mapper.selectOne(w.last("LIMIT 1")));
	}

	public Map<String, Object> insertEntity(InvoiceSeller entity) {
		mapper.insert(entity);
		return toRowMap(entity);
	}

	public Optional<InvoiceSeller> findById(Long id) {
		return Optional.ofNullable(mapper.selectById(id));
	}

	public Map<String, Object> updateEntityAndReturnRow(InvoiceSeller entity) {
		mapper.updateById(entity);
		InvoiceSeller fresh = mapper.selectById(entity.getId());
		if (fresh == null) {
			throw new ResourceException("未查询到更新数据");
		}
		return toRowMap(fresh);
	}

	public List<Map<String, Object>> selectSellerListPage(
			long companyId,
			Optional<String> sellerCompanyNameContains,
			Optional<String> sellerTaxNoEquals,
			int page,
			int pageSize) {
		LambdaQueryWrapper<InvoiceSeller> w = new LambdaQueryWrapper<>();
		w.eq(InvoiceSeller::getCompanyId, companyId);
		sellerCompanyNameContains.ifPresent(v -> w.like(InvoiceSeller::getSellerCompanyName, "%" + v + "%"));
		sellerTaxNoEquals.ifPresent(v -> w.eq(InvoiceSeller::getSellerTaxNo, v));
		w.orderByDesc(InvoiceSeller::getId);
		long offset = (long) (page - 1) * (long) pageSize;
		w.last("LIMIT " + pageSize + " OFFSET " + offset);
		List<InvoiceSeller> rows = mapper.selectList(w);
		List<Map<String, Object>> out = new ArrayList<>(rows.size());
		for (InvoiceSeller e : rows) {
			out.add(toRowMap(e));
		}
		return out;
	}

	public List<Map<String, Object>> selectSellerRowsByIdsOrderByIdDescLimit20(Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return Collections.emptyList();
		}
		LambdaQueryWrapper<InvoiceSeller> w = new LambdaQueryWrapper<>();
		w.in(InvoiceSeller::getId, ids);
		w.orderByDesc(InvoiceSeller::getId);
		w.last("LIMIT 20");
		List<InvoiceSeller> rows = mapper.selectList(w);
		List<Map<String, Object>> out = new ArrayList<>(rows.size());
		for (InvoiceSeller e : rows) {
			out.add(toRowMap(e));
		}
		return out;
	}

	public Map<String, Object> toRowMap(InvoiceSeller e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getId());
		m.put("company_id", e.getCompanyId());
		m.put("seller_name", e.getSellerName());
		m.put("payee", e.getPayee());
		m.put("reviewer", e.getReviewer());
		m.put("seller_company_name", e.getSellerCompanyName());
		m.put("seller_tax_no", e.getSellerTaxNo());
		m.put("seller_bank_name", e.getSellerBankName());
		m.put("seller_bank_account", e.getSellerBankAccount());
		m.put("seller_phone", e.getSellerPhone());
		m.put("seller_address", e.getSellerAddress());
		m.put("created_at", e.getCreatedAt());
		m.put("updated_at", e.getUpdatedAt());
		return m;
	}

	public Optional<Map<String, Object>> getInfoById(long id) {
		InvoiceSeller entity = mapper.selectById(id);
		if (entity == null) {
			return Optional.empty();
		}
		return Optional.of(toRowMap(entity));
	}
}

