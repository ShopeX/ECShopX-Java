package cn.shopex.ecshopx.orders.service.tradefinish;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeUpdateDispatchPublisher;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class OrdersTradeFinishNormalOrderPaySuccessApplyServiceTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, NormalOrders.class);
		TableInfoHelper.initTableInfo(assistant, NormalOrdersItems.class);
		TableInfoHelper.initTableInfo(assistant, OrderAssociations.class);
		TableInfoHelper.initTableInfo(assistant, SupplierOrder.class);
	}

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private NormalOrdersItemsMapper normalOrdersItemsMapper;

	@Mock
	private OrderAssociationsMapper orderAssociationsMapper;

	@Mock
	private OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;

	@Test
	void markPayedIfNeeded_employeePurchaseTradeSource_updatesNotpayToPayed() {
		NormalOrders existing = new NormalOrders();
		existing.setOrderStatus("NOTPAY");
		existing.setPayStatus("NOTPAY");
		when(normalOrdersMapper.selectOne(any())).thenReturn(existing);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);
		when(orderAssociationsMapper.update(any(), any())).thenReturn(1);
		SupplierOrderMapper supplierOrderMapper = mock(SupplierOrderMapper.class);
		when(supplierOrderMapper.update(any(), any())).thenReturn(1);

		ThirdPartyTradeUpdateDispatchPublisher tradeUpdatePublisher =
				mock(ThirdPartyTradeUpdateDispatchPublisher.class);
		existing.setOrderClass("employee_purchase");
		existing.setUserId(42L);
		OrdersTradeFinishNormalOrderPaySuccessApplyService service =
				new OrdersTradeFinishNormalOrderPaySuccessApplyService(
						normalOrdersMapper,
						normalOrdersItemsMapper,
						orderAssociationsMapper,
						supplierOrderMapper,
						orderValiditySettingRedisReadService,
						tradeUpdatePublisher);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("trade_state", "SUCCESS");
		payload.put("company_id", 1L);
		payload.put("order_id", 5331426000090002L);
		payload.put("trade_source_type", "normal_employee_purchase");
		payload.put("pay_type", "deposit");

		Optional<Map<String, Object>> out = service.markPayedIfNeeded(payload);

		assertTrue(out.isPresent());
		assertEquals(1L, out.get().get("company_id"));
		assertEquals(5331426000090002L, out.get().get("order_id"));
		verify(normalOrdersMapper).update(any(), any());
		verify(orderAssociationsMapper).update(any(), any());
		verify(supplierOrderMapper).update(any(), any());
		verify(tradeUpdatePublisher)
				.publish(
						argThat(
								m ->
										m != null
												&& Long.valueOf(1L).equals(m.get("company_id"))
												&& Long.valueOf(5331426000090002L).equals(m.get("order_id"))
												&& Long.valueOf(42L).equals(m.get("user_id"))
												&& "employee_purchase".equals(m.get("order_class"))));
	}

	@Test
	void markPayedIfNeeded_partPaymentOrderStatus_advancesToPayed() {
		NormalOrders existing = new NormalOrders();
		existing.setOrderStatus("PART_PAYMENT");
		existing.setPayStatus("NOTPAY");
		existing.setOrderClass("normal");
		existing.setUserId(9L);
		when(normalOrdersMapper.selectOne(any())).thenReturn(existing);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);
		when(orderAssociationsMapper.update(any(), any())).thenReturn(1);
		SupplierOrderMapper supplierOrderMapper = mock(SupplierOrderMapper.class);
		when(supplierOrderMapper.update(any(), any())).thenReturn(1);
		ThirdPartyTradeUpdateDispatchPublisher tradeUpdatePublisher =
				mock(ThirdPartyTradeUpdateDispatchPublisher.class);
		OrdersTradeFinishNormalOrderPaySuccessApplyService service =
				new OrdersTradeFinishNormalOrderPaySuccessApplyService(
						normalOrdersMapper,
						normalOrdersItemsMapper,
						orderAssociationsMapper,
						supplierOrderMapper,
						orderValiditySettingRedisReadService,
						tradeUpdatePublisher);

		Map<String, Object> payload = successPayload(1L, 600L, "normal_normal");

		Optional<Map<String, Object>> out = service.markPayedIfNeeded(payload);

		assertTrue(out.isPresent());
		assertEquals(1L, out.get().get("company_id"));
		assertEquals(600L, out.get().get("order_id"));
		verify(normalOrdersMapper).update(any(), any());
		verify(orderAssociationsMapper).update(any(), any());
		verify(supplierOrderMapper).update(any(), any());
	}

	@Test
	void markPayedIfNeeded_alreadyPayedOrderStatus_returnsEmpty() {
		NormalOrders existing = new NormalOrders();
		existing.setOrderStatus("PAYED");
		existing.setPayStatus("PAYED");
		when(normalOrdersMapper.selectOne(any())).thenReturn(existing);
		when(normalOrdersMapper.update(any(), any())).thenReturn(0);
		SupplierOrderMapper supplierOrderMapper = mock(SupplierOrderMapper.class);
		ThirdPartyTradeUpdateDispatchPublisher tradeUpdatePublisher =
				mock(ThirdPartyTradeUpdateDispatchPublisher.class);
		OrdersTradeFinishNormalOrderPaySuccessApplyService service =
				new OrdersTradeFinishNormalOrderPaySuccessApplyService(
						normalOrdersMapper,
						normalOrdersItemsMapper,
						orderAssociationsMapper,
						supplierOrderMapper,
						orderValiditySettingRedisReadService,
						tradeUpdatePublisher);

		assertTrue(service.markPayedIfNeeded(successPayload(1L, 700L, "normal_normal")).isEmpty());
		verify(normalOrdersMapper).update(any(), any());
		verify(orderAssociationsMapper, never()).update(any(), any());
		verify(supplierOrderMapper, never()).update(any(), any());
		verify(tradeUpdatePublisher, never()).publish(any());
	}

	@Test
	void markPayedIfNeeded_unknownTradeSource_skipsUpdate() {
		SupplierOrderMapper supplierOrderMapper = mock(SupplierOrderMapper.class);
		ThirdPartyTradeUpdateDispatchPublisher tradeUpdatePublisher =
				mock(ThirdPartyTradeUpdateDispatchPublisher.class);
		OrdersTradeFinishNormalOrderPaySuccessApplyService service =
				new OrdersTradeFinishNormalOrderPaySuccessApplyService(
						normalOrdersMapper,
						normalOrdersItemsMapper,
						orderAssociationsMapper,
						supplierOrderMapper,
						orderValiditySettingRedisReadService,
						tradeUpdatePublisher);

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("trade_state", "SUCCESS");
		payload.put("company_id", 1L);
		payload.put("order_id", 100L);
		payload.put("trade_source_type", "membercard");

		assertTrue(service.markPayedIfNeeded(payload).isEmpty());
		verify(normalOrdersMapper, never()).update(any(), any());
		verify(tradeUpdatePublisher, never()).publish(any());
	}

	/**
	 * Fallback: synchronizations active but no actual TX — publish TradeUpdate immediately.
	 */
	@Test
	void markPayedIfNeeded_syncActiveWithoutActualTx_publishesTradeUpdateImmediately() {
		OrdersTradeFinishNormalOrderPaySuccessApplyService service =
				newServiceForPayedUpdate(tradeUpdateFixture(38L, 5437476000090278L, "normal", 278L));
		Map<String, Object> payload = successPayload(38L, 5437476000090278L, "normal_normal");
		ThirdPartyTradeUpdateDispatchPublisher tradeUpdatePublisher = lastTradeUpdatePublisher;

		TransactionSynchronizationManager.initSynchronization();
		try {
			assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
			assertTrue(service.markPayedIfNeeded(payload).isPresent());
			verify(tradeUpdatePublisher)
					.publish(
							argThat(
									m ->
											m != null
													&& Long.valueOf(38L).equals(m.get("company_id"))
													&& Long.valueOf(5437476000090278L).equals(m.get("order_id"))
													&& "normal".equals(m.get("order_class"))));
		} finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	/**
	 * Happy path for {@code REQUIRES_NEW}: real TX still open → defer to afterCommit, then publish.
	 */
	@Test
	void markPayedIfNeeded_actualTxActive_publishesTradeUpdateOnAfterCommit() {
		OrdersTradeFinishNormalOrderPaySuccessApplyService service =
				newServiceForPayedUpdate(tradeUpdateFixture(38L, 5437544000200278L, "normal", 278L));
		Map<String, Object> payload = successPayload(38L, 5437544000200278L, "normal_normal");
		ThirdPartyTradeUpdateDispatchPublisher tradeUpdatePublisher = lastTradeUpdatePublisher;

		TransactionSynchronizationManager.initSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(true);
		try {
			assertTrue(service.markPayedIfNeeded(payload).isPresent());
			verify(tradeUpdatePublisher, never()).publish(any());

			List<TransactionSynchronization> synchs =
					new ArrayList<>(TransactionSynchronizationManager.getSynchronizations());
			assertTrue(synchs.size() >= 1);
			for (TransactionSynchronization sync : synchs) {
				sync.afterCommit();
			}
			verify(tradeUpdatePublisher)
					.publish(
							argThat(
									m ->
											m != null
													&& Long.valueOf(38L).equals(m.get("company_id"))
													&& Long.valueOf(5437544000200278L).equals(m.get("order_id"))
													&& "normal".equals(m.get("order_class"))));
		} finally {
			TransactionSynchronizationManager.clear();
		}
	}

	/**
	 * Documents the pre-{@code REQUIRES_NEW} failure: registering afterCommit while already inside an
	 * outer afterCommit snapshot never runs. Production relies on REQUIRES_NEW so markPayed runs in a
	 * fresh TX whose afterCommit is invoked by Spring after that TX commits.
	 */
	@Test
	void markPayedIfNeeded_nestedInsideAfterCommitSnapshot_missesUntilNewSyncFired() {
		OrdersTradeFinishNormalOrderPaySuccessApplyService service =
				newServiceForPayedUpdate(tradeUpdateFixture(38L, 5437544000200278L, "normal", 278L));
		Map<String, Object> payload = successPayload(38L, 5437544000200278L, "normal_normal");
		ThirdPartyTradeUpdateDispatchPublisher tradeUpdatePublisher = lastTradeUpdatePublisher;

		TransactionSynchronizationManager.initSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(true);
		AtomicBoolean outerRan = new AtomicBoolean(false);
		TransactionSynchronizationManager.registerSynchronization(
				new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						outerRan.set(true);
						assertTrue(service.markPayedIfNeeded(payload).isPresent());
					}
				});
		try {
			List<TransactionSynchronization> snapshot =
					List.copyOf(TransactionSynchronizationManager.getSynchronizations());
			Set<TransactionSynchronization> snapshotSet = new HashSet<>(snapshot);
			for (TransactionSynchronization sync : snapshot) {
				sync.afterCommit();
			}
			assertTrue(outerRan.get());
			// Spring-style snapshot miss: nested registration did not run.
			verify(tradeUpdatePublisher, never()).publish(any());

			for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
				if (!snapshotSet.contains(sync)) {
					sync.afterCommit();
				}
			}
			verify(tradeUpdatePublisher)
					.publish(
							argThat(
									m ->
											m != null
													&& Long.valueOf(38L).equals(m.get("company_id"))
													&& Long.valueOf(5437544000200278L).equals(m.get("order_id"))));
		} finally {
			TransactionSynchronizationManager.clear();
		}
	}

	private ThirdPartyTradeUpdateDispatchPublisher lastTradeUpdatePublisher;

	private record OrderFixture(long companyId, long orderId, String orderClass, long userId) {}

	private static OrderFixture tradeUpdateFixture(
			long companyId, long orderId, String orderClass, long userId) {
		return new OrderFixture(companyId, orderId, orderClass, userId);
	}

	private static Map<String, Object> successPayload(long companyId, long orderId, String tradeSource) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("trade_state", "SUCCESS");
		payload.put("company_id", companyId);
		payload.put("order_id", orderId);
		payload.put("trade_source_type", tradeSource);
		return payload;
	}

	private OrdersTradeFinishNormalOrderPaySuccessApplyService newServiceForPayedUpdate(
			OrderFixture fixture) {
		NormalOrders existing = new NormalOrders();
		existing.setOrderStatus("NOTPAY");
		existing.setPayStatus("NOTPAY");
		existing.setOrderClass(fixture.orderClass());
		existing.setUserId(fixture.userId());
		when(normalOrdersMapper.selectOne(any())).thenReturn(existing);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);
		when(orderAssociationsMapper.update(any(), any())).thenReturn(1);
		SupplierOrderMapper supplierOrderMapper = mock(SupplierOrderMapper.class);
		when(supplierOrderMapper.update(any(), any())).thenReturn(1);
		lastTradeUpdatePublisher = mock(ThirdPartyTradeUpdateDispatchPublisher.class);
		return new OrdersTradeFinishNormalOrderPaySuccessApplyService(
				normalOrdersMapper,
				normalOrdersItemsMapper,
				orderAssociationsMapper,
				supplierOrderMapper,
				orderValiditySettingRedisReadService,
				lastTradeUpdatePublisher);
	}
}
