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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.domain.Statements;
import cn.shopex.ecshopx.orders.mapper.StatementsMapper;
import cn.shopex.ecshopx.supplier.domain.Supplier;
import cn.shopex.ecshopx.supplier.mapper.SupplierMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class StatementsAdminComfirmStatementService {

	private final StatementsMapper statementsMapper;
	private final SupplierMapper supplierMapper;

	public StatementsAdminComfirmStatementService(
			StatementsMapper statementsMapper, SupplierMapper supplierMapper) {
		this.statementsMapper = statementsMapper;
		this.supplierMapper = supplierMapper;
	}

	public Map<String, Object> comfirmStatement(
			long companyId,
			long statementId,
			String operatorType,
			Long distributorIdOrNull,
			Long merchantIdOrNull,
			long operatorId) {
		LambdaQueryWrapper<Statements> w =
				buildSelectWrapper(
						companyId, statementId, operatorType, distributorIdOrNull, merchantIdOrNull, operatorId);
		Statements row = statementsMapper.selectOne(w);
		if (row == null) {
			throw new ResourceException("结算单不存在");
		}

		if (Objects.equals("done", row.getStatementStatus())) {
			throw new ResourceException("已结算");
		}

		int nowSec = (int) Instant.now().getEpochSecond();

		if ("admin".equals(operatorType) || "staff".equals(operatorType)) {
			if (!Objects.equals("confirmed", row.getStatementStatus())) {
				throw new ResourceException("商家未确认");
			}
			LambdaUpdateWrapper<Statements> uw =
					buildUpdateWrapper(
									companyId,
									statementId,
									operatorType,
									distributorIdOrNull,
									merchantIdOrNull,
									operatorId)
							.set(Statements::getStatementStatus, "done")
							.set(Statements::getStatementTime, nowSec);
			int affected = statementsMapper.update(null, uw);
			if (affected == 0) {
				throw new ResourceException("未查询到更新数据");
			}
		} else if ("distributor".equals(operatorType)
				|| "merchant".equals(operatorType)
				|| "supplier".equals(operatorType)) {
			if (!Objects.equals("ready", row.getStatementStatus())) {
				throw new ResourceException("已确认");
			}
			LambdaUpdateWrapper<Statements> uw =
					buildUpdateWrapper(
									companyId,
									statementId,
									operatorType,
									distributorIdOrNull,
									merchantIdOrNull,
									operatorId)
							.set(Statements::getStatementStatus, "confirmed")
							.set(Statements::getConfirmTime, nowSec);
			int affected = statementsMapper.update(null, uw);
			if (affected == 0) {
				throw new ResourceException("未查询到更新数据");
			}
		} else {
			throw new ResourceException("没有权限操作");
		}

		LambdaQueryWrapper<Statements> wAgain =
				buildSelectWrapper(
						companyId, statementId, operatorType, distributorIdOrNull, merchantIdOrNull, operatorId);
		Statements refreshed = statementsMapper.selectOne(wAgain);
		if (refreshed == null) {
			throw new ResourceException("未查询到更新数据");
		}
		return toStatementRowMap(refreshed);
	}

	private long resolveSupplierIdByOperator(long companyId, long operatorId) {
		if (operatorId == 0L) {
			return 0L;
		}
		Supplier row =
				supplierMapper.selectOne(
						new LambdaQueryWrapper<Supplier>()
								.eq(Supplier::getCompanyId, companyId)
								.eq(Supplier::getOperatorId, operatorId)
								.last("LIMIT 1"));
		if (row == null || row.getId() == null) {
			return 0L;
		}
		return row.getId();
	}

	private LambdaQueryWrapper<Statements> buildSelectWrapper(
			long companyId,
			long statementId,
			String operatorType,
			Long distributorIdOrNull,
			Long merchantIdOrNull,
			long operatorId) {
		LambdaQueryWrapper<Statements> w =
				new LambdaQueryWrapper<Statements>()
						.eq(Statements::getId, statementId)
						.eq(Statements::getCompanyId, companyId);
		applyRoleDimensionToQuery(w, companyId, operatorType, distributorIdOrNull, merchantIdOrNull, operatorId);
		return w;
	}

	private LambdaUpdateWrapper<Statements> buildUpdateWrapper(
			long companyId,
			long statementId,
			String operatorType,
			Long distributorIdOrNull,
			Long merchantIdOrNull,
			long operatorId) {
		LambdaUpdateWrapper<Statements> uw =
				new LambdaUpdateWrapper<Statements>()
						.eq(Statements::getId, statementId)
						.eq(Statements::getCompanyId, companyId);
		applyRoleDimensionToUpdate(uw, companyId, operatorType, distributorIdOrNull, merchantIdOrNull, operatorId);
		return uw;
	}

	private void applyRoleDimensionToQuery(
			LambdaQueryWrapper<Statements> w,
			long companyId,
			String operatorType,
			Long distributorIdOrNull,
			Long merchantIdOrNull,
			long operatorId) {
		if ("distributor".equals(operatorType)) {
			w.eq(Statements::getDistributorId, distributorIdOrNull != null ? distributorIdOrNull : 0L);
		} else if ("merchant".equals(operatorType)) {
			w.eq(Statements::getMerchantId, merchantIdOrNull != null ? merchantIdOrNull : 0L);
		} else if ("supplier".equals(operatorType)) {
			long sid = resolveSupplierIdByOperator(companyId, operatorId);
			w.eq(Statements::getSupplierId, sid);
		} else if ("admin".equals(operatorType) || "staff".equals(operatorType)) {
			// tenant-wide: id + company_id only
		} else {
			throw new ResourceException("没有权限操作");
		}
	}

	private void applyRoleDimensionToUpdate(
			LambdaUpdateWrapper<Statements> uw,
			long companyId,
			String operatorType,
			Long distributorIdOrNull,
			Long merchantIdOrNull,
			long operatorId) {
		if ("distributor".equals(operatorType)) {
			uw.eq(Statements::getDistributorId, distributorIdOrNull != null ? distributorIdOrNull : 0L);
		} else if ("merchant".equals(operatorType)) {
			uw.eq(Statements::getMerchantId, merchantIdOrNull != null ? merchantIdOrNull : 0L);
		} else if ("supplier".equals(operatorType)) {
			long sid = resolveSupplierIdByOperator(companyId, operatorId);
			uw.eq(Statements::getSupplierId, sid);
		} else if ("admin".equals(operatorType) || "staff".equals(operatorType)) {
			// tenant-wide: id + company_id only
		} else {
			throw new ResourceException("没有权限操作");
		}
	}

	private static Map<String, Object> toStatementRowMap(Statements s) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("id", s.getId());
		m.put("company_id", s.getCompanyId());
		m.put("merchant_id", s.getMerchantId());
		m.put("distributor_id", s.getDistributorId());
		m.put("statement_no", s.getStatementNo());
		m.put("order_num", s.getOrderNum());
		m.put("total_fee", s.getTotalFee());
		m.put("freight_fee", s.getFreightFee());
		m.put("intra_city_freight_fee", s.getIntraCityFreightFee());
		m.put("rebate_fee", s.getRebateFee());
		m.put("refund_fee", s.getRefundFee());
		m.put("statement_fee", s.getStatementFee());
		m.put("start_time", s.getStartTime());
		m.put("end_time", s.getEndTime());
		m.put("supplier_id", s.getSupplierId());
		m.put("merchant_type", s.getMerchantType());
		m.put("point_fee", s.getPointFee());
		m.put("refund_num", s.getRefundNum());
		m.put("refund_point", s.getRefundPoint());
		m.put("refund_cost_fee", s.getRefundCostFee());
		m.put("confirm_time", s.getConfirmTime());
		m.put("statement_time", s.getStatementTime());
		m.put("statement_status", s.getStatementStatus());
		return m;
	}
}
