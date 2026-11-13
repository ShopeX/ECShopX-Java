package cn.shopex.ecshopx.orders.service.normal.create;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OrderCreatePersistencePortImplIsOnlineOrderTest {

	@Test
	void resolve_shopadminOrder_isOffline() {
		Map<String, Object> od = new LinkedHashMap<>();
		od.put("order_class", "shopadmin");
		assertThat(OrderCreatePersistencePortImpl.resolveIsOnlineOrderForPersist(od)).isFalse();
	}

	@Test
	void resolve_explicitOfflineFlag_isOffline() {
		Map<String, Object> od = Map.of("order_class", "normal", "is_online_order", Boolean.FALSE);
		assertThat(OrderCreatePersistencePortImpl.resolveIsOnlineOrderForPersist(od)).isFalse();
	}

	@Test
	void resolve_wxappNormal_defaultsOnline() {
		Map<String, Object> od = Map.of("order_class", "normal", "is_online_order", Boolean.TRUE);
		assertThat(OrderCreatePersistencePortImpl.resolveIsOnlineOrderForPersist(od)).isTrue();
	}

	@Test
	void resolve_dianwuSource_isOfflineEvenWhenClassMissing() {
		Map<String, Object> od = Map.of("order_class", "normal", "source_from", "dianwu");
		assertThat(OrderCreatePersistencePortImpl.resolveIsOnlineOrderForPersist(od)).isFalse();
	}
}
