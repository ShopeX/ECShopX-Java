package cn.shopex.ecshopx.members.service.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.MembersCreateMemberSuccessDispatchPublisher;
import cn.shopex.ecshopx.companys.service.protocol.ShopProtocolSetService;
import cn.shopex.ecshopx.members.dispatch.BindSalsepersonJobDispatchPublisher;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersDeleteRecordMapper;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmSettingReadPort;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringJUnitConfig(MemberAccountServiceDispatchIntegrationTestConfiguration.class)
@TestPropertySource(properties = "ecshopx.h5.encrypt-sensitive-data=false")
class MemberAccountServiceCreateMemberLocalAutoRegisterDispatchTest {

	@Autowired
	private MemberAccountService memberAccountService;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private MembersMapper membersMapper;

	@Autowired
	private MembersInfoMapper membersInfoMapper;

	@Autowired
	private MembersCreateMemberSuccessDispatchPublisher membersCreateMemberSuccessDispatchPublisher;

	@Autowired
	private BindSalsepersonJobDispatchPublisher bindSalsepersonJobDispatchPublisher;

	@Autowired
	private ShopProtocolSetService shopProtocolSetService;

	@Autowired
	private MembersDeleteRecordMapper membersDeleteRecordMapper;

	@Autowired
	private DmCrmSettingReadPort dmCrmSettingReadPort;

	@BeforeEach
	void stubPersistenceAndPromotion() {
		reset(
				membersMapper,
				membersInfoMapper,
				membersCreateMemberSuccessDispatchPublisher,
				bindSalsepersonJobDispatchPublisher,
				shopProtocolSetService,
				membersDeleteRecordMapper,
				dmCrmSettingReadPort);
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
	@SuppressWarnings("unchecked")
	void createMemberLocalAutoRegister_shouldPublishAfterCommitWhenExtrasProvided() {
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

		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(membersCreateMemberSuccessDispatchPublisher).publish(captor.capture());
		Map<String, Object> payload = captor.getValue();
		assertEquals(10L, payload.get("company_id"));
		assertEquals(901L, payload.get("user_id"));
		assertEquals("13800138000", payload.get("mobile"));
		assertEquals("openid-from-session", payload.get("openid"));
		assertEquals("wxapp-id-value", payload.get("wxa_appid"));
		assertEquals(3L, payload.get("source_id"));
		assertEquals(4L, payload.get("monitor_id"));
		assertEquals(5L, payload.get("inviter_id"));
		ArgumentCaptor<MembersInfo> infoCaptor = ArgumentCaptor.forClass(MembersInfo.class);
		verify(membersInfoMapper).insert(infoCaptor.capture());
		assertEquals("[]", infoCaptor.getValue().getOtherParams());
		verify(bindSalsepersonJobDispatchPublisher, never())
				.enqueueBindSalseperson(anyLong(), anyString(), anyString(), anyInt(), anyString(), anyLong());
		verify(bindSalsepersonJobDispatchPublisher, never())
				.enqueueBindSalsepersonAfterDmMemberCreate(anyLong(), anyString(), anyString(), anyInt(), anyString());
	}

	@Test
	void createMemberLocalAutoRegister_whenWorkUseridAndChannel1_shouldEnqueueBindSalsepersonAfterCommit() {
		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		Map<String, Object> extras = new LinkedHashMap<>();
		extras.put("work_userid", " work-x ");
		extras.put("channel", "1");
		extras.put("unionid", "wx-union");
		tt.execute(
				status -> {
					memberAccountService.createMemberLocalAutoRegister(
							10L, "13800138000", "secret-plain", extras);
					return null;
				});

		verify(bindSalsepersonJobDispatchPublisher)
				.enqueueBindSalseperson(eq(10L), eq("wx-union"), eq("work-x"), eq(1), eq("13800138000"), eq(901L));
		verify(bindSalsepersonJobDispatchPublisher, never())
				.enqueueBindSalsepersonAfterDmMemberCreate(anyLong(), anyString(), anyString(), anyInt(), anyString());
	}

	@Test
	void createMemberLocalAutoRegister_whenWorkUseridAndChannel1_andDmOpen_shouldEnqueueJob186AfterCommit() {
		when(dmCrmSettingReadPort.isPointIntegrationOpen(10L)).thenReturn(true);
		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		Map<String, Object> extras = new LinkedHashMap<>();
		extras.put("work_userid", " work-x ");
		extras.put("channel", "1");
		extras.put("unionid", "wx-union");
		tt.execute(
				status -> {
					memberAccountService.createMemberLocalAutoRegister(
							10L, "13800138000", "secret-plain", extras);
					return null;
				});

		verify(bindSalsepersonJobDispatchPublisher)
				.enqueueBindSalsepersonAfterDmMemberCreate(
						eq(10L), eq("wx-union"), eq("work-x"), eq(1), eq("13800138000"));
		verify(bindSalsepersonJobDispatchPublisher, never())
				.enqueueBindSalseperson(anyLong(), anyString(), anyString(), anyInt(), anyString(), anyLong());
	}

	@Test
	void createMemberLocalAutoRegister_whenChannelNot1_shouldNotEnqueueBindSalseperson() {
		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		Map<String, Object> extras = new LinkedHashMap<>();
		extras.put("work_userid", "w1");
		extras.put("channel", 2);
		extras.put("unionid", "u1");
		tt.execute(
				status -> {
					memberAccountService.createMemberLocalAutoRegister(
							10L, "13800138000", "secret-plain", extras);
					return null;
				});

		verify(bindSalsepersonJobDispatchPublisher, never())
				.enqueueBindSalseperson(anyLong(), anyString(), anyString(), anyInt(), anyString(), anyLong());
		verify(bindSalsepersonJobDispatchPublisher, never())
				.enqueueBindSalsepersonAfterDmMemberCreate(anyLong(), anyString(), anyString(), anyInt(), anyString());
	}

	@Test
	void createMemberLocalAutoRegister_whenWorkUseridBlank_shouldNotEnqueueBindSalseperson() {
		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		Map<String, Object> extras = new LinkedHashMap<>();
		extras.put("work_userid", "   ");
		extras.put("channel", 1);
		extras.put("unionid", "u1");
		tt.execute(
				status -> {
					memberAccountService.createMemberLocalAutoRegister(
							10L, "13800138000", "secret-plain", extras);
					return null;
				});

		verify(bindSalsepersonJobDispatchPublisher, never())
				.enqueueBindSalseperson(anyLong(), anyString(), anyString(), anyInt(), anyString(), anyLong());
		verify(bindSalsepersonJobDispatchPublisher, never())
				.enqueueBindSalsepersonAfterDmMemberCreate(anyLong(), anyString(), anyString(), anyInt(), anyString());
	}

	@Test
	void createMemberLocalAutoRegister_whenRollback_shouldNotEnqueueBindSalseperson() {
		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		tt.execute(
				status -> {
					status.setRollbackOnly();
					Map<String, Object> extras = new LinkedHashMap<>();
					extras.put("work_userid", "w1");
					extras.put("channel", 1);
					extras.put("unionid", "u1");
					memberAccountService.createMemberLocalAutoRegister(11L, "13900139000", "pw", extras);
					return null;
				});

		verify(membersCreateMemberSuccessDispatchPublisher, never()).publish(any());
		verify(bindSalsepersonJobDispatchPublisher, never())
				.enqueueBindSalseperson(anyLong(), anyString(), anyString(), anyInt(), anyString(), anyLong());
		verify(bindSalsepersonJobDispatchPublisher, never())
				.enqueueBindSalsepersonAfterDmMemberCreate(anyLong(), anyString(), anyString(), anyInt(), anyString());
	}

	@Test
	void createMemberLocalAutoRegister_shouldNotPublishWhenTransactionRollsBack() {
		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		tt.execute(
				status -> {
					status.setRollbackOnly();
					memberAccountService.createMemberLocalAutoRegister(11L, "13900139000", "pw", Map.of());
					return null;
				});

		verify(membersCreateMemberSuccessDispatchPublisher, never()).publish(any());
		verify(bindSalsepersonJobDispatchPublisher, never())
				.enqueueBindSalseperson(anyLong(), anyString(), anyString(), anyInt(), anyString(), anyLong());
		verify(bindSalsepersonJobDispatchPublisher, never())
				.enqueueBindSalsepersonAfterDmMemberCreate(anyLong(), anyString(), anyString(), anyInt(), anyString());
	}
}
