package cn.shopex.ecshopx.orders.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.port.order.OrderCancelItemStoreRestorePort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.port.order.PartialCancelPromotionRestorePort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.point.service.PointMemberCancelOrderReturnPointsService;
import cn.shopex.ecshopx.point.service.PointMemberMinusOrderUppointsService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupsOrderServiceTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrdersItems.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OrderAssociations.class);
	}

	@Mock NormalOrdersMapper normalOrdersMapper;
	@Mock OrderAssociationsMapper orderAssociationsMapper;
	@Mock NormalOrdersItemsMapper normalOrdersItemsMapper;
	@Mock OrderCancelItemStoreRestorePort orderCancelItemStoreRestorePort;
	@Mock PartialCancelPromotionRestorePort partialCancelPromotionRestorePort;
	@Mock PointMemberCancelOrderReturnPointsService pointMemberCancelOrderReturnPointsService;
	@Mock PointMemberMinusOrderUppointsService pointMemberMinusOrderUppointsService;
	@Mock OrderProcessLogPublishPort orderProcessLogPublishPort;

	@InjectMocks
	GroupsOrderService service;

	// ---- helpers ----

	private NormalOrders makeOrder(long orderId, long companyId, long userId) {
		NormalOrders o = new NormalOrders();
		o.setOrderId(orderId);
		o.setCompanyId(companyId);
		o.setUserId(userId);
		o.setOrderClass("groups");
		o.setOrderStatus("NOTPAY");
		o.setAutoCancelTime(String.valueOf(System.currentTimeMillis() / 1000L - 10));
		o.setPointUse(0);
		o.setUppointUse(0);
		o.setPayType("wechat");
		return o;
	}

	private NormalOrdersItems makeItem(long orderId, long companyId, long userId,
			long itemId, int num, boolean isTotalStore, long distributorId) {
		NormalOrdersItems item = new NormalOrdersItems();
		item.setOrderId(orderId);
		item.setCompanyId(companyId);
		item.setUserId(userId);
		item.setItemId(itemId);
		item.setNum(num);
		item.setIsTotalStore(isTotalStore);
		item.setDistributorId(distributorId);
		return item;
	}

	// ---- §3 步骤 2：totalCount == 0 早退出 ----

	@Test
	void scheduleCancelOrders_noOrders_returnsZeroAndNoUpdates() {
		when(normalOrdersMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
		int result = service.scheduleCancelOrders();
		assertThat(result).isEqualTo(0);
		verify(normalOrdersMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
	}

	// ---- §3 步骤 3-4-A / 3-4-B：批量取消 1 笔订单 ----

	@Test
	void scheduleCancelOrders_oneOrder_updatesMainAndAssociation() {
		NormalOrders order = makeOrder(1001L, 100L, 200L);
		when(normalOrdersMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
		when(normalOrdersMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

		int result = service.scheduleCancelOrders();

		assertThat(result).isEqualTo(1);
		verify(normalOrdersMapper, times(1)).update(eq(null), any(LambdaUpdateWrapper.class));
		verify(orderAssociationsMapper, times(1)).update(eq(null), any(LambdaUpdateWrapper.class));
	}

	// ---- §3 步骤 3-4-C-1：item 库存恢复 ----

	@Test
	void scheduleCancelOrders_itemWithStore_restoresItemStore() {
		NormalOrders order = makeOrder(1001L, 100L, 200L);
		NormalOrdersItems item = makeItem(1001L, 100L, 200L, 500L, 2, true, 0L);

		when(normalOrdersMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
		when(normalOrdersMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(item));

		service.scheduleCancelOrders();

		verify(orderCancelItemStoreRestorePort, times(1))
				.restoreStore(eq(500L), eq(100L), eq(2), eq(true), eq(0L));
	}

	// ---- §3 步骤 3-4-C-2 / C-3：拼团活动库存恢复 ----

	@Test
	void scheduleCancelOrders_item_restoresGroupItemStore() {
		NormalOrders order = makeOrder(1001L, 100L, 200L);
		NormalOrdersItems item = makeItem(1001L, 100L, 100L, 500L, 2, true, 0L);

		when(normalOrdersMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
		when(normalOrdersMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(item));

		service.scheduleCancelOrders();

		verify(partialCancelPromotionRestorePort, times(1))
				.restoreGroupItemStore(eq(100L), eq(1001L), eq(100L), eq(500L), eq(2));
	}

	// ---- §3 步骤 3-4-C：num <= 0 时跳过库存恢复 ----

	@Test
	void scheduleCancelOrders_itemNumZero_skipsStoreRestore() {
		NormalOrders order = makeOrder(1001L, 100L, 200L);
		NormalOrdersItems item = makeItem(1001L, 100L, 200L, 500L, 0, true, 0L);

		when(normalOrdersMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
		when(normalOrdersMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(item));

		service.scheduleCancelOrders();

		verify(orderCancelItemStoreRestorePort, never()).restoreStore(anyLong(), anyLong(), anyInt(), anyBoolean(), anyLong());
		verify(partialCancelPromotionRestorePort, never()).restoreGroupItemStore(anyLong(), anyLong(), anyLong(), anyLong(), anyInt());
	}

	// ---- §3 步骤 3-4-D-1：积分返还（满足条件） ----

	@Test
	@SuppressWarnings("unchecked")
	void scheduleCancelOrders_pointUsePositiveNotPointPay_returnsPoints() {
		NormalOrders order = makeOrder(1001L, 100L, 200L);
		order.setPointUse(10);
		order.setPayType("wechat");

		when(normalOrdersMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
		when(normalOrdersMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

		service.scheduleCancelOrders();

		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
		verify(pointMemberCancelOrderReturnPointsService, times(1))
				.cancelOrderReturnBackPoints(cap.capture());
		assertThat(cap.getValue()).containsEntry("point_use", 10);
	}

	// ---- §3 步骤 3-4-D-1：积分返还跳过（point_use == 0） ----

	@Test
	void scheduleCancelOrders_pointUseZero_skipsReturnPoints() {
		NormalOrders order = makeOrder(1001L, 100L, 200L);
		order.setPointUse(0);

		when(normalOrdersMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
		when(normalOrdersMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

		service.scheduleCancelOrders();

		verify(pointMemberCancelOrderReturnPointsService, never()).cancelOrderReturnBackPoints(any());
	}

	// ---- §3 步骤 3-4-D-1：积分返还跳过（pay_type == 'point'） ----

	@Test
	void scheduleCancelOrders_payTypePoint_skipsReturnPoints() {
		NormalOrders order = makeOrder(1001L, 100L, 200L);
		order.setPointUse(5);
		order.setPayType("point");

		when(normalOrdersMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
		when(normalOrdersMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

		service.scheduleCancelOrders();

		verify(pointMemberCancelOrderReturnPointsService, never()).cancelOrderReturnBackPoints(any());
	}

	// ---- §3 步骤 3-4-D-2：上分回退（满足条件） ----

	@Test
	@SuppressWarnings("unchecked")
	void scheduleCancelOrders_uppointUsePositive_minusUppoints() {
		NormalOrders order = makeOrder(1001L, 100L, 200L);
		order.setUppointUse(3);

		when(normalOrdersMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
		when(normalOrdersMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

		service.scheduleCancelOrders();

		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
		verify(pointMemberMinusOrderUppointsService, times(1)).minusOrderUppoints(cap.capture());
		assertThat(cap.getValue()).containsEntry("uppoint_use", 3);
	}

	// ---- §3 步骤 3-4-D-2：上分回退跳过（uppoint_use == 0） ----

	@Test
	void scheduleCancelOrders_uppointUseZero_skipsMinusUppoints() {
		NormalOrders order = makeOrder(1001L, 100L, 200L);
		order.setUppointUse(0);

		when(normalOrdersMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
		when(normalOrdersMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

		service.scheduleCancelOrders();

		verify(pointMemberMinusOrderUppointsService, never()).minusOrderUppoints(any());
	}

	// ---- §3 步骤 3-4-D-3：流程日志事件 ----

	@Test
	void scheduleCancelOrders_oneOrder_publishesOrderProcessLogViaDispatchPort() {
		long orderId = 1001L;
		long companyId = 100L;
		NormalOrders order = makeOrder(orderId, companyId, 200L);

		when(normalOrdersMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
		when(normalOrdersMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(order));
		when(normalOrdersItemsMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

		service.scheduleCancelOrders();

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
												&& Boolean.FALSE.equals(map.get("is_show"))
												&& Long.valueOf(0L).equals(map.get("supplier_id"))));
	}
}
