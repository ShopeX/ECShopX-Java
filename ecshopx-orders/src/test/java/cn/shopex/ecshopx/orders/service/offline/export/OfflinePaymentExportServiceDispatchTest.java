package cn.shopex.ecshopx.orders.service.offline.export;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.OfflinePaymentExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.orders.mapper.OfflinePaymentMapper;
import cn.shopex.ecshopx.orders.service.offline.OfflinePaymentAdminQueryFilterBuilder;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OfflinePaymentExportServiceDispatchTest {

	@Mock
	private OfflinePaymentAdminQueryFilterBuilder offlinePaymentAdminQueryFilterBuilder;

	@Mock
	private OfflinePaymentMapper offlinePaymentMapper;

	@Mock
	private OfflinePaymentExportFileJobDispatchPublisher offlinePaymentExportFileJobDispatchPublisher;

	@InjectMocks
	private OfflinePaymentExportService offlinePaymentExportService;

	@Test
	void exportData_enqueuesWhenCountInRange() {
		long companyId = 11L;
		long operatorId = 22L;
		Map<String, Object> params = Map.of();

		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		when(offlinePaymentAdminQueryFilterBuilder.buildFilter(companyId, params)).thenReturn(filter);
		when(offlinePaymentMapper.selectCount(any())).thenReturn(100L);

		offlinePaymentExportService.exportData(companyId, operatorId, params);

		verify(offlinePaymentExportFileJobDispatchPublisher)
				.enqueueOfflinePaymentExport(eq(companyId), eq(operatorId), ArgumentMatchers.argThat(f -> f == filter));
	}

	@Test
	void exportData_noEnqueueWhenCountZero() {
		long companyId = 11L;
		Map<String, Object> params = Map.of();
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		when(offlinePaymentAdminQueryFilterBuilder.buildFilter(companyId, params)).thenReturn(filter);
		when(offlinePaymentMapper.selectCount(any())).thenReturn(0L);

		assertThrows(ResourceException.class, () -> offlinePaymentExportService.exportData(companyId, 0L, params));

		verifyNoInteractions(offlinePaymentExportFileJobDispatchPublisher);
	}

	@Test
	void exportData_noEnqueueWhenCountExceedsLimit() {
		long companyId = 11L;
		Map<String, Object> params = Map.of();
		LinkedHashMap<String, Object> filter = new LinkedHashMap<>();
		filter.put("company_id", companyId);
		when(offlinePaymentAdminQueryFilterBuilder.buildFilter(companyId, params)).thenReturn(filter);
		when(offlinePaymentMapper.selectCount(any())).thenReturn(20_000L);

		assertThrows(ResourceException.class, () -> offlinePaymentExportService.exportData(companyId, 0L, params));

		verifyNoInteractions(offlinePaymentExportFileJobDispatchPublisher);
	}
}
