package cn.shopex.ecshopx.orders.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.ConsumptionOrderJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.FinishOrderJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.NormalOrderCancelDispatchPublisher;
import cn.shopex.ecshopx.common.port.order.EmployeePurchaseOrderCancelRestorePort;
import cn.shopex.ecshopx.common.port.order.OrderCancelItemStoreRestorePort;
import cn.shopex.ecshopx.common.port.order.OrderCancelMarketingJoinCountPort;
import cn.shopex.ecshopx.common.port.order.OrderCancelScdRestorePort;
import cn.shopex.ecshopx.common.port.order.OrderCancelSeckillTicketPort;
import cn.shopex.ecshopx.common.port.order.OrderCancelUserDiscountRestorePort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.port.order.PartialCancelPromotionRestorePort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OfflinePayment;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.OrderPromotions;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OfflinePaymentMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.OrderPromotionsMapper;
import cn.shopex.ecshopx.orders.service.invoice.OrderInvoiceCancelOnOrderCancelService;
import cn.shopex.ecshopx.point.service.PointMemberCancelOrderReturnPointsService;
import cn.shopex.ecshopx.point.service.PointMemberMinusOrderUppointsService;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Publish-side probe for {@link NormalOrderCronService#scheduleOfflinePayCancelOrders()} order-process-log
 * dispatch path (offline transfer timeout cancel batch), aligned with queued listener payload expectations.
 */
@ExtendWith(MockitoExtension.class)
class NormalOrderCronServiceScheduleOfflinePayCancelLogOrderProcessLogDispatchPublishProbeTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrdersItems.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OrderAssociations.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OrderPromotions.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), SupplierOrder.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OfflinePayment.class);
	}

	@Mock NormalOrdersMapper normalOrdersMapper;
	@Mock OrderAssociationsMapper orderAssociationsMapper;
	@Mock NormalOrdersItemsMapper normalOrdersItemsMapper;
	@Mock OrderPromotionsMapper orderPromotionsMapper;
	@Mock SupplierOrderMapper supplierOrderMapper;
	@Mock OrderInvoiceCancelOnOrderCancelService orderInvoiceCancelOnOrderCancelService;
	@Mock PointMemberCancelOrderReturnPointsService pointMemberCancelOrderReturnPointsService;
	@Mock PointMemberMinusOrderUppointsService pointMemberMinusOrderUppointsService;
	@Mock EmployeePurchaseOrderCancelRestorePort employeePurchaseOrderCancelRestorePort;
	@Mock PartialCancelPromotionRestorePort partialCancelPromotionRestorePort;
	@Mock OrderCancelMarketingJoinCountPort orderCancelMarketingJoinCountPort;
	@Mock OrderCancelItemStoreRestorePort orderCancelItemStoreRestorePort;
	@Mock OrderCancelSeckillTicketPort orderCancelSeckillTicketPort;
	@Mock OrderCancelUserDiscountRestorePort orderCancelUserDiscountRestorePort;
	@Mock OrderCancelScdRestorePort orderCancelScdRestorePort;
	@Mock OfflinePaymentMapper offlinePaymentMapper;
	@Mock OrderProcessLogPublishPort orderProcessLogPublishPort;
	@Mock NormalOrderCancelDispatchPublisher normalOrderCancelDispatchPublisher;
	@Mock ConsumptionOrderJobDispatchPublisher consumptionOrderJobDispatchPublisher;
	@Mock FinishOrderJobDispatchPublisher finishOrderJobDispatchPublisher;

	@InjectMocks NormalOrderCronService service;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void injectObjectMapper() {
		ReflectionTestUtils.setField(service, "objectMapper", objectMapper);
		ReflectionTestUtils.setField(service, "oemShuyun", false);
	}

	@Test
	void scheduleOfflinePayCancelOrders_invokesOrderProcessLogPublishPortOnce_withOfflinePayCancelKernelOplPayload() {
		long orderId = 100L;
		long companyId = 1L;
		NormalOrders order = new NormalOrders();
		order.setOrderId(orderId);
		order.setCompanyId(companyId);
		order.setUserId(10L);
		order.setOrderClass("normal");
		order.setPayType("offline_pay");
		order.setOrderStatus("NOTPAY");
		order.setPointUse(0);
		order.setUppointUse(0);
		order.setAutoCancelTime(String.valueOf(System.currentTimeMillis() / 1000L - 120));

		when(normalOrdersMapper.countOfflinePayCancelable(anyLong())).thenReturn(1L);
		when(normalOrdersMapper.selectOfflinePayCancelable(anyLong(), anyInt())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);

		service.scheduleOfflinePayCancelOrders();

		verify(orderProcessLogPublishPort, times(1))
				.publish(
						argThat(
								map ->
										"订单取消".equals(map.get("remarks"))
												&& ("订单单号：" + orderId + "，取消订单退款").equals(map.get("detail"))
												&& "system".equals(map.get("operator_type"))
												&& Long.valueOf(0L).equals(map.get("operator_id"))
												&& orderId == ((Number) map.get("order_id")).longValue()
												&& companyId == ((Number) map.get("company_id")).longValue()
												&& Boolean.TRUE.equals(map.get("is_show"))
												&& Long.valueOf(0L).equals(map.get("supplier_id"))));
		verify(normalOrderCancelDispatchPublisher, times(1)).publish(any(Map.class));
	}
}
