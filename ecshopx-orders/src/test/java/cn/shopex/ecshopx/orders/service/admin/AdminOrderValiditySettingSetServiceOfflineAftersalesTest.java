package cn.shopex.ecshopx.orders.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.RefundFreightAutoZyEventDispatchPublisher;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.service.setting.OrdersErpSettingRedisReadService;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisWriteService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminOrderValiditySettingSetServiceOfflineAftersalesTest {

	@Mock
	private OrderValiditySettingRedisWriteService orderValiditySettingRedisWriteService;

	@Mock
	private OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private OrdersErpSettingRedisReadService ordersErpSettingRedisReadService;

	@Mock
	private RefundFreightAutoZyEventDispatchPublisher refundFreightAutoZyEventDispatchPublisher;

	@Captor
	private ArgumentCaptor<Map<String, Object>> storedCaptor;

	private AdminOrderValiditySettingSetService service;

	@BeforeEach
	void setUp() {
		service =
				new AdminOrderValiditySettingSetService(
						orderValiditySettingRedisWriteService,
						orderValiditySettingRedisReadService,
						normalOrdersMapper,
						ordersErpSettingRedisReadService,
						refundFreightAutoZyEventDispatchPublisher);
	}

	@Test
	@DisplayName("offline_aftersales=true：与 PHP 一致写入 true")
	void setOrderSetting_offlineAftersalesBooleanTrue_storesTrue() {
		stubReadBack();

		Map<String, Object> input = baseInput();
		input.put("offline_aftersales", Boolean.TRUE);

		service.setOrderSetting(38L, input);

		assertStoredFlag("offline_aftersales", Boolean.TRUE);
	}

	@Test
	@DisplayName("offline_aftersales=\"true\"：写入 true")
	void setOrderSetting_offlineAftersalesStringTrue_storesTrue() {
		stubReadBack();

		Map<String, Object> input = baseInput();
		input.put("offline_aftersales", "true");

		service.setOrderSetting(38L, input);

		assertStoredFlag("offline_aftersales", Boolean.TRUE);
	}

	@Test
	@DisplayName("offline_aftersales=1：PHP 严格模式不开启")
	void setOrderSetting_offlineAftersalesNumericOne_storesFalse() {
		stubReadBack();

		Map<String, Object> input = baseInput();
		input.put("offline_aftersales", 1);

		service.setOrderSetting(38L, input);

		assertStoredFlag("offline_aftersales", Boolean.FALSE);
	}

	@Test
	@DisplayName("auto_aftersales=true：写入 true")
	void setOrderSetting_autoAftersalesBooleanTrue_storesTrue() {
		stubReadBack();

		Map<String, Object> input = baseInput();
		input.put("auto_aftersales", Boolean.TRUE);

		service.setOrderSetting(38L, input);

		assertStoredFlag("auto_aftersales", Boolean.TRUE);
	}

	@Test
	@DisplayName("auto_aftersales=1：写入 true")
	void setOrderSetting_autoAftersalesNumericOne_storesTrue() {
		stubReadBack();

		Map<String, Object> input = baseInput();
		input.put("auto_aftersales", 1);

		service.setOrderSetting(38L, input);

		assertStoredFlag("auto_aftersales", Boolean.TRUE);
	}

	private void stubReadBack() {
		when(orderValiditySettingRedisReadService.readPlatformSetting(38L)).thenReturn(Map.of());
	}

	private void assertStoredFlag(String key, Boolean expected) {
		verify(orderValiditySettingRedisWriteService).writeCompanySettingJson(eq(38L), storedCaptor.capture());
		assertThat(storedCaptor.getValue().get(key)).isEqualTo(expected);
	}

	private static Map<String, Object> baseInput() {
		Map<String, Object> input = new LinkedHashMap<>();
		input.put("order_cancel_time", 30);
		input.put("order_finish_time", "7");
		input.put("latest_aftersale_time", 0);
		input.put("auto_refuse_time", 1);
		input.put("auto_aftersales", 0);
		input.put("offline_aftersales", false);
		input.put("is_refund_freight", 0);
		return input;
	}
}
