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

package cn.shopex.ecshopx.orders.service.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.port.OrdersMiniProgramHfpayPayPort;
import cn.shopex.ecshopx.payment.service.AlipayExternalPaySdkService;
import cn.shopex.ecshopx.payment.service.AlipayPaymentConfigValidationService;
import cn.shopex.ecshopx.payment.service.DoumenIntlPaymentService;
import cn.shopex.ecshopx.payment.service.WxpayPaymentConfigValidationService;
import cn.shopex.ecshopx.payment.support.PaymentSettingRedisKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class OrdersExternalPayParamBuildServiceAlipayPcPaymentTest {

	@Mock private StringRedisTemplate companysRedisTemplate;
	@Mock private ValueOperations<String, String> valueOps;
	@Mock private WxpayPaymentConfigValidationService wxpayPaymentConfigValidationService;
	@Mock private AlipayPaymentConfigValidationService alipayPaymentConfigValidationService;
	@Mock private AlipayExternalPaySdkService alipayExternalPaySdkService;
	@Mock private DoumenIntlPaymentService doumenIntlPaymentService;
	@Mock private NormalOrdersItemsMapper normalOrdersItemsMapper;
	@Mock private TradeMapper tradeMapper;
	@Mock private ObjectProvider<OrdersMiniProgramHfpayPayPort> hfpayPayPort;

	private OrdersExternalPayParamBuildService service;

	@BeforeEach
	void setUp() {
		when(companysRedisTemplate.opsForValue()).thenReturn(valueOps);
		service =
				new OrdersExternalPayParamBuildService(
						companysRedisTemplate,
						new ObjectMapper(),
						wxpayPaymentConfigValidationService,
						alipayPaymentConfigValidationService,
						alipayExternalPaySdkService,
						doumenIntlPaymentService,
						normalOrdersItemsMapper,
						tradeMapper,
						hfpayPayPort,
						"https://example.com/wx/notify",
						"https://example.com/alipay/notify");
	}

	@Test
	void buildPayParams_alipay_returnsPaymentFromPagePay_notOrderString() {
		long companyId = 38L;
		long distributorId = 269L;
		when(valueOps.get(PaymentSettingRedisKeys.alipayRedisKey(companyId, distributorId)))
				.thenReturn("{\"app_id\":\"app1\",\"private_key\":\"pk1\"}");
		when(alipayExternalPaySdkService.pagePayFormHtml(
						eq(companyId),
						eq(distributorId),
						anyString(),
						eq("http://localhost:3000"),
						eq("https://example.com/alipay/notify")))
				.thenReturn("<form id=\"alipay_submit\">pc</form>");

		Trade trade = new Trade();
		trade.setTradeId("T100");
		trade.setPayFee(100);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("order_id", "5442465000100261");
		data.put("body", "订单支付");
		data.put("return_url", "http://localhost:3000");
		data.put("trade_source_type", "normal");

		Map<String, Object> out =
				service.buildPayParamsOrInvoke("alipay", companyId, distributorId, Map.of(), data, trade);

		assertTrue(out.containsKey("payment"));
		assertEquals("<form id=\"alipay_submit\">pc</form>", out.get("payment"));
		assertFalse(out.containsKey("orderString"));
		verify(alipayExternalPaySdkService)
				.pagePayFormHtml(
						eq(companyId),
						eq(distributorId),
						anyString(),
						eq("http://localhost:3000"),
						eq("https://example.com/alipay/notify"));
		verify(alipayExternalPaySdkService, never())
				.sdkAppPayOrderString(anyLong(), anyLong(), anyString());
		verify(alipayExternalPaySdkService, never())
				.pageWapPayFormHtml(anyLong(), anyLong(), anyString(), anyString(), anyString());

		ArgumentCaptor<String> bizCaptor = ArgumentCaptor.forClass(String.class);
		verify(alipayExternalPaySdkService)
				.pagePayFormHtml(
						eq(companyId), eq(distributorId), bizCaptor.capture(), anyString(), anyString());
		String bizJson = bizCaptor.getValue();
		assertTrue(bizJson.contains("FAST_INSTANT_TRADE_PAY"));
		assertTrue(bizJson.contains("pay_type%3Dalipay") || bizJson.contains("pay_type=alipay"));
	}
}
