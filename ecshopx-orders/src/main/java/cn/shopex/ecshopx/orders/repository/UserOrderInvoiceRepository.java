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
import cn.shopex.ecshopx.orders.domain.UserOrderInvoice;
import cn.shopex.ecshopx.orders.mapper.UserOrderInvoiceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class UserOrderInvoiceRepository {

	private final UserOrderInvoiceMapper mapper;

	public UserOrderInvoiceRepository(UserOrderInvoiceMapper mapper) {
		this.mapper = mapper;
	}

	public Page<UserOrderInvoice> lists(
			long companyId,
			Long filterUserId,
			Long filterId,
			String filterOrderId,
			Integer filterStatus,
			int page,
			int pageSize) {
		LambdaQueryWrapper<UserOrderInvoice> w = new LambdaQueryWrapper<>();
		w.eq(UserOrderInvoice::getCompanyId, companyId);
		if (filterUserId != null && filterUserId != 0L) {
			w.eq(UserOrderInvoice::getUserId, filterUserId);
		}
		if (filterId != null && filterId != 0L) {
			w.eq(UserOrderInvoice::getId, filterId);
		}
		if (filterOrderId != null
				&& !filterOrderId.isEmpty()
				&& !"0".equals(filterOrderId)) {
			w.eq(UserOrderInvoice::getOrderId, filterOrderId);
		}
		if (filterStatus != null && filterStatus != 0) {
			w.eq(UserOrderInvoice::getStatus, filterStatus);
		}
		w.orderByDesc(UserOrderInvoice::getId);
		Page<UserOrderInvoice> p = new Page<>(page, pageSize);
		return mapper.selectPage(p, w);
	}

	/**
	 * Aligns with legacy user-order invoice save: upsert invoice JSON by order id, company, and user.
	 *
	 * @return true when a row was inserted or updated
	 */
	public boolean saveInvoiceData(String orderId, long companyId, long userId, String invoiceJson) {
		if (!StringUtils.hasText(orderId) || invoiceJson == null) {
			return false;
		}
		LambdaQueryWrapper<UserOrderInvoice> w = new LambdaQueryWrapper<>();
		w.eq(UserOrderInvoice::getOrderId, orderId)
				.eq(UserOrderInvoice::getCompanyId, companyId)
				.eq(UserOrderInvoice::getUserId, userId);
		UserOrderInvoice existing = mapper.selectOne(w.last("LIMIT 1"));
		if (existing != null) {
			LambdaUpdateWrapper<UserOrderInvoice> uw = new LambdaUpdateWrapper<>();
			uw.eq(UserOrderInvoice::getId, existing.getId());
			uw.set(UserOrderInvoice::getInvoice, invoiceJson);
			return mapper.update(null, uw) > 0;
		}
		UserOrderInvoice row = new UserOrderInvoice();
		row.setOrderId(orderId);
		row.setCompanyId(companyId);
		row.setUserId(userId);
		row.setInvoice(invoiceJson);
		row.setStatus(0);
		return mapper.insert(row) > 0;
	}

	public void updateInvoiceAndStatusById(long id, String invoiceJson, Integer statusOrNull) {
		LambdaUpdateWrapper<UserOrderInvoice> uw = new LambdaUpdateWrapper<>();
		uw.eq(UserOrderInvoice::getId, id);
		uw.set(UserOrderInvoice::getInvoice, invoiceJson);
		if (statusOrNull != null) {
			uw.set(UserOrderInvoice::getStatus, statusOrNull);
		}
		int affected = mapper.update(null, uw);
		if (affected == 0) {
			throw new ResourceException("未查询到更新数据");
		}
	}
}
