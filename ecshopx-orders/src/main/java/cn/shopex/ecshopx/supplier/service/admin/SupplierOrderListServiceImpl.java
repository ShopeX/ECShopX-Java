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

package cn.shopex.ecshopx.supplier.service.admin;

import cn.shopex.ecshopx.companys.service.setting.TradeCancelSettingRedisService;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.orders.port.AdminOrderDetailDistributionSupportPort;
import cn.shopex.ecshopx.orders.domain.CancelOrders;
import cn.shopex.ecshopx.orders.mapper.CancelOrdersMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderCanApplyCancelResolver;
import cn.shopex.ecshopx.common.web.locale.RequestLangTag;
import cn.shopex.ecshopx.orders.domain.DistributionDistributorPeek;
import cn.shopex.ecshopx.orders.mapper.DistributionDistributorPeekMapper;
import cn.shopex.ecshopx.orders.service.admin.SupplierOrderListItemsAssembler;
import cn.shopex.ecshopx.orders.service.supplier.SupplierOrderListFilterBuilder;
import cn.shopex.ecshopx.orders.service.supplier.SupplierOrderListFilterBuilder.BuiltSupplierOrderFilter;
import cn.shopex.ecshopx.orders.service.supplier.SupplierOrderListRowMaps;
import cn.shopex.ecshopx.orders.service.supplier.SupplierOrderStatusMessageService;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class SupplierOrderListServiceImpl implements SupplierOrderListService {

	private final SupplierOrderMapper supplierOrderMapper;
	private final SupplierOrderListFilterBuilder supplierOrderListFilterBuilder;
	private final DistributionDistributorPeekMapper distributionDistributorPeekMapper;
	private final AdminOrderDetailDistributionSupportPort adminOrderDetailDistributionSupportPort;
	private final SupplierOrderListItemsAssembler supplierOrderListItemsAssembler;
	private final SupplierOrderStatusMessageService supplierOrderStatusMessageService;
	private final LangueProperties langueProperties;
	private final CancelOrdersMapper cancelOrdersMapper;
	private final TradeCancelSettingRedisService tradeCancelSettingRedisService;

	public SupplierOrderListServiceImpl(
			SupplierOrderMapper supplierOrderMapper,
			SupplierOrderListFilterBuilder supplierOrderListFilterBuilder,
			DistributionDistributorPeekMapper distributionDistributorPeekMapper,
			AdminOrderDetailDistributionSupportPort adminOrderDetailDistributionSupportPort,
			SupplierOrderListItemsAssembler supplierOrderListItemsAssembler,
			SupplierOrderStatusMessageService supplierOrderStatusMessageService,
			LangueProperties langueProperties,
			CancelOrdersMapper cancelOrdersMapper,
			TradeCancelSettingRedisService tradeCancelSettingRedisService) {
		this.supplierOrderMapper = supplierOrderMapper;
		this.supplierOrderListFilterBuilder = supplierOrderListFilterBuilder;
		this.distributionDistributorPeekMapper = distributionDistributorPeekMapper;
		this.adminOrderDetailDistributionSupportPort = adminOrderDetailDistributionSupportPort;
		this.supplierOrderListItemsAssembler = supplierOrderListItemsAssembler;
		this.supplierOrderStatusMessageService = supplierOrderStatusMessageService;
		this.langueProperties = langueProperties;
		this.cancelOrdersMapper = cancelOrdersMapper;
		this.tradeCancelSettingRedisService = tradeCancelSettingRedisService;
	}

	@Override
	public Map<String, Object> getOrderList(long companyId, long supplierId, HttpServletRequest request) {
		int page = Math.max(1, intParam(request.getParameter("page"), 1));
		int pageSize = Math.max(1, intParam(request.getParameter("pageSize"), 10));
		BuiltSupplierOrderFilter built =
				supplierOrderListFilterBuilder.buildQueryAndEchoFilter(companyId, supplierId, request);
		LambdaQueryWrapper<SupplierOrder> queryWrapper = built.wrapper().orderByDesc(SupplierOrder::getId);
		Map<String, Object> echoFilter = built.echoFilter();
		Page<SupplierOrder> p = new Page<>(page, pageSize);
		supplierOrderMapper.selectPage(p, queryWrapper);
		Map<String, Object> root = new LinkedHashMap<>();
		root.put("total_count", p.getTotal());
		List<Map<String, Object>> list = new ArrayList<>();
		root.put("list", list);
		List<SupplierOrder> records = p.getRecords();
		if (records == null || records.isEmpty()) {
			putDatapassAndFilter(request, root, echoFilter);
			return root;
		}
		Set<Long> storeIds = new LinkedHashSet<>();
		for (SupplierOrder r : records) {
			Long did = r.getDistributorId();
			if (did != null && did >= 0L) {
				storeIds.add(did);
			}
		}
		Map<Long, Map<String, Object>> storeRowById = new LinkedHashMap<>();
		if (!storeIds.isEmpty()) {
			List<DistributionDistributorPeek> storeRows =
					distributionDistributorPeekMapper.selectList(
							new LambdaQueryWrapper<DistributionDistributorPeek>()
									.eq(DistributionDistributorPeek::getCompanyId, companyId)
									.in(DistributionDistributorPeek::getDistributorId, storeIds)
									.last("LIMIT " + pageSize));
			for (DistributionDistributorPeek peek : storeRows) {
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("name", peek.getName());
				storeRowById.put(peek.getDistributorId(), row);
			}
			Map<String, Object> selfInfo =
					adminOrderDetailDistributionSupportPort.getDistributorSelfSimpleInfo(companyId);
			Map<String, Object> selfMap = new LinkedHashMap<>();
			selfMap.put("name", Objects.toString(selfInfo.get("name"), ""));
			storeRowById.put(0L, selfMap);
		}
		List<Long> orderIds = records.stream().map(SupplierOrder::getOrderId).toList();
		int supplierIdInt = (int) supplierId;
		String langTag = RequestLangTag.current(langueProperties);
		Map<Long, List<Map<String, Object>>> itemsByOrderId =
				supplierOrderListItemsAssembler.loadItemsGroupedByOrderId(companyId, supplierIdInt, orderIds, langTag);
		Map<Long, CancelOrders> cancelByOrder = loadCancelByOrder(companyId, orderIds);
		boolean repeatCancel =
				Boolean.TRUE.equals(tradeCancelSettingRedisService.getCancelSetting(companyId).get("repeat_cancel"));
		for (SupplierOrder entity : records) {
			Map<String, Object> row = SupplierOrderListRowMaps.toRow(entity);
			row.putIfAbsent("prescription_status", 0);
			row.putIfAbsent("diagnosis_data", null);
			row.put("order_status_msg", supplierOrderStatusMessageService.getOrderStatusMsgForSupplier(row));
			row.put("items", itemsByOrderId.getOrDefault(entity.getOrderId(), List.of()));
			AdminOrderCanApplyCancelResolver.apply(row, cancelByOrder.get(entity.getOrderId()), repeatCancel);
			Long did = entity.getDistributorId();
			String distributorName = "";
			if (did != null) {
				distributorName =
						Optional.ofNullable(storeRowById.get(did)).map(m -> Objects.toString(m.get("name"), "")).orElse("");
			}
			row.put("distributor_name", distributorName);
			list.add(row);
		}
		putDatapassAndFilter(request, root, echoFilter);
		return root;
	}

	private Map<Long, CancelOrders> loadCancelByOrder(long companyId, List<Long> orderIds) {
		if (orderIds == null || orderIds.isEmpty()) {
			return Map.of();
		}
		List<CancelOrders> rows =
				cancelOrdersMapper.selectList(
						new LambdaQueryWrapper<CancelOrders>()
								.eq(CancelOrders::getCompanyId, companyId)
								.in(CancelOrders::getOrderId, orderIds));
		Map<Long, CancelOrders> byOrder = new LinkedHashMap<>();
		for (CancelOrders row : rows) {
			if (row.getOrderId() != null) {
				byOrder.put(row.getOrderId(), row);
			}
		}
		return byOrder;
	}

	private static void putDatapassAndFilter(HttpServletRequest request, Map<String, Object> root, Map<String, Object> echoFilter) {
		Object v = request.getHeader("x-datapass-block");
		if (v == null) {
			v = request.getParameter("x-datapass-block");
		}
		if (v == null) {
			v = 0;
		}
		root.put("datapass_block", v);
		root.put("filter", echoFilter);
	}

	private static int intParam(String raw, int defaultVal) {
		if (raw == null || raw.isBlank()) {
			return defaultVal;
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}
}
