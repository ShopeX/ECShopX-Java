package cn.shopex.ecshopx.orders.service.localdelivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.common.port.localdelivery.DadaLocalDeliveryReceiptPort;
import cn.shopex.ecshopx.common.port.localdelivery.ShansongLocalDeliveryReceiptPort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.dispatch.DispatchFacade;
import cn.shopex.ecshopx.dispatch.DispatchOptions;
import cn.shopex.ecshopx.dispatch.integration.OrderProcessLogPublishPortImpl;
import cn.shopex.ecshopx.orders.domain.NormalOrdersRelDada;
import cn.shopex.ecshopx.orders.mapper.CompanyRelDadaMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersRelDadaMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("EVENT_ORDER_PROCESS_LOG: businessReceipt publishEvent probe")
class LocalDeliveryBusinessReceiptServiceBusinessReceiptOrderProcessLogDispatchPublishProbeTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrdersRelDada.class);
	}

	@Test
	void businessReceipt_afterDadaUpdate_success_invokesPublishEventOnce_withBusinessReceiptPayload() {
		NormalOrdersRelDadaMapper normalOrdersRelDadaMapper = mock(NormalOrdersRelDadaMapper.class);
		CompanyRelDadaMapper companyRelDadaMapper = mock(CompanyRelDadaMapper.class);
		LocalDeliveryOrderInfoLoadService localDeliveryOrderInfoLoadService =
				mock(LocalDeliveryOrderInfoLoadService.class);
		DadaLocalDeliveryReceiptPort dadaLocalDeliveryReceiptPort = mock(DadaLocalDeliveryReceiptPort.class);
		ShansongLocalDeliveryReceiptPort shansongLocalDeliveryReceiptPort =
				mock(ShansongLocalDeliveryReceiptPort.class);
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = new OrderProcessLogPublishPortImpl(dispatchFacade);

		LocalDeliveryBusinessReceiptService svc =
				new LocalDeliveryBusinessReceiptService(
						normalOrdersRelDadaMapper,
						companyRelDadaMapper,
						localDeliveryOrderInfoLoadService,
						dadaLocalDeliveryReceiptPort,
						shansongLocalDeliveryReceiptPort,
						orderProcessLogPublishPort,
						"dada");

		long companyId = 91561L;
		long orderId = 61561L;
		long operatorId = 81761L;

		NormalOrdersRelDada row = new NormalOrdersRelDada();
		row.setCompanyId(companyId);
		row.setOrderId(orderId);
		row.setDadaStatus(0);
		row.setUpdateTime((int) (System.currentTimeMillis() / 1000L));
		row.setDadaDeliveryNo("existing-dn");

		when(normalOrdersRelDadaMapper.selectOne(any())).thenReturn(row);
		when(normalOrdersRelDadaMapper.update(isNull(), any())).thenReturn(1);

		svc.businessReceipt(companyId, " 0061561 ", operatorId);

		ArgumentCaptor<Map<String, Object>> payloadCaptor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));

		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals(Long.valueOf(orderId), payload.get("order_id"));
		assertEquals(companyId, ((Number) payload.get("company_id")).longValue());
		assertEquals("admin", payload.get("operator_type"));
		assertEquals(operatorId, ((Number) payload.get("operator_id")).longValue());
		assertEquals("商家接单", payload.get("remarks"));
		assertEquals("订单号：0061561，商家已接单", payload.get("detail"));
		Object params = payload.get("params");
		assertInstanceOf(Map.class, params);
		assertTrue(((Map<?, ?>) params).isEmpty());
	}

	@Test
	void businessReceipt_afterShansongOrderPlaceApiAndUpdate_success_invokesPublishEventOnce_withBusinessReceiptPayload() {
		NormalOrdersRelDadaMapper normalOrdersRelDadaMapper = mock(NormalOrdersRelDadaMapper.class);
		CompanyRelDadaMapper companyRelDadaMapper = mock(CompanyRelDadaMapper.class);
		LocalDeliveryOrderInfoLoadService localDeliveryOrderInfoLoadService =
				mock(LocalDeliveryOrderInfoLoadService.class);
		DadaLocalDeliveryReceiptPort dadaLocalDeliveryReceiptPort = mock(DadaLocalDeliveryReceiptPort.class);
		ShansongLocalDeliveryReceiptPort shansongLocalDeliveryReceiptPort =
				mock(ShansongLocalDeliveryReceiptPort.class);
		DispatchFacade dispatchFacade = mock(DispatchFacade.class);
		OrderProcessLogPublishPort orderProcessLogPublishPort = new OrderProcessLogPublishPortImpl(dispatchFacade);

		LocalDeliveryBusinessReceiptService svc =
				new LocalDeliveryBusinessReceiptService(
						normalOrdersRelDadaMapper,
						companyRelDadaMapper,
						localDeliveryOrderInfoLoadService,
						dadaLocalDeliveryReceiptPort,
						shansongLocalDeliveryReceiptPort,
						orderProcessLogPublishPort,
						"shansong");

		long companyId = 91561L;
		long orderId = 61561L;
		long operatorId = 81761L;

		NormalOrdersRelDada row = new NormalOrdersRelDada();
		row.setCompanyId(companyId);
		row.setOrderId(orderId);
		row.setDadaStatus(0);
		row.setUpdateTime((int) (System.currentTimeMillis() / 1000L));
		row.setDadaDeliveryNo("existing-dn");

		when(normalOrdersRelDadaMapper.selectOne(any())).thenReturn(row);
		when(normalOrdersRelDadaMapper.update(isNull(), any())).thenReturn(1);

		svc.businessReceipt(companyId, " 0061561 ", operatorId);

		verify(shansongLocalDeliveryReceiptPort).orderPlaceApi(eq(companyId), eq("existing-dn"));

		ArgumentCaptor<Map<String, Object>> payloadCaptor =
				ArgumentCaptor.forClass((Class<Map<String, Object>>) (Class<?>) Map.class);
		verify(dispatchFacade, times(1))
				.publishEvent(
						eq(OrdersDispatchEventNames.EVENT_ORDER_PROCESS_LOG),
						payloadCaptor.capture(),
						eq(DispatchOptions.oplQueuedAfterCommit()));

		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals(Long.valueOf(orderId), payload.get("order_id"));
		assertEquals(companyId, ((Number) payload.get("company_id")).longValue());
		assertEquals("admin", payload.get("operator_type"));
		assertEquals(operatorId, ((Number) payload.get("operator_id")).longValue());
		assertEquals("商家接单", payload.get("remarks"));
		assertEquals("订单号：0061561，商家已接单", payload.get("detail"));
		Object params = payload.get("params");
		assertInstanceOf(Map.class, params);
		assertTrue(((Map<?, ?>) params).isEmpty());
	}
}
