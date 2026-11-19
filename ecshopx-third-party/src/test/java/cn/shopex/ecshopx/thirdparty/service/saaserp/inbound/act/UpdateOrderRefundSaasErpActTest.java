package cn.shopex.ecshopx.thirdparty.service.saaserp.inbound.act;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpInboundResult;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpRefundOutboundBodyPort;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpRefundStatePort;
import cn.shopex.ecshopx.common.saaserp.inbound.SaasErpTradeUpdateBodyPort;
import cn.shopex.ecshopx.thirdparty.service.saaserp.DefaultSaasErpStoreTradeUpdatePort;
import cn.shopex.ecshopx.thirdparty.service.saaserp.SaasErpMatrixOutboundClient;
import cn.shopex.ecshopx.thirdparty.service.saaserp.SaasErpStoreTradeRefundAddPort;
import cn.shopex.ecshopx.thirdparty.service.saaserp.SaasErpStoreTradeUpdatePort;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@ExtendWith(MockitoExtension.class)
class UpdateOrderRefundSaasErpActTest {

	private static final long COMPANY_ID = 10L;
	private static final long REFUND_COMPANY_ID = 20L;
	private static final long AFTERSALES_COMPANY_ID = 30L;
	private static final String PARAM_ORDER_ID = "param-order";
	private static final String REFUND_ORDER_ID = "9001";
	private static final String AFTERSALES_ORDER_ID = "8001";
	private static final String REFUND_ID = "r-1";

	@Mock
	private SaasErpRefundStatePort statePort;

	@Mock
	private SaasErpRefundOutboundBodyPort outboundBodyPort;

	@Mock
	private SaasErpTradeUpdateBodyPort tradeUpdateBodyPort;

	@Mock
	private SaasErpStoreTradeRefundAddPort refundAddPort;

	@Mock
	private SaasErpStoreTradeUpdatePort tradeUpdatePort;

	private UpdateOrderRefundSaasErpAct handler;

	@BeforeEach
	void setUp() {
		handler =
				new UpdateOrderRefundSaasErpAct(
						statePort, outboundBodyPort, tradeUpdateBodyPort, refundAddPort, tradeUpdatePort);
	}

	@Test
	void actIsOmeCreateReimburse() {
		assertEquals("ome_create_reimburse", handler.act());
	}

	@Test
	void conditionalOnStateBodyAndOutboundPorts() {
		assertNotNull(UpdateOrderRefundSaasErpAct.class.getAnnotation(Service.class));
		ConditionalOnBean conditional =
				UpdateOrderRefundSaasErpAct.class.getAnnotation(ConditionalOnBean.class);
		assertNotNull(conditional);
		assertEquals(
				Set.of(
						SaasErpRefundStatePort.class,
						SaasErpRefundOutboundBodyPort.class,
						SaasErpTradeUpdateBodyPort.class,
						SaasErpStoreTradeRefundAddPort.class,
						SaasErpStoreTradeUpdatePort.class),
				Set.of(conditional.value()));
	}

	@Test
	void missingOrderIdFailsBeforeStatus() {
		Map<String, String> params = new LinkedHashMap<>();
		params.put("status", "refund");

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertFail(result, "订单号缺少");
		verifyNoPortCalls();
	}

	@Test
	void blankOrderIdFails() {
		Map<String, String> params = baseParams("refund");
		params.put("order_id", "  ");

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertFail(result, "订单号缺少");
		verifyNoPortCalls();
	}

	@Test
	void missingStatusFails() {
		Map<String, String> params = baseParams("refund");
		params.remove("status");

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertFail(result, "退款状态缺少");
		verifyNoPortCalls();
	}

	@Test
	void blankStatusFails() {
		Map<String, String> params = baseParams("  ");

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertFail(result, "退款状态缺少");
		verifyNoPortCalls();
	}

	@Test
	void memoDigitsBeforePipeResolveAftersalesBn() {
		Map<String, String> params = baseParams("succ");
		params.put("memo", "88|note");
		params.put("refund_bn", "rb-9");
		stubAftersalesAndOrder("88", "REFUND_GOODS", "normal");

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertOkEmptyList(result);
		verify(statePort).findAftersales(COMPANY_ID, "88");
		verify(statePort, never()).findAftersalesBnByRefundBn(anyString());
		verify(statePort, never()).findRefund(anyLong(), anyString(), anyString());
		verifyNoOutbound();
	}

	@Test
	void memoNonDigitsBeforePipeAreRejected() {
		Map<String, String> params = baseParams("succ");
		params.put("memo", "abc|note");
		params.put("refund_bn", "rb-9");
		when(statePort.findAftersalesBnByRefundBn("rb-9")).thenReturn(Optional.of("AS-BN"));
		stubAftersalesAndOrder("AS-BN", "REFUND_GOODS", "normal");

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertOkEmptyList(result);
		verify(statePort).findAftersales(COMPANY_ID, "AS-BN");
		verify(statePort, never()).findAftersales(COMPANY_ID, "abc");
		verify(statePort, never()).findAftersalesBnByRefundBn(REFUND_ID);
		verifyNoOutbound();
	}

	@Test
	void directAftersalesBnStopsBeforeMemo() {
		Map<String, String> params = baseParams("succ");
		params.put("aftersales_bn", "AS-DIRECT");
		params.put("memo", "88|note");
		stubAftersalesAndOrder("AS-DIRECT", "ONLY_REFUND", "normal");

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertOkEmptyList(result);
		verify(statePort).findAftersales(COMPANY_ID, "AS-DIRECT");
		verify(statePort, never()).findAftersales(COMPANY_ID, "88");
		verify(statePort, never()).findAftersalesBnByRefundBn(anyString());
	}

	@Test
	void refundIdLookupUsedWhenRefundBnLookupIsBlank() {
		Map<String, String> params = baseParams("succ");
		params.put("refund_bn", "rb-9");
		when(statePort.findAftersalesBnByRefundBn("rb-9")).thenReturn(Optional.empty());
		when(statePort.findAftersalesBnByRefundBn(REFUND_ID)).thenReturn(Optional.of("77"));
		stubAftersalesAndOrder("77", "ONLY_REFUND", "normal");

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertOkEmptyList(result);
		verify(statePort).findAftersales(COMPANY_ID, "77");
		verify(statePort).findAftersalesBnByRefundBn("rb-9");
		verify(statePort).findAftersalesBnByRefundBn(REFUND_ID);
	}

	@Test
	void noAftersalesMissingRefundFails() {
		Map<String, String> params = baseParams("succ");
		when(statePort.findRefund(COMPANY_ID, PARAM_ORDER_ID, REFUND_ID)).thenReturn(Optional.empty());

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertFail(result, "退款单详情获取失败");
		verify(statePort, never()).findOrder(anyLong(), nullable(String.class));
		verifyNoOutbound();
	}

	@Test
	void noAftersalesMissingOrderFails() {
		Map<String, String> params = baseParams("succ");
		when(statePort.findRefund(COMPANY_ID, PARAM_ORDER_ID, REFUND_ID))
				.thenReturn(Optional.of(refundRow()));
		when(statePort.findOrder(REFUND_COMPANY_ID, REFUND_ORDER_ID)).thenReturn(Optional.empty());

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertFail(result, "未找到订单");
		verify(statePort).findOrder(REFUND_COMPANY_ID, REFUND_ORDER_ID);
		verify(statePort, never()).findOrder(COMPANY_ID, PARAM_ORDER_ID);
		verifyNoOutbound();
	}

	@Test
	void noAftersalesSuccReturnsEmptyListWithoutOutbound() {
		Map<String, String> params = baseParams("succ");
		stubRefundAndOrder(params);

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertOkEmptyList(result);
		verify(statePort).findRefund(COMPANY_ID, PARAM_ORDER_ID, REFUND_ID);
		verify(statePort).findOrder(REFUND_COMPANY_ID, REFUND_ORDER_ID);
		verify(statePort, never()).agreeRefundWithoutAftersales(anyNullableMap(), anyNullableMap());
		verify(statePort, never()).refuseRefundWithoutAftersales(anyNullableMap(), anyNullableMap());
		verifyNoOutbound();
	}

	@Test
	void noAftersalesCancelRefusesWithoutOutbound() {
		Map<String, String> params = baseParams("cancel");
		Map<String, Object> refund = stubRefundAndOrder(params);
		Object refused = Map.of("refused", true);
		when(statePort.refuseRefundWithoutAftersales(refund, params)).thenReturn(refused);

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertOk(result, refused);
		verify(statePort).refuseRefundWithoutAftersales(refund, params);
		verify(statePort, never()).agreeRefundWithoutAftersales(anyNullableMap(), anyNullableMap());
		verifyNoOutbound();
	}

	@Test
	void noAftersalesRefundTruthyCallsRefundAddAndTradeUpdate() {
		Map<String, String> params = baseParams("refund");
		Map<String, Object> refund = stubRefundAndOrder(params);
		Object agreed = Map.of("agreed", true);
		Map<String, Object> refundBody = Map.of("refund", "add");
		Map<String, Object> updateBody = Map.of("trade", "update");
		when(statePort.agreeRefundWithoutAftersales(refund, params)).thenReturn(agreed);
		when(outboundBodyPort.refundAddBody(REFUND_ID, REFUND_COMPANY_ID, REFUND_ORDER_ID, null))
				.thenReturn(refundBody);
		when(tradeUpdateBodyPort.tradeUpdateBody(REFUND_COMPANY_ID, REFUND_ORDER_ID, "normal"))
				.thenReturn(Optional.of(updateBody));

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertOk(result, agreed);
		assertEquals("12.5", params.get("money"));
		verify(outboundBodyPort).refundAddBody(eq(REFUND_ID), eq(REFUND_COMPANY_ID), eq(REFUND_ORDER_ID), isNull());
		verify(refundAddPort).callStoreTradeRefundAdd(REFUND_COMPANY_ID, refundBody);
		verify(tradeUpdateBodyPort).tradeUpdateBody(REFUND_COMPANY_ID, REFUND_ORDER_ID, "normal");
		verify(tradeUpdatePort).callStoreTradeUpdate(REFUND_COMPANY_ID, updateBody);
		verify(outboundBodyPort, never())
				.refundSendSaasErpBody(anyString(), anyLong(), anyString(), anyString());
	}

	@Test
	void noAftersalesRefundSkipsTradeUpdateWhenBodyAbsent() {
		Map<String, String> params = baseParams("refund");
		Map<String, Object> refund = stubRefundAndOrder(params);
		Map<String, Object> refundBody = Map.of("refund", "add");
		when(statePort.agreeRefundWithoutAftersales(refund, params)).thenReturn(Boolean.TRUE);
		when(outboundBodyPort.refundAddBody(REFUND_ID, REFUND_COMPANY_ID, REFUND_ORDER_ID, null))
				.thenReturn(refundBody);
		when(tradeUpdateBodyPort.tradeUpdateBody(REFUND_COMPANY_ID, REFUND_ORDER_ID, "normal"))
				.thenReturn(Optional.empty());

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertOk(result, Boolean.TRUE);
		verify(refundAddPort).callStoreTradeRefundAdd(REFUND_COMPANY_ID, refundBody);
		verify(tradeUpdatePort, never()).callStoreTradeUpdate(anyLong(), anyNullableMap());
	}

	@ParameterizedTest
	@MethodSource("falsyResults")
	void noAftersalesRefundFalsySkipsOutbound(Object falsy) {
		Map<String, String> params = baseParams("refund");
		Map<String, Object> refund = stubRefundAndOrder(params);
		when(statePort.agreeRefundWithoutAftersales(refund, params)).thenReturn(falsy);

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertOk(result, falsy);
		verifyNoOutbound();
	}

	@Test
	void noAftersalesOtherStatusFails() {
		Map<String, String> params = baseParams("closed");
		stubRefundAndOrder(params);

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertFail(result, "无效的操作");
		verify(statePort, never()).agreeRefundWithoutAftersales(anyNullableMap(), anyNullableMap());
		verify(statePort, never()).refuseRefundWithoutAftersales(anyNullableMap(), anyNullableMap());
		verifyNoOutbound();
	}

	@Test
	void missingAftersalesFails() {
		Map<String, String> params = baseParams("refund");
		params.put("aftersales_bn", "AS-MISSING");
		when(statePort.findAftersales(COMPANY_ID, "AS-MISSING")).thenReturn(Optional.empty());

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertFail(result, "售后单不存在");
		verify(statePort, never()).findOrder(anyLong(), nullable(String.class));
		verifyNoOutbound();
	}

	@Test
	void aftersalesOrderLookupUsesAftersalesCompany() {
		Map<String, String> params = baseParams("refund");
		params.put("aftersales_bn", "88");
		when(statePort.findAftersales(COMPANY_ID, "88"))
				.thenReturn(Optional.of(aftersalesRow("88", "REFUND_GOODS")));
		when(statePort.findOrder(AFTERSALES_COMPANY_ID, AFTERSALES_ORDER_ID)).thenReturn(Optional.empty());

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertFail(result, "未找到订单");
		verify(statePort).findOrder(AFTERSALES_COMPANY_ID, AFTERSALES_ORDER_ID);
		verify(statePort, never()).findOrder(COMPANY_ID, PARAM_ORDER_ID);
		verifyNoOutbound();
	}

	@Test
	void aftersalesSuccReturnsEmptyListWithoutOutbound() {
		Map<String, String> params = baseParams("succ");
		params.put("aftersales_bn", "88");
		stubAftersalesAndOrder("88", "REFUND_GOODS", "normal");

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertOkEmptyList(result);
		verify(statePort, never()).confirmRefundGoods(anyNullableMap(), anyNullableMap(), anyBoolean());
		verify(statePort, never()).reviewOnlyRefund(anyNullableMap(), anyNullableMap(), anyBoolean());
		verifyNoOutbound();
	}

	@Test
	void refundGoodsRefuseDoesNotCallOutbound() {
		Map<String, String> params = baseParams("cancel");
		params.put("aftersales_bn", "88");
		Map<String, Object> aftersales = stubAftersalesAndOrder("88", "REFUND_GOODS", "normal");
		Object refused = Map.of("refused", true);
		when(statePort.confirmRefundGoods(aftersales, params, false)).thenReturn(refused);

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertOk(result, refused);
		verify(statePort).confirmRefundGoods(aftersales, params, false);
		verify(statePort, never()).reviewOnlyRefund(anyNullableMap(), anyNullableMap(), anyBoolean());
		verifyNoOutbound();
	}

	@Test
	void refundGoodsAgreeCallsRefundAddOnly() {
		Map<String, String> params = baseParams("refund");
		params.put("aftersales_bn", "88");
		Map<String, Object> aftersales = stubAftersalesAndOrder("88", "REFUND_GOODS", "groups");
		Object confirmed = Map.of("confirmed", true);
		Map<String, Object> refundBody = Map.of("refund", "send");
		when(statePort.confirmRefundGoods(aftersales, params, true)).thenReturn(confirmed);
		when(outboundBodyPort.refundSendSaasErpBody(
						REFUND_ID, AFTERSALES_COMPANY_ID, AFTERSALES_ORDER_ID, "88"))
				.thenReturn(refundBody);

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertOk(result, confirmed);
		verify(outboundBodyPort)
				.refundSendSaasErpBody(REFUND_ID, AFTERSALES_COMPANY_ID, AFTERSALES_ORDER_ID, "88");
		verify(refundAddPort).callStoreTradeRefundAdd(AFTERSALES_COMPANY_ID, refundBody);
		verify(outboundBodyPort, never())
				.refundAddBody(anyString(), anyLong(), anyString(), nullable(String.class));
		verifyNoInteractions(tradeUpdateBodyPort, tradeUpdatePort);
	}

	@Test
	void refundGoodsAgreeFalsySkipsOutbound() {
		Map<String, String> params = baseParams("refund");
		params.put("aftersales_bn", "88");
		Map<String, Object> aftersales = stubAftersalesAndOrder("88", "REFUND_GOODS", "groups");
		when(statePort.confirmRefundGoods(aftersales, params, true)).thenReturn(0);

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertOk(result, 0);
		verifyNoOutbound();
	}

	@Test
	void onlyRefundAgreeCallsRefundAddAndTradeUpdateWithNullInfo() {
		Map<String, String> params = baseParams("refund");
		params.put("aftersales_bn", "88");
		Map<String, Object> aftersales = stubAftersalesAndOrder("88", "ONLY_REFUND", "groups");
		Object reviewed = Map.of("reviewed", true);
		Map<String, Object> refundBody = Map.of("refund", "add");
		Map<String, Object> updateBody = Map.of("trade", "update");
		when(statePort.reviewOnlyRefund(aftersales, params, true)).thenReturn(reviewed);
		when(outboundBodyPort.refundAddBody(REFUND_ID, AFTERSALES_COMPANY_ID, AFTERSALES_ORDER_ID, "88"))
				.thenReturn(refundBody);
		when(tradeUpdateBodyPort.tradeUpdateBody(AFTERSALES_COMPANY_ID, AFTERSALES_ORDER_ID, "groups"))
				.thenReturn(Optional.of(updateBody));

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertOk(result, null);
		verify(statePort).reviewOnlyRefund(aftersales, params, true);
		verify(outboundBodyPort)
				.refundAddBody(REFUND_ID, AFTERSALES_COMPANY_ID, AFTERSALES_ORDER_ID, "88");
		verify(refundAddPort).callStoreTradeRefundAdd(AFTERSALES_COMPANY_ID, refundBody);
		verify(tradeUpdateBodyPort).tradeUpdateBody(AFTERSALES_COMPANY_ID, AFTERSALES_ORDER_ID, "groups");
		verify(tradeUpdatePort).callStoreTradeUpdate(AFTERSALES_COMPANY_ID, updateBody);
		verify(outboundBodyPort, never())
				.refundSendSaasErpBody(anyString(), anyLong(), anyString(), anyString());
	}

	@Test
	void onlyRefundAgreeFalsySkipsOutboundAndInfoIsNull() {
		Map<String, String> params = baseParams("refund");
		params.put("aftersales_bn", "88");
		Map<String, Object> aftersales = stubAftersalesAndOrder("88", "ONLY_REFUND", "groups");
		when(statePort.reviewOnlyRefund(aftersales, params, true)).thenReturn(Boolean.FALSE);

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertOk(result, null);
		verifyNoOutbound();
	}

	@Test
	void onlyRefundRefuseDoesNotCallOutbound() {
		Map<String, String> params = baseParams("cancel");
		params.put("aftersales_bn", "88");
		Map<String, Object> aftersales = stubAftersalesAndOrder("88", "ONLY_REFUND", "groups");
		when(statePort.reviewOnlyRefund(aftersales, params, false)).thenReturn(Map.of("reviewed", true));

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertOk(result, null);
		verify(statePort).reviewOnlyRefund(aftersales, params, false);
		verify(statePort, never()).confirmRefundGoods(anyNullableMap(), anyNullableMap(), anyBoolean());
		verifyNoOutbound();
	}

	@Test
	void otherAftersalesTypeSucceedsWithNullInfoAndNoOutbound() {
		Map<String, String> params = baseParams("refund");
		params.put("aftersales_bn", "88");
		stubAftersalesAndOrder("88", "EXCHANGE", "normal");

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertOk(result, null);
		verify(statePort, never()).confirmRefundGoods(anyNullableMap(), anyNullableMap(), anyBoolean());
		verify(statePort, never()).reviewOnlyRefund(anyNullableMap(), anyNullableMap(), anyBoolean());
		verifyNoOutbound();
	}

	@Test
	void aftersalesInvalidStatusFailsBeforeType() {
		Map<String, String> params = baseParams("closed");
		params.put("aftersales_bn", "88");
		stubAftersalesAndOrder("88", "EXCHANGE", "normal");

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertFail(result, "无效的操作");
		verify(statePort, never()).confirmRefundGoods(anyNullableMap(), anyNullableMap(), anyBoolean());
		verify(statePort, never()).reviewOnlyRefund(anyNullableMap(), anyNullableMap(), anyBoolean());
		verifyNoOutbound();
	}

	@Test
	void outboundRuntimeExceptionStillReturnsSuccess() {
		Map<String, String> params = baseParams("refund");
		Map<String, Object> refund = stubRefundAndOrder(params);
		Object agreed = Map.of("agreed", true);
		Map<String, Object> refundBody = Map.of("refund", "add");
		when(statePort.agreeRefundWithoutAftersales(refund, params)).thenReturn(agreed);
		when(outboundBodyPort.refundAddBody(REFUND_ID, REFUND_COMPANY_ID, REFUND_ORDER_ID, null))
				.thenReturn(refundBody);
		doThrow(new RuntimeException("erp down"))
				.when(refundAddPort)
				.callStoreTradeRefundAdd(REFUND_COMPANY_ID, refundBody);
		Logger logger = (Logger) LoggerFactory.getLogger(UpdateOrderRefundSaasErpAct.class);
		Level previous = logger.getLevel();
		logger.setLevel(Level.WARN);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		try {
			SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

			assertOk(result, agreed);
			verify(refundAddPort).callStoreTradeRefundAdd(REFUND_COMPANY_ID, refundBody);
			verifyNoInteractions(tradeUpdateBodyPort, tradeUpdatePort);
			assertTrue(
					appender.list.stream()
							.anyMatch(
									event ->
											event.getLevel() == Level.WARN
													&& event.getFormattedMessage()
															.contains("ome_create_reimburse outbound failed")
													&& event.getThrowableProxy() != null
													&& "erp down".equals(event.getThrowableProxy().getMessage())));
		} finally {
			logger.detachAppender(appender);
			appender.stop();
			logger.setLevel(previous);
		}
	}

	@Test
	void statePortExceptionReturnsFailWithMessage() {
		Map<String, String> params = baseParams("refund");
		when(statePort.findRefund(COMPANY_ID, PARAM_ORDER_ID, REFUND_ID))
				.thenThrow(new IllegalStateException("db down"));

		SaasErpInboundResult result = handler.handle(params, COMPANY_ID);

		assertFail(result, "db down");
		verifyNoOutbound();
	}

	@Test
	void defaultTradeUpdatePortDelegatesToMatrixClient() {
		assertNotNull(DefaultSaasErpStoreTradeUpdatePort.class.getAnnotation(Component.class));
		SaasErpMatrixOutboundClient client = mock(SaasErpMatrixOutboundClient.class);
		DefaultSaasErpStoreTradeUpdatePort port = new DefaultSaasErpStoreTradeUpdatePort(client);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("k", "v");
		port.callStoreTradeUpdate(0L, body);
		port.callStoreTradeUpdate(20L, Map.of());
		port.callStoreTradeUpdate(20L, body);
		verify(client).call(20L, "store.trade.update", body);
	}

	static Stream<Object> falsyResults() {
		return Stream.of(null, Boolean.FALSE, 0, 0.0d, "", Map.of(), List.of());
	}

	private Map<String, Object> stubRefundAndOrder(Map<String, String> params) {
		Map<String, Object> refund = refundRow();
		when(statePort.findRefund(COMPANY_ID, params.get("order_id"), params.get("refund_id")))
				.thenReturn(Optional.of(refund));
		when(statePort.findOrder(REFUND_COMPANY_ID, REFUND_ORDER_ID))
				.thenReturn(Optional.of(orderRow(REFUND_COMPANY_ID, REFUND_ORDER_ID, "normal")));
		return refund;
	}

	private Map<String, Object> stubAftersalesAndOrder(String bn, String type, String orderClass) {
		Map<String, Object> aftersales = aftersalesRow(bn, type);
		when(statePort.findAftersales(COMPANY_ID, bn)).thenReturn(Optional.of(aftersales));
		when(statePort.findOrder(AFTERSALES_COMPANY_ID, AFTERSALES_ORDER_ID))
				.thenReturn(Optional.of(orderRow(AFTERSALES_COMPANY_ID, AFTERSALES_ORDER_ID, orderClass)));
		return aftersales;
	}

	private static Map<String, String> baseParams(String status) {
		Map<String, String> params = new LinkedHashMap<>();
		params.put("order_id", PARAM_ORDER_ID);
		params.put("status", status);
		params.put("refund_id", REFUND_ID);
		params.put("money", "12.5");
		return params;
	}

	private static Map<String, Object> refundRow() {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("company_id", REFUND_COMPANY_ID);
		row.put("order_id", REFUND_ORDER_ID);
		return row;
	}

	private static Map<String, Object> orderRow(long companyId, String orderId, String orderClass) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("company_id", companyId);
		row.put("order_id", orderId);
		row.put("order_class", orderClass);
		return row;
	}

	private static Map<String, Object> aftersalesRow(String bn, String type) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("company_id", AFTERSALES_COMPANY_ID);
		row.put("order_id", AFTERSALES_ORDER_ID);
		row.put("aftersales_bn", bn);
		row.put("aftersales_type", type);
		return row;
	}

	@SuppressWarnings("unchecked")
	private static <T> T anyNullableMap() {
		return (T) nullable(Map.class);
	}

	private void verifyNoPortCalls() {
		verifyNoInteractions(
				statePort, outboundBodyPort, tradeUpdateBodyPort, refundAddPort, tradeUpdatePort);
	}

	private void verifyNoOutbound() {
		verifyNoInteractions(outboundBodyPort, tradeUpdateBodyPort, refundAddPort, tradeUpdatePort);
	}

	private static void assertFail(SaasErpInboundResult result, String msg) {
		assertEquals("fail", result.status());
		assertEquals(msg, result.msg());
		assertNull(result.info());
	}

	private static void assertOkEmptyList(SaasErpInboundResult result) {
		assertOk(result, List.of());
	}

	private static void assertOk(SaasErpInboundResult result, Object info) {
		assertEquals("true", result.status());
		assertEquals("操作成功", result.msg());
		assertEquals(info, result.info());
	}
}
