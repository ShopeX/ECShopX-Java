package cn.shopex.ecshopx.members.service.h5;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
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
import cn.shopex.ecshopx.members.integration.front.FrontMemberInfoUpdateValidationPort;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
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

@SpringJUnitConfig({
	WxappMemberUpdateMemberDispatchTransactionTestConfiguration.class,
	WxappMemberUpdateMemberShopexCrmDispatchFlowTestConfiguration.class
})
@TestPropertySource(properties = {"ecshopx.h5.encrypt-sensitive-data=false", "crm.crm-sync=enabled"})
class WxappMemberUpdateMemberShopexCrmDispatchFlowTest {

	@Autowired
	private WxappMemberUpdateMemberService wxappMemberUpdateMemberService;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private MembersInfoMapper membersInfoMapper;

	@Autowired
	private MemberAccountService memberAccountService;

	@Autowired
	private FrontMemberInfoUpdateValidationPort frontMemberInfoUpdateValidationPort;

	@Autowired
	private DmCrmSettingReadPort dmCrmSettingReadPort;

	@Autowired
	@Qualifier("wxappUpdateMemberShopexCrmFlowCapturedMessages")
	private List<DispatchMessage> capturedMessages;

	@Autowired
	@Qualifier("wxappUpdateMemberShopexCrmFlowInMemoryDispatchRegistry")
	private InMemoryDispatchRegistry dispatchRegistry;

	@Autowired
	@Qualifier("wxappUpdateMemberShopexCrmFlowShopexCrmSyncSingleMemberPort")
	private ShopexCrmSyncSingleMemberPort shopexCrmPort;

	@BeforeEach
	void stubCollaborators() {
		capturedMessages.clear();
		reset(membersInfoMapper, memberAccountService, frontMemberInfoUpdateValidationPort, dmCrmSettingReadPort, shopexCrmPort);
		when(frontMemberInfoUpdateValidationPort.prepareAndValidate(anyLong(), anyMap(), anyString()))
				.thenReturn(new LinkedHashMap<>());
		when(dmCrmSettingReadPort.isPointIntegrationOpen(anyLong())).thenReturn(false);
		MembersInfo fresh = new MembersInfo();
		fresh.setCompanyId(10L);
		fresh.setUserId(901L);
		when(membersInfoMapper.selectOne(any())).thenReturn(fresh);
		LinkedHashMap<String, Object> apiMap = new LinkedHashMap<>();
		apiMap.put("company_id", 10L);
		apiMap.put("user_id", 901L);
		apiMap.put("username", "u1");
		when(memberAccountService.toMembersInfoApiMap(any(MembersInfo.class), eq(false))).thenReturn(apiMap);
		when(memberAccountService.getMemberInfo(901L, 10L)).thenReturn(new LinkedHashMap<>());
	}

	@Test
	void updateMember_afterCommit_publishFanOut_thenConsume_invokesShopexCrmPort() {
		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		Map<String, Object> postData = new LinkedHashMap<>();
		postData.put("username", "u1");
		tt.execute(
				status -> {
					wxappMemberUpdateMemberService.updateMember(
							10L, 901L, null, postData, "", "", "", "", false, "");
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
		Map<String, Object> postData = new LinkedHashMap<>();
		postData.put("username", "u2");
		tt.execute(
				status -> {
					status.setRollbackOnly();
					wxappMemberUpdateMemberService.updateMember(
							10L, 901L, null, postData, "", "", "", "", false, "");
					return null;
				});

		assertTrue(capturedMessages.isEmpty());
		verify(shopexCrmPort, never()).syncUpdatedMember(anyLong(), anyLong());
	}
}
