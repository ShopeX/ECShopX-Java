package cn.shopex.ecshopx.members.service.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.MembersCreateMemberSuccessDispatchPublisher;
import cn.shopex.ecshopx.companys.service.protocol.ShopProtocolSetService;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.mapper.MembersDeleteRecordMapper;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
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
class MemberAccountServiceRegisterShuyunMemberDispatchTest {

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
	private MembersCreateMemberSuccessDispatchPublisher membersCreateMemberSuccessDispatchPublisher;

	@Autowired
	private ShopProtocolSetService shopProtocolSetService;

	@Autowired
	private MembersDeleteRecordMapper membersDeleteRecordMapper;

	@BeforeEach
	void stubPersistenceAndPromotion() {
		reset(
				membersMapper,
				membersInfoMapper,
				membersAssociationsMapper,
				membersCreateMemberSuccessDispatchPublisher,
				shopProtocolSetService,
				membersDeleteRecordMapper);
		when(membersMapper.selectDefaultGradeId(anyString())).thenReturn(1L);
		when(membersMapper.insert(any(Members.class)))
				.thenAnswer(
						inv -> {
							Members mm = inv.getArgument(0);
							mm.setUserId(901L);
							return 1;
						});
		when(membersInfoMapper.insert(any(MembersInfo.class))).thenReturn(1);
		when(membersAssociationsMapper.insert(any(MembersAssociations.class))).thenReturn(1);
		when(shopProtocolSetService.get(anyLong(), anyString(), anyString())).thenReturn(Map.of());
		when(membersDeleteRecordMapper.selectOne(any())).thenReturn(null);
	}

	@Test
	@SuppressWarnings("unchecked")
	void registerShuyunMember_shouldPublishAfterCommitOnce() {
		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", 10L);
		params.put("mobile", "13800138000");
		params.put("user_type", "wechat");
		params.put("unionid", "union-openid-semantic");
		params.put("wxa_appid", "wxapp-id-value");
		params.put("source_id", 3L);
		params.put("monitor_id", 4L);
		params.put("inviter_id", 5L);
		tt.execute(
				status -> {
					memberAccountService.registerShuyunMember(params);
					return null;
				});

		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(membersCreateMemberSuccessDispatchPublisher, times(1)).publish(captor.capture());
		Map<String, Object> payload = captor.getValue();
		assertEquals(10L, payload.get("company_id"));
		assertEquals(901L, payload.get("user_id"));
		assertEquals("13800138000", payload.get("mobile"));
		assertEquals("union-openid-semantic", payload.get("openid"));
		assertEquals("wxapp-id-value", payload.get("wxa_appid"));
		assertEquals(3L, payload.get("source_id"));
		assertEquals(4L, payload.get("monitor_id"));
		assertEquals(5L, payload.get("inviter_id"));
	}

	@Test
	void registerShuyunMember_shouldNotPublishWhenTransactionRollsBack() {
		TransactionTemplate tt = new TransactionTemplate(transactionManager);
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", 11L);
		params.put("mobile", "13900139000");
		params.put("user_type", "wechat");
		tt.execute(
				status -> {
					status.setRollbackOnly();
					memberAccountService.registerShuyunMember(params);
					return null;
				});

		verify(membersCreateMemberSuccessDispatchPublisher, never()).publish(any());
	}
}
