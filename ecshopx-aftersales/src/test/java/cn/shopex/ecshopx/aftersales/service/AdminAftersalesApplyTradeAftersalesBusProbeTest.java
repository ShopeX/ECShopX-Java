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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * Dispatch-bus probes for admin {@code POST /api/v1/aftersales/apply} ({@link cn.shopex.ecshopx.aftersales.api.admin.v1.AftersalesController#apply})
 * through {@link AftersalesApplyService#apply}, {@link AftersalesApplyShopApplyByNumService#shopApplyByNum}, and
 * {@link AftersalesApplyShopApplyByNumHandleService#shopApplyByNumHandle}: for {@code REFUND_GOODS} and {@code EXCHANGING_GOODS},
 * post-commit ordered publishing issues the same aftersales payload to system-link trade-aftersales then third-party SaaS ERP
 * publishers (see {@link AftersalesApplyShopApplyByNumHandleService} post-commit ordering).
 */
@ExtendWith(MockitoExtension.class)
class AdminAftersalesApplyTradeAftersalesBusProbeTest {

	private static final long COMPANY_ID = 9001L;
	private static final long ORDER_ID = 5001L;
	private static final long USER_ID = 3001L;
	private static final long SUB_ORDER_ID = 101L;

	@Mock private SendAfterSaleWaitDealNoticeJobDispatchPublisher noticePublisher;

	private RefundGoodsApplyProbeHarness refundGoodsHarness;

	private record RefundGoodsApplyProbeHarness(
			TradeAftersalesDispatchPublisher tradeAftersalesDispatchPublisher,
			HashOperations<String, Object, Object> hashOps,
			ThirdPartyTradeAftersalesSaasErpDispatchPublisher thirdPartyTradeAftersalesSaasErpDispatchPublisher,
			JushuitanTradeAftersalesDispatchPublisher jushuitanTradeAftersalesDispatchPublisher) {}

	/**
	 * Admin merchant V1 apply for {@link #performPostCommitOrdered_refundGoodsBranch_publishesTradeAftersalesBusOnce()} (
	 * {@code REFUND_GOODS}).
	 */
	private void runMerchantV1RefundGoodsApplyThroughApplyService() {
		runMerchantV1TradeAftersalesApplyThroughApplyService("REFUND_GOODS");
	}

	/**
	 * Drives {@link AftersalesApplyService#apply} with {@code aftersales_type} set to {@code REFUND_GOODS} or
	 * {@code EXCHANGING_GOODS}, matching the admin apply-by-number stack behind {@code POST /api/v1/aftersales/apply}.
	 */
	private void runMerchantV1TradeAftersalesApplyThroughApplyService(String aftersalesType) {
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
		ThirdPartyTradeAftersalesSaasErpDispatchPublisher thirdPartyTradeAftersalesSaasErpDispatchPublisher =
				mock(ThirdPartyTradeAftersalesSaasErpDispatchPublisher.class);
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
						thirdPartyTradeAftersalesSaasErpDispatchPublisher);

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
		params.setAftersalesType(aftersalesType);
		params.setReason("probe reason");
		params.setRefundFeeRaw("100");
		params.setRefundPointRaw("0");
		params.setContact("buyer");

		applyService.apply(request, params);

		this.refundGoodsHarness =
				new RefundGoodsApplyProbeHarness(
						tradeAftersalesDispatchPublisher,
						hashOps,
						thirdPartyTradeAftersalesSaasErpDispatchPublisher,
						jushuitanTradeAftersalesDispatchPublisher);
	}

	@Test
	void performPostCommitOrdered_refundGoodsBranch_publishesTradeAftersalesBusOnce() {
		runMerchantV1RefundGoodsApplyThroughApplyService();
		RefundGoodsApplyProbeHarness h = refundGoodsHarness;

		ArgumentCaptor<Long> bnCaptor = ArgumentCaptor.forClass(Long.class);
		verify(noticePublisher, times(1)).publish(eq(COMPANY_ID), bnCaptor.capture());
		assertThat(bnCaptor.getValue()).isPositive();

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> tradePayloadCap = ArgumentCaptor.forClass(Map.class);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> saasErpPayloadCap = ArgumentCaptor.forClass(Map.class);

		InOrder inOrder =
				inOrder(
						h.hashOps(),
						h.tradeAftersalesDispatchPublisher(),
						h.thirdPartyTradeAftersalesSaasErpDispatchPublisher(),
						noticePublisher,
						h.jushuitanTradeAftersalesDispatchPublisher());
		inOrder.verify(h.hashOps(), times(2)).increment(anyString(), anyString(), eq(1L));
		inOrder.verify(h.tradeAftersalesDispatchPublisher()).publish(tradePayloadCap.capture());
		inOrder.verify(h.thirdPartyTradeAftersalesSaasErpDispatchPublisher(), times(1))
				.publish(saasErpPayloadCap.capture());
		inOrder.verify(noticePublisher, times(1)).publish(eq(COMPANY_ID), anyLong());
		inOrder.verify(h.jushuitanTradeAftersalesDispatchPublisher(), times(1)).publish(any());

		Map<String, Object> tradePayload = tradePayloadCap.getValue();
		Map<String, Object> saasPayload = saasErpPayloadCap.getValue();
		assertThat(tradePayload.get("company_id")).isNotNull();
		assertThat(tradePayload.get("order_id")).isNotNull();
		assertThat(tradePayload.get("aftersales_bn")).isNotNull();
		assertThat(tradePayload.get("aftersales_type").toString()).isEqualTo("REFUND_GOODS");
		assertThat(saasPayload).isSameAs(tradePayload);
		assertThat(saasPayload.get("company_id")).isEqualTo(tradePayload.get("company_id"));
		assertThat(saasPayload.get("order_id")).isEqualTo(tradePayload.get("order_id"));
		assertThat(saasPayload.get("aftersales_bn")).isEqualTo(tradePayload.get("aftersales_bn"));
		assertThat(saasPayload.get("aftersales_type").toString()).isEqualTo(tradePayload.get("aftersales_type").toString());
	}

	@Test
	void performPostCommitOrdered_exchangingGoodsBranch_publishesTradeAftersalesBusOnce() {
		runMerchantV1TradeAftersalesApplyThroughApplyService("EXCHANGING_GOODS");
		RefundGoodsApplyProbeHarness h = refundGoodsHarness;

		ArgumentCaptor<Long> bnCaptor = ArgumentCaptor.forClass(Long.class);
		verify(noticePublisher, times(1)).publish(eq(COMPANY_ID), bnCaptor.capture());
		assertThat(bnCaptor.getValue()).isPositive();

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> tradePayloadCap = ArgumentCaptor.forClass(Map.class);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> saasErpPayloadCap = ArgumentCaptor.forClass(Map.class);

		InOrder inOrder =
				inOrder(
						h.hashOps(),
						h.tradeAftersalesDispatchPublisher(),
						h.thirdPartyTradeAftersalesSaasErpDispatchPublisher(),
						noticePublisher,
						h.jushuitanTradeAftersalesDispatchPublisher());
		inOrder.verify(h.hashOps(), times(2)).increment(anyString(), anyString(), eq(1L));
		inOrder.verify(h.tradeAftersalesDispatchPublisher()).publish(tradePayloadCap.capture());
		inOrder.verify(h.thirdPartyTradeAftersalesSaasErpDispatchPublisher(), times(1))
				.publish(saasErpPayloadCap.capture());
		inOrder.verify(noticePublisher, times(1)).publish(eq(COMPANY_ID), anyLong());
		inOrder.verify(h.jushuitanTradeAftersalesDispatchPublisher(), times(1)).publish(any());

		Map<String, Object> tradePayload = tradePayloadCap.getValue();
		Map<String, Object> saasPayload = saasErpPayloadCap.getValue();
		assertThat(tradePayload.get("company_id")).isNotNull();
		assertThat(tradePayload.get("order_id")).isNotNull();
		assertThat(tradePayload.get("aftersales_bn")).isNotNull();
		assertThat(tradePayload.get("aftersales_type").toString()).isEqualTo("EXCHANGING_GOODS");
		assertThat(saasPayload).isSameAs(tradePayload);
		assertThat(saasPayload.get("company_id")).isEqualTo(tradePayload.get("company_id"));
		assertThat(saasPayload.get("order_id")).isEqualTo(tradePayload.get("order_id"));
		assertThat(saasPayload.get("aftersales_bn")).isEqualTo(tradePayload.get("aftersales_bn"));
		assertThat(saasPayload.get("aftersales_type").toString()).isEqualTo(tradePayload.get("aftersales_type").toString());
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
