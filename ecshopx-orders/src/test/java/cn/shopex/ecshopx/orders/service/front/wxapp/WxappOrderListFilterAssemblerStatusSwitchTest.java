package cn.shopex.ecshopx.orders.service.front.wxapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import cn.shopex.ecshopx.common.orders.port.AdminOrderDetailDistributionSupportPort;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WxappOrderListFilterAssemblerStatusSwitchTest {

	private final WxappOrderListFilterAssembler assembler =
			new WxappOrderListFilterAssembler(mock(AdminOrderDetailDistributionSupportPort.class));

	@Test
	@DisplayName("status=8：部分发货 → PAYED + PARTAIL")
	void status8_filtersPartialDelivery() {
		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("user_id", 1001L);
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("order_type", "normal");
		params.put("status", "8");

		Map<String, Object> f = assembler.built(141L, auth, params).filter();

		assertThat(f.get("order_status")).isEqualTo("PAYED");
		assertThat(f.get("delivery_status")).isEqualTo("PARTAIL");
		assertThat(f).doesNotContainKey("ziti_status");
	}

	@Test
	@DisplayName("status=1：待收货仍为 DONE，不受 status=8 影响")
	void status1_stillWaitBuyerConfirmDone() {
		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("user_id", 1001L);
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("order_type", "normal");
		params.put("status", "1");

		Map<String, Object> f = assembler.built(141L, auth, params).filter();

		assertThat(f.get("order_status")).isEqualTo("WAIT_BUYER_CONFIRM");
		assertThat(f.get("delivery_status")).isEqualTo("DONE");
		assertThat(f.get("ziti_status")).isEqualTo("NOTZITI");
	}
}
