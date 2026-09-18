package cn.shopex.ecshopx.community.service.chief;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderZitiWriteoffService;
import cn.shopex.ecshopx.orders.service.normal.NormalOrdersServiceOrderDataAssembler;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommunityBuyerWriteoffServiceWxOrderShippingDelegationTest {

	@Mock
	private NormalOrdersMapper normalOrdersMapper;
	@Mock
	private NormalOrderZitiWriteoffService normalOrderZitiWriteoffService;
	@Mock
	private NormalOrdersServiceOrderDataAssembler normalOrdersServiceOrderDataAssembler;

	@InjectMocks
	private CommunityBuyerWriteoffService communityBuyerWriteoffService;

	@Test
	void executeBuyerWriteoff_whenValid_invokesOrderZitiWriteoffForBuyerOperator() {
		long companyId = 100L;
		long memberUserId = 200L;
		long orderId = 300L;

		NormalOrders order = new NormalOrders();
		order.setCompanyId(companyId);
		order.setOrderId(orderId);
		order.setUserId(memberUserId);
		order.setZitiStatus("PENDING");
		order.setOrderStatus("PAYED");
		order.setCancelStatus("NO_APPLY_CANCEL");

		NormalOrders fresh = new NormalOrders();
		fresh.setCompanyId(companyId);
		fresh.setOrderId(orderId);

		when(normalOrdersMapper.selectOne(any())).thenReturn(order, fresh);
		when(normalOrdersServiceOrderDataAssembler.toServiceOrderData(any())).thenReturn(Map.of());

		communityBuyerWriteoffService.executeBuyerWriteoff(companyId, memberUserId, orderId);

		verify(normalOrderZitiWriteoffService).orderZitiWriteoffForBuyerOperator(companyId, orderId, memberUserId);
		verifyNoMoreInteractions(normalOrderZitiWriteoffService);
	}
}
