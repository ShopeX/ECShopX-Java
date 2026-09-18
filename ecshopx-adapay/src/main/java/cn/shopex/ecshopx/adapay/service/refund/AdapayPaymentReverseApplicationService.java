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

package cn.shopex.ecshopx.adapay.service.refund;

import cn.shopex.ecshopx.adapay.AdapaySdkSync;
import cn.shopex.ecshopx.adapay.domain.AdapayPaymemtConfirm;
import cn.shopex.ecshopx.adapay.domain.AdapayPaymentReverse;
import cn.shopex.ecshopx.adapay.mapper.AdapayPaymentReverseMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayPaymemtConfirmMapper;
import cn.shopex.ecshopx.adapay.service.AdapayPaymentSettingRedisReader;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.refund.AftersalesRefundPaymentContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huifu.adapay.Adapay;
import com.huifu.adapay.core.exception.BaseAdaPayException;
import com.huifu.adapay.model.MerConfig;
import com.huifu.adapay.model.PaymentReverse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Orchestrates AdaPay payment reverse for after-sales refunds when no succeeded payment
 * confirmation exists, and publishes the order process log after the reverse outcome is known.
 */
@Service
public class AdapayPaymentReverseApplicationService {

	public static final String PAY_RES_ORDER_PROCESS_LOG_VIA_PAYMENT_REVERSE =
			"order_process_log_via_payment_reverse";

	private final AdapayPaymemtConfirmMapper adapayPaymemtConfirmMapper;
	private final AdapayPaymentSettingRedisReader adapayPaymentSettingRedisReader;
	private final AdapayPaymentReverseMapper adapayPaymentReverseMapper;
	private final OrderProcessLogPublishPort orderProcessLogPublishPort;
	private final boolean prodMode;

	public AdapayPaymentReverseApplicationService(
			AdapayPaymemtConfirmMapper adapayPaymemtConfirmMapper,
			AdapayPaymentSettingRedisReader adapayPaymentSettingRedisReader,
			AdapayPaymentReverseMapper adapayPaymentReverseMapper,
			OrderProcessLogPublishPort orderProcessLogPublishPort,
			@Value("${ecshopx.adapay.prod-mode:true}") boolean prodMode) {
		this.adapayPaymemtConfirmMapper = adapayPaymemtConfirmMapper;
		this.adapayPaymentSettingRedisReader = adapayPaymentSettingRedisReader;
		this.adapayPaymentReverseMapper = adapayPaymentReverseMapper;
		this.orderProcessLogPublishPort = orderProcessLogPublishPort;
		this.prodMode = prodMode;
	}

	/**
	 * Runs payment reverse, persists reverse row on success, publishes order process log, and
	 * returns a payment-result map for the refund pipeline (including dedupe metadata for {@code
	 * AftersalesRefundDoRefundService}).
	 */
	public Map<String, Object> reverseAndPublishOrderProcessLog(AftersalesRefundPaymentContext ctx) {
		Objects.requireNonNull(ctx, "ctx");
		long companyId = ctx.getCompanyId();
		long orderId = ctx.getOrderId();

		AdapayPaymemtConfirm succeededRow =
				adapayPaymemtConfirmMapper.selectOne(
						new LambdaQueryWrapper<AdapayPaymemtConfirm>()
								.eq(AdapayPaymemtConfirm::getCompanyId, companyId)
								.eq(AdapayPaymemtConfirm::getOrderId, String.valueOf(orderId))
								.eq(AdapayPaymemtConfirm::getStatus, "succeeded")
								.last("LIMIT 1"));
		if (succeededRow != null && StringUtils.hasText(succeededRow.getPaymentConfirmationId())) {
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("status", "FAIL");
			m.put("error_desc", "订单已确认支付");
			return m;
		}

		if (!StringUtils.hasText(ctx.getTransactionId())) {
			String err = "支付单号缺失";
			publishOrderProcessLog(companyId, orderId, false, err);
			return failPayRes(err);
		}

		Map<String, Object> cfg = adapayPaymentSettingRedisReader.getPaymentSetting(companyId);
		String appId = str(cfg.get("app_id"));
		String liveKey = str(cfg.get("live_api_key"));
		String testKey = str(cfg.get("test_api_key"));
		String rsaPrivate = str(cfg.get("rsa_private_key"));
		if (!StringUtils.hasText(appId)
				|| !StringUtils.hasText(rsaPrivate)
				|| (!StringUtils.hasText(liveKey) && !StringUtils.hasText(testKey))) {
			String err = "请检查adapay支付配置";
			publishOrderProcessLog(companyId, orderId, false, err);
			return failPayRes(err);
		}

		String merKey = String.valueOf(companyId);
		String orderNo = ctx.getRefundBn() + "_rev_" + (10000 + (int) (Math.random() * 80000)) + System.currentTimeMillis();
		String reverseAmtYuan =
				BigDecimal.valueOf(ctx.getRefundFeeFen())
						.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
						.toPlainString();

		Map<String, Object> params = new LinkedHashMap<>();
		params.put("payment_id", ctx.getTransactionId().trim());
		params.put("order_no", orderNo);
		params.put("reverse_amt", reverseAmtYuan);
		if (StringUtils.hasText(ctx.getPayChannel())) {
			params.put("pay_channel", ctx.getPayChannel());
		}

		Map<String, Object> root;
		try {
			synchronized (AdapaySdkSync.LOCK) {
				Adapay.prodMode = prodMode;
				MerConfig mc = new MerConfig();
				mc.setApiKey(prodMode ? firstNonBlank(liveKey, testKey) : firstNonBlank(testKey, liveKey));
				mc.setApiMockKey(testKey);
				mc.setRSAPrivateKey(rsaPrivate);
				Adapay.addMerConfig(mc, merKey);
				root = PaymentReverse.create(params, merKey);
			}
		} catch (BaseAdaPayException e) {
			String msg = e.getMessage();
			String err = StringUtils.hasText(msg) ? msg : "AdaPay 支付撤销失败";
			publishOrderProcessLog(companyId, orderId, false, err);
			return failPayRes(err);
		} catch (Exception e) {
			String err = "AdaPay 支付撤销失败";
			publishOrderProcessLog(companyId, orderId, false, err);
			return failPayRes(err);
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> data = root == null ? null : (Map<String, Object>) root.get("data");
		if (data == null) {
			String err = "AdaPay 支付撤销失败";
			publishOrderProcessLog(companyId, orderId, false, err);
			return failPayRes(err);
		}
		if ("failed".equalsIgnoreCase(String.valueOf(data.get("status")))) {
			Object errObj = data.get("error_msg");
			if (errObj == null || !StringUtils.hasText(String.valueOf(errObj))) {
				errObj = data.get("error_desc");
			}
			String errorMsg = errObj == null ? "" : String.valueOf(errObj);
			if (!StringUtils.hasText(errorMsg)) {
				errorMsg = "AdaPay 支付撤销失败";
			}
			publishOrderProcessLog(companyId, orderId, false, errorMsg);
			return failPayRes(errorMsg);
		}

		persistReverseRowOnSuccess(ctx, appId, orderNo, reverseAmtYuan, data);
		publishOrderProcessLog(companyId, orderId, true, null);
		Map<String, Object> ok = new LinkedHashMap<>();
		ok.put("status", "SUCCESS");
		ok.put("refund_id", Objects.toString(data.get("id"), orderNo));
		ok.put(PAY_RES_ORDER_PROCESS_LOG_VIA_PAYMENT_REVERSE, Boolean.TRUE);
		return ok;
	}

	private void persistReverseRowOnSuccess(
			AftersalesRefundPaymentContext ctx,
			String appId,
			String orderNo,
			String reverseAmtYuan,
			Map<String, Object> data) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		AdapayPaymentReverse row = new AdapayPaymentReverse();
		row.setCompanyId(ctx.getCompanyId());
		row.setOrderId(String.valueOf(ctx.getOrderId()));
		row.setPaymentId(ctx.getTransactionId());
		row.setPaymentReverseId(Objects.toString(data.get("id"), ""));
		row.setAppId(appId);
		row.setOrderNo(orderNo);
		row.setReverseAmt(reverseAmtYuan);
		row.setStatus(String.valueOf(data.getOrDefault("status", "succeeded")));
		row.setRequestParams("{}");
		row.setResponseParams("{}");
		row.setCreateTime(now);
		row.setUpdateTime(now);
		adapayPaymentReverseMapper.insert(row);
	}

	private void publishOrderProcessLog(long companyId, long orderId, boolean success, String errorMsg) {
		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("order_id", orderId);
		entities.put("company_id", companyId);
		entities.put("operator_type", "system");
		entities.put("remarks", "订单退款");
		if (success) {
			entities.put("detail", "订单号：" + orderId + "，订单支付撤销成功（adapay支付渠道）");
		} else {
			String msg = errorMsg == null ? "" : errorMsg;
			entities.put("detail", "订单号：" + orderId + "，订单支付撤销（adapay支付渠道），失败原因：" + msg);
		}
		orderProcessLogPublishPort.publish(entities);
	}

	private Map<String, Object> failPayRes(String msg) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("status", "FAIL");
		m.put("error_desc", msg);
		m.put(PAY_RES_ORDER_PROCESS_LOG_VIA_PAYMENT_REVERSE, Boolean.TRUE);
		return m;
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}

	private static String firstNonBlank(String a, String b) {
		if (StringUtils.hasText(a)) {
			return a;
		}
		return b == null ? "" : b;
	}
}
