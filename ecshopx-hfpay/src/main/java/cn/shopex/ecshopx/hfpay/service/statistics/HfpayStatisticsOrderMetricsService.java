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

import cn.shopex.ecshopx.hfpay.mapper.HfpayStatisticsOrderMetricsMapper;
import cn.shopex.ecshopx.hfpay.service.export.HfpayOrderRecordExportContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class HfpayStatisticsOrderMetricsService {

	private static final BigDecimal TEN_THOUSAND = new BigDecimal("10000");

	private final HfpayStatisticsOrderMetricsMapper metricsMapper;

	public HfpayStatisticsOrderMetricsService(HfpayStatisticsOrderMetricsMapper metricsMapper) {
		this.metricsMapper = metricsMapper;
	}

	public Map<String, Object> count(
			long companyId, long distributorId, long orderFilterStartUnixSeconds, long orderFilterEndUnixSeconds) {
		Long dist = distributorId > 0 ? distributorId : null;
		Long start = orderFilterStartUnixSeconds;
		Long end = orderFilterEndUnixSeconds;

		long orderCount = metricsMapper.countPaidOrders(companyId, dist, start, end);
		Long orderTotalFeeFen = nz(metricsMapper.sumOrderTotalFees(companyId, dist, start, end));
		long orderRefundCount = metricsMapper.countDistinctOrdersRefundSuccess(companyId, dist, start, end);
		Long orderRefundTotalFeeFen = nz(metricsMapper.sumRefundedFeesSuccessOrAudit(companyId, dist, start, end));
		long orderRefundingCount = metricsMapper.countDistinctOrdersRefunding(companyId, dist, start, end);
		Long orderRefundingTotalFeeFen = nz(metricsMapper.sumRefundFeesRefunding(companyId, dist, start, end));

		BigDecimal orderProfitSharingChargeFen =
				sumSplitFeeFromRows(metricsMapper.listProfitSharingChargeRows(companyId, dist, start, end));
		BigDecimal orderTotalChargeFen = sumSplitFeeFromRows(metricsMapper.listTotalChargeRows(companyId, dist, start, end));
		BigDecimal orderRefundTotalChargeFen = sumSplitFeeFromRows(metricsMapper.listRefundChargeRows(companyId, dist, start, end));
		BigDecimal orderUnProfitSharingTotalChargeFen =
				sumSplitFeeFromRows(metricsMapper.listUnProfitChargeRows(companyId, dist, start, end));
		BigDecimal orderUnProfitSharingRefundTotalChargeFen =
				sumSplitFeeFromRows(metricsMapper.listUnProfitRefundChargeRows(companyId, dist, start, end));

		BigDecimal orderUnProfitSharingChargeFen =
				orderUnProfitSharingTotalChargeFen.subtract(orderUnProfitSharingRefundTotalChargeFen);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("order_count", orderCount);
		out.put("order_total_fee", fenToYuanPlain(orderTotalFeeFen));
		out.put("order_refund_count", orderRefundCount);
		out.put("order_refund_total_fee", fenToYuanPlain(orderRefundTotalFeeFen));
		out.put("order_refunding_count", orderRefundingCount);
		out.put("order_refunding_total_fee", fenToYuanPlain(orderRefundingTotalFeeFen));
		out.put("order_profit_sharing_charge", fenToYuanPlain(orderProfitSharingChargeFen));
		out.put("order_total_charge", fenToYuanPlain(orderTotalChargeFen));
		out.put("order_refund_total_charge", fenToYuanPlain(orderRefundTotalChargeFen));
		out.put("order_un_profit_sharing_total_charge", fenToYuanPlain(orderUnProfitSharingTotalChargeFen));
		out.put("order_un_profit_sharing_refund_total_charge", fenToYuanPlain(orderUnProfitSharingRefundTotalChargeFen));
		out.put("order_un_profit_sharing_charge", fenToYuanPlain(orderUnProfitSharingChargeFen));
		return out;
	}

	/**
	 * 订单列表页顶部汇总指标；字段与 {@link #count(long, long, long, long)} 一致。
	 * <p>在导出/列表筛选上下文（时间、商户、关键词等）下聚合；其中订单笔数与订单总金额会按请求中的
	 * {@code order_status} 进一步收窄，其余指标在同一筛选范围内统计。
	 */
	public Map<String, Object> countForOrderListStatistics(HfpayOrderRecordExportContext ctx) {
		String statusKind = resolveMetricsStatusKind(ctx.getOrderStatus());

		long orderCount = metricsMapper.countPaidOrdersFiltered(ctx, statusKind);
		Long orderTotalFeeFen = nz(metricsMapper.sumOrderTotalFeesFiltered(ctx, statusKind));
		long orderRefundCount = metricsMapper.countDistinctOrdersRefundSuccessFiltered(ctx);
		Long orderRefundTotalFeeFen = nz(metricsMapper.sumRefundedFeesSuccessOrAuditFiltered(ctx));
		long orderRefundingCount = metricsMapper.countDistinctOrdersRefundingFiltered(ctx);
		Long orderRefundingTotalFeeFen = nz(metricsMapper.sumRefundFeesRefundingFiltered(ctx));

		BigDecimal orderProfitSharingChargeFen =
				sumSplitFeeFromRows(metricsMapper.listProfitSharingChargeRowsFiltered(ctx, statusKind));
		BigDecimal orderTotalChargeFen = sumSplitFeeFromRows(metricsMapper.listTotalChargeRowsFiltered(ctx));
		BigDecimal orderRefundTotalChargeFen = sumSplitFeeFromRows(metricsMapper.listRefundChargeRowsFiltered(ctx));
		BigDecimal orderUnProfitSharingTotalChargeFen =
				sumSplitFeeFromRows(metricsMapper.listUnProfitChargeRowsFiltered(ctx));
		BigDecimal orderUnProfitSharingRefundTotalChargeFen =
				sumSplitFeeFromRows(metricsMapper.listUnProfitRefundChargeRowsFiltered(ctx));

		BigDecimal orderUnProfitSharingChargeFen =
				orderUnProfitSharingTotalChargeFen.subtract(orderUnProfitSharingRefundTotalChargeFen);

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("order_count", orderCount);
		out.put("order_total_fee", fenToYuanPlain(orderTotalFeeFen));
		out.put("order_refund_count", orderRefundCount);
		out.put("order_refund_total_fee", fenToYuanPlain(orderRefundTotalFeeFen));
		out.put("order_refunding_count", orderRefundingCount);
		out.put("order_refunding_total_fee", fenToYuanPlain(orderRefundingTotalFeeFen));
		out.put("order_profit_sharing_charge", fenToYuanPlain(orderProfitSharingChargeFen));
		out.put("order_total_charge", fenToYuanPlain(orderTotalChargeFen));
		out.put("order_refund_total_charge", fenToYuanPlain(orderRefundTotalChargeFen));
		out.put("order_un_profit_sharing_total_charge", fenToYuanPlain(orderUnProfitSharingTotalChargeFen));
		out.put("order_un_profit_sharing_refund_total_charge", fenToYuanPlain(orderUnProfitSharingRefundTotalChargeFen));
		out.put("order_un_profit_sharing_charge", fenToYuanPlain(orderUnProfitSharingChargeFen));
		return out;
	}

	private static String resolveMetricsStatusKind(String rawStatus) {
		if (!StringUtils.hasText(rawStatus)) {
			return "NONE";
		}
		return switch (rawStatus.trim()) {
			case "refunding" -> "REFUNDING";
			case "refundsuccess" -> "REFUND_SUCCESS";
			case "refundfail" -> "REFUND_FAIL";
			case "pay" -> "PAY";
			default -> "NONE";
		};
	}

	public long sumProfitShareCapitalFen(long companyId, String hfOrderDateYmd) {
		return nz(metricsMapper.sumProfitShareCapital(companyId, hfOrderDateYmd));
	}

	public long companyTodayIncomeFeeAmtFen(long companyId, long todayStartUnixSeconds) {
		return sumSplitFeeFromRows(metricsMapper.listCompanyIncomeDayRows(companyId, todayStartUnixSeconds)).longValue();
	}

	public long companyTodayRefundFeeAmtFen(long companyId, long todayStartUnixSeconds) {
		return sumSplitFeeFromRows(metricsMapper.listCompanyRefundDayRows(companyId, todayStartUnixSeconds)).longValue();
	}

	public long distributorTodayIncomeNetFenAfterSplit(
			long companyId, long distributorId, long todayStartUnixSeconds) {
		BigDecimal gross =
				sumSplitFeeFromRows(
						metricsMapper.listDistributorIncomeDayRows(companyId, distributorId, todayStartUnixSeconds));
		BigDecimal todayRefund =
				sumSplitFeeFromRows(
						metricsMapper.listDistributorRefundSuccessDayRows(
								companyId, distributorId, todayStartUnixSeconds));
		return gross.subtract(todayRefund).longValue();
	}

	public long distributorTodayRefundSuccessFenAfterSplit(
			long companyId, long distributorId, long todayStartUnixSeconds) {
		return sumSplitFeeFromRows(
						metricsMapper.listDistributorRefundSuccessDayRows(
								companyId, distributorId, todayStartUnixSeconds))
				.longValue();
	}

	public long distributorRefundingFenAfterSplit(long companyId, long distributorId) {
		return sumSplitFeeFromRows(metricsMapper.listDistributorRefundingDayRows(companyId, distributorId))
				.longValue();
	}

	public long sumProfitShareCapitalForDistributorFen(
			long companyId, long distributorId, String hfOrderDateYmd) {
		return nz(metricsMapper.sumProfitShareCapitalForDistributor(companyId, distributorId, hfOrderDateYmd));
	}

	/**
	 * 已结算资金（分）：分账日 {@code <= hfOrderYmd}，与跑批日 PHP {@code getProfitShareCapital} 的 {@code hf_order_date|lte} 对齐。
	 */
	public long sumProfitShareCapitalForDistributorFenWhereHfOrderDateLteYmd(
			long companyId, long distributorId, String hfOrderYmd) {
		return nz(metricsMapper.sumProfitShareCapitalForDistributorWhereHfOrderDateLteYmd(companyId, distributorId, hfOrderYmd));
	}

	/**
	 * 平台（details 侧 distributor_id=0）已结算分账额（分），分账日 {@code <=} 上界；跑批用，勿与
	 * {@link #sumProfitShareCapitalFen} 的 {@code hf_order_date &gt;=} 混淆。
	 */
	public long sumProfitShareCapitalForPlatformFenWhereHfOrderDateLteYmd(
			long companyId, String hfOrderYmd) {
		return nz(metricsMapper.sumProfitShareCapitalForPlatformWhereHfOrderDateLteYmd(companyId, hfOrderYmd));
	}

	/**
	 * Trait {@code income} 之分账手续费分量 [0]：全公司、累计至 {@code endUnix}，单位分。
	 */
	public long companyCumulativeSplitFeeIncomeFen0(long companyId, long endUnix) {
		return sumSplitFeeFromRows(metricsMapper.listCompanyCumulativeIncomeRowsToEndUnix(companyId, endUnix))
				.longValue();
	}

	/**
	 * Trait {@code refund}（SUCCESS）之分账手续费分量 [0]：全公司、累计至 {@code endUnix}，单位分。
	 */
	public long companyCumulativeSplitFeeRefundFen0(long companyId, long endUnix) {
		return sumSplitFeeFromRows(metricsMapper.listCompanyCumulativeRefundSuccessRowsToEndUnix(companyId, endUnix))
				.longValue();
	}

	/**
	 * Trait {@code income} 净收入分量 [1]：累计至 {@code endUnix}（日末）的 {@code 总额 - 分账手续费项}，单位分。
	 */
	public long distributorCumulativeNetIncomeFenAfterSplit(
			long companyId, long distributorId, long endUnix) {
		List<Map<String, Object>> rows =
				metricsMapper.listDistributorCumulativeIncomeRowsToEndUnix(companyId, distributorId, endUnix);
		if (rows == null || rows.isEmpty()) {
			return 0L;
		}
		long totalGrossFen = sumTotalFeeColumnAsLongFen(rows);
		BigDecimal splitFen = sumSplitFeeFromRows(rows);
		return totalGrossFen - splitFen.longValue();
	}

	/**
	 * Trait {@code refund} 与 SUCCESS 的净退款分量 [1]：单位分。
	 */
	public long distributorCumulativeNetRefundFenAfterSplit(
			long companyId, long distributorId, long endUnix) {
		List<Map<String, Object>> rows =
				metricsMapper.listDistributorCumulativeRefundSuccessRowsToEndUnix(companyId, distributorId, endUnix);
		if (rows == null || rows.isEmpty()) {
			return 0L;
		}
		long totalGrossFen = sumTotalFeeColumnAsLongFen(rows);
		BigDecimal splitFen = sumSplitFeeFromRows(rows);
		return totalGrossFen - splitFen.longValue();
	}

	private static long sumTotalFeeColumnAsLongFen(List<Map<String, Object>> rows) {
		BigDecimal sum = BigDecimal.ZERO;
		for (Map<String, Object> row : rows) {
			Object raw = row == null ? null : row.get("total_fee");
			if (raw == null) {
				continue;
			}
			try {
				sum = sum.add(new BigDecimal(String.valueOf(raw).trim()));
			} catch (Exception e) {
				// ignore bad cell
			}
		}
		return sum.longValue();
	}

	private static long nz(Long v) {
		return v == null ? 0L : v;
	}

	private static BigDecimal sumSplitFeeFromRows(List<Map<String, Object>> rows) {
		BigDecimal sum = BigDecimal.ZERO;
		if (rows == null) {
			return sum;
		}
		for (Map<String, Object> row : rows) {
			BigDecimal fee = splitFeeAmt(row.get("profitsharing_rate"), row.get("total_fee"));
			if (fee.compareTo(BigDecimal.ONE) >= 0) {
				sum = sum.add(fee);
			}
		}
		return sum;
	}

	private static BigDecimal splitFeeAmt(Object rateObj, Object amountObj) {
		if (rateObj == null || amountObj == null) {
			return BigDecimal.ZERO;
		}
		try {
			BigDecimal amount = new BigDecimal(String.valueOf(amountObj).trim());
			BigDecimal rate = new BigDecimal(String.valueOf(rateObj).trim());
			return amount.multiply(rate).divide(TEN_THOUSAND, 0, RoundingMode.DOWN);
		} catch (Exception e) {
			return BigDecimal.ZERO;
		}
	}

	private static String fenToYuanPlain(BigDecimal fen) {
		if (fen == null) {
			fen = BigDecimal.ZERO;
		}
		return fen.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP).toPlainString();
	}

	private static String fenToYuanPlain(long fen) {
		return fenToYuanPlain(BigDecimal.valueOf(fen));
	}
}
