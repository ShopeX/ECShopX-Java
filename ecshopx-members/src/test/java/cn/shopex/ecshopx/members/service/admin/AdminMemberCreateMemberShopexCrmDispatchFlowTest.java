package cn.shopex.ecshopx.members.service.admin;

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
import cn.shopex.ecshopx.common.members.admin.AdminMemberCreatePostCommitDataPort;
import cn.shopex.ecshopx.companys.service.protocol.ShopProtocolSetService;
import cn.shopex.ecshopx.dispatch.DispatchConsumerRuntime;
import cn.shopex.ecshopx.dispatch.DispatchMessage;
import cn.shopex.ecshopx.dispatch.DispatchRetryDecider;
import cn.shopex.ecshopx.dispatch.DispatchStructuredLogger;
import cn.shopex.ecshopx.dispatch.FailedJobRecorder;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchConsumerStateRecorder;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchRegistry;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersDeleteRecordMapper;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
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

@SpringJUnitConfig({
	AdminMemberCreateMemberServiceDispatchIntegrationTestConfiguration.class,
	AdminMemberCreateMemberShopexCrmDispatchFlowTestConfiguration.class
})
@TestPropertySource(properties = "ecshopx.h5.encrypt-sensitive-data=false")
class AdminMemberCreateMemberShopexCrmDispatchFlowTest {

	@Autowired
	private AdminMemberCreateMemberService adminMemberCreateMemberService;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private MembersMapper membersMapper;

	@Autowired
	private MembersInfoMapper membersInfoMapper;

	@Autowired
	private ShopProtocolSetService shopProtocolSetService;

	@Autowired
	private MembersDeleteRecordMapper membersDeleteRecordMapper;

	@Autowired
	private MemberAccountService memberAccountService;

	@Autowired
	private AdminMemberCreatePostCommitDataPort adminMemberCreatePostCommitDataPort;

	@Autowired
	private MemberUserCardCodeAllocateService memberUserCardCodeAllocateService;

	@Autowired
	@Qualifier("adminCreateMemberShopexCrmFlowCapturedMessages")
	private List<DispatchMessage> capturedMessages;

	@Autowired
	@Qualifier("adminCreateMemberShopexCrmFlowInMemoryDispatchRegistry")
	private InMemoryDispatchRegistry dispatchRegistry;

	@Autowired
	@Qualifier("adminCreateMemberShopexCrmFlowShopexCrmSyncSingleMemberPort")
	private ShopexCrmSyncSingleMemberPort shopexCrmPort;

	@BeforeEach
	void stubPersistenceAndPorts() {
		capturedMessages.clear();
		reset(
				membersMapper,
				membersInfoMapper,
				shopProtocolSetService,
				membersDeleteRecordMapper,
				memberAccountService,
				adminMemberCreatePostCommitDataPort,
				memberUserCardCodeAllocateService,
				shopexCrmPort);
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
		when(memberUserCardCodeAllocateService.allocateCode()).thenReturn("CARD01");
		when(memberAccountService.getInfoByMobile(anyLong(), anyString())).thenReturn(null);
		when(memberAccountService.getMemberInfo(anyLong(), anyLong()))
				.thenReturn(
						new LinkedHashMap<>(
								Map.of(
										"user_id",
										901L,
										"company_id",
										10L,
										"grade_id",
										1L,
										"mobile",
										"13800138000")));
		when(adminMemberCreatePostCommitDataPort.loadGradeInfo(anyLong(), anyLong()))
				.thenReturn(
						new LinkedHashMap<>(
								Map.of(
										"grade_id",
										1L,
										"company_id",
										10L,
										"grade_name",
										"默认",
										"default_grade",
										true)));
		when(adminMemberCreatePostCommitDataPort.loadVipGrade(anyLong(), anyLong()))
				.thenReturn(new LinkedHashMap<>(Map.of("is_vip", false)));
	}

	@Test
	void createMember_afterCommit_publishFanOut_thenConsume_invokesShopexCrmPort() {
		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		Map<String, Object> postData = new LinkedHashMap<>();
		postData.put("mobile", "13800138000");
		tt.execute(
				status -> {
					adminMemberCreateMemberService.createMember(10L, postData);
					return null;
				});

		assertEquals(1, capturedMessages.size());
		DispatchMessage msg = capturedMessages.get(0);
		assertEquals(MembersDispatchEventNames.LISTENER_THIRDPARTY_SHOPEX_CRM_SYNC_ADD_MEMBER, msg.listenerName());
		assertEquals("default", msg.queue());
		Map<String, Object> payload = msg.payload();
		assertEquals(10L, payload.get("company_id"));
		assertEquals(901L, payload.get("user_id"));
		assertEquals("13800138000", payload.get("mobile"));
		assertEquals("", payload.get("openid"));
		assertEquals("", payload.get("wxa_appid"));
		assertEquals(0L, payload.get("source_id"));
		assertEquals(0L, payload.get("monitor_id"));
		assertEquals(0L, payload.get("inviter_id"));
		assertEquals(0L, payload.get("salesperson_id"));
		assertEquals(0L, payload.get("distributor_id"));
		assertEquals(true, payload.get("if_register_promotion"));

		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						dispatchRegistry,
						new DispatchRetryDecider(),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());
		runtime.consume(msg, 1);
		verify(shopexCrmPort).syncUpdatedMember(10L, 901L);
	}

	@Test
	void createMember_whenRollback_shouldNotCaptureShopexMessage() {
		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		Map<String, Object> postData = new LinkedHashMap<>();
		postData.put("mobile", "13900139000");
		tt.execute(
				status -> {
					status.setRollbackOnly();
					adminMemberCreateMemberService.createMember(11L, postData);
					return null;
				});

		assertTrue(capturedMessages.isEmpty());
		verify(shopexCrmPort, never()).syncUpdatedMember(anyLong(), anyLong());
	}
}
