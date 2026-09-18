package cn.shopex.ecshopx.orders.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.NormalOrderDeliveryDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.integration.OrderProcessLogPublishPortImpl;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("EVENT_ORDER_PROCESS_LOG: admin PUT /api/v1/old_delivery/{orderId} — publishEvent probe")
class AdminOrderUpdateDeliveryOldServiceOrderProcessLogDispatchPublishProbeTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrdersItems.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OrderAssociations.class);
	}

	@Mock
	private OrderAssociationsMapper orderAssociationsMapper;

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private NormalOrdersItemsMapper normalOrdersItemsMapper;

	@Mock
	private OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService;

	@Mock
	private OrderAssociationAssociationDataAssembler orderAssociationAssociationDataAssembler;

	@Mock
	private NormalOrderDeliveryDispatchPublisher normalOrderDeliveryDispatchPublisher;

	@Captor
	private ArgumentCaptor<Map<String, Object>> payloadCaptor;

	private DispatchFacade dispatchFacade;
	private OrderProcessLogPublishPort orderProcessLogPublishPort;
	private AdminOrderUpdateDeliveryOldService service;

	@BeforeEach
	void setUp() {
		dispatchFacade = mock(DispatchFacade.class);
		orderProcessLogPublishPort = new OrderProcessLogPublishPortImpl(dispatchFacade);
		ObjectMapper objectMapper = new ObjectMapper();
		service =
				new AdminOrderUpdateDeliveryOldService(
						orderAssociationsMapper,
						normalOrdersMapper,
						normalOrdersItemsMapper,
						orderAssociationEffectiveTypeService,
						orderAssociationAssociationDataAssembler,
						orderProcessLogPublishPort,
						normalOrderDeliveryDispatchPublisher,
						objectMapper);

		OrderAssociations assoc = new OrderAssociations();
		assoc.setCompanyId(100L);
		assoc.setOrderId(200L);
		when(orderAssociationsMapper.selectOne(any())).thenReturn(assoc, assoc);
		when(orderAssociationEffectiveTypeService.effectiveOrderType(assoc)).thenReturn("normal");

		NormalOrders orderRow = new NormalOrders();
		orderRow.setOrderId(200L);
		orderRow.setCompanyId(100L);
		orderRow.setOrderStatus("WAIT_BUYER_CONFIRM");
		when(normalOrdersMapper.selectOne(any())).thenReturn(orderRow);

		when(normalOrdersItemsMapper.update(any(), any())).thenReturn(1);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);
		when(orderAssociationsMapper.update(any(), any())).thenReturn(1);

		when(orderAssociationAssociationDataAssembler.toAssociationDataMap(any()))
				.thenReturn(new LinkedHashMap<>(Map.of("order_id", 200L)));
	}

	@Test
	void updateDeliveryOld_afterCommit_invokesPublishEventOnce_withUpdateDeliveryOldShapedPayload() {
		TransactionTemplate tt = new TransactionTemplate(syncFiringTxManager());

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("delivery_type", "batch");
		merged.put("delivery_corp", "SF");
		merged.put("delivery_code", "SF123");

		tt.executeWithoutResult(
				status ->
						service.updateDeliveryOld(100L, 42L, "200", merged));

		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));

		Map<String, Object> payload = payloadCaptor.getValue();
		assertThat(payload.get("order_id")).isEqualTo(200L);
		assertThat(payload.get("company_id")).isEqualTo(100L);
		assertThat(payload.get("operator_type")).isEqualTo("admin");
		assertThat(payload.get("operator_id")).isEqualTo(42L);
		assertThat(payload.get("remarks")).isEqualTo("订单发货");
		assertThat(payload.get("detail")).isEqualTo("订单号：200，订单发货信息修改");

		@SuppressWarnings("unchecked")
		Map<String, Object> inner = (Map<String, Object>) payload.get("params");
		assertThat(inner.get("delivery_type")).isEqualTo("batch");
		assertThat(inner.get("delivery_corp")).isEqualTo("SF");
		assertThat(inner.get("delivery_code")).isEqualTo("SF123");
		assertThat(inner.get("company_id")).isEqualTo(100L);
		assertThat(inner.get("operator_type")).isEqualTo("admin");
		assertThat(inner.get("operator_id")).isEqualTo(42L);
	}

	private static PlatformTransactionManager syncFiringTxManager() {
		return new PlatformTransactionManager() {
			@Override
			public TransactionStatus getTransaction(TransactionDefinition definition)
					throws TransactionException {
				if (TransactionSynchronizationManager.isSynchronizationActive()) {
					throw new IllegalStateException("nested tx not expected in unit test");
				}
				TransactionSynchronizationManager.initSynchronization();
				return new SimpleTransactionStatus(true);
			}

			@Override
			public void commit(TransactionStatus status) throws TransactionException {
				try {
					if (TransactionSynchronizationManager.isSynchronizationActive()) {
						for (TransactionSynchronization s :
								new ArrayList<>(TransactionSynchronizationManager.getSynchronizations())) {
							s.afterCommit();
						}
					}
				} finally {
					if (TransactionSynchronizationManager.isSynchronizationActive()) {
						TransactionSynchronizationManager.clearSynchronization();
					}
				}
			}

			@Override
			public void rollback(TransactionStatus status) throws TransactionException {
				if (TransactionSynchronizationManager.isSynchronizationActive()) {
					TransactionSynchronizationManager.clearSynchronization();
				}
			}
		};
	}
}
