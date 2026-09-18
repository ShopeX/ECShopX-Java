package cn.shopex.ecshopx.orders.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.orders.domain.ServiceOrders;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.ServiceOrdersMapper;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupsServiceOrderServiceTest {

	@BeforeAll
	static void initMybatisPlusTableMetadata() {
		MybatisConfiguration cfg = new MybatisConfiguration();
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), ServiceOrders.class);
		TableInfoHelper.initTableInfo(new MapperBuilderAssistant(cfg, ""), OrderAssociations.class);
	}

	@Mock ServiceOrdersMapper serviceOrdersMapper;
	@Mock OrderAssociationsMapper orderAssociationsMapper;
	@Mock OrderProcessLogPublishPort orderProcessLogPublishPort;

	@InjectMocks
	GroupsServiceOrderService service;

	// ---- helpers ----

	private ServiceOrders makeOrder(long orderId, long companyId) {
		ServiceOrders o = new ServiceOrders();
		o.setOrderId(orderId);
		o.setCompanyId(companyId);
		o.setOrderClass("groups");
		o.setOrderStatus("NOTPAY");
		o.setAutoCancelTime(String.valueOf(System.currentTimeMillis() / 1000L - 10));
		return o;
	}

	// ---- §3 步骤 3 - totalCount == 0 早退出 ----

	@Test
	void scheduleCancelOrders_noOrders_returnsZeroAndNoUpdates() {
		when(serviceOrdersMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
		int result = service.scheduleCancelOrders();
		assertThat(result).isEqualTo(0);
		verify(serviceOrdersMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
		verify(orderAssociationsMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
	}

	// ---- §3 步骤 4-3-A：批量更新 service_orders ----

	@Test
	void scheduleCancelOrders_oneOrder_updatesServiceOrders() {
		ServiceOrders order = makeOrder(1001L, 100L);
		when(serviceOrdersMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
		when(serviceOrdersMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(order));

		service.scheduleCancelOrders();

		verify(serviceOrdersMapper, times(1)).update(eq(null), any(LambdaUpdateWrapper.class));
	}

	// ---- §3 步骤 4-3-B：批量更新 orders_associations ----

	@Test
	void scheduleCancelOrders_oneOrder_updatesOrderAssociations() {
		ServiceOrders order = makeOrder(1001L, 100L);
		when(serviceOrdersMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
		when(serviceOrdersMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(order));

		service.scheduleCancelOrders();

		verify(orderAssociationsMapper, times(1)).update(eq(null), any(LambdaUpdateWrapper.class));
	}

	// ---- §3 步骤 4-3-C-2：发布流程日志事件 ----

	@Test
	void scheduleCancelOrders_oneOrder_invokesOrderProcessLogPublishPortOnce() {
		long orderId = 1001L;
		long companyId = 100L;
		ServiceOrders order = makeOrder(orderId, companyId);
		when(serviceOrdersMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
		when(serviceOrdersMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(order));

		service.scheduleCancelOrders();

		verify(orderProcessLogPublishPort, times(1)).publish(any());
	}

	// ---- §3 步骤 4-3（orderIds 为空分支）：selectList 返回空列表时跳过更新 ----

	@Test
	void scheduleCancelOrders_emptySelectList_skipsUpdates() {
		when(serviceOrdersMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
		when(serviceOrdersMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

		int result = service.scheduleCancelOrders();

		verify(serviceOrdersMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
		verify(orderAssociationsMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
		assertThat(result).isEqualTo(0);
	}

	// ---- 返回值 = 实际取消订单数 ----

	@Test
	void scheduleCancelOrders_twoPageOneOrderEach_returnsOne() {
		ServiceOrders order = makeOrder(1001L, 100L);
		// totalCount=2 → totalPage=1（ceil(2/20)=1）；第一页返回 1 条，循环结束
		when(serviceOrdersMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
		when(serviceOrdersMapper.selectList(any(LambdaQueryWrapper.class)))
				.thenReturn(List.of(order))
				.thenReturn(List.of());

		int result = service.scheduleCancelOrders();

		// totalPage = ceil(2/20) = 1，只跑 1 轮，取消 1 笔
		assertThat(result).isEqualTo(1);
	}
}
