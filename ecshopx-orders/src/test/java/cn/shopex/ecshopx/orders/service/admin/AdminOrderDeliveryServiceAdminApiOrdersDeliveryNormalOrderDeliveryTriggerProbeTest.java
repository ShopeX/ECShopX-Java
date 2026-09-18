package cn.shopex.ecshopx.orders.service.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;

import cn.shopex.ecshopx.common.dispatch.OrdersDispatchEventNames;
import cn.shopex.ecshopx.dispatch.DispatchConsumerRuntime;
import cn.shopex.ecshopx.dispatch.DispatchDriverType;
import cn.shopex.ecshopx.dispatch.DispatchMessage;
import cn.shopex.ecshopx.dispatch.DispatchMessageType;
import cn.shopex.ecshopx.dispatch.DispatchMode;
import cn.shopex.ecshopx.dispatch.DispatchRetryDecider;
import cn.shopex.ecshopx.dispatch.DispatchStructuredLogger;
import cn.shopex.ecshopx.dispatch.FailedJobRecorder;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchConsumerStateRecorder;
import cn.shopex.ecshopx.dispatch.InMemoryDispatchRegistry;
import cn.shopex.ecshopx.dispatch.ListenerDispatchOptions;
import cn.shopex.ecshopx.dispatch.RetryPolicy;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.thirdparty.dispatch.OrderDeliveryDmCrmOnNormalOrderDeliveryDispatchListener;
import cn.shopex.ecshopx.thirdparty.dispatch.OrderDeliveryPushMarketingCenterOnNormalOrderDeliveryDispatchListener;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.OrderDeliveryDmCrmOnNormalOrderDeliveryProcessor;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.OrderDeliveryPushMarketingCenterOnNormalOrderDeliveryProcessor;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminOrderDeliveryServiceAdminApiOrdersDeliveryNormalOrderDeliveryTriggerProbeTest {

	private static final long companyId = 11L;
	private static final long orderId = 9001L;
	private static final long operatorId = 42L;

	@Mock
	OrderAssociationsMapper orderAssociationsMapper;

	@Mock
	AdminSupplierDeliveryParamsAdjustService adminSupplierDeliveryParamsAdjustService;

	@Mock
	AdminDeliveryLogisticsNameService adminDeliveryLogisticsNameService;

	@Mock
	OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService;

	@Mock
	AdminNormalOrderDeliveryCoreService adminNormalOrderDeliveryCoreService;

	@Mock
	OrderAssociationAssociationDataAssembler orderAssociationAssociationDataAssembler;

	@Mock
	OrderDeliveryPushMarketingCenterOnNormalOrderDeliveryProcessor orderDeliveryPushMarketingCenterOnNormalOrderDeliveryProcessor;

	@Mock
	OrderDeliveryDmCrmOnNormalOrderDeliveryProcessor orderDeliveryDmCrmOnNormalOrderDeliveryProcessor;

	@Captor
	private ArgumentCaptor<Map<String, Object>> paramsCaptor;

	private AdminOrderDeliveryService adminOrderDeliveryService;

	private OrderAssociations assocStub;

	@BeforeEach
	void setUp() {
		assocStub = new OrderAssociations();
		assocStub.setCompanyId(companyId);
		assocStub.setOrderId(orderId);

		when(orderAssociationsMapper.selectOne(any())).thenReturn(assocStub).thenReturn(assocStub);
		when(orderAssociationEffectiveTypeService.effectiveOrderType(assocStub)).thenReturn("normal");
		doNothing().when(adminDeliveryLogisticsNameService).fillLogiName(any());
		lenient()
				.when(orderAssociationAssociationDataAssembler.toAssociationDataMap(any()))
				.thenReturn(new LinkedHashMap<>(Map.of("order_id", orderId)));

		adminOrderDeliveryService =
				new AdminOrderDeliveryService(
						orderAssociationsMapper,
						adminSupplierDeliveryParamsAdjustService,
						adminDeliveryLogisticsNameService,
						orderAssociationEffectiveTypeService,
						adminNormalOrderDeliveryCoreService,
						orderAssociationAssociationDataAssembler);
	}

	@Test
	@DisplayName("AdminOrderDeliveryService — salesperson batch delegates to deliveryNormalPhysical once with expected params")
	void delivery_whenNormalPhysicalSalespersonBatch_invokesCoreOnceWithExpectedParams() {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("order_id", orderId);
		merged.put("delivery_type", "batch");

		adminOrderDeliveryService.delivery(companyId, "salesperson", operatorId, merged);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
		verify(adminNormalOrderDeliveryCoreService, times(1))
				.deliveryNormalPhysical(paramsCaptor.capture(), same(assocStub), eq("normal"));

		Map<String, Object> captured = paramsCaptor.getValue();
		assertEquals(companyId, toLong(captured.get("company_id")));
		assertEquals(orderId, toLong(captured.get("order_id")));
		assertEquals("salesperson", captured.get("operator_type"));
		assertEquals(operatorId, toLong(captured.get("operator_id")));
		assertEquals(0, toInt(captured.get("supplier_id")));
		assertEquals("batch", captured.get("delivery_type"));
	}

	@Test
	@DisplayName(
			"EVENT_NORMAL_ORDER_DELIVERY: admin Orders/delivery — marketing listener consumed via DispatchConsumerRuntime")
	void delivery_whenNormalPhysicalSalespersonBatch_thenDispatchConsumerRuntimeConsumesMarketingProcessorHandle() {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("order_id", orderId);
		merged.put("delivery_type", "batch");

		adminOrderDeliveryService.delivery(companyId, "salesperson", operatorId, merged);

		verify(adminNormalOrderDeliveryCoreService, times(1))
				.deliveryNormalPhysical(paramsCaptor.capture(), same(assocStub), eq("normal"));
		Map<String, Object> coreParams = paramsCaptor.getValue();

		Map<String, Object> marketingPayload = new LinkedHashMap<>();
		marketingPayload.put("company_id", coreParams.get("company_id"));
		marketingPayload.put("order_id", coreParams.get("order_id"));

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_DELIVERY,
				OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_DELIVERY_PUSH_MARKETING_CENTER_ON_NORMAL_ORDER_DELIVERY,
				ListenerDispatchOptions.async("default", null),
				new OrderDeliveryPushMarketingCenterOnNormalOrderDeliveryDispatchListener(
						orderDeliveryPushMarketingCenterOnNormalOrderDeliveryProcessor));
		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						mock(DispatchRetryDecider.class),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());

		DispatchMessage message =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						OrdersDispatchEventNames.EVENT_NORMAL_ORDER_DELIVERY,
						marketingPayload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T12:00:00Z"),
						"trace-adminapi-orders-delivery-marketing",
						OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_DELIVERY_PUSH_MARKETING_CENTER_ON_NORMAL_ORDER_DELIVERY);

		runtime.consume(message, 1);

		verify(orderDeliveryPushMarketingCenterOnNormalOrderDeliveryProcessor, times(1)).handle(marketingPayload);
	}

	@Test
	@DisplayName(
			"EVENT_NORMAL_ORDER_DELIVERY: admin Orders/delivery — marketing child DispatchMessage literals + DispatchConsumerRuntime consume")
	void delivery_whenNormalPhysicalSalespersonBatch_thenMarketingChildDispatchMessageLiteralsMatchPlanAndDispatchConsumerRuntimeConsumes() {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("order_id", orderId);
		merged.put("delivery_type", "batch");

		adminOrderDeliveryService.delivery(companyId, "salesperson", operatorId, merged);

		verify(adminNormalOrderDeliveryCoreService, times(1))
				.deliveryNormalPhysical(paramsCaptor.capture(), same(assocStub), eq("normal"));
		Map<String, Object> coreParams = paramsCaptor.getValue();

		Map<String, Object> marketingPayload = new LinkedHashMap<>();
		marketingPayload.put("company_id", coreParams.get("company_id"));
		marketingPayload.put("order_id", coreParams.get("order_id"));

		assertEquals(orderId, toLong(marketingPayload.get("order_id")));
		assertEquals(companyId, toLong(marketingPayload.get("company_id")));

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_DELIVERY,
				OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_DELIVERY_PUSH_MARKETING_CENTER_ON_NORMAL_ORDER_DELIVERY,
				ListenerDispatchOptions.async("default", null),
				new OrderDeliveryPushMarketingCenterOnNormalOrderDeliveryDispatchListener(
						orderDeliveryPushMarketingCenterOnNormalOrderDeliveryProcessor));
		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						mock(DispatchRetryDecider.class),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());

		DispatchMessage message =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						OrdersDispatchEventNames.EVENT_NORMAL_ORDER_DELIVERY,
						marketingPayload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T12:00:00Z"),
						"trace-adminapi-orders-delivery-marketing",
						OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_DELIVERY_PUSH_MARKETING_CENTER_ON_NORMAL_ORDER_DELIVERY);

		assertEquals(DispatchMessageType.EVENT, message.messageType());
		assertEquals(DispatchMode.ASYNC, message.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, message.driverType());
		assertEquals(OrdersDispatchEventNames.EVENT_NORMAL_ORDER_DELIVERY, message.messageName());
		assertEquals(orderId, toLong(message.payload().get("order_id")));
		assertEquals(companyId, toLong(message.payload().get("company_id")));
		assertEquals(2, message.payload().size());
		assertEquals("default", message.queue());
		assertNull(message.delay());
		assertEquals(RetryPolicy.platformDefault(), message.retryPolicy());
		assertEquals(Instant.parse("2026-05-10T12:00:00Z"), message.occurredAt());
		assertEquals("trace-adminapi-orders-delivery-marketing", message.traceId());
		assertEquals(
				OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_DELIVERY_PUSH_MARKETING_CENTER_ON_NORMAL_ORDER_DELIVERY,
				message.listenerName());

		runtime.consume(message, 1);

		verify(orderDeliveryPushMarketingCenterOnNormalOrderDeliveryProcessor, times(1)).handle(marketingPayload);
	}

	@Test
	@DisplayName(
			"EVENT_NORMAL_ORDER_DELIVERY: admin Orders/delivery — plan §6 DmCrm child DispatchMessage literals + admin traces + DispatchConsumerRuntime")
	void delivery_whenNormalPhysicalSalespersonBatch_thenPlanSection6DmCrmChildDispatchMessageLiteralsAndDispatchConsumerRuntimeConsumes() {
		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("order_id", orderId);
		merged.put("delivery_type", "batch");

		adminOrderDeliveryService.delivery(companyId, "salesperson", operatorId, merged);

		verify(adminNormalOrderDeliveryCoreService, times(1))
				.deliveryNormalPhysical(paramsCaptor.capture(), same(assocStub), eq("normal"));
		Map<String, Object> coreParams = paramsCaptor.getValue();

		Map<String, Object> dmCrmPayload = new LinkedHashMap<>();
		dmCrmPayload.put("company_id", coreParams.get("company_id"));
		dmCrmPayload.put("order_id", coreParams.get("order_id"));

		assertEquals(orderId, toLong(dmCrmPayload.get("order_id")));
		assertEquals(companyId, toLong(dmCrmPayload.get("company_id")));

		Map<String, Object> parentPayload = new LinkedHashMap<>();
		parentPayload.put("order_id", orderId);
		parentPayload.put("company_id", companyId);
		DispatchMessage parentTemplate =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						OrdersDispatchEventNames.EVENT_NORMAL_ORDER_DELIVERY,
						parentPayload,
						null,
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T12:00:00Z"),
						"trace-parent-normal-order-delivery-event235-entry03-adminapi",
						null);
		assertEquals(DispatchMessageType.EVENT, parentTemplate.messageType());
		assertEquals(DispatchMode.ASYNC, parentTemplate.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, parentTemplate.driverType());
		assertEquals(OrdersDispatchEventNames.EVENT_NORMAL_ORDER_DELIVERY, parentTemplate.messageName());
		assertEquals(orderId, toLong(parentTemplate.payload().get("order_id")));
		assertEquals(companyId, toLong(parentTemplate.payload().get("company_id")));
		assertEquals(2, parentTemplate.payload().size());
		assertNull(parentTemplate.queue());
		assertNull(parentTemplate.delay());
		assertEquals(RetryPolicy.platformDefault(), parentTemplate.retryPolicy());
		assertEquals(Instant.parse("2026-05-10T12:00:00Z"), parentTemplate.occurredAt());
		assertEquals("trace-parent-normal-order-delivery-event235-entry03-adminapi", parentTemplate.traceId());
		assertNull(parentTemplate.listenerName());

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_DELIVERY,
				OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_DELIVERY_PUSH_DM_CRM_ON_NORMAL_ORDER_DELIVERY,
				ListenerDispatchOptions.async("default", null),
				new OrderDeliveryDmCrmOnNormalOrderDeliveryDispatchListener(
						orderDeliveryDmCrmOnNormalOrderDeliveryProcessor));
		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						mock(DispatchRetryDecider.class),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());

		DispatchMessage dmCrmChild =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						OrdersDispatchEventNames.EVENT_NORMAL_ORDER_DELIVERY,
						dmCrmPayload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T12:00:00Z"),
						"trace-adminapi-orders-delivery-dmcrm-event235-entry03",
						OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_DELIVERY_PUSH_DM_CRM_ON_NORMAL_ORDER_DELIVERY);

		assertEquals(DispatchMessageType.EVENT, dmCrmChild.messageType());
		assertEquals(DispatchMode.ASYNC, dmCrmChild.dispatchMode());
		assertEquals(DispatchDriverType.REDIS, dmCrmChild.driverType());
		assertEquals(OrdersDispatchEventNames.EVENT_NORMAL_ORDER_DELIVERY, dmCrmChild.messageName());
		assertEquals(orderId, toLong(dmCrmChild.payload().get("order_id")));
		assertEquals(companyId, toLong(dmCrmChild.payload().get("company_id")));
		assertEquals(2, dmCrmChild.payload().size());
		assertEquals("default", dmCrmChild.queue());
		assertNull(dmCrmChild.delay());
		assertEquals(RetryPolicy.platformDefault(), dmCrmChild.retryPolicy());
		assertEquals(Instant.parse("2026-05-10T12:00:00Z"), dmCrmChild.occurredAt());
		assertEquals("trace-adminapi-orders-delivery-dmcrm-event235-entry03", dmCrmChild.traceId());
		assertEquals(
				OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_DELIVERY_PUSH_DM_CRM_ON_NORMAL_ORDER_DELIVERY,
				dmCrmChild.listenerName());

		runtime.consume(dmCrmChild, 1);

		verify(orderDeliveryDmCrmOnNormalOrderDeliveryProcessor, times(1)).handle(dmCrmPayload);
	}

	@Test
	@DisplayName(
			"EVENT_NORMAL_ORDER_DELIVERY: AdminApi Orders delivery — event237 entry-03 — DmCrm parent/child DispatchMessage + DispatchConsumerRuntime")
	void delivery_whenNormalPhysicalSalespersonBatch_event237_entry03_thenDmCrmParentAndChildDispatchMessage606505AndDispatchConsumerRuntimeConsumes() {
		final long event237CompanyId = 505L;
		final long event237OrderId = 606L;
		OrderAssociations assoc606505 = new OrderAssociations();
		assoc606505.setCompanyId(event237CompanyId);
		assoc606505.setOrderId(event237OrderId);
		reset(orderAssociationsMapper, orderAssociationEffectiveTypeService);
		when(orderAssociationsMapper.selectOne(any())).thenReturn(assoc606505, assoc606505);
		when(orderAssociationEffectiveTypeService.effectiveOrderType(assoc606505)).thenReturn("normal");

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("order_id", event237OrderId);
		merged.put("delivery_type", "batch");

		adminOrderDeliveryService.delivery(event237CompanyId, "salesperson", operatorId, merged);

		verify(adminNormalOrderDeliveryCoreService, times(1))
				.deliveryNormalPhysical(paramsCaptor.capture(), same(assoc606505), eq("normal"));
		Map<String, Object> coreParams = paramsCaptor.getValue();

		Map<String, Object> dmCrmPayload = new LinkedHashMap<>();
		dmCrmPayload.put("company_id", coreParams.get("company_id"));
		dmCrmPayload.put("order_id", coreParams.get("order_id"));

		assertThat(toLong(dmCrmPayload.get("order_id"))).isEqualTo(event237OrderId);
		assertThat(toLong(dmCrmPayload.get("company_id"))).isEqualTo(event237CompanyId);

		Map<String, Object> parentPayload = new LinkedHashMap<>();
		parentPayload.put("order_id", event237OrderId);
		parentPayload.put("company_id", event237CompanyId);
		DispatchMessage parentTemplate =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						OrdersDispatchEventNames.EVENT_NORMAL_ORDER_DELIVERY,
						parentPayload,
						null,
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T12:00:00Z"),
						"trace-parent-normal-order-delivery-event237-entry03-adminapi",
						null);
		assertThat(parentTemplate.messageType()).isEqualTo(DispatchMessageType.EVENT);
		assertThat(parentTemplate.dispatchMode()).isEqualTo(DispatchMode.ASYNC);
		assertThat(parentTemplate.driverType()).isEqualTo(DispatchDriverType.REDIS);
		assertThat(parentTemplate.messageName()).isEqualTo(OrdersDispatchEventNames.EVENT_NORMAL_ORDER_DELIVERY);
		assertThat(toLong(parentTemplate.payload().get("order_id"))).isEqualTo(event237OrderId);
		assertThat(toLong(parentTemplate.payload().get("company_id"))).isEqualTo(event237CompanyId);
		assertThat(parentTemplate.payload()).hasSize(2);
		assertThat(parentTemplate.queue()).isNull();
		assertThat(parentTemplate.delay()).isNull();
		assertThat(parentTemplate.retryPolicy()).isEqualTo(RetryPolicy.platformDefault());
		assertThat(parentTemplate.occurredAt()).isEqualTo(Instant.parse("2026-05-10T12:00:00Z"));
		assertThat(parentTemplate.traceId())
				.isEqualTo("trace-parent-normal-order-delivery-event237-entry03-adminapi");
		assertThat(parentTemplate.listenerName()).isNull();

		InMemoryDispatchRegistry registry = new InMemoryDispatchRegistry();
		registry.registerEventListener(
				OrdersDispatchEventNames.EVENT_NORMAL_ORDER_DELIVERY,
				OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_DELIVERY_PUSH_DM_CRM_ON_NORMAL_ORDER_DELIVERY,
				ListenerDispatchOptions.async("default", null),
				new OrderDeliveryDmCrmOnNormalOrderDeliveryDispatchListener(
						orderDeliveryDmCrmOnNormalOrderDeliveryProcessor));
		DispatchConsumerRuntime runtime =
				new DispatchConsumerRuntime(
						registry,
						mock(DispatchRetryDecider.class),
						mock(FailedJobRecorder.class),
						mock(DispatchStructuredLogger.class),
						new InMemoryDispatchConsumerStateRecorder());

		DispatchMessage dmCrmChild =
				new DispatchMessage(
						DispatchMessageType.EVENT,
						DispatchMode.ASYNC,
						DispatchDriverType.REDIS,
						OrdersDispatchEventNames.EVENT_NORMAL_ORDER_DELIVERY,
						dmCrmPayload,
						"default",
						null,
						RetryPolicy.platformDefault(),
						Instant.parse("2026-05-10T12:00:00Z"),
						"trace-adminapi-orders-delivery-dmcrm-event237-entry03",
						OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_DELIVERY_PUSH_DM_CRM_ON_NORMAL_ORDER_DELIVERY);

		assertThat(dmCrmChild.messageType()).isEqualTo(DispatchMessageType.EVENT);
		assertThat(dmCrmChild.dispatchMode()).isEqualTo(DispatchMode.ASYNC);
		assertThat(dmCrmChild.driverType()).isEqualTo(DispatchDriverType.REDIS);
		assertThat(dmCrmChild.messageName()).isEqualTo(OrdersDispatchEventNames.EVENT_NORMAL_ORDER_DELIVERY);
		assertThat(toLong(dmCrmChild.payload().get("order_id"))).isEqualTo(event237OrderId);
		assertThat(toLong(dmCrmChild.payload().get("company_id"))).isEqualTo(event237CompanyId);
		assertThat(dmCrmChild.payload()).hasSize(2);
		assertThat(dmCrmChild.queue()).isEqualTo("default");
		assertThat(dmCrmChild.delay()).isNull();
		assertThat(dmCrmChild.retryPolicy()).isEqualTo(RetryPolicy.platformDefault());
		assertThat(dmCrmChild.occurredAt()).isEqualTo(Instant.parse("2026-05-10T12:00:00Z"));
		assertThat(dmCrmChild.traceId()).isEqualTo("trace-adminapi-orders-delivery-dmcrm-event237-entry03");
		assertThat(dmCrmChild.listenerName())
				.isEqualTo(OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_DELIVERY_PUSH_DM_CRM_ON_NORMAL_ORDER_DELIVERY);

		runtime.consume(dmCrmChild, 1);

		verify(orderDeliveryDmCrmOnNormalOrderDeliveryProcessor, times(1)).handle(dmCrmPayload);
	}

	@Test
	@DisplayName("AdminOrderDeliveryService — non-normal physical effective types do not invoke core")
	void delivery_whenNotNormalPhysicalFamily_neverInvokesCore() {
		when(orderAssociationEffectiveTypeService.effectiveOrderType(assocStub)).thenReturn("service");

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("order_id", orderId);
		merged.put("delivery_type", "batch");

		adminOrderDeliveryService.delivery(companyId, "salesperson", operatorId, merged);

		verify(adminNormalOrderDeliveryCoreService, never())
				.deliveryNormalPhysical(anyMap(), any(OrderAssociations.class), any());
	}

	private static long toLong(Object raw) {
		assertInstanceOf(Number.class, raw);
		return ((Number) raw).longValue();
	}

	private static int toInt(Object raw) {
		assertInstanceOf(Number.class, raw);
		return ((Number) raw).intValue();
	}
}
