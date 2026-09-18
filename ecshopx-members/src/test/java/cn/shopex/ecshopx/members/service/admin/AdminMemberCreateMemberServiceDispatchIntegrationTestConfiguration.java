package cn.shopex.ecshopx.members.service.admin;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.MembersCreateMemberSuccessDispatchPublisher;
import cn.shopex.ecshopx.common.members.admin.AdminMemberCreateDistributorUserSideEffectPort;
import cn.shopex.ecshopx.common.members.admin.AdminMemberCreatePostCommitDataPort;
import cn.shopex.ecshopx.common.members.admin.AdminMemberCreatePromoterSideEffectPort;
import cn.shopex.ecshopx.companys.service.protocol.ShopProtocolSetService;
import cn.shopex.ecshopx.members.mapper.MembersDeleteRecordMapper;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
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
@Import(AdminMemberCreateMemberService.class)
class AdminMemberCreateMemberServiceDispatchIntegrationTestConfiguration {

	@Bean
	DataSource adminMemberCreateMemberDispatchIntegrationDataSource() {
		return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
	}

	@Bean
	PlatformTransactionManager platformTransactionManager(
			DataSource adminMemberCreateMemberDispatchIntegrationDataSource) {
		return new DataSourceTransactionManager(adminMemberCreateMemberDispatchIntegrationDataSource);
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
	MembersDeleteRecordMapper membersDeleteRecordMapper() {
		return mock(MembersDeleteRecordMapper.class);
	}

	@Bean
	ShopProtocolSetService shopProtocolSetService() {
		return mock(ShopProtocolSetService.class);
	}

	@Bean
	MemberUserCardCodeAllocateService memberUserCardCodeAllocateService() {
		return mock(MemberUserCardCodeAllocateService.class);
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
	MembersCreateMemberSuccessDispatchPublisher membersCreateMemberSuccessDispatchPublisher() {
		return mock(MembersCreateMemberSuccessDispatchPublisher.class);
	}

	@Bean("adminMemberCreatePromoterSideEffectPortImpl")
	AdminMemberCreatePromoterSideEffectPort adminMemberCreatePromoterSideEffectPortImpl() {
		return mock(AdminMemberCreatePromoterSideEffectPort.class);
	}

	@Bean("adminMemberCreateDistributorUserSideEffectPortImpl")
	AdminMemberCreateDistributorUserSideEffectPort adminMemberCreateDistributorUserSideEffectPortImpl() {
		return mock(AdminMemberCreateDistributorUserSideEffectPort.class);
	}

	@Bean("adminMemberCreatePostCommitDataPortImpl")
	AdminMemberCreatePostCommitDataPort adminMemberCreatePostCommitDataPortImpl() {
		return mock(AdminMemberCreatePostCommitDataPort.class);
	}

	@Bean
	MemberAccountService memberAccountService() {
		return mock(MemberAccountService.class);
	}
}
