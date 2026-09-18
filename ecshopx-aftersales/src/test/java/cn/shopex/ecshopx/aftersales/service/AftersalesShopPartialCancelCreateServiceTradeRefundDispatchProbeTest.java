package cn.shopex.ecshopx.aftersales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.jushuitan.JushuitanTradeAftersalesBusPayloadBuilder;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.aftersales.port.AftersalesRefundAsyncPort;
import cn.shopex.ecshopx.aftersales.support.AftersalesRefundEntityTradeRefundPayloadMapper;
import cn.shopex.ecshopx.aftersales.wdterp.WdtErpTradeAfterSaleBusPayloadBuilder;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeRefundDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeAfterSaleDispatchPublisher;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderHeaderReadPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderItemsReadPort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.port.order.OrderSuccessTradeReadPort;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("门店部分取消创单：trade refund Bus publish after commit")
class AftersalesShopPartialCancelCreateServiceTradeRefundDispatchProbeTest {

	private static final long COMPANY_ID = 9103L;
	private static final long ORDER_ID = 5103L;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Aftersales.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), AftersalesRefund.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), AftersalesDetail.class);
	}

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clear();
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	@DisplayName(
			"Admin order cancel with delivery_status PARTAIL: partial cancel creates aftersales then afterCommit"
					+ " publishes trade refund (AdminNormalOrderPartialCancelService → createForShopPartialCancel)")
	void createForShopPartialCancel_afterCommit_invokesTradeRefundDispatchPublisherOnce_withExpectedKeys() {
		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		AftersalesRefundMapper aftersalesRefundMapper = mock(AftersalesRefundMapper.class);
		OrderSuccessTradeReadPort orderSuccessTradeReadPort = mock(OrderSuccessTradeReadPort.class);
		OrderNormalOrderItemsReadPort orderNormalOrderItemsReadPort = mock(OrderNormalOrderItemsReadPort.class);
		OrderNormalOrderHeaderReadPort orderNormalOrderHeaderReadPort = mock(OrderNormalOrderHeaderReadPort.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		AftersalesRefundAsyncPort aftersalesRefundAsyncPort = mock(AftersalesRefundAsyncPort.class);
		StringRedisTemplate sharedStringRedisTemplate = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
		when(sharedStringRedisTemplate.opsForHash()).thenReturn(hashOps);
		when(hashOps.increment(any(), any(), anyLong())).thenReturn(1L);
		JushuitanTradeAftersalesDispatchPublisher jushuitanTradeAftersalesDispatchPublisher =
				mock(JushuitanTradeAftersalesDispatchPublisher.class);
		WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher =
				mock(WdtErpTradeAfterSaleDispatchPublisher.class);
		TradeRefundDispatchPublisher tradeRefundDispatchPublisher = mock(TradeRefundDispatchPublisher.class);
		AftersalesRefundEntityTradeRefundPayloadMapper tradeRefundPayloadMapper =
				new AftersalesRefundEntityTradeRefundPayloadMapper();

		AtomicLong insertedBn = new AtomicLong();
		when(aftersalesMapper.insert(any(Aftersales.class)))
				.thenAnswer(
						inv -> {
							Aftersales m = inv.getArgument(0);
							insertedBn.set(m.getAftersalesBn());
							return 1;
						});
		when(aftersalesMapper.selectOne(any()))
				.thenAnswer(
						inv -> {
							long bn = insertedBn.get();
							if (bn == 0L) {
								return null;
							}
							Aftersales a = new Aftersales();
							a.setCompanyId(COMPANY_ID);
							a.setAftersalesBn(bn);
							a.setOrderId(ORDER_ID);
							a.setDistributorId(0L);
							a.setAftersalesType("ONLY_REFUND");
							return a;
						});

		when(aftersalesDetailMapper.insert(any(AftersalesDetail.class))).thenReturn(1);
		AtomicReference<AftersalesRefund> refundSnap = new AtomicReference<>();
		when(aftersalesRefundMapper.insert(any(AftersalesRefund.class)))
				.thenAnswer(
						inv -> {
							refundSnap.set(copyAftersalesRefund(inv.getArgument(0)));
							return 1;
						});
		when(aftersalesRefundMapper.selectOne(any())).thenAnswer(inv -> refundSnap.get());

		Map<String, Object> trade = new LinkedHashMap<>();
		trade.put("trade_id", "TR-PR");
		trade.put("pay_type", "wxpay");
		trade.put("cur_fee_rate", 1.0);
		trade.put("fee_type", "CNY");
		trade.put("cur_fee_type", "CNY");
		trade.put("cur_fee_symbol", "\u00a5");
		trade.put("merchant_id", 0L);
		when(orderSuccessTradeReadPort.primarySuccessTrade(eq(COMPANY_ID), eq(ORDER_ID)))
				.thenReturn(Optional.of(trade));

		Map<String, Object> head = new LinkedHashMap<>();
		head.put("shop_id", 11L);
		head.put("distributor_id", 0L);
		head.put("merchant_id", 0L);
		head.put("pay_type", "wxpay");
		head.put("freight_type", "cash");
		when(orderNormalOrderHeaderReadPort.getHeader(eq(COMPANY_ID), eq(ORDER_ID))).thenReturn(Optional.of(head));

		Map<String, Object> line = new LinkedHashMap<>();
		line.put("id", 601L);
		line.put("num", 5);
		line.put("total_fee", 500);
		line.put("point", 0);
		line.put("supplier_id", 0L);
		line.put("distributor_id", 0L);
		line.put("goods_id", 1L);
		line.put("item_id", 10L);
		line.put("item_bn", "SKU-PR");
		line.put("item_name", "item");
		line.put("order_item_type", "normal");
		line.put("pic", "");
		when(orderNormalOrderItemsReadPort.listItems(eq(COMPANY_ID), eq(ORDER_ID))).thenReturn(List.of(line));

		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(any()))
				.thenAnswer(
						inv -> {
							if (!TransactionSynchronizationManager.isSynchronizationActive()) {
								TransactionSynchronizationManager.initSynchronization();
							}
							return new SimpleTransactionStatus(true);
						});
		doAnswer(
						inv -> {
							if (TransactionSynchronizationManager.isSynchronizationActive()) {
								for (TransactionSynchronization synchronization :
										TransactionSynchronizationManager.getSynchronizations()) {
									synchronization.afterCommit();
								}
								TransactionSynchronizationManager.clear();
							}
							return null;
						})
				.when(txMgr)
				.commit(any());

		JushuitanTradeAftersalesBusPayloadBuilder jushuitanTradeAftersalesBusPayloadBuilder =
				new JushuitanTradeAftersalesBusPayloadBuilder();
		WdtErpTradeAfterSaleBusPayloadBuilder wdtErpTradeAfterSaleBusPayloadBuilder =
				new WdtErpTradeAfterSaleBusPayloadBuilder();

		AftersalesShopPartialCancelCreateService service =
				new AftersalesShopPartialCancelCreateService(
						aftersalesMapper,
						aftersalesDetailMapper,
						aftersalesRefundMapper,
						orderSuccessTradeReadPort,
						orderNormalOrderItemsReadPort,
						orderNormalOrderHeaderReadPort,
						orderProcessLogPublishPort,
						aftersalesRefundAsyncPort,
						sharedStringRedisTemplate,
						jushuitanTradeAftersalesDispatchPublisher,
						jushuitanTradeAftersalesBusPayloadBuilder,
						wdtErpTradeAfterSaleDispatchPublisher,
						wdtErpTradeAfterSaleBusPayloadBuilder,
						tradeRefundDispatchPublisher,
						tradeRefundPayloadMapper);

		TransactionTemplate requiresNew = new TransactionTemplate(txMgr);
		requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		Map<String, Object> detailRow = new LinkedHashMap<>();
		detailRow.put("id", 601L);
		detailRow.put("num", 2);

		requiresNew.executeWithoutResult(
				st ->
						service.createForShopPartialCancel(
								COMPANY_ID,
								ORDER_ID,
								3300L,
								0L,
								"admin",
								99L,
								"partial cancel probe",
								List.of(detailRow)));

		ArgumentCaptor<Map<String, Object>> captor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		verify(tradeRefundDispatchPublisher, times(1)).publish(captor.capture());
		Map<String, Object> published = captor.getValue();

		assertThat(published.get("company_id")).isEqualTo(COMPANY_ID);
		assertThat(published.get("order_id")).isEqualTo(ORDER_ID);
		assertThat(((Number) published.get("aftersales_bn")).longValue()).isEqualTo(insertedBn.get());

		assertThat(published.get("refund_bn")).isNotNull();
		Object refundBnExpected = refundSnap.get() != null ? refundSnap.get().getRefundBn() : null;
		assertThat(refundBnExpected).isNotNull();
		assertThat(((Number) published.get("refund_bn")).longValue()).isEqualTo(((Number) refundBnExpected).longValue());
		verify(jushuitanTradeAftersalesDispatchPublisher, times(0)).publish(any());
		verify(wdtErpTradeAfterSaleDispatchPublisher, times(1)).publish(any());
	}

	@Test
	@SuppressWarnings("unchecked")
	void createForShopPartialCancel_whenRefundRowMissingAfterCommit_doesNotPublish() {
		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		AftersalesRefundMapper aftersalesRefundMapper = mock(AftersalesRefundMapper.class);
		OrderSuccessTradeReadPort orderSuccessTradeReadPort = mock(OrderSuccessTradeReadPort.class);
		OrderNormalOrderItemsReadPort orderNormalOrderItemsReadPort = mock(OrderNormalOrderItemsReadPort.class);
		OrderNormalOrderHeaderReadPort orderNormalOrderHeaderReadPort = mock(OrderNormalOrderHeaderReadPort.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		AftersalesRefundAsyncPort aftersalesRefundAsyncPort = mock(AftersalesRefundAsyncPort.class);
		StringRedisTemplate sharedStringRedisTemplate = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
		when(sharedStringRedisTemplate.opsForHash()).thenReturn(hashOps);
		when(hashOps.increment(any(), any(), anyLong())).thenReturn(1L);
		JushuitanTradeAftersalesDispatchPublisher jushuitanTradeAftersalesDispatchPublisher =
				mock(JushuitanTradeAftersalesDispatchPublisher.class);
		WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher =
				mock(WdtErpTradeAfterSaleDispatchPublisher.class);
		TradeRefundDispatchPublisher tradeRefundDispatchPublisher = mock(TradeRefundDispatchPublisher.class);
		AftersalesRefundEntityTradeRefundPayloadMapper tradeRefundPayloadMapper =
				new AftersalesRefundEntityTradeRefundPayloadMapper();

		AtomicLong insertedBn = new AtomicLong();
		when(aftersalesMapper.insert(any(Aftersales.class)))
				.thenAnswer(
						inv -> {
							Aftersales m = inv.getArgument(0);
							insertedBn.set(m.getAftersalesBn());
							return 1;
						});
		when(aftersalesMapper.selectOne(any()))
				.thenAnswer(
						inv -> {
							long bn = insertedBn.get();
							if (bn == 0L) {
								return null;
							}
							Aftersales a = new Aftersales();
							a.setCompanyId(COMPANY_ID);
							a.setAftersalesBn(bn);
							a.setOrderId(ORDER_ID);
							a.setDistributorId(0L);
							a.setAftersalesType("ONLY_REFUND");
							return a;
						});
		when(aftersalesDetailMapper.insert(any(AftersalesDetail.class))).thenReturn(1);
		when(aftersalesRefundMapper.insert(any(AftersalesRefund.class))).thenReturn(1);
		when(aftersalesRefundMapper.selectOne(any())).thenReturn(null);

		Map<String, Object> trade = new LinkedHashMap<>();
		trade.put("trade_id", "TR-X");
		trade.put("pay_type", "wxpay");
		trade.put("cur_fee_rate", 1.0);
		trade.put("fee_type", "CNY");
		trade.put("cur_fee_type", "CNY");
		trade.put("cur_fee_symbol", "\u00a5");
		trade.put("merchant_id", 0L);
		when(orderSuccessTradeReadPort.primarySuccessTrade(eq(COMPANY_ID), eq(ORDER_ID)))
				.thenReturn(Optional.of(trade));

		Map<String, Object> head = new LinkedHashMap<>();
		head.put("shop_id", 11L);
		head.put("distributor_id", 0L);
		head.put("merchant_id", 0L);
		head.put("pay_type", "wxpay");
		head.put("freight_type", "cash");
		when(orderNormalOrderHeaderReadPort.getHeader(eq(COMPANY_ID), eq(ORDER_ID))).thenReturn(Optional.of(head));

		Map<String, Object> line = new LinkedHashMap<>();
		line.put("id", 601L);
		line.put("num", 5);
		line.put("total_fee", 500);
		line.put("point", 0);
		line.put("supplier_id", 0L);
		line.put("distributor_id", 0L);
		line.put("goods_id", 1L);
		line.put("item_id", 10L);
		line.put("item_bn", "SKU");
		line.put("item_name", "item");
		line.put("order_item_type", "normal");
		line.put("pic", "");
		when(orderNormalOrderItemsReadPort.listItems(eq(COMPANY_ID), eq(ORDER_ID))).thenReturn(List.of(line));

		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(any()))
				.thenAnswer(
						inv -> {
							if (!TransactionSynchronizationManager.isSynchronizationActive()) {
								TransactionSynchronizationManager.initSynchronization();
							}
							return new SimpleTransactionStatus(true);
						});
		doAnswer(
						inv -> {
							if (TransactionSynchronizationManager.isSynchronizationActive()) {
								for (TransactionSynchronization synchronization :
										TransactionSynchronizationManager.getSynchronizations()) {
									synchronization.afterCommit();
								}
								TransactionSynchronizationManager.clear();
							}
							return null;
						})
				.when(txMgr)
				.commit(any());

		AftersalesShopPartialCancelCreateService service =
				new AftersalesShopPartialCancelCreateService(
						aftersalesMapper,
						aftersalesDetailMapper,
						aftersalesRefundMapper,
						orderSuccessTradeReadPort,
						orderNormalOrderItemsReadPort,
						orderNormalOrderHeaderReadPort,
						orderProcessLogPublishPort,
						aftersalesRefundAsyncPort,
						sharedStringRedisTemplate,
						jushuitanTradeAftersalesDispatchPublisher,
						new JushuitanTradeAftersalesBusPayloadBuilder(),
						wdtErpTradeAfterSaleDispatchPublisher,
						new WdtErpTradeAfterSaleBusPayloadBuilder(),
						tradeRefundDispatchPublisher,
						tradeRefundPayloadMapper);

		TransactionTemplate requiresNew = new TransactionTemplate(txMgr);
		requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		Map<String, Object> detailRow = new LinkedHashMap<>();
		detailRow.put("id", 601L);
		detailRow.put("num", 2);
		requiresNew.executeWithoutResult(
				st ->
						service.createForShopPartialCancel(
								COMPANY_ID,
								ORDER_ID,
								3300L,
								0L,
								"admin",
								99L,
								"probe",
								List.of(detailRow)));

		verify(tradeRefundDispatchPublisher, never()).publish(any());
	}

	private static AftersalesRefund copyAftersalesRefund(AftersalesRefund r) {
		AftersalesRefund c = new AftersalesRefund();
		c.setRefundBn(r.getRefundBn());
		c.setAftersalesBn(r.getAftersalesBn());
		c.setOrderId(r.getOrderId());
		c.setTradeId(r.getTradeId());
		c.setCompanyId(r.getCompanyId());
		c.setSupplierId(r.getSupplierId());
		c.setUserId(r.getUserId());
		c.setShopId(r.getShopId());
		c.setDistributorId(r.getDistributorId());
		c.setRefundType(r.getRefundType());
		c.setRefundChannel(r.getRefundChannel());
		c.setRefundStatus(r.getRefundStatus());
		c.setRefundFee(r.getRefundFee());
		c.setRefundPoint(r.getRefundPoint());
		c.setReturnFreight(r.getReturnFreight());
		c.setFreight(r.getFreight());
		c.setFreightType(r.getFreightType());
		c.setPayType(r.getPayType());
		c.setCurrency(r.getCurrency());
		c.setCurFeeType(r.getCurFeeType());
		c.setCurFeeRate(r.getCurFeeRate());
		c.setCurFeeSymbol(r.getCurFeeSymbol());
		c.setCurPayFee(r.getCurPayFee());
		c.setMerchantId(r.getMerchantId());
		c.setReturnPoint(r.getReturnPoint());
		c.setCreateTime(r.getCreateTime());
		c.setUpdateTime(r.getUpdateTime());
		return c;
	}
}
