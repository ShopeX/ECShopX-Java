package cn.shopex.ecshopx.orders.service.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.binarywang.wxpay.bean.request.WxPayMicropayRequest;
import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderRequest;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.exception.WxPayException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Locks WeChat v2 unifiedorder / micropay request shape to the legacy PHP (EasyWeChat) checkout behavior: {@code detail}
 * defaulting to {@code body}, {@code time_expire}, JSAPI {@code scene_info} = {@code []}, MWEB must not use that
 * JSAPI {@code scene_info} JSON, servicer {@code sub_*} / {@code sub_openid}.
 */
class OrdersExternalWxPayV2RequestsParityTest {

	private static final String TEST_KEY = "abcdefghijklmnopqrstuvwxyz012345";

	@Test
	void mweb_normalMerchant_detailTimeExpire_noSceneInfo() throws WxPayException {
		WxPayUnifiedOrderRequest req =
				OrdersExternalWxPayV2Requests.unifiedOrderRequest(
						false,
						"",
						"",
						"mch01",
						"wxmini",
						"wxmini",
						"",
						"wxfromcfg",
						true,
						"MWEB",
						"",
						"订单支付",
						"",
						"OUT001",
						100,
						"8.8.8.8",
						"https://notify.example/wx",
						"company_id%3D9%26pay_type%3Dwxpayh5",
						"20260102150000",
						"aabbccddeeff00112233445566778899");
		WxPayConfig cfg = OrdersExternalWxPayV2NativeClient.v2Md5Config(TEST_KEY);
		req.checkAndSign(cfg);
		Map<String, String> p = req.getSignParams();
		assertEquals("wxfromcfg", p.get("appid"));
		assertEquals("mch01", p.get("mch_id"));
		assertEquals("MWEB", p.get("trade_type"));
		assertEquals("订单支付", p.get("detail"));
		assertEquals("20260102150000", p.get("time_expire"));
		// PHP H5 不下发 scene_info；SDK 的 sign map 可能仍带空值键，但绝不能是 JSAPI 的 "[]"
		assertNotEquals("[]", p.get("scene_info"));
		assertEquals("https://notify.example/wx", p.get("notify_url"));
		assertNotNull(req.getSign());
		assertEquals(32, req.getSign().length());
	}

	@Test
	void jsapi_normalMerchant_sceneInfoEmptyJsonArray() throws WxPayException {
		WxPayUnifiedOrderRequest req =
				OrdersExternalWxPayV2Requests.unifiedOrderRequest(
						false,
						"",
						"",
						"mch01",
						"wxmini",
						"wxmini",
						"",
						"wxcfg",
						true,
						"JSAPI",
						"oOPENIDxxxxxxxx",
						"商品说明",
						"自定义明细",
						"OUT002",
						50,
						"127.0.0.1",
						"https://notify.example/wx",
						"attach",
						"20260102150100",
						"11223344556677889900112233445566");
		WxPayConfig cfg = OrdersExternalWxPayV2NativeClient.v2Md5Config(TEST_KEY);
		req.checkAndSign(cfg);
		Map<String, String> p = req.getSignParams();
		assertEquals("JSAPI", p.get("trade_type"));
		assertEquals("[]", p.get("scene_info"));
		assertEquals("自定义明细", p.get("detail"));
		assertEquals("oOPENIDxxxxxxxx", p.get("openid"));
		assertEquals("wxcfg", p.get("appid"));
	}

	@Test
	void jsapi_servicer_subOpenidAndSubApp() throws WxPayException {
		WxPayUnifiedOrderRequest req =
				OrdersExternalWxPayV2Requests.unifiedOrderRequest(
						true,
						"wxservicerapp",
						"servicerMch",
						"subMch01",
						"wxsubmini",
						"wxmini",
						"",
						"wxcfg",
						true,
						"JSAPI",
						"oSUBOIDxxxxxx",
						"body",
						"",
						"OUT003",
						10,
						"127.0.0.1",
						"https://notify.example/wx",
						"a",
						"20260102150200",
						"00112233445566778899aabbccddeeff");
		WxPayConfig cfg = OrdersExternalWxPayV2NativeClient.v2Md5Config(TEST_KEY);
		req.checkAndSign(cfg);
		Map<String, String> p = req.getSignParams();
		assertEquals("wxservicerapp", p.get("appid"));
		assertEquals("servicerMch", p.get("mch_id"));
		assertEquals("wxsubmini", p.get("sub_appid"));
		assertEquals("subMch01", p.get("sub_mch_id"));
		assertEquals("oSUBOIDxxxxxx", p.get("sub_openid"));
		assertEquals("[]", p.get("scene_info"));
	}

	@Test
	void micropay_servicer_subMerchantFields() throws WxPayException {
		WxPayMicropayRequest mic =
				OrdersExternalWxPayV2Requests.micropayRequest(
						true,
						"wxspapp",
						"spMch",
						"leafMch",
						"wxleafmini",
						"",
						"付款",
						"OUTMIC01",
						200,
						"192.168.1.1",
						"134567890123456789",
						"fedcba9876543210fedcba9876543210");
		WxPayConfig cfg = OrdersExternalWxPayV2NativeClient.v2Md5Config(TEST_KEY);
		mic.checkAndSign(cfg);
		Map<String, String> p = mic.getSignParams();
		assertEquals("wxspapp", p.get("appid"));
		assertEquals("spMch", p.get("mch_id"));
		assertEquals("wxleafmini", p.get("sub_appid"));
		assertEquals("leafMch", p.get("sub_mch_id"));
		assertEquals("134567890123456789", p.get("auth_code"));
	}

	@Test
	void timeExpirePrefersOrderAutoCancelTime() {
		long autoCancelEpoch = 1_767_398_400L;
		String expected =
				ZonedDateTime.ofInstant(Instant.ofEpochSecond(autoCancelEpoch), ZoneId.systemDefault())
						.format(OrdersExternalWxPayV2Requests.WX_TIME_EXPIRE_FORMATTER);
		assertEquals(expected, OrdersExternalWxPayV2Requests.formatTimeExpire(autoCancelEpoch));
		assertEquals(expected, OrdersExternalWxPayV2Requests.formatTimeExpire(String.valueOf(autoCancelEpoch)));
	}

	@Test
	void timeExpireFallsBackToNowPlusFiveMinutesWhenAutoCancelMissing() {
		long before = Instant.now().getEpochSecond() + 300;
		String formatted = OrdersExternalWxPayV2Requests.formatTimeExpire(null);
		long parsed =
				java.time.LocalDateTime.parse(formatted, OrdersExternalWxPayV2Requests.WX_TIME_EXPIRE_FORMATTER)
						.atZone(ZoneId.systemDefault())
						.toEpochSecond();
		long after = Instant.now().getEpochSecond() + 300;
		assertTrue(parsed >= before - 1 && parsed <= after + 1);
		assertEquals(14, OrdersExternalWxPayV2Requests.formatTimeExpire("0").length());
		assertEquals(14, OrdersExternalWxPayV2Requests.formatTimeExpire("").length());
	}
}
