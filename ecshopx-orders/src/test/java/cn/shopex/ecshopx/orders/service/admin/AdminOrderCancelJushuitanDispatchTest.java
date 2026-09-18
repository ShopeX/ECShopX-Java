package cn.shopex.ecshopx.orders.service.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
 * Locks admin cancel routing: {@code PENDING} → {@link AdminNormalOrderFullCancelService} with {@code cancelFrom}
 * {@code "shop"} (shared with wxapp; Jushuitan publish lives there). Other delivery states → partial cancel only.
 */
@ExtendWith(MockitoExtension.class)
class AdminOrderCancelJushuitanDispatchTest {

	@Mock
	OrderAssociationsMapper orderAssociationsMapper;

	@Mock
	OrderCancelReasonConfig orderCancelReasonConfig;

	@Mock
	AdminNormalOrderFullCancelService adminNormalOrderFullCancelService;

	@Mock
	AdminNormalOrderPartialCancelService adminNormalOrderPartialCancelService;

	@InjectMocks
	AdminOrderCancelService adminOrderCancelService;

	@Test
	void cancelOrder_whenDeliveryPending_andNormalOrder_delegatesToFullCancelWithShopCancelFrom() {
		long companyId = 10L;
		String operatorType = "admin";
		long operatorId = 2L;
		long orderId = 9001L;
		long userId = 500L;
		String mobile = "13800000000";

		OrderAssociations assoc = new OrderAssociations();
		assoc.setCompanyId(companyId);
		assoc.setOrderId(orderId);
		assoc.setOrderType("normal");
		assoc.setDeliveryStatus("PENDING");
		assoc.setUserId(userId);
		assoc.setMobile(mobile);

		when(orderAssociationsMapper.selectOne(any())).thenReturn(assoc);
		when(orderCancelReasonConfig.reasonTextForKey(eq(3))).thenReturn("买家取消");
		when(adminNormalOrderFullCancelService.execute(
						eq(companyId),
						eq(operatorType),
						eq(operatorId),
						anyLong(),
						eq(userId),
						eq(mobile),
						eq(orderId),
						anyString(),
						anyMap(),
						eq("shop")))
				.thenReturn(Map.of("ok", true));

		Map<String, Object> mergedParams = new LinkedHashMap<>();
		mergedParams.put("cancel_reason", 3);

		adminOrderCancelService.cancelOrder(companyId, operatorType, operatorId, String.valueOf(orderId), mergedParams);

		verify(adminNormalOrderFullCancelService, times(1))
				.execute(
						eq(companyId),
						eq(operatorType),
						eq(operatorId),
						anyLong(),
						eq(userId),
						eq(mobile),
						eq(orderId),
						anyString(),
						anyMap(),
						eq("shop"));
		verify(adminNormalOrderPartialCancelService, never()).execute(anyLong(), any(), anyLong(), anyLong(), anyLong(), anyLong(), any());
	}

	@Test
	void cancelOrder_whenDeliveryNotPending_doesNotDelegateToFullCancel() {
		long companyId = 11L;
		String operatorType = "admin";
		long operatorId = 3L;
		long orderId = 9002L;
		long userId = 501L;

		OrderAssociations assoc = new OrderAssociations();
		assoc.setCompanyId(companyId);
		assoc.setOrderId(orderId);
		assoc.setOrderType("normal");
		assoc.setDeliveryStatus("PARTAIL_DELIVERY");
		assoc.setUserId(userId);
		assoc.setMobile("13900000000");

		when(orderAssociationsMapper.selectOne(any())).thenReturn(assoc);
		when(orderCancelReasonConfig.reasonTextForKey(eq(1))).thenReturn("拍错/不想要了");
		when(adminNormalOrderPartialCancelService.execute(
						eq(companyId), eq(operatorType), eq(operatorId), eq(0L), eq(userId), eq(orderId), anyString()))
				.thenReturn(Map.of("partial", true));

		Map<String, Object> mergedParams = new LinkedHashMap<>();
		mergedParams.put("cancel_reason", 1);

		adminOrderCancelService.cancelOrder(companyId, operatorType, operatorId, String.valueOf(orderId), mergedParams);

		verify(adminNormalOrderFullCancelService, never())
				.execute(anyLong(), any(), anyLong(), anyLong(), anyLong(), any(), anyLong(), any(), anyMap(), any());
		verify(adminNormalOrderPartialCancelService, times(1))
				.execute(eq(companyId), eq(operatorType), eq(operatorId), eq(0L), eq(userId), eq(orderId), eq("拍错/不想要了"));
	}
}
