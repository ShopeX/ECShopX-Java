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

package cn.shopex.ecshopx.hfpay.service.statistics;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.hfpay.mapper.HfpayStatisticsOrderListMapper;
import cn.shopex.ecshopx.hfpay.mapper.dto.HfpayStatisticsOrderListParams;
import cn.shopex.ecshopx.hfpay.mapper.dto.HfpayStatisticsRefundPartitionParams;
import cn.shopex.ecshopx.hfpay.service.export.HfpayOrderRecordExportContext;
import cn.shopex.ecshopx.orders.config.OrderAppPayTypeDescHolder;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HfpayStatisticsOrderListService {

	private static final Set<String> REFUNDFAIL_INTERSECT =
			Set.of("READY", "AUDIT_SUCCESS", "SUCCESS", "REFUSE", "CANCEL", "REFUNDCLOSE", "PROCESSING");
	private static final Set<String> PAY_INTERSECT =
			Set.of("READY", "AUDIT_SUCCESS", "SUCCESS", "PROCESSING", "CHANGE");

	private final HfpayStatisticsOrderListMapper orderListMapper;
	private final AftersalesRefundMapper aftersalesRefundMapper;
	private final OrderAppPayTypeDescHolder orderAppPayTypeDescHolder;

	public HfpayStatisticsOrderListService(
			HfpayStatisticsOrderListMapper orderListMapper,
			AftersalesRefundMapper aftersalesRefundMapper,
			OrderAppPayTypeDescHolder orderAppPayTypeDescHolder) {
		this.orderListMapper = orderListMapper;
		this.aftersalesRefundMapper = aftersalesRefundMapper;
		this.orderAppPayTypeDescHolder = orderAppPayTypeDescHolder;
	}

	public long countOrders(HfpayOrderRecordExportContext ctx) {
		HfpayStatisticsOrderListParams p = buildParams(ctx);
		if (p == null) {
			return 0L;
		}
		return orderListMapper.countOrders(p);
	}

	public List<Map<String, Object>> pageOrders(HfpayOrderRecordExportContext ctx, int page, int pageSize) {
		HfpayStatisticsOrderListParams p = buildParams(ctx);
		if (p == null) {
			return List.of();
		}
		int offset = Math.max(0, (page - 1) * pageSize);
		return orderListMapper.pageOrders(p, offset, pageSize);
	}

	/**
	 * @return null when refund-fail filter matches no orders (count/list must be empty).
	 */
	private HfpayStatisticsOrderListParams buildParams(HfpayOrderRecordExportContext ctx) {
		String rawStatus = ctx.getOrderStatus();
		String statusKind = "NONE";
		if (StringUtils.hasText(rawStatus)) {
			statusKind =
					switch (rawStatus.trim()) {
						case "refunding" -> "REFUNDING";
						case "refundsuccess" -> "REFUND_SUCCESS";
						case "refundfail" -> "REFUND_FAIL";
						case "pay" -> "PAY";
						default -> "NONE";
					};
		}

		HfpayStatisticsRefundPartitionParams partitionFilter = buildRefundPartitionParams(ctx);

		List<String> partitionIds = List.of();
		if ("REFUND_FAIL".equals(statusKind)) {
			partitionIds = computeRefundFailOrderIds(partitionFilter);
			if (partitionIds.isEmpty()) {
				return null;
			}
		} else if ("PAY".equals(statusKind)) {
			partitionIds = computePayOrderIds(partitionFilter);
		}

		HfpayStatisticsOrderListParams.HfpayStatisticsOrderListParamsBuilder b =
				HfpayStatisticsOrderListParams.builder()
						.companyId(ctx.getCompanyId())
						.startUnix(ctx.getStartDateEpochSec())
						.endUnix(ctx.getEndDateEpochSec())
						.distributorId(ctx.getDistributorId())
						.narrowOrderId(narrowOrderId(ctx.getOrderId()))
						.appPayType(StringUtils.hasText(ctx.getAppPayType()) ? ctx.getAppPayType().trim() : null)
						.profitsharingStatus(ctx.getProfitsharingStatus())
						.statusKind(statusKind)
						.partitionIds(partitionIds);

		return b.build();
	}

	private static String narrowOrderId(String orderId) {
		if (!StringUtils.hasText(orderId)) {
			return null;
		}
		String t = orderId.trim();
		if ("0".equals(t)) {
			return null;
		}
		return t;
	}

	private HfpayStatisticsRefundPartitionParams buildRefundPartitionParams(HfpayOrderRecordExportContext ctx) {
		return HfpayStatisticsRefundPartitionParams.builder()
				.companyId(ctx.getCompanyId())
				.startUnix(ctx.getStartDateEpochSec())
				.endUnix(ctx.getEndDateEpochSec())
				.distributorId(ctx.getDistributorId())
				.narrowOrderId(narrowOrderId(ctx.getOrderId()))
				.appPayType(StringUtils.hasText(ctx.getAppPayType()) ? ctx.getAppPayType().trim() : null)
				.profitsharingStatus(ctx.getProfitsharingStatus())
				.build();
	}

	private List<String> computeRefundFailOrderIds(HfpayStatisticsRefundPartitionParams partitionFilter) {
		return computePartitionOrderIds(partitionFilter, REFUNDFAIL_INTERSECT);
	}

	private List<String> computePayOrderIds(HfpayStatisticsRefundPartitionParams partitionFilter) {
		return computePartitionOrderIds(partitionFilter, PAY_INTERSECT);
	}

	private List<String> computePartitionOrderIds(
			HfpayStatisticsRefundPartitionParams partitionFilter, Set<String> blockingStatuses) {
		List<Map<String, Object>> rows = orderListMapper.selectRefundStatusesForPartitionScan(partitionFilter);
		Map<String, Set<String>> byOrder = new HashMap<>();
		for (Map<String, Object> row : rows) {
			Object oid = row.get("order_id");
			Object st = row.get("refund_status");
			if (oid == null) {
				continue;
			}
			String orderKey = String.valueOf(oid);
			String status = st == null ? "" : String.valueOf(st);
			byOrder.computeIfAbsent(orderKey, k -> new HashSet<>()).add(status);
		}
		List<String> out = new ArrayList<>();
		for (Map.Entry<String, Set<String>> e : byOrder.entrySet()) {
			Set<String> s = e.getValue();
			boolean hit = false;
			for (String x : blockingStatuses) {
				if (s.contains(x)) {
					hit = true;
					break;
				}
			}
			if (!hit) {
				out.add(e.getKey());
			}
		}
		return out;
	}

	/**
	 * Row enrich identical to list export: refund-derived status, {@link HfpayStatisticsOrderDetailService}
	 * handlers, refund fee sum.
	 */
	public void enrichListRow(long companyId, Map<String, Object> row) {
		if (row == null || row.isEmpty()) {
			return;
		}
		Object orderIdObj = row.get("order_id");
		if (orderIdObj == null) {
			return;
		}
		String orderId = String.valueOf(orderIdObj);

		QueryWrapper<AftersalesRefund> refundListWrapper = new QueryWrapper<>();
		refundListWrapper
				.eq("company_id", companyId)
				.eq("order_id", orderId)
				.notIn("refund_status", "CANCEL");
		List<AftersalesRefund> refundRows = aftersalesRefundMapper.selectList(refundListWrapper);

		String refundStatus = HfpayStatisticsOrderDetailService.computeRefundStatus(refundRows);
		if (StringUtils.hasText(refundStatus)) {
			row.put("order_status", refundStatus);
		}

		HfpayStatisticsOrderDetailService.handelOrderStatus(row);

		long refundFeeSum = sumRefundFeeForExport(companyId, orderId);
		row.put("refund_fee", refundFeeSum);

		appendAppPayTypeDesc(row);
	}

	private void appendAppPayTypeDesc(Map<String, Object> row) {
		Object raw = row.get("app_pay_type");
		String key = raw == null ? "" : String.valueOf(raw).trim();
		String desc = orderAppPayTypeDescHolder.descForAppPayTypeOrNull(key);
		row.put("app_pay_type_desc", desc != null ? desc : "");
	}

	private long sumRefundFeeForExport(long companyId, String orderId) {
		QueryWrapper<AftersalesRefund> w = new QueryWrapper<>();
		w.select("IFNULL(SUM(refund_fee), 0) AS s");
		w.eq("company_id", companyId)
				.eq("order_id", orderId)
				.notIn("refund_status", "CANCEL", "REFUSE", "REFUNDCLOSE", "CHANGE");
		List<Map<String, Object>> rows = aftersalesRefundMapper.selectMaps(w);
		if (rows == null || rows.isEmpty()) {
			return 0L;
		}
		Map<String, Object> r = rows.get(0);
		Object v = r.get("s");
		if (v == null && r.size() == 1) {
			v = r.values().iterator().next();
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (Exception e) {
			return 0L;
		}
	}
}
