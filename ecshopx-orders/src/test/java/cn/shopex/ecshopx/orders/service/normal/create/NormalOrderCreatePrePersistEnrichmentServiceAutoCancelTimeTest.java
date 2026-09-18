package cn.shopex.ecshopx.orders.service.normal.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.order.normal.NormalOrderCreateParams;
import cn.shopex.ecshopx.orders.service.normal.NormalOrderMemberGetPointsService;
import cn.shopex.ecshopx.orders.service.setting.OrderValiditySettingRedisReadService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NormalOrderCreatePrePersistEnrichmentServiceAutoCancelTimeTest {

	@Mock
	private NormalOrderMemberGetPointsService normalOrderMemberGetPointsService;

	@Mock
	private OrderValiditySettingRedisReadService orderValiditySettingRedisReadService;

	private NormalOrderCreatePrePersistEnrichmentService service;

	@BeforeEach
	void setUp() {
		service =
				new NormalOrderCreatePrePersistEnrichmentService(
						normalOrderMemberGetPointsService, orderValiditySettingRedisReadService);
	}

	@Test
	void applyBeforePersist_missingAutoCancel_usesCompanyOrderCancelMinutes() {
		when(orderValiditySettingRedisReadService.readPlatformSetting(141L))
				.thenReturn(Map.of("order_cancel_time", 5));

		NormalOrderCreateState state = new NormalOrderCreateState();
		Map<String, Object> od = new LinkedHashMap<>();
		od.put("company_id", 141L);
		od.put("user_id", 1L);
		od.put("order_type", "normal");
		od.put("items", java.util.List.of());
		state.setOrderData(od);
		state.getParams().put("pay_type", "wxpay");

		long before = Instant.now().getEpochSecond();
		service.applyBeforePersist(state);
		long after = Instant.now().getEpochSecond();

		long cancelAt = Long.parseLong(String.valueOf(od.get("auto_cancel_time")));
		assertTrue(cancelAt >= before + 5 * 60L);
		assertTrue(cancelAt <= after + 5 * 60L);
	}

	@Test
	void applyBeforePersist_existingAutoCancel_notOverwritten() {
		NormalOrderCreateState state = new NormalOrderCreateState();
		Map<String, Object> od = new LinkedHashMap<>();
		od.put("company_id", 141L);
		od.put("user_id", 1L);
		od.put("order_type", "normal");
		od.put("items", java.util.List.of());
		od.put("auto_cancel_time", "1700000000");
		state.setOrderData(od);

		service.applyBeforePersist(state);

		assertEquals("1700000000", String.valueOf(od.get("auto_cancel_time")));
		verifyNoInteractions(orderValiditySettingRedisReadService);
	}
}
