package cn.shopex.ecshopx.wechat.api.open;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.WechatSubscribeDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WxShopsAddDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WxShopsUpdateDispatchPublisher;
import cn.shopex.ecshopx.wechat.domain.WechatAuth;
import cn.shopex.ecshopx.wechat.mapper.WechatAuthMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
class WechatOpenPlatformMessageCallbackControllerSubscribeDispatchTest {

	@Mock
	private WechatSubscribeDispatchPublisher publisher;

	@Mock
	private WxShopsAddDispatchPublisher wxShopsAddDispatchPublisher;

	@Mock
	private WxShopsUpdateDispatchPublisher wxShopsUpdateDispatchPublisher;

	@Mock
	private WechatAuthMapper wechatAuthMapper;

	private WechatOpenPlatformMessageCallbackController controller;

	@BeforeEach
	void setUp() {
		controller =
				new WechatOpenPlatformMessageCallbackController(
						publisher, wxShopsAddDispatchPublisher, wxShopsUpdateDispatchPublisher, wechatAuthMapper, "");
		WechatAuth auth = new WechatAuth();
		auth.setCompanyId(42L);
		when(wechatAuthMapper.selectById("wxAuthorizer1")).thenReturn(auth);
	}

	@Test
	void whenEventIsSubscribe_thenPublishPayloadContainsOpenIdCompanyAndEvent() {
		String xml =
				"<xml><MsgType><![CDATA[event]]></MsgType><Event><![CDATA[subscribe]]></Event>"
						+ "<FromUserName><![CDATA[open-id-1]]></FromUserName></xml>";

		ResponseEntity<String> res =
				controller.handleMessage("wxAuthorizer1", null, null, null, null, xml, new MockHttpServletRequest());

		assertEquals(HttpStatus.OK, res.getStatusCode());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass((Class) Map.class);
		verify(publisher).publish(cap.capture());
		verify(wxShopsAddDispatchPublisher, never()).publish(any());
		verify(wxShopsUpdateDispatchPublisher, never()).publish(any());
		Map<String, Object> m = cap.getValue();
		assertEquals("open-id-1", m.get("openId"));
		assertEquals("wxAuthorizer1", m.get("authorizerAppId"));
		assertEquals(42L, m.get("company_id"));
		assertEquals("subscribe", m.get("event"));
	}

	@Test
	void whenEventIsUnsubscribe_thenPublishWithUnsubscribeEvent() {
		String xml =
				"<xml><MsgType><![CDATA[event]]></MsgType><Event><![CDATA[unsubscribe]]></Event>"
						+ "<FromUserName><![CDATA[open-id-2]]></FromUserName></xml>";

		ResponseEntity<String> res =
				controller.handleMessage("wxAuthorizer1", null, null, null, null, xml, new MockHttpServletRequest());

		assertEquals(HttpStatus.OK, res.getStatusCode());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass((Class) Map.class);
		verify(publisher).publish(cap.capture());
		verify(wxShopsAddDispatchPublisher, never()).publish(any());
		verify(wxShopsUpdateDispatchPublisher, never()).publish(any());
		Map<String, Object> m = cap.getValue();
		assertEquals("unsubscribe", m.get("event"));
		assertEquals("open-id-2", m.get("openId"));
	}

	@Test
	void whenCallbackTokenSet_andPostSignatureValid_thenPublishes() throws NoSuchAlgorithmException {
		String token = "verify-tok";
		WechatOpenPlatformMessageCallbackController c =
				new WechatOpenPlatformMessageCallbackController(
						publisher, wxShopsAddDispatchPublisher, wxShopsUpdateDispatchPublisher, wechatAuthMapper, token);
		String ts = "1400000001";
		String nonce = "nonce-a";
		String sig = sha1HexSorted(token, ts, nonce);
		String xml =
				"<xml><MsgType><![CDATA[event]]></MsgType><Event><![CDATA[subscribe]]></Event>"
						+ "<FromUserName><![CDATA[open-id-x]]></FromUserName></xml>";

		ResponseEntity<String> res =
				c.handleMessage("wxAuthorizer1", sig, null, ts, nonce, xml, new MockHttpServletRequest());

		assertEquals(HttpStatus.OK, res.getStatusCode());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass((Class) Map.class);
		verify(publisher).publish(cap.capture());
		verify(wxShopsAddDispatchPublisher, never()).publish(any());
		verify(wxShopsUpdateDispatchPublisher, never()).publish(any());
		assertEquals("open-id-x", cap.getValue().get("openId"));
	}

	@Test
	void whenCallbackTokenSet_andPostSignatureValid_thenPublishesUnsubscribeEvent() throws NoSuchAlgorithmException {
		String token = "verify-tok";
		WechatOpenPlatformMessageCallbackController c =
				new WechatOpenPlatformMessageCallbackController(
						publisher, wxShopsAddDispatchPublisher, wxShopsUpdateDispatchPublisher, wechatAuthMapper, token);
		String ts = "1400000001";
		String nonce = "nonce-a";
		String sig = sha1HexSorted(token, ts, nonce);
		String xml =
				"<xml><MsgType><![CDATA[event]]></MsgType><Event><![CDATA[unsubscribe]]></Event>"
						+ "<FromUserName><![CDATA[open-id-u]]></FromUserName></xml>";

		ResponseEntity<String> res =
				c.handleMessage("wxAuthorizer1", sig, null, ts, nonce, xml, new MockHttpServletRequest());

		assertEquals(HttpStatus.OK, res.getStatusCode());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass((Class) Map.class);
		verify(publisher).publish(cap.capture());
		verify(wxShopsAddDispatchPublisher, never()).publish(any());
		verify(wxShopsUpdateDispatchPublisher, never()).publish(any());
		Map<String, Object> m = cap.getValue();
		assertEquals("unsubscribe", m.get("event"));
		assertEquals("open-id-u", m.get("openId"));
	}

	@Test
	void whenCallbackTokenSet_andPostSignatureInvalid_thenForbiddenAndNoPublish() throws Exception {
		String token = "verify-tok";
		WechatOpenPlatformMessageCallbackController c =
				new WechatOpenPlatformMessageCallbackController(
						publisher, wxShopsAddDispatchPublisher, wxShopsUpdateDispatchPublisher, wechatAuthMapper, token);
		String ts = "1400000001";
		String nonce = "nonce-a";
		String xml =
				"<xml><MsgType><![CDATA[event]]></MsgType><Event><![CDATA[subscribe]]></Event>"
						+ "<FromUserName><![CDATA[open-id-x]]></FromUserName></xml>";

		ResponseEntity<String> res =
				c.handleMessage("wxAuthorizer1", "not-a-valid-sig", null, ts, nonce, xml, new MockHttpServletRequest());

		assertEquals(HttpStatus.FORBIDDEN, res.getStatusCode());
		verify(publisher, never()).publish(any());
		verify(wxShopsAddDispatchPublisher, never()).publish(any());
		verify(wxShopsUpdateDispatchPublisher, never()).publish(any());
	}

	private static String sha1HexSorted(String token, String timestamp, String nonce)
			throws NoSuchAlgorithmException {
		String[] arr = new String[] {token, timestamp, nonce};
		Arrays.sort(arr);
		String plain = arr[0] + arr[1] + arr[2];
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		byte[] digest = md.digest(plain.getBytes(StandardCharsets.UTF_8));
		StringBuilder sb = new StringBuilder(digest.length * 2);
		for (byte b : digest) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
}
