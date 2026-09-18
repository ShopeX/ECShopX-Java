package cn.shopex.ecshopx.orders.service.rights.export;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.OrderListExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.mapper.RightsMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RightsExportServiceRightsDispatchTest {

	@Mock
	private RightsMapper rightsMapper;

	@Mock
	private RightsExportFilterAssembler rightsExportFilterAssembler;

	@Mock
	private OrderListExportFileJobDispatchPublisher publisher;

	@Mock
	private HttpServletRequest request;

	@InjectMocks
	private RightsExportService rightsExportService;

	@Test
	void exportRightData_enqueuesOnceWhenCountPositive() {
		long companyId = 501L;
		long operatorId = 502L;
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		when(rightsExportFilterAssembler.assemble(
						eq(companyId),
						eq(request),
						any(),
						any(),
						any(),
						any(),
						any(),
						any(),
						any(),
						any()))
				.thenReturn(new RightsExportFilterAssembler.Assembly(filter, false));
		when(rightsMapper.selectCount(any())).thenReturn(5L);

		rightsExportService.exportRightData(
				companyId, operatorId, request, "", "", "", "", "", "", "", "", "hdr");

		verify(publisher).enqueueRightsExport(eq(companyId), eq(operatorId), any(LinkedHashMap.class));
	}

	@Test
	void exportRightData_noEnqueueWhenCountZero() {
		long companyId = 701L;
		long operatorId = 702L;
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		when(rightsExportFilterAssembler.assemble(
						eq(companyId),
						eq(request),
						any(),
						any(),
						any(),
						any(),
						any(),
						any(),
						any(),
						any()))
				.thenReturn(new RightsExportFilterAssembler.Assembly(filter, false));
		when(rightsMapper.selectCount(any())).thenReturn(0L);

		assertThrows(
				ResourceException.class,
				() ->
						rightsExportService.exportRightData(
								companyId, operatorId, request, "", "", "", "", "", "", "", "", null));

		verifyNoInteractions(publisher);
	}

	@Test
	void exportRightData_emptyShopNoMembers_noEnqueue() {
		long companyId = 801L;
		long operatorId = 802L;
		when(rightsExportFilterAssembler.assemble(
						eq(companyId),
						eq(request),
						any(),
						any(),
						any(),
						any(),
						any(),
						any(),
						any(),
						any()))
				.thenReturn(new RightsExportFilterAssembler.Assembly(new LinkedHashMap<>(), true));

		rightsExportService.exportRightData(
				companyId, operatorId, request, "", "", "", "", "", "", "", "", null);

		verifyNoInteractions(publisher);
		verifyNoInteractions(rightsMapper);
	}
}
