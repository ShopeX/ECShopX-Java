package cn.shopex.ecshopx.members.service.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentCaptor;

import cn.shopex.ecshopx.common.dispatch.MembersCreateMemberSuccessDispatchPublisher;
import cn.shopex.ecshopx.companys.service.protocol.ShopProtocolSetService;
import cn.shopex.ecshopx.members.dispatch.BindSalsepersonJobDispatchPublisher;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Asserts {@link MemberAccountService#createMemberForWxappBind} does not publish {@code BindSalseperson}
 * for a newly created member when the bind payload omits salesperson identifiers and uses the default
 * sales channel.
 * Also verifies the wxapp bind path does not call {@code BindSalsepersonJobDispatchPublisher#enqueueBindSalsepersonAfterDmMemberCreate}
 * or {@code BindSalsepersonJobDispatchPublisher#enqueueBindSalsepersonAfterWxappBindSalesperson}.
 */
@SpringJUnitConfig(MemberAccountServiceDispatchIntegrationTestConfiguration.class)
@TestPropertySource(properties = "ecshopx.h5.encrypt-sensitive-data=false")
class MemberAccountServiceCreateMemberForWxappBindNoBindSalespersonDispatchTest {

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
	private BindSalsepersonJobDispatchPublisher bindSalsepersonJobDispatchPublisher;

	@Autowired
	private ShopProtocolSetService shopProtocolSetService;

	@Autowired
	private MembersDeleteRecordMapper membersDeleteRecordMapper;

	@BeforeEach
	void stubPersistence() {
		reset(
				membersMapper,
				membersInfoMapper,
				membersAssociationsMapper,
				membersCreateMemberSuccessDispatchPublisher,
				bindSalsepersonJobDispatchPublisher,
				shopProtocolSetService,
				membersDeleteRecordMapper);
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
	void createMemberForWxappBind_defaultBindPayload_afterCommit_neverEnqueueBindSalseperson() {
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

		verify(bindSalsepersonJobDispatchPublisher, never())
				.enqueueBindSalseperson(anyLong(), anyString(), anyString(), anyInt(), anyString(), anyLong());
		verify(bindSalsepersonJobDispatchPublisher, never())
				.enqueueBindSalsepersonAfterDmMemberCreate(anyLong(), anyString(), anyString(), anyInt(), anyString());
		verify(bindSalsepersonJobDispatchPublisher, never())
				.enqueueBindSalsepersonAfterWxappBindSalesperson(
						anyLong(), anyString(), anyString(), anyInt(), anyString(), anyLong());
		verify(membersCreateMemberSuccessDispatchPublisher).publish(any());
	}

	@Test
	void createMemberForWxappBind_afterCommit_publishPayload_matchesBindPathKeysAndValues() {
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

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(membersCreateMemberSuccessDispatchPublisher).publish(captor.capture());
		Map<String, Object> payload = captor.getValue();

		assertEquals(10L, ((Number) payload.get("company_id")).longValue());
		assertEquals(902L, ((Number) payload.get("user_id")).longValue());
		assertEquals("13800138002", payload.get("mobile"));
		assertEquals("openid-wxapp", payload.get("openid"));
		assertEquals("wxa-bind", payload.get("wxa_appid"));
		assertEquals(51L, ((Number) payload.get("inviter_id")).longValue());
		assertEquals(0L, ((Number) payload.get("distributor_id")).longValue());
		assertEquals(31L, ((Number) payload.get("source_id")).longValue());
		assertEquals(41L, ((Number) payload.get("monitor_id")).longValue());
		assertEquals(0L, ((Number) payload.get("salesperson_id")).longValue());
		assertInstanceOf(Boolean.class, payload.get("if_register_promotion"));
		assertTrue((Boolean) payload.get("if_register_promotion"));

		verify(bindSalsepersonJobDispatchPublisher, never())
				.enqueueBindSalseperson(anyLong(), anyString(), anyString(), anyInt(), anyString(), anyLong());
		verify(bindSalsepersonJobDispatchPublisher, never())
				.enqueueBindSalsepersonAfterDmMemberCreate(anyLong(), anyString(), anyString(), anyInt(), anyString());
		verify(bindSalsepersonJobDispatchPublisher, never())
				.enqueueBindSalsepersonAfterWxappBindSalesperson(
						anyLong(), anyString(), anyString(), anyInt(), anyString(), anyLong());
	}
}
