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

package cn.shopex.ecshopx.chinaumspay.service.transfer;

import cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivision;
import cn.shopex.ecshopx.chinaumspay.domain.ChinaumspayDivisionDetail;
import cn.shopex.ecshopx.chinaumspay.mapper.ChinaumspayDivisionDetailMapper;
import cn.shopex.ecshopx.chinaumspay.mapper.ChinaumspayDivisionMapper;
import cn.shopex.ecshopx.chinaumspay.port.ChinaumsPaymentSettingLoadPort;
import cn.shopex.ecshopx.orders.domain.DistributionDistributorPeek;
import cn.shopex.ecshopx.orders.domain.OrdersRelChinaumspayDivision;
import cn.shopex.ecshopx.orders.mapper.DistributionDistributorPeekMapper;
import cn.shopex.ecshopx.orders.mapper.OrdersRelChinaumspayDivisionMapper;
import cn.shopex.ecshopx.orders.service.division.DivisionFormatResult;
import cn.shopex.ecshopx.orders.service.division.NeedTransferOrderRow;
import cn.shopex.ecshopx.orders.service.division.OrderAppliedTotalRefundFenQueryService;
import cn.shopex.ecshopx.orders.service.division.OrderDivisionRelStatus;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 与 PHP {@code ChinaumsPayDivisionService::formatTransferData} 对齐；分账/明细写库不在
 * 上传事务中提交。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChinaumsPayDivisionFormatTransferDataService {

	private static final int SCALE_FEN = 0;

	private final ChinaumsPaymentSettingLoadPort paymentSettingLoadPort;
	private final DistributionDistributorPeekMapper distributionDistributorPeekMapper;
	private final OrderAppliedTotalRefundFenQueryService orderAppliedTotalRefundFenQueryService;
	private final ChinaumspayDivisionMapper chinaumspayDivisionMapper;
	private final ChinaumspayDivisionDetailMapper chinaumspayDivisionDetailMapper;
	private final OrdersRelChinaumspayDivisionMapper ordersRelChinaumspayDivisionMapper;
	private final ObjectMapper objectMapper;

	/**
	 * 空/无可划付/解析失败时返回 {@code null}，与 PHP 假值/空数组跳过语义一致。
	 */
	public DivisionFormatResult formatTransferData(
			long companyId, long distributorId, List<NeedTransferOrderRow> orderDivisionList) {
		if (orderDivisionList == null || orderDivisionList.isEmpty()) {
			return null;
		}
		DistributionDistributorPeek d = distributionDistributorPeekMapper.selectById(distributorId);
		if (d == null) {
			return null;
		}
		if (!StringUtils.hasText(d.getSplitLedgerInfo())) {
			return null;
		}
		JsonNode split;
		try {
			split = objectMapper.readTree(d.getSplitLedgerInfo());
		} catch (Exception e) {
			log.warn("split_ledger_info parse: distributorId={} err={}", distributorId, e.toString());
			return null;
		}
		if (split == null || !split.isObject()) {
			return null;
		}
		Map<String, Object> pay = paymentSettingLoadPort.load(companyId, "");
		if (pay == null || pay.isEmpty()) {
			return null;
		}
		Map<String, Object> distPay = paymentSettingLoadPort.load(companyId, "distributor_" + distributorId);
		if (distPay == null || distPay.isEmpty()) {
			return null;
		}
		int dealerIdNum = d.getDealerId() == null ? 0 : d.getDealerId().intValue();
		Map<String, Object> dealerPay = null;
		if (dealerIdNum > 0) {
			dealerPay = paymentSettingLoadPort.load(companyId, "dealer_" + dealerIdNum);
			if (dealerPay == null || dealerPay.isEmpty()) {
				return null;
			}
		}
		BigDecimal payRate = ratePercentToFraction(str(pay.get("rate")));
		int now = (int) Instant.now().getEpochSecond();
		List<Long> divisionDetailIds = new ArrayList<>();
		List<Long> orderIds = new ArrayList<>();
		BigDecimal distributorTotalFee = BigDecimal.ZERO;
		BigDecimal distributorActualFee = BigDecimal.ZERO;
		BigDecimal distributorCommission = BigDecimal.ZERO;
		BigDecimal distributorDivisionFen = BigDecimal.ZERO;
		for (NeedTransferOrderRow row : orderDivisionList) {
			int appliedRefundFen = orderAppliedTotalRefundFenQueryService.sum(companyId, row.getOrderId());
			BigDecimal totalFen = fenFromOrderTotal(row.getTotalFee());
			BigDecimal refundFen = BigDecimal.valueOf(appliedRefundFen);
			BigDecimal actualFen = totalFen.subtract(refundFen);
			log.info("划付上传 order_id:{},actualFee:{}", row.getOrderId(), actualFen);
			if (actualFen.compareTo(BigDecimal.ZERO) <= 0) {
				markRelSkipById(row.getId(), now);
				continue;
			}
			BigDecimal totalRateFee = totalFen.multiply(payRate).setScale(SCALE_FEN, RoundingMode.DOWN);
			BigDecimal refundRateFee = refundFen.multiply(payRate).setScale(SCALE_FEN, RoundingMode.DOWN);
			BigDecimal finalRateFee = totalRateFee.subtract(refundRateFee);
			BigDecimal divisionFen = actualFen.subtract(finalRateFee);
			ChinaumspayDivisionDetail det = new ChinaumspayDivisionDetail();
			det.setCompanyId(companyId);
			det.setDivisionId(0L);
			det.setOrderId(row.getOrderId());
			det.setDistributorId(row.getDistributorId());
			det.setTotalFee(str(row.getTotalFee()));
			det.setActualFee(actualFen.setScale(0, RoundingMode.DOWN).toPlainString());
			det.setCommissionRate(toDouble(pay.get("rate")));
			det.setCommissionRateFee(finalRateFee.setScale(0, RoundingMode.DOWN).intValue());
			det.setDivisionFee(divisionFen.setScale(0, RoundingMode.DOWN).intValue());
			det.setCreateTime(now);
			det.setUpdateTime(now);
			chinaumspayDivisionDetailMapper.insert(det);
			if (det.getId() == null) {
				continue;
			}
			divisionDetailIds.add(det.getId());
			orderIds.add(row.getOrderId());
			distributorTotalFee = distributorTotalFee.add(totalFen);
			distributorActualFee = distributorActualFee.add(actualFen);
			distributorCommission = distributorCommission.add(finalRateFee);
			distributorDivisionFen = distributorDivisionFen.add(divisionFen);
		}
		if (divisionDetailIds.isEmpty()) {
			return null;
		}
		ChinaumspayDivision div = new ChinaumspayDivision();
		div.setCompanyId(companyId);
		div.setDistributorId(distributorId);
		div.setTotalFee(distributorTotalFee.setScale(0, RoundingMode.DOWN).toPlainString());
		div.setActualFee(distributorActualFee.setScale(0, RoundingMode.DOWN).toPlainString());
		div.setCommissionRateFee(distributorCommission.setScale(0, RoundingMode.DOWN).intValue());
		div.setDivisionFee(distributorDivisionFen.setScale(0, RoundingMode.DOWN).intValue());
		div.setCreateTime(now);
		div.setUpdateTime(now);
		chinaumspayDivisionMapper.insert(div);
		LambdaUpdateWrapper<ChinaumspayDivisionDetail> u =
				new LambdaUpdateWrapper<ChinaumspayDivisionDetail>()
						.in(ChinaumspayDivisionDetail::getId, divisionDetailIds)
						.set(ChinaumspayDivisionDetail::getDivisionId, div.getId());
		chinaumspayDivisionDetailMapper.update(null, u);
		BigDecimal hqPart = toDecimal(split, "headquarters_proportion").divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
		BigDecimal distDivFen = distributorDivisionFen;
		List<Map<String, Object>> uploadDiv = new ArrayList<>();
		List<Map<String, Object>> uploadTf = new ArrayList<>();
		if (dealerIdNum == 0) {
			BigDecimal headquartersFee = distDivFen.multiply(hqPart).setScale(0, RoundingMode.DOWN);
			BigDecimal distributorFee = distDivFen.subtract(headquartersFee);
			uploadDiv.add(divisionMap(div.getId(), str(pay.get("enterpriseid")), headquartersFee, pay, true, distributorId));
			uploadTf.add(
					transferMap(div.getId(), str(distPay.get("enterpriseid")), distributorFee, distributorId));
		} else {
			BigDecimal dealerPart = toDecimal(split, "dealer_proportion").divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
			BigDecimal headquartersFee = distDivFen.multiply(hqPart).setScale(0, RoundingMode.DOWN);
			BigDecimal dealerFee = distDivFen.multiply(dealerPart).setScale(0, RoundingMode.DOWN);
			BigDecimal left = distDivFen.subtract(headquartersFee).subtract(dealerFee);
			uploadDiv.add(divisionMap(div.getId(), str(pay.get("enterpriseid")), headquartersFee, pay, true, distributorId));
			uploadTf.add(
					transferMap(div.getId(), str(distPay.get("enterpriseid")), left, distributorId));
			uploadTf.add(
					transferMap(div.getId(), str(dealerPay.get("enterpriseid")), dealerFee, distributorId));
		}
		DivisionFormatResult r = new DivisionFormatResult();
		r.setDivision(uploadDiv);
		r.setTransfer(uploadTf);
		r.setDivisionId(div.getId());
		r.setOrderIds(orderIds);
		return r;
	}

	private void markRelSkipById(Long relId, int now) {
		if (relId == null) {
			return;
		}
		LambdaUpdateWrapper<OrdersRelChinaumspayDivision> w =
				new LambdaUpdateWrapper<OrdersRelChinaumspayDivision>()
						.eq(OrdersRelChinaumspayDivision::getId, relId)
						.set(OrdersRelChinaumspayDivision::getStatus, OrderDivisionRelStatus.SKIP)
						.set(OrdersRelChinaumspayDivision::getUpdateTime, now);
		ordersRelChinaumspayDivisionMapper.update(null, w);
	}

	private static Map<String, Object> divisionMap(
			long divId, String entId, BigDecimal fee, Map<String, Object> pay, boolean platform, long distId) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("division_id", divId);
		m.put("enterpriseid", entId);
		m.put("type", 0);
		m.put("fee", fee);
		if (platform) {
			m.put("payee", "平台");
			m.put("bank_name", str(pay.get("bank_name")));
			m.put("bank_code", str(pay.get("bank_code")));
			m.put("bank_account", str(pay.get("bank_account")));
		}
		m.put("distributor_id", distId);
		return m;
	}

	private static Map<String, Object> transferMap(
			long divId, String entId, BigDecimal fee, long distId) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("division_id", divId);
		m.put("enterpriseid", entId);
		m.put("type", 0);
		m.put("fee", fee);
		m.put("distributor_id", distId);
		return m;
	}

	private static String str(Object o) {
		if (o == null) {
			return "";
		}
		if (o instanceof String s) {
			return s;
		}
		return o.toString();
	}

	private static double toDouble(Object o) {
		if (o == null) {
			return 0d;
		}
		try {
			return Double.parseDouble(str(o).trim());
		} catch (Exception e) {
			return 0d;
		}
	}

	private static BigDecimal fenFromOrderTotal(String totalFee) {
		if (!StringUtils.hasText(totalFee)) {
			return BigDecimal.ZERO;
		}
		try {
			return new BigDecimal(totalFee.trim());
		} catch (Exception e) {
			return BigDecimal.ZERO;
		}
	}

	private static BigDecimal ratePercentToFraction(String rateStr) {
		if (!StringUtils.hasText(rateStr)) {
			return BigDecimal.ZERO;
		}
		try {
			return new BigDecimal(rateStr.trim()).divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
		} catch (Exception e) {
			return BigDecimal.ZERO;
		}
	}

	private static BigDecimal toDecimal(JsonNode split, String k) {
		if (split == null || k == null) {
			return BigDecimal.ZERO;
		}
		JsonNode n = split.get(k);
		if (n == null || !n.isNumber()) {
			return BigDecimal.ZERO;
		}
		return n.decimalValue();
	}
}
