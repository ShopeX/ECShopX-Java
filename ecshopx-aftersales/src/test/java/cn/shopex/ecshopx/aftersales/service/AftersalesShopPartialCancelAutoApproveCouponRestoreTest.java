package cn.shopex.ecshopx.aftersales.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.jushuitan.JushuitanTradeAftersalesBusPayloadBuilder;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesRefundMapper;
import cn.shopex.ecshopx.aftersales.port.AftersalesRefundAsyncPort;
import cn.shopex.ecshopx.aftersales.support.AftersalesRefundEntityTradeRefundPayloadMapper;
import cn.shopex.ecshopx.aftersales.wdterp.WdtErpTradeAfterSaleBusPayloadBuilder;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeRefundDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeAfterSaleDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesAgreedCouponRestorePort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderHeaderReadPort;
import cn.shopex.ecshopx.common.port.order.OrderNormalOrderItemsReadPort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.port.order.OrderSuccessTradeReadPort;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class AftersalesShopPartialCancelAutoApproveCouponRestoreTest {

	private static final long COMPANY_ID = 9001L;
	private static final long ORDER_ID = 5001L;
	private static final long AFTERSALES_BN = 202601011234567L;

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Aftersales.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), AftersalesRefund.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), AftersalesDetail.class);
	}

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clear();
		}
	}

	@Test
	void autoApproveOnlyRefund_onlyRefund_restoresCouponsAfterDetailUpdate() {
		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		AftersalesRefundMapper aftersalesRefundMapper = mock(AftersalesRefundMapper.class);
		OrderSuccessTradeReadPort orderSuccessTradeReadPort = mock(OrderSuccessTradeReadPort.class);
		OrderNormalOrderItemsReadPort orderNormalOrderItemsReadPort = mock(OrderNormalOrderItemsReadPort.class);
		OrderNormalOrderHeaderReadPort orderNormalOrderHeaderReadPort = mock(OrderNormalOrderHeaderReadPort.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		AftersalesRefundAsyncPort aftersalesRefundAsyncPort = mock(AftersalesRefundAsyncPort.class);
		StringRedisTemplate sharedStringRedisTemplate = mock(StringRedisTemplate.class);
		JushuitanTradeAftersalesDispatchPublisher jushuitanTradeAftersalesDispatchPublisher =
				mock(JushuitanTradeAftersalesDispatchPublisher.class);
		WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher =
				mock(WdtErpTradeAfterSaleDispatchPublisher.class);
		TradeRefundDispatchPublisher tradeRefundDispatchPublisher = mock(TradeRefundDispatchPublisher.class);
		AftersalesRefundEntityTradeRefundPayloadMapper tradeRefundPayloadMapper =
				new AftersalesRefundEntityTradeRefundPayloadMapper();
		AftersalesAgreedCouponRestorePort aftersalesAgreedCouponRestorePort =
				mock(AftersalesAgreedCouponRestorePort.class);

		Aftersales row = new Aftersales();
		row.setCompanyId(COMPANY_ID);
		row.setAftersalesBn(AFTERSALES_BN);
		row.setOrderId(ORDER_ID);
		row.setAftersalesType("ONLY_REFUND");
		row.setAftersalesStatus(0);
		row.setProgress(0);
		row.setRefundFee(100);
		row.setRefundPoint(0);
		row.setFreight(0);

		when(aftersalesMapper.selectOne(any())).thenReturn(row);
		when(aftersalesRefundMapper.update(any(), any())).thenReturn(1);
		when(aftersalesMapper.update(any(), any())).thenReturn(1);
		when(aftersalesDetailMapper.update(any(), any())).thenReturn(1);

		JushuitanTradeAftersalesBusPayloadBuilder jushuitanTradeAftersalesBusPayloadBuilder =
				new JushuitanTradeAftersalesBusPayloadBuilder();
		WdtErpTradeAfterSaleBusPayloadBuilder wdtErpTradeAfterSaleBusPayloadBuilder =
				new WdtErpTradeAfterSaleBusPayloadBuilder();
		AftersalesShopPartialCancelCreateService service =
				new AftersalesShopPartialCancelCreateService(
						aftersalesMapper,
						aftersalesDetailMapper,
						aftersalesRefundMapper,
						orderSuccessTradeReadPort,
						orderNormalOrderItemsReadPort,
						orderNormalOrderHeaderReadPort,
						orderProcessLogPublishPort,
						aftersalesRefundAsyncPort,
						sharedStringRedisTemplate,
						jushuitanTradeAftersalesDispatchPublisher,
						jushuitanTradeAftersalesBusPayloadBuilder,
						wdtErpTradeAfterSaleDispatchPublisher,
						wdtErpTradeAfterSaleBusPayloadBuilder,
						tradeRefundDispatchPublisher,
						tradeRefundPayloadMapper,
						aftersalesAgreedCouponRestorePort);

		service.autoApproveOnlyRefund(
				COMPANY_ID, AFTERSALES_BN, 100, 0, 0, "admin", 700L, 3001L);

		InOrder order = inOrder(aftersalesDetailMapper, aftersalesAgreedCouponRestorePort);
		order.verify(aftersalesDetailMapper).update(isNull(), any());
		order.verify(aftersalesAgreedCouponRestorePort).restoreOnRefundAgreed(COMPANY_ID, ORDER_ID);
	}

	@Test
	void autoApproveOnlyRefund_whenWrongType_doesNotRestoreCoupons() {
		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		AftersalesRefundMapper aftersalesRefundMapper = mock(AftersalesRefundMapper.class);
		OrderSuccessTradeReadPort orderSuccessTradeReadPort = mock(OrderSuccessTradeReadPort.class);
		OrderNormalOrderItemsReadPort orderNormalOrderItemsReadPort = mock(OrderNormalOrderItemsReadPort.class);
		OrderNormalOrderHeaderReadPort orderNormalOrderHeaderReadPort = mock(OrderNormalOrderHeaderReadPort.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		AftersalesRefundAsyncPort aftersalesRefundAsyncPort = mock(AftersalesRefundAsyncPort.class);
		StringRedisTemplate sharedStringRedisTemplate = mock(StringRedisTemplate.class);
		JushuitanTradeAftersalesDispatchPublisher jushuitanTradeAftersalesDispatchPublisher =
				mock(JushuitanTradeAftersalesDispatchPublisher.class);
		WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher =
				mock(WdtErpTradeAfterSaleDispatchPublisher.class);
		TradeRefundDispatchPublisher tradeRefundDispatchPublisher = mock(TradeRefundDispatchPublisher.class);
		AftersalesRefundEntityTradeRefundPayloadMapper tradeRefundPayloadMapper =
				new AftersalesRefundEntityTradeRefundPayloadMapper();
		AftersalesAgreedCouponRestorePort aftersalesAgreedCouponRestorePort =
				mock(AftersalesAgreedCouponRestorePort.class);

		Aftersales row = new Aftersales();
		row.setCompanyId(COMPANY_ID);
		row.setAftersalesBn(AFTERSALES_BN);
		row.setOrderId(ORDER_ID);
		row.setAftersalesType("REFUND_GOODS");
		row.setAftersalesStatus(0);

		when(aftersalesMapper.selectOne(any())).thenReturn(row);

		JushuitanTradeAftersalesBusPayloadBuilder jushuitanTradeAftersalesBusPayloadBuilder =
				new JushuitanTradeAftersalesBusPayloadBuilder();
		WdtErpTradeAfterSaleBusPayloadBuilder wdtErpTradeAfterSaleBusPayloadBuilder =
				new WdtErpTradeAfterSaleBusPayloadBuilder();
		AftersalesShopPartialCancelCreateService service =
				new AftersalesShopPartialCancelCreateService(
						aftersalesMapper,
						aftersalesDetailMapper,
						aftersalesRefundMapper,
						orderSuccessTradeReadPort,
						orderNormalOrderItemsReadPort,
						orderNormalOrderHeaderReadPort,
						orderProcessLogPublishPort,
						aftersalesRefundAsyncPort,
						sharedStringRedisTemplate,
						jushuitanTradeAftersalesDispatchPublisher,
						jushuitanTradeAftersalesBusPayloadBuilder,
						wdtErpTradeAfterSaleDispatchPublisher,
						wdtErpTradeAfterSaleBusPayloadBuilder,
						tradeRefundDispatchPublisher,
						tradeRefundPayloadMapper,
						aftersalesAgreedCouponRestorePort);

		assertThatThrownBy(
						() ->
								service.autoApproveOnlyRefund(
										COMPANY_ID, AFTERSALES_BN, 0, 0, 0, "admin", 700L, 3001L))
				.isInstanceOf(ResourceException.class)
				.hasMessageContaining("售后类型不支持自动审核");

		verify(aftersalesAgreedCouponRestorePort, never()).restoreOnRefundAgreed(COMPANY_ID, ORDER_ID);
	}
}
