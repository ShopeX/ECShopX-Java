package cn.shopex.ecshopx.orders.service.admin;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cn.shopex.ecshopx.orders.mapper.DistributionDistributorRefundFreightBulkMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefundFreightAutoZyDispatchExecutionServiceTest {

	@Mock
	private DistributionDistributorRefundFreightBulkMapper distributionDistributorRefundFreightBulkMapper;

	@InjectMocks
	private RefundFreightAutoZyDispatchExecutionService refundFreightAutoZyDispatchExecutionService;

	@Test
	void executeFromDispatchPayload_callsMapperWhenIsRefundFreightOne() {
		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("company_id", 77L);
		entities.put("is_refund_freight", 1);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		refundFreightAutoZyDispatchExecutionService.executeFromDispatchPayload(payload);

		verify(distributionDistributorRefundFreightBulkMapper)
				.updateIsRefundFreightByCompanyAndDistributionType(77L, 0, 1);
	}

	@Test
	void executeFromDispatchPayload_acceptsStringOneForIsRefundFreight() {
		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("company_id", 3L);
		entities.put("is_refund_freight", " 1 ");
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		refundFreightAutoZyDispatchExecutionService.executeFromDispatchPayload(payload);

		verify(distributionDistributorRefundFreightBulkMapper)
				.updateIsRefundFreightByCompanyAndDistributionType(3L, 0, 1);
	}

	@Test
	void executeFromDispatchPayload_skipsMapperWhenIsRefundFreightNotOne() {
		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("company_id", 77L);
		entities.put("is_refund_freight", 2);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		refundFreightAutoZyDispatchExecutionService.executeFromDispatchPayload(payload);

		verify(distributionDistributorRefundFreightBulkMapper, never())
				.updateIsRefundFreightByCompanyAndDistributionType(anyLong(), anyInt(), anyInt());
	}

	@Test
	void executeFromDispatchPayload_swallowsMapperException() {
		doThrow(new RuntimeException("mapper failed"))
				.when(distributionDistributorRefundFreightBulkMapper)
				.updateIsRefundFreightByCompanyAndDistributionType(9L, 0, 1);

		Map<String, Object> entities = new LinkedHashMap<>();
		entities.put("company_id", 9L);
		entities.put("is_refund_freight", 1);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("entities", entities);

		assertDoesNotThrow(() -> refundFreightAutoZyDispatchExecutionService.executeFromDispatchPayload(payload));

		verify(distributionDistributorRefundFreightBulkMapper)
				.updateIsRefundFreightByCompanyAndDistributionType(9L, 0, 1);
	}
}
