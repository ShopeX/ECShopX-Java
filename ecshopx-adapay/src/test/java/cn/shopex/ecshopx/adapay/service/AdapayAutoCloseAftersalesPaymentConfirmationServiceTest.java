package cn.shopex.ecshopx.adapay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.adapay.domain.AdapayDivFee;
import cn.shopex.ecshopx.adapay.domain.AdapayMember;
import cn.shopex.ecshopx.adapay.domain.AdapayMerchantResident;
import cn.shopex.ecshopx.adapay.domain.AdapayPaymemtConfirm;
import cn.shopex.ecshopx.adapay.mapper.AdapayDivFeeMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayMemberMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayMerchantResidentMapper;
import cn.shopex.ecshopx.adapay.mapper.AdapayPaymemtConfirmMapper;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.common.cron.payment.AdapayPaymentConfirmHttpGateway;
import cn.shopex.ecshopx.common.port.payment.AdapayPaymentSettingsReadPort;
import cn.shopex.ecshopx.distribution.domain.Distributor;
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
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdapayAutoCloseAftersalesPaymentConfirmationServiceTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), AdapayPaymemtConfirm.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Trade.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), AdapayDivFee.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Distributor.class);
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
	private AdapayPaymemtConfirmMapper adapayPaymemtConfirmMapper;

	@Mock
	private AdapayDivFeeMapper adapayDivFeeMapper;

	@Mock
	private AdapayMemberMapper adapayMemberMapper;

	@Mock
	private AdapayMerchantResidentMapper adapayMerchantResidentMapper;

	@Mock
	private DistributorMapper distributorMapper;

	@Mock
	private AdapayPaymentSettingsReadPort adapayPaymentSettingsReadPort;

	@Mock
	private AdapayPaymentConfirmHttpGateway adapayPaymentConfirmHttpGateway;

	private AdapayAutoCloseAftersalesPaymentConfirmationService newService() {
		return new AdapayAutoCloseAftersalesPaymentConfirmationService(
				normalOrdersItemsMapper,
				normalOrdersMapper,
				tradeMapper,
				aftersalesRefundMapper,
				adapayPaymemtConfirmMapper,
				adapayDivFeeMapper,
				adapayMemberMapper,
				adapayMerchantResidentMapper,
				distributorMapper,
				adapayPaymentSettingsReadPort,
				adapayPaymentConfirmHttpGateway,
				new ObjectMapper());
	}

	private static AdapayPaymemtConfirm pendingRetryFixture(long companyId, long orderId) {
		int now = (int) (System.currentTimeMillis() / 1000L);
		var row = new AdapayPaymemtConfirm();
		row.setCompanyId(companyId);
		row.setOrderId(String.valueOf(orderId));
		row.setStatus("pending");
		row.setCreateTime(now - 700);
		return row;
	}

	@Test
	@DisplayName("§3 scheduleAutoPaymentConfirmation 步骤 B：子单未全 CLOSED 时不 INSERT 确认行、不调 HTTP")
	void whenItemsNotAllClosed_skipsConfirmInsertAndHttp() {
		var open = new NormalOrdersItems();
		open.setAftersalesStatus("REFUNDING");
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(open));
		var service = newService();
		service.scheduleAutoPaymentConfirmation(9L, 2L);
		verify(adapayPaymemtConfirmMapper, never()).insert(ArgumentMatchers.<AdapayPaymemtConfirm>any());
		verify(adapayPaymentConfirmHttpGateway, never()).call(any());
	}

	@Test
	@DisplayName("§3 scheduleAutoPaymentConfirmation 步骤 C：子单全 CLOSED 时 INSERT pending 确认行（后续链路在订单缺失时早退）")
	void whenAllClosed_insertsPendingConfirmRow() {
		var closed = new NormalOrdersItems();
		closed.setAftersalesStatus("CLOSED");
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(closed));
		when(normalOrdersMapper.selectOne(any())).thenReturn(null);
		var service = newService();
		service.scheduleAutoPaymentConfirmation(9L, 2L);
		verify(adapayPaymemtConfirmMapper, times(1))
				.insert(
						ArgumentMatchers.<AdapayPaymemtConfirm>argThat(
								row ->
										row != null
												&& "pending".equals(row.getStatus())
												&& row.getCompanyId() != null
												&& row.getCompanyId() == 9L
												&& "2".equals(row.getOrderId())));
		verify(adapayPaymentConfirmHttpGateway, never()).call(any());
	}

	@Test
	@DisplayName("§3 adaPayPaymentConfirmRetry：无 pending 行时返回 0 且不调 HTTP")
	void adaPayPaymentConfirmRetry_whenNoPending_returns0() {
		when(adapayPaymemtConfirmMapper.selectList(any())).thenReturn(List.of());
		var service = newService();
		assertThat(service.adaPayPaymentConfirmRetry()).isZero();
		verify(adapayPaymentConfirmHttpGateway, never()).call(any());
	}

	@Test
	@DisplayName("§3 adaPayPaymentConfirmRetry 步骤 2：订单不存在早退不调 HTTP")
	void adaPayPaymentConfirmRetry_step2_orderMissing_noHttp() {
		when(adapayPaymemtConfirmMapper.selectList(any())).thenReturn(List.of(pendingRetryFixture(1L, 200L)));
		when(normalOrdersMapper.selectOne(any())).thenReturn(null);
		var service = newService();
		assertThat(service.adaPayPaymentConfirmRetry()).isEqualTo(1);
		verify(adapayPaymentConfirmHttpGateway, never()).call(any());
	}

	@Test
	@DisplayName("§3 adaPayPaymentConfirmRetry 步骤 2：非 adapay 支付方式早退不调 HTTP")
	void adaPayPaymentConfirmRetry_step2_nonAdapayPayType_noHttp() {
		when(adapayPaymemtConfirmMapper.selectList(any())).thenReturn(List.of(pendingRetryFixture(1L, 201L)));
		var order = new NormalOrders();
		order.setCompanyId(1L);
		order.setOrderId(201L);
		order.setPayType("wx_pub");
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		var service = newService();
		assertThat(service.adaPayPaymentConfirmRetry()).isEqualTo(1);
		verify(adapayPaymentConfirmHttpGateway, never()).call(any());
	}

	@Test
	@DisplayName("§3 adaPayPaymentConfirmRetry 步骤 3：无成功 adapay 交易早退不调 HTTP")
	void adaPayPaymentConfirmRetry_step3_noTrade_noHttp() {
		when(adapayPaymemtConfirmMapper.selectList(any())).thenReturn(List.of(pendingRetryFixture(1L, 202L)));
		var order = new NormalOrders();
		order.setCompanyId(1L);
		order.setOrderId(202L);
		order.setPayType("adapay");
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(tradeMapper.selectOne(any())).thenReturn(null);
		var service = newService();
		assertThat(service.adaPayPaymentConfirmRetry()).isEqualTo(1);
		verify(adapayPaymentConfirmHttpGateway, never()).call(any());
	}

	@Test
	@DisplayName("§3 adaPayPaymentConfirmRetry 步骤 3：交易缺少 transaction_id 早退不调 HTTP")
	void adaPayPaymentConfirmRetry_step3_blankTransactionId_noHttp() {
		when(adapayPaymemtConfirmMapper.selectList(any())).thenReturn(List.of(pendingRetryFixture(1L, 203L)));
		var order = new NormalOrders();
		order.setCompanyId(1L);
		order.setOrderId(203L);
		order.setPayType("adapay");
		var trade = new Trade();
		trade.setTradeId("tid-203");
		trade.setCompanyId("1");
		trade.setOrderId("203");
		trade.setTradeState("SUCCESS");
		trade.setPayType("adapay");
		trade.setTransactionId("");
		trade.setTotalFee(100);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(tradeMapper.selectOne(any())).thenReturn(trade);
		var service = newService();
		assertThat(service.adaPayPaymentConfirmRetry()).isEqualTo(1);
		verify(adapayPaymentConfirmHttpGateway, never()).call(any());
	}

	@Test
	@DisplayName("§3 adaPayPaymentConfirmRetry 步骤 4：退款后剩余金额≤0 静默早退不调 HTTP")
	void adaPayPaymentConfirmRetry_step4_refundExhaustsAmount_noHttp() {
		when(adapayPaymemtConfirmMapper.selectList(any())).thenReturn(List.of(pendingRetryFixture(1L, 204L)));
		var order = new NormalOrders();
		order.setCompanyId(1L);
		order.setOrderId(204L);
		order.setPayType("adapay");
		var trade = new Trade();
		trade.setTradeId("tid-204");
		trade.setCompanyId("1");
		trade.setOrderId("204");
		trade.setTradeState("SUCCESS");
		trade.setPayType("adapay");
		trade.setTransactionId("tx-204");
		trade.setTotalFee(10000);
		var refund = new AftersalesRefund();
		refund.setRefundFee(10000);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(tradeMapper.selectOne(any())).thenReturn(trade);
		when(aftersalesRefundMapper.selectList(any())).thenReturn(List.of(refund));
		var service = newService();
		assertThat(service.adaPayPaymentConfirmRetry()).isEqualTo(1);
		verify(adapayPaymentConfirmHttpGateway, never()).call(any());
	}

	@Test
	@DisplayName("§3 adaPayPaymentConfirmRetry 步骤 5：未配置费率早退不调 HTTP")
	void adaPayPaymentConfirmRetry_step5_noFeeRate_noHttp() {
		when(adapayPaymemtConfirmMapper.selectList(any())).thenReturn(List.of(pendingRetryFixture(1L, 205L)));
		var order = new NormalOrders();
		order.setCompanyId(1L);
		order.setOrderId(205L);
		order.setPayType("adapay");
		var trade = new Trade();
		trade.setTradeId("tid-205");
		trade.setCompanyId("1");
		trade.setOrderId("205");
		trade.setTradeState("SUCCESS");
		trade.setPayType("adapay");
		trade.setTransactionId("tx-205");
		trade.setTotalFee(10000);
		trade.setPayChannel("wx_pub");
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(tradeMapper.selectOne(any())).thenReturn(trade);
		when(aftersalesRefundMapper.selectList(any())).thenReturn(List.of());
		when(adapayPaymentSettingsReadPort.getPaymentSetting(1L)).thenReturn(new HashMap<>());
		var service = newService();
		assertThat(service.adaPayPaymentConfirmRetry()).isEqualTo(1);
		verify(adapayPaymentConfirmHttpGateway, never()).call(any());
	}

	@Test
	@DisplayName("§3 adaPayPaymentConfirmRetry 步骤 6：汇付请求 div_members 剔除 amount 为 0.00 的项")
	void adaPayPaymentConfirmRetry_step6_httpRequest_excludesZeroAmountDivMembers() throws Exception {
		when(adapayPaymemtConfirmMapper.selectList(any())).thenReturn(List.of(pendingRetryFixture(1L, 100L)));
		var order = new NormalOrders();
		order.setCompanyId(1L);
		order.setOrderId(100L);
		order.setPayType("adapay");
		order.setDistributorId(5L);
		var trade = new Trade();
		trade.setTradeId("tid-step6");
		trade.setCompanyId("1");
		trade.setOrderId("100");
		trade.setTradeState("SUCCESS");
		trade.setPayType("adapay");
		trade.setTransactionId("tx-step6");
		trade.setTotalFee(10000);
		trade.setPayChannel("wx_pub");
		var split = new HashMap<String, Object>();
		split.put("adapay_fee_mode", "I");
		split.put("headquarters_proportion", 0);
		split.put("dealer_proportion", 30);
		var dist = new Distributor();
		dist.setCompanyId(1L);
		dist.setDistributorId(5L);
		dist.setDealerId(7);
		dist.setSplitLedgerInfo(new ObjectMapper().writeValueAsString(split));
		var memA = new AdapayMember();
		memA.setId(11L);
		var memB = new AdapayMember();
		memB.setId(22L);
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(tradeMapper.selectOne(any())).thenReturn(trade);
		when(aftersalesRefundMapper.selectList(any())).thenReturn(List.of());
		var settings = new HashMap<String, Object>();
		settings.put("wxpay_fee_type", "1");
		settings.put("wx_pub_1", new BigDecimal("0.6"));
		when(adapayPaymentSettingsReadPort.getPaymentSetting(1L)).thenReturn(settings);
		when(distributorMapper.selectOne(any())).thenReturn(dist);
		when(adapayMemberMapper.selectOne(any())).thenReturn(memA, memB);
		when(adapayPaymentConfirmHttpGateway.call(any()))
				.thenReturn(Map.of("data", Map.of("status", "succeeded", "id", "pc-step6")));
		when(adapayPaymemtConfirmMapper.update(any(), any())).thenReturn(1);
		when(tradeMapper.update(any(), any())).thenReturn(1);
		when(adapayDivFeeMapper.insert(ArgumentMatchers.<AdapayDivFee>any())).thenReturn(1);
		var service = newService();
		assertThat(service.adaPayPaymentConfirmRetry()).isEqualTo(1);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
		verify(adapayPaymentConfirmHttpGateway, times(1)).call(cap.capture());
		Object divObj = cap.getValue().get("div_members");
		assertThat(divObj).isInstanceOf(List.class);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> divMembers = (List<Map<String, Object>>) divObj;
		assertThat(divMembers)
				.isNotEmpty()
				.allMatch(m -> !"0.00".equals(String.valueOf(m.get("amount"))));
	}

	@Test
	@DisplayName("§3 adaPayPaymentConfirmRetry 步骤 7：汇付 status=failed 仅更新确认表不写 trade/div_fee")
	void adaPayPaymentConfirmRetry_step7_httpFailed_onlyUpdatesConfirm() {
		when(adapayPaymemtConfirmMapper.selectList(any())).thenReturn(List.of(pendingRetryFixture(1L, 300L)));
		var order = new NormalOrders();
		order.setCompanyId(1L);
		order.setOrderId(300L);
		order.setPayType("adapay");
		order.setDistributorId(0L);
		var trade = new Trade();
		trade.setTradeId("tid-fail");
		trade.setCompanyId("1");
		trade.setOrderId("300");
		trade.setTradeState("SUCCESS");
		trade.setPayType("adapay");
		trade.setTransactionId("tx-fail");
		trade.setTotalFee(10000);
		trade.setPayChannel("wx_pub");
		var resident = new AdapayMerchantResident();
		resident.setAdapayFeeMode("I");
		var settings = new HashMap<String, Object>();
		settings.put("wxpay_fee_type", "1");
		settings.put("wx_pub_1", new BigDecimal("0.6"));
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(tradeMapper.selectOne(any())).thenReturn(trade);
		when(aftersalesRefundMapper.selectList(any())).thenReturn(List.of());
		when(adapayPaymentSettingsReadPort.getPaymentSetting(1L)).thenReturn(settings);
		when(adapayMerchantResidentMapper.selectOne(any())).thenReturn(resident);
		when(adapayPaymentConfirmHttpGateway.call(any()))
				.thenReturn(Map.of("data", Map.of("status", "failed", "error_msg", "stub")));
		when(adapayPaymemtConfirmMapper.update(any(), any())).thenReturn(1);
		var service = newService();
		assertThat(service.adaPayPaymentConfirmRetry()).isEqualTo(1);
		verify(adapayPaymentConfirmHttpGateway, times(1)).call(any());
		verify(adapayPaymemtConfirmMapper, times(1)).update(any(), any());
		verify(tradeMapper, never()).update(any(), any());
		verify(adapayDivFeeMapper, never()).insert(ArgumentMatchers.<AdapayDivFee>any());
	}

	@Test
	@DisplayName("§3 adaPayPaymentConfirmRetry 步骤 8：非 failed 成功路径更新确认表、trade 与 div_fee")
	void adaPayPaymentConfirmRetry_step8_success_updatesConfirmTradeAndDivFee() {
		when(adapayPaymemtConfirmMapper.selectList(any())).thenReturn(List.of(pendingRetryFixture(1L, 301L)));
		var order = new NormalOrders();
		order.setCompanyId(1L);
		order.setOrderId(301L);
		order.setPayType("adapay");
		order.setDistributorId(0L);
		var trade = new Trade();
		trade.setTradeId("tid-ok");
		trade.setCompanyId("1");
		trade.setOrderId("301");
		trade.setTradeState("SUCCESS");
		trade.setPayType("adapay");
		trade.setTransactionId("tx-ok");
		trade.setTotalFee(10000);
		trade.setPayChannel("wx_pub");
		var resident = new AdapayMerchantResident();
		resident.setAdapayFeeMode("I");
		var settings = new HashMap<String, Object>();
		settings.put("wxpay_fee_type", "1");
		settings.put("wx_pub_1", new BigDecimal("0.6"));
		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(tradeMapper.selectOne(any())).thenReturn(trade);
		when(aftersalesRefundMapper.selectList(any())).thenReturn(List.of());
		when(adapayPaymentSettingsReadPort.getPaymentSetting(1L)).thenReturn(settings);
		when(adapayMerchantResidentMapper.selectOne(any())).thenReturn(resident);
		when(adapayPaymentConfirmHttpGateway.call(any()))
				.thenReturn(Map.of("data", Map.of("status", "succeeded", "id", "pc-301")));
		when(adapayPaymemtConfirmMapper.update(any(), any())).thenReturn(1);
		when(tradeMapper.update(any(), any())).thenReturn(1);
		when(adapayDivFeeMapper.insert(ArgumentMatchers.<AdapayDivFee>any())).thenReturn(1);
		var service = newService();
		assertThat(service.adaPayPaymentConfirmRetry()).isEqualTo(1);
		verify(adapayPaymentConfirmHttpGateway, times(1)).call(any());
		verify(adapayPaymemtConfirmMapper, times(1)).update(any(), any());
		verify(tradeMapper, times(1)).update(any(), any());
		verify(adapayDivFeeMapper, times(1)).insert(ArgumentMatchers.<AdapayDivFee>any());
	}
}
