package cn.shopex.ecshopx.openapi.thirdapi.v2.orders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.companys.service.setting.PickupcodeSettingRedisService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderZitiWriteoffService;
import cn.shopex.ecshopx.orders.service.normal.OrderPickupSmsRedisVerifyService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpenapiThirdApiV2OrdersOrderWriteoffDelegationTest {

	@Mock private OrderAssociationsMapper orderAssociationsMapper;
	@Mock private PickupcodeSettingRedisService pickupcodeSettingRedisService;
	@Mock private OrderPickupSmsRedisVerifyService orderPickupSmsRedisVerifyService;
	@Mock private NormalOrdersMapper normalOrdersMapper;
	@Mock private NormalOrderZitiWriteoffService normalOrderZitiWriteoffService;

	@InjectMocks private OpenapiThirdApiV2OrderWriteoffService openapiThirdApiV2OrderWriteoffService;

	@Test
	void orderWriteoff_whenValid_invokesOrderZitiWriteoffForOpenapiWithPickupArgs() {
		long companyId = 9L;
		long orderId = 501L;

		OrderAssociations assoc = new OrderAssociations();
		assoc.setCompanyId(companyId);
		assoc.setOrderId(orderId);
		assoc.setOrderType("normal");
		assoc.setOrderClass("normal");

		NormalOrders normal = new NormalOrders();
		normal.setCompanyId(companyId);
		normal.setOrderId(orderId);
		normal.setReceiptType("ziti");
		normal.setOrderStatus("PAYED");
		normal.setZitiStatus("PENDING");
		normal.setPayStatus("PAYED");
		normal.setCancelStatus("NO_APPLY_CANCEL");
		normal.setMobile("13800000000");

		when(orderAssociationsMapper.selectOne(any())).thenReturn(assoc);
		when(pickupcodeSettingRedisService.handle(eq(companyId), eq(null)))
				.thenReturn(Map.of("pickupcode_status", false));
		when(normalOrdersMapper.selectOne(any())).thenReturn(normal);

		openapiThirdApiV2OrderWriteoffService.executeOpenapiWriteoff(companyId, "501", null);

		verify(normalOrderZitiWriteoffService)
				.orderZitiWriteoffForOpenapi(eq(companyId), eq(orderId), eq(false), eq(""));
	}
}
