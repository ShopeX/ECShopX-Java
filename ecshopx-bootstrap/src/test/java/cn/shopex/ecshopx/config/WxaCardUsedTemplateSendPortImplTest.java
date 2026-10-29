package cn.shopex.ecshopx.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.wechat.WxaMemberOpenIdLookupPort;
import cn.shopex.ecshopx.promotions.service.WxaTemplateMsgActivityRemindSendService;
import cn.shopex.ecshopx.wechat.domain.Weapp;
import cn.shopex.ecshopx.wechat.mapper.WeappMapper;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WxaCardUsedTemplateSendPortImplTest {

	@Mock
	private WeappMapper weappMapper;

	@Mock
	private WxaMemberOpenIdLookupPort wxaMemberOpenIdLookupPort;

	@Mock
	private WxaTemplateMsgActivityRemindSendService wxaTemplateMsgActivityRemindSendService;

	@InjectMocks
	private WxaCardUsedTemplateSendPortImpl port;

	@Test
	@DisplayName("cardUsedSucc is skipped")
	void skipsCardUsedSucc() {
		port.send(1L, 2L, "cardUsedSucc", Map.of("title", "x"));
		verifyNoInteractions(weappMapper, wxaMemberOpenIdLookupPort, wxaTemplateMsgActivityRemindSendService);
	}

	@Test
	@DisplayName("userGetCardSucc builds subscribe payload")
	void sendsUserGetCardSucc() {
		Weapp weapp = new Weapp();
		weapp.setAuthorizerAppid("wxapp");
		when(weappMapper.selectOne(any())).thenReturn(weapp);
		when(wxaMemberOpenIdLookupPort.resolveOpenId(2L, "wxapp")).thenReturn("oid");

		port.send(1L, 2L, "userGetCardSucc", Map.of("title", "券A", "remarks", "满10可用"));

		verify(wxaTemplateMsgActivityRemindSendService)
				.send(
						argThat(
								m -> "userGetCardSucc".equals(m.get("scenes_name"))
										&& Long.valueOf(1L).equals(asLong(m.get("company_id")))
										&& "wxapp".equals(m.get("appid"))
										&& "oid".equals(m.get("openid"))
										&& m.get("data") instanceof Map<?, ?> data
										&& "券A".equals(data.get("title"))),
						eq(true));
	}

	@Test
	@DisplayName("registrationResultNotice builds subscribe payload")
	void sendsRegistrationResultNotice() {
		Weapp weapp = new Weapp();
		weapp.setAuthorizerAppid("wxapp");
		when(weappMapper.selectOne(any())).thenReturn(weapp);
		when(wxaMemberOpenIdLookupPort.resolveOpenId(9L, "wxapp")).thenReturn("oid9");

		port.send(3L, 9L, "registrationResultNotice", Map.of("activity_name", "活动", "review_result", "审核通过"));

		verify(wxaTemplateMsgActivityRemindSendService)
				.send(
						argThat(m -> "registrationResultNotice".equals(m.get("scenes_name"))),
						eq(true));
	}

	@Test
	@DisplayName("missing openid skips send")
	void skipsWhenNoOpenId() {
		Weapp weapp = new Weapp();
		weapp.setAuthorizerAppid("wxapp");
		when(weappMapper.selectOne(any())).thenReturn(weapp);
		when(wxaMemberOpenIdLookupPort.resolveOpenId(2L, "wxapp")).thenReturn("");

		port.send(1L, 2L, "userGetCardSucc", Map.of("title", "x"));

		verify(wxaTemplateMsgActivityRemindSendService, never()).send(any(), eq(true));
	}

	private static Long asLong(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(raw));
	}
}
