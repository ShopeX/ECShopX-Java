package cn.shopex.ecshopx.members.service.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.MembersDispatchEventNames;
import cn.shopex.ecshopx.companys.service.protocol.ShopProtocolSetService;
import cn.shopex.ecshopx.dispatch.DispatchConsumerRuntime;
import cn.shopex.ecshopx.dispatch.DispatchMessage;
import cn.shopex.ecshopx.dispatch.DispatchRetryDecider;
import cn.shopex.ecshopx.dispatch.DispatchStructuredLogger;
import cn.shopex.ecshopx.dispatch.FailedJobRecorder;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchConsumerStateRecorder;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchRegistry;
import cn.shopex.ecshopx.members.dispatch.BindSalsepersonJobDispatchPublisher;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersDeleteRecordMapper;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmSettingReadPort;
import cn.shopex.ecshopx.thirdparty.service.shopexcrm.ShopexCrmSyncSingleMemberPort;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * In-memory dispatch only: exercises the Shopex CRM listener tuple for local auto-register
 * without registering a second production {@code registerEventListener} wiring.
 */
@SpringJUnitConfig({
	MemberAccountServiceDispatchIntegrationTestConfiguration.class,
	MemberAccountServiceCreateMemberLocalAutoRegisterShopexCrmDispatchFlowTestConfiguration.class
})
@TestPropertySource(properties = "ecshopx.h5.encrypt-sensitive-data=false")
class MemberAccountServiceCreateMemberLocalAutoRegisterShopexCrmDispatchFlowTest {

	@Autowired
	private MemberAccountService memberAccountService;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private MembersMapper membersMapper;

	@Autowired
	private MembersInfoMapper membersInfoMapper;

	@Autowired
	private BindSalsepersonJobDispatchPublisher bindSalsepersonJobDispatchPublisher;

	@Autowired
	private ShopProtocolSetService shopProtocolSetService;

	@Autowired
	private MembersDeleteRecordMapper membersDeleteRecordMapper;

	@Autowired
	private DmCrmSettingReadPort dmCrmSettingReadPort;

	@Autowired
	@Qualifier("localAutoRegisterShopexCrmDispatchCapturedMessages")
	private List<DispatchMessage> capturedMessages;

	@Autowired
	@Qualifier("localAutoRegisterInMemoryDispatchRegistry")
	private InMemoryDispatchRegistry dispatchRegistry;

	@Autowired
	@Qualifier("localAutoRegisterShopexCrmSyncSingleMemberPort")
	private ShopexCrmSyncSingleMemberPort localAutoRegisterShopexCrmSyncSingleMemberPort;

	@BeforeEach
	void stubPersistenceAndPromotion() {
		capturedMessages.clear();
		reset(
				membersMapper,
				membersInfoMapper,
				bindSalsepersonJobDispatchPublisher,
				shopProtocolSetService,
				membersDeleteRecordMapper,
				dmCrmSettingReadPort,
				localAutoRegisterShopexCrmSyncSingleMemberPort);
		when(dmCrmSettingReadPort.isPointIntegrationOpen(anyLong())).thenReturn(false);
		when(membersMapper.selectDefaultGradeId(anyString())).thenReturn(1L);
		when(membersMapper.insert(any(Members.class)))
				.thenAnswer(
						inv -> {
							Members mm = inv.getArgument(0);
							mm.setUserId(901L);
							return 1;
						});
		when(membersInfoMapper.insert(any(MembersInfo.class))).thenReturn(1);
		when(shopProtocolSetService.get(anyLong(), anyString(), anyString())).thenReturn(Map.of());
		when(membersDeleteRecordMapper.selectOne(any())).thenReturn(null);
	}

	@Test
	void createMemberLocalAutoRegister_afterCommit_publishFanOut_thenConsume_invokesShopexCrmPort() {
		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		Map<String, Object> extras = new LinkedHashMap<>();
		extras.put("open_id", "openid-from-session");
		extras.put("wxa_appid", "wxapp-id-value");
		extras.put("source_id", 3L);
		extras.put("monitor_id", 4L);
		extras.put("inviter_id", 5L);
		tt.execute(
				status -> {
					memberAccountService.createMemberLocalAutoRegister(
							10L, "13800138000", "secret-plain", extras);
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
		verify(localAutoRegisterShopexCrmSyncSingleMemberPort).syncUpdatedMember(10L, 901L);
	}

	@Test
	void createMemberLocalAutoRegister_whenRollback_shouldNotCaptureShopexMessage() {
		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		tt.execute(
				status -> {
					status.setRollbackOnly();
					Map<String, Object> extras = new LinkedHashMap<>();
					extras.put("open_id", "openid-from-session");
					extras.put("wxa_appid", "wxapp-id-value");
					extras.put("source_id", 3L);
					extras.put("monitor_id", 4L);
					extras.put("inviter_id", 5L);
					memberAccountService.createMemberLocalAutoRegister(11L, "13900139000", "pw", extras);
					return null;
				});

		assertTrue(capturedMessages.isEmpty());
		verify(localAutoRegisterShopexCrmSyncSingleMemberPort, never())
				.syncUpdatedMember(anyLong(), anyLong());
	}
}
