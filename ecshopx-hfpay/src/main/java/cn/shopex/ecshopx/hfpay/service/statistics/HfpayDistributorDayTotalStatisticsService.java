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

import cn.shopex.ecshopx.hfpay.domain.HfpayDistributorStatisticsDay;
import cn.shopex.ecshopx.hfpay.domain.HfpayEnterapply;
import cn.shopex.ecshopx.hfpay.mapper.HfpayDistributorStatisticsDayMapper;
import cn.shopex.ecshopx.hfpay.mapper.HfpayEnterapplyMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HfpayDistributorDayTotalStatisticsService {

	private final HfpayEnterapplyMapper enterapplyMapper;
	private final HfpayDistributorStatisticsDayMapper distributorStatisticsDayMapper;
	private final HfpayAcouQry001BalanceFenService acouQry001BalanceFenService;
	private final HfpayStatisticsOrderMetricsService orderMetricsService;
	private final ZoneId businessZoneId;

	public HfpayDistributorDayTotalStatisticsService(
			HfpayEnterapplyMapper enterapplyMapper,
			HfpayDistributorStatisticsDayMapper distributorStatisticsDayMapper,
			HfpayAcouQry001BalanceFenService acouQry001BalanceFenService,
			HfpayStatisticsOrderMetricsService orderMetricsService,
			@Value("${ecshopx.hfpay.business-zone-id:}") String businessZoneIdProp) {
		this.enterapplyMapper = enterapplyMapper;
		this.distributorStatisticsDayMapper = distributorStatisticsDayMapper;
		this.acouQry001BalanceFenService = acouQry001BalanceFenService;
		this.orderMetricsService = orderMetricsService;
		this.businessZoneId =
				StringUtils.hasText(businessZoneIdProp) ? ZoneId.of(businessZoneIdProp.trim()) : ZoneId.systemDefault();
	}

	/** Aggregates distributor totals from persisted day statistics and same-day order metrics. */
	public Map<String, Object> countTotals(long companyId, Long distributorId) {
		if (distributorId == null || distributorId == 0L) {
			return zeroTotalsMap();
		}
		long dist = distributorId.longValue();

		HfpayEnterapply enter = loadEnterapply(companyId, dist);
		long withdrawalFen = 0L;
		if (enter != null
				&& StringUtils.hasText(enter.getUserCustId())
				&& StringUtils.hasText(enter.getAcctId())) {
			withdrawalFen =
					acouQry001BalanceFenService.queryBalanceFenOrZero(companyId, enter.getUserCustId(), enter.getAcctId());
		}

		HfpayDistributorStatisticsDay statRow = loadLatestStatisticsRow(companyId, distributorId.intValue());
		long statIncomeFen = intFieldToFen(statRow != null ? statRow.getIncome() : null);
		long statRefundFen = intFieldToFen(statRow != null ? statRow.getRefund() : null);
		long statUnsettledFen = intFieldToFen(statRow != null ? statRow.getUnsettledFunds() : null);
		long statSettlementFen = intFieldToFen(statRow != null ? statRow.getSettlementFunds() : null);

		long todayStart = LocalDate.now(businessZoneId).atStartOfDay(businessZoneId).toEpochSecond();
		String hfOrderYmd = LocalDate.now(businessZoneId).format(DateTimeFormatter.BASIC_ISO_DATE);

		long settlementFen = orderMetricsService.sumProfitShareCapitalForDistributorFen(companyId, dist, hfOrderYmd);
		long todayIncomeNetFen = orderMetricsService.distributorTodayIncomeNetFenAfterSplit(companyId, dist, todayStart);
		long todayRefundFen = orderMetricsService.distributorTodayRefundSuccessFenAfterSplit(companyId, dist, todayStart);
		long refundingFen = orderMetricsService.distributorRefundingFenAfterSplit(companyId, dist);

		long incomeFen = statIncomeFen + todayIncomeNetFen - refundingFen;
		long refundFenTotal = statRefundFen + todayRefundFen;
		long unsettledFen = statUnsettledFen + todayIncomeNetFen - settlementFen;
		long settlementFundsFen = statSettlementFen + settlementFen;

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("income", fenToYuanPlain(incomeFen));
		out.put("refund", fenToYuanPlain(refundFenTotal));
		out.put("withdrawal_balance", fenToYuanPlain(withdrawalFen));
		out.put("unsettled_funds", fenToYuanPlain(unsettledFen));
		out.put("settlement_funds", fenToYuanPlain(settlementFundsFen));
		return out;
	}

	private static Map<String, Object> zeroTotalsMap() {
		String z = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP).toPlainString();
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("income", z);
		m.put("refund", z);
		m.put("withdrawal_balance", z);
		m.put("unsettled_funds", z);
		m.put("settlement_funds", z);
		return m;
	}

	private HfpayEnterapply loadEnterapply(long companyId, long distributorId) {
		LambdaQueryWrapper<HfpayEnterapply> w = new LambdaQueryWrapper<>();
		w.eq(HfpayEnterapply::getCompanyId, companyId)
				.eq(HfpayEnterapply::getDistributorId, distributorId)
				.eq(HfpayEnterapply::getStatus, "3")
				.in(HfpayEnterapply::getApplyType, "1", "2");
		return enterapplyMapper.selectOne(w);
	}

	private HfpayDistributorStatisticsDay loadLatestStatisticsRow(long companyId, int distributorId) {
		LambdaQueryWrapper<HfpayDistributorStatisticsDay> w = new LambdaQueryWrapper<>();
		w.apply("company_id = {0}", companyId)
				.eq(HfpayDistributorStatisticsDay::getDistributorId, distributorId)
				.orderByDesc(HfpayDistributorStatisticsDay::getCreatedAt)
				.last("LIMIT 1");
		return distributorStatisticsDayMapper.selectOne(w);
	}

	private static long intFieldToFen(Integer v) {
		return v == null ? 0L : v.longValue();
	}

	private static String fenToYuanPlain(long fen) {
		return BigDecimal.valueOf(fen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).toPlainString();
	}
}
