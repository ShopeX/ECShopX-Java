package cn.shopex.ecshopx.members.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.MembersCreateMemberSuccessDispatchPublisher;
import cn.shopex.ecshopx.common.members.admin.AdminMemberCreatePostCommitDataPort;
import cn.shopex.ecshopx.companys.service.protocol.ShopProtocolSetService;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersDeleteRecordMapper;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
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

@SpringJUnitConfig(AdminMemberCreateMemberServiceDispatchIntegrationTestConfiguration.class)
@TestPropertySource(properties = "ecshopx.h5.encrypt-sensitive-data=false")
class AdminMemberCreateMemberServiceCreateMemberDispatchTest {

	@Autowired
	private AdminMemberCreateMemberService adminMemberCreateMemberService;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private MembersMapper membersMapper;

	@Autowired
	private MembersInfoMapper membersInfoMapper;

	@Autowired
	private MembersCreateMemberSuccessDispatchPublisher membersCreateMemberSuccessDispatchPublisher;

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

	@BeforeEach
	void stubPersistenceAndPorts() {
		reset(
				membersMapper,
				membersInfoMapper,
				membersCreateMemberSuccessDispatchPublisher,
				shopProtocolSetService,
				membersDeleteRecordMapper,
				memberAccountService,
				adminMemberCreatePostCommitDataPort,
				memberUserCardCodeAllocateService);
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
	@SuppressWarnings("unchecked")
	void createMember_shouldPublishCreateMemberSuccessAfterCommitWhenTransactionCommits() {
		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		Map<String, Object> postData = new LinkedHashMap<>();
		postData.put("mobile", "13800138000");
		tt.execute(
				status -> {
					adminMemberCreateMemberService.createMember(10L, postData);
					return null;
				});

		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(membersCreateMemberSuccessDispatchPublisher).publish(captor.capture());
		Map<String, Object> payload = captor.getValue();
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
	}

	@Test
	void createMember_shouldNotPublishWhenTransactionRollsBack() {
		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		Map<String, Object> postData = new LinkedHashMap<>();
		postData.put("mobile", "13900139000");
		tt.execute(
				status -> {
					status.setRollbackOnly();
					adminMemberCreateMemberService.createMember(11L, postData);
					return null;
				});

		verify(membersCreateMemberSuccessDispatchPublisher, never()).publish(any());
	}
}
