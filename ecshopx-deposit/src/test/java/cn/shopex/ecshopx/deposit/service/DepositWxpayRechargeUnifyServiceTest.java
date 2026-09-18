package cn.shopex.ecshopx.deposit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class DepositWxpayRechargeUnifyServiceTest {

	private static final String UNIFIED_ORDER_URL = "https://api.mch.weixin.qq.com/pay/unifiedorder";

	@Mock
	private StringRedisTemplate companysRedisTemplate;

	@Mock
	private ValueOperations<String, String> valueOps;

	@Mock
	private RestTemplate restTemplate;

	private DepositWxpayRechargeUnifyService service;

	@BeforeEach
	void setUp() {
		when(companysRedisTemplate.opsForValue()).thenReturn(valueOps);
		service =
				new DepositWxpayRechargeUnifyService(
						companysRedisTemplate,
						new ObjectMapper(),
						restTemplate,
						"https://notify.example/wx");
	}

	@Test
	void wxpayh5_usesPaymentConfigAppIdAndMweb() {
		when(valueOps.get(any())).thenReturn(wxpayCfgJson("wxoa_from_setting"));
		ArgumentCaptor<HttpEntity<String>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
		when(restTemplate.postForEntity(eq(UNIFIED_ORDER_URL), entityCaptor.capture(), eq(String.class)))
				.thenReturn(ResponseEntity.ok(mwebSuccessXml("https://wx.tenpay.com/cgi-bin/mweb?t=1")));

		Map<String, Object> out =
				service.unifyAndBuildClientPayParams(
						141L,
						0L,
						"CZ123",
						2000L,
						"",
						"",
						"",
						"",
						"充值",
						"8.8.8.8",
						"wxpayh5");

		String xml = entityCaptor.getValue().getBody();
		assertTrue(xml.contains("<appid><![CDATA[wxoa_from_setting]]></appid>"));
		assertTrue(xml.contains("<trade_type><![CDATA[MWEB]]></trade_type>"));
		assertFalse(xml.contains("<openid>"));
		assertEquals("https://wx.tenpay.com/cgi-bin/mweb?t=1", out.get("mweb_url"));
		assertEquals("CZ123", ((Map<?, ?>) out.get("trade_info")).get("order_id"));
	}

	@Test
	void wxpay_jsapi_usesWxaAppId() {
		when(valueOps.get(any())).thenReturn(wxpayCfgJson("wxoa_from_setting"));
		ArgumentCaptor<HttpEntity<String>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
		when(restTemplate.postForEntity(eq(UNIFIED_ORDER_URL), entityCaptor.capture(), eq(String.class)))
				.thenReturn(ResponseEntity.ok(jsapiSuccessXml("prepay_abc")));

		Map<String, Object> out =
				service.unifyAndBuildClientPayParams(
						141L,
						0L,
						"CZ123",
						2000L,
						"oOPENID",
						"wxmini_appid",
						"",
						"",
						"充值",
						"8.8.8.8",
						"wxpay");

		String xml = entityCaptor.getValue().getBody();
		assertTrue(xml.contains("<appid><![CDATA[wxmini_appid]]></appid>"));
		assertTrue(xml.contains("<trade_type><![CDATA[JSAPI]]></trade_type>"));
		assertTrue(xml.contains("<openid><![CDATA[oOPENID]]></openid>"));
		assertEquals("wxmini_appid", out.get("appId"));
		assertEquals("prepay_id=prepay_abc", out.get("package"));
	}

	private static String wxpayCfgJson(String appId) {
		return "{"
				+ "\"app_id\":\""
				+ appId
				+ "\","
				+ "\"merchant_id\":\"mch01\","
				+ "\"key\":\"abcdefghijklmnopqrstuvwxyz012345\","
				+ "\"cert\":\"c\","
				+ "\"cert_key\":\"k\","
				+ "\"is_open\":\"true\""
				+ "}";
	}

	private static String mwebSuccessXml(String mwebUrl) {
		return "<xml>"
				+ "<return_code><![CDATA[SUCCESS]]></return_code>"
				+ "<result_code><![CDATA[SUCCESS]]></result_code>"
				+ "<mweb_url><![CDATA["
				+ mwebUrl
				+ "]]></mweb_url>"
				+ "</xml>";
	}

	private static String jsapiSuccessXml(String prepayId) {
		return "<xml>"
				+ "<return_code><![CDATA[SUCCESS]]></return_code>"
				+ "<result_code><![CDATA[SUCCESS]]></result_code>"
				+ "<prepay_id><![CDATA["
				+ prepayId
				+ "]]></prepay_id>"
				+ "</xml>";
	}
}
