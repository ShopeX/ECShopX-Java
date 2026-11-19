package cn.shopex.ecshopx.orders.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.NormalOrderDeliveryDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeUpdateDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WxOrderShippingDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
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
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminNormalOrderDeliverySepCancelQtyTest {

	private static final long COMPANY_ID = 100L;
	private static final long ORDER_ID = 200L;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrdersItems.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OrderAssociations.class);
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

	@Spy
	private ObjectMapper objectMapper = new ObjectMapper();

	@Mock
	private WxOrderShippingDispatchPublisher wxOrderShippingDispatchPublisher;

	@Mock
	private ThirdPartyTradeUpdateDispatchPublisher thirdPartyTradeUpdateDispatchPublisher;

	@Mock
	private NormalOrderDeliveryDispatchPublisher normalOrderDeliveryDispatchPublisher;

	@InjectMocks
	private AdminNormalOrderDeliveryCoreService service;

	private final List<String> itemLineUpdateSql = new ArrayList<>();

	@BeforeEach
	void setUp() {
		itemLineUpdateSql.clear();

		ValueOperations<String, String> vo = mock(ValueOperations.class);
		when(stringRedisTemplate.opsForValue()).thenReturn(vo);
		when(vo.get(any())).thenReturn("");

		when(orderValiditySettingRedisReadService.readPlatformSetting(any(Long.class)))
				.thenReturn(Map.of("order_finish_time", 7));
		when(companyRelLogisticsMapper.selectOne(any())).thenReturn(null);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);
		when(orderAssociationsMapper.update(any(), any())).thenReturn(1);
		when(ordersDeliveryItemsMapper.insert(any(OrdersDeliveryItems.class))).thenReturn(1);
		doAnswer(
						inv -> {
							OrdersDelivery d = inv.getArgument(0);
							d.setOrdersDeliveryId(1L);
							return 1;
						})
				.when(ordersDeliveryMapper)
				.insert(any(OrdersDelivery.class));

		doAnswer(
						inv -> {
							Object w = inv.getArgument(1);
							if (w instanceof LambdaUpdateWrapper<?> uw) {
								itemLineUpdateSql.add(
										String.valueOf(uw.getSqlSet()) + uw.getParamNameValuePairs());
							}
							return 1;
						})
				.when(normalOrdersItemsMapper)
				.update(any(), any());

		Trade trade = new Trade();
		trade.setTradeId("tr-sep-1");
		when(tradeMapper.selectList(any())).thenReturn(List.of(trade));

		NormalOrders orderRow = baseOrder();
		when(normalOrdersMapper.selectOne(any())).thenReturn(orderRow);

		OrderAssociations assoc = new OrderAssociations();
		assoc.setCompanyId(COMPANY_ID);
		assoc.setOrderId(ORDER_ID);
		assoc.setUserId(55L);
		assoc.setOrderClass("normal");
		when(orderAssociationsMapper.selectOne(any())).thenReturn(assoc);

		NormalOrdersItems line = baseLineItem();
		when(normalOrdersItemsMapper.selectOne(any())).thenReturn(line);
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(shippedLineSnapshot(line)));
	}

	@Test
	void sepDelivery_countsCancelledInRemain_andMarksDoneWhenFullySettled() {
		TransactionTemplate tt = new TransactionTemplate(syncFiringTxManager());
		tt.executeWithoutResult(status -> service.deliveryNormalPhysical(sepParams("3"), null, "normal"));

		ArgumentCaptor<OrdersDeliveryItems> captor = ArgumentCaptor.forClass(OrdersDeliveryItems.class);
		verify(ordersDeliveryItemsMapper).insert(captor.capture());
		assertThat(captor.getValue().getNum()).isEqualTo(3);

		String allItemUpdates = String.join("|", itemLineUpdateSql);
		assertThat(allItemUpdates).contains("delivery_item_num");
		assertThat(allItemUpdates).contains("=4}");
		assertThat(allItemUpdates).contains("delivery_status");
		assertThat(allItemUpdates).contains("DONE");
	}

	@Test
	void sepDelivery_rejectsWhenDeliveryNumExceedsRemainAfterCancelled() {
		TransactionTemplate tt = new TransactionTemplate(syncFiringTxManager());
		assertThatThrownBy(() -> tt.executeWithoutResult(
						status -> service.deliveryNormalPhysical(sepParams("4"), null, "normal")))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("大于购买数量3");
	}

	private static Map<String, Object> sepParams(String deliveryNum) {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", COMPANY_ID);
		params.put("order_id", ORDER_ID);
		params.put("supplier_id", 0);
		params.put("delivery_type", "sep");
		params.put("delivery_corp", "SF");
		params.put("delivery_code", "SF001");
		params.put("sepInfo", "[{\"id\":11,\"delivery_num\":\"" + deliveryNum + "\"}]");
		return params;
	}

	private static NormalOrders baseOrder() {
		NormalOrders orderRow = new NormalOrders();
		orderRow.setOrderId(ORDER_ID);
		orderRow.setCompanyId(COMPANY_ID);
		orderRow.setReceiptType("normal");
		orderRow.setOrderStatus("PAYED");
		orderRow.setCancelStatus("NO_APPLY_CANCEL");
		orderRow.setDeliveryStatus("PENDING");
		orderRow.setUserId(55L);
		orderRow.setLeftAftersalesNum(0);
		return orderRow;
	}

	private static NormalOrdersItems baseLineItem() {
		NormalOrdersItems line = new NormalOrdersItems();
		line.setId(11L);
		line.setCompanyId(COMPANY_ID);
		line.setOrderId(ORDER_ID);
		line.setNum(5);
		line.setDeliveryItemNum(1);
		line.setCancelItemNum(1);
		line.setDeliveryStatus("PENDING");
		line.setSupplierId(0);
		line.setGoodsId(1L);
		line.setItemId(2L);
		line.setItemName("sku");
		return line;
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

	private static NormalOrdersItems shippedLineSnapshot(NormalOrdersItems before) {
		NormalOrdersItems after = new NormalOrdersItems();
		after.setId(before.getId());
		after.setCompanyId(before.getCompanyId());
		after.setOrderId(before.getOrderId());
		after.setNum(before.getNum());
		after.setDeliveryItemNum(4);
		after.setCancelItemNum(before.getCancelItemNum());
		after.setDeliveryStatus("DONE");
		return after;
	}
}
