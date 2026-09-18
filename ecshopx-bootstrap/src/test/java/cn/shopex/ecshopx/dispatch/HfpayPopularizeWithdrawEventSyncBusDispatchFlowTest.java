package cn.shopex.ecshopx.dispatch;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.HfpayDispatchEventNames;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayAcouJsonPostClient;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayPaymentSettingService;
import cn.shopex.ecshopx.orders.domain.MerchantPaymentTrade;
import cn.shopex.ecshopx.orders.hfpay.dispatch.HfpayPopularizeWithdrawDispatchExecutionService;
import cn.shopex.ecshopx.orders.hfpay.dispatch.PopularizeWithdrawDispatchListener;
import cn.shopex.ecshopx.orders.mapper.MerchantPaymentTradeMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HfpayPopularizeWithdrawEventSyncBusDispatchFlowTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), MerchantPaymentTrade.class);
	}

	@Test
	void publishPopularizeWithdraw_sync_realListener_invokesCash01AndUpdatesMerchantTrade() {
		MerchantPaymentTradeMapper tradeMapper = mock(MerchantPaymentTradeMapper.class);
		HfPayPaymentSettingService paymentSettingService = mock(HfPayPaymentSettingService.class);
		HfPayAcouJsonPostClient acouJsonPostClient = mock(HfPayAcouJsonPostClient.class);

		MerchantPaymentTrade trade = new MerchantPaymentTrade();
		trade.setMerchantTradeId("MT-1");
		trade.setCompanyId(1L);
		trade.setUserCustId("uc");
		trade.setBindCardId("b1");
		trade.setHfCashType("T1");
		trade.setAmount(1000L);
		when(tradeMapper.selectById("MT-1")).thenReturn(trade);

		Map<String, Object> setting = new LinkedHashMap<>();
		setting.put("mer_cust_id", "m1");
		when(paymentSettingService.loadForCompany(1L)).thenReturn(setting);

		Map<String, Object> cashRes = new LinkedHashMap<>();
		cashRes.put("resp_code", "C00001");
		cashRes.put("resp_desc", "ok");
		cashRes.put("order_id", "HF-OID");
		cashRes.put("order_date", "20240102");
		when(acouJsonPostClient.cash01(eq(setting), any())).thenReturn(cashRes);

		ObjectMapper om = new ObjectMapper();
		HfpayPopularizeWithdrawDispatchExecutionService execution =
				new HfpayPopularizeWithdrawDispatchExecutionService(
						tradeMapper, paymentSettingService, acouJsonPostClient, om, "", "");
		PopularizeWithdrawDispatchListener listener = new PopularizeWithdrawDispatchListener(execution);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		registry.registerEventListener(
				HfpayDispatchEventNames.EVENT_HFPAY_POPULARIZE_WITHDRAW,
				HfpayDispatchEventNames.LISTENER_HFPAY_POPULARIZE_WITHDRAW,
				ListenerDispatchOptions.syncDefaults(),
				listener);

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("merchant_trade_id", "MT-1");
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		assertDoesNotThrow(
				() ->
						facade.publishEvent(
								HfpayDispatchEventNames.EVENT_HFPAY_POPULARIZE_WITHDRAW,
								payload,
								DispatchOptions.eventDefaults()));

		verify(acouJsonPostClient).cash01(eq(setting), any());
		verify(tradeMapper).updateById(any(MerchantPaymentTrade.class));
		assertTrue(captured.isEmpty());
	}

	@Test
	@DisplayName("event-270 payload matches publisher contract (regression)")
	void publishPopularizeWithdraw_sync_regressionEvent270EntitiesShape() {
		MerchantPaymentTradeMapper tradeMapper = mock(MerchantPaymentTradeMapper.class);
		HfPayPaymentSettingService paymentSettingService = mock(HfPayPaymentSettingService.class);
		HfPayAcouJsonPostClient acouJsonPostClient = mock(HfPayAcouJsonPostClient.class);

		MerchantPaymentTrade trade = new MerchantPaymentTrade();
		trade.setMerchantTradeId("MT-1");
		trade.setCompanyId(1L);
		trade.setUserCustId("uc");
		trade.setBindCardId("b1");
		trade.setHfCashType("T1");
		trade.setAmount(1000L);
		when(tradeMapper.selectById("MT-1")).thenReturn(trade);

		Map<String, Object> setting = new LinkedHashMap<>();
		setting.put("mer_cust_id", "m1");
		when(paymentSettingService.loadForCompany(1L)).thenReturn(setting);

		Map<String, Object> cashRes = new LinkedHashMap<>();
		cashRes.put("resp_code", "C00001");
		cashRes.put("resp_desc", "ok");
		cashRes.put("order_id", "HF-OID");
		cashRes.put("order_date", "20240102");
		when(acouJsonPostClient.cash01(eq(setting), any())).thenReturn(cashRes);

		ObjectMapper om = new ObjectMapper();
		HfpayPopularizeWithdrawDispatchExecutionService execution =
				new HfpayPopularizeWithdrawDispatchExecutionService(
						tradeMapper, paymentSettingService, acouJsonPostClient, om, "", "");
		PopularizeWithdrawDispatchListener listener = new PopularizeWithdrawDispatchListener(execution);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		registry.registerEventListener(
				HfpayDispatchEventNames.EVENT_HFPAY_POPULARIZE_WITHDRAW,
				HfpayDispatchEventNames.LISTENER_HFPAY_POPULARIZE_WITHDRAW,
				ListenerDispatchOptions.syncDefaults(),
				listener);

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("merchant_trade_id", "MT-1");
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		assertDoesNotThrow(
				() ->
						facade.publishEvent(
								HfpayDispatchEventNames.EVENT_HFPAY_POPULARIZE_WITHDRAW,
								payload,
								DispatchOptions.eventDefaults()));

		verify(acouJsonPostClient).cash01(eq(setting), any());
		verify(tradeMapper).updateById(any(MerchantPaymentTrade.class));
		assertTrue(captured.isEmpty());
	}

	@Test
	void publishPopularizeWithdraw_sync_missingTrade_noCash01() {
		MerchantPaymentTradeMapper tradeMapper = mock(MerchantPaymentTradeMapper.class);
		HfPayPaymentSettingService paymentSettingService = mock(HfPayPaymentSettingService.class);
		HfPayAcouJsonPostClient acouJsonPostClient = mock(HfPayAcouJsonPostClient.class);
		when(tradeMapper.selectById("MT-x")).thenReturn(null);

		ObjectMapper om = new ObjectMapper();
		HfpayPopularizeWithdrawDispatchExecutionService execution =
				new HfpayPopularizeWithdrawDispatchExecutionService(
						tradeMapper, paymentSettingService, acouJsonPostClient, om, "", "");
		PopularizeWithdrawDispatchListener listener = new PopularizeWithdrawDispatchListener(execution);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		registry.registerEventListener(
				HfpayDispatchEventNames.EVENT_HFPAY_POPULARIZE_WITHDRAW,
				HfpayDispatchEventNames.LISTENER_HFPAY_POPULARIZE_WITHDRAW,
				ListenerDispatchOptions.syncDefaults(),
				listener);

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("merchant_trade_id", "MT-x");
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		facade.publishEvent(
				HfpayDispatchEventNames.EVENT_HFPAY_POPULARIZE_WITHDRAW, payload, DispatchOptions.eventDefaults());

		verify(acouJsonPostClient, never()).cash01(any(), any());
		assertTrue(captured.isEmpty());
	}
}
