package cn.shopex.ecshopx.orders.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.port.order.OrderCancelSeckillTicketPort;
import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SeckillNormalOrderServiceTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), NormalOrdersItems.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OrderAssociations.class);
	}

	@Mock
	NormalOrdersMapper normalOrdersMapper;
	@Mock
	OrderAssociationsMapper orderAssociationsMapper;
	@Mock
	NormalOrdersItemsMapper normalOrdersItemsMapper;
	@Mock
	OrderCancelSeckillTicketPort orderCancelSeckillTicketPort;
	@Mock
	PointMemberCancelOrderReturnPointsService pointMemberCancelOrderReturnPointsService;
	@Mock
	PointMemberMinusOrderUppointsService pointMemberMinusOrderUppointsService;
	@Mock
	OrderProcessLogPublishPort orderProcessLogPublishPort;

	@InjectMocks
	SeckillNormalOrderService service;

	// §3 步骤 1、2：查询条件含秒杀实体普通单过滤
	@Test
	@SuppressWarnings("unchecked")
	void scheduleCancelOrders_queryFilter_seckillNormalAndAutoCancel() {
		when(normalOrdersMapper.selectCount(any())).thenReturn(0L);
		service.scheduleCancelOrders();
		ArgumentCaptor<LambdaQueryWrapper<NormalOrders>> cap = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(normalOrdersMapper).selectCount(cap.capture());
		String seg = cap.getValue().getSqlSegment();
		assertThat(seg).contains("order_status");
		assertThat(seg).contains("order_class");
		assertThat(seg).contains("order_type");
		assertThat(seg).contains("auto_cancel_time");
		assertThat(seg).contains("UNSIGNED");
		verify(normalOrdersMapper, never()).selectList(any());
	}

	// §3 步骤 3：totalCount=0 早退出
	@Test
	void scheduleCancelOrders_totalZero_returnsZero() {
		when(normalOrdersMapper.selectCount(any())).thenReturn(0L);
		int result = service.scheduleCancelOrders();
		assertThat(result).isEqualTo(0);
		verify(normalOrdersMapper, never()).selectList(any());
		verify(normalOrdersMapper, never()).update(any(), any());
	}

	// §3 步骤 4、4-1、4-2、5：分页 LIMIT 20
	@Test
	void scheduleCancelOrders_pagination_usesLimit20() {
		NormalOrders o = makeSeckillOrder(1L, 1L, 10L, "wechat", 0, 0);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(o));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		service.scheduleCancelOrders();
		ArgumentCaptor<LambdaQueryWrapper<NormalOrders>> cap = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(normalOrdersMapper, times(1)).selectList(cap.capture());
		assertThat(cap.getValue().getSqlSegment()).contains("LIMIT 20");
	}

	// §3 步骤 4、5：多页累计取消条数
	@Test
	void scheduleCancelOrders_multiPage_sumsCancelledCount() {
		List<NormalOrders> page1 = IntStream.rangeClosed(1, 20)
				.mapToObj(i -> makeSeckillOrder(100L + i, 1L, 10L, "wechat", 0, 0))
				.collect(Collectors.toList());
		List<NormalOrders> page2 = List.of(makeSeckillOrder(200L, 1L, 10L, "wechat", 0, 0));
		when(normalOrdersMapper.selectCount(any())).thenReturn(21L);
		when(normalOrdersMapper.selectList(any())).thenReturn(page1, page2);
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		int result = service.scheduleCancelOrders();
		assertThat(result).isEqualTo(21);
		verify(normalOrdersMapper, times(2)).selectList(any());
		verify(normalOrdersMapper, times(2)).update(any(), any(LambdaUpdateWrapper.class));
	}

	// §3 步骤 4-3：页内无单
	@Test
	void scheduleCancelOrders_pageEmpty_skipsUpdates() {
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of());
		int result = service.scheduleCancelOrders();
		assertThat(result).isEqualTo(0);
		verify(normalOrdersMapper, never()).update(any(), any());
		verify(orderAssociationsMapper, never()).update(any(), any());
		verify(orderCancelSeckillTicketPort, never()).restoreUserBuysStore(any(Long.class), any(Long.class),
				any(Long.class), any(Long.class), any(Integer.class));
		verify(orderProcessLogPublishPort, never()).publish(any());
	}

	// §3 步骤 4-3-a、4-3-b
	@Test
	void scheduleCancelOrders_mainFlow_batchCancels() {
		NormalOrders o1 = makeSeckillOrder(101L, 1L, 10L, "wechat", 0, 0);
		NormalOrders o2 = makeSeckillOrder(102L, 1L, 11L, "wechat", 0, 0);
		when(normalOrdersMapper.selectCount(any())).thenReturn(2L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(o1, o2));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		int result = service.scheduleCancelOrders();
		assertThat(result).isEqualTo(2);
		verify(normalOrdersMapper, times(1)).update(any(), any(LambdaUpdateWrapper.class));
		verify(orderAssociationsMapper, times(1)).update(any(), any(LambdaUpdateWrapper.class));
	}

	// §3 步骤 4-3-c
	@Test
	void scheduleCancelOrders_loadsOrderItems() {
		NormalOrders o = makeSeckillOrder(100L, 1L, 10L, "wechat", 0, 0);
		NormalOrdersItems item = makeSeckillItem(100L, 1L, 10L, 200L, 77L, 2);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(o));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(item));
		service.scheduleCancelOrders();
		verify(normalOrdersItemsMapper, times(1)).selectList(any());
	}

	// §3 步骤 4-3-d
	@Test
	void scheduleCancelOrders_seckillRestore_called() {
		NormalOrders o = makeSeckillOrder(100L, 1L, 10L, "wechat", 0, 0);
		NormalOrdersItems item = makeSeckillItem(100L, 1L, 10L, 200L, 300L, 2);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(o));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(item));
		service.scheduleCancelOrders();
		verify(orderCancelSeckillTicketPort, times(1))
				.restoreUserBuysStore(eq(1L), eq(10L), eq(300L), eq(200L), eq(2));
	}

	// §3 步骤 4-3-d actId/itemId 缺失
	@Test
	void scheduleCancelOrders_seckillRestore_skipsWhenActOrItemNull() {
		NormalOrders o = makeSeckillOrder(100L, 1L, 10L, "wechat", 0, 0);
		NormalOrdersItems noAct = makeSeckillItem(100L, 1L, 10L, 200L, null, 2);
		NormalOrdersItems noItem = makeSeckillItem(100L, 1L, 10L, null, 300L, 2);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(o));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(noAct, noItem));
		service.scheduleCancelOrders();
		verify(orderCancelSeckillTicketPort, never()).restoreUserBuysStore(any(Long.class), any(Long.class),
				any(Long.class), any(Long.class), any(Integer.class));
	}

	// §3 步骤 4-3-e 积分回退路径（调用方始终调 port，内部按 point/payType 判断）
	@Test
	void scheduleCancelOrders_pointReturn_invokesCancelPointsWithHighPointUse() {
		NormalOrders o = makeSeckillOrder(100L, 1L, 10L, "wechat", 100, 0);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(o));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		service.scheduleCancelOrders();
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
		verify(pointMemberCancelOrderReturnPointsService, times(1)).cancelOrderReturnBackPoints(cap.capture());
		assertThat(cap.getValue().get("point_use")).isEqualTo(100);
		assertThat(cap.getValue().get("pay_type")).isEqualTo("wechat");
	}

	// §3 步骤 4-3-e payType=point 仍调用，由 Point 服务内部不加分
	@Test
	void scheduleCancelOrders_pointPayType_stillInvokesCancelPointsService() {
		NormalOrders o = makeSeckillOrder(100L, 1L, 10L, "point", 100, 0);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(o));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		service.scheduleCancelOrders();
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
		verify(pointMemberCancelOrderReturnPointsService, times(1)).cancelOrderReturnBackPoints(cap.capture());
		assertThat(cap.getValue().get("pay_type")).isEqualTo("point");
	}

	// §3 步骤 4-3-e pointUse=0
	@Test
	void scheduleCancelOrders_zeroPointUse_stillInvokesCancelPointsService() {
		NormalOrders o = makeSeckillOrder(100L, 1L, 10L, "wechat", 0, 0);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(o));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		service.scheduleCancelOrders();
		ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
		verify(pointMemberCancelOrderReturnPointsService, times(1)).cancelOrderReturnBackPoints(cap.capture());
		assertThat(cap.getValue().get("point_use")).isEqualTo(0);
	}

	// §3 步骤 4-3-e 上分回退
	@Test
	void scheduleCancelOrders_uppointReturn_called() {
		NormalOrders o = makeSeckillOrder(100L, 1L, 10L, "wechat", 0, 50);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(o));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		service.scheduleCancelOrders();
		verify(pointMemberMinusOrderUppointsService, times(1)).minusOrderUppoints(any(Map.class));
	}

	// §3 步骤 4-3-e 上分跳过
	@Test
	void scheduleCancelOrders_zeroUppoint_skips() {
		NormalOrders o = makeSeckillOrder(100L, 1L, 10L, "wechat", 0, 0);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(o));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		service.scheduleCancelOrders();
		verify(pointMemberMinusOrderUppointsService, never()).minusOrderUppoints(any());
	}

	// §3 步骤 4-3-e 流程日志
	@Test
	void scheduleCancelOrders_publishesOrderProcessLogOnce() {
		NormalOrders o = makeSeckillOrder(100L, 1L, 10L, "wechat", 0, 0);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(o));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		service.scheduleCancelOrders();
		verify(orderProcessLogPublishPort, times(1))
				.publish(
						argThat(
								map ->
										"订单取消".equals(map.get("remarks"))
												&& ("订单单号：100，取消订单退款").equals(map.get("detail"))
												&& "system".equals(map.get("operator_type"))
												&& Long.valueOf(0L).equals(map.get("operator_id"))
												&& 100L == ((Number) map.get("order_id")).longValue()
												&& 1L == ((Number) map.get("company_id")).longValue()
												&& Boolean.FALSE.equals(map.get("is_show"))
												&& Long.valueOf(0L).equals(map.get("supplier_id"))));
	}

	// §3 步骤 6：有 totalCount 但每页空，返回 0
	@Test
	void scheduleCancelOrders_countPositiveButAllPagesEmpty_returnsZero() {
		when(normalOrdersMapper.selectCount(any())).thenReturn(3L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of());
		int result = service.scheduleCancelOrders();
		assertThat(result).isEqualTo(0);
	}

	// §3 步骤 5：单页多条仍正确累计
	@Test
	void scheduleCancelOrders_singlePage_accumulatesSize() {
		List<NormalOrders> batch = new ArrayList<>();
		batch.add(makeSeckillOrder(1L, 1L, 10L, "wechat", 0, 0));
		batch.add(makeSeckillOrder(2L, 1L, 10L, "wechat", 0, 0));
		when(normalOrdersMapper.selectCount(any())).thenReturn(2L);
		when(normalOrdersMapper.selectList(any())).thenReturn(batch);
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		assertThat(service.scheduleCancelOrders()).isEqualTo(2);
	}

	private NormalOrders makeSeckillOrder(long orderId, long companyId, long userId,
			String payType, int pointUse, int uppointUse) {
		NormalOrders o = new NormalOrders();
		o.setOrderId(orderId);
		o.setCompanyId(companyId);
		o.setUserId(userId);
		o.setOrderClass("seckill");
		o.setOrderType("normal");
		o.setPayType(payType);
		o.setOrderStatus("NOTPAY");
		o.setPointUse(pointUse);
		o.setUppointUse(uppointUse);
		o.setAutoCancelTime(String.valueOf(System.currentTimeMillis() / 1000L - 120));
		return o;
	}

	private NormalOrdersItems makeSeckillItem(long orderId, long companyId, long userId, Long itemId, Long actId,
			int num) {
		NormalOrdersItems i = new NormalOrdersItems();
		i.setOrderId(orderId);
		i.setCompanyId(companyId);
		i.setUserId(userId);
		i.setItemId(itemId);
		i.setActId(actId);
		i.setNum(num);
		return i;
	}
}
