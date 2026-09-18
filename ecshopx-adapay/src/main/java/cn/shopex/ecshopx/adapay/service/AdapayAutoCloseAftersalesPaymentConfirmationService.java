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

package cn.shopex.ecshopx.adapay.service;

import cn.shopex.ecshopx.adapay.domain.AdapayDivFee;
import cn.shopex.ecshopx.adapay.domain.AdapayMember;
import cn.shopex.ecshopx.adapay.domain.AdapayMerchantResident;
import cn.shopex.ecshopx.adapay.domain.AdapayPaymemtConfirm;
import cn.shopex.ecshopx.adapay.mapper.AdapayDivFeeMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayMemberMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayMerchantResidentMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayPaymemtConfirmMapper;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.common.cron.payment.AdapayPaymentConfirmHttpGateway;
import cn.shopex.ecshopx.common.port.payment.AdapayPaymentSettingsReadPort;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 对位汇付 Adapay 侧「子单全 CLOSED 后支付确认 + 分账」同步链路；外发 HTTP 经 {@link AdapayPaymentConfirmHttpGateway}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdapayAutoCloseAftersalesPaymentConfirmationService {

	private static final String CLOSED = "CLOSED";
	private static final String STATUS_PENDING = "pending";

	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final TradeMapper tradeMapper;
	private final AftersalesRefundMapper aftersalesRefundMapper;
	private final AdapayPaymemtConfirmMapper adapayPaymemtConfirmMapper;
	private final AdapayDivFeeMapper adapayDivFeeMapper;
	private final AdapayMemberMapper adapayMemberMapper;
	private final AdapayMerchantResidentMapper adapayMerchantResidentMapper;
	private final DistributorMapper distributorMapper;
	private final AdapayPaymentSettingsReadPort adapayPaymentSettingsReadPort;
	private final AdapayPaymentConfirmHttpGateway adapayPaymentConfirmHttpGateway;
	private final ObjectMapper objectMapper;

	public void scheduleAutoPaymentConfirmation(long companyId, long orderId) {
		var items =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, orderId));
		boolean allClosed = true;
		for (var v : items) {
			if (!CLOSED.equals(v.getAftersalesStatus())) {
				allClosed = false;
				break;
			}
		}
		if (!allClosed) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		AdapayPaymemtConfirm ins = new AdapayPaymemtConfirm();
		ins.setCompanyId(companyId);
		ins.setOrderId(String.valueOf(orderId));
		ins.setStatus(STATUS_PENDING);
		ins.setCreateTime(now);
		ins.setUpdateTime(now);
		adapayPaymemtConfirmMapper.insert(ins);
		adaPayPaymentConfirmation(companyId, orderId);
	}

	/**
	 * 扫描超时 pending 确认行并逐条重试汇付支付确认；返回本批查询到的行数。
	 */
	public int adaPayPaymentConfirmRetry() {
		int nowSec = (int) (System.currentTimeMillis() / 1000L);
		int minCreateTime = nowSec - 600;
		var pending =
				adapayPaymemtConfirmMapper.selectList(
						new LambdaQueryWrapper<AdapayPaymemtConfirm>()
								.eq(AdapayPaymemtConfirm::getStatus, STATUS_PENDING)
								.le(AdapayPaymemtConfirm::getCreateTime, minCreateTime));
		if (pending == null || pending.isEmpty()) {
			return 0;
		}
		int n = pending.size();
		for (AdapayPaymemtConfirm row : pending) {
			long companyId = row.getCompanyId() == null ? 0L : row.getCompanyId();
			if (!StringUtils.hasText(row.getOrderId())) {
				log.debug("adapay 重试支付确认 => 缺订单号");
				continue;
			}
			long orderId;
			try {
				orderId = Long.parseLong(row.getOrderId().trim());
			} catch (NumberFormatException e) {
				log.debug("adapay 重试支付确认 => 订单号非数字: {}", row.getOrderId());
				continue;
			}
			adaPayPaymentConfirmation(companyId, orderId);
		}
		return n;
	}

	private void adaPayPaymentConfirmation(long companyId, long orderId) {
		NormalOrders orderInfo =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (orderInfo == null) {
			log.debug("adapay 支付确认失败 => 无效订单号:{}", orderId);
			return;
		}
		if (!"adapay".equals(orderInfo.getPayType())) {
			log.debug("adapay 支付确认失败 => 支付方式非 adapay 订单:{}", orderId);
			return;
		}
		Trade tradeInfo =
				tradeMapper.selectOne(
						new LambdaQueryWrapper<Trade>()
								.eq(Trade::getCompanyId, String.valueOf(companyId))
								.eq(Trade::getOrderId, String.valueOf(orderId))
								.eq(Trade::getTradeState, "SUCCESS")
								.eq(Trade::getPayType, "adapay")
								.last("LIMIT 1"));
		if (tradeInfo == null) {
			log.debug("adapay 支付确认失败 => 交易单不存在:{}", orderId);
			return;
		}
		if (!"adapay".equals(tradeInfo.getPayType())) {
			log.debug("adapay 支付确认失败 => 交易单支付类型非 adapay:{}", tradeInfo.getTradeId());
			return;
		}
		if (!StringUtils.hasText(tradeInfo.getTransactionId())) {
			log.debug("adapay 支付确认失败 => 交易单缺 transaction_id:{}", tradeInfo.getTradeId());
			return;
		}
		int totalFeeFen = tradeInfo.getTotalFee() == null ? 0 : tradeInfo.getTotalFee();
		for (AftersalesRefund r : listRefundsForConfirm(companyId, orderId)) {
			totalFeeFen -= r.getRefundFee() == null ? 0 : r.getRefundFee();
		}
		if (totalFeeFen <= 0) {
			return;
		}
		BigDecimal totalFeeYuan = BigDecimal.valueOf(totalFeeFen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
		String orderNo = tradeInfo.getTradeId() + "_" + ThreadLocalRandom.current().nextInt(10_000, 100_000);
		LinkedHashMap<String, Object> objParams = new LinkedHashMap<>();
		objParams.put("company_id", companyId);
		objParams.put("api_method", "PaymentConfirm.create");
		objParams.put("payment_id", tradeInfo.getTransactionId());
		objParams.put("order_no", orderNo);
		objParams.put("confirm_amt", totalFeeYuan.toPlainString());

		String payChannel = tradeInfo.getPayChannel() == null ? "" : tradeInfo.getPayChannel();
		if ("wx_qr".equals(payChannel)) {
			payChannel = "wx_pub";
		}
		BigDecimal feeRate = resolveFeeRate(companyId, payChannel);
		if (feeRate == null) {
			log.debug("adapay 支付确认失败 => 没有设置费率");
			return;
		}
		BigDecimal feeAmt = totalFeeYuan.multiply(feeRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

		long distributorId = orderInfo.getDistributorId() == null ? 0L : orderInfo.getDistributorId();
		List<Map<String, Object>> originalDiv;
		if (distributorId == 0L) {
			AdapayMerchantResident resident =
					adapayMerchantResidentMapper.selectOne(
							new LambdaQueryWrapper<AdapayMerchantResident>()
									.eq(AdapayMerchantResident::getCompanyId, companyId)
									.last("LIMIT 1"));
			if (resident == null || !StringUtils.hasText(resident.getAdapayFeeMode())) {
				log.debug("adapay 支付确认失败 => 主商户进件/费率模式缺失");
				return;
			}
			objParams.put("fee_mode", resident.getAdapayFeeMode());
			originalDiv = new ArrayList<>();
			originalDiv.add(divRow("0", totalFeeYuan.toPlainString(), "Y"));
		} else {
			Distributor dist =
					distributorMapper.selectOne(
							new LambdaQueryWrapper<Distributor>()
									.eq(Distributor::getCompanyId, companyId)
									.eq(Distributor::getDistributorId, distributorId)
									.last("LIMIT 1"));
			if (dist == null) {
				log.debug("adapay 支付确认失败 => 无效店铺:{}", distributorId);
				return;
			}
			if (!StringUtils.hasText(dist.getSplitLedgerInfo())) {
				log.debug("adapay 支付确认失败 => 未设置分账信息 店铺:{}", distributorId);
				return;
			}
			Map<String, Object> split;
			try {
				split = objectMapper.readValue(dist.getSplitLedgerInfo(), new TypeReference<>() {});
			} catch (Exception e) {
				log.debug("adapay 支付确认失败 => split_ledger 解析失败");
				return;
			}
			String feeMode = str(split.get("adapay_fee_mode"));
			if (!StringUtils.hasText(feeMode)) {
				return;
			}
			objParams.put("fee_mode", feeMode);
			Long distributorMemberId = memberIdByOperator(companyId, distributorId, "distributor");
			if (distributorMemberId == null) {
				log.debug("adapay 支付确认失败 => 店铺子商户未进件:{}", distributorId);
				return;
			}
			originalDiv =
					buildDistributorDivMembers(
							feeMode, totalFeeYuan, feeAmt, dist, companyId, distributorMemberId, split);
			if (originalDiv == null) {
				return;
			}
		}

		List<Map<String, Object>> divForRequest = new ArrayList<>();
		for (Map<String, Object> m : originalDiv) {
			if (!"0.00".equals(String.valueOf(m.get("amount")))) {
				divForRequest.add(m);
			}
		}
		objParams.put("div_members", divForRequest);

		Map<String, Object> res = adapayPaymentConfirmHttpGateway.call(objParams);
		Object dataObj = res == null ? null : res.get("data");
		@SuppressWarnings("unchecked")
		Map<String, Object> data = dataObj instanceof Map ? (Map<String, Object>) dataObj : Map.of();
		persistConfirmAndTrade(orderInfo, tradeInfo, objParams, data, totalFeeYuan, feeAmt, originalDiv, companyId, orderId);
	}

	private List<AftersalesRefund> listRefundsForConfirm(long companyId, long orderId) {
		return aftersalesRefundMapper.selectList(
				new LambdaQueryWrapper<AftersalesRefund>()
						.eq(AftersalesRefund::getCompanyId, companyId)
						.eq(AftersalesRefund::getOrderId, orderId)
						.in(
								AftersalesRefund::getRefundStatus,
								List.of("SUCCESS", "AUDIT_SUCCESS", "CHANGE")));
	}

	private List<Map<String, Object>> buildDistributorDivMembers(
			String feeMode,
			BigDecimal totalFeeYuan,
			BigDecimal feeAmt,
			Distributor dist,
			long companyId,
			long distributorMemberId,
			Map<String, Object> split) {
		BigDecimal headquartersProportion =
				BigDecimal.valueOf(dbl(split.get("headquarters_proportion")))
						.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
		int dealerOpId = dist.getDealerId() == null ? 0 : dist.getDealerId();
		BigDecimal divFeeYuan = totalFeeYuan.subtract(feeAmt);
		if ("I".equals(feeMode)) {
			if (dealerOpId == 0) {
				BigDecimal headquartersFee =
						divFeeYuan.multiply(headquartersProportion).setScale(2, RoundingMode.HALF_UP);
				BigDecimal distributorFee =
						divFeeYuan.subtract(headquartersFee).add(feeAmt).setScale(2, RoundingMode.HALF_UP);
				List<Map<String, Object>> out = new ArrayList<>();
				out.add(divRow("0", headquartersFee.toPlainString(), "N"));
				out.add(divRow(String.valueOf(distributorMemberId), distributorFee.toPlainString(), "Y"));
				return out;
			}
			BigDecimal dealerProportion =
					BigDecimal.valueOf(dbl(split.get("dealer_proportion")))
							.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
			BigDecimal headquartersFee =
					divFeeYuan.multiply(headquartersProportion).setScale(2, RoundingMode.HALF_UP);
			BigDecimal dealerFee = divFeeYuan.multiply(dealerProportion).setScale(2, RoundingMode.HALF_UP);
			BigDecimal distributorFee =
					divFeeYuan
							.subtract(headquartersFee)
							.subtract(dealerFee)
							.add(feeAmt)
							.setScale(2, RoundingMode.HALF_UP);
			Long dealerMemberId = memberIdByOperator(companyId, dealerOpId, "dealer");
			if (dealerMemberId == null) {
				log.debug("adapay 支付确认失败 => 经销商子商户未进件:{}", dealerOpId);
				return null;
			}
			List<Map<String, Object>> out = new ArrayList<>();
			out.add(divRow("0", headquartersFee.toPlainString(), "N"));
			out.add(divRow(String.valueOf(distributorMemberId), distributorFee.toPlainString(), "Y"));
			out.add(divRow(String.valueOf(dealerMemberId), dealerFee.toPlainString(), "N"));
			return out;
		}
		if (dealerOpId == 0) {
			BigDecimal headquartersFee =
					totalFeeYuan.multiply(headquartersProportion).setScale(2, RoundingMode.HALF_UP);
			BigDecimal distributorFee = totalFeeYuan.subtract(headquartersFee).setScale(2, RoundingMode.HALF_UP);
			List<Map<String, Object>> out = new ArrayList<>();
			out.add(divRow("0", headquartersFee.toPlainString(), "N"));
			out.add(divRow(String.valueOf(distributorMemberId), distributorFee.toPlainString(), "N"));
			return out;
		}
		BigDecimal dealerProportion =
				BigDecimal.valueOf(dbl(split.get("dealer_proportion")))
						.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
		BigDecimal headquartersFee = totalFeeYuan.multiply(headquartersProportion).setScale(2, RoundingMode.HALF_UP);
		BigDecimal dealerFee = totalFeeYuan.multiply(dealerProportion).setScale(2, RoundingMode.HALF_UP);
		BigDecimal distributorFee =
				totalFeeYuan.subtract(headquartersFee).subtract(dealerFee).setScale(2, RoundingMode.HALF_UP);
		Long dealerMemberId = memberIdByOperator(companyId, dealerOpId, "dealer");
		if (dealerMemberId == null) {
			log.debug("adapay 支付确认失败 => 经销商子商户未进件:{}", dealerOpId);
			return null;
		}
		List<Map<String, Object>> out = new ArrayList<>();
		out.add(divRow("0", headquartersFee.toPlainString(), "N"));
		out.add(divRow(String.valueOf(distributorMemberId), distributorFee.toPlainString(), "N"));
		out.add(divRow(String.valueOf(dealerMemberId), dealerFee.toPlainString(), "N"));
		return out;
	}

	private void persistConfirmAndTrade(
			NormalOrders orderInfo,
			Trade tradeInfo,
			LinkedHashMap<String, Object> objParams,
			Map<String, Object> data,
			BigDecimal totalFeeYuan,
			BigDecimal feeAmt,
			List<Map<String, Object>> originalDiv,
			long companyId,
			long orderId) {
		String st = str(data.get("status"));
		int now = (int) (System.currentTimeMillis() / 1000L);
		if ("failed".equals(st)) {
			LinkedHashMap<String, Object> upd = new LinkedHashMap<>();
			upd.put("distributor_id", orderInfo.getDistributorId());
			upd.put("payment_id", objParams.get("payment_id"));
			upd.put("order_no", objParams.get("order_no"));
			upd.put("confirm_amt", totalFeeYuan.toPlainString());
			upd.put("div_members", toJson(objParams.get("div_members")));
			upd.put("status", st);
			upd.put("request_params", toJson(objParams));
			upd.put("response_params", toJson(data));
			upd.put("update_time", now);
			confirmPatch(companyId, orderId, upd);
			return;
		}
		LinkedHashMap<String, Object> updOk = new LinkedHashMap<>();
		updOk.put("distributor_id", orderInfo.getDistributorId());
		updOk.put("payment_id", objParams.get("payment_id"));
		updOk.put("payment_confirmation_id", str(data.get("id")));
		updOk.put("order_no", objParams.get("order_no"));
		updOk.put("confirm_amt", totalFeeYuan.toPlainString());
		updOk.put("div_members", toJson(objParams.get("div_members")));
		updOk.put("status", st);
		updOk.put("request_params", toJson(objParams));
		updOk.put("response_params", toJson(data));
		updOk.put("update_time", now);
		confirmPatch(companyId, orderId, updOk);

		String divJson = toJson(objParams.get("div_members"));
		int feeFen = feeAmt.multiply(BigDecimal.valueOf(100)).intValue();
		tradeMapper.update(
				null,
				new LambdaUpdateWrapper<Trade>()
						.eq(Trade::getTradeId, tradeInfo.getTradeId())
						.set(Trade::getDivMembers, divJson)
						.set(Trade::getAdapayFeeMode, str(objParams.get("fee_mode")))
						.set(Trade::getAdapayFee, feeFen)
						.set(Trade::getAdapayDivStatus, "DIVED"));
		int payFeeFen = tradeInfo.getTotalFee() == null ? 0 : tradeInfo.getTotalFee();
		if (!originalDiv.isEmpty()) {
			String m0f = str(originalDiv.get(0).get("fee_flag"));
			int d0 = fenAmount(originalDiv.get(0).get("amount"));
			int rowDiv =
					"Y".equals(m0f) ? d0 - feeFen : d0;
			insertDivFee(
					tradeInfo,
					companyId,
					orderId,
					orderInfo.getDistributorId() == null ? "0" : String.valueOf(orderInfo.getDistributorId()),
					payFeeFen,
					rowDiv,
					str(originalDiv.get(0).get("member_id")),
					"admin");
		}
		if (originalDiv.size() > 1) {
			String m1f = str(originalDiv.get(1).get("fee_flag"));
			int d1 = fenAmount(originalDiv.get(1).get("amount"));
			int rowDiv = "Y".equals(m1f) ? d1 - feeFen : d1;
			insertDivFee(
					tradeInfo,
					companyId,
					orderId,
					orderInfo.getDistributorId() == null ? "0" : String.valueOf(orderInfo.getDistributorId()),
					payFeeFen,
					rowDiv,
					str(originalDiv.get(1).get("member_id")),
					"distributor");
		}
		if (originalDiv.size() > 2) {
			int d2 = fenAmount(originalDiv.get(2).get("amount"));
			insertDivFee(
					tradeInfo,
					companyId,
					orderId,
					orderInfo.getDistributorId() == null ? "0" : String.valueOf(orderInfo.getDistributorId()),
					payFeeFen,
					d2,
					str(originalDiv.get(2).get("member_id")),
					"dealer");
			Long did = orderInfo.getDistributorId();
			if (did != null) {
				var drow =
						distributorMapper.selectOne(
								new LambdaQueryWrapper<Distributor>()
										.eq(Distributor::getCompanyId, companyId)
										.eq(Distributor::getDistributorId, did)
										.last("LIMIT 1"));
				long dealer = drow != null && drow.getDealerId() != null ? drow.getDealerId().longValue() : 0L;
				tradeMapper.update(
						null,
						new LambdaUpdateWrapper<Trade>()
								.eq(Trade::getTradeId, tradeInfo.getTradeId())
								.set(Trade::getDealerId, String.valueOf(dealer)));
			}
		}
	}

	private void insertDivFee(
			Trade tradeInfo,
			long companyId,
			long orderId,
			String distributorIdStr,
			int payFeeFen,
			int divFen,
			String memberId,
			String opType) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		AdapayDivFee row = new AdapayDivFee();
		row.setTradeId(tradeInfo.getTradeId());
		row.setOrderId(String.valueOf(orderId));
		row.setCompanyId(String.valueOf(companyId));
		row.setDistributorId(distributorIdStr);
		row.setOperatorType(opType);
		row.setPayFee(payFeeFen);
		row.setDivFee(divFen);
		try {
			row.setAdapayMemberId(Integer.parseInt(memberId));
		} catch (Exception e) {
			row.setAdapayMemberId(0);
		}
		row.setCreateTime(now);
		row.setUpdateTime(now);
		adapayDivFeeMapper.insert(row);
	}

	private void confirmPatch(long companyId, long orderId, LinkedHashMap<String, Object> upd) {
		var uw =
				new LambdaUpdateWrapper<AdapayPaymemtConfirm>()
						.eq(AdapayPaymemtConfirm::getCompanyId, companyId)
						.eq(AdapayPaymemtConfirm::getOrderId, String.valueOf(orderId));
		if (upd.get("distributor_id") != null) {
			uw.set(AdapayPaymemtConfirm::getDistributorId, longVal(upd.get("distributor_id")));
		}
		if (upd.get("payment_id") != null) {
			uw.set(AdapayPaymemtConfirm::getPaymentId, str(upd.get("payment_id")));
		}
		if (upd.get("payment_confirmation_id") != null) {
			uw.set(AdapayPaymemtConfirm::getPaymentConfirmationId, str(upd.get("payment_confirmation_id")));
		}
		if (upd.get("order_no") != null) {
			uw.set(AdapayPaymemtConfirm::getOrderNo, str(upd.get("order_no")));
		}
		if (upd.get("confirm_amt") != null) {
			uw.set(AdapayPaymemtConfirm::getConfirmAmt, str(upd.get("confirm_amt")));
		}
		if (upd.get("div_members") != null) {
			uw.set(AdapayPaymemtConfirm::getDivMembers, str(upd.get("div_members")));
		}
		if (upd.get("status") != null) {
			uw.set(AdapayPaymemtConfirm::getStatus, str(upd.get("status")));
		}
		if (upd.get("request_params") != null) {
			uw.set(AdapayPaymemtConfirm::getRequestParams, str(upd.get("request_params")));
		}
		if (upd.get("response_params") != null) {
			uw.set(AdapayPaymemtConfirm::getResponseParams, str(upd.get("response_params")));
		}
		if (upd.get("update_time") != null) {
			uw.set(AdapayPaymemtConfirm::getUpdateTime, intVal(upd.get("update_time")));
		}
		adapayPaymemtConfirmMapper.update(null, uw);
	}

	private static Map<String, Object> divRow(String memberId, String amount, String feeFlag) {
		LinkedHashMap<String, Object> m = new LinkedHashMap<>();
		m.put("member_id", memberId);
		m.put("amount", amount);
		m.put("fee_flag", feeFlag);
		return m;
	}

	private Long memberIdByOperator(long companyId, long operatorId, String type) {
		AdapayMember m =
				adapayMemberMapper.selectOne(
						new LambdaQueryWrapper<AdapayMember>()
								.eq(AdapayMember::getCompanyId, companyId)
								.eq(AdapayMember::getOperatorId, (int) operatorId)
								.eq(AdapayMember::getOperatorType, type)
								.last("LIMIT 1"));
		return m == null ? null : m.getId();
	}

	private BigDecimal resolveFeeRate(long companyId, String payChannel) {
		Map<String, Object> s = adapayPaymentSettingsReadPort.getPaymentSetting(companyId);
		if (s == null || s.isEmpty()) {
			return null;
		}
		return switch (payChannel) {
			case "wx_lite", "wx_pub" -> toBd(s.get("wx_pub_" + str(s.get("wxpay_fee_type"))));
			case "alipay", "alipay_wap" -> toBd(s.get("alipay_call"));
			case "alipay_qr" -> toBd(s.get("alipay_qr_" + str(s.get("alipay_fee_type"))));
			default -> null;
		};
	}

	private static BigDecimal toBd(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof BigDecimal b) {
			return b;
		}
		if (o instanceof Number n) {
			return BigDecimal.valueOf(n.doubleValue());
		}
		try {
			return new BigDecimal(String.valueOf(o).trim());
		} catch (Exception e) {
			return null;
		}
	}

	private String toJson(Object o) {
		try {
			return objectMapper.writeValueAsString(o);
		} catch (Exception e) {
			return "{}";
		}
	}

	private static String str(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	private static double dbl(Object o) {
		if (o == null) {
			return 0.0;
		}
		if (o instanceof Number n) {
			return n.doubleValue();
		}
		try {
			return Double.parseDouble(String.valueOf(o).trim());
		} catch (Exception e) {
			return 0.0;
		}
	}

	private static int intVal(Object o) {
		if (o instanceof Number n) {
			return n.intValue();
		}
		return (int) longVal(o);
	}

	private static long longVal(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(o).trim());
		} catch (Exception e) {
			return 0L;
		}
	}

	private static int fenAmount(Object amount) {
		if (amount == null) {
			return 0;
		}
		try {
			BigDecimal a = new BigDecimal(String.valueOf(amount).trim());
			return a.multiply(BigDecimal.valueOf(100)).intValue();
		} catch (Exception e) {
			return 0;
		}
	}
}
