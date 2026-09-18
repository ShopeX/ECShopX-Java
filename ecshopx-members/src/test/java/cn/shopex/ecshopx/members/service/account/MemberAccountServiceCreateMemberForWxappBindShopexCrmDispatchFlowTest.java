package cn.shopex.ecshopx.members.service.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.MembersDispatchEventNames;
import cn.shopex.ecshopx.companys.service.protocol.ShopProtocolSetService;
import cn.shopex.ecshopx.dispatch.DispatchConsumerRuntime;
import cn.shopex.ecshopx.dispatch.DispatchCore;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchFanOutPlanner;
import cn.shopex.ecshopx.dispatch.DispatchMessage;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.DispatchRetryDecider;
import cn.shopex.ecshopx.dispatch.DispatchStructuredLogger;
import cn.shopex.ecshopx.dispatch.FailedJobRecorder;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchConsumerStateRecorder;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchRegistry;
import cn.shopex.ecshopx.dispatch.ListenerDispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import cn.shopex.ecshopx.dispatch.SyncDispatchDriver;
import cn.shopex.ecshopx.members.dispatch.BindSalsepersonJobDispatchPublisher;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.mapper.MembersDeleteRecordMapper;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.thirdparty.dispatch.CreateMemberSuccessShopexCrmSyncDispatchListener;
import cn.shopex.ecshopx.thirdparty.service.shopexcrm.CreateMemberSuccessShopexCrmSyncExecutionService;
import cn.shopex.ecshopx.thirdparty.service.shopexcrm.ShopexCrmSyncSingleMemberPort;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * In-memory dispatch only: exercises the Shopex CRM listener tuple for a wxapp-bind-shaped payload
 * without registering a second production {@code registerEventListener} wiring.
 */
class MemberAccountServiceCreateMemberForWxappBindShopexCrmDispatchFlowTest {

	@Test
	void bindShapedPayload_publishThenConsume_invokesShopexCrmPort() {
		ShopexCrmSyncSingleMemberPort port = mock(ShopexCrmSyncSingleMemberPort.class);
		CreateMemberSuccessShopexCrmSyncExecutionService executionService =
				new CreateMemberSuccessShopexCrmSyncExecutionService(port, "enabled");
		CreateMemberSuccessShopexCrmSyncDispatchListener listener =
				new CreateMemberSuccessShopexCrmSyncDispatchListener(executionService);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				MembersDispatchEventNames.EVENT_CREATE_MEMBER_SUCCESS,
				MembersDispatchEventNames.LISTENER_THIRDPARTY_SHOPEX_CRM_SYNC_ADD_MEMBER,
				ListenerDispatchOptions.asyncDefaults(),
				listener);

		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("company_id", 10L);
		payload.put("user_id", 902L);
		payload.put("mobile", "13800138002");
		payload.put("openid", "openid-wxapp");
		payload.put("wxa_appid", "wxa-bind");
		payload.put("inviter_id", 51L);
		payload.put("distributor_id", 0L);
		payload.put("source_id", 31L);
		payload.put("monitor_id", 41L);
		payload.put("salesperson_id", 0L);
		payload.put("if_register_promotion", true);

		facade.publishEvent(
				MembersDispatchEventNames.EVENT_CREATE_MEMBER_SUCCESS,
				payload,
				new DispatchOptions(
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						null,
						null,
						RetryPolicy.platformDefault()));

		assertEquals(1, captured.size());
		DispatchMessage msg = captured.get(0);
		assertEquals(MembersDispatchEventNames.LISTENER_THIRDPARTY_SHOPEX_CRM_SYNC_ADD_MEMBER, msg.listenerName());
		assertEquals("default", msg.queue());

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);
		verify(port).syncUpdatedMember(10L, 902L);
	}

	@Nested
	@SpringJUnitConfig({
		MemberAccountServiceDispatchIntegrationTestConfiguration.class,
		MemberAccountServiceWxappBindShopexCrmDispatchFlowTestConfiguration.class
	})
	@TestPropertySource(properties = "ecshopx.h5.encrypt-sensitive-data=false")
	class CreateMemberForWxappBindAfterCommitShopexCrmDispatchFlow {

		@Autowired
		private MemberAccountService memberAccountService;

		@Autowired
		private PlatformTransactionManager transactionManager;

		@Autowired
		private MembersMapper membersMapper;

		@Autowired
		private MembersInfoMapper membersInfoMapper;

		@Autowired
		private MembersAssociationsMapper membersAssociationsMapper;

		@Autowired
		private BindSalsepersonJobDispatchPublisher bindSalsepersonJobDispatchPublisher;

		@Autowired
		private ShopProtocolSetService shopProtocolSetService;

		@Autowired
		private MembersDeleteRecordMapper membersDeleteRecordMapper;

		@Autowired
		@Qualifier("wxappBindShopexCrmDispatchCapturedMessages")
		private List<DispatchMessage> capturedMessages;

		@Autowired
		@Qualifier("wxappBindInMemoryDispatchRegistry")
		private InMemoryDispatchRegistry dispatchRegistry;

		@Autowired
		private ShopexCrmSyncSingleMemberPort wxappBindShopexCrmSyncSingleMemberPort;

		@BeforeEach
		void stubPersistence() {
			capturedMessages.clear();
			reset(
					membersMapper,
					membersInfoMapper,
					membersAssociationsMapper,
					bindSalsepersonJobDispatchPublisher,
					shopProtocolSetService,
					membersDeleteRecordMapper,
					wxappBindShopexCrmSyncSingleMemberPort);
			when(membersMapper.selectDefaultGradeId(anyString())).thenReturn(1L);
			when(membersMapper.insert(any(Members.class)))
					.thenAnswer(
							inv -> {
								Members mm = inv.getArgument(0);
								mm.setUserId(902L);
								return 1;
							});
			when(membersInfoMapper.insert(any(MembersInfo.class))).thenReturn(1);
			when(membersAssociationsMapper.selectOne(any())).thenReturn(null);
			when(membersAssociationsMapper.insert(any(MembersAssociations.class))).thenReturn(1);
			when(shopProtocolSetService.get(anyLong(), anyString(), anyString())).thenReturn(Map.of());
			when(membersDeleteRecordMapper.selectOne(any())).thenReturn(null);
		}

		@Test
		void createMemberForWxappBind_afterCommit_fanOutShopexListener_consume_invokesShopexCrmPort() {
			TransactionTemplate tt = new TransactionTemplate(transactionManager);
			Map<String, Object> wechatUserRow = new LinkedHashMap<>();
			wechatUserRow.put("unionid", "union-wxapp");
			wechatUserRow.put("open_id", "openid-wxapp");
			wechatUserRow.put("authorizer_appid", "auth-app");
			wechatUserRow.put("nickname", "nick");
			wechatUserRow.put("headimgurl", "");

			Map<String, Object> requestExtras = new LinkedHashMap<>();
			requestExtras.put("wxa_appid", "wxa-bind");
			requestExtras.put("source_id", 31L);
			requestExtras.put("monitor_id", 41L);
			requestExtras.put("inviter_id", 51L);

			tt.execute(
					status -> {
						memberAccountService.createMemberForWxappBind(
								10L, "13800138002", "plain-pwd", wechatUserRow, requestExtras);
						return null;
					});

			assertEquals(1, capturedMessages.size());
			DispatchMessage msg = capturedMessages.get(0);
			assertEquals(MembersDispatchEventNames.LISTENER_THIRDPARTY_SHOPEX_CRM_SYNC_ADD_MEMBER, msg.listenerName());
			assertEquals("default", msg.queue());

			DispatchConsumerRuntime runtime =
					new DispatchConsumerRuntime(
							dispatchRegistry,
							new DispatchRetryDecider(),
							mock(FailedJobRecorder.class),
							mock(DispatchStructuredLogger.class),
							new InMemoryDispatchConsumerStateRecorder());
			runtime.consume(msg, 1);
			verify(wxappBindShopexCrmSyncSingleMemberPort).syncUpdatedMember(10L, 902L);
		}
	}
}
