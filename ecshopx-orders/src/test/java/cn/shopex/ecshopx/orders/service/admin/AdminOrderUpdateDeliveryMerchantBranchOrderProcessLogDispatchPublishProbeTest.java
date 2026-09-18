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
import cn.shopex.ecshopx.orders.domain.OrdersDelivery;
import cn.shopex.ecshopx.orders.mapper.CompanyRelLogisticsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrdersDeliveryMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("EVENT_ORDER_PROCESS_LOG: admin updateDelivery merchant branch — publishEvent probe")
class AdminOrderUpdateDeliveryMerchantBranchOrderProcessLogDispatchPublishProbeTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OrdersDelivery.class);
	}

	@Mock
	private OrdersDeliveryMapper ordersDeliveryMapper;

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private CompanyRelLogisticsMapper companyRelLogisticsMapper;

	@Mock
	private StringRedisTemplate stringRedisTemplate;

	@Mock
	private AdminSelfDeliveryStaffFeeService adminSelfDeliveryStaffFeeService;

	@Captor
	private ArgumentCaptor<Map<String, Object>> payloadCaptor;

	private DispatchFacade dispatchFacade;
	private OrderProcessLogPublishPort orderProcessLogPublishPort;
	private AdminOrderUpdateDeliveryService service;

	@BeforeEach
	void setUp() {
		dispatchFacade = mock(DispatchFacade.class);
		orderProcessLogPublishPort = new OrderProcessLogPublishPortImpl(dispatchFacade);

		service =
				new AdminOrderUpdateDeliveryService(
						ordersDeliveryMapper,
						normalOrdersMapper,
						companyRelLogisticsMapper,
						stringRedisTemplate,
						adminSelfDeliveryStaffFeeService,
						orderProcessLogPublishPort);

		OrdersDelivery delivery = new OrdersDelivery();
		delivery.setOrdersDeliveryId(888L);
		delivery.setCompanyId(100L);
		delivery.setOrderId(200L);
		when(ordersDeliveryMapper.selectOne(any())).thenReturn(delivery);

		NormalOrders order = new NormalOrders();
		order.setOrderId(200L);
		order.setCompanyId(100L);
		order.setReceiptType("merchant");
		order.setSelfDeliveryFee(100);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(normalOrdersMapper.update(any(), any())).thenReturn(1);
	}

	@Test
	void updateMerchantBranch_afterCommit_invokesPublishEventOnce_withMerchantUpdateDeliveryShapedPayload() {
		TransactionTemplate tt = new TransactionTemplate(syncFiringTxManager());

		Map<String, Object> params = new LinkedHashMap<>();
		params.put("self_delivery_status", "PACKAGED");
		params.put("delivery_remark", "轻放");
		params.put("delivery_pics", List.of("http://example.com/p1.png"));

		tt.executeWithoutResult(status -> service.updateDelivery(100L, 42L, "888", params));

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
		assertThat(payload.get("is_show")).isEqualTo(true);
		assertThat(payload.get("remarks")).isEqualTo("已打包");
		assertThat(payload.get("detail")).isEqualTo("订单号：200，订单发货信息修改");
		assertThat(payload.get("delivery_remark")).isEqualTo("轻放");
		assertThat(payload.get("pics")).isEqualTo(List.of("http://example.com/p1.png"));

		@SuppressWarnings("unchecked")
		Map<String, Object> inner = (Map<String, Object>) payload.get("params");
		assertThat(inner.get("orders_delivery_id")).isEqualTo("888");
		assertThat(inner.get("company_id")).isEqualTo(100L);
		assertThat(inner.get("operator_type")).isEqualTo("admin");
		assertThat(inner.get("operator_id")).isEqualTo(42L);
		assertThat(inner.get("self_delivery_status")).isEqualTo("PACKAGED");
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
