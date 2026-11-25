package cn.shopex.ecshopx.orders.service.tradefinish;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.service.AftersalesRefundService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** ECX-11406 Spec §7.1–4、9（建单侧） */
@ExtendWith(MockitoExtension.class)
class OrdersTradeFinishReconcileServiceTest {

	private static final long COMPANY_ID = 1L;
	private static final long ORDER_ID = 5412622000140002L;
	private static final String TRADE_ID = "5412622000130002";

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(cfg, "");
		TableInfoHelper.initTableInfo(assistant, NormalOrders.class);
		TableInfoHelper.initTableInfo(assistant, Trade.class);
	}

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private TradeMapper tradeMapper;

	@Mock
	private OrdersTradeFinishNormalOrderPaySuccessApplyService applyService;

	@Mock
	private AftersalesRefundService aftersalesRefundService;

	private OrdersTradeFinishReconcileService service() {
		return new OrdersTradeFinishReconcileService(
				normalOrdersMapper, tradeMapper, applyService, aftersalesRefundService);
	}

	@Test
	@DisplayName("§7.1 NOTPAY 未超时 → payable")
	void notPayWithinAutoCancelWindowIsPayable() {
		long now = System.currentTimeMillis() / 1000;
		NormalOrders order = order("NOTPAY", String.valueOf(now + 3600));
		assertThat(service().resolveReconcileBranch(order, TRADE_ID, now))
				.isEqualTo(OrdersTradeFinishReconcileService.BRANCH_PAYABLE);
	}

	@Test
	@DisplayName("§7.2 PART_PAYMENT 不因超时走异常")
	void partPaymentIsPayableRegardlessOfAutoCancel() {
		NormalOrders order = order("PART_PAYMENT", String.valueOf(System.currentTimeMillis() / 1000 - 1));
		assertThat(service().resolveReconcileBranch(order, TRADE_ID, System.currentTimeMillis() / 1000))
				.isEqualTo(OrdersTradeFinishReconcileService.BRANCH_PAYABLE);
	}

	@Test
	@DisplayName("§7.3 超时 NOTPAY → PAY_AFTER_CANCEL 分支")
	void notPayTimedOutCreatesPayAfterCancelBranch() {
		long now = System.currentTimeMillis() / 1000;
		NormalOrders order = order("NOTPAY", String.valueOf(now - 60));
		assertThat(service().isNotPayTimedOut(order, now)).isTrue();
		assertThat(service().resolveReconcileBranch(order, TRADE_ID, now))
				.isEqualTo(OrdersTradeFinishReconcileService.BRANCH_PAY_AFTER_CANCEL);
	}

	@Test
	@DisplayName("§7.3 CANCEL → PAY_AFTER_CANCEL 分支")
	void cancelOrderCreatesPayAfterCancelBranch() {
		NormalOrders order = order("CANCEL", "0");
		assertThat(service().resolveReconcileBranch(order, TRADE_ID, System.currentTimeMillis() / 1000))
				.isEqualTo(OrdersTradeFinishReconcileService.BRANCH_PAY_AFTER_CANCEL);
	}

	@Test
	@DisplayName("§7.4 后置态无其他 SUCCESS 非积分 trade → 同笔忽略")
	void postPaidSameTradeIgnoresWhenNoOtherSuccessTrade() {
		when(tradeMapper.selectList(any())).thenReturn(List.of(successTrade(TRADE_ID, "wxpayjs")));
		NormalOrders order = order("PAYED", "0");
		assertThat(service().resolveReconcileBranch(order, TRADE_ID, System.currentTimeMillis() / 1000))
				.isEqualTo(OrdersTradeFinishReconcileService.BRANCH_SAME_TRADE_IGNORE);
	}

	@Test
	@DisplayName("§7.4 后置态有其他 SUCCESS 非积分 trade → DUP_PAY")
	void postPaidOtherSuccessTradeCreatesDupPayBranch() {
		when(tradeMapper.selectList(any()))
				.thenReturn(
						List.of(
								successTrade(TRADE_ID, "wxpayjs"),
								successTrade("5412622000130003", "wxpayjs")));
		NormalOrders order = order("DONE", "0");
		assertThat(service().resolveReconcileBranch(order, "5412622000130003", System.currentTimeMillis() / 1000))
				.isEqualTo(OrdersTradeFinishReconcileService.BRANCH_DUP_PAY);
	}

	@Test
	void hasOtherSuccessfulNonPointTradeIgnoresPointAndSelf() {
		when(tradeMapper.selectList(any()))
				.thenReturn(
						List.of(
								successTrade("5412622000130003", "wxpayjs"),
								successTrade("5412622000130099", "point")));
		assertThat(service().hasOtherSuccessfulNonPointTrade(COMPANY_ID, ORDER_ID, "5412622000130003"))
				.isFalse();
	}

	@Test
	@DisplayName("§7.3 逾期分支建 PAY_AFTER_CANCEL，不发 PaySuccess")
	void payAfterCancelBranchDelegatesCreateAbnormalTradeRefund() {
		OrdersTradeFinishReconcileService svc = service();
		stubTradeLoad();
		NormalOrders cancelOrder = order("CANCEL", "0");
		when(normalOrdersMapper.selectOne(any())).thenReturn(cancelOrder);
		AftersalesRefund created = new AftersalesRefund();
		created.setRefundBn(2202511250001L);
		created.setRefundStatus(AftersalesRefundService.REFUND_STATUS_PAY_AFTER_CANCEL);
		when(aftersalesRefundService.createAbnormalTradeRefund(
						any(), any(), eq(AftersalesRefundService.REFUND_STATUS_PAY_AFTER_CANCEL)))
				.thenReturn(created);

		Optional<Map<String, Object>> out = svc.reconcile(successPayload());

		assertThat(out).isEmpty();
		verify(aftersalesRefundService)
				.createAbnormalTradeRefund(
						any(), any(), eq(AftersalesRefundService.REFUND_STATUS_PAY_AFTER_CANCEL));
		verifyNoInteractions(applyService);
	}

	@Test
	@DisplayName("§7.9 DUP_PAY 重复回调幂等")
	void dupPayBranchCreateAbnormalTradeRefundIsIdempotentOnDuplicateCallback() {
		OrdersTradeFinishReconcileService svc = service();
		stubTradeLoad();
		when(normalOrdersMapper.selectOne(any())).thenReturn(order("PAYED", "0"));
		when(tradeMapper.selectList(any()))
				.thenReturn(
						List.of(
								successTrade("5412622000130001", "wxpayjs"),
								successTrade(TRADE_ID, "wxpayjs")));
		AftersalesRefund existing = new AftersalesRefund();
		existing.setRefundBn(2202511250001L);
		existing.setRefundStatus(AftersalesRefundService.REFUND_STATUS_DUP_PAY);
		when(aftersalesRefundService.createAbnormalTradeRefund(
						any(), any(), eq(AftersalesRefundService.REFUND_STATUS_DUP_PAY)))
				.thenReturn(existing);

		Map<String, Object> payload = successPayload();
		payload.put("trade_id", TRADE_ID);
		assertThat(svc.reconcile(payload)).isEmpty();
		assertThat(svc.reconcile(payload)).isEmpty();
		verify(aftersalesRefundService, times(2))
				.createAbnormalTradeRefund(any(), any(), eq(AftersalesRefundService.REFUND_STATUS_DUP_PAY));
		verify(applyService, never()).markPayedIfNeeded(any());
	}

	@Test
	@DisplayName("§7.1 可收付成功发布 PaySuccess payload")
	void payableBranchReturnsPaySuccessPayload() {
		OrdersTradeFinishReconcileService svc = service();
		stubTradeLoad();
		when(normalOrdersMapper.selectOne(any())).thenReturn(order("NOTPAY", String.valueOf(System.currentTimeMillis() / 1000 + 3600)));
		Map<String, Object> paySuccess = Map.of("order_id", ORDER_ID, "company_id", COMPANY_ID);
		when(applyService.markPayedIfNeeded(any())).thenReturn(Optional.of(paySuccess));

		Optional<Map<String, Object>> out = svc.reconcile(successPayload());

		assertThat(out).contains(paySuccess);
		verify(applyService).markPayedIfNeeded(any());
	}

	@Test
	void casMissRetriesThenSucceeds() {
		OrdersTradeFinishReconcileService svc = service();
		stubTradeLoad();
		NormalOrders notPay = order("NOTPAY", String.valueOf(System.currentTimeMillis() / 1000 + 3600));
		when(normalOrdersMapper.selectOne(any())).thenReturn(notPay, notPay, notPay);
		Map<String, Object> paySuccess = Map.of("order_id", ORDER_ID);
		when(applyService.markPayedIfNeeded(any()))
				.thenReturn(Optional.empty(), Optional.empty(), Optional.of(paySuccess));

		Optional<Map<String, Object>> out = svc.reconcile(successPayload());
		assertThat(out).contains(paySuccess);
		verify(applyService, times(3)).markPayedIfNeeded(any());
	}

	private void stubTradeLoad() {
		Trade t = successTrade(TRADE_ID, "wxpayjs");
		when(tradeMapper.selectOne(any())).thenReturn(t);
	}

	private static NormalOrders order(String status, String autoCancelTime) {
		NormalOrders o = new NormalOrders();
		o.setCompanyId(COMPANY_ID);
		o.setOrderId(ORDER_ID);
		o.setUserId(2L);
		o.setShopId(10L);
		o.setDistributorId(12L);
		o.setFreightType("cash");
		o.setOrderStatus(status);
		o.setAutoCancelTime(autoCancelTime);
		return o;
	}

	private static Trade successTrade(String tradeId, String payType) {
		Trade t = new Trade();
		t.setTradeId(tradeId);
		t.setPayType(payType);
		t.setTradeState("SUCCESS");
		t.setCompanyId(String.valueOf(COMPANY_ID));
		t.setOrderId(String.valueOf(ORDER_ID));
		t.setPayFee(100);
		t.setMerchantId(88L);
		return t;
	}

	private static Map<String, Object> successPayload() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("trade_state", "SUCCESS");
		m.put("company_id", COMPANY_ID);
		m.put("order_id", ORDER_ID);
		m.put("trade_id", TRADE_ID);
		m.put("trade_source_type", "normal_normal");
		m.put("pay_type", "wxpayjs");
		return m;
	}
}