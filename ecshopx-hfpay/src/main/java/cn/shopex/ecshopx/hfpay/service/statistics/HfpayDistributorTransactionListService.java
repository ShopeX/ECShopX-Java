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

import cn.shopex.ecshopx.hfpay.domain.HfpayEnterapply;
import cn.shopex.ecshopx.hfpay.mapper.HfpayEnterapplyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HfpayDistributorTransactionListService {

	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final HfpayEnterapplyMapper enterapplyMapper;
	private final HfpayAcouQry001BalanceFenService acouQry001BalanceFenService;
	private final HfpayStatisticsOrderMetricsService orderMetricsService;
	private final ZoneId businessZoneId;

	public HfpayDistributorTransactionListService(
			HfpayEnterapplyMapper enterapplyMapper,
			HfpayAcouQry001BalanceFenService acouQry001BalanceFenService,
			HfpayStatisticsOrderMetricsService orderMetricsService,
			@Value("${ecshopx.hfpay.business-zone-id:}") String businessZoneIdProp) {
		this.enterapplyMapper = enterapplyMapper;
		this.acouQry001BalanceFenService = acouQry001BalanceFenService;
		this.orderMetricsService = orderMetricsService;
		this.businessZoneId =
				StringUtils.hasText(businessZoneIdProp) ? ZoneId.of(businessZoneIdProp.trim()) : ZoneId.systemDefault();
	}

	public Map<String, Object> transactionList(
			long companyId,
			String startDateTime,
			String endDateTime,
			Integer distributorId,
			int page,
			int pageSize) {
		long startUnix = LocalDateTime.parse(startDateTime, DATE_TIME).atZone(businessZoneId).toEpochSecond();
		long endUnix = LocalDateTime.parse(endDateTime, DATE_TIME).atZone(businessZoneId).toEpochSecond();

		Long distFilter = distributorId != null && distributorId != 0 ? distributorId.longValue() : null;

		long totalCount = enterapplyMapper.countStatisticsEnterapplyList(companyId, distFilter);

		int offset = (page - 1) * pageSize;
		List<Map<String, Object>> rawList =
				enterapplyMapper.selectStatisticsEnterapplyList(companyId, distFilter, offset, pageSize);

		if (rawList == null || rawList.isEmpty()) {
			Map<String, Object> empty = new LinkedHashMap<>();
			empty.put("total_count", 0L);
			empty.put("list", Collections.emptyList());
			return empty;
		}

		List<Map<String, Object>> list = new ArrayList<>();
		for (Map<String, Object> row : rawList) {
			long cid = toLong(row.get("company_id"));
			long did = toLong(row.get("distributor_id"));

			Map<String, Object> item = new LinkedHashMap<>();
			item.put("company_id", cid);
			item.put("distributor_id", did);
			item.put("distributor_name", distributorNameFromRow(row.get("distributor_name")));

			item.put("order_count", 0);
			item.put("order_total_fee", "0");
			item.put("order_refund_count", 0);
			item.put("order_refund_total_fee", "0");
			item.put("order_refunding_count", 0);
			item.put("order_refunding_total_fee", "0");
			item.put("order_profit_sharing_charge", "0");
			item.put("order_un_profit_sharing_charge", "0");

			HfpayEnterapply enter = loadEnterapplyRow(cid, did);
			long withdrawalFen = 0L;
			if (enter != null
					&& StringUtils.hasText(enter.getUserCustId())
					&& StringUtils.hasText(enter.getAcctId())) {
				withdrawalFen =
						acouQry001BalanceFenService.queryBalanceFenOrZero(cid, enter.getUserCustId(), enter.getAcctId());
			}
			item.put("withdrawal_balance", withdrawalFen);

			Map<String, Object> stats =
					orderMetricsService.count(cid, did, startUnix, endUnix);
			applyStatsToItem(item, stats);

			list.add(item);
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total_count", totalCount);
		result.put("list", list);
		return result;
	}

	private static String distributorNameFromRow(Object raw) {
		if (raw == null) {
			return "";
		}
		String s = String.valueOf(raw).trim();
		return StringUtils.hasText(s) ? s : "";
	}

	private HfpayEnterapply loadEnterapplyRow(long companyId, long distributorId) {
		LambdaQueryWrapper<HfpayEnterapply> w = new LambdaQueryWrapper<>();
		w.eq(HfpayEnterapply::getCompanyId, companyId)
				.eq(HfpayEnterapply::getDistributorId, distributorId)
				.eq(HfpayEnterapply::getStatus, "3")
				.in(HfpayEnterapply::getApplyType, "1", "2");
		return enterapplyMapper.selectOne(w);
	}

	private void applyStatsToItem(Map<String, Object> item, Map<String, Object> stats) {
		item.put("order_count", toInt(stats.get("order_count")));
		item.put("order_total_fee", yuanToFenString(stats.get("order_total_fee")));
		item.put("order_refund_count", toInt(stats.get("order_refund_count")));
		item.put("order_refund_total_fee", yuanToFenString(stats.get("order_refund_total_fee")));
		item.put("order_refunding_count", toInt(stats.get("order_refunding_count")));
		item.put("order_refunding_total_fee", yuanToFenString(stats.get("order_refunding_total_fee")));
		item.put("order_profit_sharing_charge", yuanToFenString(stats.get("order_profit_sharing_charge")));
		item.put("order_un_profit_sharing_charge", yuanToFenString(stats.get("order_un_profit_sharing_charge")));
	}

	private static int toInt(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		return 0;
	}

	private static long toLong(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String yuanToFenString(Object yuanObj) {
		if (yuanObj == null) {
			return "0";
		}
		try {
			BigDecimal yuan = new BigDecimal(String.valueOf(yuanObj).trim());
			return yuan.movePointRight(2).toPlainString();
		} catch (Exception e) {
			return "0";
		}
	}
}
