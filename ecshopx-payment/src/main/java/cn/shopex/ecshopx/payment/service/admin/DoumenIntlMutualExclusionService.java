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

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.payment.service.settings.DoumenIntlPaymentSettingReader;
import cn.shopex.ecshopx.payment.support.PaymentConfigJsonSupport;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 斗门国际与其它支付方式互斥：启用斗门时关闭其它渠道；斗门已启用时不允许再开启其它渠道（point_pay 除外）。
 */
@Service
public class DoumenIntlMutualExclusionService {

	public static final String MSG_CLOSE_DOUMEN_FIRST = "请先关闭斗门国际收银台";

	private static final String PT_DOUMEN = "doumen_intl";
	private static final String PT_POINT = "point_pay";

	private final DoumenIntlPaymentSettingReader doumenIntlPaymentSettingReader;
	private final StringRedisTemplate companysRedisTemplate;
	private final StringRedisTemplate sharedStringRedisTemplate;
	private final ObjectMapper objectMapper;

	public DoumenIntlMutualExclusionService(
			DoumenIntlPaymentSettingReader doumenIntlPaymentSettingReader,
			@Qualifier("companysRedisTemplate") StringRedisTemplate companysRedisTemplate,
			@Qualifier("sharedStringRedisTemplate") StringRedisTemplate sharedStringRedisTemplate,
			ObjectMapper objectMapper) {
		this.doumenIntlPaymentSettingReader = doumenIntlPaymentSettingReader;
		this.companysRedisTemplate = companysRedisTemplate;
		this.sharedStringRedisTemplate = sharedStringRedisTemplate;
		this.objectMapper = objectMapper;
	}

	public void validateBeforeSave(long companyId, String payType, boolean isOpening) {
		if (PT_DOUMEN.equals(payType) || PT_POINT.equals(payType) || !isOpening) {
			return;
		}
		if (doumenIntlPaymentSettingReader.isOpen(companyId)) {
			throw new ResourceException(MSG_CLOSE_DOUMEN_FIRST);
		}
	}

	public void closeAllOtherPaymentMethods(long companyId) {
		closeIfOpen(
				companysRedisTemplate,
				PaymentSettingRedisKeys.wxpayRedisKey(companyId, 0L),
				"false");
		closeIfOpen(
				companysRedisTemplate,
				PaymentSettingRedisKeys.alipayRedisKey(companyId, 0L),
				Boolean.FALSE);
		closeIfOpen(
				companysRedisTemplate,
				PaymentSettingRedisKeys.paypalRedisKey(companyId, 0L),
				Boolean.FALSE);
		closeIfOpen(
				companysRedisTemplate,
				PaymentSettingRedisKeys.chinaumsPaymentSettingKey(companyId, ""),
				Boolean.FALSE);
		closeIfOpen(
				companysRedisTemplate,
				PaymentSettingRedisKeys.offlinePaySettingKey(companyId, "zh-CN"),
				"false");
		closeIfOpen(
				companysRedisTemplate,
				PaymentSettingRedisKeys.bspaySettingKey(companyId),
				Boolean.FALSE);
		closeIfOpen(
				companysRedisTemplate,
				PaymentSettingRedisKeys.hfPaymentSettingKey(companyId),
				"false");
		closeIfOpen(
				sharedStringRedisTemplate,
				PaymentSettingRedisKeys.adapaySettingKey(companyId),
				Boolean.FALSE);
		closeIfOpen(
				companysRedisTemplate,
				PaymentSettingRedisKeys.icbcPaymentSettingKey(companyId),
				0);
	}

	private void closeIfOpen(StringRedisTemplate redis, String key, Object closedValue) {
		String raw = redis.opsForValue().get(key);
		Map<String, Object> config = PaymentConfigJsonSupport.parseObjectMap(objectMapper, raw);
		if (config.isEmpty()) {
			return;
		}
		if (!PaymentConfigJsonSupport.normalizeIsOpen(config.get("is_open"))) {
			return;
		}
		config.put("is_open", closedValue);
		try {
			redis.opsForValue().set(key, objectMapper.writeValueAsString(config));
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("failed to close payment setting: " + key, e);
		}
	}

	/** 解析保存请求是否在「开启」该支付方式（对齐 PHP resolvePaymentIsOpening 主路径）。 */
	public static boolean resolveIsOpening(String payType, Object rawIsOpen) {
		if (PT_POINT.equals(payType)) {
			return false;
		}
		if ("icbcpay".equals(payType)) {
			if (rawIsOpen instanceof Boolean b) {
				return b;
			}
			if (rawIsOpen instanceof Number n) {
				return n.intValue() == 1;
			}
			String s = rawIsOpen == null ? "" : String.valueOf(rawIsOpen).trim();
			return "true".equals(s) || "1".equals(s);
		}
		if (List.of("alipay", "adapay", "bspay", "chinaumspay", "paypal", "doumen_intl")
				.contains(payType)) {
			return PaymentSettingBooleanParsing.strictTrueString(rawIsOpen);
		}
		return PaymentConfigJsonSupport.normalizeIsOpen(rawIsOpen);
	}
}
