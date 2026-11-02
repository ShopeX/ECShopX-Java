package cn.shopex.ecshopx.companys.service.posaccount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.crypto.SensitiveFieldEncryptor;
import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.companys.domain.PosStoreAccount;
import cn.shopex.ecshopx.companys.mapper.PosStoreAccountMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PosStoreAccountPasswordServiceTest {

	private static final String APP_KEY = "base64:MTIzNDU2Nzg5MDEyMzQ1Ng==";

	@Mock
	private PosStoreAccountMapper mapper;

	private PosStoreAccountPasswordService service;

	@BeforeEach
	void setUp() {
		SensitiveFieldEncryptor enc = new SensitiveFieldEncryptor(false, APP_KEY, "AES-128-CBC");
		service =
				new PosStoreAccountPasswordService(
						mapper, enc, new PosStoreAccountPasswordGenerator());
	}

	@Test
	void resetWritesCipherAndReturnsPlainOnce() {
		PosStoreAccount row = enabledAccount();
		when(mapper.selectById(9L)).thenReturn(row);
		when(mapper.updateById(any(PosStoreAccount.class))).thenReturn(1);

		Map<String, Object> jwt =
				Map.of(
						"company_id",
						1L,
						"operator_type",
						"staff",
						"distributor_ids",
						List.of(88L));
		Map<String, Object> out = service.reset(jwt, 9L);

		String plain = String.valueOf(out.get("password"));
		assertEquals(8, plain.length());
		assertTrue(plain.chars().anyMatch(Character::isLetter));
		assertTrue(plain.chars().anyMatch(Character::isDigit));

		ArgumentCaptor<PosStoreAccount> cap = ArgumentCaptor.forClass(PosStoreAccount.class);
		verify(mapper).updateById(cap.capture());
		assertEquals(plain, new SensitiveFieldEncryptor(false, APP_KEY, "AES-128-CBC")
				.decryptAlways(cap.getValue().getPasswordCipher()));
		assertTrue(!plain.equals(cap.getValue().getPasswordCipher()));
	}

	@Test
	void resetRefusesResigned() {
		PosStoreAccount row = enabledAccount();
		row.setStatus("resigned");
		when(mapper.selectById(9L)).thenReturn(row);
		Map<String, Object> jwt =
				Map.of("company_id", 1L, "operator_type", "admin", "distributor_ids", List.of());
		assertThrows(BadRequestException.class, () -> service.reset(jwt, 9L));
		verify(mapper, never()).updateById(any(PosStoreAccount.class));
	}

	@Test
	void resetRefusesOutOfShopScope() {
		when(mapper.selectById(9L)).thenReturn(enabledAccount());
		Map<String, Object> jwt =
				Map.of(
						"company_id",
						1L,
						"operator_type",
						"staff",
						"distributor_ids",
						List.of(1L));
		assertThrows(ForbiddenException.class, () -> service.reset(jwt, 9L));
		verify(mapper, never()).updateById(any(PosStoreAccount.class));
	}

	@Test
	void encryptFailureDoesNotOverwrite() {
		SensitiveFieldEncryptor broken = new SensitiveFieldEncryptor(false, "", "AES-128-CBC");
		service =
				new PosStoreAccountPasswordService(
						mapper, broken, new PosStoreAccountPasswordGenerator());
		when(mapper.selectById(9L)).thenReturn(enabledAccount());
		Map<String, Object> jwt =
				Map.of("company_id", 1L, "operator_type", "admin");
		BadRequestException ex = assertThrows(BadRequestException.class, () -> service.reset(jwt, 9L));
		assertEquals("密码生成失败请重试", ex.getMessage());
		verify(mapper, never()).updateById(any(PosStoreAccount.class));
	}

	private static PosStoreAccount enabledAccount() {
		PosStoreAccount row = new PosStoreAccount();
		row.setId(9L);
		row.setCompanyId(1L);
		row.setDistributorId(88L);
		row.setStatus("enabled");
		row.setPasswordCipher("old-cipher");
		row.setEmployeeNo("A01");
		return row;
	}
}
