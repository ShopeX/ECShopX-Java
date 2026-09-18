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

package cn.shopex.ecshopx.bootstrap.popularize;

import cn.shopex.ecshopx.adapay.config.AdapayCallbackProperties;
import cn.shopex.ecshopx.adapay.service.AdapayAdaPayPaymentSettingReadService;
import cn.shopex.ecshopx.adapay.service.AdapayPaymentSettingRedisReader;
import cn.shopex.ecshopx.adapay.service.AdapayPromoterCertService;
import cn.shopex.ecshopx.adapay.service.callback.AdapaySubMerchantAdaPayOutboundGateway;
import cn.shopex.ecshopx.adapay.service.integration.AdapayDrawCashAdaPayGateway;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.popularize.domain.PromoterCashWithdrawal;
import cn.shopex.ecshopx.popularize.port.PopularizeCashWithdrawalBankcardPayPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PopularizeCashWithdrawalBankcardPayAdapter implements PopularizeCashWithdrawalBankcardPayPort {

	private final AdapayDrawCashAdaPayGateway adapayDrawCashAdaPayGateway;
	private final AdapayPromoterCertService adapayPromoterCertService;
	private final AdapayPaymentSettingRedisReader adapayPaymentSettingRedisReader;
	private final AdapayAdaPayPaymentSettingReadService adapayAdaPayPaymentSettingReadService;
	private final AdapaySubMerchantAdaPayOutboundGateway adapaySubMerchantAdaPayOutboundGateway;
	private final AdapayCallbackProperties adapayCallbackProperties;
	private final String adapayNotifyUrl;

	public PopularizeCashWithdrawalBankcardPayAdapter(
			AdapayDrawCashAdaPayGateway adapayDrawCashAdaPayGateway,
			AdapayPromoterCertService adapayPromoterCertService,
			AdapayPaymentSettingRedisReader adapayPaymentSettingRedisReader,
			AdapayAdaPayPaymentSettingReadService adapayAdaPayPaymentSettingReadService,
			AdapaySubMerchantAdaPayOutboundGateway adapaySubMerchantAdaPayOutboundGateway,
			AdapayCallbackProperties adapayCallbackProperties,
			@Value("${adapay.notify-url:}") String adapayNotifyUrl) {
		this.adapayDrawCashAdaPayGateway = adapayDrawCashAdaPayGateway;
		this.adapayPromoterCertService = adapayPromoterCertService;
		this.adapayPaymentSettingRedisReader = adapayPaymentSettingRedisReader;
		this.adapayAdaPayPaymentSettingReadService = adapayAdaPayPaymentSettingReadService;
		this.adapaySubMerchantAdaPayOutboundGateway = adapaySubMerchantAdaPayOutboundGateway;
		this.adapayCallbackProperties = adapayCallbackProperties;
		this.adapayNotifyUrl = adapayNotifyUrl;
	}

	@Override
	public Map<String, Object> payToBankcard(long companyId, PromoterCashWithdrawal info) {
		requireOutboundBaseUrl();
		String raw = info.getUserId() == null ? "" : info.getUserId().trim();
		if (!StringUtils.hasText(raw)) {
			throw new ResourceException("处理的佣金提现申请不存在");
		}
		long promoterUserId;
		try {
			promoterUserId = Long.parseLong(raw);
		} catch (NumberFormatException e) {
			throw new ResourceException("处理的佣金提现申请不存在");
		}

		String appId = adapayAdaPayPaymentSettingReadService.loadAppId(companyId).trim();
		if (!StringUtils.hasText(appId)) {
			throw new BadRequestException("adapay 支付信息未配置");
		}

		LinkedHashMap<String, Object> mainBalBody = new LinkedHashMap<>();
		mainBalBody.put("company_id", Long.valueOf(companyId));
		mainBalBody.put("notify_url", adapayNotifyUrl == null ? "" : adapayNotifyUrl.trim());
		mainBalBody.put("api_method", "SettleAccount.balance");
		mainBalBody.put("app_id", appId);
		mainBalBody.put("member_id", "0");
		Map<String, Object> mainRoot = adapayDrawCashAdaPayGateway.settleAccountBalance(mainBalBody);
		Map<String, Object> mainData = parseDataMap(mainRoot);
		BigDecimal mainAvl = readAvlBalanceYuan(mainData);

		Map<String, Object> cert = adapayPromoterCertService.getCertInfo(companyId, promoterUserId, false);
		Object mid = cert.get("member_id");
		long memberPk;
		try {
			if (mid == null) {
				return Map.of("status", "FAILED", "error_desc", "未入网,开户信息不全");
			}
			memberPk = ((Number) mid).longValue();
			if (memberPk == 0L) {
				return Map.of("status", "FAILED", "error_desc", "未入网,开户信息不全");
			}
		} catch (Exception e) {
			return Map.of("status", "FAILED", "error_desc", "未入网,开户信息不全");
		}
		String settleAccountId = String.valueOf(cert.getOrDefault("settle_account_id", "")).trim();
		if (!StringUtils.hasText(settleAccountId)) {
			return Map.of("status", "FAILED", "error_desc", "未入网,开户信息不全");
		}

		LinkedHashMap<String, Object> subBalBody = new LinkedHashMap<>();
		subBalBody.put("company_id", Long.valueOf(companyId));
		subBalBody.put("notify_url", adapayNotifyUrl == null ? "" : adapayNotifyUrl.trim());
		subBalBody.put("api_method", "SettleAccount.balance");
		subBalBody.put("app_id", appId);
		subBalBody.put("member_id", String.valueOf(memberPk));
		subBalBody.put("settle_account_id", settleAccountId);
		BigDecimal subAvl =
				readAvlBalanceYuan(parseDataMap(adapayDrawCashAdaPayGateway.settleAccountBalance(subBalBody)));

		BigDecimal needYuan = new BigDecimal(info.getMoney() == null ? 0 : info.getMoney())
				.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

		if (subAvl.compareTo(needYuan) < 0) {
			BigDecimal transferYuan = needYuan.subtract(subAvl).setScale(2, RoundingMode.HALF_UP);
			if (transferYuan.compareTo(BigDecimal.ZERO) > 0) {
				if (transferYuan.compareTo(mainAvl) > 0) {
					return Map.of("status", "FAILED", "error_desc", "余额不足");
				}
				LinkedHashMap<String, Object> tfBody = new LinkedHashMap<>();
				tfBody.put("company_id", Long.valueOf(companyId));
				tfBody.put("notify_url", adapayNotifyUrl == null ? "" : adapayNotifyUrl.trim());
				tfBody.put("api_method", "SettleAccount.transfer");
				tfBody.put("app_id", appId);
				tfBody.put(
						"order_no",
						"TF_"
								+ DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
										.withZone(ZoneId.systemDefault())
										.format(Instant.now())
								+ ThreadLocalRandom.current().nextInt(100000, 1000000));
				tfBody.put("out_member_id", "0");
				tfBody.put("in_member_id", String.valueOf(memberPk));
				tfBody.put("trans_amt", transferYuan.toPlainString());
				tfBody.put("remark", "推广员佣金提现");
				Map<String, Object> tfRoot = mergeMerchantInfoAndPost(companyId, tfBody);
				Map<String, Object> tfData = parseDataMap(tfRoot);
				if ("failed".equalsIgnoreCase(String.valueOf(tfData.get("status")))) {
					return Map.of(
							"status",
							"FAILED",
							"error_desc",
							String.valueOf(tfData.getOrDefault("error_msg", "汇付接口错误")));
				}
			}
		}

		String drawOrderNo =
				"TF_"
						+ DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
								.withZone(ZoneId.systemDefault())
								.format(Instant.now())
						+ ThreadLocalRandom.current().nextInt(100000, 1000000);
		LinkedHashMap<String, Object> drawBody = new LinkedHashMap<>();
		drawBody.put("company_id", Long.valueOf(companyId));
		drawBody.put("order_no", drawOrderNo);
		drawBody.put("app_id", appId);
		drawBody.put("cash_type", "D0");
		drawBody.put("cash_amt", needYuan.toPlainString());
		drawBody.put("member_id", String.valueOf(memberPk));
		drawBody.put("notify_url", adapayNotifyUrl == null ? "" : adapayNotifyUrl.trim());
		drawBody.put("api_method", "DrawCash.create");
		drawBody.put("operator", "-");
		Map<String, Object> drawRoot = adapayDrawCashAdaPayGateway.drawCashCreate(drawBody);
		Map<String, Object> drawData = parseDataMap(drawRoot);
		if ("failed".equalsIgnoreCase(String.valueOf(drawData.get("status")))) {
			return Map.of(
					"status",
					"FAILED",
					"error_desc",
					String.valueOf(drawData.getOrDefault("error_msg", "汇付接口错误")));
		}

		String drawStatus = String.valueOf(drawData.getOrDefault("status", "")).trim();
		if (Objects.equals(drawStatus, "pending")) {
			return Map.of("status", "PROCESS");
		}
		if ("succeeded".equalsIgnoreCase(drawStatus) || "success".equalsIgnoreCase(drawStatus)) {
			return Map.of("status", "SUCCESS");
		}
		return Map.of("status", "SUCCESS");
	}

	private void requireOutboundBaseUrl() {
		if (!StringUtils.hasText(adapayCallbackProperties.getSettleOutboundBaseUrl())) {
			throw new ResourceException("汇付 outbound 地址未配置");
		}
	}

	private Map<String, Object> mergeMerchantInfoAndPost(long companyId, LinkedHashMap<String, Object> body) {
		body.put("merchant_info", new LinkedHashMap<>(adapayPaymentSettingRedisReader.getPaymentSetting(companyId)));
		@SuppressWarnings("unchecked")
		Map<String, Object> merchantInfo = (Map<String, Object>) body.get("merchant_info");
		if (merchantInfo == null || merchantInfo.isEmpty()) {
			throw new BadRequestException("adapay 支付信息未配置");
		}
		return adapaySubMerchantAdaPayOutboundGateway.postAdaPayRequest(body);
	}

	private static Map<String, Object> parseDataMap(Map<String, Object> root) {
		Object d = root.get("data");
		if (!(d instanceof Map<?, ?> m)) {
			return Map.of("status", "failed", "error_msg", "汇付接口错误");
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> dataMap = (Map<String, Object>) m;
		return dataMap;
	}

	private static BigDecimal readAvlBalanceYuan(Map<String, Object> dataMap) {
		if ("failed".equalsIgnoreCase(String.valueOf(dataMap.get("status")))) {
			throw new ResourceException(String.valueOf(dataMap.getOrDefault("error_msg", "汇付接口错误")));
		}
		if (dataMap.get("avl_balance") == null) {
			throw new ResourceException("汇付接口错误");
		}
		return new BigDecimal(dataMap.get("avl_balance").toString()).setScale(2, RoundingMode.HALF_UP);
	}
}
