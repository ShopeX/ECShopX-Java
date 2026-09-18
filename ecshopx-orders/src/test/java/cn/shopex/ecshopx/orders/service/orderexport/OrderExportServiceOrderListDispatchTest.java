package cn.shopex.ecshopx.orders.service.orderexport;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.OrderListExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.service.orderexport.support.OrderExportDadaOrderIdResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderExportServiceOrderListDispatchTest {

	@Mock
	private OrderExportFilterAssembler filterAssembler;

	@Mock
	private OrderExportDadaOrderIdResolver orderExportDadaOrderIdResolver;

	@Mock
	private OrderExportCountCoordinator orderExportCountCoordinator;

	@Mock
	private OrderListExportFileJobDispatchPublisher orderListExportFileJobDispatchPublisher;

	@InjectMocks
	private OrderExportService orderExportService;

	@Test
	void exportOrderData_enqueuesOnceWhenCountPositive() {
		long companyId = 31L;
		long operatorId = 32L;
		HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);

		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		OrderExportAssemblyResult assembled = new OrderExportAssemblyResult(filter, "normal_order", "normal");
		when(filterAssembler.assemble(
						eq(companyId),
						eq(operatorId),
						eq("admin"),
						isNull(),
						eq(List.of()),
						eq(List.of()),
						eq(request)))
				.thenReturn(assembled);
		when(orderExportCountCoordinator.count(eq("normal"), eq("normal_order"), any()))
				.thenReturn(10L);

		orderExportService.exportOrderData(
				companyId, operatorId, "admin", null, List.of(), List.of(), request);

		verify(orderListExportFileJobDispatchPublisher)
				.enqueueOrderListExport(eq(companyId), eq(operatorId), eq("normal_order"), any());
	}

	@Test
	void exportOrderData_noEnqueueWhenCountZero() {
		long companyId = 41L;
		long operatorId = 42L;
		HttpServletRequest request = org.mockito.Mockito.mock(HttpServletRequest.class);

		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		OrderExportAssemblyResult assembled = new OrderExportAssemblyResult(filter, "normal_order", "normal");
		when(filterAssembler.assemble(
						anyLong(),
						anyLong(),
						any(),
						any(),
						any(),
						any(),
						any()))
				.thenReturn(assembled);
		when(orderExportCountCoordinator.count(any(), any(), any())).thenReturn(0L);

		assertThrows(
				ResourceException.class,
				() ->
						orderExportService.exportOrderData(
								companyId, operatorId, "admin", null, List.of(), List.of(), request));

		verifyNoInteractions(orderListExportFileJobDispatchPublisher);
	}
}
