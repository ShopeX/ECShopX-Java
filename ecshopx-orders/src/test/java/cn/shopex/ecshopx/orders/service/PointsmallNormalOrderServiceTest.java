package cn.shopex.ecshopx.orders.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.common.port.order.PointsmallPartialCancelItemStorePort;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.NormalOrdersItems;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersItemsMapper;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.point.service.PointMemberAddPointService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PointsmallNormalOrderServiceTest {

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
	PointsmallPartialCancelItemStorePort pointsmallPartialCancelItemStorePort;
	@Mock
	PointMemberAddPointService pointMemberAddPointService;
	@Mock
	OrderProcessLogPublishPort orderProcessLogPublishPort;

	@InjectMocks
	PointsmallNormalOrderService service;

	@Test
	@DisplayName("plan§5 §3 1、2 阈值与 filter")
	@SuppressWarnings("unchecked")
	void scheduleCancelOrders_queryFilter_pointsmallNoOrderType() {
		when(normalOrdersMapper.selectCount(any())).thenReturn(0L);
		service.scheduleCancelOrders();
		ArgumentCaptor<LambdaQueryWrapper<NormalOrders>> cap = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(normalOrdersMapper).selectCount(cap.capture());
		String seg = cap.getValue().getSqlSegment();
		assertThat(seg).contains("order_status");
		assertThat(seg).contains("order_class");
		assertThat(seg).contains("auto_cancel_time");
		assertThat(seg).contains("UNSIGNED");
		assertThat(seg).doesNotContain("order_type");
		verify(normalOrdersMapper, never()).selectList(any());
	}

	@Test
	@DisplayName("plan§5 §3 3 早退出")
	void scheduleCancelOrders_totalZero_returnsZero() {
		when(normalOrdersMapper.selectCount(any())).thenReturn(0L);
		int result = service.scheduleCancelOrders();
		assertThat(result).isEqualTo(0);
		verify(normalOrdersMapper, never()).selectList(any());
		verify(normalOrdersMapper, never()).update(any(), any());
		verify(pointMemberAddPointService, never()).addPointForOrderCancelReturn(
				anyLong(), anyLong(), anyInt(), anyLong());
	}

	@Test
	@DisplayName("plan§5 §3 4、6、6.1、6.2 分页与循环")
	void scheduleCancelOrders_pagination_usesLimit20() {
		NormalOrders o = makePointsmallOrder(1L, 1L, 10L, 5);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(o));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		service.scheduleCancelOrders();
		ArgumentCaptor<LambdaQueryWrapper<NormalOrders>> cap = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
		verify(normalOrdersMapper, times(1)).selectList(cap.capture());
		assertThat(cap.getValue().getSqlSegment()).contains("LIMIT 20");
	}

	@Test
	@DisplayName("plan§5 §3 6、7 累计返回（多页）")
	void scheduleCancelOrders_multiPage_sumsCancelledCount() {
		List<NormalOrders> page1 = IntStream.rangeClosed(1, 20)
				.mapToObj(i -> makePointsmallOrder(100L + i, 1L, 10L, 0))
				.collect(Collectors.toList());
		List<NormalOrders> page2 = List.of(makePointsmallOrder(200L, 1L, 10L, 0));
		when(normalOrdersMapper.selectCount(any())).thenReturn(21L);
		when(normalOrdersMapper.selectList(any())).thenReturn(page1, page2);
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		int result = service.scheduleCancelOrders();
		assertThat(result).isEqualTo(21);
		verify(normalOrdersMapper, times(2)).selectList(any());
		verify(normalOrdersMapper, times(2)).update(any(), any());
	}

	@Test
	@DisplayName("plan§5 §3 5、6.4.5 Port 注入与库存回补（多行）")
	void scheduleCancelOrders_portReused_forMultipleItemRows() {
		NormalOrders o = makePointsmallOrder(100L, 1L, 10L, 0);
		NormalOrdersItems i1 = makeItem(100L, 1L, 10L, 1L, 1);
		NormalOrdersItems i2 = makeItem(100L, 1L, 10L, 2L, 2);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(o));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(i1, i2));
		service.scheduleCancelOrders();
		verify(pointsmallPartialCancelItemStorePort, times(1)).minusItemStore(1L, 1L, -1, true);
		verify(pointsmallPartialCancelItemStorePort, times(1)).minusItemStore(1L, 2L, -2, true);
	}

	@Test
	@DisplayName("plan§5 §3 6.3 页内无单")
	void scheduleCancelOrders_pageEmpty_skipsUpdates() {
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of());
		int result = service.scheduleCancelOrders();
		assertThat(result).isEqualTo(0);
		verify(normalOrdersMapper, never()).update(any(), any());
		verify(orderAssociationsMapper, never()).update(any(), any());
		verify(pointsmallPartialCancelItemStorePort, never()).minusItemStore(
				anyLong(), anyLong(), anyInt(), anyBoolean());
		verify(orderProcessLogPublishPort, never()).publish(any());
	}

	@Test
	@DisplayName("plan§5 §3 6.4.1、6.4.2 主链批量取消")
	void scheduleCancelOrders_mainFlow_batchCancels() {
		NormalOrders o1 = makePointsmallOrder(101L, 1L, 10L, 0);
		NormalOrders o2 = makePointsmallOrder(102L, 1L, 11L, 0);
		when(normalOrdersMapper.selectCount(any())).thenReturn(2L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(o1, o2));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		int result = service.scheduleCancelOrders();
		assertThat(result).isEqualTo(2);
		verify(normalOrdersMapper, times(1)).update(any(), any(LambdaUpdateWrapper.class));
		verify(orderAssociationsMapper, times(1)).update(any(), any(LambdaUpdateWrapper.class));
	}

	@Test
	@DisplayName("plan§5 §3 6.4.3 明细加载")
	void scheduleCancelOrders_loadsOrderItems() {
		NormalOrders o = makePointsmallOrder(100L, 1L, 10L, 0);
		NormalOrdersItems item = makeItem(100L, 1L, 10L, 200L, 2);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(o));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(item));
		service.scheduleCancelOrders();
		verify(normalOrdersItemsMapper, times(1)).selectList(any());
	}

	@Test
	@DisplayName("plan§5 §3 6.4.4 积分返还 point>0")
	void scheduleCancelOrders_addPointForOrderCancelReturn_withPositivePoint() {
		NormalOrders o = makePointsmallOrder(100L, 1L, 10L, 50);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(o));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		service.scheduleCancelOrders();
		verify(pointMemberAddPointService, times(1)).addPointForOrderCancelReturn(10L, 1L, 50, 100L);
	}

	@Test
	@DisplayName("plan§5 §3 6.4.4 积分返还 point=0")
	void scheduleCancelOrders_addPointForOrderCancelReturn_withZeroPoint() {
		NormalOrders o = makePointsmallOrder(100L, 1L, 10L, 0);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(o));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		service.scheduleCancelOrders();
		verify(pointMemberAddPointService, times(1)).addPointForOrderCancelReturn(10L, 1L, 0, 100L);
	}

	@Test
	@DisplayName("plan§5 §3 6.4.4 积分返还 pay_type=point")
	void scheduleCancelOrders_addPointForOrderCancelReturn_pointPayType() {
		NormalOrders o = makePointsmallOrder(100L, 1L, 10L, 30);
		o.setPayType("point");
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(o));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		service.scheduleCancelOrders();
		verify(pointMemberAddPointService, times(1)).addPointForOrderCancelReturn(10L, 1L, 30, 100L);
	}

	@Test
	@DisplayName("plan§5 §3 6.4.4 流程日志")
	void scheduleCancelOrders_publishesOrderProcessLogPerOrder() {
		long orderId = 100L;
		long companyId = 1L;
		NormalOrders o = makePointsmallOrder(orderId, companyId, 10L, 0);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(o));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
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

	@Test
	@DisplayName("plan§5 §3 6.4.5 库存回补有效 itemId")
	void scheduleCancelOrders_itemStore_restockCallsMinusWithNegativeNum() {
		NormalOrders o = makePointsmallOrder(100L, 1L, 10L, 0);
		NormalOrdersItems item = makeItem(100L, 1L, 10L, 77L, 2);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(o));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(item));
		service.scheduleCancelOrders();
		verify(pointsmallPartialCancelItemStorePort, times(1)).minusItemStore(1L, 77L, -2, true);
	}

	@Test
	@DisplayName("plan§5 §3 6.4.5 无效 itemId 跳过")
	void scheduleCancelOrders_itemStore_skipsInvalidItemId() {
		NormalOrders o = makePointsmallOrder(100L, 1L, 10L, 0);
		NormalOrdersItems nullId = makeItem(100L, 1L, 10L, null, 1);
		NormalOrdersItems zeroId = makeItem(100L, 1L, 10L, 0L, 1);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(o));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(nullId, zeroId));
		service.scheduleCancelOrders();
		verify(pointsmallPartialCancelItemStorePort, never()).minusItemStore(
				anyLong(), anyLong(), anyInt(), anyBoolean());
	}

	@Test
	@DisplayName("plan§5 §3 6.4.5 Port 返回 false 不中断")
	void scheduleCancelOrders_itemStore_falseDoesNotStopBatch() {
		NormalOrders o = makePointsmallOrder(100L, 1L, 10L, 0);
		NormalOrdersItems item = makeItem(100L, 1L, 10L, 1L, 1);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(o));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of(item));
		when(pointsmallPartialCancelItemStorePort.minusItemStore(1L, 1L, -1, true))
				.thenReturn(false);
		int r = service.scheduleCancelOrders();
		assertThat(r).isEqualTo(1);
		verify(pointMemberAddPointService, times(1)).addPointForOrderCancelReturn(
				anyLong(), anyLong(), anyInt(), anyLong());
	}

	@Test
	@DisplayName("plan§5 §3 6.4.5 空明细不调 port")
	void scheduleCancelOrders_emptyOrderItems_noItemStore() {
		NormalOrders o = makePointsmallOrder(100L, 1L, 10L, 0);
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(o));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		service.scheduleCancelOrders();
		verify(pointsmallPartialCancelItemStorePort, never()).minusItemStore(
				anyLong(), anyLong(), anyInt(), anyBoolean());
	}

	@Test
	@DisplayName("plan§5 §3 6.3 计数>0 但各页无单")
	void scheduleCancelOrders_countPositiveButAllPagesEmpty_returnsZero() {
		when(normalOrdersMapper.selectCount(any())).thenReturn(3L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of());
		int result = service.scheduleCancelOrders();
		assertThat(result).isEqualTo(0);
	}

	@Test
	@DisplayName("plan§5 §3 6.4.4 user/company 默认 0")
	void scheduleCancelOrders_nullUserCompany_defaultsToZero() {
		NormalOrders o = new NormalOrders();
		o.setOrderId(1L);
		o.setOrderClass("pointsmall");
		o.setOrderStatus("NOTPAY");
		o.setPoint(1);
		o.setAutoCancelTime(String.valueOf(System.currentTimeMillis() / 1000L - 120));
		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(o));
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		service.scheduleCancelOrders();
		verify(pointMemberAddPointService, times(1)).addPointForOrderCancelReturn(0L, 0L, 1, 1L);
	}

	@Test
	@DisplayName("plan§5 §3 6、7 累计返回（单页多笔）")
	void scheduleCancelOrders_singlePage_accumulatesSize() {
		List<NormalOrders> batch = new ArrayList<>();
		batch.add(makePointsmallOrder(1L, 1L, 10L, 0));
		batch.add(makePointsmallOrder(2L, 1L, 10L, 0));
		when(normalOrdersMapper.selectCount(any())).thenReturn(2L);
		when(normalOrdersMapper.selectList(any())).thenReturn(batch);
		when(normalOrdersItemsMapper.selectList(any())).thenReturn(List.of());
		assertThat(service.scheduleCancelOrders()).isEqualTo(2);
	}

	private NormalOrders makePointsmallOrder(long orderId, long companyId, long userId, int point) {
		NormalOrders o = new NormalOrders();
		o.setOrderId(orderId);
		o.setCompanyId(companyId);
		o.setUserId(userId);
		o.setOrderClass("pointsmall");
		o.setOrderStatus("NOTPAY");
		o.setPoint(point);
		o.setAutoCancelTime(String.valueOf(System.currentTimeMillis() / 1000L - 120));
		return o;
	}

	private NormalOrdersItems makeItem(long orderId, long companyId, long userId, Long itemId, int num) {
		NormalOrdersItems i = new NormalOrdersItems();
		i.setOrderId(orderId);
		i.setCompanyId(companyId);
		i.setUserId(userId);
		i.setItemId(itemId);
		i.setNum(num);
		return i;
	}
}
