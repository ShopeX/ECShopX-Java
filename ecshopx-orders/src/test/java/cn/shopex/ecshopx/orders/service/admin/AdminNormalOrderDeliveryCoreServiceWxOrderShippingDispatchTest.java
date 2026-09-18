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
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class AdminNormalOrderDeliveryCoreServiceWxOrderShippingDispatchTest {

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
		lenient().when(normalOrdersItemsMapper.selectCount(any())).thenReturn(0L);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);
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
		line.setDeliveryItemNum(0);
		line.setCancelItemNum(0);
		line.setDeliveryStatus("PENDING");
		line.setGoodsId(1L);
		line.setItemId(2L);
		line.setItemName("Item-A");
		AtomicBoolean lineDelivered = new AtomicBoolean(false);
		when(normalOrdersItemsMapper.selectList(any()))
				.thenAnswer(
						inv -> {
							NormalOrdersItems row = new NormalOrdersItems();
							row.setId(line.getId());
							row.setCompanyId(line.getCompanyId());
							row.setOrderId(line.getOrderId());
							row.setNum(line.getNum());
							row.setGoodsId(line.getGoodsId());
							row.setItemId(line.getItemId());
							row.setItemName(line.getItemName());
							row.setCancelItemNum(line.getCancelItemNum());
							if (lineDelivered.get()) {
								row.setDeliveryItemNum(line.getNum());
								row.setDeliveryStatus("DONE");
							} else {
								row.setDeliveryItemNum(line.getDeliveryItemNum());
								row.setDeliveryStatus(line.getDeliveryStatus());
							}
							return List.of(row);
						});
		doAnswer(
						inv -> {
							lineDelivered.set(true);
							return 1;
						})
				.when(normalOrdersItemsMapper)
				.update(any(), any());

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
	void deliveryNormalPhysical_whenReceiptTypeMerchant_publishesWxOrderShippingWithDadaReceiptType() {
		NormalOrders merchantOrder = new NormalOrders();
		merchantOrder.setOrderId(200L);
		merchantOrder.setCompanyId(100L);
		merchantOrder.setReceiptType("merchant");
		merchantOrder.setOrderStatus("PAYED");
		merchantOrder.setCancelStatus("NO_APPLY_CANCEL");
		merchantOrder.setDeliveryStatus("PENDING");
		merchantOrder.setUserId(55L);
		merchantOrder.setLeftAftersalesNum(0);
		when(normalOrdersMapper.selectOne(any())).thenReturn(merchantOrder);

		TransactionTemplate tt = new TransactionTemplate(syncFiringTxManager());

		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", 100L);
		params.put("order_id", 200L);
		params.put("supplier_id", 0);
		params.put("delivery_type", "batch");
		params.put("delivery_corp", "MERCHANT");
		params.put("delivery_code", "SELF-DELIVERY-001");
		params.put("self_delivery_status", "DELIVERING");
		params.put("self_delivery_operator_id", 99L);

		tt.executeWithoutResult(
				status -> {
					service.deliveryNormalPhysical(params, null, "normal");
					verify(wxOrderShippingDispatchPublisher, never()).publish(any());
				});

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(wxOrderShippingDispatchPublisher, times(1)).publish(captor.capture());
		Map<String, Object> published = captor.getValue();
		assertThat(published.get("receipt_type")).isEqualTo("dada");
		assertThat(published.get("delivery_code")).isEqualTo("SELF-DELIVERY-001");
	}

	@Test
	void deliveryNormalPhysical_publishesWxOrderShippingAfterCommit_withLogisticsPayload() {
		TransactionTemplate tt = new TransactionTemplate(syncFiringTxManager());

		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", 100L);
		params.put("order_id", 200L);
		params.put("supplier_id", 0);
		params.put("delivery_type", "batch");
		params.put("delivery_corp", "SF");
		params.put("delivery_code", "SFTRACK001");

		tt.executeWithoutResult(
				status -> {
					service.deliveryNormalPhysical(params, null, "normal");
					verify(wxOrderShippingDispatchPublisher, never()).publish(any());
				});

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(wxOrderShippingDispatchPublisher, times(1)).publish(captor.capture());
		Map<String, Object> published = captor.getValue();
		assertThat(published.get("receipt_type")).isEqualTo("logistics");
		assertThat(published.get("delivery_type")).isEqualTo("batch");
		assertThat(published.get("is_all_delivered")).isEqualTo(true);
		assertThat(published.get("delivery_corp")).isEqualTo("SF");
		assertThat(published.get("delivery_code")).isEqualTo("SFTRACK001");
		assertThat(published.get("company_id")).isEqualTo(100L);
		assertThat(published.get("order_id")).isEqualTo(200L);
		assertThat(published.get("trade_id")).isEqualTo("tr-wx-1");
		assertThat(published.get("delivery_items")).asList().hasSize(1);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> tradeUpdateCaptor = ArgumentCaptor.forClass(Map.class);
		verify(thirdPartyTradeUpdateDispatchPublisher, times(1)).publish(tradeUpdateCaptor.capture());
		Map<String, Object> tradeUpdate = tradeUpdateCaptor.getValue();
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
