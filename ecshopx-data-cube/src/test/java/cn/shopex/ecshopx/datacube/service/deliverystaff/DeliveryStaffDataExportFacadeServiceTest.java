package cn.shopex.ecshopx.datacube.service.deliverystaff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import cn.shopex.ecshopx.companys.service.deliverystaff.AdminDeliveryStaffDataExportFilter;
import cn.shopex.ecshopx.common.dispatch.DeliveryStaffDataExportFileJobDispatchPublisher;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryStaffDataExportFacadeServiceTest {

	@Mock
	private DeliveryStaffDataExportFileJobDispatchPublisher publisher;

	@Mock
	private DeliveryStaffDataCsvExportService csvExportService;

	@Test
	void submitExport_whenSyncInline_callsCsvDirectly() {
		DeliveryStaffDataExportFacadeService facade =
				new DeliveryStaffDataExportFacadeService(publisher, csvExportService, true);

		AdminDeliveryStaffDataExportFilter filter = new AdminDeliveryStaffDataExportFilter();
		filter.setCompanyId(10L);
		filter.setOperatorId(20L);
		facade.submitExport(filter);

		verify(csvExportService).runExport(filter);
		verifyNoInteractions(publisher);
	}

	@Test
	void submitExport_whenNotSync_callsPublisher() {
		DeliveryStaffDataExportFacadeService facade =
				new DeliveryStaffDataExportFacadeService(publisher, csvExportService, false);

		AdminDeliveryStaffDataExportFilter filter = new AdminDeliveryStaffDataExportFilter();
		filter.setCompanyId(10L);
		filter.setOperatorId(20L);
		filter.setUsername("bob");
		filter.setStartEpoch(1000L);
		filter.setEndEpoch(2000L);
		facade.submitExport(filter);

		verifyNoInteractions(csvExportService);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<LinkedHashMap<String, Object>> cap = ArgumentCaptor.forClass(LinkedHashMap.class);
		verify(publisher)
				.enqueueDeliveryStaffDataExport(eq(10L), eq(20L), cap.capture());
		LinkedHashMap<String, Object> sent = cap.getValue();
		assertThat(sent.get("username")).isEqualTo("bob");
		assertThat(sent.get("start_date")).isEqualTo(1000L);
		assertThat(sent.get("end_date")).isEqualTo(2000L);
	}
}
