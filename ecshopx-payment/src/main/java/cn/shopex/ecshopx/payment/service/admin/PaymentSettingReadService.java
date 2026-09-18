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

package cn.shopex.ecshopx.payment.service.admin;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.hfpay.HfPayPaymentSettingApplyPort;
import cn.shopex.ecshopx.payment.service.settings.DoumenIntlPaymentSettingReader;
import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import cn.shopex.ecshopx.point.service.PointMemberRuleReadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PaymentSettingReadService {

	private static final String PT_WXPAY = "wxpay";
	private static final String PT_ALIPAY = "alipay";
	private static final String PT_PAYPAL = "paypal";
	private static final String PT_POINT_PAY = "point_pay";
	private static final String PT_HFPAY = "hfpay";
	private static final String PT_ADAPAY = "adapay";
	private static final String PT_OFFLINE = "offline_pay";
	private static final String PT_CHINAUMS = "chinaumspay";
	private static final String PT_BSPAY = "bspay";
	private static final String PT_ICBC = "icbcpay";
	private static final String PT_DOUMEN_INTL = "doumen_intl";

	private final PaymentSettingInputResolver paymentSettingInputResolver;
	private final PointMemberRuleReadService pointMemberRuleReadService;
	private final HfPayPaymentSettingApplyPort hfPayPaymentSettingApplyPort;
	private final DoumenIntlPaymentSettingReader doumenIntlPaymentSettingReader;
	private final StringRedisTemplate companysRedisTemplate;
	private final StringRedisTemplate sharedStringRedisTemplate;
	private final ObjectMapper objectMapper;

	public PaymentSettingReadService(
			PaymentSettingInputResolver paymentSettingInputResolver,
			PointMemberRuleReadService pointMemberRuleReadService,
			HfPayPaymentSettingApplyPort hfPayPaymentSettingApplyPort,
			DoumenIntlPaymentSettingReader doumenIntlPaymentSettingReader,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			ObjectMapper objectMapper) {
		this.paymentSettingInputResolver = paymentSettingInputResolver;
		this.pointMemberRuleReadService = pointMemberRuleReadService;
		this.hfPayPaymentSettingApplyPort = hfPayPaymentSettingApplyPort;
		this.doumenIntlPaymentSettingReader = doumenIntlPaymentSettingReader;
		this.companysRedisTemplate = companysRedisTemplate;
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public Object getPaymentSetting(
			HttpServletRequest request, String payType, long distributorId, String countryCodeResolved) {
		long companyId = paymentSettingInputResolver.requireCompanyId(request);

		if (PT_POINT_PAY.equals(payType)) {
			return pointMemberRuleReadService.getPointRule(companyId, countryCodeResolved);
		}
		if (PT_WXPAY.equals(payType)) {
			String raw = companysRedisTemplate.opsForValue()
					.get(PaymentSettingRedisKeys.wxpayRedisKey(companyId, distributorId));
			Map<String, Object> data = PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
			return emptyPaymentSettingBodyOrArray(data);
		}
		if (PT_ALIPAY.equals(payType)) {
			String raw = companysRedisTemplate.opsForValue()
					.get(PaymentSettingRedisKeys.alipayRedisKey(companyId, distributorId));
			Map<String, Object> data = PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
			return emptyPaymentSettingBodyOrArray(data);
		}
		if (PT_HFPAY.equals(payType)) {
			Map<String, Object> data =
					new LinkedHashMap<>(hfPayPaymentSettingApplyPort.loadForAdminPaymentSettingGet(companyId));
			return emptyPaymentSettingBodyOrArray(data);
		}
		if (PT_ADAPAY.equals(payType)) {
			String raw = sharedStringRedisTemplate.opsForValue()
					.get(PaymentSettingRedisKeys.adapaySettingKey(companyId));
			Map<String, Object> data = PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
			return emptyPaymentSettingBodyOrArray(data);
		}
		if (PT_OFFLINE.equals(payType)) {
			String raw = companysRedisTemplate.opsForValue()
					.get(PaymentSettingRedisKeys.offlinePaySettingKey(companyId, countryCodeResolved));
			Map<String, Object> data = PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
			String nameOverride =
					companysRedisTemplate.opsForValue()
							.get(PaymentSettingRedisKeys.offlinePayNameLangKey(companyId, countryCodeResolved));
			if (StringUtils.hasText(nameOverride)) {
				data.put("pay_name", nameOverride);
			}
			return emptyPaymentSettingBodyOrArray(data);
		}
		if (PT_ICBC.equals(payType)) {
			String raw = companysRedisTemplate.opsForValue()
					.get(PaymentSettingRedisKeys.icbcPaymentSettingKey(companyId));
			Map<String, Object> data = PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
			if (data.isEmpty()) {
				Map<String, Object> def = new LinkedHashMap<>();
				def.put("is_open", Boolean.TRUE);
				return def;
			}
			return data;
		}
		if (PT_BSPAY.equals(payType)) {
			String raw = companysRedisTemplate.opsForValue()
					.get(PaymentSettingRedisKeys.bspaySettingKey(companyId));
			Map<String, Object> data = PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
			return emptyPaymentSettingBodyOrArray(data);
		}
		if (PT_CHINAUMS.equals(payType)) {
			String subKey = paymentSettingInputResolver.resolveChinaumsSubKey(request, distributorId);
			String redisKey = PaymentSettingRedisKeys.chinaumsPaymentSettingKey(companyId, subKey);
			String raw = companysRedisTemplate.opsForValue().get(redisKey);
			Map<String, Object> data = PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
			return emptyPaymentSettingBodyOrArray(data);
		}
		if (PT_PAYPAL.equals(payType)) {
			String shopRaw = companysRedisTemplate.opsForValue()
					.get(PaymentSettingRedisKeys.paypalRedisKey(companyId, distributorId));
			Map<String, Object> shop = PaymentConfigJsonSupport.parseObjectMap(objectMapper, shopRaw);
			Map<String, Object> data;
			if (shop.isEmpty() && distributorId > 0) {
				String platRaw = companysRedisTemplate.opsForValue()
						.get(PaymentSettingRedisKeys.paypalRedisKey(companyId, 0L));
				data = PaymentConfigJsonSupport.parseObjectMap(objectMapper, platRaw);
			} else {
				data = shop;
			}
			return emptyPaymentSettingBodyOrArray(data);
		}
		if (PT_DOUMEN_INTL.equals(payType)) {
			return emptyPaymentSettingBodyOrArray(doumenIntlPaymentSettingReader.getMasked(companyId));
		}

		throw new BadRequestException("暂时不支持");
	}

	/**
	 * 无 Redis 配置或解析结果为空时，响应体需序列化为 JSON 数组 {@code []}，与运营端既有约定一致。
	 */
	private static Object emptyPaymentSettingBodyOrArray(Map<String, Object> data) {
		if (data == null || data.isEmpty()) {
			return Collections.emptyList();
		}
		return data;
	}
}
