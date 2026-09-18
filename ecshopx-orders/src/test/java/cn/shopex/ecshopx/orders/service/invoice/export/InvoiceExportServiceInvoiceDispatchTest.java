package cn.shopex.ecshopx.orders.service.invoice.export;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.OrderListExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.mapper.NormalOrdersMapper;
import cn.shopex.ecshopx.supplier.mapper.SupplierOrderMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvoiceExportServiceInvoiceDispatchTest {

	@Mock
	private NormalOrdersMapper normalOrdersMapper;

	@Mock
	private SupplierOrderMapper supplierOrderMapper;

	@Mock
	private InvoiceExportFilterAssembler invoiceExportFilterAssembler;

	@Mock
	private OrderListExportFileJobDispatchPublisher publisher;

	@Mock
	private HttpServletRequest request;

	@InjectMocks
	private InvoiceExportService invoiceExportService;

	@Test
	void exportInvoiceData_enqueuesOnceWhenCountPositive() {
		long companyId = 601L;
		long operatorId = 602L;
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		filter.put("order_type", "normal");
		when(invoiceExportFilterAssembler.assemble(
						eq(companyId),
						eq(operatorId),
						eq("admin"),
						eq(null),
						eq(List.of()),
						eq(request)))
				.thenReturn(filter);
		when(request.getParameter("order_type")).thenReturn("normal");
		when(normalOrdersMapper.selectCount(any())).thenReturn(5L);

		invoiceExportService.exportInvoiceData(companyId, operatorId, "admin", null, List.of(), request);

		verify(publisher).enqueueInvoiceExport(eq(companyId), eq(operatorId), any(LinkedHashMap.class));
		verifyNoInteractions(supplierOrderMapper);
	}

	@Test
	void exportInvoiceData_noEnqueueWhenCountZero() {
		long companyId = 701L;
		long operatorId = 702L;
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		when(invoiceExportFilterAssembler.assemble(
						anyLong(), anyLong(), any(), any(), any(), eq(request)))
				.thenReturn(filter);
		when(request.getParameter("order_type")).thenReturn("normal");
		when(normalOrdersMapper.selectCount(any())).thenReturn(0L);

		assertThrows(
				ResourceException.class,
				() ->
						invoiceExportService.exportInvoiceData(
								companyId, operatorId, "admin", null, List.of(), request));

		verifyNoInteractions(publisher);
	}

	@Test
	void exportInvoiceData_invalidOrderType_throwsBeforeEnqueue() {
		long companyId = 801L;
		long operatorId = 802L;
		when(invoiceExportFilterAssembler.assemble(
						anyLong(), anyLong(), any(), any(), any(), eq(request)))
				.thenReturn(new LinkedHashMap<>());
		when(request.getParameter("order_type")).thenReturn("bogus");

		assertThrows(
				ResourceException.class,
				() ->
						invoiceExportService.exportInvoiceData(
								companyId, operatorId, "admin", null, List.of(), request));

		verifyNoInteractions(publisher);
		verifyNoInteractions(normalOrdersMapper);
		verifyNoInteractions(supplierOrderMapper);
	}
}
