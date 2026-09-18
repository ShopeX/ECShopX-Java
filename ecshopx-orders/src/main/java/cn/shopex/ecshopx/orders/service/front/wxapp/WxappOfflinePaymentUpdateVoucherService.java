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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.espier.domain.OfflineBankAccount;
import cn.shopex.ecshopx.espier.mapper.OfflineBankAccountMapper;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OfflinePayment;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OfflinePaymentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class WxappOfflinePaymentUpdateVoucherService {

	private final OfflinePaymentMapper offlinePaymentMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final OfflineBankAccountMapper offlineBankAccountMapper;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;
	private final ObjectMapper objectMapper;

	public WxappOfflinePaymentUpdateVoucherService(
			OfflinePaymentMapper offlinePaymentMapper,
			NormalOrdersMapper normalOrdersMapper,
			OfflineBankAccountMapper offlineBankAccountMapper,
			OrderProcessLogPublishPort orderProcessLogPublishPort,
			ObjectMapper objectMapper) {
		this.offlinePaymentMapper = offlinePaymentMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.offlineBankAccountMapper = offlineBankAccountMapper;
		this.orderProcessLogPublishPort = orderProcessLogPublishPort;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> updateOfflineVoucher(
			HttpServletRequest request, Map<String, Object> merged, Map<String, Object> auth) {
		long companyId = longVal(auth.get("company_id"));
		long userId = longVal(auth.get("user_id"));
		if (companyId <= 0L || userId <= 0L) {
			throw new UnauthorizedException("未登录");
		}
		merged.put("company_id", companyId);
		merged.put("user_id", userId);

		long paymentId = parseRequiredPositiveLong(merged.get("id"), "凭证id缺少！");
		long orderId = parseRequiredPositiveLong(merged.get("order_id"), "订单号缺少！");
		long bankAccountId = parseRequiredPositiveLong(merged.get("bank_account_id"), "收款账户id缺少！");

		String voucherPicJson = validateAndSerializeVoucherPic(merged.get("voucher_pic"));

		String rawRemark = merged.get("transfer_remark") == null ? "" : String.valueOf(merged.get("transfer_remark"));
		String transferRemark = truncateByCodePoints(rawRemark, 100);

		OfflinePayment info = offlinePaymentMapper.selectById(paymentId);
		if (info == null) {
			throw new ResourceException("凭证不存在");
		}

		NormalOrders order = normalOrdersMapper.selectOne(
				new LambdaQueryWrapper<NormalOrders>()
						.eq(NormalOrders::getCompanyId, companyId)
						.eq(NormalOrders::getOrderId, orderId));
		if (order == null) {
			throw new ResourceException("订单不存在");
		}
		if (!"NOTPAY".equals(order.getOrderStatus())) {
			throw new ResourceException("订单已支付，请勿重复操作");
		}
		if (!"offline_pay".equals(order.getPayType())) {
			throw new ResourceException("订单支付方式错误");
		}

		boolean promoterTruthy =
				merged.get("promoter_user_id") != null && isTruthyLoose(merged.get("promoter_user_id"));
		if (promoterTruthy) {
			long promoterId = toLongLoose(merged.get("promoter_user_id"));
			Long salesmanId = order.getSalesmanId() == null ? 0L : order.getSalesmanId();
			if (!Objects.equals(salesmanId, promoterId)) {
				throw new ResourceException("订单用户错误");
			}
		} else {
			if (!Objects.equals(order.getUserId(), userId)) {
				throw new ResourceException("订单用户错误");
			}
		}

		Integer cs = info.getCheckStatus();
		if (!Objects.equals(cs, Integer.valueOf(2))) {
			throw new ResourceException("不能修改凭证");
		}

		OfflineBankAccount acc = offlineBankAccountMapper.selectOne(
				new LambdaQueryWrapper<OfflineBankAccount>()
						.eq(OfflineBankAccount::getCompanyId, companyId)
						.eq(OfflineBankAccount::getId, bankAccountId));
		if (acc == null) {
			throw new ResourceException("收款账户不存在");
		}

		long orderTotalFee = OfflinePaymentPayFeeSupport.parseTotalFeeToLong(order.getTotalFee());
		int now = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<OfflinePayment> uw = new LambdaUpdateWrapper<>();
		uw.eq(OfflinePayment::getId, paymentId);
		uw.set(OfflinePayment::getCompanyId, companyId);
		uw.set(OfflinePayment::getUserId, userId);
		uw.set(OfflinePayment::getOrderId, orderId);
		uw.set(OfflinePayment::getBankAccountId, acc.getId());
		uw.set(OfflinePayment::getBankAccountName, nullToEmpty(acc.getBankAccountName()));
		uw.set(OfflinePayment::getBankAccountNo, nullToEmpty(acc.getBankAccountNo()));
		uw.set(OfflinePayment::getBankName, nullToEmpty(acc.getBankName()));
		uw.set(OfflinePayment::getChinaUmsNo, nullToEmpty(acc.getChinaUmsNo()));
		uw.set(OfflinePayment::getCheckStatus, 0);
		uw.set(OfflinePayment::getPayFee, OfflinePaymentPayFeeSupport.resolveUploadPayFee(merged.get("pay_fee"), orderTotalFee));
		uw.set(OfflinePayment::getVoucherPic, voucherPicJson);
		uw.set(OfflinePayment::getTransferRemark, transferRemark);
		uw.set(OfflinePayment::getUpdateTime, now);
		if (merged.containsKey("pay_account_name")) {
			uw.set(OfflinePayment::getPayAccountName, stringFromMerged(merged, "pay_account_name"));
		}
		if (merged.containsKey("pay_account_bank")) {
			uw.set(OfflinePayment::getPayAccountBank, stringFromMerged(merged, "pay_account_bank"));
		}
		if (merged.containsKey("pay_account_no")) {
			uw.set(OfflinePayment::getPayAccountNo, stringFromMerged(merged, "pay_account_no"));
		}
		if (merged.containsKey("pay_sn")) {
			uw.set(OfflinePayment::getPaySn, stringFromMerged(merged, "pay_sn"));
		}

		int rows = offlinePaymentMapper.update(null, uw);
		if (rows == 0) {
			throw new ResourceException("未查询到更新数据");
		}

		OfflinePayment updated = offlinePaymentMapper.selectById(paymentId);
		if (updated == null) {
			throw new ResourceException("未查询到更新数据");
		}

		int checkStatus = updated.getCheckStatus() == null ? 0 : updated.getCheckStatus();
		int n = normalOrdersMapper.update(
				null,
				new LambdaUpdateWrapper<NormalOrders>()
						.eq(NormalOrders::getOrderId, updated.getOrderId())
						.set(NormalOrders::getOfflinePaymentStatus, checkStatus));
		if (n == 0) {
			throw new ResourceException("未查询到更新数据");
		}

		Map<String, Object> logMap = new LinkedHashMap<>();
		logMap.put("order_id", order.getOrderId());
		logMap.put("company_id", order.getCompanyId());
		Integer supplierId = order.getSupplierId();
		logMap.put("supplier_id", supplierId == null ? 0L : supplierId.longValue());
		logMap.put("operator_type", "user");
		logMap.put("operator_id", order.getUserId() == null ? 0L : order.getUserId());
		logMap.put("remarks", "线下转账提交");
		logMap.put("detail", "订单号：" + order.getOrderId() + "，线下转账信息提交");
		logMap.put("params", new LinkedHashMap<>(merged));
		orderProcessLogPublishPort.publish(logMap);

		return toResponseRow(updated);
	}

	private String validateAndSerializeVoucherPic(Object raw) {
		if (raw == null) {
			throw new ResourceException("凭证图片缺少！");
		}
		List<Object> asList;
		if (raw instanceof Collection<?> c) {
			if (c.isEmpty()) {
				throw new ResourceException("凭证图片缺少！");
			}
			asList = new ArrayList<>(c);
		} else if (raw instanceof String s) {
			try {
				asList = objectMapper.readValue(s, new TypeReference<List<Object>>() {});
			} catch (Exception e) {
				throw new ResourceException("凭证图片缺少！");
			}
			if (asList == null || asList.isEmpty()) {
				throw new ResourceException("凭证图片缺少！");
			}
		} else {
			throw new ResourceException("凭证图片缺少！");
		}
		for (Object elem : asList) {
			String t = elem == null ? "" : String.valueOf(elem).trim();
			if (t.isEmpty()) {
				throw new ResourceException("凭证图片缺少！");
			}
		}
		try {
			return objectMapper.writeValueAsString(asList);
		} catch (JsonProcessingException e) {
			throw new ResourceException("凭证图片缺少！");
		}
	}

	private long parseRequiredPositiveLong(Object raw, String message) {
		if (raw == null) {
			throw new ResourceException(message);
		}
		String s = String.valueOf(raw).trim();
		if (s.isEmpty() || "0".equals(s)) {
			throw new ResourceException(message);
		}
		long v;
		try {
			v = Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new ResourceException(message);
		}
		if (v <= 0L) {
			throw new ResourceException(message);
		}
		return v;
	}

	private Map<String, Object> toResponseRow(OfflinePayment row) {
		ObjectMapper rowMapper =
				objectMapper.copy().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
		Map<String, Object> snakeRow = rowMapper.convertValue(row, new TypeReference<Map<String, Object>>() {});
		LinkedHashMap<String, Object> out = new LinkedHashMap<>(snakeRow);

		String voucherRaw = row.getVoucherPic();
		List<Object> voucherList;
		if (voucherRaw == null || voucherRaw.isEmpty()) {
			voucherList = Collections.emptyList();
		} else {
			try {
				voucherList = objectMapper.readValue(voucherRaw, new TypeReference<List<Object>>() {});
			} catch (Exception e) {
				voucherList = Collections.emptyList();
			}
		}
		out.put("voucher_pic", voucherList);
		return out;
	}

	private static long longVal(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static boolean isTruthyLoose(Object v) {
		if (v == null) {
			return false;
		}
		if (v instanceof Boolean b) {
			return b.booleanValue();
		}
		if (v instanceof Number n) {
			return n.longValue() != 0L;
		}
		if (v instanceof String s) {
			String t = s.trim();
			return !t.isEmpty() && !"0".equals(t);
		}
		String t = String.valueOf(v).trim();
		return !t.isEmpty() && !"0".equals(t);
	}

	private static long toLongLoose(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static String truncateByCodePoints(String s, int maxCodePoints) {
		return s.codePoints()
				.limit(maxCodePoints)
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
				.toString();
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	private static String stringFromMerged(Map<String, Object> merged, String key) {
		Object v = merged.get(key);
		if (v == null) {
			return "";
		}
		return String.valueOf(v).trim();
	}
}
