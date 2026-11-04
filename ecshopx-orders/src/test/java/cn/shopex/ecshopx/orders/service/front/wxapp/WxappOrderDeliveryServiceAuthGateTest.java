package cn.shopex.ecshopx.orders.service.front.wxapp;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
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
class WxappOrderDeliveryServiceAuthGateTest {

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
	private WxappFrontMerchantSelfDeliveryAccessGuard accessGuard;

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
						orderAssociationAssociationDataAssembler,
						accessGuard);
	}

	@Test
	void delivery_whenAccessGuardRejects_doesNotInvokeCore() {
		OrderAssociations assoc = new OrderAssociations();
		assoc.setCompanyId(100L);
		assoc.setOrderId(200L);
		when(orderAssociationsMapper.selectOne(any())).thenReturn(assoc);
		doThrow(new ForbiddenException("无权操作该单据"))
				.when(accessGuard)
				.assertAuthorizedForOrder(100L, 55L, 200L);

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("order_id", 200L);
		Map<String, Object> auth = Map.of("company_id", 100L, "user_id", 55L);

		assertThatThrownBy(() -> service.delivery(httpServletRequest, merged, auth))
				.isInstanceOf(ForbiddenException.class);

		verify(adminNormalOrderDeliveryCoreService, never())
				.deliveryNormalPhysical(any(), any(), anyString());
		verify(orderAssociationEffectiveTypeService, never()).effectiveOrderType(any());
	}

	@Test
	void delivery_whenAuthorized_invokesCoreOnce() {
		OrderAssociations assoc = new OrderAssociations();
		assoc.setCompanyId(100L);
		assoc.setOrderId(200L);
		when(orderAssociationsMapper.selectOne(any())).thenReturn(assoc);
		when(orderAssociationEffectiveTypeService.effectiveOrderType(assoc)).thenReturn("normal");
		when(orderAssociationAssociationDataAssembler.toAssociationDataMap(assoc)).thenReturn(Map.of());

		Map<String, Object> merged = new LinkedHashMap<>();
		merged.put("order_id", 200L);
		Map<String, Object> auth = Map.of("company_id", 100L, "user_id", 55L);

		service.delivery(httpServletRequest, merged, auth);

		verify(accessGuard).assertAuthorizedForOrder(100L, 55L, 200L);
		verify(adminNormalOrderDeliveryCoreService)
				.deliveryNormalPhysical(any(), any(), anyString());
	}
}
