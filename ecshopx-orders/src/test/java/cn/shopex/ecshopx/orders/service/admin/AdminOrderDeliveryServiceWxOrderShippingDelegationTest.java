package cn.shopex.ecshopx.orders.service.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.orders.domain.OrderAssociations;
import cn.shopex.ecshopx.orders.mapper.OrderAssociationsMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Ensures admin-side order delivery reaches {@link AdminNormalOrderDeliveryCoreService#deliveryNormalPhysical}
 * for normal physical orders (Wx shipping dispatch scheduled after commit).
 */
@ExtendWith(MockitoExtension.class)
class AdminOrderDeliveryServiceWxOrderShippingDelegationTest {

	@Mock
	OrderAssociationsMapper orderAssociationsMapper;

	@Mock
	AdminSupplierDeliveryParamsAdjustService adminSupplierDeliveryParamsAdjustService;

	@Mock
	AdminDeliveryLogisticsNameService adminDeliveryLogisticsNameService;

	@Mock
	OrderAssociationEffectiveTypeService orderAssociationEffectiveTypeService;

	@Mock
	AdminNormalOrderDeliveryCoreService adminNormalOrderDeliveryCoreService;

	@Mock
	OrderAssociationAssociationDataAssembler orderAssociationAssociationDataAssembler;

	@InjectMocks
	AdminOrderDeliveryService adminOrderDeliveryService;

	@Test
	void delivery_whenNormalPhysicalOrder_delegatesToDeliveryNormalPhysical() {
		long companyId = 1L;
		long orderId = 99L;
		OrderAssociations assoc = new OrderAssociations();
		assoc.setCompanyId(companyId);
		assoc.setOrderId(orderId);

		when(orderAssociationsMapper.selectOne(any())).thenReturn(assoc).thenReturn(assoc);
		when(orderAssociationEffectiveTypeService.effectiveOrderType(assoc)).thenReturn("normal");
		when(orderAssociationAssociationDataAssembler.toAssociationDataMap(any())).thenReturn(Map.of());

		Map<String, Object> params = new LinkedHashMap<>();
		params.put("order_id", orderId);

		adminOrderDeliveryService.delivery(companyId, "admin", 1L, params);

		verify(adminNormalOrderDeliveryCoreService, times(1))
				.deliveryNormalPhysical(anyMap(), eq(assoc), eq("normal"));
	}
}
