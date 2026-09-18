package cn.shopex.ecshopx.promotions.service.export;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.dispatch.LuckyDrawLogExportFileJobDispatchPublisher;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.promotions.service.TurntableConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TurntableLuckyDrawLogExportServiceDispatchTest {

	@Mock
	private LuckyDrawLogExportFileJobDispatchPublisher publisher;

	@Mock
	private TurntableConfigService turntableConfigService;

	@Mock
	private OperatorsQueryService operatorsQueryService;

	@InjectMocks
	private TurntableLuckyDrawLogExportService service;

	@Test
	void exportLog_enqueuesOnce_viaPublisher() {
		long companyId = 501L;
		long operatorId = 0L;
		long merchantId = 77L;
		String activityIdRaw = "99";
		String datapass = "x-datapass";
		when(turntableConfigService.parseRequiredActivityId(activityIdRaw)).thenReturn(99L);

		service.exportLog(companyId, operatorId, merchantId, activityIdRaw, datapass);

		verify(publisher, times(1))
				.enqueue(eq(companyId), eq(operatorId), eq(merchantId), eq(0L), eq(activityIdRaw), eq(datapass));
	}
}
