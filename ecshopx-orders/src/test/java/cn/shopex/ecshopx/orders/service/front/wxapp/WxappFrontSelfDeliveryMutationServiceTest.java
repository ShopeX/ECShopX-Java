package cn.shopex.ecshopx.orders.service.front.wxapp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.exception.ForbiddenException;
import cn.shopex.ecshopx.orders.service.admin.AdminCancelDeliveryStaffService;
import cn.shopex.ecshopx.orders.service.admin.AdminConfirmDeliveryPackagService;
import cn.shopex.ecshopx.orders.service.admin.AdminOrderUpdateDeliveryService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WxappFrontSelfDeliveryMutationServiceTest {

	@Mock
	private WxappFrontMerchantSelfDeliveryAccessGuard accessGuard;

	@Mock
	private AdminOrderUpdateDeliveryService adminOrderUpdateDeliveryService;

	@Mock
	private AdminConfirmDeliveryPackagService adminConfirmDeliveryPackagService;

	@Mock
	private AdminCancelDeliveryStaffService adminCancelDeliveryStaffService;

	private WxappFrontSelfDeliveryMutationService service;

	@BeforeEach
	void setUp() {
		service =
				new WxappFrontSelfDeliveryMutationService(
						accessGuard,
						adminOrderUpdateDeliveryService,
						adminConfirmDeliveryPackagService,
						adminCancelDeliveryStaffService);
	}

	@Test
	void updateDelivery_whenUnauthorized_doesNotDelegate() {
		doThrow(new ForbiddenException("无权操作该单据"))
				.when(accessGuard)
				.assertAuthorizedForDeliveryId(100L, 55L, "501");

		org.assertj.core.api.Assertions.assertThatThrownBy(
						() -> service.updateDelivery(100L, 55L, "501", new LinkedHashMap<>()))
				.isInstanceOf(ForbiddenException.class);

		verify(adminOrderUpdateDeliveryService, never())
				.updateDelivery(anyLong(), anyLong(), anyString(), any());
	}

	@Test
	void updateDelivery_whenAuthorized_delegates() {
		Map<String, Object> params = new LinkedHashMap<>();
		when(adminOrderUpdateDeliveryService.updateDelivery(100L, 0L, "501", params))
				.thenReturn(Map.of("ok", true));

		service.updateDelivery(100L, 55L, "501", params);

		verify(accessGuard).assertAuthorizedForDeliveryId(100L, 55L, "501");
		verify(adminOrderUpdateDeliveryService).updateDelivery(100L, 0L, "501", params);
	}

	@Test
	void confirmDeliveryPackag_whenUnauthorized_doesNotDelegate() {
		doThrow(new ForbiddenException("无权操作该单据"))
				.when(accessGuard)
				.assertAuthorizedForOrder(100L, 55L, 200L);

		org.assertj.core.api.Assertions.assertThatThrownBy(
						() -> service.confirmDeliveryPackag(100L, 55L, "200"))
				.isInstanceOf(ForbiddenException.class);

		verify(adminConfirmDeliveryPackagService, never())
				.confirmDeliveryPackag(anyLong(), anyString(), anyLong(), anyString());
	}

	@Test
	void confirmDeliveryPackag_whenAuthorized_delegates() {
		service.confirmDeliveryPackag(100L, 55L, "200");

		verify(accessGuard).assertAuthorizedForOrder(100L, 55L, 200L);
		verify(adminConfirmDeliveryPackagService)
				.confirmDeliveryPackag(
						eq(100L),
						eq(WxappFrontSelfDeliveryMutationService.WXAPP_OPERATOR_TYPE),
						eq(WxappFrontSelfDeliveryMutationService.WXAPP_OPERATOR_ID),
						eq("200"));
	}

	@Test
	void cancelDeliveryStaff_whenUnauthorized_doesNotDelegate() {
		doThrow(new ForbiddenException("无权操作该单据"))
				.when(accessGuard)
				.assertAuthorizedForOrder(100L, 55L, 200L);

		org.assertj.core.api.Assertions.assertThatThrownBy(
						() -> service.cancelDeliveryStaff(100L, 55L, "200"))
				.isInstanceOf(ForbiddenException.class);

		verify(adminCancelDeliveryStaffService, never())
				.cancelDeliveryStaff(anyLong(), anyString(), anyLong(), anyString());
	}

	@Test
	void cancelDeliveryStaff_whenAuthorized_delegates() {
		service.cancelDeliveryStaff(100L, 55L, "200");

		verify(accessGuard).assertAuthorizedForOrder(100L, 55L, 200L);
		verify(adminCancelDeliveryStaffService)
				.cancelDeliveryStaff(
						eq(100L),
						eq(WxappFrontSelfDeliveryMutationService.WXAPP_OPERATOR_TYPE),
						eq(WxappFrontSelfDeliveryMutationService.WXAPP_OPERATOR_ID),
						eq("200"));
	}
}
