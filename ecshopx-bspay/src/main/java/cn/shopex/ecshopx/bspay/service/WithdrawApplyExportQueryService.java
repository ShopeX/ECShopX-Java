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

package cn.shopex.ecshopx.bspay.service;

import cn.shopex.ecshopx.bspay.domain.WithdrawApply;
import cn.shopex.ecshopx.bspay.mapper.WithdrawApplyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WithdrawApplyExportQueryService {

	private final WithdrawApplyMapper withdrawApplyMapper;

	public WithdrawApplyExportQueryService(WithdrawApplyMapper withdrawApplyMapper) {
		this.withdrawApplyMapper = withdrawApplyMapper;
	}

	public long countWithdrawApplies(Map<String, Object> filter) {
		LambdaQueryWrapper<WithdrawApply> w = new LambdaQueryWrapper<>();
		applyWithdrawExportFilter(w, filter);
		return withdrawApplyMapper.selectCount(w);
	}

	public List<WithdrawApply> pageWithdrawAppliesForExport(
			Map<String, Object> filter, int pageOneBased, int pageSize) {
		int p = Math.max(pageOneBased, 1);
		int ps = pageSize > 0 ? pageSize : 1000;
		if (ps <= 0) {
			throw new IllegalArgumentException("pageSize");
		}
		long offsetLong = (p - 1L) * ps;
		LambdaQueryWrapper<WithdrawApply> w = new LambdaQueryWrapper<>();
		applyWithdrawExportFilter(w, filter);
		w.orderByDesc(WithdrawApply::getCreated).last("LIMIT " + offsetLong + ", " + ps);
		return withdrawApplyMapper.selectList(w);
	}

	private static void applyWithdrawExportFilter(LambdaQueryWrapper<WithdrawApply> w, Map<String, Object> filter) {
		Object rawCompany = filter.get("company_id");
		if (!(rawCompany instanceof Long companyId)) {
			throw new IllegalStateException("company_id");
		}
		w.eq(WithdrawApply::getCompanyId, companyId);
		Object operatorType = filter.get("operator_type");
		if (operatorType != null) {
			w.eq(WithdrawApply::getOperatorType, String.valueOf(operatorType));
		}
		Object operatorId = filter.get("operator_id");
		if (operatorId != null) {
			w.eq(WithdrawApply::getOperatorId, toLongBox(operatorId));
		}
		Object distributorId = filter.get("distributor_id");
		if (distributorId != null) {
			w.eq(WithdrawApply::getDistributorId, toLongBox(distributorId));
		}
		Object merchantId = filter.get("merchant_id");
		if (merchantId != null) {
			w.eq(WithdrawApply::getMerchantId, toLongBox(merchantId));
		}
		Object status = filter.get("status");
		if (status != null) {
			w.eq(WithdrawApply::getStatus, toIntegerBox(status));
		}
		Object gte = filter.get("created|gte");
		if (gte != null) {
			w.ge(WithdrawApply::getCreated, toIntegerBox(gte));
		}
		Object lte = filter.get("created|lte");
		if (lte != null) {
			w.le(WithdrawApply::getCreated, toIntegerBox(lte));
		}
	}

	private static Long toLongBox(Object o) {
		if (o instanceof Long l) {
			return l;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(o.toString().trim());
	}

	private static Integer toIntegerBox(Object o) {
		if (o instanceof Integer i) {
			return i;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		return Integer.parseInt(o.toString().trim());
	}
}
