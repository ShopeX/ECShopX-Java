package cn.shopex.ecshopx.members.service.h5.prelogin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.members.config.H5LocalProperties;
import cn.shopex.ecshopx.members.mapper.MembersWhitelistMapper;
import cn.shopex.ecshopx.members.dispatch.MemberRegisterJobDispatchPublisher;
import cn.shopex.ecshopx.members.service.account.MemberAccountServiceDispatchIntegrationTestConfiguration;
import cn.shopex.ecshopx.members.service.admin.AdminBindUserSalespersonRelService;
import cn.shopex.ecshopx.members.service.h5.auth.LocalH5AuthStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
	MemberAccountServiceDispatchIntegrationTestConfiguration.class,
	H5WxappLoginRegisterFacade.class,
	LocalH5AuthStrategy.class
})
class H5NewLoginPathsProbeTestConfiguration {

	@Bean
	MembersWhitelistMapper membersWhitelistMapper() {
		MembersWhitelistMapper mock = mock(MembersWhitelistMapper.class);
		when(mock.selectCount(any())).thenReturn(0L);
		return mock;
	}

	@Bean
	AdminBindUserSalespersonRelService adminBindUserSalespersonRelService() {
		return mock(AdminBindUserSalespersonRelService.class);
	}

	@Bean
	MemberRegisterJobDispatchPublisher memberRegisterJobDispatchPublisher() {
		return mock(MemberRegisterJobDispatchPublisher.class);
	}

	/**
	 * Non-OEM branch for {@link H5WxappLoginRegisterFacade} aliapp path ({@code createMemberLocalAutoRegister}), as in plan.
	 */
	@Bean
	H5LocalProperties h5LocalPropertiesProbe() {
		H5LocalProperties p = new H5LocalProperties();
		p.setOemShuyun(false);
		p.setSystemIsSaas(false);
		p.setSystemCompanysId("10");
		return p;
	}
}
