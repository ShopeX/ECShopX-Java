package cn.shopex.ecshopx.aftersales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.dto.AftersalesApplyParams;
import cn.shopex.ecshopx.aftersales.jushuitan.JushuitanTradeAftersalesBusPayloadBuilder;
import cn.shopex.ecshopx.aftersales.wdterp.WdtErpTradeAfterSaleBusPayloadBuilder;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.port.AftersalesApplyAsyncPort;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.dispatch.InvoiceRedJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.SendAfterSaleWaitDealNoticeJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesSaasErpDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeRefundDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeAfterSaleDispatchPublisher;
import cn.shopex.ecshopx.common.event.SaasErpRefundSpringEvent;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesBrokeragePort;
import cn.shopex.ecshopx.common.port.distribution.DistributorAftersalesAddressDetailReadPort;
import cn.shopex.ecshopx.common.port.distribution.OfflineAftersalesDistributorHeadReadPort;
import cn.shopex.ecshopx.common.port.order.OrderAssociationReadPort;
import cn.shopex.ecshopx.common.port.order.OrderItemsProfitWritePort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderHeaderReadPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderItemsReadPort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.port.order.OrderSuccessTradeReadPort;
import cn.shopex.ecshopx.common.port.order.OrderValidityPlatformSettingReadPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

@DisplayName("Merchant admin aftersales apply — post-commit trade-refund dispatch publisher probe (Mockito harness)")
@ExtendWith(MockitoExtension.class)
class AdminAftersalesApplyNoticePublisherProbeTest {

	private static final long COMPANY_ID = 9001L;
	private static final long ORDER_ID = 5001L;
	private static final long USER_ID = 3001L;
	private static final long SUB_ORDER_ID = 101L;

	@Mock private SendAfterSaleWaitDealNoticeJobDispatchPublisher noticePublisher;

	/** Set by {@link #runMerchantV1OnlyRefundApplyThroughApplyService()} for the current test method. */
	private MerchantV1OnlyRefundApplyProbeHarness merchantV1ProbeHarness;

	private record MerchantV1OnlyRefundApplyProbeHarness(
			TradeRefundDispatchPublisher tradeRefundDispatchPublisher,
			HashOperations<String, Object, Object> hashOps,
			JushuitanTradeAftersalesDispatchPublisher jushuitanTradeAftersalesDispatchPublisher,
			ApplicationEventPublisher eventPublisher,
			AftersalesApplyAsyncPort asyncPort) {}

	/**
	 * Shared path: {@link AftersalesApplyService#apply} with ONLY_REFUND and admin JWT — merchant V1
	 * {@code POST /api/v1/aftersales/apply} stack (probe; no servlet container).
	 */
	private void runMerchantV1OnlyRefundApplyThroughApplyService() {
		OrderNormalOrderHeaderReadPort headerPort = mock(OrderNormalOrderHeaderReadPort.class);
		OrderNormalOrderItemsReadPort itemsPort = mock(OrderNormalOrderItemsReadPort.class);
		AftersalesApplyDetailQueryService detailQuery = mock(AftersalesApplyDetailQueryService.class);

		when(headerPort.getHeader(eq(COMPANY_ID), eq(ORDER_ID))).thenReturn(Optional.of(orderHeader()));
		when(itemsPort.listItems(eq(COMPANY_ID), eq(ORDER_ID))).thenReturn(List.of(orderLine()));

		when(detailQuery.sumAppliedNum(anyLong(), anyLong(), anyLong())).thenReturn(0);
		when(detailQuery.sumAppliedRefundFee(anyLong(), anyLong(), anyLong())).thenReturn(0);
		when(detailQuery.sumAppliedRefundPoint(anyLong(), anyLong(), anyLong())).thenReturn(0);

		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(any())).thenReturn(new SimpleTransactionStatus(true));
		doNothing().when(txMgr).commit(any());

		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		when(aftersalesMapper.insert(any(Aftersales.class))).thenReturn(1);
		when(aftersalesDetailMapper.insert(any(AftersalesDetail.class))).thenReturn(1);

		AftersalesRefundService refundService = mock(AftersalesRefundService.class);
		doAnswer(
						inv -> {
							@SuppressWarnings("unchecked")
							Map<String, Object> p = inv.getArgument(0);
							p.put("refund_bn", 88001L);
							return null;
						})
				.when(refundService)
				.createRefund(any());

		OrderItemsProfitWritePort profitWrite = mock(OrderItemsProfitWritePort.class);
		AftersalesBrokeragePort brokeragePort = mock(AftersalesBrokeragePort.class);
		JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
		when(jdbcTemplate.update(anyString(), eq(COMPANY_ID), eq(ORDER_ID))).thenReturn(1);

		OrderProcessLogPublishPort logPort = mock(OrderProcessLogPublishPort.class);
		ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
		JushuitanTradeAftersalesDispatchPublisher jushuitanTradeAftersalesDispatchPublisher =
				mock(JushuitanTradeAftersalesDispatchPublisher.class);
		WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher =
				mock(WdtErpTradeAfterSaleDispatchPublisher.class);
		InvoiceRedJobDispatchPublisher invoiceRedJobDispatchPublisher =
				mock(InvoiceRedJobDispatchPublisher.class);
		TradeRefundDispatchPublisher tradeRefundDispatchPublisher = mock(TradeRefundDispatchPublisher.class);
		TradeAftersalesDispatchPublisher tradeAftersalesDispatchPublisher =
				mock(TradeAftersalesDispatchPublisher.class);
		AftersalesApplyAsyncPort asyncPort = mock(AftersalesApplyAsyncPort.class);

		OrderValidityPlatformSettingReadPort validityRead = mock(OrderValidityPlatformSettingReadPort.class);
		when(validityRead.readPlatformSetting(COMPANY_ID)).thenReturn(Map.of());

		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked")
		HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
		when(redisTemplate.opsForHash()).thenReturn(hashOps);
		when(hashOps.increment(anyString(), anyString(), anyLong())).thenReturn(1L);

		DistributorAftersalesAddressDetailReadPort distAddrRead =
				mock(DistributorAftersalesAddressDetailReadPort.class);
		OfflineAftersalesDistributorHeadReadPort offlineHeadRead =
				mock(OfflineAftersalesDistributorHeadReadPort.class);

		AftersalesApplyShopApplyByNumHandleService handleService =
				new AftersalesApplyShopApplyByNumHandleService(
						aftersalesMapper,
						aftersalesDetailMapper,
						refundService,
						detailQuery,
						profitWrite,
						brokeragePort,
						jdbcTemplate,
						logPort,
						eventPublisher,
						new JushuitanTradeAftersalesBusPayloadBuilder(),
						jushuitanTradeAftersalesDispatchPublisher,
						invoiceRedJobDispatchPublisher,
						asyncPort,
						noticePublisher,
						validityRead,
						itemsPort,
						redisTemplate,
						new ObjectMapper(),
						txMgr,
						distAddrRead,
						offlineHeadRead,
						wdtErpTradeAfterSaleDispatchPublisher,
						new WdtErpTradeAfterSaleBusPayloadBuilder(),
						tradeRefundDispatchPublisher,
						tradeAftersalesDispatchPublisher,
						mock(ThirdPartyTradeAftersalesSaasErpDispatchPublisher.class));

		AftersalesApplyCheckApplyService checkService =
				new AftersalesApplyCheckApplyService(headerPort, itemsPort, detailQuery);

		OrderSuccessTradeReadPort tradeRead = mock(OrderSuccessTradeReadPort.class);
		when(tradeRead.primarySuccessTrade(COMPANY_ID, ORDER_ID)).thenReturn(Optional.of(successTrade()));

		AftersalesApplyShopApplyByNumService shopApplyService =
				new AftersalesApplyShopApplyByNumService(
						checkService, handleService, headerPort, tradeRead);

		ObjectMapper objectMapper = new ObjectMapper();
		OrderAssociationReadPort associationReadPort = mock(OrderAssociationReadPort.class);
		when(associationReadPort.getAssociation(COMPANY_ID, ORDER_ID))
				.thenReturn(Optional.of(Map.of("order_type", "normal", "user_id", USER_ID)));

		Validator validator = mock(Validator.class);
		when(validator.validate(any(AftersalesApplyParams.class))).thenReturn(Set.of());

		AftersalesApplyService applyService =
				new AftersalesApplyService(objectMapper, associationReadPort, shopApplyService, validator);

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA))
				.thenReturn(
						Map.of(
								"company_id", COMPANY_ID,
								"operator_type", "admin",
								"operator_id", 700L));

		AftersalesApplyParams params = new AftersalesApplyParams();
		params.setOrderId(ORDER_ID);
		List<Map<String, Object>> detail = new ArrayList<>();
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", SUB_ORDER_ID);
		row.put("num", 1);
		detail.add(row);
		params.setDetail(detail);
		params.setAftersalesType("ONLY_REFUND");
		params.setReason("probe reason");
		params.setRefundFeeRaw("100");
		params.setRefundPointRaw("0");
		params.setContact("buyer");

		applyService.apply(request, params);

		this.merchantV1ProbeHarness =
				new MerchantV1OnlyRefundApplyProbeHarness(
						tradeRefundDispatchPublisher,
						hashOps,
						jushuitanTradeAftersalesDispatchPublisher,
						eventPublisher,
						asyncPort);
	}

	/**
	 * Third-party queued listener {@code listener:thirdparty.listeners.TradeRefundSendSaasErp} consumes
	 * the same {@code EVENT_TRADE_REFUND} fan-out as this path. This test only verifies the
	 * {@code TradeRefundDispatchPublisher} payload and the post-commit side-effect dispatch shown in
	 * the harness; it does not assert every downstream Spring application event or queued job that may
	 * also subscribe to that fan-out.
	 */
	/**
	 * Explicit name lock: {@code POST /api/v1/aftersales/apply} → {@link AftersalesApplyService#apply} →
	 * {@link AftersalesApplyShopApplyByNumService#shopApplyByNum} → {@link AftersalesApplyShopApplyByNumHandleService#shopApplyByNumHandle}
	 * post-commit {@link TradeRefundDispatchPublisher#publish} once (same assertions as {@link
	 * #adminApply_afterCommit_invokesTradeRefundDispatchPublisher_once_withRefundPayloadContainingAftersalesBn()}).
	 */
	@Test
	public void adminApiV1Apply_onlyRefund_afterCommit_invokesTradeRefundDispatchPublisher_once_alignedWithShopApplyByNumHandle() {
		adminApply_afterCommit_invokesTradeRefundDispatchPublisher_once_withRefundPayloadContainingAftersalesBn();
	}

	/**
	 * Same ordering probe as {@link #merchantApiV1Apply_afterCommit_redisIncrementBeforeTradeRefundBeforeJushuitan_andPayloadHasAftersalesBnOrderIdCompanyId()},
	 * under an {@code adminApiV1Apply} name for the HTTP entry above.
	 */
	@Test
	public void adminApiV1Apply_onlyRefund_postCommit_redisThenTradeRefundThenJushuitan_ordering() {
		merchantApiV1Apply_afterCommit_redisIncrementBeforeTradeRefundBeforeJushuitan_andPayloadHasAftersalesBnOrderIdCompanyId();
	}

	@Test
	public void adminApply_afterCommit_invokesTradeRefundDispatchPublisher_once_withRefundPayloadContainingAftersalesBn() {
		runMerchantV1OnlyRefundApplyThroughApplyService();
		MerchantV1OnlyRefundApplyProbeHarness h = merchantV1ProbeHarness;

		ArgumentCaptor<Long> bnCaptor = ArgumentCaptor.forClass(Long.class);
		verify(noticePublisher, times(1)).publish(eq(COMPANY_ID), bnCaptor.capture());
		assertThat(bnCaptor.getValue()).isPositive();
		String bnStr = String.valueOf(bnCaptor.getValue());
		String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
		assertThat(bnStr).startsWith(day);
		assertThat(bnStr).hasSize(15);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> refundCap = ArgumentCaptor.forClass(Map.class);
		verify(h.tradeRefundDispatchPublisher(), times(1)).publish(refundCap.capture());
		Map<String, Object> tradeRefundPayload = refundCap.getValue();
		assertThat(tradeRefundPayload.get("aftersales_bn")).isNotNull();
		assertThat(tradeRefundPayload.get("order_id")).isNotNull();
		assertThat(tradeRefundPayload.get("company_id")).isNotNull();
		assertThat(tradeRefundPayload.get("aftersales_type")).isEqualTo("ONLY_REFUND");
		assertMerchantOnlyRefundPayloadHasUserIdAndRefundBn(tradeRefundPayload);

		verify(h.eventPublisher(), times(1)).publishEvent(any(SaasErpRefundSpringEvent.class));
		verify(h.asyncPort(), times(1)).dispatchPostCommitSideEffects(any());
	}

	/**
	 * @see #adminApply_afterCommit_invokesTradeRefundDispatchPublisher_once_withRefundPayloadContainingAftersalesBn()
	 *     for {@code listener:thirdparty.listeners.TradeRefundSendSaasErp} fan-out context.
	 */
	@DisplayName(
			"Admin POST /api/v1/aftersales/apply: after commit, trade-refund dispatch fan-out runs in post-commit ordering (redis hash increments before TradeRefundDispatchPublisher, then Jushuitan); payload carries aftersales_bn, order_id, company_id")
	@Test
	public void
			merchantApiV1Apply_afterCommit_redisIncrementBeforeTradeRefundBeforeJushuitan_andPayloadHasAftersalesBnOrderIdCompanyId() {
		runMerchantV1OnlyRefundApplyThroughApplyService();
		MerchantV1OnlyRefundApplyProbeHarness h = merchantV1ProbeHarness;

		HashOperations<String, Object, Object> hashOps = h.hashOps();
		TradeRefundDispatchPublisher tradeRefundDispatchPublisher = h.tradeRefundDispatchPublisher();
		JushuitanTradeAftersalesDispatchPublisher jushuitanTradeAftersalesDispatchPublisher =
				h.jushuitanTradeAftersalesDispatchPublisher();

		InOrder inOrder =
				inOrder(hashOps, tradeRefundDispatchPublisher, jushuitanTradeAftersalesDispatchPublisher);
		inOrder.verify(hashOps, times(2)).increment(anyString(), anyString(), eq(1L));
		inOrder.verify(tradeRefundDispatchPublisher).publish(any());
		inOrder.verify(jushuitanTradeAftersalesDispatchPublisher).publish(any());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> refundCap = ArgumentCaptor.forClass(Map.class);
		verify(tradeRefundDispatchPublisher, times(1)).publish(refundCap.capture());
		Map<String, Object> payload = refundCap.getValue();
		assertThat(payload.get("aftersales_bn")).isNotNull();
		assertThat(payload.get("order_id")).isNotNull();
		assertThat(payload.get("company_id")).isNotNull();
		assertThat(payload.get("aftersales_type")).isEqualTo("ONLY_REFUND");
		assertMerchantOnlyRefundPayloadHasUserIdAndRefundBn(payload);
		Object refundFeeRaw = payload.get("refund_fee");
		assertThat(refundFeeRaw).isNotNull();
		if (refundFeeRaw instanceof Number n) {
			assertThat(n.intValue()).isPositive();
		} else if (refundFeeRaw instanceof String s) {
			assertThat(new BigDecimal(s).compareTo(BigDecimal.ZERO)).isGreaterThan(0);
		} else {
			assertThat(refundFeeRaw.toString()).isNotBlank();
		}

		verify(noticePublisher, times(1)).publish(eq(COMPANY_ID), anyLong());
		verify(h.eventPublisher(), times(1)).publishEvent(any(SaasErpRefundSpringEvent.class));
		verify(h.asyncPort(), times(1)).dispatchPostCommitSideEffects(any());
	}

	/**
	 * Admin ONLY_REFUND path: payload keys required by {@link
	 * cn.shopex.ecshopx.thirdparty.service.saaserp.TradeRefundSendSaasErpBusService#handleTradeRefundEntities}
	 * (guards for {@code company_id}, {@code order_id}, {@code refund_bn}).
	 */
	@Test
	public void adminApply_afterCommit_tradeRefundPayload_satisfiesTradeRefundSendSaasErpBusServiceGuards() {
		runMerchantV1OnlyRefundApplyThroughApplyService();
		MerchantV1OnlyRefundApplyProbeHarness h = merchantV1ProbeHarness;

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> refundCap = ArgumentCaptor.forClass(Map.class);
		verify(h.tradeRefundDispatchPublisher(), times(1)).publish(refundCap.capture());
		Map<String, Object> p = refundCap.getValue();

		Object companyRaw = p.get("company_id");
		assertThat(companyRaw).isNotNull();
		long companyId =
				companyRaw instanceof Number n ? n.longValue() : Long.parseLong(companyRaw.toString());
		assertThat(companyId).isEqualTo(COMPANY_ID).isPositive();

		Object orderRaw = p.get("order_id");
		assertThat(orderRaw).isNotNull();
		long orderId = orderRaw instanceof Number n ? n.longValue() : Long.parseLong(orderRaw.toString());
		assertThat(orderId).isEqualTo(ORDER_ID).isPositive();

		Object refundBnRaw = p.get("refund_bn");
		assertThat(refundBnRaw).isNotNull();
		long refundBn =
				refundBnRaw instanceof Number n ? n.longValue() : Long.parseLong(refundBnRaw.toString());
		assertThat(refundBn).isPositive();

		assertThat(p.get("aftersales_type")).isEqualTo("ONLY_REFUND");
	}

	/**
	 * Merchant V1 only-refund path: {@link OrderAssociationReadPort} supplies {@link #USER_ID};
	 * {@link AftersalesRefundService#createRefund} stub injects {@code refund_bn} 88001L.
	 */
	private static void assertMerchantOnlyRefundPayloadHasUserIdAndRefundBn(Map<String, Object> payload) {
		Object userIdRaw = payload.get("user_id");
		assertThat(userIdRaw).isNotNull();
		long userId =
				userIdRaw instanceof Number n ? n.longValue() : Long.parseLong(userIdRaw.toString());
		assertThat(userId).isEqualTo(USER_ID);

		Object refundBnRaw = payload.get("refund_bn");
		assertThat(refundBnRaw).isNotNull();
		long refundBn =
				refundBnRaw instanceof Number n ? n.longValue() : Long.parseLong(refundBnRaw.toString());
		assertThat(refundBn).isEqualTo(88001L);
	}

	private static Map<String, Object> orderHeader() {
		Map<String, Object> h = new LinkedHashMap<>();
		h.put("shop_id", 11L);
		h.put("distributor_id", 22L);
		h.put("merchant_id", 0L);
		h.put("mobile", "13900000000");
		h.put("pay_type", "online");
		h.put("freight_type", "cash");
		h.put("freight_fee", 0);
		h.put("freight_point", 0);
		h.put("receipt_type", "express");
		h.put("order_status", "PAID");
		return h;
	}

	private static Map<String, Object> orderLine() {
		Map<String, Object> line = new LinkedHashMap<>();
		line.put("id", SUB_ORDER_ID);
		line.put("num", 5);
		line.put("delivery_item_num", 5);
		line.put("delivery_status", "DONE");
		line.put("cancel_item_num", 0);
		line.put("total_fee", 10_000);
		line.put("point", 0);
		line.put("item_name", "probe-item");
		line.put("goods_id", 501L);
		line.put("item_id", 601L);
		line.put("item_bn", "BN-PROBE");
		line.put("pic", "");
		line.put("order_item_type", "product");
		line.put("supplier_id", 1);
		line.put("get_points", 0);
		return line;
	}

	private static Map<String, Object> successTrade() {
		Map<String, Object> t = new LinkedHashMap<>();
		t.put("pay_type", "online");
		t.put("trade_id", "TR-PROBE");
		t.put("fee_type", "CNY");
		t.put("cur_fee_rate", "1");
		t.put("cur_fee_type", "CNY");
		t.put("cur_fee_symbol", "¥");
		return t;
	}
}
