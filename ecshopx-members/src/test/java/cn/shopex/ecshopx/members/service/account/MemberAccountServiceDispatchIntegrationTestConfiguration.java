package cn.shopex.ecshopx.members.service.account;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.MembersCreateMemberSuccessDispatchPublisher;
import cn.shopex.ecshopx.common.members.admin.AdminMemberCreateDistributorUserSideEffectPort;
import cn.shopex.ecshopx.common.members.admin.AdminMemberCreatePromoterSideEffectPort;
import cn.shopex.ecshopx.companys.service.protocol.ShopProtocolSetService;
import cn.shopex.ecshopx.members.mapper.MembersAssociationsMapper;
import cn.shopex.ecshopx.members.mapper.MembersDeleteRecordMapper;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.mapper.ShopRelMemberMapper;
import cn.shopex.ecshopx.members.dispatch.BindSalsepersonJobDispatchPublisher;
import cn.shopex.ecshopx.members.mapper.WechatUsersMapper;
import cn.shopex.ecshopx.members.service.h5.bind.ShoppingGuideForH5BindLookup;
import cn.shopex.ecshopx.members.service.reg.MemberRegSettingService;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmSettingReadPort;
import cn.shopex.ecshopx.workwechat.mapper.WorkWechatRelMapper;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@Import(MemberAccountService.class)
public class MemberAccountServiceDispatchIntegrationTestConfiguration {

	@Bean
	DataSource memberAccountDispatchIntegrationDataSource() {
		return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
	}

	@Bean
	PlatformTransactionManager platformTransactionManager(DataSource memberAccountDispatchIntegrationDataSource) {
		return new DataSourceTransactionManager(memberAccountDispatchIntegrationDataSource);
	}

	@Bean
	MembersMapper membersMapper() {
		return mock(MembersMapper.class);
	}

	@Bean
	MembersInfoMapper membersInfoMapper() {
		return mock(MembersInfoMapper.class);
	}

	@Bean
	WechatUsersMapper wechatUsersMapper() {
		return mock(WechatUsersMapper.class);
	}

	@Bean
	MembersAssociationsMapper membersAssociationsMapper() {
		return mock(MembersAssociationsMapper.class);
	}

	@Bean
	MembersCreateMemberSuccessDispatchPublisher membersCreateMemberSuccessDispatchPublisher() {
		return mock(MembersCreateMemberSuccessDispatchPublisher.class);
	}

	@Bean
	BindSalsepersonJobDispatchPublisher bindSalsepersonJobDispatchPublisher() {
		return mock(BindSalsepersonJobDispatchPublisher.class);
	}

	@Bean
	@Qualifier("sharedStringRedisTemplate")
	StringRedisTemplate sharedStringRedisTemplate() {
		StringRedisTemplate template = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		SetOperations<String, String> setOps = mock(SetOperations.class);
		when(template.opsForSet()).thenReturn(setOps);
		when(setOps.add(anyString(), anyString())).thenReturn(1L);
		return template;
	}

	@Bean
	ShopProtocolSetService shopProtocolSetService() {
		return mock(ShopProtocolSetService.class);
	}

	@Bean
	MembersDeleteRecordMapper membersDeleteRecordMapper() {
		return mock(MembersDeleteRecordMapper.class);
	}

	@Bean
	AdminMemberCreatePromoterSideEffectPort adminMemberCreatePromoterSideEffectPort() {
		return mock(AdminMemberCreatePromoterSideEffectPort.class);
	}

	@Bean
	AdminMemberCreateDistributorUserSideEffectPort adminMemberCreateDistributorUserSideEffectPort() {
		return mock(AdminMemberCreateDistributorUserSideEffectPort.class);
	}

	@Bean
	ShoppingGuideForH5BindLookup shoppingGuideForH5BindLookup() {
		return mock(ShoppingGuideForH5BindLookup.class);
	}

	@Bean
	WorkWechatRelMapper workWechatRelMapper() {
		return mock(WorkWechatRelMapper.class);
	}

	@Bean
	ShopRelMemberMapper shopRelMemberMapper() {
		return mock(ShopRelMemberMapper.class);
	}

	@Bean
	DmCrmSettingReadPort dmCrmSettingReadPort() {
		return mock(DmCrmSettingReadPort.class);
	}

	@Bean
	MemberRegSettingService memberRegSettingService() {
		return mock(MemberRegSettingService.class);
	}
}
