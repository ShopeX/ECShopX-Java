package cn.shopex.ecshopx.companys.service.posauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import cn.shopex.ecshopx.companys.domain.PosStoreAccount;
import cn.shopex.ecshopx.companys.mapper.DistributionDistributorSelfReadMapper;
import cn.shopex.ecshopx.companys.mapper.PosStoreAccountMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class PosMeServiceTest {

	@Mock
	private PosStoreAccountMapper mapper;

	@Mock
	private DistributionDistributorSelfReadMapper shopMapper;

	@Mock
	private MessageSource messageSource;

	private PosMeService service;

	@BeforeEach
	void setUp() {
		lenient()
				.when(messageSource.getMessage(anyString(), any(), anyString(), any()))
				.thenAnswer(inv -> inv.getArgument(2));
		service = new PosMeService(mapper, shopMapper, messageSource);
	}

	@Test
	void meReturnsPermissionsAndShop() {
		PosStoreAccount row = new PosStoreAccount();
		row.setId(9L);
		row.setEmployeeNo("A01");
		row.setName("张三");
		row.setMobile("13800000000");
		row.setEmail("a@b.c");
		row.setRole("clerk");
		row.setCompanyId(1L);
		row.setDistributorId(88L);
		row.setStatus("enabled");
		when(mapper.selectById(9L)).thenReturn(row);
		when(shopMapper.selectShopByCompanyAndId(1L, 88L))
				.thenReturn(Map.of("distributor_id", 88L, "name", "旗舰店", "shop_code", "S88"));
		Map<String, Object> out = service.me(Map.of("id", 9L));
		assertEquals("A01", out.get("employee_no"));
		assertEquals("店员", out.get("role_name"));
		assertEquals("S88", out.get("shop_code"));
		assertEquals("旗舰店", out.get("shop_name"));
		@SuppressWarnings("unchecked")
		Map<String, Boolean> permissions = (Map<String, Boolean>) out.get("permissions");
		assertTrue(permissions.get("cashier"));
		assertFalse(permissions.get("refund"));
		@SuppressWarnings("unchecked")
		Map<String, String> doc = (Map<String, String>) out.get("doc_policy");
		assertEquals("own_only", doc.get("operate"));
	}
}
