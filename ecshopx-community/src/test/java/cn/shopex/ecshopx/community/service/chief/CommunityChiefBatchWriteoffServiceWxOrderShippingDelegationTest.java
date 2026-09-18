package cn.shopex.ecshopx.community.service.chief;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.community.service.CommunityActivityService;
import cn.shopex.ecshopx.orders.domain.NormalOrders;
import cn.shopex.ecshopx.orders.service.normal.CommunityChiefBatchWriteoffOrderListService;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderZitiWriteoffService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommunityChiefBatchWriteoffServiceWxOrderShippingDelegationTest {

	@Mock
	private CommunityActivityService communityActivityService;
	@Mock
	private CommunityChiefBatchWriteoffOrderListService communityChiefBatchWriteoffOrderListService;
	@Mock
	private NormalOrderZitiWriteoffService normalOrderZitiWriteoffService;

	@InjectMocks
	private CommunityChiefBatchWriteoffService communityChiefBatchWriteoffService;

	@Test
	void executeBatchWriteoff_whenMultiplePending_orders_invokesOrderZitiWriteoffForChiefPerOrder() {
		long companyId = 100L;
		long chiefId = 200L;
		long activityId = 400L;

		NormalOrders o1 = new NormalOrders();
		o1.setOrderId(301L);
		NormalOrders o2 = new NormalOrders();
		o2.setOrderId(302L);
		NormalOrders o3 = new NormalOrders();
		o3.setOrderId(303L);

		when(communityChiefBatchWriteoffOrderListService.listPendingCommunityOrdersForChiefBatchWriteoff(
						companyId, activityId))
				.thenReturn(List.of(o1, o2, o3));

		communityChiefBatchWriteoffService.executeBatchWriteoff(companyId, chiefId, activityId);

		verify(normalOrderZitiWriteoffService).orderZitiWriteoffForChief(companyId, 301L, chiefId);
		verify(normalOrderZitiWriteoffService).orderZitiWriteoffForChief(companyId, 302L, chiefId);
		verify(normalOrderZitiWriteoffService).orderZitiWriteoffForChief(companyId, 303L, chiefId);
		verify(normalOrderZitiWriteoffService, times(3)).orderZitiWriteoffForChief(eq(companyId), anyLong(), eq(chiefId));
		verifyNoMoreInteractions(normalOrderZitiWriteoffService);
	}
}
