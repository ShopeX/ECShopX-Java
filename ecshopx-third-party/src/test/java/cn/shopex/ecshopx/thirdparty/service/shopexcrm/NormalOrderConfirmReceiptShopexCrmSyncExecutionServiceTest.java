package cn.shopex.ecshopx.thirdparty.service.shopexcrm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class NormalOrderConfirmReceiptShopexCrmSyncExecutionServiceTest {

	private static final String SQL_PAY_STATUS =
			"SELECT pay_status FROM orders_normal_orders WHERE company_id = ? AND order_id = ? LIMIT 1";

	@Mock
	private ShopexCrmSyncSingleOrderPort port;

	@Mock
	private JdbcTemplate jdbcTemplate;

	private NormalOrderConfirmReceiptShopexCrmSyncExecutionService service;

	@BeforeEach
	void setUp() {
		service = new NormalOrderConfirmReceiptShopexCrmSyncExecutionService(port, jdbcTemplate, "on");
	}

	@Test
	void whenCrmSyncBlank_skipsPort() {
		NormalOrderConfirmReceiptShopexCrmSyncExecutionService blank =
				new NormalOrderConfirmReceiptShopexCrmSyncExecutionService(port, jdbcTemplate, "");
		blank.executeAfterNormalOrderConfirmReceipt(Map.of("company_id", 1L, "order_id", 2L));
		verify(port, never()).syncSingleOrder(anyLong(), any());
	}

	@Test
	void whenPayStatusNotPayed_skipsPort() {
		when(jdbcTemplate.queryForObject(eq(SQL_PAY_STATUS), eq(String.class), eq(10L), eq(20L)))
				.thenReturn("UNPAYED");
		service.executeAfterNormalOrderConfirmReceipt(Map.of("company_id", 10L, "order_id", 20L));
		verify(port, never()).syncSingleOrder(anyLong(), any());
	}

	@Test
	void whenNoOrderRow_skipsPort() {
		when(jdbcTemplate.queryForObject(eq(SQL_PAY_STATUS), eq(String.class), eq(10L), eq(20L)))
				.thenThrow(new EmptyResultDataAccessException(1));
		service.executeAfterNormalOrderConfirmReceipt(Map.of("company_id", 10L, "order_id", 20L));
		verify(port, never()).syncSingleOrder(anyLong(), any());
	}

	@Test
	void whenPayed_invokesSyncSingleOrder() {
		when(jdbcTemplate.queryForObject(eq(SQL_PAY_STATUS), eq(String.class), eq(10L), eq(20L)))
				.thenReturn("PAYED");
		service.executeAfterNormalOrderConfirmReceipt(Map.of("company_id", 10L, "order_id", 20L));
		verify(port).syncSingleOrder(10L, 20L);
	}
}
