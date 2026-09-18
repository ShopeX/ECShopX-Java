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

import cn.shopex.ecshopx.companys.mapper.DistributionDistributorSelfReadMapper;
import cn.shopex.ecshopx.orders.domain.Statements;
import cn.shopex.ecshopx.orders.mapper.StatementsMapper;
import cn.shopex.ecshopx.orders.service.statement.StatementsSummarizedAdminFilterAssembler;
import cn.shopex.ecshopx.orders.service.statement.export.StatementsSummarizedExportQuerySupport;
import cn.shopex.ecshopx.supplier.domain.Supplier;
import cn.shopex.ecshopx.supplier.mapper.SupplierMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class StatementsAdminGetSummarizedService {

	private final StatementsSummarizedAdminFilterAssembler filterAssembler;
	private final StatementsMapper statementsMapper;
	private final DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper;
	private final SupplierMapper supplierMapper;

	public StatementsAdminGetSummarizedService(
			StatementsSummarizedAdminFilterAssembler filterAssembler,
			StatementsMapper statementsMapper,
			DistributionDistributorSelfReadMapper distributionDistributorSelfReadMapper,
			SupplierMapper supplierMapper) {
		this.filterAssembler = filterAssembler;
		this.statementsMapper = statementsMapper;
		this.distributionDistributorSelfReadMapper = distributionDistributorSelfReadMapper;
		this.supplierMapper = supplierMapper;
	}

	public Map<String, Object> getSummarized(
			long companyId,
			long operatorId,
			String operatorType,
			Long jwtDistributorIdOrNull,
			Long jwtMerchantIdOrNull,
			Map<String, Object> queryParams,
			String pageRaw,
			String pageSizeRaw) {
		LinkedHashMap<String, Object> listFilter =
				filterAssembler.assembleListFilter(
						companyId, operatorId, operatorType, jwtDistributorIdOrNull, jwtMerchantIdOrNull, queryParams);

		LambdaQueryWrapper<Statements> countW = StatementsSummarizedExportQuerySupport.toWrapper(listFilter);
		long totalLong = statementsMapper.selectCount(countW);
		int totalCount = totalLong > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalLong;

		int page = parsePageOneBased(pageRaw, 1);
		int pageSize = parsePageSize(pageSizeRaw, 20);

		List<Statements> records;
		if (totalCount == 0) {
			records = List.of();
		} else {
			LambdaQueryWrapper<Statements> listQuery =
					StatementsSummarizedExportQuerySupport.toWrapper(listFilter);
			listQuery.orderByDesc(Statements::getCreated);
			if (pageSize > 0) {
				Page<Statements> p = new Page<>(page, pageSize, false);
				records = statementsMapper.selectPage(p, listQuery).getRecords();
			} else {
				records = statementsMapper.selectList(listQuery);
			}
		}

		LinkedHashMap<String, Object> sumFiler = new LinkedHashMap<>();
		sumFiler.put("company_id", companyId);
		sumFiler.put("merchant_type", listFilter.get("merchant_type"));

		String op = operatorType == null ? "" : operatorType.trim();
		if ("distributor".equals(op)) {
			sumFiler.put(
					"distributor_id", jwtDistributorIdOrNull != null ? jwtDistributorIdOrNull : 0L);
		} else if ("merchant".equals(op)) {
			sumFiler.put("merchant_id", jwtMerchantIdOrNull != null ? jwtMerchantIdOrNull : 0L);
		} else if ("supplier".equals(op)) {
			Object sid = listFilter.get("supplier_id");
			long midVal = 0L;
			if (sid instanceof Number n) {
				midVal = n.longValue();
			} else if (sid != null) {
				try {
					midVal = Long.parseLong(String.valueOf(sid).trim());
				} catch (NumberFormatException e) {
					midVal = 0L;
				}
			}
			sumFiler.put("merchant_id", midVal);
		}

		sumFiler.put("statement_status", List.of("ready", "confirmed"));
		long readySum = sumViaMapper(sumFiler);

		sumFiler.put("statement_status", "done");
		long doneSum = sumViaMapper(sumFiler);

		List<Map<String, Object>> listRows = new ArrayList<>();
		if (!records.isEmpty()) {
			Set<Long> distributorIds = new LinkedHashSet<>();
			Set<Long> merchantIds = new LinkedHashSet<>();
			Set<Long> supplierIds = new LinkedHashSet<>();
			for (Statements s : records) {
				if (s.getDistributorId() != null) {
					distributorIds.add(s.getDistributorId());
				}
				if (s.getMerchantId() != null) {
					merchantIds.add(s.getMerchantId());
				}
				if (s.getSupplierId() != null && s.getSupplierId() > 0) {
					supplierIds.add(s.getSupplierId());
				}
			}

			Map<Long, String> distributorNameById = loadDistributorNames(companyId, distributorIds);
			Map<Long, String> merchantNameById = loadMerchantNames(companyId, merchantIds);
			Map<Long, String> supplierNameById = loadSupplierNames(supplierIds);

			for (Statements s : records) {
				Map<String, Object> row = statementToRow(s);
				row.put(
						"distributor_name",
						distributorNameById.getOrDefault(
								s.getDistributorId() == null ? 0L : s.getDistributorId(), ""));
				row.put(
						"merchant_name",
						merchantNameById.getOrDefault(s.getMerchantId() == null ? 0L : s.getMerchantId(), ""));
				row.put(
						"supplier_name",
						supplierNameById.getOrDefault(s.getSupplierId() == null ? 0L : s.getSupplierId(), ""));
				listRows.add(row);
			}
		}

		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", totalCount);
		out.put("list", listRows);
		out.put("total_statement_fee_ready", readySum);
		out.put("total_statement_fee_done", doneSum);
		return out;
	}

	private long sumViaMapper(LinkedHashMap<String, Object> sumFiler) {
		LambdaQueryWrapper<Statements> w = StatementsSummarizedExportQuerySupport.toWrapper(sumFiler);
		Long v = statementsMapper.sumStatementFee(w);
		return v == null ? 0L : v;
	}

	private static int parsePageOneBased(String raw, int defaultValue) {
		if (raw == null || raw.trim().isEmpty()) {
			return defaultValue;
		}
		try {
			int v = Integer.parseInt(raw.trim());
			return v < 1 ? 1 : v;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private static int parsePageSize(String raw, int defaultValue) {
		if (raw == null || raw.trim().isEmpty()) {
			return defaultValue;
		}
		try {
			int v = Integer.parseInt(raw.trim());
			return v < 1 ? defaultValue : v;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private static Map<String, Object> statementToRow(Statements s) {
		LinkedHashMap<String, Object> row = new LinkedHashMap<>();
		row.put("id", s.getId());
		row.put("company_id", s.getCompanyId());
		row.put("merchant_id", s.getMerchantId());
		row.put("supplier_id", s.getSupplierId());
		row.put("distributor_id", s.getDistributorId());
		row.put("merchant_type", s.getMerchantType());
		row.put("statement_no", s.getStatementNo());
		row.put("order_num", s.getOrderNum());
		row.put("total_fee", s.getTotalFee());
		row.put("freight_fee", s.getFreightFee());
		row.put("intra_city_freight_fee", s.getIntraCityFreightFee());
		row.put("rebate_fee", s.getRebateFee());
		row.put("refund_fee", s.getRefundFee());
		row.put("statement_fee", s.getStatementFee());
		row.put("start_time", s.getStartTime());
		row.put("end_time", s.getEndTime());
		row.put("confirm_time", s.getConfirmTime());
		row.put("statement_time", s.getStatementTime());
		row.put("statement_status", s.getStatementStatus());
		row.put("created", s.getCreated());
		row.put("updated", s.getUpdated());
		row.put("point_fee", s.getPointFee());
		row.put("refund_num", s.getRefundNum());
		row.put("refund_point", s.getRefundPoint());
		row.put("refund_cost_fee", s.getRefundCostFee());
		return row;
	}

	private Map<Long, String> loadDistributorNames(long companyId, Set<Long> distributorIds) {
		Map<Long, String> out = new HashMap<>();
		if (distributorIds.isEmpty()) {
			return out;
		}
		List<Long> idList = new ArrayList<>(distributorIds);
		List<Map<String, Object>> rows =
				distributionDistributorSelfReadMapper.listDistributorNamesByCompanyAndIds(companyId, idList);
		for (Map<String, Object> dbRow : rows) {
			Long did = longFromCell(dbRow.get("distributor_id"));
			if (did != null) {
				Object nameObj = dbRow.get("name");
				out.put(did, nameObj == null ? "" : String.valueOf(nameObj));
			}
		}
		return out;
	}

	private Map<Long, String> loadMerchantNames(long companyId, Set<Long> merchantIds) {
		Map<Long, String> out = new HashMap<>();
		if (merchantIds.isEmpty()) {
			return out;
		}
		List<Long> idList = new ArrayList<>(merchantIds);
		List<Map<String, Object>> dbRows =
				distributionDistributorSelfReadMapper.listMerchantNamesByCompanyAndIds(companyId, idList);
		for (Map<String, Object> dbRow : dbRows) {
			Long mid = longFromCell(dbRow.get("merchant_id"));
			if (mid != null) {
				Object nameObj = dbRow.get("merchant_name");
				out.put(mid, nameObj == null ? "" : String.valueOf(nameObj));
			}
		}
		return out;
	}

	private Map<Long, String> loadSupplierNames(Set<Long> supplierIds) {
		Map<Long, String> out = new HashMap<>();
		if (supplierIds.isEmpty()) {
			return out;
		}
		List<Supplier> list =
				supplierMapper.selectList(new LambdaQueryWrapper<Supplier>().in(Supplier::getId, supplierIds));
		for (Supplier sup : list) {
			if (sup.getId() != null) {
				out.put(sup.getId(), sup.getSupplierName() == null ? "" : sup.getSupplierName());
			}
		}
		return out;
	}

	private static Long longFromCell(Object cell) {
		if (cell == null) {
			return null;
		}
		if (cell instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(cell).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
