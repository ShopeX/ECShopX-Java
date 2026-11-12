package cn.shopex.ecshopx.popularize.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import cn.shopex.ecshopx.common.port.popularize.OrderValidityLatestAftersaleDaysReadPort;
import cn.shopex.ecshopx.popularize.mapper.BrokerageMapper;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BrokeragePlanCloseTimeServiceTest {

	@Mock
	private BrokerageMapper brokerageMapper;

	@Mock
	private PopularizeSettingSaveService popularizeSettingSaveService;

	@Mock
	private OrderValidityLatestAftersaleDaysReadPort orderValidityLatestAftersaleDaysReadPort;

	private BrokeragePlanCloseTimeService service;

	@BeforeEach
	void setUp() {
		service =
				new BrokeragePlanCloseTimeService(
						brokerageMapper,
						popularizeSettingSaveService,
						orderValidityLatestAftersaleDaysReadPort);
	}

	@Test
	void computePlanCloseTimeEpochSec_sumsLimitTimeAndLatestAftersale() {
		when(popularizeSettingSaveService.getMergedPopularizeConfig(38L))
				.thenReturn(Map.of("limit_time", "1"));
		when(orderValidityLatestAftersaleDaysReadPort.readLatestAftersaleTimeDays(38L)).thenReturn(1);

		int before = (int) Instant.now().getEpochSecond();
		int plan = service.computePlanCloseTimeEpochSec(38L);
		int after = (int) Instant.now().getEpochSecond();

		int expectedLow = before + 86400 * 2;
		int expectedHigh = after + 86400 * 2;
		assertThat(plan).isBetween(expectedLow, expectedHigh);
	}

	@Test
	void pendingPlanCloseTimeForInsert_isMaxInt() {
		assertThat(service.pendingPlanCloseTimeForInsert())
				.isEqualTo(BrokeragePlanCloseTimeService.PLAN_CLOSE_TIME_PENDING);
	}
}
