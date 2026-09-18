package cn.shopex.ecshopx.members.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.MembersDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchConsumerRuntime;
import cn.shopex.ecshopx.dispatch.DispatchMessage;
import cn.shopex.ecshopx.dispatch.DispatchRetryDecider;
import cn.shopex.ecshopx.dispatch.DispatchStructuredLogger;
import cn.shopex.ecshopx.dispatch.FailedJobRecorder;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchConsumerStateRecorder;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchRegistry;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
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
	AdminMemberMembersInfoUpdateDispatchIntegrationTestConfiguration.class,
	AdminMemberMembersInfoUpdateShopexCrmDispatchFlowTestConfiguration.class
})
@TestPropertySource(properties = "ecshopx.h5.encrypt-sensitive-data=false")
class AdminMemberMembersInfoUpdateShopexCrmDispatchFlowTest {

	@Autowired
	private AdminMemberMembersInfoUpdateService adminMemberMembersInfoUpdateService;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private MembersInfoMapper membersInfoMapper;

	@Autowired
	private MemberAccountService memberAccountService;

	@Autowired
	@Qualifier("adminUpdateMemberShopexCrmFlowCapturedMessages")
	private List<DispatchMessage> capturedMessages;

	@Autowired
	@Qualifier("adminUpdateMemberShopexCrmFlowInMemoryDispatchRegistry")
	private InMemoryDispatchRegistry dispatchRegistry;

	@Autowired
	@Qualifier("adminUpdateMemberShopexCrmFlowShopexCrmSyncSingleMemberPort")
	private ShopexCrmSyncSingleMemberPort shopexCrmPort;

	@BeforeEach
	void stubPersistenceAndPorts() {
		capturedMessages.clear();
		reset(membersInfoMapper, memberAccountService, shopexCrmPort);
		MembersInfo before = new MembersInfo();
		before.setCompanyId(10L);
		before.setUserId(901L);
		before.setOtherParams("[]");
		MembersInfo after = new MembersInfo();
		after.setCompanyId(10L);
		after.setUserId(901L);
		after.setOtherParams("[]");
		when(membersInfoMapper.selectOne(any())).thenReturn(before).thenReturn(after);
		when(membersInfoMapper.updateById(any(MembersInfo.class))).thenReturn(1);
		LinkedHashMap<String, Object> apiMap = new LinkedHashMap<>();
		apiMap.put("company_id", 10L);
		apiMap.put("user_id", 901L);
		apiMap.put("username", "u1");
		when(memberAccountService.toMembersInfoApiMap(any(MembersInfo.class), eq(false))).thenReturn(apiMap);
	}

	@Test
	void updateMember_afterCommit_publishFanOut_thenConsume_invokesShopexCrmPort() {
		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		Map<String, Object> postdata = new LinkedHashMap<>();
		postdata.put("username", "u1");
		tt.execute(
				status -> {
					adminMemberMembersInfoUpdateService.updateMember(10L, 901L, postdata);
					return null;
				});

		assertEquals(1, capturedMessages.size());
		DispatchMessage msg = capturedMessages.get(0);
		assertEquals(MembersDispatchEventNames.LISTENER_THIRDPARTY_SHOPEX_CRM_SYNC_UPDATE_MEMBER, msg.listenerName());
		assertEquals("default", msg.queue());
		Map<String, Object> payload = msg.payload();
		assertEquals(10L, payload.get("company_id"));
		assertEquals(901L, payload.get("user_id"));

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
	void updateMember_whenRollback_shouldNotCaptureShopexMessage() {
		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		Map<String, Object> postdata = new LinkedHashMap<>();
		postdata.put("username", "u2");
		tt.execute(
				status -> {
					status.setRollbackOnly();
					adminMemberMembersInfoUpdateService.updateMember(10L, 901L, postdata);
					return null;
				});

		assertTrue(capturedMessages.isEmpty());
		verify(shopexCrmPort, never()).syncUpdatedMember(anyLong(), anyLong());
	}
}
