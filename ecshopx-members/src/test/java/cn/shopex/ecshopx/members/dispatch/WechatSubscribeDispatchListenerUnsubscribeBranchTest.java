package cn.shopex.ecshopx.members.dispatch;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.members.domain.WechatFans;
import cn.shopex.ecshopx.members.domain.WechatFansBindWechatTag;
import cn.shopex.ecshopx.members.mapper.WechatFansBindWechatTagMapper;
import cn.shopex.ecshopx.members.mapper.WechatFansMapper;
import cn.shopex.ecshopx.members.service.WechatFansSyncPersistService;
import cn.shopex.ecshopx.wechat.mp.OfficialAccountUserInfoBatchService;
import cn.shopex.ecshopx.wechat.mp.OfficialAccountUserTagMemberService;
import cn.shopex.ecshopx.wechat.wxa.WechatOpenPlatformAuthorizerTokenService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class WechatSubscribeDispatchListenerUnsubscribeBranchTest {

	@Mock
	private WechatOpenPlatformAuthorizerTokenService tokenService;

	@Mock
	private OfficialAccountUserInfoBatchService userInfoBatchService;

	@Mock
	private WechatFansSyncPersistService wechatFansSyncPersistService;

	@Mock
	private OfficialAccountUserTagMemberService tagMemberService;

	@Mock
	private WechatFansBindWechatTagMapper wechatFansBindWechatTagMapper;

	@Mock
	private WechatFansMapper wechatFansMapper;

	@Mock
	private PlatformTransactionManager platformTransactionManager;

	@Test
	void onEvent_whenUnsubscribe_removesTagBindsUntagsAndSetsSubscribedFalse() {
		when(platformTransactionManager.getTransaction(any(TransactionDefinition.class)))
				.thenReturn(new SimpleTransactionStatus());
		doNothing().when(platformTransactionManager).commit(any());

		WechatSubscribeDispatchListener listener =
				new WechatSubscribeDispatchListener(
						tokenService,
						userInfoBatchService,
						wechatFansSyncPersistService,
						tagMemberService,
						wechatFansBindWechatTagMapper,
						wechatFansMapper,
						platformTransactionManager);

		String openId = "o-open";
		String authorizerAppId = "wx-auth";
		long companyId = 99L;
		long tagId = 77L;

		WechatFansBindWechatTag bind = new WechatFansBindWechatTag();
		bind.setTagId(tagId);
		bind.setOpenId(openId);
		bind.setCompanyId(companyId);
		bind.setAuthorizerAppid(authorizerAppId);

		when(wechatFansBindWechatTagMapper.selectList(any())).thenReturn(List.of(bind));

		WechatFans fan = new WechatFans();
		fan.setOpenId(openId);
		fan.setCompanyId(companyId);
		fan.setAuthorizerAppid(authorizerAppId);
		fan.setSubscribed(Boolean.TRUE);
		when(wechatFansMapper.selectOne(any())).thenReturn(fan);

		Map<String, Object> payload =
				Map.of(
						"event",
						"unsubscribe",
						"openId",
						openId,
						"authorizerAppId",
						authorizerAppId,
						"company_id",
						companyId);

		listener.onEvent(payload);

		verify(tagMemberService).untagUsers(eq(authorizerAppId), eq(List.of(openId)), eq(tagId));
		verify(wechatFansBindWechatTagMapper).delete(any());
		ArgumentCaptor<WechatFans> fanCaptor = ArgumentCaptor.forClass(WechatFans.class);
		verify(wechatFansMapper).updateById(fanCaptor.capture());
		assertFalse(fanCaptor.getValue().getSubscribed());

		verifyNoInteractions(userInfoBatchService, wechatFansSyncPersistService);
	}

	@Test
	void onEvent_whenUnsubscribe_andMissingRequiredFields_noSideEffects() {
		WechatSubscribeDispatchListener listener =
				new WechatSubscribeDispatchListener(
						tokenService,
						userInfoBatchService,
						wechatFansSyncPersistService,
						tagMemberService,
						wechatFansBindWechatTagMapper,
						wechatFansMapper,
						platformTransactionManager);

		listener.onEvent(
				Map.of(
						"event",
						"unsubscribe",
						"authorizerAppId",
						"wx",
						"company_id",
						1L));

		verifyNoInteractions(tagMemberService, wechatFansBindWechatTagMapper, wechatFansMapper);
	}
}
