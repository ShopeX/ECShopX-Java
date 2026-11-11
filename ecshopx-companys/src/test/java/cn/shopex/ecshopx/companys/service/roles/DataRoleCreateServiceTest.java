package cn.shopex.ecshopx.companys.service.roles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.companys.domain.Roles;
import cn.shopex.ecshopx.companys.dto.CreateDataRoleRequest;
import cn.shopex.ecshopx.companys.mapper.RolesMapper;
import cn.shopex.ecshopx.companys.service.CommonLangModWriteService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataRoleCreateServiceTest {

	private static final long COMPANY_ID = 1001L;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Roles.class);
	}

	@Mock
	private RolesMapper rolesMapper;

	@Mock
	private CommonLangModWriteService commonLangModWriteService;

	private LangueProperties langueProperties;
	private DataRoleCreateService service;

	@BeforeEach
	void setUp() {
		langueProperties = new LangueProperties();
		service =
				new DataRoleCreateService(
						langueProperties, rolesMapper, new ObjectMapper(), commonLangModWriteService);
	}

	@Test
	@DisplayName("非默认语种创建时主表 role_name 写入空串占位")
	void createNonDefaultLangUsesEmptyMainRoleName() {
		when(rolesMapper.insert(any(Roles.class)))
				.thenAnswer(
						inv -> {
							Roles e = inv.getArgument(0);
							e.setRoleId(99L);
							return 1;
						});

		CreateDataRoleRequest body = new CreateDataRoleRequest();
		body.setRoleName("test1111");
		LinkedHashMap<String, Object> permission = new LinkedHashMap<>();
		permission.put("shopmenu_alias_name", List.of());
		body.setPermission(permission);
		body.setRoleSource("platform");

		Map<String, Object> jwt = Map.of("company_id", COMPANY_ID, "username", "op");

		service.create(body, "platform", "en-CN", jwt);

		ArgumentCaptor<Roles> captor = ArgumentCaptor.forClass(Roles.class);
		verify(rolesMapper).insert(captor.capture());
		assertThat(captor.getValue().getRoleName()).isEqualTo("");

		verify(commonLangModWriteService)
				.saveLang(
						eq((int) COMPANY_ID),
						eq(Map.of("role_name", "test1111")),
						eq("companys_roles"),
						eq(99),
						eq("companys_roles"),
						eq("en-CN"));
	}

	@Test
	@DisplayName("默认语种创建时主表写入角色名")
	void createDefaultLangWritesRoleNameToMainTable() {
		when(rolesMapper.insert(any(Roles.class)))
				.thenAnswer(
						inv -> {
							Roles e = inv.getArgument(0);
							e.setRoleId(1L);
							return 1;
						});

		CreateDataRoleRequest body = new CreateDataRoleRequest();
		body.setRoleName("默认角色");
		LinkedHashMap<String, Object> permission = new LinkedHashMap<>();
		permission.put("shopmenu_alias_name", List.of("home"));
		body.setPermission(permission);

		service.create(body, "platform", "zh-CN", Map.of("company_id", COMPANY_ID));

		ArgumentCaptor<Roles> captor = ArgumentCaptor.forClass(Roles.class);
		verify(rolesMapper).insert(captor.capture());
		assertThat(captor.getValue().getRoleName()).isEqualTo("默认角色");
	}
}
