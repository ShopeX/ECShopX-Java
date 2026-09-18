package cn.shopex.ecshopx.aftersales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
import cn.shopex.ecshopx.common.exception.ResourceException;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
class AftersalesShopPartialCancelAutoApproveOnlyRefundPostCommitJobsProbeTest {

	private static final long COMPANY_ID = 9001L;
	private static final long ORDER_ID = 5001L;
	private static final long AFTERSALES_BN = 202601011234567L;

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
	void autoApproveOnlyRefund_afterCommit_publishesJushuitanTradeAftersalesExactlyOnce_usingBusPayloadBuilder() {
		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		AftersalesRefundMapper aftersalesRefundMapper = mock(AftersalesRefundMapper.class);
		OrderSuccessTradeReadPort orderSuccessTradeReadPort = mock(OrderSuccessTradeReadPort.class);
		OrderNormalOrderItemsReadPort orderNormalOrderItemsReadPort = mock(OrderNormalOrderItemsReadPort.class);
		OrderNormalOrderHeaderReadPort orderNormalOrderHeaderReadPort = mock(OrderNormalOrderHeaderReadPort.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		AftersalesRefundAsyncPort aftersalesRefundAsyncPort = mock(AftersalesRefundAsyncPort.class);
		StringRedisTemplate sharedStringRedisTemplate = mock(StringRedisTemplate.class);
		JushuitanTradeAftersalesDispatchPublisher jushuitanTradeAftersalesDispatchPublisher =
				mock(JushuitanTradeAftersalesDispatchPublisher.class);
		WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher =
				mock(WdtErpTradeAfterSaleDispatchPublisher.class);
		TradeRefundDispatchPublisher tradeRefundDispatchPublisher = mock(TradeRefundDispatchPublisher.class);
		AftersalesRefundEntityTradeRefundPayloadMapper tradeRefundPayloadMapper =
				new AftersalesRefundEntityTradeRefundPayloadMapper();

		Aftersales row = new Aftersales();
		row.setCompanyId(COMPANY_ID);
		row.setAftersalesBn(AFTERSALES_BN);
		row.setOrderId(ORDER_ID);
		row.setAftersalesType("ONLY_REFUND");
		row.setAftersalesStatus(0);
		row.setProgress(0);
		row.setRefundFee(100);
		row.setRefundPoint(0);
		row.setFreight(0);

		AtomicInteger aftersalesReloadSeq = new AtomicInteger();
		when(aftersalesMapper.selectOne(any()))
				.thenAnswer(
						inv -> {
							if (aftersalesReloadSeq.incrementAndGet() == 1) {
								return row;
							}
							Aftersales reloaded = new Aftersales();
							reloaded.setCompanyId(row.getCompanyId());
							reloaded.setAftersalesBn(row.getAftersalesBn());
							reloaded.setOrderId(row.getOrderId());
							reloaded.setAftersalesType(row.getAftersalesType());
							reloaded.setShopId(1L);
							reloaded.setDistributorId(0L);
							reloaded.setSupplierId(0);
							reloaded.setUserId(3300L);
							reloaded.setProgress(9);
							reloaded.setAftersalesStatus(1);
							reloaded.setRefundFee(row.getRefundFee());
							reloaded.setRefundPoint(row.getRefundPoint());
							reloaded.setFreight(0);
							reloaded.setMerchantId(0L);
							reloaded.setReason("snap");
							reloaded.setIsPartialCancel(true);
							return reloaded;
						});
		when(aftersalesRefundMapper.update(any(), any())).thenReturn(1);
		when(aftersalesMapper.update(any(), any())).thenReturn(1);
		when(aftersalesDetailMapper.update(any(), any())).thenReturn(1);

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

		TransactionTemplate transactionTemplate = new TransactionTemplate(txMgr);
		transactionTemplate.executeWithoutResult(
				status ->
						service.autoApproveOnlyRefund(
								COMPANY_ID, AFTERSALES_BN, 100, 0, 0, "admin", 700L, 3001L));

		InOrder inOrder = inOrder(aftersalesRefundAsyncPort);
		inOrder.verify(aftersalesRefundAsyncPort, times(1)).scheduleOrderRefundComplete(eq(COMPANY_ID), eq(ORDER_ID));
		ArgumentCaptor<Map<String, Object>> invoiceRedCaptor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		inOrder.verify(aftersalesRefundAsyncPort, times(1)).scheduleInvoiceRed(invoiceRedCaptor.capture());

		Map<String, Object> captured = invoiceRedCaptor.getValue();
		assertThat(captured.get("company_id")).isEqualTo(COMPANY_ID);
		assertThat(captured.get("order_id")).isEqualTo(ORDER_ID);
		assertThat(captured.get("aftersales_bn")).isEqualTo(AFTERSALES_BN);
		assertThat(captured.get("aftersales_status")).isEqualTo(1);
		assertThat(captured.get("progress")).isEqualTo(9);
		assertThat(captured.get("refund_fee")).isEqualTo(100);

		ArgumentCaptor<Map<String, Object>> jushuitanCaptor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		verify(jushuitanTradeAftersalesDispatchPublisher, times(1)).publish(jushuitanCaptor.capture());
		assertThat(jushuitanCaptor.getValue().get("aftersales_bn")).isEqualTo(AFTERSALES_BN);
		assertThat(jushuitanCaptor.getValue().get("company_id")).isEqualTo(COMPANY_ID);
		assertThat(jushuitanCaptor.getValue().get("order_id")).isEqualTo(ORDER_ID);
		verify(wdtErpTradeAfterSaleDispatchPublisher, never()).publish(any());
		verify(tradeRefundDispatchPublisher, never()).publish(any());

		ArgumentCaptor<Map<String, Object>> orderLogCaptor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		verify(orderProcessLogPublishPort, times(1)).publish(orderLogCaptor.capture());
		Map<String, Object> logPayload = orderLogCaptor.getValue();
		assertThat(logPayload.get("detail"))
				.isEqualTo("售后单号：" + AFTERSALES_BN + "，同意退款");
		assertThat(logPayload.get("remarks")).isEqualTo("订单售后");
		assertThat(logPayload.get("order_id")).isEqualTo(ORDER_ID);
		assertThat(logPayload.get("company_id")).isEqualTo(COMPANY_ID);
	}

	@Test
	void createForShopPartialCancel_afterInnerTxCommit_neverPublishesJushuitanTradeAftersales() {
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
		when(hashOps.increment(anyString(), anyString(), anyLong())).thenReturn(1L);
		JushuitanTradeAftersalesDispatchPublisher jushuitanTradeAftersalesDispatchPublisher =
				mock(JushuitanTradeAftersalesDispatchPublisher.class);
		WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher =
				mock(WdtErpTradeAfterSaleDispatchPublisher.class);
		TradeRefundDispatchPublisher tradeRefundDispatchPublisherForCreate = mock(TradeRefundDispatchPublisher.class);
		AftersalesRefundEntityTradeRefundPayloadMapper tradeRefundPayloadMapperForCreate =
				new AftersalesRefundEntityTradeRefundPayloadMapper();

		Map<String, Object> trade = new LinkedHashMap<>();
		trade.put("trade_id", "T1");
		trade.put("pay_type", "wxpay");
		trade.put("cur_fee_rate", 1.0);
		trade.put("fee_type", "CNY");
		trade.put("cur_fee_type", "CNY");
		trade.put("cur_fee_symbol", "\u00a5");
		trade.put("merchant_id", 0L);
		when(orderSuccessTradeReadPort.primarySuccessTrade(eq(COMPANY_ID), eq(ORDER_ID)))
				.thenReturn(Optional.of(trade));

		Map<String, Object> head = new LinkedHashMap<>();
		head.put("shop_id", 1L);
		head.put("distributor_id", 0L);
		head.put("merchant_id", 0L);
		head.put("pay_type", "wxpay");
		head.put("freight_type", "cash");
		when(orderNormalOrderHeaderReadPort.getHeader(eq(COMPANY_ID), eq(ORDER_ID))).thenReturn(Optional.of(head));

		Map<String, Object> line = new LinkedHashMap<>();
		line.put("id", 501L);
		line.put("num", 5);
		line.put("total_fee", 500);
		line.put("point", 0);
		line.put("supplier_id", 0L);
		line.put("distributor_id", 0L);
		line.put("goods_id", 1L);
		line.put("item_id", 10L);
		line.put("item_bn", "SKU1");
		line.put("item_name", "item");
		line.put("order_item_type", "normal");
		line.put("pic", "");
		when(orderNormalOrderItemsReadPort.listItems(eq(COMPANY_ID), eq(ORDER_ID))).thenReturn(List.of(line));

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
							a.setAftersalesStatus(0);
							a.setProgress(0);
							return a;
						});
		when(aftersalesDetailMapper.insert(any(AftersalesDetail.class))).thenReturn(1);
		AtomicReference<AftersalesRefund> refundSnapCreate = new AtomicReference<>();
		when(aftersalesRefundMapper.insert(any(AftersalesRefund.class)))
				.thenAnswer(
						inv -> {
							refundSnapCreate.set(copyAftersalesRefund(inv.getArgument(0)));
							return 1;
						});
		when(aftersalesRefundMapper.selectOne(any())).thenAnswer(inv -> refundSnapCreate.get());

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
						tradeRefundDispatchPublisherForCreate,
						tradeRefundPayloadMapperForCreate);

		TransactionTemplate requiresNew = new TransactionTemplate(txMgr);
		requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

		Map<String, Object> d0 = new LinkedHashMap<>();
		d0.put("id", 501L);
		d0.put("num", 3);
		requiresNew.executeWithoutResult(
				st ->
						service.createForShopPartialCancel(
								COMPANY_ID,
								ORDER_ID,
								3300L,
								0L,
								"admin",
								2L,
								"buyer regret",
								List.of(d0)));

		verify(jushuitanTradeAftersalesDispatchPublisher, never()).publish(any());
		verifyNoMoreInteractions(jushuitanTradeAftersalesDispatchPublisher);
		verify(wdtErpTradeAfterSaleDispatchPublisher, times(1)).publish(any());
		verify(tradeRefundDispatchPublisherForCreate, times(1)).publish(any());
	}

	@Test
	void autoApproveOnlyRefund_whenWrongType_doesNotSchedule() {
		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		AftersalesRefundMapper aftersalesRefundMapper = mock(AftersalesRefundMapper.class);
		OrderSuccessTradeReadPort orderSuccessTradeReadPort = mock(OrderSuccessTradeReadPort.class);
		OrderNormalOrderItemsReadPort orderNormalOrderItemsReadPort = mock(OrderNormalOrderItemsReadPort.class);
		OrderNormalOrderHeaderReadPort orderNormalOrderHeaderReadPort = mock(OrderNormalOrderHeaderReadPort.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		AftersalesRefundAsyncPort aftersalesRefundAsyncPort = mock(AftersalesRefundAsyncPort.class);
		StringRedisTemplate sharedStringRedisTemplate = mock(StringRedisTemplate.class);
		JushuitanTradeAftersalesDispatchPublisher jushuitanTradeAftersalesDispatchPublisher =
				mock(JushuitanTradeAftersalesDispatchPublisher.class);
		WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher =
				mock(WdtErpTradeAfterSaleDispatchPublisher.class);
		JushuitanTradeAftersalesBusPayloadBuilder jushuitanTradeAftersalesBusPayloadBuilder =
				new JushuitanTradeAftersalesBusPayloadBuilder();
		WdtErpTradeAfterSaleBusPayloadBuilder wdtErpTradeAfterSaleBusPayloadBuilder =
				new WdtErpTradeAfterSaleBusPayloadBuilder();
		TradeRefundDispatchPublisher tradeRefundDispatchPublisherReject = mock(TradeRefundDispatchPublisher.class);
		AftersalesRefundEntityTradeRefundPayloadMapper tradeRefundPayloadMapperReject =
				new AftersalesRefundEntityTradeRefundPayloadMapper();

		Aftersales row = new Aftersales();
		row.setCompanyId(COMPANY_ID);
		row.setAftersalesBn(AFTERSALES_BN);
		row.setOrderId(ORDER_ID);
		row.setAftersalesType("REFUND_GOODS");
		row.setAftersalesStatus(0);

		when(aftersalesMapper.selectOne(any())).thenReturn(row);

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
						tradeRefundDispatchPublisherReject,
						tradeRefundPayloadMapperReject);

		assertThatThrownBy(
						() ->
								service.autoApproveOnlyRefund(
										COMPANY_ID, AFTERSALES_BN, 0, 0, 0, "admin", 700L, 3001L))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("售后类型不支持自动审核");

		verify(aftersalesRefundAsyncPort, never()).scheduleOrderRefundComplete(anyLong(), anyLong());
		verify(aftersalesRefundAsyncPort, never()).scheduleInvoiceRed(any());
		verify(jushuitanTradeAftersalesDispatchPublisher, never()).publish(any());
		verify(wdtErpTradeAfterSaleDispatchPublisher, never()).publish(any());
		verify(tradeRefundDispatchPublisherReject, never()).publish(any());
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
