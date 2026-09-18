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

package cn.shopex.ecshopx.openapi.thirdapi.v2.member;

import cn.shopex.ecshopx.members.openapi.thirdapi.v2.OpenapiThirdApiV2MemberTagListService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.service.orderexport.support.NormalOrderExportDistributorLookupService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiThirdApiV2MemberOrderListService {

	private static final DateTimeFormatter DATETIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private final OpenapiMemberOrderV2ListFilterBuilder filterBuilder;
	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrderExportDistributorLookupService distributorLookupService;

	public OpenapiThirdApiV2MemberOrderListService(
			OpenapiMemberOrderV2ListFilterBuilder filterBuilder,
			NormalOrdersMapper normalOrdersMapper,
			NormalOrderExportDistributorLookupService distributorLookupService) {
		this.filterBuilder = filterBuilder;
		this.normalOrdersMapper = normalOrdersMapper;
		this.distributorLookupService = distributorLookupService;
	}

	public Map<String, Object> executeOpenapiList(
			long companyId,
			boolean mobilePresent,
			String mobileRaw,
			boolean startDatePresent,
			String startDateRaw,
			boolean endDatePresent,
			String endDateRaw,
			int page,
			int pageSize) {
		OpenapiMemberOrderV2ListFilterBuilder.FilterSpec spec =
				filterBuilder.build(
						companyId,
						mobilePresent,
						mobileRaw,
						startDatePresent,
						startDateRaw,
						endDatePresent,
						endDateRaw);

		LambdaQueryWrapper<NormalOrders> base =
				new LambdaQueryWrapper<NormalOrders>()
						.eq(NormalOrders::getCompanyId, spec.companyId())
						.ge(NormalOrders::getCreateTime, spec.createTimeGte());
		if (spec.createTimeLte() != null) {
			base.le(NormalOrders::getCreateTime, spec.createTimeLte());
		}
		if (spec.userId() != null) {
			base.eq(NormalOrders::getUserId, spec.userId());
		}

		long totalCount = normalOrdersMapper.selectCount(base);

		LambdaQueryWrapper<NormalOrders> listWrapper =
				base.clone()
						.select(
								NormalOrders::getOrderId,
								NormalOrders::getCreateTime,
								NormalOrders::getTotalFee,
								NormalOrders::getOrderClass,
								NormalOrders::getOrderType,
								NormalOrders::getType,
								NormalOrders::getOrderStatus,
								NormalOrders::getZitiStatus,
								NormalOrders::getCancelStatus,
								NormalOrders::getPayStatus,
								NormalOrders::getAuditStatus,
								NormalOrders::getDistributorId)
						.orderByDesc(NormalOrders::getOrderId);

		Page<NormalOrders> pageReq = new Page<>(page, pageSize, false);
		normalOrdersMapper.selectPage(pageReq, listWrapper);
		List<NormalOrders> entities = pageReq.getRecords();

		List<Map<String, Object>> list = List.of();
		if (entities != null && !entities.isEmpty()) {
			list = formatOrderRows(entities);
			appendDistributorNames(companyId, list);
		}

		return OpenapiThirdApiV2MemberTagListService.formatListStruct(
				totalCount, list, page, pageSize);
	}

	private List<Map<String, Object>> formatOrderRows(List<NormalOrders> entities) {
		List<Map<String, Object>> rows = new ArrayList<>(entities.size());
		for (NormalOrders entity : entities) {
			LinkedHashMap<String, Object> row = new LinkedHashMap<>();
			row.put("order_id", String.valueOf(entity.getOrderId()));
			row.put("create_time", formatEpochSeconds(entity.getCreateTime()));
			row.put("total_fee", centsToYuanString(parseCents(entity.getTotalFee())));
			row.put("order_class", entity.getOrderClass());
			row.put("order_type", entity.getOrderType());
			row.put("type", entity.getType() != null ? entity.getType() : 0);
			row.put("order_status", entity.getOrderStatus());
			row.put("ziti_status", entity.getZitiStatus());
			row.put("cancel_status", entity.getCancelStatus());
			row.put("pay_status", entity.getPayStatus());
			row.put("audit_status", entity.getAuditStatus());
			row.put("distributor_id", entity.getDistributorId() != null ? entity.getDistributorId().intValue() : 0);
			rows.add(row);
		}
		return rows;
	}

	private void appendDistributorNames(long companyId, List<Map<String, Object>> list) {
		List<Long> ids =
				list.stream()
						.map(row -> longOrZero(row.get("distributor_id")))
						.filter(id -> id > 0L)
						.distinct()
						.toList();
		Map<Long, NormalOrderExportDistributorLookupService.StoreInfo> stores =
				distributorLookupService.loadStores(companyId, ids);
		for (Map<String, Object> row : list) {
			long did = longOrZero(row.get("distributor_id"));
			String name = stores.containsKey(did) ? stores.get(did).name() : "";
			row.put("distributor_name", name);
		}
	}

	private static String formatEpochSeconds(Integer epochSeconds) {
		if (epochSeconds == null || epochSeconds <= 0) {
			return "";
		}
		return DATETIME_FMT.format(Instant.ofEpochSecond(epochSeconds.longValue()));
	}

	private static long parseCents(String raw) {
		if (raw == null || raw.isBlank()) {
			return 0L;
		}
		try {
			if (raw.contains(".")) {
				return new BigDecimal(raw.trim()).movePointRight(2).longValue();
			}
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String centsToYuanString(long cents) {
		return BigDecimal.valueOf(cents)
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
				.toPlainString();
	}

	private static long longOrZero(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(raw).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
