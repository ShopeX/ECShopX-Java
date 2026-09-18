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
class WechatOpenPlatformMessageCallbackControllerWxShopsAddDispatchTest {

	@Mock
	private WechatSubscribeDispatchPublisher wechatSubscribeDispatchPublisher;

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
						wechatSubscribeDispatchPublisher,
						wxShopsAddDispatchPublisher,
						wxShopsUpdateDispatchPublisher,
						wechatAuthMapper,
						"");
		WechatAuth auth = new WechatAuth();
		auth.setCompanyId(99L);
		when(wechatAuthMapper.selectById("wxAid")).thenReturn(auth);
	}

	@Test
	void whenEventIsAddStoreAuditInfo_thenPublishPayloadContainsAuditFields() {
		String xml =
				"<xml><MsgType><![CDATA[event]]></MsgType>"
						+ "<Event><![CDATA[add_store_audit_info]]></Event>"
						+ "<audit_id><![CDATA[audit-42]]></audit_id>"
						+ "<status><![CDATA[2]]></status>"
						+ "<reason><![CDATA[review note]]></reason>"
						+ "<is_upgrade><![CDATA[0]]></is_upgrade>"
						+ "<poiid><![CDATA[poi-x]]></poiid></xml>";

		ResponseEntity<String> res =
				controller.handleMessage("wxAid", null, null, null, null, xml, new MockHttpServletRequest());

		assertEquals(HttpStatus.OK, res.getStatusCode());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass((Class) Map.class);
		verify(wxShopsAddDispatchPublisher).publish(cap.capture());
		verify(wxShopsUpdateDispatchPublisher, never()).publish(any());
		verify(wechatSubscribeDispatchPublisher, never()).publish(any());
		Map<String, Object> m = cap.getValue();
		assertEquals("audit-42", m.get("audit_id"));
		assertEquals("2", m.get("status"));
		assertEquals("review note", m.get("reason"));
		assertEquals("0", m.get("is_upgrade"));
		assertEquals("poi-x", m.get("poiid"));
	}

	@Test
	void whenEventIsSubscribe_thenWxShopsPublisherNeverCalled() {
		String xml =
				"<xml><MsgType><![CDATA[event]]></MsgType><Event><![CDATA[subscribe]]></Event>"
						+ "<FromUserName><![CDATA[open-id-1]]></FromUserName></xml>";

		controller.handleMessage("wxAid", null, null, null, null, xml, new MockHttpServletRequest());

		verify(wxShopsAddDispatchPublisher, never()).publish(any());
		verify(wxShopsUpdateDispatchPublisher, never()).publish(any());
	}
}
