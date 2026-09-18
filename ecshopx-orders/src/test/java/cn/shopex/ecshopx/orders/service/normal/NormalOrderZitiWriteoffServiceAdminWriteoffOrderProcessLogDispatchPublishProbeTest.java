package cn.shopex.ecshopx.orders.service.normal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeUpdateDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WxOrderShippingDispatchPublisher;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.integration.OrderProcessLogPublishPortImpl;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDetailService;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.ArrayList;
import java.util.Collections;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("EVENT_ORDER_PROCESS_LOG: ziti writeoff publishEvent probe (admin + OpenAPI)")
class NormalOrderZitiWriteoffServiceAdminWriteoffOrderProcessLogDispatchPublishProbeTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, NormalOrders.class);
		TableInfoHelper.initTableInfo(assistant, NormalOrdersItems.class);
		TableInfoHelper.initTableInfo(assistant, OrderAssociations.class);
		TableInfoHelper.initTableInfo(assistant, Trade.class);
	}

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private NormalOrdersItemsMapper normalOrdersItemsMapper;

	@Mock
	private OrderAssociationsMapper orderAssociationsMapper;

	@Mock
	private TradeMapper tradeMapper;

	@Mock
	private NormalOrderBrokerageOnFinishService normalOrderBrokerageOnFinishService;

	@Mock
	private MemberConsumptionOnNormalOrderFinishService memberConsumptionOnNormalOrderFinishService;

	@Mock
	private OrdersRelChinaumspayDivisionWriteService ordersRelChinaumspayDivisionWriteService;

	@Mock
	private WxOrderShippingDispatchPublisher wxOrderShippingDispatchPublisher;

	@Mock
	private OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;

	@Mock
	private AdminNormalOrderDetailService adminNormalOrderDetailService;

	@Mock
	private ThirdPartyTradeUpdateDispatchPublisher thirdPartyTradeUpdateDispatchPublisher;

	@Captor
	private ArgumentCaptor<Map<String, Object>> payloadCaptor;

	private DispatchFacade dispatchFacade;

	private OrderProcessLogPublishPort orderProcessLogPublishPort;

	private NormalOrderZitiWriteoffService service;

	private TransactionTemplate transactionTemplate;

	@BeforeEach
	void setUp() {
		dispatchFacade = mock(DispatchFacade.class);
		orderProcessLogPublishPort = new OrderProcessLogPublishPortImpl(dispatchFacade);
		service =
				new NormalOrderZitiWriteoffService(
						normalOrdersMapper,
						normalOrdersItemsMapper,
						orderAssociationsMapper,
						tradeMapper,
						normalOrderBrokerageOnFinishService,
						memberConsumptionOnNormalOrderFinishService,
						ordersRelChinaumspayDivisionWriteService,
						wxOrderShippingDispatchPublisher,
						orderValiditySettingRedisReadService,
						orderProcessLogPublishPort,
						adminNormalOrderDetailService,
						thirdPartyTradeUpdateDispatchPublisher);
		transactionTemplate = new TransactionTemplate(syncFiringTxManager());
	}

	@Test
	void orderZitiWriteoffForAdmin_whenPickupCodeDisabled_invokesPublishEventOnce_withPhpAlignedPayload() {
		long companyId = 12L;
		long orderId = 70001L;
		long operatorId = 88L;

		NormalOrders order = adminEligibleOrder(companyId, orderId);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);

		NormalOrdersItems line = new NormalOrdersItems();
		line.setNum(2);
		line.setId(null);
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(line));
		when(orderValiditySettingRedisReadService.readPlatformSetting(companyId))
				.thenReturn(Collections.emptyMap());
		when(normalOrdersMapper.update(isNull(), any())).thenReturn(1);
		when(normalOrdersItemsMapper.update(isNull(), any())).thenReturn(1);
		when(orderAssociationsMapper.update(isNull(), any())).thenReturn(1);
		when(tradeMapper.selectList(any())).thenReturn(Collections.emptyList());

		transactionTemplate.executeWithoutResult(
				status -> service.orderZitiWriteoffForAdmin(companyId, orderId, operatorId, false, ""));

		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));

		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals("70001", payload.get("order_id"));
		assertEquals("12", payload.get("company_id"));
		assertEquals("admin", payload.get("operator_type"));
		assertEquals(Boolean.FALSE, payload.get("is_show"));
		assertEquals(operatorId, payload.get("operator_id"));
		assertEquals("订单核销", payload.get("remarks"));
		assertEquals("订单号: 70001, 已被核销. ", payload.get("detail"));

		assertInstanceOf(Map.class, payload.get("params"));
		@SuppressWarnings("unchecked")
		Map<String, Object> params = (Map<String, Object>) payload.get("params");
		assertEquals("70001", params.get("order_id"));
		assertEquals("12", params.get("company_id"));
		assertEquals(Boolean.FALSE, params.get("pickupcode_status"));
		assertEquals("", params.get("pickupcode"));
		assertEquals("admin", params.get("operator_type"));
		assertEquals(operatorId, params.get("operator_id"));
	}

	@Test
	void orderZitiWriteoffForAdmin_whenPickupCodeEnabled_appendsPickupCodeToDetailAndParams() {
		long companyId = 12L;
		long orderId = 70001L;
		long operatorId = 88L;

		NormalOrders order = adminEligibleOrder(companyId, orderId);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);

		NormalOrdersItems line = new NormalOrdersItems();
		line.setNum(2);
		line.setId(null);
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(line));
		when(orderValiditySettingRedisReadService.readPlatformSetting(companyId))
				.thenReturn(Collections.emptyMap());
		when(normalOrdersMapper.update(isNull(), any())).thenReturn(1);
		when(normalOrdersItemsMapper.update(isNull(), any())).thenReturn(1);
		when(orderAssociationsMapper.update(isNull(), any())).thenReturn(1);
		when(tradeMapper.selectList(any())).thenReturn(Collections.emptyList());

		transactionTemplate.executeWithoutResult(
				status -> service.orderZitiWriteoffForAdmin(companyId, orderId, operatorId, true, "PCODE"));

		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));

		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals("订单号: 70001, 已被核销. 核销号: PCODE", payload.get("detail"));

		@SuppressWarnings("unchecked")
		Map<String, Object> params = (Map<String, Object>) payload.get("params");
		assertEquals(Boolean.TRUE, params.get("pickupcode_status"));
		assertEquals("PCODE", params.get("pickupcode"));
	}

	@Test
	@DisplayName("OpenAPI ziti writeoff: single publishEvent, section-3 aligned payload (pickup code off)")
	void orderZitiWriteoffForOpenapi_whenPickupCodeDisabled_invokesPublishEventOnce_withPhpSection3OpenapiPayload() {
		long companyId = 12L;
		long orderId = 70001L;

		NormalOrders order = openapiEligibleOrder(companyId, orderId);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);

		NormalOrdersItems line = new NormalOrdersItems();
		line.setNum(2);
		line.setId(null);
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(line));
		when(orderValiditySettingRedisReadService.readPlatformSetting(companyId))
				.thenReturn(Collections.emptyMap());
		when(normalOrdersMapper.update(isNull(), any())).thenReturn(1);
		when(normalOrdersItemsMapper.update(isNull(), any())).thenReturn(1);
		when(orderAssociationsMapper.update(isNull(), any())).thenReturn(1);
		when(tradeMapper.selectList(any())).thenReturn(Collections.emptyList());

		transactionTemplate.executeWithoutResult(
				status -> service.orderZitiWriteoffForOpenapi(companyId, orderId, false, ""));

		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));

		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals("70001", payload.get("order_id"));
		assertEquals("12", payload.get("company_id"));
		assertEquals("openapi", payload.get("operator_type"));
		assertEquals(Boolean.FALSE, payload.get("is_show"));
		assertEquals(0L, payload.get("operator_id"));
		assertEquals("订单核销", payload.get("remarks"));
		assertEquals("订单号: 70001, 已被核销. ", payload.get("detail"));

		assertInstanceOf(Map.class, payload.get("params"));
		@SuppressWarnings("unchecked")
		Map<String, Object> params = (Map<String, Object>) payload.get("params");
		assertEquals("70001", params.get("order_id"));
		assertEquals("12", params.get("company_id"));
		assertEquals(Boolean.FALSE, params.get("pickupcode_status"));
		assertEquals("", params.get("pickupcode"));
		assertEquals("openapi", params.get("operator_type"));
		assertEquals(0L, params.get("operator_id"));
	}

	@Test
	@DisplayName("OpenAPI ziti writeoff: detail/params include pickup code, openapi operator")
	void orderZitiWriteoffForOpenapi_whenPickupCodeEnabled_appendsPickupCodeToDetailAndParams_openapiOperator() {
		long companyId = 12L;
		long orderId = 70001L;

		NormalOrders order = openapiEligibleOrder(companyId, orderId);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);

		NormalOrdersItems line = new NormalOrdersItems();
		line.setNum(2);
		line.setId(null);
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(line));
		when(orderValiditySettingRedisReadService.readPlatformSetting(companyId))
				.thenReturn(Collections.emptyMap());
		when(normalOrdersMapper.update(isNull(), any())).thenReturn(1);
		when(normalOrdersItemsMapper.update(isNull(), any())).thenReturn(1);
		when(orderAssociationsMapper.update(isNull(), any())).thenReturn(1);
		when(tradeMapper.selectList(any())).thenReturn(Collections.emptyList());

		transactionTemplate.executeWithoutResult(
				status -> service.orderZitiWriteoffForOpenapi(companyId, orderId, true, "PCODE"));

		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));

		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals("openapi", payload.get("operator_type"));
		assertEquals(0L, payload.get("operator_id"));
		assertEquals("订单号: 70001, 已被核销. 核销号: PCODE", payload.get("detail"));

		@SuppressWarnings("unchecked")
		Map<String, Object> params = (Map<String, Object>) payload.get("params");
		assertEquals(Boolean.TRUE, params.get("pickupcode_status"));
		assertEquals("PCODE", params.get("pickupcode"));
		assertEquals("openapi", params.get("operator_type"));
		assertEquals(0L, params.get("operator_id"));
	}

	private static NormalOrders adminEligibleOrder(long companyId, long orderId) {
		NormalOrders order = new NormalOrders();
		order.setCompanyId(companyId);
		order.setOrderId(orderId);
		order.setPayType("wxpay");
		order.setDistributorId(0L);
		order.setUserId(1L);
		return order;
	}

	private static NormalOrders openapiEligibleOrder(long companyId, long orderId) {
		NormalOrders order = new NormalOrders();
		order.setCompanyId(companyId);
		order.setOrderId(orderId);
		order.setPayType("wxpay");
		order.setDistributorId(0L);
		order.setUserId(1L);
		order.setReceiptType("ziti");
		order.setOrderStatus("PAYED");
		order.setZitiStatus("PENDING");
		order.setPayStatus("PAYED");
		order.setCancelStatus("NO_APPLY_CANCEL");
		return order;
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
