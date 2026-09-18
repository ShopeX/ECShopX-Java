package cn.shopex.ecshopx.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.payment.config.DoumenIntlProperties;
import cn.shopex.ecshopx.payment.integration.doumenintl.DoumenIntlGatewayClient;
import cn.shopex.ecshopx.payment.service.settings.DoumenIntlPaymentSettingReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DoumenIntlPaymentServiceTest {

	@Mock
	private DoumenIntlPaymentSettingReader settingReader;

	@Mock
	private DoumenIntlGatewayClient gatewayClient;

	private DoumenIntlProperties properties;
	private DoumenIntlPaymentService service;

	@BeforeEach
	void setUp() {
		properties = new DoumenIntlProperties();
		properties.setBaseUrl("https://gateway.example.com");
		properties.setNotifyUrl("https://api.example.com/doumen-intl/notify");
		service = new DoumenIntlPaymentService(settingReader, gatewayClient, properties);
	}

	@Test
	void doPay_incompleteMerchantConfig_returnsBadRequestNot500() {
		when(settingReader.isConfigured(38L)).thenReturn(false);
		when(settingReader.getRaw(38L))
				.thenReturn(
						Map.of(
								"is_open", true,
								"X-AccessCode", "AC",
								"appId", "APP"));

		BadRequestException ex =
				assertThrows(
						BadRequestException.class,
						() ->
								service.doPay(
										payData(),
										List.of(product())));

		assertEquals("请检查斗门国际支付配置", ex.getMessage());
	}

	@Test
	void doPay_missingBaseUrl_returnsBadRequestNot500() {
		properties.setBaseUrl("");
		when(settingReader.isConfigured(38L)).thenReturn(true);
		when(settingReader.getRaw(38L)).thenReturn(openSetting());

		BadRequestException ex =
				assertThrows(
						BadRequestException.class,
						() ->
								service.doPay(
										payData(),
										List.of(product())));

		assertEquals("斗门国际支付网关地址未配置", ex.getMessage());
	}

	@Test
	void doPay_gatewayAuthFailure_returnsBadRequestNot500() {
		when(settingReader.isConfigured(38L)).thenReturn(true);
		when(settingReader.getRaw(38L)).thenReturn(openSetting());
		when(gatewayClient.createCheckout(anyString(), anyString(), any()))
				.thenThrow(
						new IllegalStateException(
								"Doumen Intl gateway authentication failed: 401 Unauthorized"));

		BadRequestException ex =
				assertThrows(
						BadRequestException.class,
						() ->
								service.doPay(
										payData(),
										List.of(product())));

		assertEquals("斗门国际支付网关鉴权失败，请检查 AccessCode/SecretKey", ex.getMessage());
	}

	@Test
	void doPay_gatewayConnectionFailure_returnsBadRequestNot500() {
		when(settingReader.isConfigured(38L)).thenReturn(true);
		when(settingReader.getRaw(38L)).thenReturn(openSetting());
		when(gatewayClient.createCheckout(anyString(), anyString(), any()))
				.thenThrow(
						new IllegalStateException(
								"Doumen Intl gateway authentication failed: Connection refused",
								new org.springframework.web.client.ResourceAccessException(
										"I/O error on GET request for \"https://bad.example/authorize\": Connection refused")));

		BadRequestException ex =
				assertThrows(
						BadRequestException.class,
						() ->
								service.doPay(
										payData(),
										List.of(product())));

		assertEquals("斗门国际支付网关连接失败，请检查支付配置", ex.getMessage());
	}

	private static Map<String, Object> payData() {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("company_id", 38L);
		data.put("trade_id", "T001");
		data.put("order_id", "5359710000211170");
		data.put("pay_fee", 100);
		data.put("fee_type", "CNY");
		data.put("return_url", "https://shop.example.com");
		data.put("trade_source_type", "normal");
		return data;
	}

	private static Map<String, Object> product() {
		Map<String, Object> p = new LinkedHashMap<>();
		p.put("item_bn", "SKU1");
		p.put("item_name", "商品");
		p.put("num", 1);
		p.put("price", 100);
		p.put("total_fee", 100);
		return p;
	}

	private static Map<String, Object> openSetting() {
		Map<String, Object> setting = new LinkedHashMap<>();
		setting.put("is_open", true);
		setting.put("X-AccessCode", "AC");
		setting.put("X-SecretKey", "SECRET");
		setting.put("appId", "APP");
		setting.put("return_url", "https://shop.example.com/return");
		return setting;
	}
}
