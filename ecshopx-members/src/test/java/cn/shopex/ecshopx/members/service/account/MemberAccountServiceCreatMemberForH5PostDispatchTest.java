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
import cn.shopex.ecshopx.members.dispatch.BindSalsepersonJobDispatchPublisher;
import cn.shopex.ecshopx.companys.service.protocol.ShopProtocolSetService;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersDeleteRecordMapper;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmSettingReadPort;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
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
class MemberAccountServiceCreatMemberForH5PostDispatchTest {

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
							mm.setUserId(902L);
							return 1;
						});
		when(membersInfoMapper.insert(any(MembersInfo.class))).thenReturn(1);
		when(shopProtocolSetService.get(anyLong(), anyString(), anyString())).thenReturn(Map.of());
		when(membersDeleteRecordMapper.selectOne(any())).thenReturn(null);
	}

	@Test
	@SuppressWarnings("unchecked")
	void creatMemberForH5Post_newMember_shouldPublishAfterCommitWithExpectedPayload() {
		AtomicInteger membersSelectOneCalls = new AtomicInteger();
		when(membersMapper.selectOne(any()))
				.thenAnswer(
						inv -> {
							int n = membersSelectOneCalls.incrementAndGet();
							if (n == 1) {
								return null;
							}
							Members m = new Members();
							m.setUserId(902L);
							m.setCompanyId(10L);
							m.setGradeId(1L);
							m.setMobile("");
							m.setRegionMobile("13800138002");
							m.setMobileCountryCode("86");
							m.setUserCardCode("card");
							m.setCreated(1L);
							m.setUpdated(1L);
							m.setDisabled(false);
							m.setSourceFrom("default");
							return m;
						});

		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		Map<String, Object> postData = new LinkedHashMap<>();
		postData.put("company_id", 10L);
		postData.put("mobile", "13800138002");
		postData.put("api_from", "h5");
		postData.put("auth_type", "");
		postData.put("force_password", 0);
		postData.put("open_id", "openid-h5");
		postData.put("wxa_appid", "wxa-h5");
		postData.put("source_id", 31L);
		postData.put("monitor_id", 41L);
		postData.put("inviter_id", 51L);

		tt.execute(
				status -> {
					memberAccountService.creatMemberForH5Post(postData, true);
					return null;
				});

		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(membersCreateMemberSuccessDispatchPublisher).publish(captor.capture());
		Map<String, Object> payload = captor.getValue();
		assertEquals(10L, payload.get("company_id"));
		assertEquals(902L, payload.get("user_id"));
		assertEquals("13800138002", payload.get("mobile"));
		assertEquals("openid-h5", payload.get("openid"));
		assertEquals("wxa-h5", payload.get("wxa_appid"));
		assertEquals(31L, payload.get("source_id"));
		assertEquals(41L, payload.get("monitor_id"));
		assertEquals(51L, payload.get("inviter_id"));
		assertEquals(0L, payload.get("distributor_id"));
		assertEquals(0L, payload.get("salesperson_id"));
		assertEquals(true, payload.get("if_register_promotion"));
	}

	@Test
	void creatMemberForH5Post_newMember_whenWorkUseridAndChannel1_shouldEnqueueBindSalsepersonAfterCommit() {
		AtomicInteger membersSelectOneCalls = new AtomicInteger();
		when(membersMapper.selectOne(any()))
				.thenAnswer(
						inv -> {
							int n = membersSelectOneCalls.incrementAndGet();
							if (n == 1) {
								return null;
							}
							Members m = new Members();
							m.setUserId(902L);
							m.setCompanyId(10L);
							m.setGradeId(1L);
							m.setMobile("");
							m.setRegionMobile("13800138002");
							m.setMobileCountryCode("86");
							m.setUserCardCode("card");
							m.setCreated(1L);
							m.setUpdated(1L);
							m.setDisabled(false);
							m.setSourceFrom("default");
							return m;
						});

		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		Map<String, Object> postData = new LinkedHashMap<>();
		postData.put("company_id", 10L);
		postData.put("mobile", "13800138002");
		postData.put("api_from", "h5");
		postData.put("auth_type", "");
		postData.put("force_password", 0);
		postData.put("open_id", "openid-h5");
		postData.put("wxa_appid", "wxa-h5");
		postData.put("source_id", 31L);
		postData.put("monitor_id", 41L);
		postData.put("inviter_id", 51L);
		postData.put("work_userid", "wu-x");
		postData.put("unionid", "union-x");
		postData.put("channel", 1);

		tt.execute(
				status -> {
					memberAccountService.creatMemberForH5Post(postData, true);
					return null;
				});

		verify(bindSalsepersonJobDispatchPublisher)
				.enqueueBindSalseperson(eq(10L), eq("union-x"), eq("wu-x"), eq(1), eq("13800138002"), eq(902L));
	}

	@Test
	void creatMemberForH5Post_newMember_whenChannelNot1_shouldNotEnqueueBindSalseperson() {
		AtomicInteger membersSelectOneCalls = new AtomicInteger();
		when(membersMapper.selectOne(any()))
				.thenAnswer(
						inv -> {
							int n = membersSelectOneCalls.incrementAndGet();
							if (n == 1) {
								return null;
							}
							Members m = new Members();
							m.setUserId(902L);
							m.setCompanyId(10L);
							m.setGradeId(1L);
							m.setMobile("");
							m.setRegionMobile("13800138002");
							m.setMobileCountryCode("86");
							m.setUserCardCode("card");
							m.setCreated(1L);
							m.setUpdated(1L);
							m.setDisabled(false);
							m.setSourceFrom("default");
							return m;
						});

		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		Map<String, Object> postData = new LinkedHashMap<>();
		postData.put("company_id", 10L);
		postData.put("mobile", "13800138002");
		postData.put("api_from", "h5");
		postData.put("auth_type", "");
		postData.put("force_password", 0);
		postData.put("open_id", "openid-h5");
		postData.put("wxa_appid", "wxa-h5");
		postData.put("work_userid", "wu-x");
		postData.put("unionid", "union-x");
		postData.put("channel", 2);

		tt.execute(
				status -> {
					memberAccountService.creatMemberForH5Post(postData, true);
					return null;
				});

		verify(bindSalsepersonJobDispatchPublisher, never())
				.enqueueBindSalseperson(anyLong(), anyString(), anyString(), anyInt(), anyString(), anyLong());
	}

	@Test
	void creatMemberForH5Post_newMember_whenWorkUseridBlank_shouldNotEnqueueBindSalseperson() {
		AtomicInteger membersSelectOneCalls = new AtomicInteger();
		when(membersMapper.selectOne(any()))
				.thenAnswer(
						inv -> {
							int n = membersSelectOneCalls.incrementAndGet();
							if (n == 1) {
								return null;
							}
							Members m = new Members();
							m.setUserId(902L);
							m.setCompanyId(10L);
							m.setGradeId(1L);
							m.setMobile("");
							m.setRegionMobile("13800138002");
							m.setMobileCountryCode("86");
							m.setUserCardCode("card");
							m.setCreated(1L);
							m.setUpdated(1L);
							m.setDisabled(false);
							m.setSourceFrom("default");
							return m;
						});

		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		Map<String, Object> postData = new LinkedHashMap<>();
		postData.put("company_id", 10L);
		postData.put("mobile", "13800138002");
		postData.put("api_from", "h5");
		postData.put("auth_type", "");
		postData.put("force_password", 0);
		postData.put("open_id", "openid-h5");
		postData.put("wxa_appid", "wxa-h5");
		postData.put("work_userid", "  ");
		postData.put("unionid", "union-x");
		postData.put("channel", 1);

		tt.execute(
				status -> {
					memberAccountService.creatMemberForH5Post(postData, true);
					return null;
				});

		verify(bindSalsepersonJobDispatchPublisher, never())
				.enqueueBindSalseperson(anyLong(), anyString(), anyString(), anyInt(), anyString(), anyLong());
	}

	@Test
	void creatMemberForH5Post_whenRollback_shouldNotEnqueueBindSalseperson() {
		AtomicInteger membersSelectOneCalls = new AtomicInteger();
		when(membersMapper.selectOne(any()))
				.thenAnswer(
						inv -> {
							int n = membersSelectOneCalls.incrementAndGet();
							if (n == 1) {
								return null;
							}
							Members m = new Members();
							m.setUserId(903L);
							m.setCompanyId(11L);
							m.setGradeId(1L);
							m.setMobile("");
							m.setRegionMobile("13900139003");
							m.setMobileCountryCode("86");
							m.setUserCardCode("card");
							m.setCreated(1L);
							m.setUpdated(1L);
							m.setDisabled(false);
							m.setSourceFrom("default");
							return m;
						});

		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		Map<String, Object> postData = new LinkedHashMap<>();
		postData.put("company_id", 11L);
		postData.put("mobile", "13900139003");
		postData.put("api_from", "h5");
		postData.put("auth_type", "");
		postData.put("force_password", 0);
		postData.put("work_userid", "wu-rollback");
		postData.put("unionid", "union-rollback");
		postData.put("channel", 1);

		tt.execute(
				status -> {
					status.setRollbackOnly();
					memberAccountService.creatMemberForH5Post(postData, true);
					return null;
				});

		verify(membersCreateMemberSuccessDispatchPublisher, never()).publish(any());
		verify(bindSalsepersonJobDispatchPublisher, never())
				.enqueueBindSalseperson(anyLong(), anyString(), anyString(), anyInt(), anyString(), anyLong());
	}

	@Test
	@SuppressWarnings("unchecked")
	void creatMemberForH5Post_whenPointIntegrationOpen_stillCreatesMemberAndPublishesAfterCommit() {
		when(dmCrmSettingReadPort.isPointIntegrationOpen(anyLong())).thenReturn(true);

		AtomicInteger membersSelectOneCalls = new AtomicInteger();
		when(membersMapper.selectOne(any()))
				.thenAnswer(
						inv -> {
							int n = membersSelectOneCalls.incrementAndGet();
							if (n == 1) {
								return null;
							}
							Members m = new Members();
							m.setUserId(902L);
							m.setCompanyId(10L);
							m.setGradeId(1L);
							m.setMobile("");
							m.setRegionMobile("13800138002");
							m.setMobileCountryCode("86");
							m.setUserCardCode("card");
							m.setCreated(1L);
							m.setUpdated(1L);
							m.setDisabled(false);
							m.setSourceFrom("default");
							return m;
						});

		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		Map<String, Object> postData = new LinkedHashMap<>();
		postData.put("company_id", 10L);
		postData.put("mobile", "13800138002");
		postData.put("api_from", "h5");
		postData.put("auth_type", "");
		postData.put("force_password", 0);
		postData.put("open_id", "openid-h5");
		postData.put("wxa_appid", "wxa-h5");
		postData.put("source_id", 31L);
		postData.put("monitor_id", 41L);
		postData.put("inviter_id", 51L);

		tt.execute(
				status -> {
					memberAccountService.creatMemberForH5Post(postData, true);
					return null;
				});

		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(membersCreateMemberSuccessDispatchPublisher).publish(captor.capture());
		Map<String, Object> payload = captor.getValue();
		assertEquals(10L, payload.get("company_id"));
		assertEquals(902L, payload.get("user_id"));
		assertEquals("13800138002", payload.get("mobile"));
		assertEquals("openid-h5", payload.get("openid"));
		assertEquals("wxa-h5", payload.get("wxa_appid"));
		assertEquals(31L, payload.get("source_id"));
		assertEquals(41L, payload.get("monitor_id"));
		assertEquals(51L, payload.get("inviter_id"));
		assertEquals(0L, payload.get("distributor_id"));
		assertEquals(0L, payload.get("salesperson_id"));
		assertEquals(true, payload.get("if_register_promotion"));
	}
}
