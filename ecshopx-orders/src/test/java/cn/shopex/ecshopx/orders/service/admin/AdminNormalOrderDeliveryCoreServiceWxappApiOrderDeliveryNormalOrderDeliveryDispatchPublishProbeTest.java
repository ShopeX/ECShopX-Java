package cn.shopex.ecshopx.orders.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.NormalOrderDeliveryDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeUpdateDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WxOrderShippingDispatchPublisher;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.integration.OrderProcessLogPublishPortImpl;
import cn.shopex.ecshopx.orders.domain.CompanyRelLogistics;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.OrdersDelivery;
import cn.shopex.ecshopx.orders.domain.OrdersDeliveryItems;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.integration.notify.OrderDeliverySuccessNotifyPort;
import cn.shopex.ecshopx.orders.mapper.CompanyRelLogisticsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.OrdersDeliveryItemsMapper;
import cn.shopex.ecshopx.orders.mapper.OrdersDeliveryMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("EVENT_NORMAL_ORDER_DELIVERY: wxapp delivery core — NormalOrderDeliveryDispatchPublisher.publish probe")
class AdminNormalOrderDeliveryCoreServiceWxappApiOrderDeliveryNormalOrderDeliveryDispatchPublishProbeTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrdersItems.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OrderAssociations.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Trade.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), CompanyRelLogistics.class);
	}

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private NormalOrdersItemsMapper normalOrdersItemsMapper;

	@Mock
	private OrdersDeliveryMapper ordersDeliveryMapper;

	@Mock
	private OrdersDeliveryItemsMapper ordersDeliveryItemsMapper;

	@Mock
	private OrderAssociationsMapper orderAssociationsMapper;

	@Mock
	private CompanyRelLogisticsMapper companyRelLogisticsMapper;

	@Mock
	private SupplierOrderMapper supplierOrderMapper;

	@Mock
	private StringRedisTemplate stringRedisTemplate;

	@Mock
	private OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;

	@Mock
	private ApplicationEventPublisher applicationEventPublisher;

	@Mock
	private OrderDeliverySuccessNotifyPort orderDeliverySuccessNotifyPort;

	@Mock
	private TradeMapper tradeMapper;

	@Mock
	private WxOrderShippingDispatchPublisher wxOrderShippingDispatchPublisher;

	@Mock
	private ThirdPartyTradeUpdateDispatchPublisher thirdPartyTradeUpdateDispatchPublisher;

	@Mock
	private NormalOrderDeliveryDispatchPublisher normalOrderDeliveryDispatchPublisher;

	@Captor
	private ArgumentCaptor<Map<String, Object>> deliveryPayloadCaptor;

	private DispatchFacade dispatchFacade;
	private OrderProcessLogPublishPort orderProcessLogPublishPort;
	private ObjectMapper objectMapper;
	private AdminNormalOrderDeliveryCoreService service;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		dispatchFacade = mock(DispatchFacade.class);
		orderProcessLogPublishPort = new OrderProcessLogPublishPortImpl(dispatchFacade);

		ValueOperations<String, String> vo = mock(ValueOperations.class);
		when(stringRedisTemplate.opsForValue()).thenReturn(vo);
		when(vo.get(any())).thenReturn(null);

		service =
				new AdminNormalOrderDeliveryCoreService(
						normalOrdersMapper,
						normalOrdersItemsMapper,
						ordersDeliveryMapper,
						ordersDeliveryItemsMapper,
						orderAssociationsMapper,
						companyRelLogisticsMapper,
						supplierOrderMapper,
						stringRedisTemplate,
						orderValiditySettingRedisReadService,
						applicationEventPublisher,
						orderProcessLogPublishPort,
						orderDeliverySuccessNotifyPort,
						tradeMapper,
						objectMapper,
						wxOrderShippingDispatchPublisher,
						thirdPartyTradeUpdateDispatchPublisher,
						normalOrderDeliveryDispatchPublisher);

		OrderAssociations postDeliveryAssoc = new OrderAssociations();
		postDeliveryAssoc.setCompanyId(100L);
		postDeliveryAssoc.setOrderId(200L);
		postDeliveryAssoc.setUserId(55L);
		postDeliveryAssoc.setOrderClass("normal");
		postDeliveryAssoc.setOrderStatus("WAIT_BUYER_CONFIRM");
		lenient().when(orderAssociationsMapper.selectOne(any())).thenReturn(postDeliveryAssoc);

		when(orderValiditySettingRedisReadService.readPlatformSetting(any(Long.class)))
				.thenReturn(Map.of("order_finish_time", 7));
		when(companyRelLogisticsMapper.selectOne(any())).thenReturn(null);
		when(normalOrdersItemsMapper.selectCount(any())).thenReturn(0L);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);
		when(normalOrdersItemsMapper.update(any(), any())).thenReturn(1);
		when(orderAssociationsMapper.update(any(), any())).thenReturn(1);
		when(ordersDeliveryItemsMapper.insert(any(OrdersDeliveryItems.class))).thenReturn(1);
		doAnswer(
						inv -> {
							OrdersDelivery d = inv.getArgument(0);
							d.setOrdersDeliveryId(999L);
							return 1;
						})
				.when(ordersDeliveryMapper)
				.insert(any(OrdersDelivery.class));

		Trade trade = new Trade();
		trade.setTradeId("tr-wx-1");
		trade.setTransactionId("wx_txn_ok");
		trade.setWxaAppid("wxa-test");
		when(tradeMapper.selectList(any())).thenReturn(List.of(trade));

		NormalOrders orderRow = new NormalOrders();
		orderRow.setOrderId(200L);
		orderRow.setCompanyId(100L);
		orderRow.setReceiptType("normal");
		orderRow.setOrderStatus("PAYED");
		orderRow.setCancelStatus("NO_APPLY_CANCEL");
		orderRow.setDeliveryStatus("PENDING");
		orderRow.setUserId(55L);
		orderRow.setLeftAftersalesNum(0);
		when(normalOrdersMapper.selectOne(any())).thenReturn(orderRow);

		NormalOrdersItems line = new NormalOrdersItems();
		line.setId(10L);
		line.setCompanyId(100L);
		line.setOrderId(200L);
		line.setNum(2);
		line.setDeliveryStatus("PENDING");
		line.setGoodsId(1L);
		line.setItemId(2L);
		line.setItemName("Item-A");
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(line));
	}

	@Test
	@DisplayName(
			"EVENT_NORMAL_ORDER_DELIVERY: wxapp delivery core — NormalOrderDeliveryDispatchPublisher.publish once after commit")
	void deliveryNormalPhysical_afterCommit_invokesNormalOrderDeliveryPublisherOnceWithPayload() {
		TransactionTemplate tt = new TransactionTemplate(syncFiringTxManager());

		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", 100L);
		params.put("order_id", 200L);
		params.put("supplier_id", 0);
		params.put("delivery_type", "batch");
		params.put("delivery_corp", "SF");
		params.put("delivery_code", "SFTRACK001");
		params.put("operator_type", "user");
		params.put("operator_id", 55L);

		tt.executeWithoutResult(status -> service.deliveryNormalPhysical(params, null, "normal"));

		verify(normalOrderDeliveryDispatchPublisher, times(1)).publish(deliveryPayloadCaptor.capture());
		Map<String, Object> payload = deliveryPayloadCaptor.getValue();
		assertThat(payload.get("order_id")).isEqualTo(200L);
		assertThat(payload.get("company_id")).isEqualTo(100L);
	}

	@Test
	void deliveryNormalPhysical_salespersonBatch_afterCommit_invokesNormalOrderDeliveryPublisherOnceWithOrderAndCompanyPayload() {
		TransactionTemplate tt = new TransactionTemplate(syncFiringTxManager());

		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", 100L);
		params.put("order_id", 200L);
		params.put("supplier_id", 0);
		params.put("delivery_type", "batch");
		params.put("delivery_corp", "SF");
		params.put("delivery_code", "SFTRACK003");
		params.put("operator_type", "salesperson");
		params.put("operator_id", 42L);

		tt.executeWithoutResult(status -> service.deliveryNormalPhysical(params, null, "normal"));

		verify(normalOrderDeliveryDispatchPublisher, times(1)).publish(deliveryPayloadCaptor.capture());
		Map<String, Object> payload = deliveryPayloadCaptor.getValue();
		assertThat(payload).containsOnlyKeys("order_id", "company_id");
		assertThat(payload.get("order_id")).isEqualTo(200L);
		assertThat(payload.get("company_id")).isEqualTo(100L);
	}

	@Test
	@DisplayName(
			"event:237 / DmCrm row: wxapp core afterCommit parent publish — two-key map only (order_id, company_id); unreachable legacy listener anchor excluded from this probe")
	void deliveryNormalPhysical_salespersonBatch_afterCommit_event237DmCrmAnchor_publishOnce_twoKeyMap_9002_22() {
		NormalOrders orderRow237 = new NormalOrders();
		orderRow237.setOrderId(9002L);
		orderRow237.setCompanyId(22L);
		orderRow237.setReceiptType("normal");
		orderRow237.setOrderStatus("PAYED");
		orderRow237.setCancelStatus("NO_APPLY_CANCEL");
		orderRow237.setDeliveryStatus("PENDING");
		orderRow237.setUserId(77L);
		orderRow237.setLeftAftersalesNum(0);
		when(normalOrdersMapper.selectOne(any())).thenReturn(orderRow237);

		NormalOrdersItems line237 = new NormalOrdersItems();
		line237.setId(1102L);
		line237.setCompanyId(22L);
		line237.setOrderId(9002L);
		line237.setNum(1);
		line237.setDeliveryStatus("PENDING");
		line237.setGoodsId(901L);
		line237.setItemId(902L);
		line237.setItemName("Item-event237");
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(line237));

		OrderAssociations assoc237 = new OrderAssociations();
		assoc237.setCompanyId(22L);
		assoc237.setOrderId(9002L);
		assoc237.setUserId(77L);
		assoc237.setOrderClass("normal");
		assoc237.setOrderStatus("WAIT_BUYER_CONFIRM");
		lenient().when(orderAssociationsMapper.selectOne(any())).thenReturn(assoc237);

		TransactionTemplate tt = new TransactionTemplate(syncFiringTxManager());

		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", 22L);
		params.put("order_id", 9002L);
		params.put("supplier_id", 0);
		params.put("delivery_type", "batch");
		params.put("delivery_corp", "JD");
		params.put("delivery_code", "JDV2379002");
		params.put("operator_type", "salesperson");
		params.put("operator_id", 99L);

		tt.executeWithoutResult(status -> service.deliveryNormalPhysical(params, null, "normal"));

		verify(normalOrderDeliveryDispatchPublisher, times(1)).publish(deliveryPayloadCaptor.capture());
		Map<String, Object> payload = deliveryPayloadCaptor.getValue();
		assertThat(payload).containsOnlyKeys("order_id", "company_id");
		assertThat(payload.get("order_id")).isEqualTo(9002L);
		assertThat(payload.get("company_id")).isEqualTo(22L);
	}

	@Test
	void deliveryNormalPhysical_onRollback_doesNotInvokeNormalOrderDeliveryPublisher() {
		TransactionTemplate tt = new TransactionTemplate(rollbackSkippingAfterCommitTxManager());

		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", 100L);
		params.put("order_id", 200L);
		params.put("supplier_id", 0);
		params.put("delivery_type", "batch");
		params.put("delivery_corp", "SF");
		params.put("delivery_code", "SFTRACK002");
		params.put("operator_type", "user");
		params.put("operator_id", 55L);

		tt.executeWithoutResult(
				status -> {
					service.deliveryNormalPhysical(params, null, "normal");
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
