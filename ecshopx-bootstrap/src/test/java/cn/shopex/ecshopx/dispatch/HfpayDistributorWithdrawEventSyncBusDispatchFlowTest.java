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
import cn.shopex.ecshopx.hfpay.dispatch.DistributorWithdrawDispatchListener;
import cn.shopex.ecshopx.hfpay.dispatch.HfpayDistributorWithdrawDispatchExecutionService;
import cn.shopex.ecshopx.hfpay.domain.HfpayCashRecord;
import cn.shopex.ecshopx.hfpay.mapper.HfpayCashRecordMapper;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayAcouJsonPostClient;
import cn.shopex.ecshopx.hfpay.service.payment.HfPayPaymentSettingService;
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

class HfpayDistributorWithdrawEventSyncBusDispatchFlowTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), HfpayCashRecord.class);
	}

	@Test
	void publishDistributorWithdraw_sync_realListener_invokesCash01AndUpdatesCashRecord() {
		HfpayCashRecordMapper cashRecordMapper = mock(HfpayCashRecordMapper.class);
		HfPayPaymentSettingService paymentSettingService = mock(HfPayPaymentSettingService.class);
		HfPayAcouJsonPostClient acouJsonPostClient = mock(HfPayAcouJsonPostClient.class);

		HfpayCashRecord data = new HfpayCashRecord();
		data.setHfpayCashRecordId(90L);
		data.setCompanyId(1L);
		data.setUserCustId("uc");
		data.setOrderId("OID");
		data.setBindCardId("b1");
		data.setCashType("T1");
		data.setTransAmt(1000);
		when(cashRecordMapper.selectById(90L)).thenReturn(data);

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
		HfpayDistributorWithdrawDispatchExecutionService execution =
				new HfpayDistributorWithdrawDispatchExecutionService(
						cashRecordMapper, paymentSettingService, acouJsonPostClient, om, "", "");
		DistributorWithdrawDispatchListener listener = new DistributorWithdrawDispatchListener(execution);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		registry.registerEventListener(
				HfpayDispatchEventNames.EVENT_HFPAY_DISTRIBUTOR_WITHDRAW,
				HfpayDispatchEventNames.LISTENER_HFPAY_DISTRIBUTOR_WITHDRAW,
				ListenerDispatchOptions.syncDefaults(),
				listener);

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("hfpay_cash_record_id", 90L);
		entities.put("company_id", 1L);
		entities.put("distributor_id", 2L);
		entities.put("trans_amt", 1000L);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		assertDoesNotThrow(
				() ->
						facade.publishEvent(
								HfpayDispatchEventNames.EVENT_HFPAY_DISTRIBUTOR_WITHDRAW,
								payload,
								DispatchOptions.eventDefaults()));

		verify(acouJsonPostClient).cash01(eq(setting), any());
		verify(cashRecordMapper).updateById(any(HfpayCashRecord.class));
		assertTrue(captured.isEmpty());
	}

	/**
	 * Manual withdraw HTTP path builds the same {@code entities} map as {@code
	 * HfpayDistributorWithdrawEventDispatchPublisherImpl}; this regression keeps listener + cash01 aligned with that
	 * contract.
	 */
	@Test
	@DisplayName("event-269 HTTP 路径载荷与 publisher 契约一致（回归）")
	void publishDistributorWithdraw_sync_regressionEvent269HttpEntitiesShape() {
		HfpayCashRecordMapper cashRecordMapper = mock(HfpayCashRecordMapper.class);
		HfPayPaymentSettingService paymentSettingService = mock(HfPayPaymentSettingService.class);
		HfPayAcouJsonPostClient acouJsonPostClient = mock(HfPayAcouJsonPostClient.class);

		HfpayCashRecord data = new HfpayCashRecord();
		data.setHfpayCashRecordId(90L);
		data.setCompanyId(1L);
		data.setUserCustId("uc");
		data.setOrderId("OID");
		data.setBindCardId("b1");
		data.setCashType("T1");
		data.setTransAmt(1000);
		when(cashRecordMapper.selectById(90L)).thenReturn(data);

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
		HfpayDistributorWithdrawDispatchExecutionService execution =
				new HfpayDistributorWithdrawDispatchExecutionService(
						cashRecordMapper, paymentSettingService, acouJsonPostClient, om, "", "");
		DistributorWithdrawDispatchListener listener = new DistributorWithdrawDispatchListener(execution);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		registry.registerEventListener(
				HfpayDispatchEventNames.EVENT_HFPAY_DISTRIBUTOR_WITHDRAW,
				HfpayDispatchEventNames.LISTENER_HFPAY_DISTRIBUTOR_WITHDRAW,
				ListenerDispatchOptions.syncDefaults(),
				listener);

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("hfpay_cash_record_id", 90L);
		entities.put("company_id", 1L);
		entities.put("distributor_id", 2L);
		entities.put("trans_amt", 1000L);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		assertDoesNotThrow(
				() ->
						facade.publishEvent(
								HfpayDispatchEventNames.EVENT_HFPAY_DISTRIBUTOR_WITHDRAW,
								payload,
								DispatchOptions.eventDefaults()));

		verify(acouJsonPostClient).cash01(eq(setting), any());
		verify(cashRecordMapper).updateById(any(HfpayCashRecord.class));
		assertTrue(captured.isEmpty());
	}

	@Test
	void publishDistributorWithdraw_sync_missingCashRecord_noCash01() {
		HfpayCashRecordMapper cashRecordMapper = mock(HfpayCashRecordMapper.class);
		HfPayPaymentSettingService paymentSettingService = mock(HfPayPaymentSettingService.class);
		HfPayAcouJsonPostClient acouJsonPostClient = mock(HfPayAcouJsonPostClient.class);
		when(cashRecordMapper.selectById(90L)).thenReturn(null);

		ObjectMapper om = new ObjectMapper();
		HfpayDistributorWithdrawDispatchExecutionService execution =
				new HfpayDistributorWithdrawDispatchExecutionService(
						cashRecordMapper, paymentSettingService, acouJsonPostClient, om, "", "");
		DistributorWithdrawDispatchListener listener = new DistributorWithdrawDispatchListener(execution);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		registry.registerEventListener(
				HfpayDispatchEventNames.EVENT_HFPAY_DISTRIBUTOR_WITHDRAW,
				HfpayDispatchEventNames.LISTENER_HFPAY_DISTRIBUTOR_WITHDRAW,
				ListenerDispatchOptions.syncDefaults(),
				listener);

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("hfpay_cash_record_id", 90L);
		entities.put("company_id", 1L);
		entities.put("distributor_id", 2L);
		entities.put("trans_amt", 1000L);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		facade.publishEvent(
				HfpayDispatchEventNames.EVENT_HFPAY_DISTRIBUTOR_WITHDRAW, payload, DispatchOptions.eventDefaults());

		verify(acouJsonPostClient, never()).cash01(any(), any());
		assertTrue(captured.isEmpty());
	}

	@Test
	void publishDistributorWithdraw_sync_transAmtBelowOne_noCash01() {
		HfpayCashRecordMapper cashRecordMapper = mock(HfpayCashRecordMapper.class);
		HfPayPaymentSettingService paymentSettingService = mock(HfPayPaymentSettingService.class);
		HfPayAcouJsonPostClient acouJsonPostClient = mock(HfPayAcouJsonPostClient.class);

		HfpayCashRecord data = new HfpayCashRecord();
		data.setHfpayCashRecordId(90L);
		data.setCompanyId(1L);
		data.setTransAmt(0);
		when(cashRecordMapper.selectById(90L)).thenReturn(data);

		ObjectMapper om = new ObjectMapper();
		HfpayDistributorWithdrawDispatchExecutionService execution =
				new HfpayDistributorWithdrawDispatchExecutionService(
						cashRecordMapper, paymentSettingService, acouJsonPostClient, om, "", "");
		DistributorWithdrawDispatchListener listener = new DistributorWithdrawDispatchListener(execution);

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		List<DispatchMessage> captured = new ArrayList<>();
		DispatchCore core = DispatchCore.asyncReady(
				registry, new SyncDispatchDriver(registry), Map.of(DispatchDriverType.REDIS, captured::add));
		DispatchFacade facade = new DispatchFacade(core, new DispatchFanOutPlanner(registry));

		registry.registerEventListener(
				HfpayDispatchEventNames.EVENT_HFPAY_DISTRIBUTOR_WITHDRAW,
				HfpayDispatchEventNames.LISTENER_HFPAY_DISTRIBUTOR_WITHDRAW,
				ListenerDispatchOptions.syncDefaults(),
				listener);

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("hfpay_cash_record_id", 90L);
		entities.put("company_id", 1L);
		entities.put("distributor_id", 2L);
		entities.put("trans_amt", 0L);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		facade.publishEvent(
				HfpayDispatchEventNames.EVENT_HFPAY_DISTRIBUTOR_WITHDRAW, payload, DispatchOptions.eventDefaults());

		verify(acouJsonPostClient, never()).cash01(any(), any());
		assertTrue(captured.isEmpty());
	}
}
