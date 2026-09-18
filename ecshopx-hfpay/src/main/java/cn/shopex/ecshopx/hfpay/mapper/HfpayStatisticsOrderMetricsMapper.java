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

package cn.shopex.ecshopx.hfpay.mapper;

import cn.shopex.ecshopx.hfpay.service.export.HfpayOrderRecordExportContext;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HfpayStatisticsOrderMetricsMapper {

	long countPaidOrders(
			@Param("companyId") long companyId,
			@Param("distributorId") Long distributorId,
			@Param("startUnix") Long startUnix,
			@Param("endUnix") Long endUnix);

	Long sumOrderTotalFees(
			@Param("companyId") long companyId,
			@Param("distributorId") Long distributorId,
			@Param("startUnix") Long startUnix,
			@Param("endUnix") Long endUnix);

	long countDistinctOrdersRefundSuccess(
			@Param("companyId") long companyId,
			@Param("distributorId") Long distributorId,
			@Param("startUnix") Long startUnix,
			@Param("endUnix") Long endUnix);

	Long sumRefundedFeesSuccessOrAudit(
			@Param("companyId") long companyId,
			@Param("distributorId") Long distributorId,
			@Param("startUnix") Long startUnix,
			@Param("endUnix") Long endUnix);

	long countDistinctOrdersRefunding(
			@Param("companyId") long companyId,
			@Param("distributorId") Long distributorId,
			@Param("startUnix") Long startUnix,
			@Param("endUnix") Long endUnix);

	Long sumRefundFeesRefunding(
			@Param("companyId") long companyId,
			@Param("distributorId") Long distributorId,
			@Param("startUnix") Long startUnix,
			@Param("endUnix") Long endUnix);

	List<Map<String, Object>> listProfitSharingChargeRows(
			@Param("companyId") long companyId,
			@Param("distributorId") Long distributorId,
			@Param("startUnix") Long startUnix,
			@Param("endUnix") Long endUnix);

	List<Map<String, Object>> listTotalChargeRows(
			@Param("companyId") long companyId,
			@Param("distributorId") Long distributorId,
			@Param("startUnix") Long startUnix,
			@Param("endUnix") Long endUnix);

	List<Map<String, Object>> listRefundChargeRows(
			@Param("companyId") long companyId,
			@Param("distributorId") Long distributorId,
			@Param("startUnix") Long startUnix,
			@Param("endUnix") Long endUnix);

	List<Map<String, Object>> listUnProfitChargeRows(
			@Param("companyId") long companyId,
			@Param("distributorId") Long distributorId,
			@Param("startUnix") Long startUnix,
			@Param("endUnix") Long endUnix);

	List<Map<String, Object>> listUnProfitRefundChargeRows(
			@Param("companyId") long companyId,
			@Param("distributorId") Long distributorId,
			@Param("startUnix") Long startUnix,
			@Param("endUnix") Long endUnix);

	List<Map<String, Object>> listCompanyIncomeDayRows(
			@Param("companyId") long companyId, @Param("todayStartUnix") long todayStartUnix);

	List<Map<String, Object>> listCompanyRefundDayRows(
			@Param("companyId") long companyId, @Param("todayStartUnix") long todayStartUnix);

	Long sumProfitShareCapital(@Param("companyId") long companyId, @Param("hfOrderDateYmd") String hfOrderDateYmd);

	List<Map<String, Object>> listDistributorIncomeDayRows(
			@Param("companyId") long companyId,
			@Param("distributorId") long distributorId,
			@Param("todayStartUnix") long todayStartUnix);

	List<Map<String, Object>> listDistributorRefundSuccessDayRows(
			@Param("companyId") long companyId,
			@Param("distributorId") long distributorId,
			@Param("todayStartUnix") long todayStartUnix);

	List<Map<String, Object>> listDistributorRefundingDayRows(
			@Param("companyId") long companyId, @Param("distributorId") long distributorId);

	Long sumProfitShareCapitalForDistributor(
			@Param("companyId") long companyId,
			@Param("distributorId") long distributorId,
			@Param("hfOrderDateYmd") String hfOrderDateYmd);

	/**
	 * 与 {@link #sumProfitShareCapitalForDistributor} 同表同条件，但分账日为上界：{@code p.hf_order_date <= hfOrderDateYmd}，对齐跑批
	 * {@code getProfitShareCapital} 的 {@code hf_order_date|lte} 语义；禁止与 {@code hf_order_date &gt;=} 混用。
	 */
	Long sumProfitShareCapitalForDistributorWhereHfOrderDateLteYmd(
			@Param("companyId") long companyId,
			@Param("distributorId") long distributorId,
			@Param("hfOrderDateYmd") String hfOrderDateYmd);

	/**
	 * 跑批日累计「收入」行：与 PHP Trait {@code income} 一致，按 trade 成功且 {@code time_expire} 不晚于日末；不含 {@code a.create_time} 日窗
	 *（与 {@link #listTotalChargeRows} 的 create_time 条件不同，勿混用）。
	 */
	List<Map<String, Object>> listDistributorCumulativeIncomeRowsToEndUnix(
			@Param("companyId") long companyId,
			@Param("distributorId") long distributorId,
			@Param("endUnix") long endUnix);

	/**
	 * 跑批日累计「退款成功」行：与 PHP Trait {@code refund} 且 {@code refund_status=SUCCESS} 一致，按 {@code refund_success_time|lte} 日末。
	 */
	List<Map<String, Object>> listDistributorCumulativeRefundSuccessRowsToEndUnix(
			@Param("companyId") long companyId,
			@Param("distributorId") long distributorId,
			@Param("endUnix") long endUnix);

	/**
	 * 平台已结算分账（details.distributor_id=0），分账日 {@code hf_order_date <= hfOrderDateYmd}，与
	 * {@code getProfitShareCapital} 之 {@code hf_order_date|lte} 一致。
	 */
	Long sumProfitShareCapitalForPlatformWhereHfOrderDateLteYmd(
			@Param("companyId") long companyId, @Param("hfOrderDateYmd") String hfOrderDateYmd);

	/**
	 * 全公司累计「收入」侧 Trait [0] 行：与 {@link #listDistributorCumulativeIncomeRowsToEndUnix} 同构，但无
	 * {@code distributor_id} 条件。
	 */
	List<Map<String, Object>> listCompanyCumulativeIncomeRowsToEndUnix(
			@Param("companyId") long companyId, @Param("endUnix") long endUnix);

	/**
	 * 全公司累计「退款成功」侧 Trait [0] 行：与
	 * {@link #listDistributorCumulativeRefundSuccessRowsToEndUnix} 同构，但无 {@code distributor_id} 条件。
	 */
	List<Map<String, Object>> listCompanyCumulativeRefundSuccessRowsToEndUnix(
			@Param("companyId") long companyId, @Param("endUnix") long endUnix);

	// --- Filtered metrics for order list statistics (HfpayOrderRecordExportContext) ---

	long countPaidOrdersFiltered(
			@Param("ctx") HfpayOrderRecordExportContext ctx, @Param("statusKind") String statusKind);

	Long sumOrderTotalFeesFiltered(
			@Param("ctx") HfpayOrderRecordExportContext ctx, @Param("statusKind") String statusKind);

	long countDistinctOrdersRefundSuccessFiltered(@Param("ctx") HfpayOrderRecordExportContext ctx);

	Long sumRefundedFeesSuccessOrAuditFiltered(@Param("ctx") HfpayOrderRecordExportContext ctx);

	long countDistinctOrdersRefundingFiltered(@Param("ctx") HfpayOrderRecordExportContext ctx);

	Long sumRefundFeesRefundingFiltered(@Param("ctx") HfpayOrderRecordExportContext ctx);

	List<Map<String, Object>> listProfitSharingChargeRowsFiltered(
			@Param("ctx") HfpayOrderRecordExportContext ctx, @Param("statusKind") String statusKind);

	List<Map<String, Object>> listTotalChargeRowsFiltered(@Param("ctx") HfpayOrderRecordExportContext ctx);

	List<Map<String, Object>> listRefundChargeRowsFiltered(@Param("ctx") HfpayOrderRecordExportContext ctx);

	List<Map<String, Object>> listUnProfitChargeRowsFiltered(@Param("ctx") HfpayOrderRecordExportContext ctx);

	List<Map<String, Object>> listUnProfitRefundChargeRowsFiltered(@Param("ctx") HfpayOrderRecordExportContext ctx);
}
