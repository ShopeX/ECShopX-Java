package cn.shopex.ecshopx.aftersales.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.aftersales.domain.Aftersales;
import cn.shopex.ecshopx.aftersales.domain.AftersalesDetail;
import cn.shopex.ecshopx.aftersales.jushuitan.JushuitanTradeAftersalesBusPayloadBuilder;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesDetailMapper;
import cn.shopex.ecshopx.aftersales.mapper.AftersalesMapper;
import cn.shopex.ecshopx.aftersales.port.AftersalesRefundAsyncPort;
import cn.shopex.ecshopx.aftersales.wdterp.WdtErpTradeAfterSaleBusPayloadBuilder;
import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.config.LangueProperties;
import cn.shopex.ecshopx.common.dispatch.JushuitanTradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.ThirdPartyTradeAftersalesSaasErpDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.TradeAftersalesDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.WdtErpTradeAfterSaleDispatchPublisher;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class AftersalesReviewServiceRejectTradeAftersalesBusPublishProbeTest {

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
	@SuppressWarnings("unchecked")
	void reject_review_invokesTradeAftersalesDispatchPublisher_once_withSnakeAftersalesPayload() {
		long companyId = 10L;
		long aftersalesBn = 202601011234570L;
		long orderId = 100L;

		ThirdPartyTradeAftersalesSaasErpDispatchPublisher thirdPartyTradeAftersalesSaasErpDispatchPublisher =
				mock(ThirdPartyTradeAftersalesSaasErpDispatchPublisher.class);

		AftersalesMapper aftersalesMapper = mock(AftersalesMapper.class);
		AtomicInteger selectPass = new AtomicInteger();
		Aftersales loaded = pendingRow(companyId, aftersalesBn, orderId);
		Aftersales reloadedRejected = rejectedRow(companyId, aftersalesBn, orderId);
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
		TradeAftersalesDispatchPublisher tradeAftersalesDispatchPublisher =
				mock(TradeAftersalesDispatchPublisher.class);
		WdtErpTradeAfterSaleDispatchPublisher wdtErpTradeAfterSaleDispatchPublisher =
				mock(WdtErpTradeAfterSaleDispatchPublisher.class);
		ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher thirdPartyTradeAftersalesCancelSaasErpDispatchPublisher =
				mock(ThirdPartyTradeAftersalesCancelSaasErpDispatchPublisher.class);

		PlatformTransactionManager txMgr = mock(PlatformTransactionManager.class);
		when(txMgr.getTransaction(any(TransactionDefinition.class)))
				.thenAnswer(
						inv -> {
							if (!TransactionSynchronizationManager.isSynchronizationActive()) {
								TransactionSynchronizationManager.initSynchronization();
							}
							return new SimpleTransactionStatus(true);
						});
		doAnswer(
						inv -> {
							if (TransactionSynchronizationManager.isSynchronizationActive()) {
								for (TransactionSynchronization sync :
										TransactionSynchronizationManager.getSynchronizations()) {
									sync.afterCommit();
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
						thirdPartyTradeAftersalesSaasErpDispatchPublisher,
						tradeAftersalesDispatchPublisher);

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

		verify(tradeAftersalesDispatchPublisher, times(1)).publish(any());
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
		verify(tradeAftersalesDispatchPublisher).publish(cap.capture());
		Map<String, Object> payload = cap.getValue();
		assertTrue(payload.containsKey("company_id"));
		assertTrue(payload.containsKey("order_id"));
		assertTrue(payload.containsKey("aftersales_bn"));
		assertEquals(companyId, ((Number) payload.get("company_id")).longValue());
		assertEquals(orderId, ((Number) payload.get("order_id")).longValue());
		assertEquals(aftersalesBn, ((Number) payload.get("aftersales_bn")).longValue());
	}

	private static Aftersales pendingRow(long companyId, long aftersalesBn, long orderId) {
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
		a.setDistributorId(2L);
		a.setShopId(1L);
		a.setSalesmanId(88L);
		return a;
	}

	private static Aftersales rejectedRow(long companyId, long aftersalesBn, long orderId) {
		Aftersales a = pendingRow(companyId, aftersalesBn, orderId);
		a.setProgress(3);
		a.setAftersalesStatus(3);
		a.setRefuseReason("probe");
		return a;
	}
}
