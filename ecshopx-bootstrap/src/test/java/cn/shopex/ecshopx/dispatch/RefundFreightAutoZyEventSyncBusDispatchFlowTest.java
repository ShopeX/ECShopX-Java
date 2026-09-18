package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.common.dispatch.DistributionDispatchEventNames;
import cn.shopex.ecshopx.orders.dispatch.RefundFreightAutoZyDispatchListener;
import cn.shopex.ecshopx.orders.mapper.DistributionDistributorRefundFreightBulkMapper;
import cn.shopex.ecshopx.orders.service.admin.RefundFreightAutoZyDispatchExecutionService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RefundFreightAutoZyEventSyncBusDispatchFlowTest {

	@Test
	void publishRefundFreightAutoZy_sync_realListener_updatesMapperWhenIsRefundFreightOne() {
		DistributionDistributorRefundFreightBulkMapper mapper = mock(DistributionDistributorRefundFreightBulkMapper.class);
		RefundFreightAutoZyDispatchExecutionService execution =
				new RefundFreightAutoZyDispatchExecutionService(mapper);
		RefundFreightAutoZyDispatchListener listener = new RefundFreightAutoZyDispatchListener(execution);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		registry.registerEventListener(
				DistributionDispatchEventNames.EVENT_REFUND_FREIGHT_AUTO_ZY,
				DistributionDispatchEventNames.LISTENER_REFUND_FREIGHT_AUTO_ZY,
				ListenerDispatchOptions.syncDefaults(),
				listener);

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("company_id", 55L);
		entities.put("is_refund_freight", 1);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		assertDoesNotThrow(
				() -> facade.publishEvent(
						DistributionDispatchEventNames.EVENT_REFUND_FREIGHT_AUTO_ZY,
						payload,
						DispatchOptions.eventDefaults()));

		verify(mapper).updateIsRefundFreightByCompanyAndDistributionType(55L, 0, 1);
		assertTrue(captured.isEmpty());
	}

	@Test
	void publishRefundFreightAutoZy_sync_realListener_swallowsMapperFailure() {
		DistributionDistributorRefundFreightBulkMapper mapper = mock(DistributionDistributorRefundFreightBulkMapper.class);
		doThrow(new RuntimeException("db down"))
				.when(mapper)
				.updateIsRefundFreightByCompanyAndDistributionType(55L, 0, 1);
		RefundFreightAutoZyDispatchExecutionService execution =
				new RefundFreightAutoZyDispatchExecutionService(mapper);
		RefundFreightAutoZyDispatchListener listener = new RefundFreightAutoZyDispatchListener(execution);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		registry.registerEventListener(
				DistributionDispatchEventNames.EVENT_REFUND_FREIGHT_AUTO_ZY,
				DistributionDispatchEventNames.LISTENER_REFUND_FREIGHT_AUTO_ZY,
				ListenerDispatchOptions.syncDefaults(),
				listener);

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("company_id", 55L);
		entities.put("is_refund_freight", 1);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		assertDoesNotThrow(
				() -> facade.publishEvent(
						DistributionDispatchEventNames.EVENT_REFUND_FREIGHT_AUTO_ZY,
						payload,
						DispatchOptions.eventDefaults()));

		verify(mapper).updateIsRefundFreightByCompanyAndDistributionType(55L, 0, 1);
		assertTrue(captured.isEmpty());
	}

	@Test
	void publishRefundFreightAutoZy_sync_realListener_skipsMapperWhenNotRefundFreightOne() {
		DistributionDistributorRefundFreightBulkMapper mapper = mock(DistributionDistributorRefundFreightBulkMapper.class);
		RefundFreightAutoZyDispatchExecutionService execution =
				new RefundFreightAutoZyDispatchExecutionService(mapper);
		RefundFreightAutoZyDispatchListener listener = new RefundFreightAutoZyDispatchListener(execution);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		registry.registerEventListener(
				DistributionDispatchEventNames.EVENT_REFUND_FREIGHT_AUTO_ZY,
				DistributionDispatchEventNames.LISTENER_REFUND_FREIGHT_AUTO_ZY,
				ListenerDispatchOptions.syncDefaults(),
				listener);

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("company_id", 55L);
		entities.put("is_refund_freight", 0);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		facade.publishEvent(
				DistributionDispatchEventNames.EVENT_REFUND_FREIGHT_AUTO_ZY, payload, DispatchOptions.eventDefaults());

		verify(mapper, never()).updateIsRefundFreightByCompanyAndDistributionType(anyLong(), anyInt(), anyInt());
		assertTrue(captured.isEmpty());
	}
}
