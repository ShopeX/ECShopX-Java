package cn.shopex.ecshopx.companys.service.posaccount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.companys.domain.PosStoreAccount;
import cn.shopex.ecshopx.companys.mapper.DistributionDistributorSelfReadMapper;
import cn.shopex.ecshopx.companys.mapper.PosStoreAccountMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PosStoreAccountQueryServiceTest {

	@Mock
	private PosStoreAccountMapper mapper;

	@Mock
	private DistributionDistributorSelfReadMapper shopMapper;

	private PosStoreAccountQueryService service;

	@BeforeAll
	static void initTable() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), PosStoreAccount.class);
	}

	@BeforeEach
	void setUp() {
		service = new PosStoreAccountQueryService(mapper, shopMapper);
	}

	@Test
	void listOmitsPasswordAndKeepsAuthStatsWhenEmpty() {
		when(mapper.selectMaps(any())).thenReturn(List.of(
				stat("enabled", 6),
				stat("disabled", 2),
				stat("resigned", 1)));
		Page<PosStoreAccount> empty = new Page<>(1, 20);
		empty.setRecords(List.of());
		empty.setTotal(0);
		when(mapper.selectPage(any(Page.class), any())).thenReturn(empty);

		Map<String, Object> jwt = Map.of("company_id", 1L, "operator_type", "admin");
		Map<String, Object> out = service.list(jwt, "nobody", null, null, null, 1, 20);

		@SuppressWarnings("unchecked")
		Map<String, Object> stats = (Map<String, Object>) out.get("stats");
		assertEquals(9L, stats.get("total"));
		assertEquals(6L, stats.get("enabled"));
		assertEquals(2L, stats.get("disabled"));
		assertEquals(1L, stats.get("resigned"));
		assertEquals(List.of(), out.get("list"));
	}

	@Test
	void listRowHasShopFieldsWithoutCipher() {
		when(mapper.selectMaps(any())).thenReturn(List.of(stat("enabled", 1)));
		Page<PosStoreAccount> page = new Page<>(1, 20);
		page.setRecords(List.of(enabledAccount()));
		page.setTotal(1);
		when(mapper.selectPage(any(Page.class), any())).thenReturn(page);
		when(shopMapper.listDistributorNamesByCompanyAndIds(eq(1L), any()))
				.thenReturn(List.of(Map.of("distributor_id", 88L, "name", "旗舰店", "shop_code", "S88")));

		Map<String, Object> jwt = Map.of("company_id", 1L, "operator_type", "admin");
		Map<String, Object> out = service.list(jwt, null, null, null, null, 1, 20);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = (List<Map<String, Object>>) out.get("list");
		Map<String, Object> row = list.get(0);
		assertEquals("A01", row.get("employee_no"));
		assertEquals("S88", row.get("shop_code"));
		assertEquals("旗舰店", row.get("shop_name"));
		assertFalse(row.containsKey("password"));
		assertFalse(row.containsKey("password_cipher"));
	}

	@Test
	void mapOrderInfoByIdsReturnsPublicFieldsWithoutCipher() {
		when(mapper.selectList(any())).thenReturn(List.of(enabledAccount()));
		Map<Long, Map<String, Object>> out = service.mapOrderInfoByIds(1L, List.of(0L, 9L, 9L));
		assertEquals(1, out.size());
		Map<String, Object> info = out.get(9L);
		assertEquals(9L, info.get("id"));
		assertEquals("A01", info.get("employee_no"));
		assertEquals("张三", info.get("name"));
		assertEquals("clerk", info.get("role"));
		assertEquals(88L, info.get("distributor_id"));
		assertEquals("enabled", info.get("status"));
		assertFalse(info.containsKey("password_cipher"));
		assertEquals(info, PosStoreAccountQueryService.asInfoMap(info));
		assertTrue(PosStoreAccountQueryService.asInfoMap(null).isEmpty());
	}

	@Test
	void mapOrderInfoByIdsEmptyWhenNoPositiveIds() {
		assertTrue(service.mapOrderInfoByIds(1L, List.of(0L, -1L)).isEmpty());
	}

	@Test
	void detailRefusesOtherShop() {
		when(mapper.selectById(9L)).thenReturn(enabledAccount());
		Map<String, Object> jwt =
				Map.of("company_id", 1L, "operator_type", "staff", "distributor_ids", List.of(1L));
		assertThrows(ForbiddenException.class, () -> service.detail(jwt, 9L));
	}

	@Test
	void staffWithoutShopsIsForbidden() {
		Map<String, Object> jwt = Map.of("company_id", 1L, "operator_type", "staff");
		ForbiddenException ex =
				assertThrows(ForbiddenException.class, () -> service.list(jwt, null, null, null, null, 1, 20));
		assertTrue(ex.getMessage().contains("无访问权限"));
	}

	private static Map<String, Object> stat(String status, long cnt) {
		Map<String, Object> m = new HashMap<>();
		m.put("status", status);
		m.put("cnt", cnt);
		return m;
	}

	private static PosStoreAccount enabledAccount() {
		PosStoreAccount row = new PosStoreAccount();
		row.setId(9L);
		row.setCompanyId(1L);
		row.setEmployeeNo("A01");
		row.setName("张三");
		row.setEmail("");
		row.setMobile("");
		row.setRole("clerk");
		row.setDistributorId(88L);
		row.setStatus("enabled");
		row.setPasswordCipher("secret-cipher");
		row.setCreated(1);
		return row;
	}
}
