package cn.shopex.ecshopx.orders.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderZitiWriteoffService;
import cn.shopex.ecshopx.orders.service.normal.NormalOrdersServiceOrderDataAssembler;
import cn.shopex.ecshopx.orders.service.normal.OrderZitiQrCodeRedisService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminOrderQrWriteoffServiceWxOrderShippingDelegationTest {

	@Mock
	OrderZitiQrCodeRedisService orderZitiQrCodeRedisService;

	@Mock
	NormalOrdersMapper normalOrdersMapper;

	@Mock
	NormalOrderZitiWriteoffService normalOrderZitiWriteoffService;

	@Mock
	NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler;

	@InjectMocks
	AdminOrderQrWriteoffService adminOrderQrWriteoffService;

	@Test
	void orderWriteoffQR_whenValid_invokesOrderZitiWriteoffForAdminWithResolvedOrder() {
		long companyId = 1L;
		long operatorId = 2L;
		long orderId = 100L;
		long shopId = 10L;
		String code = "1234567890";

		List<Long> shopIds = List.of(shopId);
		List<Long> distributorIds = List.of();

		when(orderZitiQrCodeRedisService.resolveOrderIdOrThrow(code)).thenReturn(orderId);

		NormalOrders order = new NormalOrders();
		order.setCompanyId(companyId);
		order.setOrderId(orderId);
		order.setShopId(shopId);
		order.setDistributorId(0L);
		order.setZitiCode(123456L);
		order.setZitiStatus("PENDING");
		order.setOrderStatus("PAYED");
		order.setCancelStatus("NO_APPLY_CANCEL");

		when(normalOrdersMapper.selectOne(any())).thenReturn(order);
		when(normalOrdersServiceOrderDataAssembler.toServiceOrderData(any())).thenReturn(Map.of());

		Map<String, Object> result =
				adminOrderQrWriteoffService.orderWriteoffQR(
						companyId, operatorId, shopIds, distributorIds, code);

		verify(normalOrderZitiWriteoffService).orderZitiWriteoffForAdmin(companyId, orderId, operatorId);
		verifyNoMoreInteractions(normalOrderZitiWriteoffService);
		assertThat(result).isEqualTo(Map.of());
	}
}
