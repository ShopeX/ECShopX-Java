package cn.shopex.ecshopx.bspay.service;

import static cn.shopex.ecshopx.bspay.service.BspayAutoCloseAftersalesPaymentConfirmationService.TRADE_PENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.bspay.domain.DivFee;
import cn.shopex.ecshopx.bspay.domain.PaymemtConfirm;
import cn.shopex.ecshopx.bspay.mapper.DivFeeMapper;
import cn.shopex.ecshopx.bspay.mapper.PaymemtConfirmMapper;
import cn.shopex.ecshopx.common.cron.payment.BspayPaymentConfirmHttpGateway;
import cn.shopex.ecshopx.common.port.payment.BspayPaymentSettingsReadPort;
import cn.shopex.ecshopx.distribution.mapper.DistributorMapper;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.Trade;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.TradeMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BspayAutoCloseAftersalesPaymentConfirmationServiceTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), PaymemtConfirm.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Trade.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), DivFee.class);
	}

	@Mock
	private NormalOrdersItemsMapper normalOrdersItemsMapper;

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private TradeMapper tradeMapper;

	@Mock
	private AftersalesRefundMapper aftersalesRefundMapper;

	@Mock
	private PaymemtConfirmMapper paymemtConfirmMapper;

	@Mock
	private DivFeeMapper divFeeMapper;

	@Mock
	private DistributorMapper distributorMapper;

	@Mock
	private BspayPaymentSettingsReadPort bspayPaymentSettingsReadPort;

	@Mock
	private BspayHuifuIdResolver bspayHuifuIdResolver;

	@Mock
	private BspayPaymentConfirmHttpGateway bspayPaymentConfirmHttpGateway;

	private BspayAutoCloseAftersalesPaymentConfirmationService newService() {
		return new BspayAutoCloseAftersalesPaymentConfirmationService(
				normalOrdersItemsMapper,
				normalOrdersMapper,
				tradeMapper,
				aftersalesRefundMapper,
				paymemtConfirmMapper,
				divFeeMapper,
				distributorMapper,
				bspayPaymentSettingsReadPort,
				bspayHuifuIdResolver,
				bspayPaymentConfirmHttpGateway,
				new ObjectMapper());
	}

	@Test
	@DisplayName("§3 scheduleAutoPaymentConfirmation 步骤 B：子单未全 CLOSED 时不 INSERT 确认行、不调 HTTP")
	void whenItemsNotAllClosed_skipsConfirmInsertAndHttp() {
		var open = new NormalOrdersItems();
		open.setAftersalesStatus("WAIT");
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(open));
		var service = newService();
		service.scheduleAutoPaymentConfirmation(7L, 3L);
		verify(paymemtConfirmMapper, never()).insert(ArgumentMatchers.<PaymemtConfirm>any());
		verify(bspayPaymentConfirmHttpGateway, never()).callDelaytransConfirm(any());
	}

	@Test
	@DisplayName("§3 scheduleAutoPaymentConfirmation 步骤 C：子单全 CLOSED 时 INSERT pending（TRADE_PENDING）确认行")
	void whenAllClosed_insertsPendingConfirmRow() {
		var closed = new NormalOrdersItems();
		closed.setAftersalesStatus("CLOSED");
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(closed));
		when(normalOrdersMapper.selectOne(any())).thenReturn(null);
		var service = newService();
		service.scheduleAutoPaymentConfirmation(7L, 3L);
		verify(paymemtConfirmMapper, times(1))
				.insert(
						ArgumentMatchers.<PaymemtConfirm>argThat(
								row ->
										row != null
												&& TRADE_PENDING.equals(
														row.getStatus())
												&& row.getCompanyId() != null
												&& row.getCompanyId() == 7L
												&& "3".equals(row.getOrderId())));
		verify(bspayPaymentConfirmHttpGateway, never()).callDelaytransConfirm(any());
	}

	@Test
	@DisplayName("§3 scheduleRetryBsPayConfirm 步骤 1：无待重试行时返回 0、不调 HTTP")
	void whenNoPendingRows_scheduleRetryReturnsZeroAndNoHttp() {
		when(paymemtConfirmMapper.selectList(any())).thenReturn(List.of());
		var service = newService();
		assertThat(service.scheduleRetryBsPayConfirm()).isZero();
		verify(bspayPaymentConfirmHttpGateway, never()).callDelaytransConfirm(any());
	}

	@Test
	@DisplayName("§3 scheduleRetryBsPayConfirm paymentConfirmation 步骤 2：订单不存在时早退、不调 HTTP")
	void scheduleRetry_step2_orderMissing_noHttp() {
		when(paymemtConfirmMapper.selectList(any())).thenReturn(List.of(pendingRow(10L, 1L, 100L)));
		when(normalOrdersMapper.selectOne(any())).thenReturn(null);
		var service = newService();
		assertThat(service.scheduleRetryBsPayConfirm()).isEqualTo(1);
		verify(bspayPaymentConfirmHttpGateway, never()).callDelaytransConfirm(any());
	}

	@Test
	@DisplayName("§3 scheduleRetryBsPayConfirm paymentConfirmation 步骤 2：pay_type 非 bspay 早退、不调 HTTP")
	void scheduleRetry_step2_payTypeNotBspay_noHttp() {
		var order = new NormalOrders();
		order.setCompanyId(1L);
		order.setOrderId(100L);
		order.setPayType("wechat");
		when(paymemtConfirmMapper.selectList(any())).thenReturn(List.of(pendingRow(10L, 1L, 100L)));
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		var service = newService();
		assertThat(service.scheduleRetryBsPayConfirm()).isEqualTo(1);
		verify(bspayPaymentConfirmHttpGateway, never()).callDelaytransConfirm(any());
	}

	@Test
	@DisplayName("§3 scheduleRetryBsPayConfirm paymentConfirmation 步骤 3：无成功 bspay 交易单时早退、不调 HTTP")
	void scheduleRetry_step3_tradeMissing_noHttp() {
		var order = bspayOrder(1L, 100L);
		when(paymemtConfirmMapper.selectList(any())).thenReturn(List.of(pendingRow(10L, 1L, 100L)));
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(tradeMapper.selectOne(any())).thenReturn(null);
		var service = newService();
		assertThat(service.scheduleRetryBsPayConfirm()).isEqualTo(1);
		verify(bspayPaymentConfirmHttpGateway, never()).callDelaytransConfirm(any());
	}

	@Test
	@DisplayName("§3 scheduleRetryBsPayConfirm paymentConfirmation 步骤 3：无 transaction_id 时早退、不调 HTTP")
	void scheduleRetry_step3_noTransactionId_noHttp() {
		var order = bspayOrder(1L, 100L);
		var tr = bspayTrade("100", 100L, 1L);
		tr.setTransactionId("");
		when(paymemtConfirmMapper.selectList(any())).thenReturn(List.of(pendingRow(10L, 1L, 100L)));
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(tradeMapper.selectOne(any())).thenReturn(tr);
		var service = newService();
		assertThat(service.scheduleRetryBsPayConfirm()).isEqualTo(1);
		verify(bspayPaymentConfirmHttpGateway, never()).callDelaytransConfirm(any());
	}

	@Test
	@DisplayName("§3 scheduleRetryBsPayConfirm paymentConfirmation 步骤 3：交易单 pay_type 非 bspay 早退、不调 HTTP")
	void scheduleRetry_step3_tradePayTypeNotBspay_noHttp() {
		var order = bspayOrder(1L, 100L);
		var tr = bspayTrade("100", 100L, 1L);
		tr.setPayType("wechat");
		when(paymemtConfirmMapper.selectList(any())).thenReturn(List.of(pendingRow(10L, 1L, 100L)));
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(tradeMapper.selectOne(any())).thenReturn(tr);
		var service = newService();
		assertThat(service.scheduleRetryBsPayConfirm()).isEqualTo(1);
		verify(bspayPaymentConfirmHttpGateway, never()).callDelaytransConfirm(any());
	}

	@Test
	@DisplayName("§3 scheduleRetryBsPayConfirm paymentConfirmation 步骤 4：退款后 totalFeeFen<=0 时早退、不调 HTTP")
	void scheduleRetry_step4_refundZeroesAmount_noHttp() {
		var order = bspayOrder(1L, 100L);
		var tr = bspayTrade("100", 100L, 1L);
		tr.setTotalFee(10_000);
		var r = new AftersalesRefund();
		r.setRefundStatus("SUCCESS");
		r.setRefundFee(10_000);
		when(paymemtConfirmMapper.selectList(any())).thenReturn(List.of(pendingRow(10L, 1L, 100L)));
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(tradeMapper.selectOne(any())).thenReturn(tr);
		when(aftersalesRefundMapper.selectList(any())).thenReturn(List.of(r));
		var service = newService();
		assertThat(service.scheduleRetryBsPayConfirm()).isEqualTo(1);
		verify(bspayPaymentConfirmHttpGateway, never()).callDelaytransConfirm(any());
	}

	@Test
	@DisplayName("§3 scheduleRetryBsPayConfirm paymentConfirmation 步骤 5：未设费率 早退、不调 HTTP")
	void scheduleRetry_step5_noFeeRate_noHttp() {
		var order = bspayOrder(1L, 100L);
		var tr = bspayTrade("100", 100L, 1L);
		tr.setPayChannel("no_such_channel");
		when(paymemtConfirmMapper.selectList(any())).thenReturn(List.of(pendingRow(10L, 1L, 100L)));
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(tradeMapper.selectOne(any())).thenReturn(tr);
		when(aftersalesRefundMapper.selectList(any())).thenReturn(List.of());
		var service = newService();
		assertThat(service.scheduleRetryBsPayConfirm()).isEqualTo(1);
		verify(bspayPaymentConfirmHttpGateway, never()).callDelaytransConfirm(any());
	}

	@Test
	@DisplayName("§3 scheduleRetryBsPayConfirm paymentConfirmation 步骤 5：含供应商品行时 getDivMember 为 null 早退、不调 HTTP")
	void scheduleRetry_step5_supplierItemsBuildDivNull_noHttp() {
		var order = bspayOrder(1L, 100L);
		var tr = bspayTrade("100", 100L, 1L);
		tr.setPayChannel("wx_lite");
		when(paymemtConfirmMapper.selectList(any())).thenReturn(List.of(pendingRow(10L, 1L, 100L)));
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(tradeMapper.selectOne(any())).thenReturn(tr);
		when(aftersalesRefundMapper.selectList(any())).thenReturn(List.of());
		when(bspayPaymentSettingsReadPort.requireSettingMap(ArgumentMatchers.anyLong())).thenReturn(wxLiteFeeSetting());
		when(normalOrdersItemsMapper.selectCount(any())).thenReturn(1L);
		var service = newService();
		assertThat(service.scheduleRetryBsPayConfirm()).isEqualTo(1);
		verify(bspayPaymentConfirmHttpGateway, never()).callDelaytransConfirm(any());
	}

	@Test
	@DisplayName("§3 scheduleRetryBsPayConfirm paymentConfirmation 步骤 5：mode2 下可分账为 0 无 div 早退、不调 HTTP")
	void scheduleRetry_step5_mode2_zeroDivNoMembers_noHttp() {
		var order = bspayOrder(1L, 100L);
		var tr = bspayTrade("100", 100L, 1L);
		tr.setPayChannel("wx_lite");
		when(paymemtConfirmMapper.selectList(any())).thenReturn(List.of(pendingRow(10L, 1L, 100L)));
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(tradeMapper.selectOne(any())).thenReturn(tr);
		when(aftersalesRefundMapper.selectList(any())).thenReturn(List.of());
		when(bspayPaymentSettingsReadPort.requireSettingMap(ArgumentMatchers.anyLong())).thenReturn(wxLiteFeeSettingFullRate());
		when(normalOrdersItemsMapper.selectCount(any())).thenReturn(0L);
		var service = newService();
		ReflectionTestUtils.setField(service, "headquartersFeeMode", "2");
		assertThat(service.scheduleRetryBsPayConfirm()).isEqualTo(1);
		verify(bspayPaymentConfirmHttpGateway, never()).callDelaytransConfirm(any());
	}

	@Test
	@DisplayName("§3 scheduleRetryBsPayConfirm paymentConfirmation 步骤 6 trans_stat F：只更新确认表 不调 trade 更新 不插 div_fee")
	void scheduleRetry_step6_transStatF_onlyConfirmUpdate() {
		var order = bspayOrder(1L, 100L);
		var tr = bspayTrade("T-100", 100L, 1L);
		when(paymemtConfirmMapper.selectList(any())).thenReturn(List.of(pendingRow(10L, 1L, 100L)));
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(tradeMapper.selectOne(any())).thenReturn(tr);
		when(aftersalesRefundMapper.selectList(any())).thenReturn(List.of());
		when(bspayPaymentSettingsReadPort.requireSettingMap(ArgumentMatchers.anyLong())).thenReturn(wxLiteFeeSetting());
		when(normalOrdersItemsMapper.selectCount(any())).thenReturn(0L);
		when(bspayPaymentConfirmHttpGateway.callDelaytransConfirm(any()))
				.thenReturn(Map.of("data", Map.of("trans_stat", "F", "resp_desc", "fail")));
		var service = newService();
		ReflectionTestUtils.setField(service, "headquartersFeeMode", "1");
		assertThat(service.scheduleRetryBsPayConfirm()).isEqualTo(1);
		verify(paymemtConfirmMapper, times(1)).update(any(), any());
		verify(tradeMapper, never()).update(any(), any());
		verify(divFeeMapper, never()).insert(ArgumentMatchers.<DivFee>any());
	}

	@Test
	@DisplayName("§3 scheduleRetryBsPayConfirm paymentConfirmation 步骤 6 非 trans_stat F：确认表+trade+div_fee 三表落库")
	void scheduleRetry_step6_nonF_writesConfirmTradeDivFee() {
		var order = bspayOrder(1L, 100L);
		var tr = bspayTrade("T-100", 100L, 1L);
		when(paymemtConfirmMapper.selectList(any())).thenReturn(List.of(pendingRow(10L, 1L, 100L)));
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(tradeMapper.selectOne(any())).thenReturn(tr);
		when(aftersalesRefundMapper.selectList(any())).thenReturn(List.of());
		when(bspayPaymentSettingsReadPort.requireSettingMap(ArgumentMatchers.anyLong())).thenReturn(wxLiteFeeSetting());
		when(normalOrdersItemsMapper.selectCount(any())).thenReturn(0L);
		when(bspayPaymentConfirmHttpGateway.callDelaytransConfirm(any()))
				.thenReturn(Map.of("data", Map.of("trans_stat", "S", "hf_seq_id", "HF-SEQ-1")));
		var service = newService();
		ReflectionTestUtils.setField(service, "headquartersFeeMode", "1");
		assertThat(service.scheduleRetryBsPayConfirm()).isEqualTo(1);
		verify(paymemtConfirmMapper, times(1)).update(any(), any());
		verify(tradeMapper, times(1)).update(any(), any());
		verify(divFeeMapper, times(1)).insert(ArgumentMatchers.<DivFee>any());
	}

	@Test
	@DisplayName("§3 scheduleRetryBsPayConfirm 步骤 6：HTTP 抛错 中断 foreach 不处理下一条、不调第二条订单")
	void scheduleRetry_step6_firstHttpError_breaksForEach() {
		when(paymemtConfirmMapper.selectList(any()))
				.thenReturn(List.of(pendingRow(1L, 1L, 1L), pendingRow(2L, 1L, 2L)));
		var o1 = bspayOrder(1L, 1L);
		var t1 = bspayTrade("T-1", 1L, 1L);
		when(normalOrdersMapper.selectOne(any())).thenReturn(o1);
		when(tradeMapper.selectOne(any())).thenReturn(t1);
		when(aftersalesRefundMapper.selectList(any())).thenReturn(List.of());
		when(bspayPaymentSettingsReadPort.requireSettingMap(ArgumentMatchers.anyLong())).thenReturn(wxLiteFeeSetting());
		when(normalOrdersItemsMapper.selectCount(any())).thenReturn(0L);
		when(bspayPaymentConfirmHttpGateway.callDelaytransConfirm(any()))
				.thenThrow(new RuntimeException("simulated BspayPaymentConfirm http failure"));
		var service = newService();
		ReflectionTestUtils.setField(service, "headquartersFeeMode", "1");
		assertThrows(RuntimeException.class, service::scheduleRetryBsPayConfirm);
		verify(normalOrdersMapper, times(1)).selectOne(any());
		verify(bspayPaymentConfirmHttpGateway, times(1)).callDelaytransConfirm(any());
	}

	private static PaymemtConfirm pendingRow(long id, long companyId, long orderId) {
		int t = (int) (System.currentTimeMillis() / 1000L) - 700;
		var p = new PaymemtConfirm();
		p.setId(id);
		p.setCompanyId(companyId);
		p.setOrderId(String.valueOf(orderId));
		p.setStatus(TRADE_PENDING);
		p.setCreated(t);
		return p;
	}

	private static NormalOrders bspayOrder(long companyId, long orderId) {
		var o = new NormalOrders();
		o.setCompanyId(companyId);
		o.setOrderId(orderId);
		o.setPayType("bspay");
		o.setDistributorId(0L);
		return o;
	}

	/** 与 paymentConfirmation 查询条件一致：SUCCESS + bspay */
	private static Trade bspayTrade(String tradeId, long orderId, long companyId) {
		var t = new Trade();
		t.setTradeId(tradeId);
		t.setOrderId(String.valueOf(orderId));
		t.setCompanyId(String.valueOf(companyId));
		t.setTradeState("SUCCESS");
		t.setPayType("bspay");
		t.setTransactionId("tx-1");
		t.setTotalFee(10_000);
		t.setPayChannel("wx_lite");
		t.setBspayReqDate("20200101");
		return t;
	}

	/** 费率 0.6%：可分账金额 >0，distributor_id=0 走 admin 单分账方 */
	private static Map<String, Object> wxLiteFeeSetting() {
		var m = new HashMap<String, Object>();
		m.put("wxpay_fee_type", "miniprogram");
		m.put("wx_lite_miniprogram", new BigDecimal("0.6"));
		m.put("sys_id", "SYS-1");
		return m;
	}

	/** 费率 100%：在 headquartersFeeMode=2 下 可分账为 0，buildDivMember 在 distributor_id=0 时返回 null */
	private static Map<String, Object> wxLiteFeeSettingFullRate() {
		var m = new HashMap<String, Object>();
		m.put("wxpay_fee_type", "miniprogram");
		m.put("wx_lite_miniprogram", new BigDecimal("100"));
		m.put("sys_id", "SYS-1");
		return m;
	}
}
