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
import cn.shopex.ecshopx.adapay.mapper.AdapayPaymemtConfirmMapper;
import cn.shopex.ecshopx.adapay.service.AdapayPaymentSettingRedisReader;
import cn.shopex.ecshopx.common.refund.AftersalesRefundPayChannelExecutor;
import cn.shopex.ecshopx.common.refund.AftersalesRefundPaymentContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huifu.adapay.Adapay;
import com.huifu.adapay.core.exception.BaseAdaPayException;
import com.huifu.adapay.model.MerConfig;
import com.huifu.adapay.model.Refund;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * AdaPay 原路退款（已确认支付分支走 {@code Refund.create}）。
 */
@Service
@Order(55)
public class AdapayAftersalesRefundPayExecutor implements AftersalesRefundPayChannelExecutor {

	private final AdapayPaymemtConfirmMapper adapayPaymemtConfirmMapper;
	private final AdapayPaymentSettingRedisReader adapayPaymentSettingRedisReader;
	private final AdapayPaymentReverseApplicationService adapayPaymentReverseApplicationService;
	private final boolean prodMode;

	public AdapayAftersalesRefundPayExecutor(
			AdapayPaymemtConfirmMapper adapayPaymemtConfirmMapper,
			AdapayPaymentSettingRedisReader adapayPaymentSettingRedisReader,
			AdapayPaymentReverseApplicationService adapayPaymentReverseApplicationService,
			@Value("${ecshopx.adapay.prod-mode:true}") boolean prodMode) {
		this.adapayPaymemtConfirmMapper = adapayPaymemtConfirmMapper;
		this.adapayPaymentSettingRedisReader = adapayPaymentSettingRedisReader;
		this.adapayPaymentReverseApplicationService = adapayPaymentReverseApplicationService;
		this.prodMode = prodMode;
	}

	@Override
	public boolean supports(String payTypeLower) {
		return "adapay".equals(payTypeLower);
	}

	@Override
	public Map<String, Object> execute(AftersalesRefundPaymentContext ctx) {
		AdapayPaymemtConfirm row =
				adapayPaymemtConfirmMapper.selectOne(
						new LambdaQueryWrapper<AdapayPaymemtConfirm>()
								.eq(AdapayPaymemtConfirm::getCompanyId, ctx.getCompanyId())
								.eq(AdapayPaymemtConfirm::getOrderId, String.valueOf(ctx.getOrderId()))
								.eq(AdapayPaymemtConfirm::getStatus, "succeeded")
								.last("LIMIT 1"));
		if (row == null || !StringUtils.hasText(row.getPaymentConfirmationId())) {
			return adapayPaymentReverseApplicationService.reverseAndPublishOrderProcessLog(ctx);
		}
		Map<String, Object> cfg = adapayPaymentSettingRedisReader.getPaymentSetting(ctx.getCompanyId());
		String appId = str(cfg.get("app_id"));
		String liveKey = str(cfg.get("live_api_key"));
		String testKey = str(cfg.get("test_api_key"));
		String rsaPrivate = str(cfg.get("rsa_private_key"));
		if (!StringUtils.hasText(appId)
				|| !StringUtils.hasText(rsaPrivate)
				|| (!StringUtils.hasText(liveKey) && !StringUtils.hasText(testKey))) {
			return fail("请检查adapay支付配置");
		}
		String refundOrderNo =
				ctx.getRefundBn() + "_" + (10000 + (int) (Math.random() * 80000)) + System.currentTimeMillis();
		String refundAmtYuan =
				BigDecimal.valueOf(ctx.getRefundFeeFen())
						.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
						.toPlainString();
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("refund_order_no", refundOrderNo);
		params.put("refund_amt", refundAmtYuan);

		String merKey = String.valueOf(ctx.getCompanyId());
		Map<String, Object> root;
		synchronized (AdapaySdkSync.LOCK) {
			try {
				Adapay.prodMode = prodMode;
				MerConfig mc = new MerConfig();
				mc.setApiKey(prodMode ? firstNonBlank(liveKey, testKey) : firstNonBlank(testKey, liveKey));
				mc.setApiMockKey(testKey);
				mc.setRSAPrivateKey(rsaPrivate);
				Adapay.addMerConfig(mc, merKey);
				root = Refund.create(row.getPaymentConfirmationId(), params, merKey);
			} catch (BaseAdaPayException e) {
				String msg = e.getMessage();
				return fail(StringUtils.hasText(msg) ? msg : "AdaPay 退款失败");
			} catch (Exception e) {
				return fail("AdaPay 退款失败");
			}
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> data = root == null ? null : (Map<String, Object>) root.get("data");
		if (data == null) {
			return fail("AdaPay 退款失败");
		}
		if ("failed".equalsIgnoreCase(String.valueOf(data.get("status")))) {
			String err = String.valueOf(data.getOrDefault("error_msg", "AdaPay 退款失败"));
			return fail(StringUtils.hasText(err) ? err : "AdaPay 退款失败");
		}
		Map<String, Object> ok = new LinkedHashMap<>();
		ok.put("status", "SUCCESS");
		ok.put("refund_id", Objects.toString(data.get("id"), refundOrderNo));
		return ok;
	}

	private static Map<String, Object> fail(String msg) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("status", "FAIL");
		m.put("error_desc", msg);
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
