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

package cn.shopex.ecshopx.bspay.service;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.bspay.domain.DivFee;
import cn.shopex.ecshopx.bspay.domain.PaymemtConfirm;
import cn.shopex.ecshopx.bspay.mapper.DivFeeMapper;
import cn.shopex.ecshopx.bspay.mapper.PaymemtConfirmMapper;
import cn.shopex.ecshopx.common.cron.payment.BspayPaymentConfirmHttpGateway;
import cn.shopex.ecshopx.common.port.payment.BspayPaymentSettingsReadPort;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class BspayAutoCloseAftersalesPaymentConfirmationService {

	public static final String TRADE_PENDING = "P";
	public static final String TRADE_FAIL = "F";

	private final NormalOrdersItemsMapper normalOrdersItemsMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final TradeMapper tradeMapper;
	private final AftersalesRefundMapper aftersalesRefundMapper;
	private final PaymemtConfirmMapper paymemtConfirmMapper;
	private final DivFeeMapper divFeeMapper;
	private final DistributorMapper distributorMapper;
	private final BspayPaymentSettingsReadPort bspayPaymentSettingsReadPort;
	private final BspayHuifuIdResolver bspayHuifuIdResolver;
	private final BspayPaymentConfirmHttpGateway bspayPaymentConfirmHttpGateway;
	private final ObjectMapper objectMapper;

	@Value("${ecshopx.bspay.headquarters-fee-mode:1}")
	private String headquartersFeeMode;

	public void scheduleAutoPaymentConfirmation(long companyId, long orderId) {
		List<NormalOrdersItems> items =
				normalOrdersItemsMapper.selectList(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, companyId)
								.eq(NormalOrdersItems::getOrderId, orderId));
		boolean allClosed = true;
		for (var v : items) {
			if (!"CLOSED".equals(v.getAftersalesStatus())) {
				allClosed = false;
				break;
			}
		}
		if (!allClosed) {
			return;
		}
		int now = (int) (System.currentTimeMillis() / 1000L);
		PaymemtConfirm ins = new PaymemtConfirm();
		ins.setCompanyId(companyId);
		ins.setOrderId(String.valueOf(orderId));
		ins.setStatus(TRADE_PENDING);
		ins.setCreated(now);
		ins.setUpdated(now);
		paymemtConfirmMapper.insert(ins);
		paymentConfirmation(companyId, orderId, ins);
	}

	/**
	 * 扫描待重试的确认行（P 且创建满 10 分钟）并逐条执行确认；返回本批查询行数。
	 */
	public int scheduleRetryBsPayConfirm() {
		int now = (int) (System.currentTimeMillis() / 1000L);
		int notAfter = now - 600;
		List<PaymemtConfirm> list =
				paymemtConfirmMapper.selectList(
						new LambdaQueryWrapper<PaymemtConfirm>()
								.eq(PaymemtConfirm::getStatus, TRADE_PENDING)
								.le(PaymemtConfirm::getCreated, notAfter));
		if (list == null || list.isEmpty()) {
			return 0;
		}
		int n = list.size();
		for (PaymemtConfirm val : list) {
			long companyId = val.getCompanyId() == null ? 0L : val.getCompanyId();
			String orderIdRaw = val.getOrderId();
			long orderId = Long.parseLong(orderIdRaw == null ? "0" : orderIdRaw.trim());
			paymentConfirmation(companyId, orderId, val);
		}
		return n;
	}

	private void paymentConfirmation(long companyId, long orderId, PaymemtConfirm confirmationData) {
		NormalOrders orderInfo =
				normalOrdersMapper.selectOne(
						new LambdaQueryWrapper<NormalOrders>()
								.eq(NormalOrders::getCompanyId, companyId)
								.eq(NormalOrders::getOrderId, orderId)
								.last("LIMIT 1"));
		if (orderInfo == null) {
			log.debug("斗拱支付确认::失败，订单不存在 => {}", orderId);
			return;
		}
		if (!"bspay".equals(orderInfo.getPayType())) {
			log.debug("斗拱支付确认::失败，支付方式只支持斗拱 => {}", orderId);
			return;
		}
		Trade tradeInfo =
				tradeMapper.selectOne(
						new LambdaQueryWrapper<Trade>()
								.eq(Trade::getCompanyId, String.valueOf(companyId))
								.eq(Trade::getOrderId, String.valueOf(orderId))
								.eq(Trade::getTradeState, "SUCCESS")
								.eq(Trade::getPayType, "bspay")
								.last("LIMIT 1"));
		if (tradeInfo == null) {
			log.debug("斗拱支付确认::失败 => 该订单的交易单不存在:{}", orderId);
			return;
		}
		if (!"bspay".equals(tradeInfo.getPayType())) {
			log.debug("斗拱支付确认::失败 => 支付方式只支持斗拱:{}", tradeInfo.getTradeId());
			return;
		}
		if (!StringUtils.hasText(tradeInfo.getTransactionId())) {
			log.debug("斗拱支付确认::失败 => 该交易单没有transaction_id:{}", tradeInfo.getTradeId());
			return;
		}
		int totalFeeFen = tradeInfo.getTotalFee() == null ? 0 : tradeInfo.getTotalFee();
		for (AftersalesRefund r : listRefunds(companyId, orderId)) {
			totalFeeFen -= r.getRefundFee() == null ? 0 : r.getRefundFee();
		}
		if (totalFeeFen <= 0) {
			return;
		}
		BigDecimal totalFeeYuan = BigDecimal.valueOf(totalFeeFen).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
		String payChannel = tradeInfo.getPayChannel() == null ? "" : tradeInfo.getPayChannel();
		BigDecimal feeRate = resolveBspayFeeRate(companyId, payChannel);
		if (feeRate == null) {
			log.debug("斗拱支付确认::getDivMember::失败 => 没有设置费率");
			return;
		}
		BigDecimal feeAmtYuan = totalFeeYuan.multiply(feeRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
		Map<String, Object> divResult = buildDivMember(orderInfo, tradeInfo, totalFeeYuan, feeAmtYuan);
		if (divResult == null || divResult.get("div_members") == null) {
			return;
		}
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> original =
				(List<Map<String, Object>>) divResult.get("div_members");
		if (original == null || original.isEmpty()) {
			return;
		}
		List<Map<String, Object>> acct = new ArrayList<>();
		for (Map<String, Object> m : original) {
			if (!"0.00".equals(String.valueOf(m.get("div_amt")))) {
				acct.add(m);
			}
		}
		Map<String, Object> setting = bspayPaymentSettingsReadPort.requireSettingMap(companyId);
		String reqSeq = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
						.format(java.time.LocalDateTime.now())
				+ ThreadLocalRandom.current().nextInt(100000, 1000000);
		LinkedHashMap<String, Object> obj = new LinkedHashMap<>();
		obj.put("company_id", companyId);
		obj.put("req_seq_id", reqSeq);
		obj.put("huifu_id", str(setting.get("sys_id")));
		obj.put("org_req_seq_id", tradeInfo.getTradeId());
		obj.put("org_req_date", tradeInfo.getBspayReqDate() == null ? "" : tradeInfo.getBspayReqDate());
		obj.put("acct_infos", acct);
		Map<String, Object> res = bspayPaymentConfirmHttpGateway.callDelaytransConfirm(obj);
		Map<String, Object> layer = extractData(res);
		String transStat = str(layer.get("trans_stat"));
		int now = (int) (System.currentTimeMillis() / 1000L);
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("company_id", companyId);
		data.put("order_id", orderId);
		data.put("distributor_id", orderInfo.getDistributorId());
		data.put("payment_id", tradeInfo.getTransactionId());
		data.put("order_no", reqSeq);
		data.put("confirm_amt", String.valueOf(totalFeeFen));
		try {
			data.put("div_members", objectMapper.writeValueAsString(obj.get("acct_infos")));
		} catch (Exception e) {
			data.put("div_members", "[]");
		}
		data.put("status", transStat);
		try {
			data.put("request_params", objectMapper.writeValueAsString(obj));
		} catch (Exception e) {
			data.put("request_params", "{}");
		}
		try {
			data.put("response_params", objectMapper.writeValueAsString(layer));
		} catch (Exception e) {
			data.put("response_params", "{}");
		}
		if (TRADE_FAIL.equals(transStat)) {
			confirmUpdate(confirmationData.getId(), data, now);
			return;
		}
		data.put("payment_confirmation_id", str(layer.get("hf_seq_id")));
		confirmUpdate(confirmationData.getId(), data, now);
		int feeFen = feeAmtYuan.multiply(BigDecimal.valueOf(100)).intValue();
		String bspayMode = str(divResult.get("headquarters_fee_mode"));
		tradeMapper.update(
				null,
				new LambdaUpdateWrapper<Trade>()
						.eq(Trade::getTradeId, tradeInfo.getTradeId())
						.set(Trade::getBspayDivMembers, str(data.get("div_members")))
						.set(Trade::getBspayFeeMode, bspayMode)
						.set(Trade::getBspayFee, feeFen)
						.set(Trade::getBspayDivStatus, "DIVED"));
		for (Map<String, Object> m : original) {
			DivFee row = new DivFee();
			row.setTradeId(tradeInfo.getTradeId());
			row.setOrderId(String.valueOf(orderId));
			row.setCompanyId(String.valueOf(companyId));
			if (m.get("distributor_id") != null) {
				row.setDistributorId(String.valueOf(m.get("distributor_id")));
			}
			if (m.get("supplier_id") != null) {
				try {
					row.setSupplierId(Long.parseLong(String.valueOf(m.get("supplier_id"))));
				} catch (Exception ignored) {
				}
			}
			row.setOperatorType(str(m.get("operator_type")));
			row.setPayFee(totalFeeFen);
			row.setDivFee(yuanToFen(str(m.get("div_amt"))));
			row.setHuifuId(str(m.get("huifu_id")));
			if (m.get("merchant_id") != null) {
				row.setMerchantId(String.valueOf(m.get("merchant_id")));
			}
			row.setCreated(now);
			row.setUpdated(now);
			divFeeMapper.insert(row);
		}
	}

	private Map<String, Object> buildDivMember(
			NormalOrders orderInfo, Trade trade, BigDecimal totalFeeYuan, BigDecimal feeAmtYuan) {
		long supCnt =
				normalOrdersItemsMapper.selectCount(
						new LambdaQueryWrapper<NormalOrdersItems>()
								.eq(NormalOrdersItems::getCompanyId, orderInfo.getCompanyId())
								.eq(NormalOrdersItems::getOrderId, orderInfo.getOrderId())
								.ge(NormalOrdersItems::getSupplierId, 1));
		if (supCnt > 0) {
			log.debug("斗拱支付确认::getDivMember: 含供应商子单行，需全量分账演算，暂跳过");
			return null;
		}
		BigDecimal divFeeYuan;
		if ("2".equals(headquartersFeeMode.trim())) {
			divFeeYuan = totalFeeYuan.subtract(feeAmtYuan);
		} else {
			divFeeYuan = totalFeeYuan;
		}
		Map<String, Object> setting = bspayPaymentSettingsReadPort.requireSettingMap(orderInfo.getCompanyId());
		long distId = orderInfo.getDistributorId() == null ? 0L : orderInfo.getDistributorId();
		if (divFeeYuan.compareTo(BigDecimal.ZERO) <= 0 && distId == 0L) {
			return null;
		}
		if (distId == 0L) {
			List<Map<String, Object>> div = new ArrayList<>();
			LinkedHashMap<String, Object> a = new LinkedHashMap<>();
			a.put("operator_type", "admin");
			a.put("huifu_id", str(setting.get("sys_id")));
			a.put("div_amt", divFeeYuan.setScale(2, RoundingMode.HALF_UP).toPlainString());
			div.add(a);
			return result(div, headquartersFeeMode, feeAmtYuan);
		}
		Distributor d =
				distributorMapper.selectOne(
						new LambdaQueryWrapper<Distributor>()
								.eq(Distributor::getCompanyId, orderInfo.getCompanyId())
								.eq(Distributor::getDistributorId, distId)
								.last("LIMIT 1"));
		if (d == null) {
			return null;
		}
		if (d.getMerchantId() != null && d.getMerchantId() > 0L) {
			log.debug("斗拱支付确认::getDivMember: 关联商户分账需扩展分支，暂跳过");
			return null;
		}
		if (!StringUtils.hasText(d.getBspaySplitLedgerInfo())) {
			log.debug("斗拱支付确认::getDivMember::失败 => 未设置分账信息 店铺:{}", distId);
			return null;
		}
		Map<String, Object> split;
		try {
			split = objectMapper.readValue(d.getBspaySplitLedgerInfo(), new TypeReference<>() {});
		} catch (Exception e) {
			return null;
		}
		double hp = dbl(split.get("headquarters_proportion"));
		if (hp == 0.0) {
			log.debug("斗拱支付确认::getDivMember: headquarters_proportion=0 需商品佣金，暂跳过");
			return null;
		}
		BigDecimal hpR = BigDecimal.valueOf(hp).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
		BigDecimal hf = divFeeYuan.multiply(hpR).setScale(2, RoundingMode.HALF_UP);
		BigDecimal distFee = divFeeYuan.subtract(hf).setScale(2, RoundingMode.HALF_UP);
		String dHuifu = bspayHuifuIdResolver.getHuifuId(orderInfo.getCompanyId(), distId, "distributor");
		if (!StringUtils.hasText(dHuifu)) {
			log.debug("斗拱支付确认::getDivMember::失败 => 用户进件未成功 店铺:{}", distId);
			return null;
		}
		List<Map<String, Object>> div = new ArrayList<>();
		LinkedHashMap<String, Object> a1 = new LinkedHashMap<>();
		a1.put("operator_type", "admin");
		a1.put("huifu_id", str(setting.get("sys_id")));
		a1.put("div_amt", hf.toPlainString());
		div.add(a1);
		LinkedHashMap<String, Object> a2 = new LinkedHashMap<>();
		a2.put("operator_type", "distributor");
		a2.put("huifu_id", dHuifu);
		a2.put("div_amt", distFee.toPlainString());
		a2.put("distributor_id", distId);
		div.add(a2);
		return result(div, headquartersFeeMode, feeAmtYuan);
	}

	private Map<String, Object> result(
			List<Map<String, Object>> div, String headquartersMode, BigDecimal feeAmt) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("div_members", div);
		m.put("headquarters_fee_mode", headquartersMode);
		m.put("fee_amt", feeAmt);
		return m;
	}

	private void confirmUpdate(Long id, LinkedHashMap<String, Object> data, int now) {
		if (id == null) {
			return;
		}
		paymemtConfirmMapper.update(
				null,
				new LambdaUpdateWrapper<PaymemtConfirm>()
						.eq(PaymemtConfirm::getId, id)
						.set(PaymemtConfirm::getDistributorId, longObj(data.get("distributor_id")))
						.set(PaymemtConfirm::getPaymentId, str(data.get("payment_id")))
						.set(
								PaymemtConfirm::getPaymentConfirmationId,
								str(data.get("payment_confirmation_id")))
						.set(PaymemtConfirm::getOrderNo, str(data.get("order_no")))
						.set(PaymemtConfirm::getConfirmAmt, str(data.get("confirm_amt")))
						.set(PaymemtConfirm::getDivMembers, str(data.get("div_members")))
						.set(PaymemtConfirm::getStatus, str(data.get("status")))
						.set(PaymemtConfirm::getRequestParams, str(data.get("request_params")))
						.set(PaymemtConfirm::getResponseParams, str(data.get("response_params")))
						.set(PaymemtConfirm::getUpdated, now));
	}

	private List<AftersalesRefund> listRefunds(long companyId, long orderId) {
		return aftersalesRefundMapper.selectList(
				new LambdaQueryWrapper<AftersalesRefund>()
						.eq(AftersalesRefund::getCompanyId, companyId)
						.eq(AftersalesRefund::getOrderId, orderId)
						.in(
								AftersalesRefund::getRefundStatus,
								List.of("SUCCESS", "AUDIT_SUCCESS", "CHANGE")));
	}

	private BigDecimal resolveBspayFeeRate(long companyId, String payChannel) {
		try {
			Map<String, Object> s = bspayPaymentSettingsReadPort.requireSettingMap(companyId);
			return switch (payChannel) {
				case "wx_lite", "wx_pub", "wx_qr" -> toBd(
						s.get(payChannel + "_" + str(s.get("wxpay_fee_type"))));
				case "alipay_wap" -> toBd(s.get("alipay_call"));
				case "alipay_qr" -> toBd(
						s.get("alipay_qr_" + str(s.get("alipay_fee_type"))));
				default -> null;
			};
		} catch (Exception e) {
			return null;
		}
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

	@SuppressWarnings("unchecked")
	private static Map<String, Object> extractData(Map<String, Object> res) {
		if (res == null) {
			return Map.of();
		}
		Object d = res.get("data");
		if (d instanceof Map<?, ?> m) {
			if (m.containsKey("trans_stat")) {
				return (Map<String, Object>) m;
			}
			Object inner = m.get("data");
			if (inner instanceof Map<?, ?> m2) {
				return (Map<String, Object>) m2;
			}
		}
		return Map.of();
	}

	private static int yuanToFen(String yuan) {
		if (!StringUtils.hasText(yuan)) {
			return 0;
		}
		try {
			return new BigDecimal(yuan.trim()).multiply(BigDecimal.valueOf(100)).intValue();
		} catch (Exception e) {
			return 0;
		}
	}

	private static long longObj(Object o) {
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
}
