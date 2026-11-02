package cn.shopex.ecshopx.companys.service.posauth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.UnauthorizedException;
import cn.shopex.ecshopx.companys.domain.PosStoreAccount;
import cn.shopex.ecshopx.companys.mapper.PosStoreAccountMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class PosJwtSessionValidatorTest {

	@Mock
	private PosStoreAccountMapper mapper;

	@Mock
	private MessageSource messageSource;

	private PosJwtSessionValidator validator;

	@BeforeEach
	void setUp() {
		lenient()
				.when(messageSource.getMessage(anyString(), any(), anyString(), any()))
				.thenAnswer(inv -> inv.getArgument(2));
		validator = new PosJwtSessionValidator(mapper, messageSource);
	}

	@Test
	void matchingSnapshotPasses() {
		PosStoreAccount row = row("enabled", 88L, "clerk");
		when(mapper.selectById(9L)).thenReturn(row);
		assertDoesNotThrow(
				() ->
						validator.assertSessionStillValid(
								Map.of(
										"pos_store_account_id",
										9L,
										"status",
										"enabled",
										"distributor_id",
										88L,
										"role",
										"clerk")));
	}

	@Test
	void roleChangeRejects() {
		when(mapper.selectById(9L)).thenReturn(row("enabled", 88L, "supervisor"));
		assertThrows(
				UnauthorizedException.class,
				() ->
						validator.assertSessionStillValid(
								Map.of(
										"id",
										9L,
										"status",
										"enabled",
										"distributor_id",
										88L,
										"role",
										"clerk")));
	}

	@Test
	void storeTransferRejects() {
		when(mapper.selectById(9L)).thenReturn(row("enabled", 99L, "clerk"));
		assertThrows(
				UnauthorizedException.class,
				() ->
						validator.assertSessionStillValid(
								Map.of(
										"id",
										9L,
										"status",
										"enabled",
										"distributor_id",
										88L,
										"role",
										"clerk")));
	}

	@Test
	void disabledRejects() {
		when(mapper.selectById(9L)).thenReturn(row("disabled", 88L, "clerk"));
		assertThrows(
				UnauthorizedException.class,
				() ->
						validator.assertSessionStillValid(
								Map.of(
										"id",
										9L,
										"status",
										"enabled",
										"distributor_id",
										88L,
										"role",
										"clerk")));
	}

	private static PosStoreAccount row(String status, long shopId, String role) {
		PosStoreAccount r = new PosStoreAccount();
		r.setId(9L);
		r.setStatus(status);
		r.setDistributorId(shopId);
		r.setRole(role);
		return r;
	}
}
