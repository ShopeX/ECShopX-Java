package cn.shopex.ecshopx.members.service.admin;

import static org.mockito.Mockito.mock;

import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
@Import(AdminMemberMembersInfoUpdateService.class)
class AdminMemberMembersInfoUpdateDispatchIntegrationTestConfiguration {

	@Bean
	DataSource adminMemberMembersInfoUpdateDispatchIntegrationDataSource() {
		return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
	}

	@Bean
	PlatformTransactionManager platformTransactionManager(
			DataSource adminMemberMembersInfoUpdateDispatchIntegrationDataSource) {
		return new DataSourceTransactionManager(adminMemberMembersInfoUpdateDispatchIntegrationDataSource);
	}

	@Bean
	MembersInfoMapper membersInfoMapper() {
		return mock(MembersInfoMapper.class);
	}

	@Bean
	MemberAccountService memberAccountService() {
		return mock(MemberAccountService.class);
	}

	@Bean
	ObjectMapper objectMapper() {
		return new ObjectMapper();
	}
}
