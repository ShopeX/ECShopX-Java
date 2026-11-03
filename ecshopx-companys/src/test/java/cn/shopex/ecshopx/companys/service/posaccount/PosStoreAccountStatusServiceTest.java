package cn.shopex.ecshopx.companys.service.posaccount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.companys.domain.PosStoreAccount;
import cn.shopex.ecshopx.companys.mapper.DistributionDistributorSelfReadMapper;
import cn.shopex.ecshopx.companys.mapper.PosStoreAccountMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PosStoreAccountStatusServiceTest {

	@Mock
	private PosStoreAccountMapper mapper;

	@Mock
	private DistributionDistributorSelfReadMapper shopMapper;

	private PosStoreAccountStatusService service;

	@BeforeEach
	void setUp() {
		PosStoreAccountQueryService query = new PosStoreAccountQueryService(mapper, shopMapper);
		service = new PosStoreAccountStatusService(mapper, query);
	}

	@Test
	void resignMarksResigned() {
		when(mapper.selectById(9L)).thenReturn(enabled(9L, 88L));
		when(mapper.updateById(any(PosStoreAccount.class))).thenReturn(1);
		Map<String, Object> jwt = Map.of("company_id", 1L, "operator_type", "admin");
		Map<String, Object> out = service.change(jwt, 9L, "resign");
		assertEquals("resigned", out.get("status"));
	}

	@Test
	void cannotEnableResigned() {
		PosStoreAccount row = enabled(9L, 88L);
		row.setStatus("resigned");
		when(mapper.selectById(9L)).thenReturn(row);
		Map<String, Object> jwt = Map.of("company_id", 1L, "operator_type", "admin");
		assertThrows(BadRequestException.class, () -> service.change(jwt, 9L, "enable"));
		verify(mapper, never()).updateById(any(PosStoreAccount.class));
	}

	@Test
	void batchPartialSuccess() {
		when(mapper.selectById(1L)).thenReturn(enabled(1L, 88L));
		PosStoreAccount resigned = enabled(2L, 88L);
		resigned.setStatus("resigned");
		when(mapper.selectById(2L)).thenReturn(resigned);
		when(mapper.selectById(3L)).thenReturn(null);
		when(mapper.updateById(any(PosStoreAccount.class))).thenReturn(1);
		Map<String, Object> jwt = Map.of("company_id", 1L, "operator_type", "admin");
		Map<String, Object> out = service.batch(jwt, List.of(1L, 2L, 3L), "disable");
		assertEquals(List.of(1L), out.get("success"));
		assertEquals(List.of(2L), out.get("skipped"));
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> failed = (List<Map<String, Object>>) out.get("failed");
		assertEquals(1, failed.size());
		assertEquals(3L, failed.get(0).get("id"));
	}

	@Test
	void batchEmptyRejected() {
		Map<String, Object> jwt = Map.of("company_id", 1L, "operator_type", "admin");
		assertThrows(BadRequestException.class, () -> service.batch(jwt, List.of(), "enable"));
	}

	private static PosStoreAccount enabled(long id, long shop) {
		PosStoreAccount row = new PosStoreAccount();
		row.setId(id);
		row.setCompanyId(1L);
		row.setDistributorId(shop);
		row.setStatus("enabled");
		row.setEmployeeNo("A" + id);
		return row;
	}
}
