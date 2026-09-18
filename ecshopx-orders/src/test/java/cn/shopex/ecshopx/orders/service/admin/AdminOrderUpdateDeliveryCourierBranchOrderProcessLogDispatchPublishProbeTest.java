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
import cn.shopex.ecshopx.orders.domain.CompanyRelLogistics;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrdersDelivery;
import cn.shopex.ecshopx.orders.mapper.CompanyRelLogisticsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrdersDeliveryMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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

/**
 * Courier-branch publish probe; {@code updateDelivery} here uses a non-zero operator id only as a Mockito stub.
 * Production Wxapp {@link cn.shopex.ecshopx.orders.api.front.v1.WxappOrderController#updateDelivery} passes
 * {@code 0L} for the operator id argument.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EVENT_ORDER_PROCESS_LOG: admin updateDelivery courier branch — publishEvent probe")
class AdminOrderUpdateDeliveryCourierBranchOrderProcessLogDispatchPublishProbeTest {

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

		OrdersDelivery refreshed = new OrdersDelivery();
		refreshed.setOrdersDeliveryId(888L);
		refreshed.setCompanyId(100L);
		refreshed.setOrderId(200L);
		refreshed.setDeliveryCorp("SF");
		refreshed.setDeliveryCode("123456789");
		refreshed.setDeliveryCorpName("顺丰速运");

		when(ordersDeliveryMapper.selectOne(any())).thenReturn(delivery, refreshed);
		when(ordersDeliveryMapper.update(any(), any())).thenReturn(1);

		NormalOrders order = new NormalOrders();
		order.setOrderId(200L);
		order.setCompanyId(100L);
		order.setReceiptType("express");
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);

		CompanyRelLogistics rel = new CompanyRelLogistics();
		rel.setCorpName("顺丰速运");
		rel.setCorpCode("SF");
		when(companyRelLogisticsMapper.selectOne(any())).thenReturn(rel);

		@SuppressWarnings("unchecked")
		ValueOperations<String, String> ops = mock(ValueOperations.class);
		when(stringRedisTemplate.opsForValue()).thenReturn(ops);
		when(ops.get(any())).thenReturn(null);
	}

	@Test
	void updateCourierBranch_afterCommit_invokesPublishEventOnce_withCourierUpdateDeliveryShapedPayload() {
		TransactionTemplate tt = new TransactionTemplate(syncFiringTxManager());

		Map<String, Object> params = new LinkedHashMap<>();
		params.put("delivery_corp", "SF");
		params.put("delivery_code", "123456789");

		tt.executeWithoutResult(status -> service.updateDelivery(100L, 42L, "888", params));

		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));

		Map<String, Object> payload = payloadCaptor.getValue();
		assertThat(payload).doesNotContainKeys("is_show", "delivery_remark", "pics");
		assertThat(payload.get("order_id")).isEqualTo(200L);
		assertThat(payload.get("company_id")).isEqualTo(100L);
		assertThat(payload.get("operator_type")).isEqualTo("admin");
		assertThat(payload.get("operator_id")).isEqualTo(42L);
		assertThat(payload.get("remarks")).isEqualTo("订单发货");
		assertThat(payload.get("detail")).isEqualTo("订单号：200，订单发货信息修改");

		@SuppressWarnings("unchecked")
		Map<String, Object> inner = (Map<String, Object>) payload.get("params");
		assertThat(inner.get("orders_delivery_id")).isEqualTo("888");
		assertThat(inner.get("company_id")).isEqualTo(100L);
		assertThat(inner.get("operator_type")).isEqualTo("admin");
		assertThat(inner.get("operator_id")).isEqualTo(42L);
		assertThat(inner.get("delivery_corp")).isEqualTo("SF");
		assertThat(inner.get("delivery_code")).isEqualTo("123456789");
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
