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

package cn.shopex.ecshopx.hfpay.service;

import cn.shopex.ecshopx.hfpay.domain.HfpayDistributorStatisticsDay;
import cn.shopex.ecshopx.hfpay.domain.HfpayDistributorTransactionStatistics;
import cn.shopex.ecshopx.hfpay.domain.HfpayEnterapply;
import cn.shopex.ecshopx.hfpay.mapper.HfpayDistributorStatisticsDayMapper;
import cn.shopex.ecshopx.hfpay.mapper.HfpayDistributorTransactionStatisticsMapper;
import cn.shopex.ecshopx.hfpay.mapper.HfpayEnterapplyMapper;
import cn.shopex.ecshopx.hfpay.mapper.HfpayTradeRecordMapper;
import cn.shopex.ecshopx.hfpay.service.statistics.HfpayStatisticsOrderMetricsService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 店铺分账日汇总批处理：按业务时区下「昨日」对入驻行跑批，写入分账日表与交易统计表；不包全任务级长事务，与现网每行落库
 * 的提交粒度一致。
 * <p>分页环路上界为「page &lt; count/500 未向上取整」，末页在特定总条数下会漏批，为既有业务行为。
 */
@Service
public class HfpayDistributorStatisticsDayService {

	private static final int PAGE = 500;
	private static final DateTimeFormatter BASIC = DateTimeFormatter.BASIC_ISO_DATE;

	private final HfpayEnterapplyMapper enterapplyMapper;
	private final HfpayDistributorStatisticsDayMapper statisticsDayMapper;
	private final HfpayDistributorTransactionStatisticsMapper transactionStatisticsMapper;
	private final HfpayTradeRecordMapper tradeRecordMapper;
	private final HfpayStatisticsOrderMetricsService orderMetricsService;
	private final ZoneId businessZoneId;

	public HfpayDistributorStatisticsDayService(
			HfpayEnterapplyMapper enterapplyMapper,
			HfpayDistributorStatisticsDayMapper statisticsDayMapper,
			HfpayDistributorTransactionStatisticsMapper transactionStatisticsMapper,
			HfpayTradeRecordMapper tradeRecordMapper,
			HfpayStatisticsOrderMetricsService orderMetricsService,
			@Value("${ecshopx.hfpay.business-zone-id:}") String businessZoneIdProp) {
		this.enterapplyMapper = enterapplyMapper;
		this.statisticsDayMapper = statisticsDayMapper;
		this.transactionStatisticsMapper = transactionStatisticsMapper;
		this.tradeRecordMapper = tradeRecordMapper;
		this.orderMetricsService = orderMetricsService;
		this.businessZoneId =
				StringUtils.hasText(businessZoneIdProp) ? ZoneId.of(businessZoneIdProp.trim()) : ZoneId.systemDefault();
	}

	/**
	 * @return 有效入驻行处理条数（与 INSERT 对数一致，跳过 company_id/distributor_id 为空的行）
	 */
	public int statistics() {
		LocalDate yesterday = LocalDate.now(businessZoneId).minusDays(1);
		int dateEpoch = (int) yesterday.atStartOfDay(businessZoneId).toEpochSecond();
		String hfOrderYmd = yesterday.format(BASIC);
		long endUnix = yesterday.atTime(23, 59, 59).atZone(businessZoneId).toEpochSecond();
		long startUnix = yesterday.atStartOfDay(businessZoneId).toEpochSecond();

		QueryWrapper<HfpayEnterapply> countWrapper = baseEnterapplyQuery();
		long count = enterapplyMapper.selectCount(countWrapper);
		if (count == 0) {
			return 0;
		}
		int processed = 0;
		// 与 PHP: $page_num = $count / 500.0, for $page=1; $page < $page_num; $page++
		if (count > PAGE) {
			double pageNum = count / (double) PAGE;
			for (int page = 1; (double) page < pageNum; page++) {
				List<HfpayEnterapply> data = listEnterapplyPage(page);
				distributorDay(data, dateEpoch, hfOrderYmd, startUnix, endUnix);
				distributorTransactionDay(data, dateEpoch, startUnix, endUnix);
				processed += countValidEnterapplyRows(data);
			}
		} else {
			List<HfpayEnterapply> data = listEnterapplyPage(1);
			distributorDay(data, dateEpoch, hfOrderYmd, startUnix, endUnix);
			distributorTransactionDay(data, dateEpoch, startUnix, endUnix);
			processed += countValidEnterapplyRows(data);
		}
		return processed;
	}

	private static int countValidEnterapplyRows(List<HfpayEnterapply> data) {
		if (data == null || data.isEmpty()) {
			return 0;
		}
		int n = 0;
		for (HfpayEnterapply val : data) {
			if (val.getCompanyId() != null && val.getDistributorId() != null) {
				n++;
			}
		}
		return n;
	}

	private static QueryWrapper<HfpayEnterapply> baseEnterapplyQuery() {
		QueryWrapper<HfpayEnterapply> w = new QueryWrapper<>();
		w.in("apply_type", "1", "2");
		w.eq("status", "3");
		return w;
	}

	private List<HfpayEnterapply> listEnterapplyPage(int page) {
		int offset = (page - 1) * PAGE;
		QueryWrapper<HfpayEnterapply> w = baseEnterapplyQuery();
		w.select("company_id", "distributor_id");
		w.last("LIMIT " + offset + ", " + PAGE);
		return enterapplyMapper.selectList(w);
	}

	private void distributorDay(
			List<HfpayEnterapply> data,
			int dateEpoch,
			String hfOrderYmd,
			long startUnix,
			long endUnix) {
		if (data == null || data.isEmpty()) {
			return;
		}
		for (HfpayEnterapply val : data) {
			if (val.getCompanyId() == null || val.getDistributorId() == null) {
				continue;
			}
			long companyId = val.getCompanyId();
			long distributorId = val.getDistributorId();
			long settlementFen = orderMetricsService.sumProfitShareCapitalForDistributorFenWhereHfOrderDateLteYmd(
					companyId, distributorId, hfOrderYmd);
			long incomeFen = orderMetricsService.distributorCumulativeNetIncomeFenAfterSplit(companyId, distributorId, endUnix);
			long refundFen = orderMetricsService.distributorCumulativeNetRefundFenAfterSplit(companyId, distributorId, endUnix);
			Long outOutcome = tradeRecordMapper.sumOutcomeFenByWithdrawalFilter(companyId, String.valueOf(distributorId), endUnix);
			long withdrawalFen = outOutcome == null ? 0L : outOutcome;
			long unsettledFen = incomeFen - refundFen - settlementFen;
			HfpayDistributorStatisticsDay row = new HfpayDistributorStatisticsDay();
			row.setCompanyId((int) companyId);
			row.setDistributorId((int) distributorId);
			row.setDate(dateEpoch);
			row.setIncome((int) (incomeFen - refundFen));
			row.setDisburse(0);
			row.setWithdrawal((int) withdrawalFen);
			row.setRefund((int) refundFen);
			row.setBalance(0);
			row.setWithdrawalBalance(0);
			row.setUnsettledFunds((int) unsettledFen);
			row.setSettlementFunds((int) settlementFen);
			statisticsDayMapper.insert(row);
		}
	}

	private void distributorTransactionDay(List<HfpayEnterapply> data, int dateEpoch, long startUnix, long endUnix) {
		if (data == null || data.isEmpty()) {
			return;
		}
		for (HfpayEnterapply val : data) {
			if (val.getCompanyId() == null || val.getDistributorId() == null) {
				continue;
			}
			long companyId = val.getCompanyId();
			long distributorId = val.getDistributorId();
			Map<String, Object> c = orderMetricsService.count(companyId, distributorId, startUnix, endUnix);
			HfpayDistributorTransactionStatistics ins = new HfpayDistributorTransactionStatistics();
			ins.setCompanyId((int) companyId);
			ins.setDistributorId((int) distributorId);
			ins.setDate(dateEpoch);
			ins.setOrderCount(toInt(c.get("order_count")));
			ins.setOrderTotalFee(yuanStringToFenInt(c.get("order_total_fee")));
			ins.setOrderRefundCount(toInt(c.get("order_refund_count")));
			ins.setOrderRefundTotalFee(yuanStringToFenInt(c.get("order_refund_total_fee")));
			ins.setOrderRefundingCount(toInt(c.get("order_refunding_count")));
			ins.setOrderRefundingTotalFee(yuanStringToFenInt(c.get("order_refunding_total_fee")));
			ins.setOrderProfitSharingCharge(yuanStringToFenInt(c.get("order_profit_sharing_charge")));
			ins.setOrderTotalCharge(yuanStringToFenInt(c.get("order_total_charge")));
			ins.setOrderRefundTotalCharge(yuanStringToFenInt(c.get("order_refund_total_charge")));
			ins.setOrderUnProfitSharingTotalCharge(yuanStringToFenInt(c.get("order_un_profit_sharing_total_charge")));
			ins.setOrderUnProfitSharingRefundTotalCharge(
					yuanStringToFenInt(c.get("order_un_profit_sharing_refund_total_charge")));
			transactionStatisticsMapper.insert(ins);
		}
	}

	private static int toInt(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		return 0;
	}

	private static int yuanStringToFenInt(Object yuanObj) {
		if (yuanObj == null) {
			return 0;
		}
		try {
			BigDecimal yuan = new BigDecimal(String.valueOf(yuanObj).trim());
			return yuan.movePointRight(2).intValue();
		} catch (Exception e) {
			return 0;
		}
	}
}
