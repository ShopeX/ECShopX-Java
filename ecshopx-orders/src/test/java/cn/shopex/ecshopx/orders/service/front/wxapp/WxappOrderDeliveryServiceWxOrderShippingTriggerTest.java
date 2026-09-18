package cn.shopex.ecshopx.orders.service.front.wxapp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import cn.shopex.ecshopx.orders.service.admin.AdminDeliveryLogisticsNameService;
import cn.shopex.ecshopx.orders.service.admin.AdminNormalOrderDeliveryCoreService;
import cn.shopex.ecshopx.orders.service.admin.OrderAssociationAssociationDataAssembler;
import cn.shopex.ecshopx.orders.service.admin.OrderAssociationEffectiveTypeService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WxappOrderDeliveryServiceWxOrderShippingTriggerTest {

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
	private HttpServletRequest httpServletRequest;

	private WxappOrderDeliveryService service;

	@BeforeEach
	void setUp() {
		service =
				new WxappOrderDeliveryService(
						orderAssociationsMapper,
						adminDeliveryLogisticsNameService,
						orderAssociationEffectiveTypeService,
						adminNormalOrderDeliveryCoreService,
						orderAssociationAssociationDataAssembler);
	}

	@Test
	void delivery_delegatesToAdminNormalOrderDeliveryCoreService_whenNormalPhysicalEffectiveType() {
		OrderAssociations assoc = new OrderAssociations();
		assoc.setCompanyId(100L);
		assoc.setOrderId(200L);

		when(orderAssociationsMapper.selectOne(any())).thenReturn(assoc);
		doNothing().when(adminDeliveryLogisticsNameService).fillLogiName(any());
		when(orderAssociationEffectiveTypeService.effectiveOrderType(assoc)).thenReturn("normal");
		when(orderAssociationAssociationDataAssembler.toAssociationDataMap(assoc))
				.thenReturn(Map.of());

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("order_id", 200L);
		merged.put("delivery_corp", "SF");
		merged.put("delivery_code", "SF001");

		Map<String, Object> auth = new LinkedHashMap<>();
		auth.put("company_id", 100L);
		auth.put("user_id", 55L);

		service.delivery(httpServletRequest, merged, auth);

		verify(adminNormalOrderDeliveryCoreService, times(1))
				.deliveryNormalPhysical(any(), any(), anyString());
	}
}
