package cn.shopex.ecshopx.members.service.h5.prelogin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.MembersCreateMemberSuccessDispatchPublisher;
import cn.shopex.ecshopx.companys.service.protocol.ShopProtocolSetService;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersAssociations;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.domain.WechatUsers;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.mapper.MembersDeleteRecordMapper;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.mapper.WechatUsersMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.members.service.h5.auth.LocalH5AuthStrategy;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmSettingReadPort;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SuppressWarnings("unchecked")
@SpringJUnitConfig(H5NewLoginPathsProbeTestConfiguration.class)
@TestPropertySource(
		properties = {
			"ecshopx.h5.encrypt-sensitive-data=false",
		})
class H5NewLoginPathsCreateMemberSuccessDispatchProbeTest {

	@BeforeAll
	static void initMembersEntityMetadataForMybatisPlusLambdas() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Members.class);
	}

	@Autowired
	private H5WxappLoginRegisterFacade h5WxappLoginRegisterFacade;

	@Autowired
	private LocalH5AuthStrategy localH5AuthStrategy;

	@Autowired
	private MemberAccountService memberAccountService;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private MembersCreateMemberSuccessDispatchPublisher membersCreateMemberSuccessDispatchPublisher;

	@Autowired
	private MembersMapper membersMapper;

	@Autowired
	private MembersInfoMapper membersInfoMapper;

	@Autowired
	private WechatUsersMapper wechatUsersMapper;

	@Autowired
	private MembersAssociationsMapper membersAssociationsMapper;

	@Autowired
	private ShopProtocolSetService shopProtocolSetService;

	@Autowired
	private MembersDeleteRecordMapper membersDeleteRecordMapper;

	@Autowired
	private DmCrmSettingReadPort dmCrmSettingReadPort;

	@BeforeEach
	void stubPersistence() {
		reset(
				membersMapper,
				membersInfoMapper,
				wechatUsersMapper,
				membersAssociationsMapper,
				membersCreateMemberSuccessDispatchPublisher,
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
		when(wechatUsersMapper.selectOne(any())).thenReturn(null);
		when(wechatUsersMapper.insert(any(WechatUsers.class))).thenReturn(1);
		when(membersAssociationsMapper.selectOne(any())).thenReturn(null);
		when(membersAssociationsMapper.insert(any(MembersAssociations.class))).thenReturn(1);
		when(membersMapper.update(any(), any())).thenReturn(1);
	}

	@Test
	void registerMemberForWxapp_whenInsertsNewMember_publishCreateMemberSuccessAfterCommit() {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", 10L);
		params.put("appid", "wx-probe-appid");

		Map<String, Object> wxSession = new LinkedHashMap<>();
		wxSession.put("purePhoneNumber", "13800138000");
		wxSession.put("openid", "wx-open-probe");
		wxSession.put("unionid", "wx-union-probe");

		h5WxappLoginRegisterFacade.registerMemberForWxapp(params, wxSession);

		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(membersCreateMemberSuccessDispatchPublisher).publish(captor.capture());
		Map<String, Object> payload = captor.getValue();
		assertEquals(10L, payload.get("company_id"));
		assertEquals(901L, payload.get("user_id"));
		assertEquals("13800138000", payload.get("mobile"));
		assertEquals("wx-open-probe", payload.get("openid"));
		assertEquals("wx-probe-appid", payload.get("wxa_appid"));
	}

	@Test
	void registerMemberForAliapp_whenInsertsNewMember_publishCreateMemberSuccessAfterCommit() {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", 10L);
		params.put("alipay_appid", "ali-probe-appid");
		params.put("appid", "ali-probe-appid");

		Map<String, Object> aliSession = new LinkedHashMap<>();
		aliSession.put("purePhoneNumber", "13800138001");
		aliSession.put("alipay_user_id", "2088123456789012");

		h5WxappLoginRegisterFacade.registerMemberForAliapp(params, aliSession);

		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(membersCreateMemberSuccessDispatchPublisher).publish(captor.capture());
		Map<String, Object> payload = captor.getValue();
		assertEquals(10L, payload.get("company_id"));
		assertEquals(901L, payload.get("user_id"));
		assertEquals("13800138001", payload.get("mobile"));
	}

	@Test
	void localAuthStrategy_whenAutoRegisterCreatesMember_publishCreateMemberSuccessAfterCommit() {
		Map<String, Object> credentials = new LinkedHashMap<>();
		credentials.put("check_type", "password");
		credentials.put("username", "13800138002");
		credentials.put("password", "probe-secret-plain");
		credentials.put("auto_register", true);

		localH5AuthStrategy.resolve(credentials);

		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(membersCreateMemberSuccessDispatchPublisher).publish(captor.capture());
		Map<String, Object> payload = captor.getValue();
		assertEquals(10L, payload.get("company_id"));
		assertEquals(901L, payload.get("user_id"));
		assertEquals("13800138002", payload.get("mobile"));
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
	}
}
