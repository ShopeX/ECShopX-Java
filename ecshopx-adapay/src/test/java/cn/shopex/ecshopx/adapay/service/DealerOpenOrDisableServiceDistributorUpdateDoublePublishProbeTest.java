package cn.shopex.ecshopx.adapay.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.adapay.AdapayOperationLogRecordPort;
import cn.shopex.ecshopx.common.dispatch.DistributorUpdateEventDispatchPublisher;
import cn.shopex.ecshopx.companys.domain.Operators;
import cn.shopex.ecshopx.companys.mapper.OperatorsMapper;
import cn.shopex.ecshopx.companys.service.OperatorsQueryService;
import cn.shopex.ecshopx.distribution.service.DistributorUpdateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DealerOpenOrDisableServiceDistributorUpdateDoublePublishProbeTest {

	@Test
	void openOrDisable_whenDistributorIdsNonEmpty_thenPublisherPublishCalledOncePerDistributorWithPerformReturnRow() {
		OperatorsMapper operatorsMapper = mock(OperatorsMapper.class);
		OperatorsQueryService operatorsQueryService = mock(OperatorsQueryService.class);
		DistributorUpdateService distributorUpdateService = mock(DistributorUpdateService.class);
		AdapayOperationLogRecordPort adapayOperationLogRecordPort = mock(AdapayOperationLogRecordPort.class);
		ObjectMapper objectMapper = new ObjectMapper();
		DistributorUpdateEventDispatchPublisher distributorUpdateEventDispatchPublisher =
				mock(DistributorUpdateEventDispatchPublisher.class);

		Operators existing = new Operators();
		existing.setCompanyId(900L);
		existing.setOperatorId(55L);
		existing.setDistributorIds("[{\"distributor_id\":701},{\"distributor_id\":702}]");
		existing.setIsDisable(false);

		when(operatorsMapper.selectOne(any())).thenReturn(existing);
		when(operatorsMapper.updateById(existing)).thenReturn(1);

		Map<String, Object> row701 = new LinkedHashMap<>();
		row701.put("distributor_id", 701L);
		row701.put("company_id", 900L);
		Map<String, Object> row702 = new LinkedHashMap<>();
		row702.put("distributor_id", 702L);
		row702.put("company_id", 900L);

		when(distributorUpdateService.performUpdateAndEvents(any(), eq(701L), isNull())).thenReturn(row701);
		when(distributorUpdateService.performUpdateAndEvents(any(), eq(702L), isNull())).thenReturn(row702);

		Map<String, Object> operatorInfo = new LinkedHashMap<>();
		operatorInfo.put("username", "dealer-user");
		operatorInfo.put("is_dealer_main", 1);
		when(operatorsQueryService.getInfo(any())).thenReturn(operatorInfo);

		DealerOpenOrDisableService service =
				new DealerOpenOrDisableService(
						operatorsMapper,
						operatorsQueryService,
						distributorUpdateService,
						adapayOperationLogRecordPort,
						objectMapper,
						distributorUpdateEventDispatchPublisher);

		service.openOrDisable(900L, 1001L, "55", "1");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(distributorUpdateEventDispatchPublisher, times(2)).publish(captor.capture());
		assertEquals(row701, captor.getAllValues().get(0));
		assertEquals(row702, captor.getAllValues().get(1));

		verify(distributorUpdateService, times(1)).performUpdateAndEvents(any(), eq(701L), isNull());
		verify(distributorUpdateService, times(1)).performUpdateAndEvents(any(), eq(702L), isNull());
		verify(adapayOperationLogRecordPort, times(2))
				.logRecord(any(), anyLong(), eq("dealer/disable"), anyString(), anyLong());
	}
}
