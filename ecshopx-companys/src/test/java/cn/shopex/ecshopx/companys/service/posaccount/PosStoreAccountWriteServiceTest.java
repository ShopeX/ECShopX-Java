package cn.shopex.ecshopx.companys.service.posaccount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.dispatch.PosStoreAccountCreateEventDispatchPublisher;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.port.digios.PosStoreAccountDigiOsCreatePort;
import cn.shopex.ecshopx.companys.domain.PosStoreAccount;
import cn.shopex.ecshopx.companys.mapper.DistributionDistributorSelfReadMapper;
import cn.shopex.ecshopx.companys.mapper.PosStoreAccountMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class PosStoreAccountWriteServiceTest {

	private static final String APP_KEY = "base64:MTIzNDU2Nzg5MDEyMzQ1Ng==";

	@Mock
	private PosStoreAccountMapper mapper;

	@Mock
	private DistributionDistributorSelfReadMapper shopMapper;

	@Mock
	private PosStoreAccountCreateEventDispatchPublisher createEventDispatchPublisher;

	@Mock
	private PosStoreAccountDigiOsCreatePort digiOsCreatePort;

	private PosStoreAccountWriteService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), PosStoreAccount.class);
	}

	@BeforeEach
	void setUp() {
		SensitiveFieldEncryptor enc = new SensitiveFieldEncryptor(false, APP_KEY, "AES-128-CBC");
		PosStoreAccountPasswordService passwords =
				new PosStoreAccountPasswordService(mapper, enc, new PosStoreAccountPasswordGenerator());
		PosStoreAccountQueryService query = new PosStoreAccountQueryService(mapper, shopMapper);
		service =
				new PosStoreAccountWriteService(
						mapper, shopMapper, passwords, query, createEventDispatchPublisher, digiOsCreatePort);
	}

	@Test
	void createReturnsPasswordOnceAndStoresCipher() {
		when(shopMapper.selectShopByCompanyAndId(1L, 88L))
				.thenReturn(Map.of("distributor_id", 88L, "name", "旗舰店", "shop_code", "S88"));
		when(mapper.selectOne(any())).thenReturn(null);
		when(mapper.insert(any(PosStoreAccount.class)))
				.thenAnswer(
						inv -> {
							PosStoreAccount e = inv.getArgument(0);
							e.setId(11L);
							return 1;
						});
		Map<String, Object> jwt = Map.of("company_id", 1L, "operator_type", "admin");
		Map<String, Object> body =
				Map.of("employee_no", " A01 ", "role", "clerk", "distributor_id", 88L);
		Map<String, Object> out = service.create(jwt, body);
		String plain = String.valueOf(out.get("password"));
		assertEquals(8, plain.length());
		assertEquals("A01", out.get("employee_no"));
		ArgumentCaptor<PosStoreAccount> cap = ArgumentCaptor.forClass(PosStoreAccount.class);
		verify(mapper).insert(cap.capture());
		assertEquals("enabled", cap.getValue().getStatus());
		assertTrue(!plain.equals(cap.getValue().getPasswordCipher()));
		assertEquals(
				plain,
				new SensitiveFieldEncryptor(false, APP_KEY, "AES-128-CBC")
						.decryptAlways(cap.getValue().getPasswordCipher()));
	}

	@Test
	void createBlocksWhenDigiOsSyncFails() {
		when(shopMapper.selectShopByCompanyAndId(1L, 88L))
				.thenReturn(Map.of("distributor_id", 88L, "name", "旗舰店", "shop_code", "S88"));
		when(mapper.selectOne(any())).thenReturn(null);
		doThrow(new BadRequestException("DigiOS同步失败：acc:账户名称格式不正确;"))
				.when(digiOsCreatePort)
				.ensureSyncedOnCreate(eq(1L), eq("A01"), anyString(), anyString(), anyString());
		Map<String, Object> jwt = Map.of("company_id", 1L, "operator_type", "admin");
		Map<String, Object> body =
				Map.of(
						"employee_no",
						"A01",
						"role",
						"clerk",
						"distributor_id",
						88L,
						"mobile",
						"13900001111");
		BadRequestException ex = assertThrows(BadRequestException.class, () -> service.create(jwt, body));
		assertEquals("DigiOS同步失败：acc:账户名称格式不正确;", ex.getMessage());
		verify(mapper, never()).insert(any(PosStoreAccount.class));
		verify(createEventDispatchPublisher, never()).publish(any());
	}

	@Test
	void duplicateEmployeeNoIsFriendly() {
		when(shopMapper.selectShopByCompanyAndId(1L, 88L))
				.thenReturn(Map.of("distributor_id", 88L, "name", "旗舰店", "shop_code", "S88"));
		when(mapper.selectOne(any())).thenReturn(null);
		when(mapper.insert(any(PosStoreAccount.class))).thenThrow(new DuplicateKeyException("uk"));
		Map<String, Object> jwt = Map.of("company_id", 1L, "operator_type", "admin");
		Map<String, Object> body =
				Map.of("employee_no", "A01", "role", "clerk", "distributor_id", 88L);
		BadRequestException ex = assertThrows(BadRequestException.class, () -> service.create(jwt, body));
		assertEquals("员工号已存在，请更换员工号", ex.getMessage());
	}

	@Test
	void globalEmployeeNoClashAcrossCompanyIsRejected() {
		when(shopMapper.selectShopByCompanyAndId(1L, 88L))
				.thenReturn(Map.of("distributor_id", 88L, "name", "旗舰店", "shop_code", "S88"));
		PosStoreAccount otherCompany = new PosStoreAccount();
		otherCompany.setId(2L);
		otherCompany.setCompanyId(99L);
		otherCompany.setEmployeeNo("A01");
		when(mapper.selectOne(any())).thenReturn(otherCompany);
		Map<String, Object> jwt = Map.of("company_id", 1L, "operator_type", "admin");
		Map<String, Object> body =
				Map.of("employee_no", "A01", "role", "clerk", "distributor_id", 88L);
		BadRequestException ex = assertThrows(BadRequestException.class, () -> service.create(jwt, body));
		assertEquals("员工号已存在，请更换员工号", ex.getMessage());
		verify(mapper, never()).insert(any(PosStoreAccount.class));
	}

	@Test
	void invalidEmailRejectedBeforePassword() {
		when(shopMapper.selectShopByCompanyAndId(1L, 88L))
				.thenReturn(Map.of("distributor_id", 88L, "name", "旗舰店", "shop_code", "S88"));
		Map<String, Object> jwt = Map.of("company_id", 1L, "operator_type", "admin");
		Map<String, Object> body =
				Map.of(
						"employee_no",
						"A01",
						"role",
						"clerk",
						"distributor_id",
						88L,
						"email",
						"not-an-email");
		BadRequestException ex = assertThrows(BadRequestException.class, () -> service.create(jwt, body));
		assertEquals("邮箱格式不正确", ex.getMessage());
		verify(mapper, never()).insert(any(PosStoreAccount.class));
	}

	@Test
	void editKeepsEmployeeNoAndPassword() {
		PosStoreAccount existing = new PosStoreAccount();
		existing.setId(9L);
		existing.setCompanyId(1L);
		existing.setEmployeeNo("A01");
		existing.setPasswordCipher("keep");
		existing.setDistributorId(88L);
		existing.setStatus("enabled");
		existing.setRole("clerk");
		existing.setUpdated(10);
		when(mapper.selectById(9L)).thenReturn(existing);
		when(shopMapper.selectShopByCompanyAndId(1L, 88L))
				.thenReturn(Map.of("distributor_id", 88L, "name", "旗舰店", "shop_code", "S88"));
		when(mapper.updateById(any(PosStoreAccount.class))).thenReturn(1);
		Map<String, Object> jwt = Map.of("company_id", 1L, "operator_type", "admin");
		Map<String, Object> body =
				Map.of(
						"employee_no",
						"B02",
						"name",
						"李四",
						"role",
						"supervisor",
						"distributor_id",
						88L,
						"status",
						"disabled");
		Map<String, Object> out = service.update(jwt, 9L, body);
		assertEquals("A01", out.get("employee_no"));
		assertEquals("supervisor", out.get("role"));
		assertEquals("disabled", out.get("status"));
		assertTrue(!out.containsKey("password"));
		ArgumentCaptor<PosStoreAccount> cap = ArgumentCaptor.forClass(PosStoreAccount.class);
		verify(mapper).updateById(cap.capture());
		assertEquals("keep", cap.getValue().getPasswordCipher());
		assertEquals("A01", cap.getValue().getEmployeeNo());
	}

	@Test
	void editCannotUnresign() {
		PosStoreAccount existing = new PosStoreAccount();
		existing.setId(9L);
		existing.setCompanyId(1L);
		existing.setDistributorId(88L);
		existing.setStatus("resigned");
		existing.setEmployeeNo("A01");
		existing.setPasswordCipher("keep");
		when(mapper.selectById(9L)).thenReturn(existing);
		Map<String, Object> jwt = Map.of("company_id", 1L, "operator_type", "admin");
		assertThrows(
				BadRequestException.class,
				() -> service.update(jwt, 9L, Map.of("status", "enabled", "role", "clerk", "distributor_id", 88L)));
		verify(mapper, never()).updateById(any(PosStoreAccount.class));
	}

	@Test
	void changeRoleRejectsResigned() {
		PosStoreAccount existing = new PosStoreAccount();
		existing.setId(9L);
		existing.setCompanyId(1L);
		existing.setDistributorId(88L);
		existing.setStatus("resigned");
		when(mapper.selectById(9L)).thenReturn(existing);
		Map<String, Object> jwt = Map.of("company_id", 1L, "operator_type", "admin");
		assertThrows(BadRequestException.class, () -> service.changeRole(jwt, 9L, "clerk"));
	}
}
