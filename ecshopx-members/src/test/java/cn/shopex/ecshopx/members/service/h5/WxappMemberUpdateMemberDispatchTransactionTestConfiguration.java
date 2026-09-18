package cn.shopex.ecshopx.members.service.h5;

import static org.mockito.Mockito.mock;

import cn.shopex.ecshopx.companys.mapper.CompanysMapper;
import cn.shopex.ecshopx.members.integration.front.FrontMemberInfoUpdateValidationPort;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmSettingReadPort;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.DmCrmUpdateMemberInfoByMobilePort;
import cn.shopex.ecshopx.thirdparty.service.shuyun.ShuyunMemberProfileModifyPort;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@Import(WxappMemberUpdateMemberService.class)
class WxappMemberUpdateMemberDispatchTransactionTestConfiguration {

	@Bean
	DataSource wxappMemberUpdateMemberDispatchIntegrationDataSource() {
		return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
	}

	@Bean
	PlatformTransactionManager platformTransactionManager(
			DataSource wxappMemberUpdateMemberDispatchIntegrationDataSource) {
		return new DataSourceTransactionManager(wxappMemberUpdateMemberDispatchIntegrationDataSource);
	}

	@Bean
	FrontMemberInfoUpdateValidationPort frontMemberInfoUpdateValidationPort() {
		return mock(FrontMemberInfoUpdateValidationPort.class);
	}

	@Bean
	MemberAccountService memberAccountService() {
		return mock(MemberAccountService.class);
	}

	@Bean
	MembersInfoMapper membersInfoMapper() {
		return mock(MembersInfoMapper.class);
	}

	@Bean
	ShuyunMemberProfileModifyPort shuyunMemberProfileModifyPort() {
		return mock(ShuyunMemberProfileModifyPort.class);
	}

	@Bean
	DmCrmSettingReadPort dmCrmSettingReadPort() {
		return mock(DmCrmSettingReadPort.class);
	}

	@Bean
	DmCrmUpdateMemberInfoByMobilePort dmCrmUpdateMemberInfoByMobilePort() {
		return mock(DmCrmUpdateMemberInfoByMobilePort.class);
	}

	@Bean
	CompanysMapper companysMapper() {
		return mock(CompanysMapper.class);
	}
}
