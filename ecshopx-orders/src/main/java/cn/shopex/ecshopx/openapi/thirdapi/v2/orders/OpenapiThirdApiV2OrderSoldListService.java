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

package cn.shopex.ecshopx.openapi.thirdapi.v2.orders;

import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.DistributionDistributorPeekMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.orderexport.support.NormalOrderExportDistributorLookupService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OpenapiThirdApiV2OrderSoldListService {

	private static final DateTimeFormatter DATE_TIME_FMT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private final MemberAccountService memberAccountService;
	private final DistributionDistributorPeekMapper distributionDistributorPeekMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final NormalOrderExportDistributorLookupService distributorLookupService;
	private final TradeMapper tradeMapper;
	private final OpenapiThirdApiV2OrderListFormatSupport formatSupport;

	public OpenapiThirdApiV2OrderSoldListService(
			MemberAccountService memberAccountService,
			DistributionDistributorPeekMapper distributionDistributorPeekMapper,
			NormalOrdersMapper normalOrdersMapper,
			NormalOrderExportDistributorLookupService distributorLookupService,
			TradeMapper tradeMapper,
			OpenapiThirdApiV2OrderListFormatSupport formatSupport) {
		this.memberAccountService = memberAccountService;
		this.distributionDistributorPeekMapper = distributionDistributorPeekMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.distributorLookupService = distributorLookupService;
		this.tradeMapper = tradeMapper;
		this.formatSupport = formatSupport;
	}

	public Object execute(
			long companyId,
			int page,
			int pageSize,
			boolean mobileTruthy,
			String mobileRaw,
			boolean timeBeginTruthy,
			String timeBeginRaw,
			boolean timeEndTruthy,
			String timeEndRaw,
			boolean shopCodeTruthy,
			String shopCodeRaw,
			boolean isSelfPresent,
			String isSelfRaw,
			boolean orderStatusPresent,
			String orderStatusRaw,
			boolean payStatusPresent,
			String payStatusRaw) {
		OrderFilter filter = new OrderFilter();
		filter.companyId = companyId;

		if (mobileTruthy) {
			Members member =
					memberAccountService.findMemberByCompanyAndMobile(companyId, mobileRaw.trim());
			if (member == null) {
				return Collections.emptyList();
			}
			filter.userId = member.getUserId();
		}

		if (timeBeginTruthy) {
			filter.createTimeGte = toEpochSecondsInt(phpStrtotime(timeBeginRaw));
		}
		if (timeEndTruthy) {
			filter.createTimeLte = toEpochSecondsInt(phpStrtotime(timeEndRaw));
		}

		if (shopCodeTruthy) {
			Long shopId =
					distributionDistributorPeekMapper.selectDistributorIdByShopCode(shopCodeRaw.trim());
			filter.distributorId = shopId != null ? shopId : -1L;
		}

		if (isSelfPresent) {
			if ("true".equals(isSelfRaw)) {
				filter.distributorId = 0L;
				filter.distributorIdGt = null;
			} else {
				filter.distributorId = null;
				filter.distributorIdGt = 0L;
			}
		}

		if (orderStatusPresent) {
			filter.orderStatus = orderStatusRaw != null ? orderStatusRaw : "";
		}
		if (payStatusPresent) {
			filter.payStatus = payStatusRaw != null ? payStatusRaw : "";
		}

		LambdaQueryWrapper<NormalOrders> wrapper = buildWrapper(filter);
		long count = normalOrdersMapper.selectCount(wrapper);

		LambdaQueryWrapper<NormalOrders> listWrapper = wrapper.clone();
		long offset = (long) (page - 1) * pageSize;
		listWrapper.last("LIMIT " + pageSize + " OFFSET " + offset);
		List<NormalOrders> orders = normalOrdersMapper.selectList(listWrapper);

		List<Long> distributorIds =
				orders.stream()
						.map(NormalOrders::getDistributorId)
						.filter(id -> id != null && id > 0L)
						.distinct()
						.toList();
		Map<Long, NormalOrderExportDistributorLookupService.StoreInfo> storeMap =
				distributorLookupService.loadStores(companyId, distributorIds);

		List<Long> orderIds =
				orders.stream()
						.map(NormalOrders::getOrderId)
						.filter(id -> id != null && id > 0L)
						.toList();
		Map<Long, String> tradeIndex = buildTradeIndex(companyId, orderIds);

		return formatSupport.formatOrderListStruct(
				companyId, count, orders, storeMap, tradeIndex, page, pageSize);
	}

	private LambdaQueryWrapper<NormalOrders> buildWrapper(OrderFilter filter) {
		LambdaQueryWrapper<NormalOrders> wrapper =
				new LambdaQueryWrapper<NormalOrders>()
						.eq(NormalOrders::getCompanyId, filter.companyId)
						.eq(NormalOrders::getSupplierId, 0);

		if (filter.userId != null) {
			wrapper.eq(NormalOrders::getUserId, filter.userId);
		}
		if (filter.distributorId != null) {
			wrapper.eq(NormalOrders::getDistributorId, filter.distributorId);
		}
		if (filter.distributorIdGt != null) {
			wrapper.gt(NormalOrders::getDistributorId, filter.distributorIdGt);
		}
		if (filter.createTimeGte != null) {
			wrapper.ge(NormalOrders::getCreateTime, filter.createTimeGte);
		}
		if (filter.createTimeLte != null) {
			wrapper.le(NormalOrders::getCreateTime, filter.createTimeLte);
		}
		if (filter.orderStatus != null) {
			wrapper.eq(NormalOrders::getOrderStatus, filter.orderStatus);
		}
		if (filter.payStatus != null) {
			wrapper.eq(NormalOrders::getPayStatus, filter.payStatus);
		}

		return wrapper.orderByDesc(NormalOrders::getCreateTime);
	}

	private Map<Long, String> buildTradeIndex(long companyId, List<Long> orderIds) {
		if (orderIds.isEmpty()) {
			return Map.of();
		}
		List<Map<String, Object>> rows = tradeMapper.selectSuccessTradeIndexRows(companyId, orderIds);
		Map<Long, String> out = new LinkedHashMap<>();
		for (Map<String, Object> row : rows) {
			long oid = longVal(row.get("order_id"));
			String tn = str(row.get("trade_no"));
			out.put(oid, (StringUtils.hasText(tn) && !"0".equals(tn)) ? tn : "-");
		}
		return out;
	}

	private static int toEpochSecondsInt(long epochSeconds) {
		return (int) epochSeconds;
	}

	private static long phpStrtotime(String raw) {
		if (!StringUtils.hasText(raw)) {
			return 0L;
		}
		String t = raw.trim();
		try {
			LocalDateTime dt = LocalDateTime.parse(t, DATE_TIME_FMT);
			return dt.atZone(ZoneId.systemDefault()).toEpochSecond();
		} catch (DateTimeParseException ignored) {
			// fall through
		}
		try {
			LocalDate d = LocalDate.parse(t, DATE_FMT);
			return d.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
		} catch (DateTimeParseException ignored) {
			return 0L;
		}
	}

	private static long longVal(Object raw) {
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

	private static String str(Object raw) {
		return raw == null ? "" : String.valueOf(raw);
	}

	private static final class OrderFilter {
		private long companyId;
		private Long userId;
		private Long distributorId;
		private Long distributorIdGt;
		private Integer createTimeGte;
		private Integer createTimeLte;
		private String orderStatus;
		private String payStatus;
	}
}
