package cn.shopex.ecshopx.companys.service.posaccount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.companys.mapper.PosStoreAccountMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PosStoreAccountImportServiceTest {

	private static final String APP_KEY = "base64:MTIzNDU2Nzg5MDEyMzQ1Ng==";

	@Mock
	private PosStoreAccountMapper mapper;

	@Mock
	private DistributionDistributorSelfReadMapper shopMapper;

	@Mock
	private OperatorsMapper operatorsMapper;

	@Mock
	private PosStoreAccountCreateEventDispatchPublisher createEventDispatchPublisher;

	@Mock
	private PosStoreAccountDigiOsCreatePort digiOsCreatePort;

	private PosStoreAccountImportService service;

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
		service =
				new PosStoreAccountImportService(
						mapper,
						shopMapper,
						passwords,
						operatorsMapper,
						createEventDispatchPublisher,
						digiOsCreatePort);
	}

	@Test
	void newRowInsertsCipherWithoutReturningPassword() {
		when(shopMapper.selectShopByCompanyAndShopCode(1L, "S88"))
				.thenReturn(Map.of("distributor_id", 88L, "name", "旗舰店", "shop_code", "S88"));
		when(mapper.selectOne(any())).thenReturn(null);
		when(mapper.insert(any(PosStoreAccount.class))).thenReturn(1);
		service.acceptRow(1L, 9L, "admin", validRow("A01"));
		ArgumentCaptor<PosStoreAccount> cap = ArgumentCaptor.forClass(PosStoreAccount.class);
		verify(mapper).insert(cap.capture());
		assertEquals("clerk", cap.getValue().getRole());
		assertEquals("enabled", cap.getValue().getStatus());
		assertTrueCipher(cap.getValue().getPasswordCipher());
		verify(digiOsCreatePort)
				.ensureSyncedOnCreate(eq(1L), eq("A01"), eq("张三"), eq("13800138000"), eq("a@b.com"));
	}

	@Test
	void existingKeepsPassword() {
		when(shopMapper.selectShopByCompanyAndShopCode(1L, "S88"))
				.thenReturn(Map.of("distributor_id", 88L, "name", "旗舰店", "shop_code", "S88"));
		PosStoreAccount existing = new PosStoreAccount();
		existing.setId(3L);
		existing.setCompanyId(1L);
		existing.setEmployeeNo("A01");
		existing.setPasswordCipher("keep");
		existing.setStatus("resigned");
		when(mapper.selectOne(any())).thenReturn(existing);
		when(mapper.updateById(any(PosStoreAccount.class))).thenReturn(1);
		service.acceptRow(1L, 9L, "admin", validRow("A01"));
		ArgumentCaptor<PosStoreAccount> cap = ArgumentCaptor.forClass(PosStoreAccount.class);
		verify(mapper).updateById(cap.capture());
		verify(mapper, never()).insert(any(PosStoreAccount.class));
		assertEquals("keep", cap.getValue().getPasswordCipher());
		assertEquals("enabled", cap.getValue().getStatus());
		verify(digiOsCreatePort)
				.ensureSyncedOnCreate(eq(1L), eq("A01"), eq("张三"), eq("13800138000"), eq("a@b.com"));
	}

	@Test
	void digiOsFailOnNew_blocksInsertAndReleasesKey() {
		when(shopMapper.selectShopByCompanyAndShopCode(1L, "S88"))
				.thenReturn(Map.of("distributor_id", 88L, "name", "旗舰店", "shop_code", "S88"));
		doThrow(new BadRequestException("DigiOS同步失败：acc:账户名称格式不正确;"))
				.when(digiOsCreatePort)
				.ensureSyncedOnCreate(eq(1L), eq("A01"), anyString(), anyString(), anyString());
		BadRequestException ex =
				assertThrows(
						BadRequestException.class,
						() -> service.acceptRow(1L, 9L, "admin", validRow("A01")));
		assertEquals("DigiOS同步失败：acc:账户名称格式不正确;", ex.getMessage());
		verify(mapper, never()).insert(any(PosStoreAccount.class));
		verify(mapper, never()).updateById(any(PosStoreAccount.class));
		// key released: same employee can be retried after DigiOS recovers
		doThrow(new BadRequestException("DigiOS同步失败：系统异常，请联系管理员。"))
				.when(digiOsCreatePort)
				.ensureSyncedOnCreate(eq(1L), eq("A01"), anyString(), anyString(), anyString());
		BadRequestException ex2 =
				assertThrows(
						BadRequestException.class,
						() -> service.acceptRow(1L, 9L, "admin", validRow("A01")));
		assertEquals("DigiOS同步失败：系统异常，请联系管理员。", ex2.getMessage());
	}

	@Test
	void digiOsFailOnUpdate_blocksUpdate() {
		when(shopMapper.selectShopByCompanyAndShopCode(1L, "S88"))
				.thenReturn(Map.of("distributor_id", 88L, "name", "旗舰店", "shop_code", "S88"));
		doThrow(new BadRequestException("DigiOS同步失败：系统异常，请联系管理员。"))
				.when(digiOsCreatePort)
				.ensureSyncedOnCreate(eq(1L), eq("A01"), anyString(), anyString(), anyString());
		assertThrows(BadRequestException.class, () -> service.acceptRow(1L, 9L, "admin", validRow("A01")));
		verify(mapper, never()).selectOne(any());
		verify(mapper, never()).updateById(any(PosStoreAccount.class));
	}

	@Test
	void duplicateInFileSkipped() {
		when(shopMapper.selectShopByCompanyAndShopCode(1L, "S88"))
				.thenReturn(Map.of("distributor_id", 88L, "name", "旗舰店", "shop_code", "S88"));
		when(mapper.selectOne(any())).thenReturn(null);
		when(mapper.insert(any(PosStoreAccount.class))).thenReturn(1);
		service.acceptRow(1L, 9L, "admin", validRow("A01"));
		assertThrows(BadRequestException.class, () -> service.acceptRow(1L, 9L, "admin", validRow("a01")));
	}

	private static Map<String, Object> validRow(String employeeNo) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("name", "张三");
		row.put("employee_no", employeeNo);
		row.put("email", "a@b.com");
		row.put("mobile", "13800138000");
		row.put("role", "店员");
		row.put("shop_code", "S88");
		row.put("status", "启用");
		return row;
	}

	private static void assertTrueCipher(String cipher) {
		if (cipher == null || cipher.isBlank() || cipher.length() < 8) {
			throw new AssertionError("cipher missing");
		}
	}
}
