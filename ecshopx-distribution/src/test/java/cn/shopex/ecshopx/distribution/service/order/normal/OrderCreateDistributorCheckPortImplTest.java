package cn.shopex.ecshopx.distribution.service.order.normal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.distribution.domain.Distributor;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderCreateDistributorCheckPortImplTest {

	@Mock
	private DistributorMapper distributorMapper;

	@InjectMocks
	private OrderCreateDistributorCheckPortImpl port;

	@Test
	@DisplayName("可买 SQL：启用/禁用，不含闭店/撤店")
	void purchasableSql_allowsEnabledAndDisabled() throws Exception {
		Field f = OrderCreateDistributorCheckPortImpl.class.getDeclaredField("SQL_IS_VALID_PURCHASABLE");
		f.setAccessible(true);
		String sql = ((String) f.get(null)).toLowerCase();
		assertTrue(sql.contains("'true'"));
		assertTrue(sql.contains("'false'"));
		assertTrue(sql.contains("'0'"));
		assertTrue(sql.contains("'1'"));
		assertTrue(!sql.contains("closed") && !sql.contains("delete"));
	}

	@Test
	@DisplayName("查询命中门店时通过校验")
	void check_passesWhenShopFound() {
		Distributor d = new Distributor();
		d.setDistributorId(306L);
		d.setIsZiti(true);
		when(distributorMapper.selectOne(any())).thenReturn(d);
		assertDoesNotThrow(() -> port.check(params(38L, 306L, "ziti")));
	}

	@Test
	@DisplayName("店铺查不到（闭店/撤店等）抛店铺不存在或已失效")
	void check_throwsWhenShopMissingOrNotPurchasable() {
		when(distributorMapper.selectOne(any())).thenReturn(null);
		ResourceException ex = assertThrows(ResourceException.class, () -> port.check(params(38L, 306L, "ziti")));
		assertTrue(ex.getMessage().contains("店铺不存在或已失效"));
	}

	private static NormalOrderCreateParams params(long companyId, long distributorId, String receiptType) {
		NormalOrderCreateParams p = new NormalOrderCreateParams();
		Map<String, Object> pr = p.getParams();
		pr.put("company_id", companyId);
		pr.put("distributor_id", distributorId);
		pr.put("receipt_type", receiptType);
		return p;
	}
}
