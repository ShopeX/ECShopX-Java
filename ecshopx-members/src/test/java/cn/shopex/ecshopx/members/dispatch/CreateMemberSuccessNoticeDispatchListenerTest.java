package cn.shopex.ecshopx.members.dispatch;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.port.members.MemberCreateSuccessWxaNoticePort;
import cn.shopex.ecshopx.members.service.CreateMemberSuccessNoticeExecutionService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateMemberSuccessNoticeDispatchListenerTest {

	@Mock
	private MemberAccountService memberAccountService;

	@Mock
	private MemberCreateSuccessWxaNoticePort wxaNoticePort;

	private CreateMemberSuccessNoticeDispatchListener listener;

	@BeforeEach
	void setUp() {
		CreateMemberSuccessNoticeExecutionService executionService =
				new CreateMemberSuccessNoticeExecutionService(memberAccountService, wxaNoticePort);
		listener = new CreateMemberSuccessNoticeDispatchListener(executionService);
	}

	@Test
	void onEvent_whenOpenidOrWxaAppidBlank_doesNotInvokeWxaPort() {
		Map<String, Object> payload = new HashMap<>();
		payload.put("openid", "");
		payload.put("wxa_appid", "app-1");
		payload.put("company_id", 7L);
		payload.put("user_id", 99L);

		listener.onEvent(payload);

		verifyNoInteractions(wxaNoticePort);
	}

	@Test
	void onEvent_whenOpenidAndWxaAppidPresent_invokesWxaTemplateOnceWithExpectedShape() {
		when(memberAccountService.getMemberInfo(99L, 7L))
				.thenReturn(Map.of("created_date", "2026-05-07 12:00:00"));

		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 7L);
		payload.put("user_id", 99L);
		payload.put("openid", "o-test");
		payload.put("wxa_appid", "wx-app-id-1");
		payload.put("inviter_id", 0L);

		listener.onEvent(payload);

		verify(wxaNoticePort)
				.send(
						argThat(
								m -> {
									if (!(m.get("company_id") instanceof Number c) || c.longValue() != 7L) {
										return false;
									}
									if (!"memberCreateSucc".equals(m.get("scenes_name"))) {
										return false;
									}
									if (!"wx-app-id-1".equals(m.get("appid"))) {
										return false;
									}
									if (!"o-test".equals(m.get("openid"))) {
										return false;
									}
									@SuppressWarnings("unchecked")
									Map<String, Object> data = (Map<String, Object>) m.get("data");
									if (data == null) {
										return false;
									}
									return "2026-05-07 12:00:00".equals(data.get("date"))
											&& "欢迎加入会员".equals(data.get("notice"));
								}));
	}
}
