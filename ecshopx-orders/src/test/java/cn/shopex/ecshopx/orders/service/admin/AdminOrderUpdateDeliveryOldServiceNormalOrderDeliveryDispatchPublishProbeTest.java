package cn.shopex.ecshopx.orders.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.NormalOrderDeliveryDispatchPublisher;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
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
@DisplayName("EVENT_NORMAL_ORDER_DELIVERY: admin updateDeliveryOld — NormalOrderDeliveryDispatchPublisher.publish probe")
class AdminOrderUpdateDeliveryOldServiceNormalOrderDeliveryDispatchPublishProbeTest {

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
	private OrderProcessLogPublishPort orderProcessLogPublishPort;

	@Mock
	private NormalOrderDeliveryDispatchPublisher normalOrderDeliveryDispatchPublisher;

	@Captor
	private ArgumentCaptor<Map<String, Object>> deliveryPayloadCaptor;

	private AdminOrderUpdateDeliveryOldService service;

	@BeforeEach
	void setUp() {
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
	void updateDeliveryOld_afterCommit_invokesNormalOrderDeliveryPublisherOnceWithPayload() {
		TransactionTemplate tt = new TransactionTemplate(syncFiringTxManager());

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("delivery_type", "batch");
		merged.put("delivery_corp", "SF");
		merged.put("delivery_code", "SF123");

		tt.executeWithoutResult(status -> service.updateDeliveryOld(100L, 42L, "200", merged));

		verify(normalOrderDeliveryDispatchPublisher, times(1)).publish(deliveryPayloadCaptor.capture());
		Map<String, Object> payload = deliveryPayloadCaptor.getValue();
		assertThat(payload.get("order_id")).isEqualTo(200L);
		assertThat(payload.get("company_id")).isEqualTo(100L);
	}

	@Test
	void updateDeliveryOld_onRollback_doesNotInvokeNormalOrderDeliveryPublisher() {
		TransactionTemplate tt = new TransactionTemplate(rollbackSkippingAfterCommitTxManager());

		LinkedHashMap<String, Object> merged = new LinkedHashMap<>();
		merged.put("delivery_type", "batch");
		merged.put("delivery_corp", "SF");
		merged.put("delivery_code", "SF456");

		tt.executeWithoutResult(
				status -> {
					service.updateDeliveryOld(100L, 42L, "200", merged);
					status.setRollbackOnly();
				});

		verify(normalOrderDeliveryDispatchPublisher, never()).publish(any());
	}

	private static PlatformTransactionManager syncFiringTxManager() {
		return new PlatformTransactionManager() {
			@Override
			public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
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

	private static PlatformTransactionManager rollbackSkippingAfterCommitTxManager() {
		return new PlatformTransactionManager() {
			@Override
			public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
				if (TransactionSynchronizationManager.isSynchronizationActive()) {
					throw new IllegalStateException("nested tx not expected in unit test");
				}
				TransactionSynchronizationManager.initSynchronization();
				return new SimpleTransactionStatus(true);
			}

			@Override
			public void commit(TransactionStatus status) throws TransactionException {
				if (status.isRollbackOnly()) {
					rollback(status);
					return;
				}
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
