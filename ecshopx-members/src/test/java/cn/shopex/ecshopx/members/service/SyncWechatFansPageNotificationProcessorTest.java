package cn.shopex.ecshopx.members.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.wechat.mp.OfficialAccountUserInfoBatchService;
import cn.shopex.ecshopx.wechat.wxa.WechatOpenPlatformAuthorizerTokenService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SyncWechatFansPageNotificationProcessorTest {

	@Mock
	private WechatOpenPlatformAuthorizerTokenService tokenService;

	@Mock
	private OfficialAccountUserInfoBatchService officialAccountUserInfoBatchService;

	@Mock
	private WechatFansSyncPersistService wechatFansSyncPersistService;

	@InjectMocks
	private SyncWechatFansPageNotificationProcessor processor;

	@Test
	void handle_invokesPersistWithBatchUserInfo() {
		when(tokenService.getAuthorizerAccessToken("wx-test")).thenReturn("tok");

		List<Map<String, Object>> batchResult = new ArrayList<>();
		Map<String, Object> u1 = new HashMap<>();
		u1.put("subscribe", 1);
		batchResult.add(u1);
		when(officialAccountUserInfoBatchService.batchGetUserInfo(eq("tok"), any()))
				.thenReturn(batchResult);

		Map<String, Object> payload = new HashMap<>();
		payload.put("company_id", 7L);
		payload.put("authorizer_appid", "wx-test");
		payload.put("count", 1);
		payload.put("open_ids", List.of("openid-a"));

		processor.handle(payload);

		verify(wechatFansSyncPersistService).saveUser(eq("wx-test"), eq(7L), eq(batchResult));
	}
}
