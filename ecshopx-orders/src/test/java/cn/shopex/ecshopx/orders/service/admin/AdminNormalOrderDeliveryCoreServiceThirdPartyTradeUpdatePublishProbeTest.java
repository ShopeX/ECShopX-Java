package cn.shopex.ecshopx.orders.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.NormalOrderDeliveryDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeUpdateDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WxOrderShippingDispatchPublisher;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
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
@DisplayName("Third-party trade update: deliveryNormalPhysical schedules publish after commit")
class AdminNormalOrderDeliveryCoreServiceThirdPartyTradeUpdatePublishProbeTest {

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
	private OrderProcessLogPublishPort orderProcessLogPublishPort;

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
	private ArgumentCaptor<Map<String, Object>> tradeUpdatePayloadCaptor;

	private ObjectMapper objectMapper;
	private AdminNormalOrderDeliveryCoreService service;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();

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
		trade.setTradeId("tr-probe-1");
		trade.setTransactionId("txn_ok");
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

		OrderAssociations postDeliveryAssoc = new OrderAssociations();
		postDeliveryAssoc.setCompanyId(100L);
		postDeliveryAssoc.setOrderId(200L);
		postDeliveryAssoc.setUserId(55L);
		postDeliveryAssoc.setOrderClass("normal");
		postDeliveryAssoc.setOrderStatus("WAIT_BUYER_CONFIRM");
		postDeliveryAssoc.setDeliveryStatus("DONE");
		when(orderAssociationsMapper.selectOne(any())).thenReturn(postDeliveryAssoc);
	}

	@Test
	void deliveryNormalPhysical_afterCommit_invokesThirdPartyTradeUpdatePublishOnce_withAssociationShapedPayload() {
		TransactionTemplate tt = new TransactionTemplate(syncFiringTxManager());

		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", 100L);
		params.put("order_id", 200L);
		params.put("supplier_id", 0);
		params.put("delivery_type", "batch");
		params.put("delivery_corp", "SF");
		params.put("delivery_code", "SFTRACK001");
		params.put("operator_type", "salesperson");
		params.put("operator_id", 771L);

		tt.executeWithoutResult(status -> service.deliveryNormalPhysical(params, null, "normal"));

		verify(thirdPartyTradeUpdateDispatchPublisher, times(1)).publish(tradeUpdatePayloadCaptor.capture());
		Map<String, Object> tradeUpdate = tradeUpdatePayloadCaptor.getValue();
		assertThat(tradeUpdate.get("company_id")).isEqualTo(100L);
		assertThat(tradeUpdate.get("order_id")).isEqualTo("200");
		assertThat(tradeUpdate.get("user_id")).isEqualTo(55L);
		assertThat(tradeUpdate.get("order_class")).isEqualTo("normal");
		assertThat(tradeUpdate.get("order_status")).isEqualTo("WAIT_BUYER_CONFIRM");
		assertThat(tradeUpdate.get("delivery_status")).isEqualTo("DONE");
	}

	/**
	 * Wxapp {@code POST /order/delivery} sets {@code operator_type=user} and {@code operator_id} from
	 * front auth, then calls the same core; third-party trade update must still publish exactly once
	 * post-commit with association-shaped payload (no Wxapp-layer second publish).
	 */
	@Test
	void deliveryNormalPhysical_wxappShapedOperator_afterCommit_invokesThirdPartyTradeUpdatePublishOnce_withAssociationShapedPayload() {
		TransactionTemplate tt = new TransactionTemplate(syncFiringTxManager());

		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", 100L);
		params.put("order_id", 200L);
		params.put("supplier_id", 0);
		params.put("delivery_type", "batch");
		params.put("delivery_corp", "SF");
		params.put("delivery_code", "SFTRACK001");
		params.put("operator_type", "user");
		params.put("operator_id", 42L);

		tt.executeWithoutResult(status -> service.deliveryNormalPhysical(params, null, "normal"));

		verify(thirdPartyTradeUpdateDispatchPublisher, times(1)).publish(tradeUpdatePayloadCaptor.capture());
		Map<String, Object> tradeUpdate = tradeUpdatePayloadCaptor.getValue();
		assertThat(tradeUpdate.get("company_id")).isEqualTo(100L);
		assertThat(tradeUpdate.get("order_id")).isEqualTo("200");
		assertThat(tradeUpdate.get("user_id")).isEqualTo(55L);
		assertThat(tradeUpdate.get("order_class")).isEqualTo("normal");
		assertThat(tradeUpdate.get("order_status")).isEqualTo("WAIT_BUYER_CONFIRM");
		assertThat(tradeUpdate.get("delivery_status")).isEqualTo("DONE");
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
