package cn.shopex.ecshopx.orders.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.port.order.OrderProcessLogPublishPort;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.domain.ServiceOrders;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.mapper.ServiceOrdersMapper;
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
 * Publish-side probe for {@link GroupsServiceOrderService#scheduleCancelOrders()} order-process-log path
 * for service-class group orders auto-cancel, aligned with queued listener payload expectations.
 */
@ExtendWith(MockitoExtension.class)
class GroupsServiceOrderServiceScheduleCancelServiceGroupsOrderProcessLogDispatchPublishProbeTest {

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

	@Test
	void scheduleCancelOrders_invokesOrderProcessLogPublishPortOnce_withCancelServiceGroupsKernelOplPayload() {
		long orderId = 100L;
		long companyId = 1L;
		ServiceOrders order = new ServiceOrders();
		order.setOrderId(orderId);
		order.setCompanyId(companyId);
		order.setOrderClass("groups");
		order.setOrderStatus("NOTPAY");
		order.setAutoCancelTime(String.valueOf(System.currentTimeMillis() / 1000L - 120));

		when(serviceOrdersMapper.selectCount(any())).thenReturn(1L);
		when(serviceOrdersMapper.selectList(any())).thenReturn(List.of(order));

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
												&& Boolean.TRUE.equals(map.get("is_show"))
												&& Long.valueOf(0L).equals(map.get("supplier_id"))));
	}
}
