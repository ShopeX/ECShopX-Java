package cn.shopex.ecshopx.point.service.export;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.MemberPointLogsExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.point.service.PointMemberListService;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class PointMemberLogExportOrchestratorServiceSyncPathTest {

	@Test
	void export_whenShouldQueueFalse_callsRunExportAndNeverEnqueues() {
		PointMemberListService pointMemberListService = mock(PointMemberListService.class);
		PointMemberLogCsvExportService pointMemberLogCsvExportService = mock(PointMemberLogCsvExportService.class);
		MemberPointLogsExportFileJobDispatchPublisher memberPointLogsExportFileJobDispatchPublisher =
				mock(MemberPointLogsExportFileJobDispatchPublisher.class);
		OperatorsQueryService operatorsQueryService = mock(OperatorsQueryService.class);

		when(pointMemberListService.countLocalPointMemberLogs(
						100L, 5L, "13800000000", "u1", "n1", 1_000L, 2_000L))
				.thenReturn(1L);
		when(operatorsQueryService.getInfo(any())).thenReturn(Collections.emptyMap());

		PointMemberLogExportOrchestratorService service = new PointMemberLogExportOrchestratorService(
				pointMemberListService,
				pointMemberLogCsvExportService,
				memberPointLogsExportFileJobDispatchPublisher,
				operatorsQueryService);

		service.export(
				100L,
				1L,
				"admin",
				1,
				20,
				5L,
				"13800000000",
				"u1",
				"n1",
				1_000L,
				2_000L,
				false);

		verify(pointMemberLogCsvExportService, times(1)).runExport(any(PointMemberLogExportContext.class));
		verify(memberPointLogsExportFileJobDispatchPublisher, never()).enqueueMemberPointLogsExport(any());
	}
}
