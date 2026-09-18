package cn.shopex.ecshopx.orders.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Publish-side probe for {@link SeckillNormalOrderService#scheduleCancelOrders()} order-process-log path
 * (seckill schedule cancel), aligned with async listener payload expectations.
 */
@ExtendWith(MockitoExtension.class)
class SeckillNormalOrderServiceScheduleCancelOplDispatchPublishProbeTest {

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

	@Test
	void scheduleCancelOrders_invokesOrderProcessLogPublishPortOnce_withSeckillScheduleCancelOplPayload() {
		long orderId = 100L;
		long companyId = 1L;
		NormalOrders order = new NormalOrders();
		order.setOrderId(orderId);
		order.setCompanyId(companyId);
		order.setUserId(10L);
		order.setOrderClass("seckill");
		order.setOrderType("normal");
		order.setPayType("wechat");
		order.setOrderStatus("NOTPAY");
		order.setPointUse(0);
		order.setUppointUse(0);
		order.setAutoCancelTime(String.valueOf(System.currentTimeMillis() / 1000L - 120));

		when(normalOrdersMapper.selectCount(any())).thenReturn(1L);
		when(normalOrdersMapper.selectList(any())).thenReturn(List.of(order));
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
}
