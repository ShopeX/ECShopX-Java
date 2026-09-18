package cn.shopex.ecshopx.orders.hfpay.dispatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.hfpay.service.payment.HfPayAcouJsonPostClient;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayPaymentSettingService;
import cn.shopex.ecshopx.orders.domain.MerchantPaymentTrade;
import cn.shopex.ecshopx.orders.mapper.MerchantPaymentTradeMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class HfpayPopularizeWithdrawDispatchExecutionServiceTest {

	@Test
	void respC00001_setsPayingAndHfFields() {
		MerchantPaymentTradeMapper tradeMapper = mock(MerchantPaymentTradeMapper.class);
		HfPayPaymentSettingService paymentSettingService = mock(HfPayPaymentSettingService.class);
		HfPayAcouJsonPostClient client = mock(HfPayAcouJsonPostClient.class);

		MerchantPaymentTrade trade = new MerchantPaymentTrade();
		trade.setMerchantTradeId("M1");
		trade.setCompanyId(10L);
		trade.setUserCustId("u1");
		trade.setBindCardId("bc");
		trade.setHfCashType("T1");
		trade.setAmount(500L);
		when(tradeMapper.selectById("M1")).thenReturn(trade);

		Map<String, Object> setting = new LinkedHashMap<>();
		setting.put("mer_cust_id", "mer");
		when(paymentSettingService.loadForCompany(10L)).thenReturn(setting);

		Map<String, Object> cashRes = new LinkedHashMap<>();
		cashRes.put("resp_code", "C00001");
		cashRes.put("resp_desc", "done");
		cashRes.put("order_id", "HFO");
		cashRes.put("order_date", "20240601");
		when(client.cash01(eq(setting), any())).thenReturn(cashRes);

		HfpayPopularizeWithdrawDispatchExecutionService svc =
				new HfpayPopularizeWithdrawDispatchExecutionService(
						tradeMapper, paymentSettingService, client, new ObjectMapper(), "", "");

		Map<String, Object> payload = new LinkedHashMap<>();
		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("merchant_trade_id", "M1");
		payload.put("entities", entities);

		svc.executeFromDispatchPayload(payload);

		ArgumentCaptor<MerchantPaymentTrade> cap = ArgumentCaptor.forClass(MerchantPaymentTrade.class);
		verify(tradeMapper).updateById(cap.capture());
		MerchantPaymentTrade upd = cap.getValue();
		assertEquals("PAYING", upd.getStatus());
		assertEquals("HFO", upd.getHfOrderId());
		assertEquals("20240601", upd.getHfOrderDate());
		assertEquals("C00001", upd.getErrorCode());
	}

	@Test
	void nonC00001_setsFail() {
		MerchantPaymentTradeMapper tradeMapper = mock(MerchantPaymentTradeMapper.class);
		HfPayPaymentSettingService paymentSettingService = mock(HfPayPaymentSettingService.class);
		HfPayAcouJsonPostClient client = mock(HfPayAcouJsonPostClient.class);

		MerchantPaymentTrade trade = new MerchantPaymentTrade();
		trade.setMerchantTradeId("M1");
		trade.setCompanyId(10L);
		trade.setUserCustId("u1");
		trade.setBindCardId("bc");
		trade.setHfCashType("T1");
		trade.setAmount(500L);
		when(tradeMapper.selectById("M1")).thenReturn(trade);

		Map<String, Object> setting = new LinkedHashMap<>();
		setting.put("mer_cust_id", "mer");
		when(paymentSettingService.loadForCompany(10L)).thenReturn(setting);

		Map<String, Object> cashRes = new LinkedHashMap<>();
		cashRes.put("resp_code", "E00001");
		cashRes.put("resp_desc", "fail");
		when(client.cash01(eq(setting), any())).thenReturn(cashRes);

		HfpayPopularizeWithdrawDispatchExecutionService svc =
				new HfpayPopularizeWithdrawDispatchExecutionService(
						tradeMapper, paymentSettingService, client, new ObjectMapper(), "", "");

		Map<String, Object> payload = new LinkedHashMap<>();
		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("merchant_trade_id", "M1");
		payload.put("entities", entities);

		svc.executeFromDispatchPayload(payload);

		ArgumentCaptor<MerchantPaymentTrade> cap = ArgumentCaptor.forClass(MerchantPaymentTrade.class);
		verify(tradeMapper).updateById(cap.capture());
		assertEquals("FAIL", cap.getValue().getStatus());
	}

	@Test
	void selectByIdNull_skipsCash01() {
		MerchantPaymentTradeMapper tradeMapper = mock(MerchantPaymentTradeMapper.class);
		HfPayPaymentSettingService paymentSettingService = mock(HfPayPaymentSettingService.class);
		HfPayAcouJsonPostClient client = mock(HfPayAcouJsonPostClient.class);
		when(tradeMapper.selectById("missing")).thenReturn(null);

		HfpayPopularizeWithdrawDispatchExecutionService svc =
				new HfpayPopularizeWithdrawDispatchExecutionService(
						tradeMapper, paymentSettingService, client, new ObjectMapper(), "", "");

		Map<String, Object> payload = new LinkedHashMap<>();
		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("merchant_trade_id", "missing");
		payload.put("entities", entities);

		svc.executeFromDispatchPayload(payload);

		verify(client, never()).cash01(any(), any());
		verify(tradeMapper, never()).updateById(any(MerchantPaymentTrade.class));
	}
}
