package cn.shopex.ecshopx.aftersales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.domain.AftersalesRefund;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.jushuitan.JushuitanTradeAftersalesBusPayloadBuilder;
import cn.shopex.ecshopx.aftersales.port.AftersalesRefundAsyncPort;
import cn.shopex.ecshopx.aftersales.wdterp.WdtErpTradeAfterSaleBusPayloadBuilder;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesSaasErpDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeAfterSaleDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesAgreedCouponRestorePort;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesBrokeragePort;
import cn.shopex.ecshopx.common.port.order.JushuitanSettingReadPort;
import cn.shopex.ecshopx.common.port.order.NormalOrderLeftAftersalesWritePort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class AftersalesRefundConfirmAgreedCouponRestoreTest {

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
	@DisplayName("同意退款：状态停在 1/9 并在明细更新后恢复优惠券")
	void agreeRefund_keepsStatus1Progress9_andRestoresCouponsAfterDetailUpdate() {
		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		AftersalesRefundService aftersalesRefundService = mock(AftersalesRefundService.class);
		AftersalesRefundAsyncPort aftersalesRefundAsyncPort = mock(AftersalesRefundAsyncPort.class);
		JushuitanSettingReadPort jushuitanSettingReadPort = mock(JushuitanSettingReadPort.class);
		AftersalesBrokeragePort aftersalesBrokeragePort = mock(AftersalesBrokeragePort.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		NormalOrderLeftAftersalesWritePort normalOrderLeftAftersalesWritePort =
				mock(NormalOrderLeftAftersalesWritePort.class);
		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		AftersalesAgreedCouponRestorePort aftersalesAgreedCouponRestorePort =
				mock(AftersalesAgreedCouponRestorePort.class);
		ObjectMapper objectMapper = new ObjectMapper();

		Aftersales aftersales = new Aftersales();
		aftersales.setCompanyId(1L);
		aftersales.setAftersalesBn(88L);
		aftersales.setOrderId(10L);
		aftersales.setAftersalesType("REFUND_GOODS");
		aftersales.setAftersalesStatus(1);
		aftersales.setProgress(2);
		aftersales.setRefundFee(100);
		aftersales.setRefundPoint(0);
		aftersales.setUserId(0L);

		AftersalesRefund refund = new AftersalesRefund();
		refund.setRefundStatus("READY");
		refund.setPayType("wx");

		AftersalesDetail detailRow = new AftersalesDetail();
		detailRow.setCompanyId(1L);
		detailRow.setAftersalesBn(88L);
		detailRow.setNum(1);

		when(aftersalesMapper.selectOne(any())).thenReturn(aftersales);
		when(aftersalesRefundService.findRefundByAftersalesBn(1L, 88L)).thenReturn(refund);
		when(jushuitanSettingReadPort.readJushuitanSetting(1L)).thenReturn(Map.of("is_open", false));
		when(aftersalesRefundService.updateRefundByAftersalesKeys(anyLong(), anyLong(), any())).thenReturn(1);
		when(aftersalesMapper.updateById(any(Aftersales.class))).thenReturn(1);
		when(aftersalesDetailMapper.selectList(any())).thenReturn(List.of(detailRow));
		when(aftersalesDetailMapper.update(isNull(), any())).thenReturn(1);

		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(any()))
				.thenAnswer(inv -> {
					if (!TransactionSynchronizationManager.isSynchronizationActive()) {
						TransactionSynchronizationManager.initSynchronization();
					}
					return new SimpleTransactionStatus(true);
				});
		doAnswer(inv -> {
					if (TransactionSynchronizationManager.isSynchronizationActive()) {
						for (TransactionSynchronization synchronization :
								TransactionSynchronizationManager.getSynchronizations()) {
							synchronization.afterCommit();
						}
						TransactionSynchronizationManager.clear();
					}
					return null;
				})
				.when(txMgr)
				.commit(any());

		AftersalesRefundConfirmService service =
				new AftersalesRefundConfirmService(
						aftersalesMapper,
						aftersalesDetailMapper,
						aftersalesRefundService,
						aftersalesRefundAsyncPort,
						jushuitanSettingReadPort,
						aftersalesBrokeragePort,
						orderProcessLogPublishPort,
						normalOrderLeftAftersalesWritePort,
						applicationEventPublisher,
						objectMapper,
						txMgr,
						mock(JushuitanTradeAftersalesDispatchPublisher.class),
						new JushuitanTradeAftersalesBusPayloadBuilder(),
						mock(WdtErpTradeAfterSaleDispatchPublisher.class),
						new WdtErpTradeAfterSaleBusPayloadBuilder(),
						mock(ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher.class),
						mock(ThirdPartyTradeAftersalesSaasErpDispatchPublisher.class),
						aftersalesAgreedCouponRestorePort);

		Map<String, Object> param = new LinkedHashMap<>();
		param.put("company_id", 1L);
		param.put("aftersales_bn", 88L);
		param.put("check_refund", true);
		param.put("operator_id", 1L);

		service.confirmRefund(param);

		assertThat(aftersales.getAftersalesStatus()).isEqualTo(1);
		assertThat(aftersales.getProgress()).isEqualTo(9);
		verify(aftersalesAgreedCouponRestorePort).restoreOnRefundAgreed(1L, 10L);

		InOrder order = inOrder(aftersalesDetailMapper, aftersalesAgreedCouponRestorePort);
		order.verify(aftersalesDetailMapper).update(isNull(), any());
		order.verify(aftersalesAgreedCouponRestorePort).restoreOnRefundAgreed(1L, 10L);
	}

	@Test
	@DisplayName("progress=9 时再次确认抛出售后单已同意退款且不恢复优惠券")
	void reentryWhenProgress9_throwsResourceException_andDoesNotRestoreCoupons() {
		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AftersalesAgreedCouponRestorePort aftersalesAgreedCouponRestorePort =
				mock(AftersalesAgreedCouponRestorePort.class);

		Aftersales aftersales = new Aftersales();
		aftersales.setCompanyId(1L);
		aftersales.setAftersalesBn(88L);
		aftersales.setOrderId(10L);
		aftersales.setAftersalesType("REFUND_GOODS");
		aftersales.setAftersalesStatus(1);
		aftersales.setProgress(9);
		aftersales.setRefundFee(100);

		AftersalesRefundService aftersalesRefundService = mock(AftersalesRefundService.class);
		when(aftersalesMapper.selectOne(any())).thenReturn(aftersales);

		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);

		AftersalesRefundConfirmService service =
				new AftersalesRefundConfirmService(
						aftersalesMapper,
						mock(AftersalesDetailMapper.class),
						aftersalesRefundService,
						mock(AftersalesRefundAsyncPort.class),
						mock(JushuitanSettingReadPort.class),
						mock(AftersalesBrokeragePort.class),
						mock(OrderProcessLogPublishPort.class),
						mock(NormalOrderLeftAftersalesWritePort.class),
						mock(ApplicationEventPublisher.class),
						new ObjectMapper(),
						txMgr,
						mock(JushuitanTradeAftersalesDispatchPublisher.class),
						new JushuitanTradeAftersalesBusPayloadBuilder(),
						mock(WdtErpTradeAfterSaleDispatchPublisher.class),
						new WdtErpTradeAfterSaleBusPayloadBuilder(),
						mock(ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher.class),
						mock(ThirdPartyTradeAftersalesSaasErpDispatchPublisher.class),
						aftersalesAgreedCouponRestorePort);

		Map<String, Object> param = new LinkedHashMap<>();
		param.put("company_id", 1L);
		param.put("aftersales_bn", 88L);
		param.put("check_refund", true);

		assertThatThrownBy(() -> service.confirmRefund(param))
				.isInstanceOf(ResourceException.class)
				.hasMessage("售后单已同意退款");
		verify(aftersalesAgreedCouponRestorePort, never()).restoreOnRefundAgreed(anyLong(), anyLong());
	}

	@Test
	@DisplayName("拒绝退款不调用优惠券恢复端口")
	void rejectRefund_doesNotRestoreCoupons() {
		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		AftersalesRefundService aftersalesRefundService = mock(AftersalesRefundService.class);
		AftersalesAgreedCouponRestorePort aftersalesAgreedCouponRestorePort =
				mock(AftersalesAgreedCouponRestorePort.class);

		Aftersales aftersales = new Aftersales();
		aftersales.setCompanyId(1L);
		aftersales.setAftersalesBn(88L);
		aftersales.setOrderId(10L);
		aftersales.setAftersalesType("ONLY_REFUND");
		aftersales.setAftersalesStatus(1);
		aftersales.setProgress(2);
		aftersales.setRefundFee(100);

		AftersalesRefund refund = new AftersalesRefund();
		refund.setRefundStatus("READY");

		when(aftersalesMapper.selectOne(any())).thenReturn(aftersales);
		when(aftersalesRefundService.findRefundByAftersalesBn(1L, 88L)).thenReturn(refund);
		when(aftersalesRefundService.updateRefundByAftersalesKeys(anyLong(), anyLong(), any())).thenReturn(1);
		when(aftersalesMapper.updateById(any(Aftersales.class))).thenReturn(1);
		when(aftersalesDetailMapper.selectList(any())).thenReturn(List.of());

		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(any()))
				.thenAnswer(inv -> {
					if (!TransactionSynchronizationManager.isSynchronizationActive()) {
						TransactionSynchronizationManager.initSynchronization();
					}
					return new SimpleTransactionStatus(true);
				});
		doAnswer(inv -> {
					if (TransactionSynchronizationManager.isSynchronizationActive()) {
						for (TransactionSynchronization synchronization :
								TransactionSynchronizationManager.getSynchronizations()) {
							synchronization.afterCommit();
						}
						TransactionSynchronizationManager.clear();
					}
					return null;
				})
				.when(txMgr)
				.commit(any());

		AftersalesRefundConfirmService service =
				new AftersalesRefundConfirmService(
						aftersalesMapper,
						aftersalesDetailMapper,
						aftersalesRefundService,
						mock(AftersalesRefundAsyncPort.class),
						mock(JushuitanSettingReadPort.class),
						mock(AftersalesBrokeragePort.class),
						mock(OrderProcessLogPublishPort.class),
						mock(NormalOrderLeftAftersalesWritePort.class),
						mock(ApplicationEventPublisher.class),
						new ObjectMapper(),
						txMgr,
						mock(JushuitanTradeAftersalesDispatchPublisher.class),
						new JushuitanTradeAftersalesBusPayloadBuilder(),
						mock(WdtErpTradeAfterSaleDispatchPublisher.class),
						new WdtErpTradeAfterSaleBusPayloadBuilder(),
						mock(ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher.class),
						mock(ThirdPartyTradeAftersalesSaasErpDispatchPublisher.class),
						aftersalesAgreedCouponRestorePort);

		Map<String, Object> param = new LinkedHashMap<>();
		param.put("company_id", 1L);
		param.put("aftersales_bn", 88L);
		param.put("check_refund", false);
		param.put("refunds_memo", "不同意");

		service.confirmRefund(param);

		verify(aftersalesAgreedCouponRestorePort, never()).restoreOnRefundAgreed(anyLong(), anyLong());
	}
}
