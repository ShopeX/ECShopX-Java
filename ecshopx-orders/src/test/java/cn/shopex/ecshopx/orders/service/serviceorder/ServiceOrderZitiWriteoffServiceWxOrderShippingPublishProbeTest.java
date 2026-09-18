package cn.shopex.ecshopx.orders.service.serviceorder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.WxOrderShippingDispatchPublisher;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.ServiceOrders;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.ServiceOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.SubOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import cn.shopex.ecshopx.orders.service.normal.MemberConsumptionOnNormalOrderFinishService;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderBrokerageOnFinishService;
import cn.shopex.ecshopx.orders.service.normal.OrdersRelChinaumspayDivisionWriteService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class ServiceOrderZitiWriteoffServiceWxOrderShippingPublishProbeTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, ServiceOrders.class);
		TableInfoHelper.initTableInfo(assistant, OrderAssociations.class);
		TableInfoHelper.initTableInfo(assistant, NormalOrders.class);
		TableInfoHelper.initTableInfo(assistant, Trade.class);
	}

	@Mock
	private ServiceOrdersMapper serviceOrdersMapper;

	@Mock
	private OrderAssociationsMapper orderAssociationsMapper;

	@Mock
	private SubOrdersMapper subOrdersMapper;

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private TradeMapper tradeMapper;

	@Mock
	private NormalOrderBrokerageOnFinishService normalOrderBrokerageOnFinishService;

	@Mock
	private MemberConsumptionOnNormalOrderFinishService memberConsumptionOnNormalOrderFinishService;

	@Mock
	private OrdersRelChinaumspayDivisionWriteService ordersRelChinaumspayDivisionWriteService;

	@Mock
	private ApplicationEventPublisher applicationEventPublisher;

	@Mock
	private WxOrderShippingDispatchPublisher wxOrderShippingDispatchPublisher;

	private ServiceOrderZitiWriteoffService service;

	@BeforeEach
	void setUp() {
		service =
				new ServiceOrderZitiWriteoffService(
						serviceOrdersMapper,
						orderAssociationsMapper,
						subOrdersMapper,
						normalOrdersMapper,
						tradeMapper,
						normalOrderBrokerageOnFinishService,
						memberConsumptionOnNormalOrderFinishService,
						ordersRelChinaumspayDivisionWriteService,
						applicationEventPublisher,
						wxOrderShippingDispatchPublisher);
	}

	@Test
	void orderZitiWriteoffForAdmin_publishesWxOrderShippingDispatchAfterCommit_withZitiPayload() {
		long companyId = 100L;
		long orderId = 500L;
		long operatorId = 42L;

		ServiceOrders svc = new ServiceOrders();
		svc.setCompanyId(companyId);
		svc.setOrderId(orderId);
		svc.setUserId(77L);
		when(serviceOrdersMapper.selectOne(any())).thenReturn(svc);
		when(serviceOrdersMapper.update(isNull(), any())).thenReturn(1);
		when(orderAssociationsMapper.update(isNull(), any())).thenReturn(1);
		when(subOrdersMapper.selectList(any())).thenReturn(Collections.emptyList());

		NormalOrders snap = new NormalOrders();
		snap.setCompanyId(companyId);
		snap.setOrderId(orderId);
		snap.setUserId(77L);
		snap.setPayType("wxpay");
		snap.setDistributorId(0L);
		when(normalOrdersMapper.selectOne(any())).thenReturn(snap);

		Trade trade = new Trade();
		trade.setTradeId("tr-svc-1");
		trade.setWxaAppid("wxa-svc");
		when(tradeMapper.selectList(any())).thenReturn(List.of(trade));

		OrderAssociations reloaded = new OrderAssociations();
		reloaded.setCompanyId(companyId);
		reloaded.setOrderId(orderId);
		when(orderAssociationsMapper.selectOne(any())).thenReturn(reloaded);

		TransactionTemplate tt = new TransactionTemplate(syncFiringTxManager());
		tt.executeWithoutResult(
				status -> {
					service.orderZitiWriteoffForAdmin(companyId, orderId, operatorId, false, "");
					verify(wxOrderShippingDispatchPublisher, never()).publish(any());
				});

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(wxOrderShippingDispatchPublisher, times(1)).publish(captor.capture());
		Map<String, Object> payload = captor.getValue();
		assertThat(payload.get("company_id")).isEqualTo(companyId);
		assertThat(payload.get("order_id")).isEqualTo(orderId);
		assertThat(payload.get("trade_id")).isEqualTo("tr-svc-1");
		assertThat(payload.get("user_id")).isEqualTo(77L);
		assertThat(payload.get("wxa_appid")).isEqualTo("wxa-svc");
		assertThat(payload.get("receipt_type")).isEqualTo("ziti");
		assertThat(payload.get("delivery_type")).isEqualTo("batch");
		assertThat(payload.get("is_all_delivered")).isEqualTo(Boolean.TRUE);
		assertThat(payload.get("delivery_corp")).isEqualTo("");
		assertThat(payload.get("delivery_code")).isEqualTo("");
		assertThat(payload.get("delivery_items")).asList().isEmpty();
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
