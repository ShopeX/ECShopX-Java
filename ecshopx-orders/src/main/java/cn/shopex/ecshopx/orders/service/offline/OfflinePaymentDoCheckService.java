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

package cn.shopex.ecshopx.orders.service.offline;

import cn.shopex.ecshopx.common.dispatch.NormalOrderPaySuccessDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.OrdersTradeFinishDispatchPublisher;
import cn.shopex.ecshopx.orders.dispatch.OrdersTradeFinishNormalOrderPaySuccessBridgeDispatchListener;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.espier.domain.OfflineBankAccount;
import cn.shopex.ecshopx.espier.mapper.OfflineBankAccountMapper;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OfflinePayment;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OfflinePaymentMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 * Admin offline bank-transfer review ({@code do_check}). Successful approval persists state, sets the trade to
 * {@code SUCCESS}, and publishes the reloaded trade row through the offline-specific
 * {@link OrdersTradeFinishDispatchPublisher} bean (CSV-292 trade-finish bus plane). The
 * synchronous listener {@link OrdersTradeFinishNormalOrderPaySuccessBridgeDispatchListener} may invoke
 * {@link NormalOrderPaySuccessDispatchPublisher} for eligible mall orders; async fan-out is handled by the dispatch
 * layer. This service does not publish normal-order pay-success events directly.
 */
@Service
public class OfflinePaymentDoCheckService {

	private static final ObjectMapper OFFLINE_PAYMENT_SNAKE_ROW =
			new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

	private final StringRedisTemplate companysRedisTemplate;
	private final StringRedisTemplate sharedStringRedisTemplate;
	private final OfflinePaymentMapper offlinePaymentMapper;
	private final OfflineBankAccountMapper offlineBankAccountMapper;
	private final NormalOrdersMapper normalOrdersMapper;
	private final TradeMapper tradeMapper;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;
	private final OrdersTradeFinishDispatchPublisher ordersTradeFinishDispatchPublisher;
	private final ObjectMapper objectMapper;
	private final TransactionTemplate transactionTemplate;

	public OfflinePaymentDoCheckService(
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			OfflinePaymentMapper offlinePaymentMapper,
			OfflineBankAccountMapper offlineBankAccountMapper,
			NormalOrdersMapper normalOrdersMapper,
			TradeMapper tradeMapper,
			OrderProcessLogPublishPort orderProcessLogPublishPort,
			@Qualifier("ordersTradeFinishDispatchPublisherOfflineCsv292")
					OrdersTradeFinishDispatchPublisher ordersTradeFinishDispatchPublisher,
			ObjectMapper objectMapper,
			TransactionTemplate transactionTemplate) {
		this.companysRedisTemplate = companysRedisTemplate;
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.offlinePaymentMapper = offlinePaymentMapper;
		this.offlineBankAccountMapper = offlineBankAccountMapper;
		this.normalOrdersMapper = normalOrdersMapper;
		this.tradeMapper = tradeMapper;
		this.orderProcessLogPublishPort = orderProcessLogPublishPort;
		this.ordersTradeFinishDispatchPublisher = ordersTradeFinishDispatchPublisher;
		this.objectMapper = objectMapper;
		this.transactionTemplate = transactionTemplate;
	}

	public Map<String, Object> doCheck(
			long companyId,
			long operatorId,
			String operatorType,
			String operatorName,
			Map<String, Object> body) {
		long id = parseLong(body, "id", 0L);
		if (id <= 0) {
			throw new BadRequestException("审核参数错误");
		}
		String orderIdRaw = trimToEmpty(body.get("order_id"));
		if (orderIdRaw.isEmpty() || "0".equals(orderIdRaw)) {
			throw new BadRequestException("审核参数错误");
		}
		long orderIdNum;
		try {
			orderIdNum = Long.parseLong(orderIdRaw);
		} catch (NumberFormatException e) {
			throw new BadRequestException("审核参数错误");
		}
		int cs = parseInt(body, "check_status", 0);
		if (cs != 1 && cs != 2) {
			throw new ResourceException("审核状态错误");
		}

		String refuseRemarkTruncated = null;
		if (cs == 2) {
			String remarkRaw = trimToEmpty(body.get("remark"));
			if (remarkRaw.isEmpty()) {
				throw new ResourceException("请输入审核说明");
			}
			refuseRemarkTruncated = truncateByCodePoints(remarkRaw, 500);
		}

		Long bankAccountId = parseBankAccountId(body);
		if (bankAccountId == null || bankAccountId <= 0) {
			throw new BadRequestException("请输入收款账户id");
		}
		if (!body.containsKey("pay_fee")) {
			throw new BadRequestException("转账金额必须是数字");
		}
		BigDecimal payFeeBd = parsePayFeeDecimal(body.get("pay_fee"));

		String lockKey = "offline_pay_check:" + id;
		String busyVal = companysRedisTemplate.opsForValue().get(lockKey);
		String v = busyVal == null ? "" : busyVal.trim();
		if (!v.isEmpty() && !"0".equals(v)) {
			throw new ResourceException("系统繁忙，请稍后再试");
		}
		companysRedisTemplate.opsForValue().set(lockKey, "1", Duration.ofSeconds(3));
		try {
			OfflinePayment row = offlinePaymentMapper.selectById(id);
			if (row == null) {
				throw new ResourceException("审核数据不存在");
			}
			if (row.getCompanyId() == null || row.getCompanyId() != companyId) {
				throw new ResourceException("审核数据不存在");
			}
			int rowCs = row.getCheckStatus() == null ? 0 : row.getCheckStatus();
			if (rowCs != 0) {
				throw new ResourceException("订单已审核，请勿重复操作");
			}
			if (row.getOrderId() == null || !row.getOrderId().equals(orderIdNum)) {
				throw new ResourceException("审核数据不存在");
			}
			OfflineBankAccount acc = offlineBankAccountMapper.selectOne(new LambdaQueryWrapper<OfflineBankAccount>()
					.eq(OfflineBankAccount::getId, bankAccountId)
					.eq(OfflineBankAccount::getCompanyId, companyId));
			if (acc == null) {
				throw new ResourceException("收款账户不存在");
			}
			body.put("bank_account_name", acc.getBankAccountName());
			body.put("bank_account_no", acc.getBankAccountNo());
			body.put("bank_name", acc.getBankName());
			body.put("china_ums_no", acc.getChinaUmsNo());

			try {
				if (cs == 1) {
					executeConfirm(
							id, companyId, orderIdNum, operatorId, operatorType, operatorName, body, payFeeBd);
				} else {
					executeRefuse(
							id,
							companyId,
							orderIdNum,
							operatorId,
							operatorType,
							operatorName,
							refuseRemarkTruncated,
							body,
							bankAccountId);
				}
			} catch (ResourceException e) {
				throw e;
			} catch (RuntimeException e) {
				throw new ResourceException(e.getMessage() != null ? e.getMessage() : "操作失败");
			}
		} finally {
			companysRedisTemplate.opsForValue().set(lockKey, "0", Duration.ofSeconds(3));
		}

		OfflinePayment latest = offlinePaymentMapper.selectById(id);
		return OFFLINE_PAYMENT_SNAKE_ROW.convertValue(latest, new TypeReference<>() {});
	}

	/**
	 * Persists offline approval, moves the trade to {@code SUCCESS}, emits order-process logs, then publishes
	 * trade-finish via {@link OrdersTradeFinishDispatchPublisher#publish(Map)} within the same transaction callback.
	 * Other synchronous trade-finish listeners (including work-wechat routing and the normal-order pay-success bridge)
	 * observe that publish.
	 */
	private void executeConfirm(
			long id,
			long companyId,
			long orderIdNum,
			long operatorId,
			String operatorType,
			String operatorName,
			Map<String, Object> body,
			BigDecimal payFeeBd) {
		String operatorNameStored = operatorName == null ? "" : operatorName;
		transactionTemplate.executeWithoutResult(status -> {
			try {
				confirmUpdateOfflinePayment(id, companyId, operatorNameStored, body, payFeeBd);
			} catch (JsonProcessingException e) {
				throw new ResourceException(e.getMessage() != null ? e.getMessage() : "操作失败");
			}
			normalOrdersMapper.update(
					null,
					new LambdaUpdateWrapper<NormalOrders>()
							.eq(NormalOrders::getOrderId, orderIdNum)
							.eq(NormalOrders::getCompanyId, companyId)
							.set(NormalOrders::getOfflinePaymentStatus, 1));
			publishOrderProcessLog(
					orderIdNum,
					companyId,
					operatorType,
					operatorId,
					"线下转账审核通过",
					"审核通过",
					buildFirstConfirmParams(id, orderIdNum, operatorNameStored, body));
			String tradeId = updateTradeStatus(companyId, orderIdNum, operatorId, operatorType);
			Trade reloaded = tradeMapper.selectById(tradeId);
			Map<String, Object> tradeRow =
					OFFLINE_PAYMENT_SNAKE_ROW.convertValue(reloaded, new TypeReference<>() {});
			ordersTradeFinishDispatchPublisher.publish(tradeRow);
		});
	}

	private void executeRefuse(
			long id,
			long companyId,
			long orderIdNum,
			long operatorId,
			String operatorType,
			String operatorName,
			String refuseRemarkTruncated,
			Map<String, Object> body,
			Long bankAccountId) {
		transactionTemplate.executeWithoutResult(status -> {
			offlinePaymentMapper.update(
					null,
					new LambdaUpdateWrapper<OfflinePayment>()
							.eq(OfflinePayment::getId, id)
							.eq(OfflinePayment::getCompanyId, companyId)
							.set(OfflinePayment::getCheckStatus, 2)
							.set(OfflinePayment::getRemark, refuseRemarkTruncated)
							.set(OfflinePayment::getOperatorName, operatorName)
							.set(OfflinePayment::getBankAccountId, bankAccountId)
							.set(OfflinePayment::getBankAccountName, nullableString(body.get("bank_account_name")))
							.set(OfflinePayment::getBankAccountNo, nullableString(body.get("bank_account_no")))
							.set(OfflinePayment::getBankName, nullableString(body.get("bank_name")))
							.set(OfflinePayment::getChinaUmsNo, nullableString(body.get("china_ums_no"))));
			normalOrdersMapper.update(
					null,
					new LambdaUpdateWrapper<NormalOrders>()
							.eq(NormalOrders::getOrderId, orderIdNum)
							.eq(NormalOrders::getCompanyId, companyId)
							.set(NormalOrders::getOfflinePaymentStatus, 2));
			Map<String, Object> params = new LinkedHashMap<>();
			params.put("check_status", 2);
			params.put("offline_payment_id", id);
			params.put("order_id", orderIdNum);
			params.put("remark", refuseRemarkTruncated);
			params.put("operator_name", operatorName == null ? "" : operatorName);
			publishOrderProcessLog(
					orderIdNum,
					companyId,
					operatorType,
					operatorId,
					"线下转账审核拒绝",
					"审核拒绝",
					params);
		});
	}

	private static String nullableString(Object v) {
		return v == null ? null : String.valueOf(v);
	}

	private void confirmUpdateOfflinePayment(
			long id, long companyId, String operatorName, Map<String, Object> body, BigDecimal payFeeBd)
			throws JsonProcessingException {
		LambdaUpdateWrapper<OfflinePayment> uw = new LambdaUpdateWrapper<>();
		uw.eq(OfflinePayment::getId, id).eq(OfflinePayment::getCompanyId, companyId);
		uw.set(OfflinePayment::getCheckStatus, 1);
		uw.set(OfflinePayment::getOperatorName, operatorName);
		if (body.containsKey("pay_fee")) {
			long cents = payFeeBd.setScale(0, RoundingMode.HALF_UP).longValue();
			uw.set(OfflinePayment::getPayFee, cents);
		}
		if (body.containsKey("bank_account_id")) {
			uw.set(OfflinePayment::getBankAccountId, toLong(body.get("bank_account_id")));
		}
		setOfflineColumnIfPresent(uw, body, "bank_account_name", OfflinePayment::getBankAccountName);
		setOfflineColumnIfPresent(uw, body, "bank_account_no", OfflinePayment::getBankAccountNo);
		setOfflineColumnIfPresent(uw, body, "bank_name", OfflinePayment::getBankName);
		setOfflineColumnIfPresent(uw, body, "china_ums_no", OfflinePayment::getChinaUmsNo);
		setOfflineColumnIfPresent(uw, body, "pay_account_name", OfflinePayment::getPayAccountName);
		setOfflineColumnIfPresent(uw, body, "pay_account_bank", OfflinePayment::getPayAccountBank);
		setOfflineColumnIfPresent(uw, body, "pay_account_no", OfflinePayment::getPayAccountNo);
		setOfflineColumnIfPresent(uw, body, "pay_sn", OfflinePayment::getPaySn);
		setOfflineColumnIfPresent(uw, body, "voucher_pic", OfflinePayment::getVoucherPic);
		setOfflineColumnIfPresent(uw, body, "transfer_remark", OfflinePayment::getTransferRemark);
		setOfflineColumnIfPresent(uw, body, "remark", OfflinePayment::getRemark);
		offlinePaymentMapper.update(null, uw);
	}

	private void setOfflineColumnIfPresent(
			LambdaUpdateWrapper<OfflinePayment> uw, Map<String, Object> body, String key, com.baomidou.mybatisplus.core.toolkit.support.SFunction<OfflinePayment, ?> column)
			throws JsonProcessingException {
		if (!body.containsKey(key)) {
			return;
		}
		Object raw = body.get(key);
		if (raw == null) {
			uw.set(column, null);
			return;
		}
		if (raw instanceof Map || raw instanceof Collection || raw.getClass().isArray()) {
			uw.set(column, objectMapper.writeValueAsString(raw));
		} else {
			uw.set(column, String.valueOf(raw));
		}
	}

	private Map<String, Object> buildFirstConfirmParams(
			long id, long orderIdNum, String operatorName, Map<String, Object> body) {
		Map<String, Object> p = new LinkedHashMap<>();
		p.put("check_status", body.containsKey("check_status") ? body.get("check_status") : null);
		p.put("offline_payment_id", id);
		p.put("order_id", orderIdNum);
		p.put("pay_fee", body.containsKey("pay_fee") ? body.get("pay_fee") : null);
		p.put("bank_account_id", body.containsKey("bank_account_id") ? body.get("bank_account_id") : null);
		p.put("bank_account_name", body.containsKey("bank_account_name") ? body.get("bank_account_name") : null);
		p.put("bank_account_no", body.containsKey("bank_account_no") ? body.get("bank_account_no") : null);
		p.put("bank_name", body.containsKey("bank_name") ? body.get("bank_name") : null);
		p.put("china_ums_no", body.containsKey("china_ums_no") ? body.get("china_ums_no") : null);
		p.put("pay_account_name", body.containsKey("pay_account_name") ? body.get("pay_account_name") : null);
		p.put("pay_account_bank", body.containsKey("pay_account_bank") ? body.get("pay_account_bank") : null);
		p.put("pay_account_no", body.containsKey("pay_account_no") ? body.get("pay_account_no") : null);
		p.put("pay_sn", body.containsKey("pay_sn") ? body.get("pay_sn") : null);
		p.put("voucher_pic", body.containsKey("voucher_pic") ? body.get("voucher_pic") : null);
		p.put("transfer_remark", body.containsKey("transfer_remark") ? body.get("transfer_remark") : null);
		p.put("operator_name", operatorName);
		p.put("remark", body.containsKey("remark") ? body.get("remark") : null);
		return p;
	}

	private String updateTradeStatus(long companyId, long orderIdNum, long operatorId, String operatorType) {
		Trade trade = tradeMapper.selectOne(new LambdaQueryWrapper<Trade>()
				.eq(Trade::getCompanyId, String.valueOf(companyId))
				.eq(Trade::getOrderId, String.valueOf(orderIdNum))
				.last("LIMIT 1"));
		if (trade == null) {
			throw new ResourceException("交易单不存在");
		}
		if (!"NOTPAY".equals(trade.getTradeState())) {
			throw new ResourceException("更新已处理，不需要更新");
		}
		String tradeId = trade.getTradeId();
		String tradeNo = buildTradeNoSuffix(trade);
		int nowSec = (int) (System.currentTimeMillis() / 1000L);
		LambdaUpdateWrapper<Trade> uw = new LambdaUpdateWrapper<>();
		uw.eq(Trade::getTradeId, tradeId)
				.eq(Trade::getTradeState, "NOTPAY")
				.set(Trade::getTradeState, "SUCCESS")
				.set(Trade::getTimeExpire, String.valueOf(nowSec))
				.set(Trade::getTradeNo, tradeNo);
		int updated = tradeMapper.update(null, uw);
		if (updated <= 0) {
			throw new ResourceException("更新订单不存在");
		}
		Map<String, Object> params2 = new LinkedHashMap<>();
		params2.put("trade_id", tradeId);
		params2.put("order_id", orderIdNum);
		params2.put("company_id", companyId);
		params2.put("trade_state", "SUCCESS");
		publishOrderProcessLog(
				orderIdNum,
				companyId,
				operatorType,
				operatorId,
				"交易支付成功",
				"NOTPAY→SUCCESS",
				params2);
		return tradeId;
	}

	private String buildTradeNoSuffix(Trade trade) {
		String companyIdStr = trade.getCompanyId() == null ? "0" : trade.getCompanyId();
		String distributorId = trade.getDistributorId() == null ? "0" : trade.getDistributorId();
		String orderId = trade.getOrderId() == null ? "" : trade.getOrderId();
		long seq = nextTodayTradeSequence(companyIdStr, distributorId, orderId);
		String md = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMdd"));
		return md + "-" + seq;
	}

	private long nextTodayTradeSequence(String companyId, String distributorId, String orderId) {
		String today = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMdd"));
		String hKey = "h_trade_no_" + companyId + "_" + distributorId + "_" + today;
		String cKey = "c_trade_no_" + companyId + "_" + distributorId + "_" + today;
		String existing = (String) sharedStringRedisTemplate.opsForHash().get(hKey, orderId);
		if (StringUtils.hasText(existing)) {
			return Long.parseLong(existing);
		}
		Long count = sharedStringRedisTemplate.opsForValue().increment(cKey);
		if (count == null) {
			count = 1L;
		}
		sharedStringRedisTemplate.opsForHash().put(hKey, orderId, String.valueOf(count));
		sharedStringRedisTemplate.expire(hKey, Duration.ofSeconds(86400));
		sharedStringRedisTemplate.expire(cKey, Duration.ofSeconds(86400));
		return count;
	}

	private void publishOrderProcessLog(
			long orderId,
			long companyId,
			String operatorType,
			long operatorId,
			String remarks,
			String detail,
			Map<String, Object> params) {
		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("order_id", orderId);
		entities.put("company_id", companyId);
		entities.put("operator_type", operatorType == null ? "" : operatorType);
		entities.put("operator_id", operatorId);
		entities.put("remarks", remarks);
		entities.put("detail", detail);
		entities.put("params", params);
		entities.put("is_show", true);
		orderProcessLogPublishPort.publish(entities);
	}

	private static long toLong(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(v).trim());
	}

	private static long parseLong(Map<String, Object> body, String key, long def) {
		if (!body.containsKey(key)) {
			return def;
		}
		Object v = body.get(key);
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static int parseInt(Map<String, Object> body, String key, int def) {
		if (!body.containsKey(key)) {
			return def;
		}
		Object v = body.get(key);
		if (v == null) {
			return def;
		}
		if (v instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}

	private static Long parseBankAccountId(Map<String, Object> body) {
		if (!body.containsKey("bank_account_id")) {
			return null;
		}
		Object v = body.get("bank_account_id");
		if (v == null) {
			return null;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(String.valueOf(v).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static BigDecimal parsePayFeeDecimal(Object v) {
		if (v == null) {
			throw new BadRequestException("转账金额必须是数字");
		}
		String s = String.valueOf(v).trim();
		if (s.isEmpty()) {
			throw new BadRequestException("转账金额必须是数字");
		}
		try {
			return new BigDecimal(s);
		} catch (NumberFormatException e) {
			throw new BadRequestException("转账金额必须是数字");
		}
	}

	private static String trimToEmpty(Object v) {
		return v == null ? "" : String.valueOf(v).trim();
	}

	private static String truncateByCodePoints(String s, int maxCodePoints) {
		return s.codePoints()
				.limit(maxCodePoints)
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
				.toString();
	}
}
