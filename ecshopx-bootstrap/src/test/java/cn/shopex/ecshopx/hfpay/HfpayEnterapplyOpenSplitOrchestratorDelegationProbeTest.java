package cn.shopex.ecshopx.hfpay;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.distribution.service.DistributorUpdateOrchestrator;
import cn.shopex.ecshopx.distribution.service.HfpayLedgerConfigReadService;
import cn.shopex.ecshopx.distribution.service.hfpay.HfpayEnterapplyOpenSplitService;
import cn.shopex.ecshopx.hfpay.mapper.HfpayEnterapplyMapper;
import cn.shopex.ecshopx.hfpay.service.enterapply.HfpayEnterapplyReadService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HfpayEnterapplyOpenSplitOrchestratorDelegationProbeTest {

	@Test
	void openSplitService_delegatesToDistributorUpdateOrchestratorUpdateWithPathDistributorIdAndNullDatapass() {
		HfpayLedgerConfigReadService hfpayLedgerConfigReadService = mock(HfpayLedgerConfigReadService.class);
		HfpayEnterapplyReadService hfpayEnterapplyReadService = mock(HfpayEnterapplyReadService.class);
		HfpayEnterapplyMapper hfpayEnterapplyMapper = mock(HfpayEnterapplyMapper.class);
		DistributorUpdateOrchestrator orchestrator = mock(DistributorUpdateOrchestrator.class);

		doNothing().when(hfpayLedgerConfigReadService).assertLedgerOpenForShopOpenSplit(anyLong());
		when(hfpayEnterapplyReadService.getEnterapply(eq(501L), eq(77L))).thenReturn(null);
		when(orchestrator.update(any(), any(), anyLong(), any(), isNull()))
				.thenReturn(Map.of("distributor_id", 77L));

		HfpayEnterapplyOpenSplitService service = new HfpayEnterapplyOpenSplitService(
				hfpayLedgerConfigReadService, hfpayEnterapplyReadService, hfpayEnterapplyMapper, orchestrator);

		Map<String, Object> operatorUser = Map.of("user_id", 1L);
		Map<String, Object> mergedParams = new LinkedHashMap<>();
		mergedParams.put("distributor_id", 77L);

		service.openSplit(501L, mergedParams, operatorUser, "zh-CN");

		verify(hfpayLedgerConfigReadService, times(1)).assertLedgerOpenForShopOpenSplit(501L);
		verify(orchestrator, times(1))
				.update(
						argThat(m -> m != null
								&& m.get("company_id") instanceof Number n
								&& n.longValue() == 501L),
						same(operatorUser),
						eq(77L),
						eq("zh-CN"),
						isNull());
	}
}
