package cn.shopex.ecshopx.orders.service.front.wxapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import cn.shopex.ecshopx.orders.service.admin.AdminDeliveryLogisticsNameService;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDeliveryCoreService;
import cn.shopex.ecshopx.orders.service.admin.OrderAssociationAssociationDataAssembler;
import cn.shopex.ecshopx.orders.service.admin.OrderAssociationEffectiveTypeService;
import cn.shopex.ecshopx.thirdparty.dispatch.OrderDeliveryDmCrmOnNormalOrderDeliveryDispatchListener;
import cn.shopex.ecshopx.thirdparty.dispatch.OrderDeliveryPushMarketingCenterOnNormalOrderDeliveryDispatchListener;
import cn.shopex.ecshopx.thirdparty.service.dmcrm.OrderDeliveryDmCrmOnNormalOrderDeliveryProcessor;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.OrderDeliveryPushMarketingCenterOnNormalOrderDeliveryProcessor;
import jakarta.servlet.http.HttpServletRequest;
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
class WxappOrderDeliveryServiceNormalOrderDeliveryTriggerProbeTest {

	@Mock
	private OrderAssociationsMapper orderAssociationsMapper;

	@Mock
	private AdminDeliveryLogisticsNameService adminDeliveryLogisticsNameService;

	@Mock
	private OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService;

	@Mock
	private AdminNormalOrderDeliveryCoreService adminNormalOrderDeliveryCoreService;

	@Mock
	private OrderAssociationAssociationDataAssembler orderAssociationAssociationDataAssembler;

	@Mock
	private OrderDeliveryPushMarketingCenterOnNormalOrderDeliveryProcessor orderDeliveryPushMarketingCenterOnNormalOrderDeliveryProcessor;

	@Mock
	private OrderDeliveryDmCrmOnNormalOrderDeliveryProcessor orderDeliveryDmCrmOnNormalOrderDeliveryProcessor;

	@Mock
	private HttpServletRequest httpServletRequest;

	@Captor
	private ArgumentCaptor<Map<String, Object>> paramsCaptor;

	private WxappOrderDeliveryService wxappOrderDeliveryService;
	private OrderAssociations assocStub;

	@BeforeEach
	void setUp() {
		wxappOrderDeliveryService =
				new WxappOrderDeliveryService(
						orderAssociationsMapper,
						adminDeliveryLogisticsNameService,
						orderAssociationEffectiveTypeService,
						adminNormalOrderDeliveryCoreService,
						orderAssociationAssociationDataAssembler);

		assocStub = new OrderAssociations();
		assocStub.setCompanyId(100L);
		assocStub.setOrderId(200L);
		assocStub.setOrderType("normal");
		assocStub.setOrderClass("normal");

		when(orderAssociationsMapper.selectOne(any())).thenReturn(assocStub);
		doNothing().when(adminDeliveryLogisticsNameService).fillLogiName(any());

		lenient().when(httpServletRequest.getMethod()).thenReturn("POST");
	}

	@Test
	@DisplayName(
			"WxappOrderDeliveryService — delegates to deliveryNormalPhysical once with merged company_id and long order_id for normal physical")
	void delivery_whenNormalPhysical_invokesCoreOnceWithExpectedParams() {
		when(orderAssociationEffectiveTypeService.effectiveOrderType(assocStub)).thenReturn("normal");
		when(orderAssociationAssociationDataAssembler.toAssociationDataMap(any(OrderAssociations.class)))
				.thenReturn(Map.of("order_id", 200L, "company_id", 100L));

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("order_id", "200");
		merged.put("delivery_corp", "SF");
		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", 100L);
		auth.put("user_id", 55L);

		wxappOrderDeliveryService.delivery(httpServletRequest, merged, auth);

		verify(adminNormalOrderDeliveryCoreService, times(1))
				.deliveryNormalPhysical(paramsCaptor.capture(), same(assocStub), eq("normal"));
		Map<String, Object> captured = paramsCaptor.getValue();
		assertThat(captured.get("company_id")).isEqualTo(100L);
		assertThat(captured.get("order_id")).isEqualTo(200L);
		assertThat(captured.get("operator_type")).isEqualTo("user");
		assertThat(captured.get("operator_id")).isEqualTo(55L);
		assertThat(captured.get("supplier_id")).isEqualTo(0);
	}

	@Test
	@DisplayName(
			"EVENT_NORMAL_ORDER_DELIVERY: wxapp order/delivery — marketing listener consumed via DispatchConsumerRuntime")
	void delivery_whenNormalPhysical_thenDispatchConsumerRuntimeConsumesMarketingProcessorHandle() {
		when(orderAssociationEffectiveTypeService.effectiveOrderType(assocStub)).thenReturn("normal");
		when(orderAssociationAssociationDataAssembler.toAssociationDataMap(any(OrderAssociations.class)))
				.thenReturn(Map.of("order_id", 200L, "company_id", 100L));

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("order_id", "200");
		merged.put("delivery_corp", "SF");
		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", 100L);
		auth.put("user_id", 55L);

		wxappOrderDeliveryService.delivery(httpServletRequest, merged, auth);

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
						"trace-wxapp-order-delivery-marketing",
						OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_DELIVERY_PUSH_MARKETING_CENTER_ON_NORMAL_ORDER_DELIVERY);

		runtime.consume(message, 1);

		verify(orderDeliveryPushMarketingCenterOnNormalOrderDeliveryProcessor, times(1)).handle(marketingPayload);
	}

	@Test
	@DisplayName(
			"EVENT_NORMAL_ORDER_DELIVERY: wxapp FrontApi order/delivery — plan §6 marketing child DispatchMessage field-by-field literals + wxapp trace + DispatchConsumerRuntime")
	void delivery_whenNormalPhysical_thenPlanSection6MarketingChildDispatchMessageLiteralsAndWxappTraceMatchAndDispatchConsumerRuntimeConsumes() {
		when(orderAssociationEffectiveTypeService.effectiveOrderType(assocStub)).thenReturn("normal");
		when(orderAssociationAssociationDataAssembler.toAssociationDataMap(any(OrderAssociations.class)))
				.thenReturn(Map.of("order_id", 200L, "company_id", 100L));

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("order_id", "200");
		merged.put("delivery_corp", "SF");
		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", 100L);
		auth.put("user_id", 55L);

		wxappOrderDeliveryService.delivery(httpServletRequest, merged, auth);

		verify(adminNormalOrderDeliveryCoreService, times(1))
				.deliveryNormalPhysical(paramsCaptor.capture(), same(assocStub), eq("normal"));
		Map<String, Object> coreParams = paramsCaptor.getValue();

		Map<String, Object> marketingPayload = new LinkedHashMap<>();
		marketingPayload.put("company_id", coreParams.get("company_id"));
		marketingPayload.put("order_id", coreParams.get("order_id"));

		Map<String, Object> parentPayload = new LinkedHashMap<>();
		parentPayload.put("order_id", 200L);
		parentPayload.put("company_id", 100L);
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
						"trace-parent-normal-order-delivery-event234-entry03-wxapp-frontapi",
						null);
		assertThat(parentTemplate.messageType()).isEqualTo(DispatchMessageType.EVENT);
		assertThat(parentTemplate.dispatchMode()).isEqualTo(DispatchMode.ASYNC);
		assertThat(parentTemplate.driverType()).isEqualTo(DispatchDriverType.REDIS);
		assertThat(parentTemplate.messageName()).isEqualTo(OrdersDispatchEventNames.EVENT_NORMAL_ORDER_DELIVERY);
		assertThat(parentTemplate.payload()).containsExactlyEntriesOf(parentPayload);
		assertThat(parentTemplate.queue()).isNull();
		assertThat(parentTemplate.delay()).isNull();
		assertThat(parentTemplate.retryPolicy()).isEqualTo(RetryPolicy.platformDefault());
		assertThat(parentTemplate.occurredAt()).isEqualTo(Instant.parse("2026-05-10T12:00:00Z"));
		assertThat(parentTemplate.traceId())
				.isEqualTo("trace-parent-normal-order-delivery-event234-entry03-wxapp-frontapi");
		assertThat(parentTemplate.listenerName()).isNull();

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

		DispatchMessage marketingChild =
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
						"trace-frontapi-wxapporder-delivery-marketing-event234-entry03",
						OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_DELIVERY_PUSH_MARKETING_CENTER_ON_NORMAL_ORDER_DELIVERY);
		assertThat(marketingChild.messageType()).isEqualTo(DispatchMessageType.EVENT);
		assertThat(marketingChild.dispatchMode()).isEqualTo(DispatchMode.ASYNC);
		assertThat(marketingChild.driverType()).isEqualTo(DispatchDriverType.REDIS);
		assertThat(marketingChild.messageName()).isEqualTo(OrdersDispatchEventNames.EVENT_NORMAL_ORDER_DELIVERY);
		assertThat(marketingChild.payload()).containsExactlyEntriesOf(marketingPayload);
		assertThat(marketingChild.queue()).isEqualTo("default");
		assertThat(marketingChild.delay()).isNull();
		assertThat(marketingChild.retryPolicy()).isEqualTo(RetryPolicy.platformDefault());
		assertThat(marketingChild.occurredAt()).isEqualTo(Instant.parse("2026-05-10T12:00:00Z"));
		assertThat(marketingChild.traceId())
				.isEqualTo("trace-frontapi-wxapporder-delivery-marketing-event234-entry03");
		assertThat(marketingChild.listenerName())
				.isEqualTo(
						OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_DELIVERY_PUSH_MARKETING_CENTER_ON_NORMAL_ORDER_DELIVERY);

		runtime.consume(marketingChild, 1);

		verify(orderDeliveryPushMarketingCenterOnNormalOrderDeliveryProcessor, times(1)).handle(marketingPayload);
	}

	@Test
	@DisplayName(
			"EVENT_NORMAL_ORDER_DELIVERY: wxapp FrontApi order/delivery — plan §3 DmCrm child DispatchMessage field-by-field literals + wxapp trace + DispatchConsumerRuntime")
	void delivery_whenNormalPhysical_thenPlanSection6DmCrmChildDispatchMessageLiteralsAndWxappTraceMatchAndDispatchConsumerRuntimeConsumes() {
		when(orderAssociationEffectiveTypeService.effectiveOrderType(assocStub)).thenReturn("normal");
		when(orderAssociationAssociationDataAssembler.toAssociationDataMap(any(OrderAssociations.class)))
				.thenReturn(Map.of("order_id", 200L, "company_id", 100L));

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("order_id", "200");
		merged.put("delivery_corp", "SF");
		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", 100L);
		auth.put("user_id", 55L);

		wxappOrderDeliveryService.delivery(httpServletRequest, merged, auth);

		verify(adminNormalOrderDeliveryCoreService, times(1))
				.deliveryNormalPhysical(paramsCaptor.capture(), same(assocStub), eq("normal"));
		Map<String, Object> coreParams = paramsCaptor.getValue();

		Map<String, Object> dmCrmPayload = new LinkedHashMap<>();
		dmCrmPayload.put("company_id", coreParams.get("company_id"));
		dmCrmPayload.put("order_id", coreParams.get("order_id"));

		Map<String, Object> parentPayload = new LinkedHashMap<>();
		parentPayload.put("order_id", 200L);
		parentPayload.put("company_id", 100L);
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
						"trace-parent-normal-order-delivery-event235-entry02-wxapp-frontapi",
						null);
		assertThat(parentTemplate.messageType()).isEqualTo(DispatchMessageType.EVENT);
		assertThat(parentTemplate.dispatchMode()).isEqualTo(DispatchMode.ASYNC);
		assertThat(parentTemplate.driverType()).isEqualTo(DispatchDriverType.REDIS);
		assertThat(parentTemplate.messageName()).isEqualTo(OrdersDispatchEventNames.EVENT_NORMAL_ORDER_DELIVERY);
		assertThat(parentTemplate.payload()).containsExactlyEntriesOf(parentPayload);
		assertThat(parentTemplate.queue()).isNull();
		assertThat(parentTemplate.delay()).isNull();
		assertThat(parentTemplate.retryPolicy()).isEqualTo(RetryPolicy.platformDefault());
		assertThat(parentTemplate.occurredAt()).isEqualTo(Instant.parse("2026-05-10T12:00:00Z"));
		assertThat(parentTemplate.traceId())
				.isEqualTo("trace-parent-normal-order-delivery-event235-entry02-wxapp-frontapi");
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
						"trace-frontapi-wxapporder-delivery-dmcrm-event235-entry02",
						OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_DELIVERY_PUSH_DM_CRM_ON_NORMAL_ORDER_DELIVERY);
		assertThat(dmCrmChild.messageType()).isEqualTo(DispatchMessageType.EVENT);
		assertThat(dmCrmChild.dispatchMode()).isEqualTo(DispatchMode.ASYNC);
		assertThat(dmCrmChild.driverType()).isEqualTo(DispatchDriverType.REDIS);
		assertThat(dmCrmChild.messageName()).isEqualTo(OrdersDispatchEventNames.EVENT_NORMAL_ORDER_DELIVERY);
		assertThat(dmCrmChild.payload()).containsExactlyEntriesOf(dmCrmPayload);
		assertThat(dmCrmChild.queue()).isEqualTo("default");
		assertThat(dmCrmChild.delay()).isNull();
		assertThat(dmCrmChild.retryPolicy()).isEqualTo(RetryPolicy.platformDefault());
		assertThat(dmCrmChild.occurredAt()).isEqualTo(Instant.parse("2026-05-10T12:00:00Z"));
		assertThat(dmCrmChild.traceId()).isEqualTo("trace-frontapi-wxapporder-delivery-dmcrm-event235-entry02");
		assertThat(dmCrmChild.listenerName())
				.isEqualTo(OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_DELIVERY_PUSH_DM_CRM_ON_NORMAL_ORDER_DELIVERY);

		runtime.consume(dmCrmChild, 1);

		verify(orderDeliveryDmCrmOnNormalOrderDeliveryProcessor, times(1)).handle(dmCrmPayload);
	}

	@Test
	@DisplayName(
			"event237 entry-02 — EVENT_NORMAL_ORDER_DELIVERY wxapp FrontApi order/delivery: DmCrm child DispatchMessage literals + trace + DispatchConsumerRuntime")
	void delivery_whenNormalPhysical_thenPlanSection6DmCrmChildDispatchMessageLiteralsAndWxappTraceMatchAndDispatchConsumerRuntimeConsumes_event237_entry02() {
		assocStub.setCompanyId(303L);
		assocStub.setOrderId(404L);
		when(orderAssociationsMapper.selectOne(any())).thenReturn(assocStub);
		when(orderAssociationEffectiveTypeService.effectiveOrderType(assocStub)).thenReturn("normal");
		when(orderAssociationAssociationDataAssembler.toAssociationDataMap(any(OrderAssociations.class)))
				.thenReturn(Map.of("order_id", 404L, "company_id", 303L));

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("order_id", "404");
		merged.put("delivery_corp", "SF");
		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", 303L);
		auth.put("user_id", 55L);

		wxappOrderDeliveryService.delivery(httpServletRequest, merged, auth);

		verify(adminNormalOrderDeliveryCoreService, times(1))
				.deliveryNormalPhysical(paramsCaptor.capture(), same(assocStub), eq("normal"));
		Map<String, Object> coreParams = paramsCaptor.getValue();

		Map<String, Object> dmCrmPayload = new LinkedHashMap<>();
		dmCrmPayload.put("company_id", coreParams.get("company_id"));
		dmCrmPayload.put("order_id", coreParams.get("order_id"));

		Map<String, Object> parentPayload = new LinkedHashMap<>();
		parentPayload.put("order_id", 404L);
		parentPayload.put("company_id", 303L);
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
						"trace-parent-normal-order-delivery-event237-entry02-wxapp-frontapi",
						null);
		assertThat(parentTemplate.messageType()).isEqualTo(DispatchMessageType.EVENT);
		assertThat(parentTemplate.dispatchMode()).isEqualTo(DispatchMode.ASYNC);
		assertThat(parentTemplate.driverType()).isEqualTo(DispatchDriverType.REDIS);
		assertThat(parentTemplate.messageName()).isEqualTo(OrdersDispatchEventNames.EVENT_NORMAL_ORDER_DELIVERY);
		assertThat(parentTemplate.payload()).containsExactlyEntriesOf(parentPayload);
		assertThat(parentTemplate.queue()).isNull();
		assertThat(parentTemplate.delay()).isNull();
		assertThat(parentTemplate.retryPolicy()).isEqualTo(RetryPolicy.platformDefault());
		assertThat(parentTemplate.occurredAt()).isEqualTo(Instant.parse("2026-05-10T12:00:00Z"));
		assertThat(parentTemplate.traceId())
				.isEqualTo("trace-parent-normal-order-delivery-event237-entry02-wxapp-frontapi");
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
						"trace-frontapi-wxapporder-delivery-dmcrm-event237-entry02",
						OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_DELIVERY_PUSH_DM_CRM_ON_NORMAL_ORDER_DELIVERY);
		assertThat(dmCrmChild.messageType()).isEqualTo(DispatchMessageType.EVENT);
		assertThat(dmCrmChild.dispatchMode()).isEqualTo(DispatchMode.ASYNC);
		assertThat(dmCrmChild.driverType()).isEqualTo(DispatchDriverType.REDIS);
		assertThat(dmCrmChild.messageName()).isEqualTo(OrdersDispatchEventNames.EVENT_NORMAL_ORDER_DELIVERY);
		assertThat(dmCrmChild.payload()).containsExactlyEntriesOf(dmCrmPayload);
		assertThat(dmCrmChild.queue()).isEqualTo("default");
		assertThat(dmCrmChild.delay()).isNull();
		assertThat(dmCrmChild.retryPolicy()).isEqualTo(RetryPolicy.platformDefault());
		assertThat(dmCrmChild.occurredAt()).isEqualTo(Instant.parse("2026-05-10T12:00:00Z"));
		assertThat(dmCrmChild.traceId()).isEqualTo("trace-frontapi-wxapporder-delivery-dmcrm-event237-entry02");
		assertThat(dmCrmChild.listenerName())
				.isEqualTo(OrdersDispatchEventNames.LISTENER_THIRDPARTY_ORDER_DELIVERY_PUSH_DM_CRM_ON_NORMAL_ORDER_DELIVERY);

		runtime.consume(dmCrmChild, 1);

		verify(orderDeliveryDmCrmOnNormalOrderDeliveryProcessor, times(1)).handle(dmCrmPayload);
	}

	@Test
	@DisplayName("WxappOrderDeliveryService — non-normal physical effective types do not invoke core")
	void delivery_whenNotNormalPhysicalFamily_neverInvokesCore() {
		when(orderAssociationEffectiveTypeService.effectiveOrderType(assocStub)).thenReturn("service");

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("order_id", "200");
		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", 100L);
		auth.put("user_id", 55L);

		wxappOrderDeliveryService.delivery(httpServletRequest, merged, auth);

		verify(adminNormalOrderDeliveryCoreService, never()).deliveryNormalPhysical(any(), any(), any());
	}
}
