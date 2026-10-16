package cn.shopex.ecshopx.orders.service.admin.orderlist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class AdminOrderListFilterBuilderOrderStatusTest {

	private final AdminOrderListFilterBuilder builder =
			new AdminOrderListFilterBuilder(
					mock(AdminOrderListDistributorSalesmanLookup.class),
					mock(AdminOrderListSupplierOperatorIdsLookup.class));

	@Test
	void payedPartial_mapsToPayedWithPartialDeliveryStatus() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("order_status", "payed_partial");

		Map<String, Object> filter = builder.build(38L, Map.of("operator_type", "admin"), request).filter();

		assertEquals("PAYED", filter.get("order_status"));
		assertEquals("PARTAIL", filter.get("delivery_status"));
		assertFalse(filter.containsKey("cancel_status"));
		assertFalse(filter.containsKey("cancel_status|in"));
	}
}
