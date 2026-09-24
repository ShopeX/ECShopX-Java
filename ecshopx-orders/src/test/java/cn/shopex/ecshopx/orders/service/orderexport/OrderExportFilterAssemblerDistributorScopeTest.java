package cn.shopex.ecshopx.orders.service.orderexport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

import cn.shopex.ecshopx.common.auth.OperatorJwtRequestAttributes;
import cn.shopex.ecshopx.common.orders.port.OrderExportActivityIdsLookupPort;
import cn.shopex.ecshopx.common.web.ActivatedRequestAttributes;
import cn.shopex.ecshopx.orders.service.invoice.export.InvoiceExportSalesmanResolver;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class OrderExportFilterAssemblerDistributorScopeTest {

	private final OrderExportFilterAssembler assembler =
			new OrderExportFilterAssembler(
					mock(InvoiceExportSalesmanResolver.class), mock(OrderExportActivityIdsLookupPort.class));

	@Test
	void distributorAccount_exportsSelectedShopOnly() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("order_type", "normal");
		request.setAttribute(ActivatedRequestAttributes.DISTRIBUTOR_ID, 3046L);

		OrderExportAssemblyResult result =
				assembler.assemble(1L, 9L, "distributor", null, List.of(), List.of(), request);

		assertEquals(3046L, result.filter().get("distributor_id"));
	}

	@Test
	void distributorAccount_withoutSelection_usesJwtDistributorIds() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("order_type", "normal");
		request.setAttribute(
				OperatorJwtRequestAttributes.OPERATOR_JWT_USER_DATA,
				Map.of("distributor_ids", "[{\"distributor_id\":3046,\"name\":\"shop\"}]"));

		OrderExportAssemblyResult result =
				assembler.assemble(1L, 9L, "distributor", null, List.of(), List.of(), request);

		assertEquals(3046L, result.filter().get("distributor_id"));
	}

	@Test
	void adminAccount_doesNotForceDistributorScope() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("order_type", "normal");

		OrderExportAssemblyResult result =
				assembler.assemble(1L, 9L, "admin", null, List.of(), List.of(), request);

		assertFalse(result.filter().containsKey("distributor_id"));
		assertFalse(result.filter().containsKey("distributor_id|in"));
	}

	@Test
	void distributorAccount_selectedShopOverridesRequestParam() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("order_type", "normal");
		request.setParameter("distributor_id", "1");
		request.setAttribute(ActivatedRequestAttributes.DISTRIBUTOR_ID, 3046L);

		LinkedHashMap<String, Object> filter =
				assembler.assemble(1L, 9L, "distributor", null, List.of(1L, 3046L), List.of(), request).filter();

		assertEquals(3046L, filter.get("distributor_id"));
	}
}
