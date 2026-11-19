package cn.shopex.ecshopx.aftersales.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
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
import cn.shopex.ecshopx.aftersales.port.AftersalesRefundAsyncPort;
import cn.shopex.ecshopx.aftersales.wdterp.WdtErpTradeAfterSaleBusPayloadBuilder;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesUpdateSaasErpDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeAfterSaleDispatchPublisher;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesAgreedCouponRestorePort;
import cn.shopex.ecshopx.common.port.aftersales.AftersalesBrokeragePort;
import cn.shopex.ecshopx.common.port.distribution.DistributorAftersalesAddressDetailReadPort;
import cn.shopex.ecshopx.common.port.order.NormalOrderLeftAftersalesWritePort;
import cn.shopex.ecshopx.common.port.order.NormalOrderPartialCancelRestorePort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.port.order.OrderValidityPlatformSettingReadPort;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class AftersalesReviewOnlyRefundCouponRestoreTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), Aftersales.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), AftersalesDetail.class);
	}

	@AfterEach
	void tearDownTransactionSync() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clear();
		}
	}

	@Test
	@DisplayName("仅退款审核同意：调用优惠券恢复端口")
	void onlyRefundApprove_restoresCoupons() {
		long companyId = 10L;
		long aftersalesBn = 202601011234567L;
		long orderId = 100L;

		AftersalesAgreedCouponRestorePort aftersalesAgreedCouponRestorePort =
				mock(AftersalesAgreedCouponRestorePort.class);
		ThirdPartyTradeAftersalesUpdateSaasErpDispatchPublisher thirdPartyTradeAftersalesUpdateSaasErpDispatchPublisher =
				mock(ThirdPartyTradeAftersalesUpdateSaasErpDispatchPublisher.class);
		TradeAftersalesDispatchPublisher tradeAftersalesDispatchPublisher =
				mock(TradeAftersalesDispatchPublisher.class);

		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AtomicInteger selectPass = new AtomicInteger();
		Aftersales loaded = pendingOnlyRefundRow(companyId, aftersalesBn, orderId);
		Aftersales reloaded = committedOnlyRefundRow(companyId, aftersalesBn, orderId);
		when(aftersalesMapper.selectOne(any()))
				.thenAnswer(
						inv -> {
							int n = selectPass.getAndIncrement();
							return n == 0 ? loaded : reloaded;
						});
		when(aftersalesMapper.updateById(any(Aftersales.class))).thenReturn(1);

		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		when(aftersalesDetailMapper.selectList(any())).thenReturn(List.of());
		when(aftersalesDetailMapper.selectCount(any())).thenReturn(0L);

		AftersalesRefundService aftersalesRefundService = mock(AftersalesRefundService.class);
		AftersalesRefund refund = new AftersalesRefund();
		refund.setPayType("wxpay");
		when(aftersalesRefundService.findRefundByAftersalesBn(companyId, aftersalesBn)).thenReturn(refund);
		when(aftersalesRefundService.updateRefundByAftersalesKeys(anyLong(), anyLong(), any()))
				.thenReturn(1);

		AftersalesRefundAsyncPort aftersalesRefundAsyncPort = mock(AftersalesRefundAsyncPort.class);
		AftersalesBrokeragePort aftersalesBrokeragePort = mock(AftersalesBrokeragePort.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		NormalOrderLeftAftersalesWritePort normalOrderLeftAftersalesWritePort =
				mock(NormalOrderLeftAftersalesWritePort.class);
		NormalOrderPartialCancelRestorePort normalOrderPartialCancelRestorePort =
				mock(NormalOrderPartialCancelRestorePort.class);
		OrderValidityPlatformSettingReadPort orderValidityPlatformSettingReadPort =
				mock(OrderValidityPlatformSettingReadPort.class);
		DistributorAftersalesAddressDetailReadPort distributorAftersalesAddressDetailReadPort =
				mock(DistributorAftersalesAddressDetailReadPort.class);
		LangueProperties langueProperties = mock(LangueProperties.class);

		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		JushuitanTradeAftersalesDispatchPublisher jushuitanPublisher =
				mock(JushuitanTradeAftersalesDispatchPublisher.class);
		WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher =
				mock(WdtErpTradeAfterSaleDispatchPublisher.class);
		ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher =
				mock(ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher.class);

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

		AftersalesReviewService service =
				new AftersalesReviewService(
						aftersalesMapper,
						aftersalesDetailMapper,
						aftersalesRefundService,
						aftersalesRefundAsyncPort,
						aftersalesBrokeragePort,
						orderProcessLogPublishPort,
						normalOrderLeftAftersalesWritePort,
						normalOrderPartialCancelRestorePort,
						orderValidityPlatformSettingReadPort,
						distributorAftersalesAddressDetailReadPort,
						applicationEventPublisher,
						new ObjectMapper(),
						txMgr,
						langueProperties,
						jushuitanPublisher,
						new JushuitanTradeAftersalesBusPayloadBuilder(),
						wdtErpTradeAfterSaleDispatchPublisher,
						new WdtErpTradeAfterSaleBusPayloadBuilder(),
						thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher,
						thirdPartyTradeAftersalesUpdateSaasErpDispatchPublisher,
						tradeAftersalesDispatchPublisher,
						aftersalesAgreedCouponRestorePort,
						mock(AftersalesRefuseWxaNotifyService.class));

		Map<String, Object> param = new LinkedHashMap<>();
		param.put("company_id", companyId);
		param.put("aftersales_bn", aftersalesBn);
		param.put("is_approved", true);
		param.put("refund_fee", 50);
		param.put("refund_point", 0);
		param.put("freight", 0);
		param.put("operator_type", "admin");
		param.put("operator_id", 1L);

		HttpServletRequest request = mock(HttpServletRequest.class);
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("company_id", companyId);
		jwt.put("operator_type", "admin");
		jwt.put("operator_id", 1L);
		when(request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA)).thenReturn(jwt);

		service.aftersalesReview(new LinkedHashMap<>(param), request);

		verify(aftersalesAgreedCouponRestorePort).restoreOnRefundAgreed(companyId, orderId);
	}

	@Test
	@DisplayName("仅退款审核驳回：不调用优惠券恢复端口")
	void onlyRefundReject_doesNotRestoreCoupons() {
		long companyId = 10L;
		long aftersalesBn = 202601011234570L;
		long orderId = 100L;

		AftersalesAgreedCouponRestorePort aftersalesAgreedCouponRestorePort =
				mock(AftersalesAgreedCouponRestorePort.class);
		ThirdPartyTradeAftersalesUpdateSaasErpDispatchPublisher thirdPartyTradeAftersalesUpdateSaasErpDispatchPublisher =
				mock(ThirdPartyTradeAftersalesUpdateSaasErpDispatchPublisher.class);
		TradeAftersalesDispatchPublisher tradeAftersalesDispatchPublisher =
				mock(TradeAftersalesDispatchPublisher.class);

		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AtomicInteger selectPass = new AtomicInteger();
		Aftersales loaded = pendingOnlyRefundRow(companyId, aftersalesBn, orderId);
		Aftersales reloadedRejected = rejectedOnlyRefundRow(companyId, aftersalesBn, orderId);
		when(aftersalesMapper.selectOne(any()))
				.thenAnswer(
						inv -> {
							int n = selectPass.getAndIncrement();
							return n == 0 ? loaded : reloadedRejected;
						});
		when(aftersalesMapper.updateById(any(Aftersales.class))).thenReturn(1);

		AftersalesDetailMapper aftersalesDetailMapper = mock(AftersalesDetailMapper.class);
		when(aftersalesDetailMapper.selectList(any())).thenReturn(List.of());
		when(aftersalesDetailMapper.selectCount(any())).thenReturn(0L);

		AftersalesRefundService aftersalesRefundService = mock(AftersalesRefundService.class);
		when(aftersalesRefundService.updateRefundByAftersalesKeys(anyLong(), anyLong(), any())).thenReturn(1);

		AftersalesRefundAsyncPort aftersalesRefundAsyncPort = mock(AftersalesRefundAsyncPort.class);
		AftersalesBrokeragePort aftersalesBrokeragePort = mock(AftersalesBrokeragePort.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = mock(OrderProcessLogPublishPort.class);
		NormalOrderLeftAftersalesWritePort normalOrderLeftAftersalesWritePort =
				mock(NormalOrderLeftAftersalesWritePort.class);
		NormalOrderPartialCancelRestorePort normalOrderPartialCancelRestorePort =
				mock(NormalOrderPartialCancelRestorePort.class);
		OrderValidityPlatformSettingReadPort orderValidityPlatformSettingReadPort =
				mock(OrderValidityPlatformSettingReadPort.class);
		DistributorAftersalesAddressDetailReadPort distributorAftersalesAddressDetailReadPort =
				mock(DistributorAftersalesAddressDetailReadPort.class);
		LangueProperties langueProperties = mock(LangueProperties.class);

		ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
		JushuitanTradeAftersalesDispatchPublisher jushuitanPublisher =
				mock(JushuitanTradeAftersalesDispatchPublisher.class);
		WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher =
				mock(WdtErpTradeAfterSaleDispatchPublisher.class);
		ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher =
				mock(ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher.class);

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

		AftersalesReviewService service =
				new AftersalesReviewService(
						aftersalesMapper,
						aftersalesDetailMapper,
						aftersalesRefundService,
						aftersalesRefundAsyncPort,
						aftersalesBrokeragePort,
						orderProcessLogPublishPort,
						normalOrderLeftAftersalesWritePort,
						normalOrderPartialCancelRestorePort,
						orderValidityPlatformSettingReadPort,
						distributorAftersalesAddressDetailReadPort,
						applicationEventPublisher,
						new ObjectMapper(),
						txMgr,
						langueProperties,
						jushuitanPublisher,
						new JushuitanTradeAftersalesBusPayloadBuilder(),
						wdtErpTradeAfterSaleDispatchPublisher,
						new WdtErpTradeAfterSaleBusPayloadBuilder(),
						thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher,
						thirdPartyTradeAftersalesUpdateSaasErpDispatchPublisher,
						tradeAftersalesDispatchPublisher,
						aftersalesAgreedCouponRestorePort,
						mock(AftersalesRefuseWxaNotifyService.class));

		Map<String, Object> param = new LinkedHashMap<>();
		param.put("company_id", companyId);
		param.put("aftersales_bn", aftersalesBn);
		param.put("is_approved", false);
		param.put("refuse_reason", "probe");
		param.put("operator_type", "admin");
		param.put("operator_id", 1L);

		HttpServletRequest request = mock(HttpServletRequest.class);
		Map<String, Object> jwt = new LinkedHashMap<>();
		jwt.put("company_id", companyId);
		jwt.put("operator_type", "admin");
		jwt.put("operator_id", 1L);
		when(request.getAttribute(OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA)).thenReturn(jwt);

		service.aftersalesReview(new LinkedHashMap<>(param), request);

		verify(aftersalesAgreedCouponRestorePort, never()).restoreOnRefundAgreed(anyLong(), anyLong());
	}

	private static Aftersales pendingOnlyRefundRow(long companyId, long aftersalesBn, long orderId) {
		Aftersales a = new Aftersales();
		a.setCompanyId(companyId);
		a.setAftersalesBn(aftersalesBn);
		a.setOrderId(orderId);
		a.setUserId(20L);
		a.setAftersalesType("ONLY_REFUND");
		a.setAftersalesStatus(0);
		a.setProgress(0);
		a.setRefundFee(100);
		a.setRefundPoint(0);
		a.setFreight(10);
		return a;
	}

	private static Aftersales committedOnlyRefundRow(long companyId, long aftersalesBn, long orderId) {
		Aftersales a = pendingOnlyRefundRow(companyId, aftersalesBn, orderId);
		a.setProgress(9);
		a.setAftersalesStatus(1);
		return a;
	}

	private static Aftersales rejectedOnlyRefundRow(long companyId, long aftersalesBn, long orderId) {
		Aftersales a = pendingOnlyRefundRow(companyId, aftersalesBn, orderId);
		a.setAftersalesStatus(2);
		a.setProgress(0);
		return a;
	}
}
