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

import cn.shopex.ecshopx.hfpay.domain.HfpayCompanyStatisticsDay;
import cn.shopex.ecshopx.hfpay.domain.HfpayLedgerConfig;
import cn.shopex.ecshopx.hfpay.mapper.HfpayCompanyStatisticsDayMapper;
import cn.shopex.ecshopx.hfpay.mapper.HfpayLedgerConfigMapper;
import cn.shopex.ecshopx.hfpay.service.statistics.HfpayStatisticsOrderMetricsService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 平台公司日汇总批处理：按 ledger 配置表分页枚举公司，写入公司日统计（type=2）；无任务级事务，与现网每行单独落库一致。
 * <p>分页上界为 {@code page &lt; count/500} 未向上取整，末页在特定总条数下会漏批，为既有业务行为（与 PHP 一致）。
 */
@Service
public class HfpayCompanyDayStatisticsService {

	private static final int PAGE_SIZE = 500;
	private static final DateTimeFormatter BASIC = DateTimeFormatter.BASIC_ISO_DATE;

	private final HfpayLedgerConfigMapper ledgerConfigMapper;
	private final HfpayCompanyStatisticsDayMapper companyStatisticsDayMapper;
	private final HfpayStatisticsOrderMetricsService orderMetricsService;
	private final ZoneId businessZoneId;

	public HfpayCompanyDayStatisticsService(
			HfpayLedgerConfigMapper ledgerConfigMapper,
			HfpayCompanyStatisticsDayMapper companyStatisticsDayMapper,
			HfpayStatisticsOrderMetricsService orderMetricsService,
			@Value("${ecshopx.hfpay.business-zone-id:}") String businessZoneIdProp) {
		this.ledgerConfigMapper = ledgerConfigMapper;
		this.companyStatisticsDayMapper = companyStatisticsDayMapper;
		this.orderMetricsService = orderMetricsService;
		this.businessZoneId =
				StringUtils.hasText(businessZoneIdProp) ? ZoneId.of(businessZoneIdProp.trim()) : ZoneId.systemDefault();
	}

	/**
	 * @return 成功处理的 ledger 行数（与 INSERT 次数一致，跳过 company_id 为空的行）
	 */
	public int scheduleCompanyDailyStatistics() {
		LocalDate yesterday = LocalDate.now(businessZoneId).minusDays(1);
		int dateEpoch = (int) yesterday.atStartOfDay(businessZoneId).toEpochSecond();
		String hfOrderYmd = yesterday.format(BASIC);
		long endUnix = yesterday.atTime(23, 59, 59).atZone(businessZoneId).toEpochSecond();

		QueryWrapper<HfpayLedgerConfig> countWrapper = new QueryWrapper<>();
		long count = ledgerConfigMapper.selectCount(countWrapper);
		if (count < 1) {
			return 0;
		}
		int processed = 0;
		// 与 PHP: $page_num = $count / 500.0, for $page=1; $page < $page_num; $page++
		if (count > PAGE_SIZE) {
			double pageNum = count / (double) PAGE_SIZE;
			for (int page = 1; (double) page < pageNum; page++) {
				List<HfpayLedgerConfig> data = listLedgerCompanyPage(page);
				processed += companyBatch(data, dateEpoch, hfOrderYmd, endUnix);
			}
		} else {
			List<HfpayLedgerConfig> data = listLedgerCompanyPage(1);
			processed += companyBatch(data, dateEpoch, hfOrderYmd, endUnix);
		}
		return processed;
	}

	private List<HfpayLedgerConfig> listLedgerCompanyPage(int page) {
		int offset = (page - 1) * PAGE_SIZE;
		QueryWrapper<HfpayLedgerConfig> w = new QueryWrapper<>();
		w.select("company_id");
		w.last("LIMIT " + offset + ", " + PAGE_SIZE);
		return ledgerConfigMapper.selectList(w);
	}

	private int companyBatch(List<HfpayLedgerConfig> list, int dateEpoch, String hfOrderYmd, long endUnix) {
		if (list == null || list.isEmpty()) {
			return 0;
		}
		int n = 0;
		for (HfpayLedgerConfig v : list) {
			if (v.getCompanyId() == null) {
				continue;
			}
			long companyId = v.getCompanyId();
			long settlementFen =
					orderMetricsService.sumProfitShareCapitalForPlatformFenWhereHfOrderDateLteYmd(companyId, hfOrderYmd);
			long incomeSplitFeeFen0 = orderMetricsService.companyCumulativeSplitFeeIncomeFen0(companyId, endUnix);
			long refundSplitFeeFen0 = orderMetricsService.companyCumulativeSplitFeeRefundFen0(companyId, endUnix);
			long unsettledFen = incomeSplitFeeFen0 - refundSplitFeeFen0 - settlementFen;
			HfpayCompanyStatisticsDay row = new HfpayCompanyStatisticsDay();
			row.setCompanyId((int) companyId);
			row.setType(2);
			row.setDate(dateEpoch);
			row.setIncome((int) incomeSplitFeeFen0);
			row.setDisburse(0);
			row.setRefund((int) refundSplitFeeFen0);
			row.setWithdrawal(0);
			row.setBalance(0);
			row.setWithdrawalBalance(0);
			row.setUnsettledFunds((int) unsettledFen);
			row.setSettlementFunds((int) settlementFen);
			LocalDateTime now = LocalDateTime.now(businessZoneId);
			row.setCreatedAt(now);
			row.setUpdatedAt(now);
			companyStatisticsDayMapper.insert(row);
			n++;
		}
		return n;
	}
}
