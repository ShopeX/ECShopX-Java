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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.hfpay.domain.HfpayCompanyStatisticsDay;
import cn.shopex.ecshopx.hfpay.mapper.HfpayCompanyStatisticsDayMapper;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayPaymentSettingService;
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
public class HfpayCompanyDayTotalStatisticsService {

	private final HfpayCompanyStatisticsDayMapper companyStatisticsDayMapper;
	private final HfPayPaymentSettingService paymentSettingService;
	private final HfpayAcouQry001BalanceFenService acouQry001BalanceFenService;
	private final HfpayStatisticsOrderMetricsService orderMetricsService;
	private final ZoneId businessZoneId;

	public HfpayCompanyDayTotalStatisticsService(
			HfpayCompanyStatisticsDayMapper companyStatisticsDayMapper,
			HfPayPaymentSettingService paymentSettingService,
			HfpayAcouQry001BalanceFenService acouQry001BalanceFenService,
			HfpayStatisticsOrderMetricsService orderMetricsService,
			@Value("${ecshopx.hfpay.business-zone-id:}") String businessZoneIdProp) {
		this.companyStatisticsDayMapper = companyStatisticsDayMapper;
		this.paymentSettingService = paymentSettingService;
		this.acouQry001BalanceFenService = acouQry001BalanceFenService;
		this.orderMetricsService = orderMetricsService;
		this.businessZoneId =
				StringUtils.hasText(businessZoneIdProp) ? ZoneId.of(businessZoneIdProp.trim()) : ZoneId.systemDefault();
	}

	public Map<String, Object> countType2(long companyId) {
		LambdaQueryWrapper<HfpayCompanyStatisticsDay> w = new LambdaQueryWrapper<>();
		w.apply("company_id = {0}", companyId)
				.eq(HfpayCompanyStatisticsDay::getType, 2)
				.orderByDesc(HfpayCompanyStatisticsDay::getCreatedAt)
				.last("LIMIT 1");
		HfpayCompanyStatisticsDay statRow = companyStatisticsDayMapper.selectOne(w);

		long statIncomeFen = statRow != null && statRow.getIncome() != null ? statRow.getIncome().longValue() : 0L;
		long statRefundFen = statRow != null && statRow.getRefund() != null ? statRow.getRefund().longValue() : 0L;
		long statUnsettledFen =
				statRow != null && statRow.getUnsettledFunds() != null ? statRow.getUnsettledFunds().longValue() : 0L;
		long statSettlementFen =
				statRow != null && statRow.getSettlementFunds() != null ? statRow.getSettlementFunds().longValue() : 0L;

		long withdrawalFen = 0L;
		try {
			Map<String, Object> setting = paymentSettingService.loadForCompany(companyId);
			String mer = paymentSettingNonBlankString(setting.get("mer_cust_id"));
			String acct = paymentSettingNonBlankString(setting.get("acct_id"));
			if (StringUtils.hasText(mer) && StringUtils.hasText(acct)) {
				withdrawalFen = acouQry001BalanceFenService.queryBalanceFenOrZero(companyId, mer, acct);
			}
		} catch (ResourceException e) {
			withdrawalFen = 0L;
		}

		long todayStart =
				LocalDate.now(businessZoneId).atStartOfDay(businessZoneId).toEpochSecond();
		String hfOrderYmd = LocalDate.now(businessZoneId).format(DateTimeFormatter.BASIC_ISO_DATE);

		long settlementFen = orderMetricsService.sumProfitShareCapitalFen(companyId, hfOrderYmd);
		long todayIncomeFeeFen = orderMetricsService.companyTodayIncomeFeeAmtFen(companyId, todayStart);
		long todayRefundFeeFen = orderMetricsService.companyTodayRefundFeeAmtFen(companyId, todayStart);

		long incomeFen = statIncomeFen + todayIncomeFeeFen;
		long refundFen = statRefundFen + todayRefundFeeFen;
		long todayNetFen = todayIncomeFeeFen - todayRefundFeeFen;
		long unsettledFen = statUnsettledFen + todayNetFen - settlementFen;
		long settlementFundsFen = statSettlementFen + settlementFen;

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("income", fenToYuanPlain(incomeFen));
		out.put("refund", fenToYuanPlain(refundFen));
		out.put("withdrawal_balance", fenToYuanPlain(withdrawalFen));
		out.put("unsettled_funds", fenToYuanPlain(unsettledFen));
		out.put("settlement_funds", fenToYuanPlain(settlementFundsFen));
		return out;
	}

	private static String fenToYuanPlain(long fen) {
		return new BigDecimal(fen).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP).toPlainString();
	}

	/**
	 * Map value absent or null must not become the literal "null" via {@link String#valueOf(Object)}.
	 * Only non-blank trimmed strings are returned; otherwise empty (caller skips downstream calls).
	 */
	private static String paymentSettingNonBlankString(Object v) {
		if (v == null) {
			return "";
		}
		String s = String.valueOf(v).trim();
		if (!StringUtils.hasText(s) || "null".equals(s)) {
			return "";
		}
		return s;
	}
}
