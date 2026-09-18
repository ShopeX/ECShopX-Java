package cn.shopex.ecshopx.orders.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
@DisplayName("EVENT_ORDER_PROCESS_LOG: admin POST /order/markdown/confirm — publishEvent probe")
class AdminOrderMarkDownPersistServiceOrderProcessLogDispatchPublishProbeTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OrderAssociations.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrdersItems.class);
	}

	@Mock
	private OrderAssociationsMapper orderAssociationsMapper;

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private NormalOrdersItemsMapper normalOrdersItemsMapper;

	@Captor
	private ArgumentCaptor<Map<String, Object>> payloadCaptor;

	private DispatchFacade dispatchFacade;
	private OrderProcessLogPublishPort orderProcessLogPublishPort;
	private AdminOrderMarkDownPersistService service;

	@BeforeEach
	void setUp() {
		dispatchFacade = mock(DispatchFacade.class);
		orderProcessLogPublishPort = new OrderProcessLogPublishPortImpl(dispatchFacade);
		service =
				new AdminOrderMarkDownPersistService(
						orderAssociationsMapper,
						normalOrdersMapper,
						normalOrdersItemsMapper,
						orderProcessLogPublishPort,
						new ObjectMapper());
		when(orderAssociationsMapper.update(any(), any())).thenReturn(1);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);
	}

	@Test
	void persistAfterMarkdown_whenShouldWrite_invokesPublishEventOnce_withMarkdownConfirmShapedPayload() {
		TransactionTemplate tt = new TransactionTemplate(syncFiringTxManager());

		LinkedHashMap<String, Object> oldOrderInfo = new LinkedHashMap<>();
		oldOrderInfo.put("order_id", 200L);
		oldOrderInfo.put("company_id", 100L);
		oldOrderInfo.put("total_fee", 1000L);

		LinkedHashMap<String, Object> newOrderInfo = new LinkedHashMap<>();
		newOrderInfo.put("order_id", 200L);
		newOrderInfo.put("company_id", 100L);
		newOrderInfo.put("total_fee", 800L);
		newOrderInfo.put("discount_fee", 0);
		newOrderInfo.put("freight_fee", 0);
		newOrderInfo.put("get_points", 0);
		newOrderInfo.put("extra_points", 0);
		newOrderInfo.put("discount_info", new LinkedHashMap<String, Object>());

		LinkedHashMap<String, Object> logParams = new LinkedHashMap<>();
		logParams.put("order_id", "200");
		logParams.put("down_type", 1);
		logParams.put("total_fee", 800);
		logParams.put("operator_id", 42L);
		logParams.put("operator_type", "admin");

		tt.executeWithoutResult(
				status ->
						service.persistAfterMarkdown(
								100L, oldOrderInfo, newOrderInfo, logParams, 42L, "admin"));

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
		assertThat(payload.get("is_show")).isEqualTo(Boolean.TRUE);
		assertThat(payload.get("remarks")).isEqualTo("订单改价");
		assertThat(payload.get("detail")).isEqualTo("订单号：200，手动改价");

		@SuppressWarnings("unchecked")
		Map<String, Object> inner = (Map<String, Object>) payload.get("params");
		assertThat(inner.get("order_id")).isEqualTo("200");
		assertThat(inner.get("down_type")).isEqualTo(1);
		assertThat(inner.get("total_fee")).isEqualTo(800);
		assertThat(inner.get("operator_id")).isEqualTo(42L);
		assertThat(inner.get("operator_type")).isEqualTo("admin");
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
