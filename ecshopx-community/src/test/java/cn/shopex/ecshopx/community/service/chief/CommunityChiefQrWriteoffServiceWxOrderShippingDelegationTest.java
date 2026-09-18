package cn.shopex.ecshopx.community.service.chief;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.community.domain.CommunityOrderRelActivity;
import cn.shopex.ecshopx.community.mapper.CommunityOrderRelActivityMapper;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderZitiWriteoffService;
import cn.shopex.ecshopx.orders.service.normal.NormalOrdersServiceOrderDataAssembler;
import cn.shopex.ecshopx.orders.service.normal.OrderZitiQrCodeRedisService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommunityChiefQrWriteoffServiceWxOrderShippingDelegationTest {

	@Mock
	private OrderZitiQrCodeRedisService orderZitiQrCodeRedisService;
	@Mock
	private NormalOrdersMapper normalOrdersMapper;
	@Mock
	private CommunityOrderRelActivityMapper communityOrderRelActivityMapper;
	@Mock
	private NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler;
	@Mock
	private NormalOrderZitiWriteoffService normalOrderZitiWriteoffService;

	@InjectMocks
	private CommunityChiefQrWriteoffService communityChiefQrWriteoffService;

	@Test
	void executeQrWriteoff_whenValid_invokesOrderZitiWriteoffForChiefWithResolvedOrder() {
		long companyId = 100L;
		long chiefId = 200L;
		long orderId = 300L;
		String code = "123456789";

		when(orderZitiQrCodeRedisService.resolveOrderIdOrThrow(code)).thenReturn(orderId);

		NormalOrders order = new NormalOrders();
		order.setCompanyId(companyId);
		order.setOrderId(orderId);
		order.setZitiCode(123456L);
		order.setZitiStatus("PENDING");
		order.setOrderStatus("PAYED");
		order.setCancelStatus("NO_APPLY_CANCEL");

		when(normalOrdersMapper.selectOne(any())).thenReturn(order);

		CommunityOrderRelActivity rel = new CommunityOrderRelActivity();
		rel.setCompanyId(companyId);
		rel.setOrderId(orderId);
		rel.setChiefId(chiefId);
		when(communityOrderRelActivityMapper.selectOne(any())).thenReturn(rel);

		when(normalOrdersServiceOrderDataAssembler.toServiceOrderData(any())).thenReturn(Map.of());

		communityChiefQrWriteoffService.executeQrWriteoff(companyId, chiefId, code);

		verify(normalOrderZitiWriteoffService).orderZitiWriteoffForChief(companyId, orderId, chiefId);
		verifyNoMoreInteractions(normalOrderZitiWriteoffService);
	}
}
