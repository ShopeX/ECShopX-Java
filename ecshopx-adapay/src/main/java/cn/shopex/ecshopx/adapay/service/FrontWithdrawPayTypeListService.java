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

import cn.shopex.ecshopx.common.payment.FrontWithdrawPayTypeListPort;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayPaymentSettingService;
import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class FrontWithdrawPayTypeListService implements FrontWithdrawPayTypeListPort {

	private final AdapayPaymentSettingRedisReader adapayPaymentSettingRedisReader;
	private final AdapayOpenAccountStepService adapayOpenAccountStepService;
	private final StringRedisTemplate companysRedisTemplate;
	private final ObjectMapper objectMapper;
	private final HfPayPaymentSettingService hfPayPaymentSettingService;
	private final MessageSource messageSource;

	public FrontWithdrawPayTypeListService(
			AdapayPaymentSettingRedisReader adapayPaymentSettingRedisReader,
			AdapayOpenAccountStepService adapayOpenAccountStepService,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			ObjectMapper objectMapper,
			HfPayPaymentSettingService hfPayPaymentSettingService,
			MessageSource messageSource) {
		this.adapayPaymentSettingRedisReader = adapayPaymentSettingRedisReader;
		this.adapayOpenAccountStepService = adapayOpenAccountStepService;
		this.companysRedisTemplate = companysRedisTemplate;
		this.objectMapper = objectMapper;
		this.hfPayPaymentSettingService = hfPayPaymentSettingService;
		this.messageSource = messageSource;
	}

	@Override
	public List<Map<String, Object>> getWithDrawList(
			long companyId, HttpServletRequest request, String countryCodeNormalized) {
		List<Map<String, Object>> result = new ArrayList<>();
		Locale locale = resolveLocale(countryCodeNormalized);

		LinkedHashMap<String, Object> alipayRow = new LinkedHashMap<>();
		alipayRow.put("pay_type_code", "alipay");
		alipayRow.put(
				"pay_type_name",
				messageSource.getMessage("payment.alipay", null, "支付宝", locale));
		result.add(alipayRow);

		Map<String, Object> adapay = adapayPaymentSettingRedisReader.getPaymentSetting(companyId);
		Map<String, Object> stepPayload = adapayOpenAccountStepService.openAccountStep(companyId, request);
		Object stepObj = stepPayload.get("step");
		int step = (stepObj instanceof Number) ? ((Number) stepObj).intValue() : 0;
		if (!adapay.isEmpty() && Integer.valueOf(4).equals(step)) {
			LinkedHashMap<String, Object> bankRow = new LinkedHashMap<>();
			bankRow.put("pay_type_code", "bankcard");
			bankRow.put(
					"pay_type_name",
					messageSource.getMessage("payment.bank_card", null, "银行卡", locale));
			result.add(bankRow);
		}

		String rawWx = companysRedisTemplate.opsForValue().get(PaymentSettingRedisKeys.wxpayRedisKey(companyId, 0L));
		Map<String, Object> wechat = PaymentConfigJsonSupport.parseObjectMap(objectMapper, rawWx);
		if (!wechat.isEmpty()) {
			LinkedHashMap<String, Object> wechatRow = new LinkedHashMap<>();
			wechatRow.put("pay_type_code", "wechat");
			wechatRow.put(
					"pay_type_name",
					messageSource.getMessage("payment.wechat_pay", null, "微信支付", locale));
			result.add(wechatRow);
		}

		Map<String, Object> hfpay = hfPayPaymentSettingService.loadForAdminPaymentSettingGet(companyId);
		if (hfpay != null && !hfpay.isEmpty()) {
			LinkedHashMap<String, Object> hfRow = new LinkedHashMap<>();
			hfRow.put("pay_type_code", "hfpay");
			hfRow.put(
					"pay_type_name",
					messageSource.getMessage("payment.wechat_pay_hfpay", null, "微信支付-汇付", locale));
			result.add(hfRow);
		}

		return result;
	}

	private static Locale resolveLocale(String countryCodeNormalized) {
		if ("en-CN".equals(countryCodeNormalized)) {
			return Locale.forLanguageTag("en-US");
		}
		return Locale.SIMPLIFIED_CHINESE;
	}
}
