package cn.shopex.ecshopx.orders.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.NormalOrderCancelDispatchPublisher;
import cn.shopex.ecshopx.common.inventory.ItemInventoryLineContext;
import cn.shopex.ecshopx.common.port.order.EmployeePurchaseOrderCancelRestorePort;
import cn.shopex.ecshopx.common.port.order.OrderCancelItemStoreRestorePort;
import cn.shopex.ecshopx.common.port.order.OrderCancelMarketingJoinCountPort;
import cn.shopex.ecshopx.common.port.order.OrderCancelSeckillTicketPort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.port.order.PartialCancelOrderItemRow;
import cn.shopex.ecshopx.common.port.order.PartialCancelPromotionRestorePort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OfflinePayment;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.OrderPromotions;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.OrderPromotionsMapper;
import cn.shopex.ecshopx.orders.mapper.OfflinePaymentMapper;
import cn.shopex.ecshopx.common.dispatch.ConsumptionOrderJobDispatchPublisher;
import cn.shopex.ecshopx.common.dispatch.FinishOrderJobDispatchPublisher;
import cn.shopex.ecshopx.orders.service.admin.NormalOrderCancelDiscountRestoreService;
import cn.shopex.ecshopx.orders.service.invoice.OrderInvoiceCancelOnOrderCancelService;
import cn.shopex.ecshopx.point.service.PointMemberCancelOrderReturnPointsService;
import cn.shopex.ecshopx.point.service.PointMemberMinusOrderUppointsService;
import cn.shopex.ecshopx.thirdparty.service.marketingcenter.ScheduleCancelOrdersPushMarketingCenterProcessor;
import cn.shopex.ecshopx.supplier.domain.SupplierOrder;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NormalOrderCronServiceTest {

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
	@Mock NormalOrderCancelDiscountRestoreService normalOrderCancelDiscountRestoreService;
	@Mock OfflinePaymentMapper offlinePaymentMapper;
	@Mock OrderProcessLogPublishPort orderProcessLogPublishPort;
	@Mock NormalOrderCancelDispatchPublisher normalOrderCancelDispatchPublisher;
	@Mock ConsumptionOrderJobDispatchPublisher consumptionOrderJobDispatchPublisher;
	@Mock FinishOrderJobDispatchPublisher finishOrderJobDispatchPublisher;

	@InjectMocks
	NormalOrderCronService service;

	@org.junit.jupiter.api.BeforeEach
	void injectOemFlag() {
		ReflectionTestUtils.setField(service, "oemShuyun", false);
	}

	// §3 step 3: totalCount=0 早退出
	@Test
	void scheduleCancelOrders_totalZero_returnsZero() {
		when(normalOrdersMapper.selectCount(any())).thenReturn(0L);
		int result = service.scheduleCancelOrders();
		assertThat(result).isEqualTo(0);
		verify(normalOrdersMapper, never()).selectList(any());
	}

	// §3 step 5-2: selectPage 返回空继续下一页
	@Test
	void scheduleCancelOrders_pageEmpty_skips() {
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of());
		int result = service.scheduleCancelOrders();
		assertThat(result).isEqualTo(0);
		verify(normalOrdersMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
	}

	// §3 step 5-3-a: 主流程批量取消
	@Test
	void scheduleCancelOrders_mainFlow_batchCancels() {
		NormalOrders o1 = makeOrder(101L, 1L, 10L, "normal", "wechat", 0, 0);
		NormalOrders o2 = makeOrder(102L, 1L, 11L, "normal", "wechat", 0, 0);
		when(normalOrdersMapper.selectCount(any())).thenReturn(2L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(o1, o2));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		int result = service.scheduleCancelOrders();
		assertThat(result).isEqualTo(2);
		verify(normalOrdersMapper, times(1)).update(any(), any(LambdaUpdateWrapper.class));
		verify(orderAssociationsMapper, times(1)).update(any(), any(LambdaUpdateWrapper.class));
	}

	// §3 step 5-3-d 发票行
	@Test
	void scheduleCancelOrders_invoiceCancel_called() {
		NormalOrders order = makeOrder(100L, 1L, 10L, "normal", "wechat", 0, 0);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		service.scheduleCancelOrders();
		verify(orderInvoiceCancelOnOrderCancelService, times(1))
				.updateInvoiceStatusCancel(eq(1L), eq(100L), eq("cancel"));
	}

	// §3 step 5-3-d supplier 有记录
	@Test
	void scheduleCancelOrders_supplierExists_cancels() {
		NormalOrders order = makeOrder(100L, 1L, 10L, "normal", "wechat", 0, 0);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(1L);
		service.scheduleCancelOrders();
		verify(supplierOrderMapper, times(1)).update(any(), any(LambdaUpdateWrapper.class));
	}

	// §3 step 5-3-d supplier 无记录
	@Test
	void scheduleCancelOrders_noSupplier_skips() {
		NormalOrders order = makeOrder(100L, 1L, 10L, "normal", "wechat", 0, 0);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		service.scheduleCancelOrders();
		verify(supplierOrderMapper, never()).update(any(), any());
	}

	// §3 step 5-3-d marketing Redis 有关联
	@Test
	void scheduleCancelOrders_marketingJoinCount_called() {
		NormalOrders order = makeOrder(100L, 1L, 10L, "normal", "wechat", 0, 0);
		OrderPromotions promo = makePromotion(100L, 10L, 999L, "marketing_activity");
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of(promo));
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		service.scheduleCancelOrders();
		verify(orderCancelMarketingJoinCountPort, times(1)).lessJoinCount(eq(1L), eq(10L), eq(999L));
	}

	// §3 step 5-3-d marketing 无关联
	@Test
	void scheduleCancelOrders_noMarketingJoin_skips() {
		NormalOrders order = makeOrder(100L, 1L, 10L, "normal", "wechat", 0, 0);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		service.scheduleCancelOrders();
		verify(orderCancelMarketingJoinCountPort, never()).lessJoinCount(anyLong(), anyLong(), anyLong());
	}

	// §3 step 5-3-d point 积分回退
	@Test
	void scheduleCancelOrders_pointReturn_called() {
		NormalOrders order = makeOrder(100L, 1L, 10L, "normal", "wechat", 100, 0);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		service.scheduleCancelOrders();
		verify(pointMemberCancelOrderReturnPointsService, times(1))
				.cancelOrderReturnBackPoints(any(Map.class));
	}

	// §3 step 5-3-d point payType=point 跳过
	@Test
	void scheduleCancelOrders_pointPayType_skips() {
		NormalOrders order = makeOrder(100L, 1L, 10L, "normal", "point", 100, 0);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		service.scheduleCancelOrders();
		verify(pointMemberCancelOrderReturnPointsService, never()).cancelOrderReturnBackPoints(any());
	}

	// §3 step 5-3-d point pointUse=0 跳过
	@Test
	void scheduleCancelOrders_zeroPointUse_skips() {
		NormalOrders order = makeOrder(100L, 1L, 10L, "normal", "wechat", 0, 0);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		service.scheduleCancelOrders();
		verify(pointMemberCancelOrderReturnPointsService, never()).cancelOrderReturnBackPoints(any());
	}

	// §3 step 5-3-d uppoint 有上分
	@Test
	void scheduleCancelOrders_uppointReturn_called() {
		NormalOrders order = makeOrder(100L, 1L, 10L, "normal", "wechat", 0, 50);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		service.scheduleCancelOrders();
		verify(pointMemberMinusOrderUppointsService, times(1)).minusOrderUppoints(any(Map.class));
	}

	// §3 step 5-3-d uppointUse=0 跳过
	@Test
	void scheduleCancelOrders_zeroUppoint_skips() {
		NormalOrders order = makeOrder(100L, 1L, 10L, "normal", "wechat", 0, 0);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		service.scheduleCancelOrders();
		verify(pointMemberMinusOrderUppointsService, never()).minusOrderUppoints(any());
	}

	// §3 step 5-3-d order process log via dispatch port
	@Test
	void scheduleCancelOrders_publishesProcessLogEvent() {
		NormalOrders order = makeOrder(100L, 1L, 10L, "normal", "wechat", 0, 0);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		service.scheduleCancelOrders();
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
		verify(orderProcessLogPublishPort, times(1)).publish(cap.capture());
		Map<String, Object> payload = cap.getValue();
		assertThat(payload).containsEntry("remarks", "订单取消");
		assertThat(payload).containsEntry("detail", "订单单号：100，取消订单退款");
		assertThat(payload).containsEntry("operator_type", "system");
		assertThat(payload).containsEntry("operator_id", 0L);
		verify(normalOrderCancelDispatchPublisher, times(1)).publish(any(Map.class));
	}

	// §3 step 5-3-e employee_purchase 恢复
	@Test
	void scheduleCancelOrders_employeePurchaseRestore_called() {
		NormalOrders order = makeOrder(100L, 1L, 10L, "employee_purchase", "wechat", 0, 0);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		service.scheduleCancelOrders();
		verify(employeePurchaseOrderCancelRestorePort, times(1)).restoreOnOrderCancel(eq(100L), eq(1L));
	}

	// §3 step 5-3-e normal 不触发内购恢复
	@Test
	void scheduleCancelOrders_normalOrder_noEmployeePurchaseRestore() {
		NormalOrders order = makeOrder(100L, 1L, 10L, "normal", "wechat", 0, 0);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		service.scheduleCancelOrders();
		verify(employeePurchaseOrderCancelRestorePort, never()).restoreOnOrderCancel(anyLong(), anyLong());
	}

	// §3 step 5-3-e 限时特惠件数+金额额度回补（活动价）
	@Test
	void scheduleCancelOrders_limitedTimeSaleQuotaRestore_usesActivityPrice() {
		NormalOrders order = makeOrder(100L, 1L, 10L, "normal", "wechat", 0, 0);
		NormalOrdersItems item = makeItem(100L, 1L, 10L, 200L, 2);
		item.setPrice(800);
		item.setDiscountFee(1400);
		OrderPromotions promo = makeSeckillPromotion(100L, 200L, 300L);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(item));
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of(promo));
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		service.scheduleCancelOrders();
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<PartialCancelOrderItemRow>> captor = ArgumentCaptor.forClass(List.class);
		verify(partialCancelPromotionRestorePort, times(1))
				.restoreLimitedTimeSaleUserBuys(eq(1L), eq(100L), captor.capture());
		PartialCancelOrderItemRow row = captor.getValue().get(0);
		assertThat(row.itemId()).isEqualTo(200L);
		assertThat(row.cancelItemNum()).isEqualTo(2);
		assertThat(row.priceFen()).isEqualTo(100);
		verify(orderCancelSeckillTicketPort, never())
				.restoreUserBuysStore(anyLong(), anyLong(), anyLong(), anyLong(), anyInt());
	}

	// §3 step 5-3-e ItemStoreService 商品库存回补
	@Test
	void scheduleCancelOrders_itemStoreRestore_called() {
		NormalOrders order = makeOrder(100L, 1L, 10L, "normal", "wechat", 0, 0);
		NormalOrdersItems item = makeItem(100L, 1L, 10L, 200L, 3);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(item));
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		service.scheduleCancelOrders();
		verify(orderCancelItemStoreRestorePort, times(1))
				.restoreStore(any(ItemInventoryLineContext.class), eq(3));
	}

	// §3 step 5-3-e reduceLimitPerson 限购恢复
	@Test
	void scheduleCancelOrders_reduceLimitPerson_called() {
		NormalOrders order = makeOrder(100L, 1L, 10L, "normal", "wechat", 0, 0);
		NormalOrdersItems item = makeItem(100L, 1L, 10L, 200L, 2);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(item));
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		service.scheduleCancelOrders();
		verify(partialCancelPromotionRestorePort, times(1))
				.reduceLimitPerson(eq(1L), eq(10L), eq(200L), eq(2));
	}

	// §3 step 5-3-f 优惠券回调
	@Test
	void scheduleCancelOrders_couponCallback_called() throws Exception {
		NormalOrders order = makeOrder(100L, 1L, 10L, "normal", "wechat", 0, 0);
		order.setDiscountInfo("[{\"coupon_code\":[\"abc123\"]}]");
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		service.scheduleCancelOrders();
		verify(normalOrderCancelDiscountRestoreService, times(1)).restoreOnAutoCancel(order);
	}

	// §3 step 5-3-f 优惠券回调失败不阻断
	@Test
	void scheduleCancelOrders_couponCallbackFails_continuesGracefully() throws Exception {
		NormalOrders order = makeOrder(100L, 1L, 10L, "normal", "wechat", 0, 0);
		order.setDiscountInfo("[{\"coupon_code\":[\"fail1\",\"ok2\"]}]");
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		int result = service.scheduleCancelOrders();
		assertThat(result).isEqualTo(1);
		verify(normalOrderCancelDiscountRestoreService, times(1)).restoreOnAutoCancel(order);
	}

	// §3 step 5-3-f type=member_tag_targeted_promotion SCD 恢复
	@Test
	void scheduleCancelOrders_scdRestore_called() throws Exception {
		NormalOrders order = makeOrder(100L, 1L, 10L, "normal", "wechat", 0, 0);
		order.setDiscountInfo("[{\"type\":\"member_tag_targeted_promotion\",\"activity_id\":5,\"discount_fee\":200}]");
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		service.scheduleCancelOrders();
		verify(normalOrderCancelDiscountRestoreService, times(1)).restoreOnAutoCancel(order);
	}

	// §3 step 5-3-f discount_info 为空跳过
	@Test
	void scheduleCancelOrders_emptyDiscountInfo_skips() {
		NormalOrders order = makeOrder(100L, 1L, 10L, "normal", "wechat", 0, 0);
		order.setDiscountInfo(null);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		service.scheduleCancelOrders();
		verify(normalOrderCancelDiscountRestoreService, times(1)).restoreOnAutoCancel(order);
	}

	// ========== scheduleOfflinePayCancelOrders ==========

	// §3 步骤 2: totalCount==0 早退出
	@Test
	void scheduleOfflinePayCancelOrders_totalZero_returnsZero() {
		when(normalOrdersMapper.countOfflinePayCancelable(anyLong())).thenReturn(0L);
		int result = service.scheduleOfflinePayCancelOrders();
		assertThat(result).isEqualTo(0);
		verify(normalOrdersMapper, never()).selectOfflinePayCancelable(anyLong(), anyInt());
		verify(normalOrdersMapper, never()).update(any(), any());
		verify(orderAssociationsMapper, never()).update(any(), any());
		verify(offlinePaymentMapper, never()).update(any(), any());
	}

	// §3 步骤 3→4-A-1→4-A-5→4-A-8: 主流程（1笔普通线下支付订单）
	@Test
	void scheduleOfflinePayCancelOrders_mainFlow_batchCancels() {
		NormalOrders order = makeOfflineOrder(200L, 2L, 20L, "normal", 0, 0);
		when(normalOrdersMapper.countOfflinePayCancelable(anyLong())).thenReturn(1L);
		when(normalOrdersMapper.selectOfflinePayCancelable(anyLong(), anyInt())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		int result = service.scheduleOfflinePayCancelOrders();
		assertThat(result).isEqualTo(1);
		verify(normalOrdersMapper, times(1)).update(any(), any());
		verify(orderAssociationsMapper, times(1)).update(any(), any());
		verify(offlinePaymentMapper, times(1)).update(any(), any());
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> logCap = ArgumentCaptor.forClass(Map.class);
		verify(orderProcessLogPublishPort, times(1)).publish(logCap.capture());
		Map<String, Object> logPayload = logCap.getValue();
		assertThat(logPayload).containsEntry("remarks", "订单取消");
		assertThat(logPayload).containsEntry("detail", "订单单号：200，取消订单退款");
		assertThat(logPayload).containsEntry("operator_type", "system");
		assertThat(logPayload).containsEntry("operator_id", 0L);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> marketingCap = ArgumentCaptor.forClass(Map.class);
		verify(normalOrderCancelDispatchPublisher, times(1)).publish(marketingCap.capture());
		Map<String, Object> marketingPayload = marketingCap.getValue();
		assertThat(marketingPayload).containsEntry("order_id", 200L);
		assertThat(marketingPayload).containsEntry("company_id", 2L);
		assertThat(marketingPayload)
				.containsEntry("source", ScheduleCancelOrdersPushMarketingCenterProcessor.CRON_SCHEDULE_CANCEL_SOURCE);
	}

	// §3 步骤 4-A-4-b: 积分回退（point_use > 0, pay_type != 'point'）
	@Test
	void scheduleOfflinePayCancelOrders_pointReturn_called() {
		NormalOrders order = makeOfflineOrder(200L, 2L, 20L, "normal", 100, 0);
		when(normalOrdersMapper.countOfflinePayCancelable(anyLong())).thenReturn(1L);
		when(normalOrdersMapper.selectOfflinePayCancelable(anyLong(), anyInt())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		service.scheduleOfflinePayCancelOrders();
		verify(pointMemberCancelOrderReturnPointsService, times(1)).cancelOrderReturnBackPoints(any(Map.class));
		verify(offlinePaymentMapper, times(1)).update(any(), any());
	}

	// §3 步骤 4-A-4-b: 积分跳过（point_use=0）
	@Test
	void scheduleOfflinePayCancelOrders_zeroPointUse_skipsPointReturn() {
		NormalOrders order = makeOfflineOrder(200L, 2L, 20L, "normal", 0, 0);
		when(normalOrdersMapper.countOfflinePayCancelable(anyLong())).thenReturn(1L);
		when(normalOrdersMapper.selectOfflinePayCancelable(anyLong(), anyInt())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		service.scheduleOfflinePayCancelOrders();
		verify(pointMemberCancelOrderReturnPointsService, never()).cancelOrderReturnBackPoints(any());
	}

	// §3 步骤 4-A-4-a: 供应商单取消（有供应商子单）
	@Test
	void scheduleOfflinePayCancelOrders_supplierExists_cancels() {
		NormalOrders order = makeOfflineOrder(200L, 2L, 20L, "normal", 0, 0);
		when(normalOrdersMapper.countOfflinePayCancelable(anyLong())).thenReturn(1L);
		when(normalOrdersMapper.selectOfflinePayCancelable(anyLong(), anyInt())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(1L);
		service.scheduleOfflinePayCancelOrders();
		verify(supplierOrderMapper, times(1)).update(any(), any());
	}

	// §3 步骤 4-A-4-a: 供应商单跳过（无子单）
	@Test
	void scheduleOfflinePayCancelOrders_noSupplier_skips() {
		NormalOrders order = makeOfflineOrder(200L, 2L, 20L, "normal", 0, 0);
		when(normalOrdersMapper.countOfflinePayCancelable(anyLong())).thenReturn(1L);
		when(normalOrdersMapper.selectOfflinePayCancelable(anyLong(), anyInt())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		service.scheduleOfflinePayCancelOrders();
		verify(supplierOrderMapper, never()).update(any(), any());
	}

	// §3 步骤 4-A-2-a: 限时特惠件数+金额额度回补（活动价）
	@Test
	void scheduleOfflinePayCancelOrders_limitedTimeSaleQuotaRestore_usesActivityPrice() {
		NormalOrders order = makeOfflineOrder(200L, 2L, 20L, "normal", 0, 0);
		NormalOrdersItems item = makeItem(200L, 2L, 20L, 300L, 2);
		item.setPrice(800);
		item.setDiscountFee(1400);
		OrderPromotions promo = makeSeckillPromotion(200L, 300L, 400L);
		when(normalOrdersMapper.countOfflinePayCancelable(anyLong())).thenReturn(1L);
		when(normalOrdersMapper.selectOfflinePayCancelable(anyLong(), anyInt())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(item));
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of(promo));
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		service.scheduleOfflinePayCancelOrders();
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<PartialCancelOrderItemRow>> captor = ArgumentCaptor.forClass(List.class);
		verify(partialCancelPromotionRestorePort, times(1))
				.restoreLimitedTimeSaleUserBuys(eq(2L), eq(200L), captor.capture());
		PartialCancelOrderItemRow row = captor.getValue().get(0);
		assertThat(row.itemId()).isEqualTo(300L);
		assertThat(row.cancelItemNum()).isEqualTo(2);
		assertThat(row.priceFen()).isEqualTo(100);
		verify(orderCancelSeckillTicketPort, never())
				.restoreUserBuysStore(anyLong(), anyLong(), anyLong(), anyLong(), anyInt());
	}

	// §3 步骤 4-A-2-b: 商品总库存回补（is_total_store=true）
	@Test
	void scheduleOfflinePayCancelOrders_itemStoreRestoreTotal_called() {
		NormalOrders order = makeOfflineOrder(200L, 2L, 20L, "normal", 0, 0);
		NormalOrdersItems item = makeItem(200L, 2L, 20L, 300L, 3);
		item.setIsTotalStore(true);
		when(normalOrdersMapper.countOfflinePayCancelable(anyLong())).thenReturn(1L);
		when(normalOrdersMapper.selectOfflinePayCancelable(anyLong(), anyInt())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(item));
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		service.scheduleOfflinePayCancelOrders();
		verify(orderCancelItemStoreRestorePort, times(1))
				.restoreStore(any(ItemInventoryLineContext.class), eq(3));
	}

	// §3 步骤 4-A-2-b: 分销商库存回补（is_total_store=false）
	@Test
	void scheduleOfflinePayCancelOrders_itemStoreRestoreDistributor_called() {
		NormalOrders order = makeOfflineOrder(200L, 2L, 20L, "normal", 0, 0);
		NormalOrdersItems item = makeItem(200L, 2L, 20L, 300L, 2);
		item.setIsTotalStore(false);
		item.setDistributorId(99L);
		when(normalOrdersMapper.countOfflinePayCancelable(anyLong())).thenReturn(1L);
		when(normalOrdersMapper.selectOfflinePayCancelable(anyLong(), anyInt())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(item));
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		service.scheduleOfflinePayCancelOrders();
		@SuppressWarnings("unchecked")
		ArgumentCaptor<ItemInventoryLineContext> ctxCap = ArgumentCaptor.forClass(ItemInventoryLineContext.class);
		verify(orderCancelItemStoreRestorePort, times(1)).restoreStore(ctxCap.capture(), eq(2));
		assertThat(ctxCap.getValue().itemId()).isEqualTo(300L);
		assertThat(ctxCap.getValue().companyId()).isEqualTo(2L);
		assertThat(ctxCap.getValue().isTotalStore()).isFalse();
		assertThat(ctxCap.getValue().distributorId()).isEqualTo(99L);
	}

	// §3 步骤 4-A-2-c: 限购次数回退
	@Test
	void scheduleOfflinePayCancelOrders_reduceLimitPerson_called() {
		NormalOrders order = makeOfflineOrder(200L, 2L, 20L, "normal", 0, 0);
		NormalOrdersItems item = makeItem(200L, 2L, 20L, 300L, 1);
		when(normalOrdersMapper.countOfflinePayCancelable(anyLong())).thenReturn(1L);
		when(normalOrdersMapper.selectOfflinePayCancelable(anyLong(), anyInt())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(item));
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		service.scheduleOfflinePayCancelOrders();
		verify(partialCancelPromotionRestorePort, times(1)).reduceLimitPerson(eq(2L), eq(20L), eq(300L), eq(1));
	}

	// §3 步骤 4-A-6: 内购活动恢复（employee_purchase）
	@Test
	void scheduleOfflinePayCancelOrders_employeePurchaseRestore_called() {
		NormalOrders order = makeOfflineOrder(200L, 2L, 20L, "employee_purchase", 0, 0);
		when(normalOrdersMapper.countOfflinePayCancelable(anyLong())).thenReturn(1L);
		when(normalOrdersMapper.selectOfflinePayCancelable(anyLong(), anyInt())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		service.scheduleOfflinePayCancelOrders();
		verify(employeePurchaseOrderCancelRestorePort, times(1)).restoreOnOrderCancel(eq(200L), eq(2L));
	}

	// §3 步骤 4-A-6: 内购跳过（非 employee_purchase）
	@Test
	void scheduleOfflinePayCancelOrders_normalOrder_noEmployeePurchaseRestore() {
		NormalOrders order = makeOfflineOrder(200L, 2L, 20L, "normal", 0, 0);
		when(normalOrdersMapper.countOfflinePayCancelable(anyLong())).thenReturn(1L);
		when(normalOrdersMapper.selectOfflinePayCancelable(anyLong(), anyInt())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		service.scheduleOfflinePayCancelOrders();
		verify(employeePurchaseOrderCancelRestorePort, never()).restoreOnOrderCancel(anyLong(), anyLong());
	}

	// §3 步骤 4-A-7-a: 优惠券回退
	@Test
	void scheduleOfflinePayCancelOrders_couponCallback_called() {
		NormalOrders order = makeOfflineOrder(200L, 2L, 20L, "normal", 0, 0);
		order.setDiscountInfo("[{\"coupon_code\":[\"CODE1\"]}]");
		when(normalOrdersMapper.countOfflinePayCancelable(anyLong())).thenReturn(1L);
		when(normalOrdersMapper.selectOfflinePayCancelable(anyLong(), anyInt())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		service.scheduleOfflinePayCancelOrders();
		verify(normalOrderCancelDiscountRestoreService, times(1)).restoreOnAutoCancel(order);
	}

	// §3 步骤 4-A-7-a: 优惠券 callbackUserCard 异常被吞，offlinePayment 仍被更新
	@Test
	void scheduleOfflinePayCancelOrders_couponCallbackFails_methodContinues() {
		NormalOrders order = makeOfflineOrder(200L, 2L, 20L, "normal", 0, 0);
		order.setDiscountInfo("[{\"coupon_code\":[\"FAIL1\"]}]");
		when(normalOrdersMapper.countOfflinePayCancelable(anyLong())).thenReturn(1L);
		when(normalOrdersMapper.selectOfflinePayCancelable(anyLong(), anyInt())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		int result = service.scheduleOfflinePayCancelOrders();
		assertThat(result).isEqualTo(1);
		verify(normalOrderCancelDiscountRestoreService, times(1)).restoreOnAutoCancel(order);
		verify(offlinePaymentMapper, times(1)).update(any(), any());
	}

	// §3 步骤 4-A-7-b: 定向促销恢复
	@Test
	void scheduleOfflinePayCancelOrders_scdRestore_called() {
		NormalOrders order = makeOfflineOrder(200L, 2L, 20L, "normal", 0, 0);
		order.setDiscountInfo("[{\"type\":\"member_tag_targeted_promotion\",\"activity_id\":5}]");
		when(normalOrdersMapper.countOfflinePayCancelable(anyLong())).thenReturn(1L);
		when(normalOrdersMapper.selectOfflinePayCancelable(anyLong(), anyInt())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		service.scheduleOfflinePayCancelOrders();
		verify(normalOrderCancelDiscountRestoreService, times(1)).restoreOnAutoCancel(order);
	}

	// §3 步骤 4-A-8: offline_payment 更新失败被吞，cancelledCount 正常计入
	@Test
	void scheduleOfflinePayCancelOrders_offlinePaymentUpdateFails_methodContinues() {
		NormalOrders order = makeOfflineOrder(200L, 2L, 20L, "normal", 0, 0);
		when(normalOrdersMapper.countOfflinePayCancelable(anyLong())).thenReturn(1L);
		when(normalOrdersMapper.selectOfflinePayCancelable(anyLong(), anyInt())).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		when(orderPromotionsMapper.selectList(any())).thenReturn(List.of());
		when(supplierOrderMapper.selectCount(any())).thenReturn(0L);
		doThrow(new RuntimeException("db error")).when(offlinePaymentMapper).update(any(), any());
		int result = service.scheduleOfflinePayCancelOrders();
		assertThat(result).isEqualTo(1);
	}

	// ---- helpers ----

	private NormalOrders makeOfflineOrder(long orderId, long companyId, long userId,
			String orderClass, int pointUse, int uppointUse) {
		return makeOrder(orderId, companyId, userId, orderClass, "offline_pay", pointUse, uppointUse);
	}

	private NormalOrders makeOrder(long orderId, long companyId, long userId,
			String orderClass, String payType, int pointUse, int uppointUse) {
		NormalOrders o = new NormalOrders();
		o.setOrderId(orderId);
		o.setCompanyId(companyId);
		o.setUserId(userId);
		o.setOrderClass(orderClass);
		o.setPayType(payType);
		o.setOrderStatus("NOTPAY");
		o.setPointUse(pointUse);
		o.setUppointUse(uppointUse);
		o.setAutoCancelTime(String.valueOf(System.currentTimeMillis() / 1000L - 120));
		return o;
	}

	private NormalOrdersItems makeItem(long orderId, long companyId, long userId, long itemId, int num) {
		NormalOrdersItems i = new NormalOrdersItems();
		i.setOrderId(orderId);
		i.setCompanyId(companyId);
		i.setUserId(userId);
		i.setItemId(itemId);
		i.setNum(num);
		i.setIsTotalStore(true);
		i.setDistributorId(0L);
		return i;
	}

	private OrderPromotions makePromotion(long moid, long userId, long activityId, String activityType) {
		OrderPromotions p = new OrderPromotions();
		p.setMoid(moid);
		p.setUserId(userId);
		p.setActivityId(activityId);
		p.setActivityType(activityType);
		p.setCompanyId(1L);
		return p;
	}

	private OrderPromotions makeSeckillPromotion(long moid, long itemId, long activityId) {
		OrderPromotions p = new OrderPromotions();
		p.setMoid(moid);
		p.setItemId(itemId);
		p.setActivityId(activityId);
		p.setActivityType("limited_time_sale");
		p.setUserId(10L);
		p.setCompanyId(1L);
		return p;
	}

	/** analysis §3 1.1 */
	@Test
	void scheduleConsumptionOrders_oemTrue_returnsZeroAndNoBatch() {
		ReflectionTestUtils.setField(service, "oemShuyun", true);
		int r = service.scheduleConsumptionOrders();
		assertThat(r).isEqualTo(0);
		verify(consumptionOrderJobDispatchPublisher, never()).publish(any());
		ReflectionTestUtils.setField(service, "oemShuyun", false);
	}

	/** analysis §3 1.2 */
	@Test
	void scheduleConsumptionOrders_nonOem_publishesOnceReturnsOneAndNeverCallsBatchDirectly() {
		int r = service.scheduleConsumptionOrders();
		assertThat(r).isEqualTo(1);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
		verify(consumptionOrderJobDispatchPublisher, times(1)).publish(cap.capture());
		assertThat(cap.getValue()).containsEntry("orderType", "normal").containsEntry("pageSize", "100");
	}

	/**
	 * scheduleFinishOrders 不随 oem 门控；每 tick 投递一次空 payload 作业并返回 1。
	 */
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void scheduleFinishOrders_alwaysPublishesOnceEmptyMapReturnsOne(boolean oemShuyun) {
		ReflectionTestUtils.setField(service, "oemShuyun", oemShuyun);
		int r = service.scheduleFinishOrders();
		assertThat(r).isEqualTo(1);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
		verify(finishOrderJobDispatchPublisher, times(1)).publish(cap.capture());
		assertThat(cap.getValue()).isEmpty();
		ReflectionTestUtils.setField(service, "oemShuyun", false);
	}
}
