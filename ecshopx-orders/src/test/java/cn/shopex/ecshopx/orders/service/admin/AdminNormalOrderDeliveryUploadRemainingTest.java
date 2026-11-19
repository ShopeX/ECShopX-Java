package cn.shopex.ecshopx.orders.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminNormalOrderDeliveryUploadRemainingTest {

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
	private final List<String> normalOrderUpdateSql = new ArrayList<>();

	@BeforeEach
	void setUp() {
		itemLineUpdateSql.clear();
		normalOrderUpdateSql.clear();
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.initSynchronization();
		}

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

		doAnswer(
						inv -> {
							Object w = inv.getArgument(1);
							if (w instanceof LambdaUpdateWrapper<?> uw) {
								normalOrderUpdateSql.add(
										String.valueOf(uw.getSqlSet()) + uw.getParamNameValuePairs());
							}
							return 1;
						})
				.when(normalOrdersMapper)
				.update(any(), any());

		Trade trade = new Trade();
		trade.setTradeId("tr-upload-1");
		when(tradeMapper.selectList(any())).thenReturn(List.of(trade));

		NormalOrders orderRow = baseOrder();
		when(normalOrdersMapper.selectOne(any())).thenReturn(orderRow);

		OrderAssociations assoc = new OrderAssociations();
		assoc.setCompanyId(COMPANY_ID);
		assoc.setOrderId(ORDER_ID);
		assoc.setUserId(55L);
		assoc.setOrderClass("normal");
		when(orderAssociationsMapper.selectOne(any())).thenReturn(assoc);
	}

	@AfterEach
	void tearDownSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void deliverUploadWholeRemaining_shipsOnlyRemain_andLeavesPartailWhenSupplierLinePending() {
		NormalOrdersItems selfLine = selfLine(11L, 5, 2, 1);
		NormalOrdersItems supplierLine = supplierLine(12L, 1, 0);
		when(normalOrdersItemsMapper.selectList(any()))
				.thenReturn(List.of(selfLine))
				.thenReturn(List.of(selfLineAfterPartialShip(), supplierLine));

		OrderAssociations assoc = new OrderAssociations();
		assoc.setCompanyId(COMPANY_ID);
		assoc.setOrderId(ORDER_ID);
		service.deliverUploadWholeRemaining(batchParams(), assoc);

		ArgumentCaptor<OrdersDeliveryItems> captor = ArgumentCaptor.forClass(OrdersDeliveryItems.class);
		verify(ordersDeliveryItemsMapper).insert(captor.capture());
		assertThat(captor.getValue().getOrderItemsId()).isEqualTo(11L);
		assertThat(captor.getValue().getNum()).isEqualTo(2);

		String itemUpdates = String.join("|", itemLineUpdateSql);
		assertThat(itemUpdates).contains("delivery_item_num");
		assertThat(itemUpdates).contains("=4}");
		assertThat(itemUpdates).contains("delivery_status");
		assertThat(itemUpdates).contains("DONE");

		String orderUpdates = String.join("|", normalOrderUpdateSql);
		assertThat(orderUpdates).contains("PARTAIL");
		assertThat(orderUpdates).doesNotContain("WAIT_BUYER_CONFIRM");
		assertThat(orderUpdates)
				.contains("left_aftersales_num=#{ew.paramNameValuePairs.MPGENVAL1}");
		assertThat(orderUpdates).containsPattern("MPGENVAL1=2[,}]");
	}

	@Test
	void deliverUploadWholeRemaining_whenAllSelfLinesShipped_marksDoneAndWaitBuyerConfirm() {
		NormalOrdersItems lineA = selfLine(11L, 5, 2, 1);
		NormalOrdersItems lineB = selfLine(13L, 3, 1, 0);
		when(normalOrdersItemsMapper.selectList(any()))
				.thenReturn(List.of(lineA, lineB))
				.thenReturn(List.of(fullySettled(lineA, 5, 1), fullySettled(lineB, 3, 0)));

		service.deliverUploadWholeRemaining(batchParams(), null);

		String orderUpdates = String.join("|", normalOrderUpdateSql);
		assertThat(orderUpdates).contains("DONE");
		assertThat(orderUpdates).contains("WAIT_BUYER_CONFIRM");
	}

	@Test
	void batchWithoutUploadRemaining_shipsFullPurchaseQty() {
		NormalOrdersItems selfLine = selfLine(11L, 5, 2, 1);
		when(normalOrdersItemsMapper.selectList(any()))
				.thenReturn(List.of(selfLine))
				.thenReturn(List.of(fullySettled(selfLine, 5, 1)));

		service.deliveryNormalPhysical(batchParams(), null, "normal");

		ArgumentCaptor<OrdersDeliveryItems> captor = ArgumentCaptor.forClass(OrdersDeliveryItems.class);
		verify(ordersDeliveryItemsMapper).insert(captor.capture());
		assertThat(captor.getValue().getNum()).isEqualTo(5);

		String itemUpdates = String.join("|", itemLineUpdateSql);
		assertThat(itemUpdates).contains("=5}");
	}

	@Test
	void deliverUploadWholeRemaining_whenNoRemain_throwsNoShippableGoods() {
		NormalOrders order = baseOrder();
		order.setDeliveryStatus("PENDING");
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);

		NormalOrdersItems settled = selfLine(11L, 5, 4, 1);
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(settled));

		assertThatThrownBy(() -> service.deliverUploadWholeRemaining(batchParams(), null))
				.isInstanceOf(ResourceException.class)
				.hasMessage("没有可发货的商品");
		verify(ordersDeliveryMapper, never()).insert(any(OrdersDelivery.class));
	}

	@Test
	void deliverUploadWholeRemaining_whenOnlySupplierLines_throwsSupplierMustShip() {
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(normalOrdersItemsMapper.selectCount(any())).thenReturn(1L);

		assertThatThrownBy(() -> service.deliverUploadWholeRemaining(batchParams(), null))
				.isInstanceOf(ResourceException.class)
				.hasMessage("供应商商品请由供应商发货");
		verify(ordersDeliveryMapper, never()).insert(any(OrdersDelivery.class));
	}

	private static Map<String, Object> batchParams() {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("company_id", COMPANY_ID);
		params.put("order_id", ORDER_ID);
		params.put("supplier_id", 0);
		params.put("delivery_type", "batch");
		params.put("delivery_corp", "SF");
		params.put("delivery_code", "SF001");
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

	private static NormalOrdersItems selfLine(long id, int num, int sent, int cancelled) {
		NormalOrdersItems line = new NormalOrdersItems();
		line.setId(id);
		line.setCompanyId(COMPANY_ID);
		line.setOrderId(ORDER_ID);
		line.setNum(num);
		line.setDeliveryItemNum(sent);
		line.setCancelItemNum(cancelled);
		line.setDeliveryStatus("PENDING");
		line.setSupplierId(0);
		line.setGoodsId(1L);
		line.setItemId(2L);
		line.setItemName("self-sku-" + id);
		return line;
	}

	private static NormalOrdersItems supplierLine(long id, int num, int sent) {
		NormalOrdersItems line = selfLine(id, num, sent, 0);
		line.setSupplierId(18);
		line.setItemName("supplier-sku-" + id);
		return line;
	}

	private static NormalOrdersItems selfLineAfterPartialShip() {
		return fullySettled(selfLine(11L, 5, 2, 1), 5, 1);
	}

	private static NormalOrdersItems fullySettled(NormalOrdersItems template, int num, int cancelled) {
		NormalOrdersItems after = new NormalOrdersItems();
		after.setId(template.getId());
		after.setCompanyId(template.getCompanyId());
		after.setOrderId(template.getOrderId());
		after.setNum(num);
		after.setDeliveryItemNum(num - cancelled);
		after.setCancelItemNum(cancelled);
		after.setSupplierId(template.getSupplierId());
		after.setDeliveryStatus("DONE");
		return after;
	}
}
