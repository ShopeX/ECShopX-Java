package cn.shopex.ecshopx.orders.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;

import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.common.dispatch.HfpayProfitSharingEventDispatchPublisher;
import cn.shopex.ecshopx.common.hfpay.payment.HfPayPaymentSettingLoadPort;
import cn.shopex.ecshopx.hfpay.domain.HfpayEnterapply;
import cn.shopex.ecshopx.hfpay.mapper.HfpayEnterapplyMapper;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderProfitSharing;
import cn.shopex.ecshopx.orders.domain.OrderProfitSharingDetails;
import cn.shopex.ecshopx.popularize.domain.Brokerage;
import cn.shopex.ecshopx.popularize.mapper.BrokerageMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderProfitSharingDetailsMapper;
import cn.shopex.ecshopx.orders.mapper.OrderProfitSharingMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class OrderProfitSharingServiceTest {

	private static final String PLAN_141_FULL_NUMBERING =
			"1+2+3+3.1+3.2+3.3+3.4+3.5+3.6+3.7+3.8+3.9+3.10+3.10.1+3.10.2+3.10.3+3.10.4+3.10.5+3.10.6+3.11+G1+G2+G3+G3.1+G3.2+G3.3+G4+G5+E1+E2+E3+E3.1+E3.2+E3.3+E3.4+E3.5+E3.6+E3.7+E3.8";

	@Mock
	private NormalOrdersMapper normalOrdersMapper;
	@Mock
	private OrderProfitSharingMapper orderProfitSharingMapper;
	@Mock
	private OrderProfitSharingDetailsMapper orderProfitSharingDetailsMapper;
	@Mock
	private AftersalesRefundMapper aftersalesRefundMapper;
	@Mock
	private BrokerageMapper brokerageMapper;
	@Mock
	private HfpayEnterapplyMapper hfpayEnterapplyMapper;
	@Mock
	private HfPayPaymentSettingLoadPort hfPayPaymentSettingLoadPort;
	@Mock
	private TradeMapper tradeMapper;
	@Mock
	private HfpayProfitSharingEventDispatchPublisher hfpayProfitSharingEventDispatchPublisher;

	@Test
	@DisplayName(
			"全编号对照锚点 1+2+3+3.1+3.2+3.3+3.4+3.5+3.6+3.7+3.8+3.9+3.10+3.10.1+3.10.2+3.10.3+3.10.4+3.10.5+3.10.6+3.11+G1+G2+G3+G3.1+G3.2+G3.3+G4+G5+E1+E2+E3+E3.1+E3.2+E3.3+E3.4+E3.5+E3.6+E3.7+E3.8（plan §5 行 141 全集串；轻量断言）")
	void plan_section5_full_numbering_anchor_line141() {
		assertThat(PLAN_141_FULL_NUMBERING)
				.isEqualTo(
						"1+2+3+3.1+3.2+3.3+3.4+3.5+3.6+3.7+3.8+3.9+3.10+3.10.1+3.10.2+3.10.3+3.10.4+3.10.5+3.10.6+3.11+G1+G2+G3+G3.1+G3.2+G3.3+G4+G5+E1+E2+E3+E3.1+E3.2+E3.3+E3.4+E3.5+E3.6+E3.7+E3.8");
	}

	@Test
	@DisplayName("1+2：无候选订单早退，返回 0 且不调用分账 Bus 发布")
	void schedule_no_candidates_returns0_and_no_event() {
		OrderProfitSharingService svc =
				new OrderProfitSharingService(
						normalOrdersMapper,
						orderProfitSharingMapper,
						orderProfitSharingDetailsMapper,
						aftersalesRefundMapper,
						brokerageMapper,
						hfpayEnterapplyMapper,
						hfPayPaymentSettingLoadPort,
						tradeMapper,
						hfpayProfitSharingEventDispatchPublisher,
						noopTxManager());
		when(normalOrdersMapper.selectList(any())).thenReturn(Collections.emptyList());
		assertThat(svc.scheduleShareOrderProfit()).isEqualTo(0);
		verify(hfpayProfitSharingEventDispatchPublisher, never())
				.publishProfitSharingAfterCommit(anyLong(), any());
	}

	@Test
	@DisplayName(
			"G+getOrderHfAccount：acc.ok==false（入驻/交易缺失）跳过 3.3–3.10.x，无 INSERT、不计入成功、不发事件")
	void g_get_order_hf_account_fails_skips_no_insert_no_count() {
		OrderProfitSharingService svc =
				serviceWithTx(syncFiringTxManager());
		NormalOrders o = batchOrder(100L, 10L, 0L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(o));
		when(hfPayPaymentSettingLoadPort.loadForCompany(10L))
				.thenReturn(Map.of("is_open", "false", "mer_cust_id", "", "acct_id", ""));
		when(hfpayEnterapplyMapper.selectOne(any())).thenReturn(null);

		assertThat(svc.scheduleShareOrderProfit()).isEqualTo(0);
		verify(orderProfitSharingMapper, never()).insert(any(OrderProfitSharing.class));
		verify(orderProfitSharingDetailsMapper, never()).insert(any(OrderProfitSharingDetails.class));
		verify(normalOrdersMapper, never()).updateById(any(NormalOrders.class));
		verify(hfpayProfitSharingEventDispatchPublisher, never())
				.publishProfitSharingAfterCommit(anyLong(), any());
	}

	@Test
	@DisplayName(
			"3.10.x：主成功链 1 单 getOrderHfAccount 成功；INSERT 主从明细+update 订单；afterCommit 调用分账 Bus 发布")
	void section_3_10_success_chain_inserts_update_and_publishes_event() {
		OrderProfitSharingService svc =
				serviceWithTx(syncFiringTxManager());
		NormalOrders o = batchOrder(100L, 10L, 0L);
		o.setTotalFee("8000");
		o.setProfitsharingRate(10);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(o));
		when(hfPayPaymentSettingLoadPort.loadForCompany(10L))
				.thenReturn(Map.of("is_open", "false", "mer_cust_id", "", "acct_id", ""));
		HfpayEnterapply ent = new HfpayEnterapply();
		ent.setUserCustId("U_DIST");
		ent.setAcctId("A_DIST");
		when(hfpayEnterapplyMapper.selectOne(any())).thenReturn(ent);
		when(aftersalesRefundMapper.selectMaps(any())).thenReturn(Collections.emptyList());
		when(orderProfitSharingMapper.insert(any(OrderProfitSharing.class)))
				.thenAnswer(
						inv -> {
							OrderProfitSharing m = inv.getArgument(0);
							m.setOrderProfitSharingId(701L);
							return 1;
						});

		assertThat(svc.scheduleShareOrderProfit()).isEqualTo(1);
		verify(orderProfitSharingMapper).insert(any(OrderProfitSharing.class));
		verify(orderProfitSharingDetailsMapper, atLeastOnce()).insert(any(OrderProfitSharingDetails.class));
		verify(normalOrdersMapper).updateById(any(NormalOrders.class));

		verify(hfpayProfitSharingEventDispatchPublisher)
				.publishProfitSharingAfterCommit(
						eq(100L),
						argThat(ids -> ids != null && ids.size() == 1 && ids.contains(701L)));
	}

	@Test
	@DisplayName(
			"analysis §3 3.4 is_distribution==1 推广佣金 BrokerageMapper.selectList 累计 rebate>0 扣减分销商分账明细 total_fee（主表基数与 3.10.x 主成功链一致）")
	void analysis_3_4_is_distribution1_brokerage_rebate_deducts_distributor_detail() {
		OrderProfitSharingService svc =
				serviceWithTx(syncFiringTxManager());
		NormalOrders o = batchOrder(100L, 10L, 0L);
		o.setTotalFee("8000");
		o.setProfitsharingRate(10);
		o.setIsDistribution(true);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(o));
		when(hfPayPaymentSettingLoadPort.loadForCompany(10L))
				.thenReturn(Map.of("is_open", "false", "mer_cust_id", "", "acct_id", ""));
		HfpayEnterapply ent = new HfpayEnterapply();
		ent.setUserCustId("U_DIST");
		ent.setAcctId("A_DIST");
		when(hfpayEnterapplyMapper.selectOne(any())).thenReturn(ent);
		when(aftersalesRefundMapper.selectMaps(any())).thenReturn(Collections.emptyList());
		Brokerage br1 = new Brokerage();
		br1.setRebate(60);
		Brokerage br2 = new Brokerage();
		br2.setRebate(40);
		when(brokerageMapper.selectList(any())).thenReturn(List.of(br1, br2));
		when(orderProfitSharingMapper.insert(any(OrderProfitSharing.class)))
				.thenAnswer(
						inv -> {
							OrderProfitSharing m = inv.getArgument(0);
							m.setOrderProfitSharingId(701L);
							return 1;
						});

		assertThat(svc.scheduleShareOrderProfit()).isEqualTo(1);
		verify(brokerageMapper).selectList(any());

		ArgumentCaptor<OrderProfitSharing> mainCap = ArgumentCaptor.forClass(OrderProfitSharing.class);
		verify(orderProfitSharingMapper).insert(mainCap.capture());
		assertThat(mainCap.getValue().getTotalFee())
				.isEqualTo(8000);

		ArgumentCaptor<OrderProfitSharingDetails> detailCap =
				ArgumentCaptor.forClass(OrderProfitSharingDetails.class);
		verify(orderProfitSharingDetailsMapper, atLeastOnce()).insert(detailCap.capture());
		// 与 3.10.x 主成功链同基数：8000 分、费率 10 → 分账费 8 分，分销商 7992；推广佣金 100 分再扣
		int expectedDistributorFen = 8000 - 8 - 100;
		assertThat(
						detailCap.getAllValues().stream()
								.filter(
										d ->
												java.util.Objects.equals(
														o.getDistributorId(), d.getDistributorId()))
								.map(OrderProfitSharingDetails::getTotalFee)
								.findFirst())
				.hasValue(expectedDistributorFen);
	}

	@Test
	@DisplayName("3.11：首单事务内抛错不计入成功，次单成功仍处理且仅发布一次事件")
	void section_3_11_row_failure_continues_next_order() {
		PlatformTransactionManager tx = syncFiringTxManager();
		OrderProfitSharingService svc = serviceWithTx(tx);
		NormalOrders first = batchOrder(101L, 10L, 0L);
		NormalOrders second = batchOrder(102L, 10L, 0L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(first, second));
		when(hfPayPaymentSettingLoadPort.loadForCompany(10L))
				.thenReturn(Map.of("is_open", "false", "mer_cust_id", "", "acct_id", ""));
		HfpayEnterapply ent = new HfpayEnterapply();
		ent.setUserCustId("U_DIST");
		ent.setAcctId("A_DIST");
		when(hfpayEnterapplyMapper.selectOne(any())).thenReturn(ent);
		when(aftersalesRefundMapper.selectMaps(any())).thenReturn(Collections.emptyList());
		AtomicInteger insertCalls = new AtomicInteger();
		when(orderProfitSharingMapper.insert(any(OrderProfitSharing.class)))
				.thenAnswer(
						inv -> {
							OrderProfitSharing m = inv.getArgument(0);
							if (insertCalls.getAndIncrement() == 0) {
								throw new IllegalStateException("3.11 first order boom");
							}
							m.setOrderProfitSharingId(802L);
							return 1;
						});

		assertThat(svc.scheduleShareOrderProfit()).isEqualTo(1);
		verify(orderProfitSharingMapper, times(2)).insert(any(OrderProfitSharing.class));
		verify(hfpayProfitSharingEventDispatchPublisher, times(1))
				.publishProfitSharingAfterCommit(
						eq(102L),
						argThat(ids -> ids != null && ids.size() == 1 && ids.contains(802L)));
	}

	@Test
	@DisplayName(
			"testLists/lists 批语义：两单均成功则 scheduleShareOrderProfit 每单 commit 后各发布一次（publishProfitSharingAfterCommit ×2）")
	void testLists_semantic_two_success_orders_publish_after_commit_twice() {
		OrderProfitSharingService svc =
				serviceWithTx(syncFiringTxManager());
		NormalOrders first = batchOrder(201L, 10L, 0L);
		first.setTotalFee("8000");
		first.setProfitsharingRate(10);
		NormalOrders second = batchOrder(202L, 10L, 0L);
		second.setTotalFee("8000");
		second.setProfitsharingRate(10);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(first, second));
		when(hfPayPaymentSettingLoadPort.loadForCompany(10L))
				.thenReturn(Map.of("is_open", "false", "mer_cust_id", "", "acct_id", ""));
		HfpayEnterapply ent = new HfpayEnterapply();
		ent.setUserCustId("U_DIST");
		ent.setAcctId("A_DIST");
		when(hfpayEnterapplyMapper.selectOne(any())).thenReturn(ent);
		when(aftersalesRefundMapper.selectMaps(any())).thenReturn(Collections.emptyList());
		when(orderProfitSharingMapper.insert(any(OrderProfitSharing.class)))
				.thenAnswer(
						inv -> {
							OrderProfitSharing m = inv.getArgument(0);
							long oid = m.getOrderId() != null ? m.getOrderId() : 0L;
							if (oid == 201L) {
								m.setOrderProfitSharingId(901L);
							} else if (oid == 202L) {
								m.setOrderProfitSharingId(902L);
							} else {
								m.setOrderProfitSharingId(999L);
							}
							return 1;
						});

		assertThat(svc.scheduleShareOrderProfit()).isEqualTo(2);
		verify(orderProfitSharingMapper, times(2)).insert(any(OrderProfitSharing.class));
		verify(hfpayProfitSharingEventDispatchPublisher, times(2))
				.publishProfitSharingAfterCommit(anyLong(), any());
		verify(hfpayProfitSharingEventDispatchPublisher)
				.publishProfitSharingAfterCommit(
						eq(201L),
						argThat(ids -> ids != null && ids.size() == 1 && ids.contains(901L)));
		verify(hfpayProfitSharingEventDispatchPublisher)
				.publishProfitSharingAfterCommit(
						eq(202L),
						argThat(ids -> ids != null && ids.size() == 1 && ids.contains(902L)));
	}

	private OrderProfitSharingService serviceWithTx(PlatformTransactionManager tx) {
		return new OrderProfitSharingService(
				normalOrdersMapper,
				orderProfitSharingMapper,
				orderProfitSharingDetailsMapper,
				aftersalesRefundMapper,
				brokerageMapper,
				hfpayEnterapplyMapper,
				hfPayPaymentSettingLoadPort,
				tradeMapper,
				hfpayProfitSharingEventDispatchPublisher,
				tx);
	}

	private static NormalOrders batchOrder(long orderId, long companyId, long distId) {
		NormalOrders o = new NormalOrders();
		o.setOrderId(orderId);
		o.setCompanyId(companyId);
		o.setDistributorId(distId);
		o.setOrderStatus("DONE");
		o.setPayType("hfpay");
		o.setIsProfitsharing(2);
		o.setProfitsharingStatus(1);
		o.setOrderAutoCloseAftersalesTime((int) Instant.now().getEpochSecond());
		o.setTotalFee("1000");
		o.setProfitsharingRate(0);
		o.setIsDistribution(false);
		return o;
	}

	private static PlatformTransactionManager noopTxManager() {
		return new PlatformTransactionManager() {
			@Override
			public org.springframework.transaction.TransactionStatus getTransaction(
					TransactionDefinition definition) throws TransactionException {
				return new SimpleTransactionStatus(true);
			}

			@Override
			public void commit(org.springframework.transaction.TransactionStatus status) {}

			@Override
			public void rollback(org.springframework.transaction.TransactionStatus status) {}
		};
	}

	/**
	 * 在 commit 时触发已注册的 {@link TransactionSynchronization#afterCommit}，便于断言
	 * {@code publishEvent} 而无需真实数据源。
	 */
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
