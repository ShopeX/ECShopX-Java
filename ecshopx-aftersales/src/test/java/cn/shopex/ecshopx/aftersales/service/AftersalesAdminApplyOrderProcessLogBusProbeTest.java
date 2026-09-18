package cn.shopex.ecshopx.aftersales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.dto.AftersalesApplyParams;
import cn.shopex.ecshopx.aftersales.jushuitan.JushuitanTradeAftersalesBusPayloadBuilder;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.port.AftersalesApplyAsyncPort;
import cn.shopex.ecshopx.aftersales.wdterp.WdtErpTradeAfterSaleBusPayloadBuilder;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.dispatch.InvoiceRedJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.SendAfterSaleWaitDealNoticeJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesSaasErpDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeRefundDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeAfterSaleDispatchPublisher;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * Probes {@link AftersalesApplyService#apply} → {@link AftersalesApplyShopApplyByNumService#shopApplyByNum} →
 * {@link AftersalesApplyShopApplyByNumHandleService#shopApplyByNumHandle} → {@link OrderProcessLogPublishPort#publish}
 * for the admin-console apply path, without invoking HTTP ingress.
 *
 * <p>Dispatch bus delivery of {@code EVENT_ORDER_PROCESS_LOG} (slow queue, listener path) is covered in module
 * {@code ecshopx-orders} (port implementation) and in bootstrap tests such as
 * {@code SupplierOrderPaidConfirmOrderProcessLogPublishTest}; this class only asserts the port call and payload
 * produced on the admin {@code apply} chain (parallel in structure to
 * {@link AdminAftersalesApplyTradeAftersalesBusProbeTest}).
 */
@ExtendWith(MockitoExtension.class)
class AftersalesAdminApplyOrderProcessLogBusProbeTest {

	private static final long COMPANY_ID = 9001L;
	private static final long ORDER_ID = 5001L;
	private static final long USER_ID = 3001L;
	private static final long SUB_ORDER_ID = 101L;
	private static final long ADMIN_OPERATOR_ID = 700L;

	@Mock private SendAfterSaleWaitDealNoticeJobDispatchPublisher noticePublisher;

	/** Captured from the latest {@link #runAdminApplyServiceProbe(String)} invocation. */
	private OrderProcessLogPublishPort orderProcessLogPortProbe;

	/**
	 * Admin merchant V1 apply via {@link AftersalesApplyService#apply}: same Mockito harness as
	 * {@link AdminAftersalesApplyTradeAftersalesBusProbeTest#runMerchantV1TradeAftersalesApplyThroughApplyService},
	 * with a shared mock {@link OrderProcessLogPublishPort} for assertions.
	 */
	private void runAdminApplyServiceProbe(String aftersalesType) {
		OrderProcessLogPublishPort logPort = mock(OrderProcessLogPublishPort.class);
		this.orderProcessLogPortProbe = logPort;

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
								"operator_id", ADMIN_OPERATOR_ID));

		AftersalesApplyParams params = new AftersalesApplyParams();
		params.setOrderId(ORDER_ID);
		List<Map<String, Object>> detail = new ArrayList<>();
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", SUB_ORDER_ID);
		row.put("num", 1);
		detail.add(row);
		params.setDetail(detail);
		params.setAftersalesType(aftersalesType);
		params.setReason("probe reason");
		params.setRefundFeeRaw("100");
		params.setRefundPointRaw("0");
		params.setContact("buyer");

		applyService.apply(request, params);
	}

	@Test
	void adminApply_refundGoods_invokesOrderProcessLogPublishPortOnceWithExpectedDetail() {
		runAdminApplyServiceProbe("REFUND_GOODS");
		assertSinglePublishMatchesHandleServicePayload();
	}

	@Test
	void adminApply_exchangingGoods_invokesOrderProcessLogPublishPortOnceWithExpectedDetail() {
		runAdminApplyServiceProbe("EXCHANGING_GOODS");
		assertSinglePublishMatchesHandleServicePayload();
	}

	private void assertSinglePublishMatchesHandleServicePayload() {
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> logCaptor = ArgumentCaptor.forClass(Map.class);
		verify(orderProcessLogPortProbe, times(1)).publish(logCaptor.capture());
		Map<String, Object> log = logCaptor.getValue();

		assertThat(log.get("order_id")).isEqualTo(ORDER_ID);
		assertThat(log.get("company_id")).isEqualTo(COMPANY_ID);
		assertThat(log.get("supplier_id")).isEqualTo(1L);
		assertThat(log.get("operator_type")).isEqualTo("admin");
		assertThat(log.get("operator_id")).isEqualTo(ADMIN_OPERATOR_ID);
		assertThat(log.get("remarks")).isEqualTo("订单售后");
		assertThat(log.get("detail").toString())
				.startsWith("售后单号：")
				.contains("后台申请售后")
				.contains("申请原因")
				.contains("probe reason");

		@SuppressWarnings("unchecked")
		Map<String, Object> paramsMap = (Map<String, Object>) log.get("params");
		assertThat(paramsMap).isNotNull();
		assertThat(paramsMap.get("order_id")).isEqualTo(ORDER_ID);
		assertThat(paramsMap.get("company_id")).isEqualTo(COMPANY_ID);
		assertThat(paramsMap.get("reason")).isEqualTo("probe reason");
		assertThat(paramsMap.get("operator_type")).isEqualTo("admin");
		assertThat(paramsMap.get("operator_id")).isEqualTo(ADMIN_OPERATOR_ID);
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
